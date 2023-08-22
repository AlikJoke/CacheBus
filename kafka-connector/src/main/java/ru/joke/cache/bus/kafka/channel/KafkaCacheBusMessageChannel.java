package ru.joke.cache.bus.kafka.channel;

import org.apache.kafka.common.errors.*;
import ru.joke.cache.bus.core.CacheBus;
import ru.joke.cache.bus.core.CacheEventMessageConsumer;
import ru.joke.cache.bus.core.impl.ImmutableComponentState;
import ru.joke.cache.bus.core.metrics.*;
import ru.joke.cache.bus.core.state.ComponentState;
import ru.joke.cache.bus.core.transport.CacheBusMessageChannel;
import ru.joke.cache.bus.core.transport.CacheEntryOutputMessage;
import ru.joke.cache.bus.core.transport.MessageChannelException;
import ru.joke.cache.bus.kafka.configuration.KafkaCacheBusMessageChannelConfiguration;
import ru.joke.cache.bus.transport.addons.ChannelRecoveryProcessor;
import ru.joke.cache.bus.transport.addons.ChannelState;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static ru.joke.cache.bus.transport.ChannelConstants.MESSAGE_TYPE;

/**
 * Implementation of message channel based on Apache Kafka.
 *
 * @author Alik
 * @see KafkaCacheBusMessageChannelConfiguration
 * @see CacheBusMessageChannel
 */
@ThreadSafe
public final class KafkaCacheBusMessageChannel implements CacheBusMessageChannel<KafkaCacheBusMessageChannelConfiguration>, MetricsWriter {

    private static final Logger logger = LoggerFactory.getLogger(KafkaCacheBusMessageChannel.class);

    private static final byte[] MESSAGE_TYPE_BYTES = MESSAGE_TYPE.getBytes(StandardCharsets.UTF_8);

    private static final String CHANNEL_ID = "kafka-channel";
    private static final String MESSAGE_TYPE_HEADER = "type";
    private static final String HOST_HEADER = "host";

    private volatile ChannelState channelState;
    private volatile KafkaProducerSessionConfiguration producerSessionConfiguration;
    private volatile KafkaConsumerSessionConfiguration consumerSessionConfiguration;
    private CacheBusMetricsRegistry metrics = new NoOpCacheBusMetricsRegistry();
    private Future<?> subscribingTask;

    @Override
    public synchronized void activate(@Nonnull KafkaCacheBusMessageChannelConfiguration configuration) {
        logger.info("Activation of channel was called");

        if (this.producerSessionConfiguration != null || this.consumerSessionConfiguration != null) {
            throw new MessageChannelException("Channel already activated");
        }

        try {
            this.channelState = new ChannelState();
            this.producerSessionConfiguration = new KafkaProducerSessionConfiguration(configuration);
            this.consumerSessionConfiguration = new KafkaConsumerSessionConfiguration(configuration, true);
            registerMetrics();
        } catch (KafkaException ex) {
            logger.error("Unable to activate channel", ex);
            throw new MessageChannelException(ex);
        }
    }

    @Override
    public void send(@Nonnull CacheEntryOutputMessage eventOutputMessage) {

        KafkaProducerSessionConfiguration configuration = this.producerSessionConfiguration;
        if (configuration == null) {
            throw new MessageChannelException("Channel not activated");
        }

        while ((configuration = this.producerSessionConfiguration) != null) {

            final ProducerRecord<Integer, byte[]> record = new ProducerRecord<>(
                    configuration.sharedConfiguration.channel(),
                    null,
                    eventOutputMessage.messageHashKey(),
                    eventOutputMessage.cacheEntryMessageBody(),
                    configuration.headers
            );

            try {
                configuration.kafkaProducer.send(record, (recordMetadata, e) -> {
                    if (e != null) {
                        logger.error("Unable to send message", e);
                    }
                });

                break;
            } catch (RetriableException | BrokerNotAvailableException ex) {
                recoverProducerSession(ex, configuration);
            }
        }
    }

    @Override
    public synchronized void subscribe(@Nonnull CacheEventMessageConsumer consumer) {

        if (this.consumerSessionConfiguration == null) {
            throw new MessageChannelException("Channel not activated");
        } else if (this.subscribingTask != null) {
            throw new MessageChannelException("Already subscribed to channel");
        }

        final ExecutorService subscribingPool = this.consumerSessionConfiguration.sharedConfiguration.subscribingPool();
        this.subscribingTask = subscribingPool.submit(() -> listenUntilNotClosed(consumer));
    }

    @Override
    public synchronized void close() {

        logger.info("Channel closure was called");

        if (this.consumerSessionConfiguration == null || this.producerSessionConfiguration == null) {
            throw new MessageChannelException("Already in unsubscribed state");
        }

        final KafkaConsumerSessionConfiguration oldConsumerConfiguration = this.consumerSessionConfiguration;
        this.consumerSessionConfiguration = null;
        oldConsumerConfiguration.closeGracefully();

        final KafkaSessionConfiguration oldProducerConfiguration = this.producerSessionConfiguration;
        this.producerSessionConfiguration = null;
        oldProducerConfiguration.close();

        if (this.subscribingTask != null) {
            this.subscribingTask.cancel(true);
            this.subscribingTask = null;
        }

        this.channelState = null;
    }

    @Nonnull
    @Override
    public synchronized ComponentState state() {

        final ChannelState channelState = this.channelState;
        if (channelState == null || this.consumerSessionConfiguration == null || this.producerSessionConfiguration == null) {
            return new ImmutableComponentState(CHANNEL_ID, ComponentState.Status.DOWN);
        } else if (this.subscribingTask == null) {
            return new ImmutableComponentState(CHANNEL_ID, ComponentState.Status.UP_NOT_READY);
        }

        final List<ComponentState.SeverityInfo> severities = new ArrayList<>();
        channelState.summarizeStats().forEach(s -> severities.add(() -> s));

        final ComponentState.Status status =
                channelState.unrecoverableConsumers() > 0
                        ? ComponentState.Status.UP_FATAL_BROKEN
                        : ComponentState.Status.UP_OK;

        return new ImmutableComponentState(CHANNEL_ID, status, severities);
    }

    private void listenUntilNotClosed(final CacheEventMessageConsumer messageConsumer) {
        logger.info("Subscribe was called");

        Consumer<Integer, byte[]> kafkaConsumer = null;
        KafkaConsumerSessionConfiguration sessionConfiguration;
        while ((sessionConfiguration = this.consumerSessionConfiguration) != null) {

            try {
                kafkaConsumer = sessionConfiguration.kafkaConsumer;
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
                            || Arrays.equals(sessionConfiguration.hostNameBytes, hostHeader.value())) {
                        return;
                    }

                    final byte[] messageBody = record.value();
                    final int messageHash = record.key();

                    messageConsumer.accept(messageHash, messageBody);
                }

                kafkaConsumer.commitSync();
            } catch (WakeupException | InterruptException ex) {
                logger.warn("Consumer was interrupted", ex);
                closeConsumer(kafkaConsumer);
                return;
            } catch (RetriableException | OffsetOutOfRangeException | BrokerNotAvailableException | FencedInstanceIdException ex) {
                recoverConsumerSession(ex, sessionConfiguration);
            } catch (KafkaException ex) {
                logger.error("Unrecoverable exception in consumer thread", ex);
                closeConsumer(kafkaConsumer);

                throw new MessageChannelException(ex);
            }
        }

        if (kafkaConsumer != null) {
            closeConsumer(kafkaConsumer);
        }
    }

    private void closeConsumer(final Consumer<Integer, byte[]> kafkaConsumer) {

        final ChannelState channelState = this.channelState;
        if (channelState != null) {
            channelState.increaseCountOfUnrecoverableConsumers();
        }

        try {
            kafkaConsumer.unsubscribe();
            kafkaConsumer.close();
        } catch (Exception ex) {
            logger.error("Unable to close consumer", ex);
        }
    }

    private void recoverProducerSession(final Exception ex, final KafkaSessionConfiguration sessionConfiguration) {

        final ChannelState channelState = this.channelState;
        if (channelState == null) {
            // The channel has been closed, there is nothing else to do
            return;
        }

        // Synchronization on the configuration object in case of parallel message sending threads
        synchronized (sessionConfiguration) {

            // If it doesn't match, it means another thread has already restored the connection => it's safe to attempt message sending
            if (this.producerSessionConfiguration != sessionConfiguration) {
                return;
            }

            channelState.increaseCountOfProducersInRecoveryState();
            this.metrics.incrementCounter(KnownMetrics.PRODUCERS_IN_RECOVERY_COUNT);

            final ChannelRecoveryProcessor recoveryProcessor = new ChannelRecoveryProcessor(
                    sessionConfiguration::close,
                    () -> this.producerSessionConfiguration = new KafkaProducerSessionConfiguration(sessionConfiguration.sharedConfiguration),
                    sessionConfiguration.sharedConfiguration.reconnectTimeoutMs()
            );

            try {
                this.metrics.recordExecutionTime(
                        KnownMetrics.PRODUCER_CONNECTION_RECOVERY_TIME,
                        () -> recoveryProcessor.recover(ex)
                );
            } catch (RuntimeException e) {
                channelState.increaseCountOfUnrecoverableProducers();
                throw new MessageChannelException(e);
            } finally {
                channelState.decreaseCountProducersInRecoveryState();
                this.metrics.decrementCounter(KnownMetrics.PRODUCERS_IN_RECOVERY_COUNT);
            }
        }
    }

    private void registerMetrics() {
        this.metrics.registerTimer(new Metrics.Timer(KnownMetrics.PRODUCER_CONNECTION_WAIT_TIME));
        this.metrics.registerTimer(new Metrics.Timer(KnownMetrics.PRODUCER_CONNECTION_RECOVERY_TIME));
        this.metrics.registerTimer(new Metrics.Timer(KnownMetrics.CONSUMER_CONNECTION_RECOVERY_TIME));
        this.metrics.registerCounter(new Metrics.Counter(KnownMetrics.PRODUCERS_IN_RECOVERY_COUNT));
        this.metrics.registerCounter(new Metrics.Counter(KnownMetrics.CONSUMERS_IN_RECOVERY_COUNT));
    }

    private void recoverConsumerSession(final Exception ex, final KafkaSessionConfiguration sessionConfiguration) {

        // Thread was interrupted
        final ChannelState channelState = this.channelState;
        if (sessionConfiguration == null) {
            return;
        }

        channelState.increaseCountOfConsumersInRecoveryState();
        this.metrics.incrementCounter(KnownMetrics.CONSUMERS_IN_RECOVERY_COUNT);

        final ChannelRecoveryProcessor recoveryProcessor = new ChannelRecoveryProcessor(
                sessionConfiguration::close,
                () -> this.consumerSessionConfiguration = new KafkaConsumerSessionConfiguration(sessionConfiguration.sharedConfiguration, false),
                Integer.MAX_VALUE
        );

        try {
            this.metrics.recordExecutionTime(
                    KnownMetrics.CONSUMER_CONNECTION_RECOVERY_TIME,
                    () -> recoveryProcessor.recover(ex)
            );
        } catch (RuntimeException e) {
            channelState.increaseCountOfUnrecoverableConsumers();
            throw new MessageChannelException(e);
        } finally {
            channelState.decreaseCountConsumersInRecoveryState();
            this.metrics.decrementCounter(KnownMetrics.CONSUMERS_IN_RECOVERY_COUNT);
        }
    }

    @Override
    public void setMetrics(@Nonnull CacheBusMetricsRegistry registry) {
        if (this.subscribingTask != null || this.consumerSessionConfiguration != null) {
            throw new MessageChannelException("Not allowed if channel already activated");
        }

        this.metrics = Objects.requireNonNull(registry, "registry");
    }

    private static abstract class KafkaSessionConfiguration implements AutoCloseable {

        protected final KafkaCacheBusMessageChannelConfiguration sharedConfiguration;
        protected final byte[] hostNameBytes;
        protected final List<Header> headers;

        private KafkaSessionConfiguration(final KafkaCacheBusMessageChannelConfiguration sharedConfiguration) {
            this.sharedConfiguration = sharedConfiguration;
            final String hostName = sharedConfiguration.hostNameResolver().resolve();
            this.hostNameBytes = hostName.getBytes(StandardCharsets.UTF_8);
            this.headers = createMessageHeaders();
        }

        @Override
        public abstract void close();

        private List<Header> createMessageHeaders() {
            final List<Header> headers = new ArrayList<>(2);
            headers.add(new RecordHeader(MESSAGE_TYPE_HEADER, MESSAGE_TYPE_BYTES));
            headers.add(new RecordHeader(HOST_HEADER, this.hostNameBytes));

            return headers;
        }
    }

    private static class KafkaProducerSessionConfiguration extends KafkaSessionConfiguration {

        private final Producer<Integer, byte[]> kafkaProducer;

        private KafkaProducerSessionConfiguration(final KafkaCacheBusMessageChannelConfiguration sharedConfiguration) {
            super(sharedConfiguration);

            final Serializer<byte[]> valueSerializer = new ByteArraySerializer();
            final Serializer<Integer> keySerializer = new IntegerSerializer();
            this.kafkaProducer = new KafkaProducer<>(
                    enrichProducerProperties(sharedConfiguration.producerProperties()),
                    keySerializer,
                    valueSerializer
            );
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
            props.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, MessageIntHashKeyPartitioner.class.getCanonicalName());

            return props;
        }

        @Override
        public void close() {
            this.kafkaProducer.close();
        }
    }

    private static class KafkaConsumerSessionConfiguration extends KafkaSessionConfiguration {

        private final Consumer<Integer, byte[]> kafkaConsumer;

        private KafkaConsumerSessionConfiguration(KafkaCacheBusMessageChannelConfiguration sharedConfiguration, boolean firstSubscription) {
            super(sharedConfiguration);

            final String hostName = sharedConfiguration.hostNameResolver().resolve();
            final Deserializer<byte[]> valueDeserializer = new ByteArrayDeserializer();
            final Deserializer<Integer> keyDeserializer = new IntegerDeserializer();
            this.kafkaConsumer = new KafkaConsumer<>(
                    enrichConsumerProperties(sharedConfiguration.consumerProperties(), firstSubscription, hostName),
                    keyDeserializer,
                    valueDeserializer
            );

            this.kafkaConsumer.subscribe(Collections.singleton(sharedConfiguration.channel()));
        }

        private Map<String, Object> enrichConsumerProperties(
                final Map<String, Object> consumerProperties,
                final boolean firstSubscription,
                final String hostName) {

            final Map<String, Object> props = new HashMap<>(consumerProperties);
            props.putIfAbsent(ConsumerConfig.CLIENT_ID_CONFIG, CacheBus.class.getSimpleName());
            props.put(ConsumerConfig.GROUP_ID_CONFIG, CacheBus.class.getSimpleName() + "_" + hostName + "_" + UUID.randomUUID());
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, firstSubscription ? "latest" : "earliest");
            props.putIfAbsent(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1_000);
            props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
            props.putIfAbsent(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30_000);
            props.putIfAbsent(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 6_000);

            return props;
        }

        @Override
        public void close() {
        }

        void closeGracefully() {
            this.kafkaConsumer.wakeup();
        }
    }
}
