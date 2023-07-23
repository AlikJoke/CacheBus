package net.cache.bus.rabbit.channel;

import com.rabbitmq.client.*;
import net.cache.bus.core.CacheEventMessageConsumer;
import net.cache.bus.core.impl.ImmutableComponentState;
import net.cache.bus.core.metrics.*;
import net.cache.bus.core.state.ComponentState;
import net.cache.bus.core.transport.CacheBusMessageChannel;
import net.cache.bus.core.transport.CacheEntryOutputMessage;
import net.cache.bus.core.transport.MessageChannelException;
import net.cache.bus.rabbit.configuration.RabbitCacheBusMessageChannelConfiguration;
import net.cache.bus.transport.addons.ChannelRecoveryProcessor;
import net.cache.bus.transport.addons.ChannelState;
import net.cache.bus.transport.addons.ConcurrentLinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.cache.bus.transport.ChannelConstants.*;

/**
 * Implementation of channel based on RabbitMQ.
 *
 * @author Alik
 * @see RabbitCacheBusMessageChannelConfiguration
 * @see CacheBusMessageChannel
 */
@ThreadSafe
public final class RabbitCacheBusMessageChannel implements CacheBusMessageChannel<RabbitCacheBusMessageChannelConfiguration>, MetricsWriter {

    static final String HOST_PROPERTY = "host";
    static final String HASH_KEY_PROPERTY = "hash";

    private static final Logger logger = LoggerFactory.getLogger(RabbitCacheBusMessageChannel.class);

    private static final String CHANNEL_ID = "rabbit-channel";

    private volatile RabbitConsumerSessionConfiguration consumerSessionConfiguration;
    private volatile RabbitProducerSessionConfiguration producerSessionConfiguration;
    private volatile ChannelState channelState;
    private CacheBusMetricsRegistry metrics = new NoOpCacheBusMetricsRegistry();
    private CacheEventMessageConsumer messageConsumer;

    @Override
    public synchronized void activate(@Nonnull RabbitCacheBusMessageChannelConfiguration rabbitConfiguration) {
        logger.info("Activation of channel was called");

        if (this.producerSessionConfiguration != null || this.consumerSessionConfiguration != null) {
            throw new MessageChannelException("Channel already activated");
        }

        this.channelState = new ChannelState();
        initializeConsumerSession(rabbitConfiguration);
        initializeProducerSession(rabbitConfiguration);
        registerMetrics();
    }

    @Override
    public void send(@Nonnull CacheEntryOutputMessage eventOutputMessage) {

        RabbitProducerSessionConfiguration sessionConfiguration = this.producerSessionConfiguration;
        if (sessionConfiguration == null) {
            throw new MessageChannelException("Channel not activated");
        }

        final AMQP.BasicProperties basicProperties = createProducerProperties(sessionConfiguration, eventOutputMessage.messageHashKey());

        boolean retry;
        do {
            retry = false;
            Channel channel = null;
            try {
                final var sendingChannels = sessionConfiguration.sendingChannels;
                channel = this.metrics.recordExecutionTime(
                        KnownMetrics.PRODUCER_CONNECTION_WAIT_TIME,
                        () -> retrieveChannel(sendingChannels)
                );

                if (channel == null) {
                    // It means that thread was interrupted
                    return;
                }

                final String channelName = sessionConfiguration.sharedConfiguration.channel();
                channel.basicPublish(
                        "",
                        channelName,
                        basicProperties,
                        eventOutputMessage.cacheEntryMessageBody()
                );

                logger.info("Message {} was sent to topic: {}", eventOutputMessage, channelName);
            } catch (IOException ex) {
                recoverProducerSession(ex, sessionConfiguration);
                retry = true;
            } catch (Exception ex) {
                // should never be thrown here
                throw new RuntimeException(ex);
            } finally {
                if (channel != null && !retry) {
                    sessionConfiguration.sendingChannels.offer(channel);
                }
            }
        } while (retry && (sessionConfiguration = this.producerSessionConfiguration) != null);
    }

    @Override
    public synchronized void subscribe(@Nonnull CacheEventMessageConsumer consumer) {
        if (this.consumerSessionConfiguration == null) {
            throw new MessageChannelException("Channel not activated");
        }

        this.messageConsumer = consumer;
        subscribeToChannel();
    }

    @Override
    public synchronized void close() {
        logger.info("Channel closure was called");

        if (this.consumerSessionConfiguration == null || this.producerSessionConfiguration == null) {
            throw new MessageChannelException("Already in closed state");
        }

        final RabbitConsumerSessionConfiguration consumerSessionConfiguration = this.consumerSessionConfiguration;
        this.consumerSessionConfiguration = null;
        consumerSessionConfiguration.close();

        final RabbitProducerSessionConfiguration producerSessionConfiguration = this.producerSessionConfiguration;
        this.producerSessionConfiguration = null;
        producerSessionConfiguration.close();

        this.messageConsumer = null;
        this.channelState = null;
    }

    @Nonnull
    @Override
    public synchronized ComponentState state() {

        final ChannelState channelState = this.channelState;
        if (channelState == null || this.consumerSessionConfiguration == null || this.producerSessionConfiguration == null) {
            return new ImmutableComponentState(CHANNEL_ID, ComponentState.Status.DOWN);
        } else if (this.messageConsumer == null) {
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

    private Channel retrieveChannel(final ConcurrentLinkedBlockingQueue<Channel> sendingChannels) {

        Channel channel = null;
        try {
            channel = sendingChannels.poll(POLL_CHANNEL_TIMEOUT, TimeUnit.SECONDS);
            if (channel == null) {
                logger.info("Could not retrieve channel in %d seconds timeout, maybe you should increase count of available channels in rabbit channel configuration?".formatted(POLL_CHANNEL_TIMEOUT));
                channel = waitForChannelUntilAvailable(sendingChannels);
            }
        } catch (InterruptedException ex) {
            handleInterruptionOfThread(ex);
        }

        return channel;
    }

    private Channel waitForChannelUntilAvailable(final ConcurrentLinkedBlockingQueue<Channel> channels) throws InterruptedException {

        final ChannelState channelState = this.channelState;
        if (channelState != null) {
            channelState.increaseCountOfProducersInBusyPollingOfConnections();
        }

        try {
            return channels.take();
        } finally {
            if (channelState != null) {
                channelState.decreaseCountOfProducersInBusyPollingOfConnections();
            }
        }
    }

    private void handleInterruptionOfThread(final InterruptedException ex) {
        logger.info("Thread was interrupted", ex);

        final ChannelState channelState = this.channelState;
        if (channelState != null) {
            channelState.increaseCountOfInterruptedThreads();
        }

        Thread.currentThread().interrupt();
    }

    private void subscribeToChannel() {

        logger.info("Subscribe was called");

        RabbitConsumerSessionConfiguration sessionConfiguration;
        ChannelState channelState = this.channelState;
        if ((sessionConfiguration = this.consumerSessionConfiguration) == null) {
            return;
        }

        try {
            final String hostName = sessionConfiguration.hostName;
            final var messageConsumer = this.messageConsumer;
            final var rabbitConsumer = new DefaultConsumer(sessionConfiguration.inputChannel) {
                @Override
                public void handleDelivery(
                        final String consumerTag,
                        final Envelope envelope,
                        final AMQP.BasicProperties properties,
                        final byte[] body) {

                    final Map<String, Object> headers = properties.getHeaders();
                    if (!MESSAGE_TYPE.equals(properties.getType())
                            || !hostName.equals(headers.get(HOST_PROPERTY))) {
                        return;
                    }

                    final Integer messageHash = (Integer) headers.get(HASH_KEY_PROPERTY);
                    messageConsumer.accept(messageHash, body);
                }
            };

            // the error will never happen, so we can use auto ack
            sessionConfiguration.inputChannel.basicConsume(
                    sessionConfiguration.sharedConfiguration.channel(),
                    true,
                    null,
                    true,
                    false,
                    Collections.emptyMap(),
                    rabbitConsumer
            );
        } catch (IOException ex) {
            logger.error("Unable to subscribe", ex);
            channelState.increaseCountOfUnrecoverableConsumers();
            throw new MessageChannelException(ex);
        }
    }

    private AMQP.BasicProperties createProducerProperties(
            final RabbitSessionConfiguration sessionConfiguration,
            final int messageHashKey) {

        return new AMQP.BasicProperties()
                            .builder()
                            .headers(
                                    Map.of(
                                            HOST_PROPERTY, sessionConfiguration.hostName,
                                            HASH_KEY_PROPERTY, messageHashKey
                                    )
                            )
                            .type(MESSAGE_TYPE)
                            .deliveryMode(2)
                            .build();
    }

    private void initializeConsumerSession(final RabbitCacheBusMessageChannelConfiguration rabbitConfiguration) {
        try {
            this.consumerSessionConfiguration = new RabbitConsumerSessionConfiguration(rabbitConfiguration);
        } catch (IOException | TimeoutException ex) {
            logger.error("Unable to activate channel", ex);
            throw new MessageChannelException(ex);
        }
    }

    private void registerMetrics() {
        this.metrics.registerTimer(new Metrics.Timer(KnownMetrics.PRODUCER_CONNECTION_WAIT_TIME));
        this.metrics.registerTimer(new Metrics.Timer(KnownMetrics.PRODUCER_CONNECTION_RECOVERY_TIME));
        this.metrics.registerTimer(new Metrics.Timer(KnownMetrics.CONSUMER_CONNECTION_RECOVERY_TIME));
        this.metrics.registerCounter(new Metrics.Counter(KnownMetrics.PRODUCERS_IN_RECOVERY_COUNT));
        this.metrics.registerCounter(new Metrics.Counter(KnownMetrics.CONSUMERS_IN_RECOVERY_COUNT));
    }

    private void initializeProducerSession(final RabbitCacheBusMessageChannelConfiguration rabbitConfiguration) {
        try {
            this.producerSessionConfiguration = new RabbitProducerSessionConfiguration(rabbitConfiguration);
        } catch (IOException | TimeoutException ex) {
            logger.error("Unable to activate channel", ex);
            throw new MessageChannelException(ex);
        }
    }

    private void recoverProducerSession(
            final Exception ex,
            final RabbitProducerSessionConfiguration sessionConfiguration) {

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

            // With high probability, if there is a problem with one channel, there will be problems with others
            // due to the shared connection, so we completely recreate the channels
            final ChannelRecoveryProcessor recoveryProcessor = new ChannelRecoveryProcessor(
                    sessionConfiguration::close,
                    () -> initializeProducerSession(sessionConfiguration.sharedConfiguration),
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
                this.metrics.incrementCounter(KnownMetrics.CONSUMERS_IN_RECOVERY_COUNT);
            }
        }
    }

    private void recoverConsumerSession(final Exception ex) {

        // Thread was interrupted
        final RabbitSessionConfiguration sessionConfiguration = this.consumerSessionConfiguration;
        final ChannelState channelState = this.channelState;
        if (sessionConfiguration == null || channelState == null) {
            return;
        }

        channelState.increaseCountOfConsumersInRecoveryState();
        this.metrics.incrementCounter(KnownMetrics.CONSUMERS_IN_RECOVERY_COUNT);

        final ChannelRecoveryProcessor recoveryProcessor = new ChannelRecoveryProcessor(
                sessionConfiguration::close,
                () -> initializeConsumerSession(sessionConfiguration.sharedConfiguration),
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
        if (this.messageConsumer != null || this.consumerSessionConfiguration != null) {
            throw new MessageChannelException("Not allowed if channel already activated");
        }

        this.metrics = Objects.requireNonNull(registry, "registry");
    }

    private abstract static class RabbitSessionConfiguration implements AutoCloseable {

        protected final Connection rabbitConnection;
        protected final String hostName;
        protected final RabbitCacheBusMessageChannelConfiguration sharedConfiguration;

        private RabbitSessionConfiguration(@Nonnull final RabbitCacheBusMessageChannelConfiguration sharedConfiguration) throws IOException, TimeoutException {
            this.sharedConfiguration = sharedConfiguration;
            this.rabbitConnection = sharedConfiguration.connectionFactory().newConnection();
            this.hostName = sharedConfiguration.hostNameResolver().resolve();
        }

        @Override
        public void close() {
            try {
                this.rabbitConnection.close();
            } catch (IOException ex) {
                logger.error("Unable to close connection to rabbit", ex);
            }
        }
    }

    private class RabbitProducerSessionConfiguration extends RabbitSessionConfiguration {

        private final ConcurrentLinkedBlockingQueue<Channel> sendingChannels;

        private RabbitProducerSessionConfiguration(@Nonnull RabbitCacheBusMessageChannelConfiguration sharedConfiguration) throws IOException, TimeoutException {
            super(sharedConfiguration);
            this.rabbitConnection.addShutdownListener(e -> {
                if (!e.isInitiatedByApplication()) {
                    recoverProducerSession(e, this);
                }
            });

            this.sendingChannels = new ConcurrentLinkedBlockingQueue<>();
            for (int i = 0; i < sharedConfiguration.availableChannelsCount() - 1; i++) {
                this.sendingChannels.offer(this.rabbitConnection.createChannel());
            }
        }
    }

    private class RabbitConsumerSessionConfiguration extends RabbitSessionConfiguration {

        private final Channel inputChannel;

        private RabbitConsumerSessionConfiguration(@Nonnull RabbitCacheBusMessageChannelConfiguration sharedConfiguration) throws IOException, TimeoutException {
            super(sharedConfiguration);
            this.rabbitConnection.addShutdownListener(e -> {
                if (!e.isInitiatedByApplication()) {
                    recoverConsumerSession(e);
                    subscribeToChannel();
                }
            });

            this.inputChannel = this.rabbitConnection.createChannel();
            this.inputChannel.queueDeclare(sharedConfiguration.channel(), false, false, false, Collections.emptyMap());
        }
    }
}
