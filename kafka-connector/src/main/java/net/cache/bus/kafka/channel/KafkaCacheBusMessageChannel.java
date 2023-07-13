package net.cache.bus.kafka.channel;

import net.cache.bus.core.CacheBus;
import net.cache.bus.core.CacheEventMessageConsumer;
import net.cache.bus.core.LifecycleException;
import net.cache.bus.core.transport.CacheBusMessageChannel;
import net.cache.bus.core.transport.CacheEntryOutputMessage;
import net.cache.bus.kafka.configuration.KafkaCacheBusMessageChannelConfiguration;
import net.cache.bus.transport.addons.ChannelRecoveryProcessor;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.*;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Реализация канала сообщений на основе Apache Kafka.
 *
 * @author Alik
 * @see KafkaCacheBusMessageChannelConfiguration
 * @see CacheBusMessageChannel
 */
@ThreadSafe
public final class KafkaCacheBusMessageChannel implements CacheBusMessageChannel<KafkaCacheBusMessageChannelConfiguration> {

    private static final Logger logger = Logger.getLogger(KafkaCacheBusMessageChannel.class.getCanonicalName());

    private static final byte[] MESSAGE_TYPE_BYTES = "CacheEvent".getBytes(StandardCharsets.UTF_8);

    private static final String MESSAGE_TYPE_HEADER = "type";
    private static final String HOST_HEADER = "host";

    private volatile KafkaConfiguration kafkaConfiguration;
    private Future<?> subscribingTask;

    @Override
    public synchronized void activate(@Nonnull KafkaCacheBusMessageChannelConfiguration configuration) {
        this.kafkaConfiguration = new KafkaConfiguration(configuration, this.subscribingTask == null);
    }

    @Override
    public void send(@Nonnull CacheEntryOutputMessage eventOutputMessage) {

        final KafkaConfiguration configuration = this.kafkaConfiguration;
        if (configuration == null) {
            throw new LifecycleException("Channel not activated");
        }

        final ProducerRecord<Integer, byte[]> record = new ProducerRecord<>(
                configuration.channelConfiguration.channel(),
                null,
                eventOutputMessage.messageHashKey(),
                eventOutputMessage.cacheEntryMessageBody(),
                configuration.headers
        );

        configuration.kafkaProducer.send(record, (recordMetadata, e) -> {
            if (e != null) {
                logger.log(Level.ALL, "Unable to send message", e);
            }
        });
    }

    @Override
    public synchronized void subscribe(@Nonnull CacheEventMessageConsumer consumer) {

        if (this.kafkaConfiguration == null) {
            throw new LifecycleException("Channel not activated");
        } else if (this.subscribingTask != null) {
            throw new LifecycleException("Already subscribed to channel");
        }

        this.subscribingTask = this.kafkaConfiguration.channelConfiguration.subscribingPool().submit(() -> listenUntilNotClosed(consumer));
    }

    @Override
    public synchronized void unsubscribe() {

        logger.info(() -> "Unsubscribe was called");

        if (this.kafkaConfiguration == null) {
            throw new LifecycleException("Already in unsubscribed state");
        }

        this.kafkaConfiguration = null;

        if (this.subscribingTask != null) {
            this.subscribingTask.cancel(true);
            this.subscribingTask = null;
        }
    }

    private void listenUntilNotClosed(final CacheEventMessageConsumer messageConsumer) {
        logger.info(() -> "Subscribe was called");

        KafkaConfiguration configuration;
        Consumer<Integer, byte[]> kafkaConsumer = null;
        while ((configuration = this.kafkaConfiguration) != null) {

            try {
                kafkaConsumer = configuration.kafkaConsumer;
                final ConsumerRecords<Integer, byte[]> records = kafkaConsumer.poll(Duration.ofMillis(Long.MAX_VALUE));
                if (records.isEmpty()) {
                    continue;
                }

                for (final var record : records) {

                    final Header messageTypeHeader = record.headers().lastHeader(MESSAGE_TYPE_HEADER);
                    final Header hostHeader = record.headers().lastHeader(HOST_HEADER);
                    if (messageTypeHeader == null
                            || hostHeader == null
                            || !Arrays.equals(MESSAGE_TYPE_BYTES, messageTypeHeader.value())
                            || Arrays.equals(configuration.hostNameBytes, hostHeader.value())) {
                        return;
                    }

                    final byte[] messageBody = record.value();
                    final int messageHash = record.key();

                    messageConsumer.accept(messageHash, messageBody);
                }

                kafkaConsumer.commitSync();
            } catch (KafkaException ex) {
                onException(ex);
            }
        }

        if (kafkaConsumer != null) {
            kafkaConsumer.unsubscribe();
            kafkaConsumer.close();
        }
    }

    private void onException(final Exception ex) {

        // Поток прервали
        if (this.kafkaConfiguration == null) {
            return;
        }

        final KafkaConfiguration oldConfiguration = this.kafkaConfiguration;
        final ChannelRecoveryProcessor recoveryProcessor = new ChannelRecoveryProcessor(
                () -> oldConfiguration.close(false),
                () -> this.activate(oldConfiguration.channelConfiguration),
                Integer.MAX_VALUE
        );

        recoveryProcessor.recover(ex);
        oldConfiguration.close(true);
    }

    static class KafkaConfiguration {

        private final KafkaCacheBusMessageChannelConfiguration channelConfiguration;
        private final Consumer<Integer, byte[]> kafkaConsumer;
        private final Producer<Integer, byte[]> kafkaProducer;
        private final byte[] hostNameBytes;
        private final List<Header> headers;

        private KafkaConfiguration(final KafkaCacheBusMessageChannelConfiguration configuration, final boolean firstSubscription) {
            this.channelConfiguration = configuration;

            final Serializer<byte[]> valueSerializer = new ByteArraySerializer();
            final Serializer<Integer> keySerializer = new IntegerSerializer();
            this.kafkaProducer = new KafkaProducer<>(
                    enrichProducerProperties(configuration.producerProperties()),
                    keySerializer,
                    valueSerializer
            );

            final String hostName = configuration.hostNameResolver().resolve();
            final Deserializer<byte[]> valueDeserializer = new ByteArrayDeserializer();
            final Deserializer<Integer> keyDeserializer = new IntegerDeserializer();
            this.kafkaConsumer = new KafkaConsumer<>(
                    enrichConsumerProperties(configuration.consumerProperties(), firstSubscription, hostName),
                    keyDeserializer,
                    valueDeserializer
            );
            this.kafkaConsumer.subscribe(Collections.singleton(configuration.channel()));

            this.hostNameBytes = hostName.getBytes(StandardCharsets.UTF_8);
            this.headers = createMessageHeaders();
        }

        private void close(final boolean closeProducer) {
            if (closeProducer) {
                this.kafkaProducer.close();
            }

            this.kafkaConsumer.close();
        }

        private Map<String, Object> enrichProducerProperties(final Map<String, Object> producerProperties) {

            final Map<String, Object> props = new HashMap<>(producerProperties);
            props.putIfAbsent(ProducerConfig.CLIENT_ID_CONFIG, CacheBus.class.getSimpleName());
            props.putIfAbsent(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
            props.putIfAbsent(ProducerConfig.ACKS_CONFIG, "all");
            props.putIfAbsent(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
            props.putIfAbsent(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120_000);
            props.putIfAbsent(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
            props.putIfAbsent(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30_000);
            props.putIfAbsent(ProducerConfig.LINGER_MS_CONFIG, 5);

            return props;
        }

        private Map<String, Object> enrichConsumerProperties(
                final Map<String, Object> consumerProperties,
                final boolean firstSubscription,
                final String hostName) {

            final Map<String, Object> props = new HashMap<>(consumerProperties);
            props.putIfAbsent(ConsumerConfig.CLIENT_ID_CONFIG, CacheBus.class.getSimpleName());
            props.put(ConsumerConfig.GROUP_ID_CONFIG, CacheBus.class.getSimpleName() + "_" + hostName + "_" + UUID.randomUUID().toString());
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, firstSubscription ? "latest" : "earliest");
            props.putIfAbsent(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1_000);
            props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
            props.putIfAbsent(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30_000);
            props.putIfAbsent(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 6_000);

            return props;
        }

        private List<Header> createMessageHeaders() {
            final List<Header> headers = new ArrayList<>(2);
            headers.add(new RecordHeader(MESSAGE_TYPE_HEADER, MESSAGE_TYPE_BYTES));
            headers.add(new RecordHeader(HOST_HEADER, this.hostNameBytes));

            return headers;
        }
    }
}
