package ru.joke.cache.bus.jms.channel;

import ru.joke.cache.bus.core.CacheEventMessageConsumer;
import ru.joke.cache.bus.core.impl.ImmutableComponentState;
import ru.joke.cache.bus.core.metrics.*;
import ru.joke.cache.bus.core.state.ComponentState;
import ru.joke.cache.bus.core.transport.CacheBusMessageChannel;
import ru.joke.cache.bus.core.transport.CacheEntryOutputMessage;
import ru.joke.cache.bus.core.transport.MessageChannelException;
import ru.joke.cache.bus.jms.configuration.JmsCacheBusMessageChannelConfiguration;
import ru.joke.cache.bus.transport.addons.ChannelRecoveryProcessor;
import ru.joke.cache.bus.transport.addons.ChannelState;
import ru.joke.cache.bus.transport.addons.ConcurrentLinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.jms.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;

import static ru.joke.cache.bus.transport.ChannelConstants.*;

/**
 * Implementation of a message channel based on JMS.
 *
 * @author Alik
 * @see JmsCacheBusMessageChannelConfiguration
 * @see CacheBusMessageChannel
 */
@ThreadSafe
public final class JmsCacheBusMessageChannel implements CacheBusMessageChannel<JmsCacheBusMessageChannelConfiguration>, MetricsWriter {

    static final String HOST_PROPERTY = "host";
    static final String HASH_KEY_PROPERTY = "hash";

    private static final String CHANNEL_ID = "jms-channel";

    private static final Logger logger = LoggerFactory.getLogger(JmsCacheBusMessageChannel.class);

    private volatile ChannelState channelState;
    private volatile JmsConsumerSessionConfiguration consumerConfiguration;
    private volatile ConcurrentLinkedBlockingQueue<JmsProducerSessionConfiguration> producerConfigurations;

    private Future<?> subscribingTask;
    private CacheBusMetricsRegistry metrics = new NoOpCacheBusMetricsRegistry();

    @Override
    public synchronized void activate(@Nonnull JmsCacheBusMessageChannelConfiguration jmsConfiguration) {
        logger.info("Activation of channel was called");

        if (this.producerConfigurations != null || this.consumerConfiguration != null) {
            throw new MessageChannelException("Channel already activated");
        }

        try {

            final ConcurrentLinkedBlockingQueue<JmsProducerSessionConfiguration> senderConfigs = new ConcurrentLinkedBlockingQueue<>();
            for (int i = 0; i < jmsConfiguration.availableConnectionsCount() - 1; i++) {
                senderConfigs.offer(new JmsProducerSessionConfiguration(jmsConfiguration));
            }

            this.channelState = new ChannelState();
            this.producerConfigurations = senderConfigs;
            this.consumerConfiguration = new JmsConsumerSessionConfiguration(jmsConfiguration);
            registerMetrics();
        } catch (JMSRuntimeException ex) {
            logger.error("Unable to activate channel", ex);
            throw new MessageChannelException(ex);
        }
    }

    @Override
    public void send(@Nonnull CacheEntryOutputMessage eventOutputMessage) {

        final var senderConfigs = this.producerConfigurations;
        if (senderConfigs == null) {
            throw new MessageChannelException("Channel not activated");
        }

        boolean retry;
        do {
            retry = false;
            JmsProducerSessionConfiguration sessionConfiguration = null;
            try {
                sessionConfiguration = this.metrics.recordExecutionTime(
                        KnownMetrics.PRODUCER_CONNECTION_WAIT_TIME,
                        () -> retrieveConnection(senderConfigs)
                );

                if (sessionConfiguration == null) {
                    // It means that thread was interrupted
                    return;
                }

                sendMessage(eventOutputMessage, sessionConfiguration);
            } catch (JMSRuntimeException ex) {
                recoverProducerSession(ex, sessionConfiguration);
                retry = true;
            } catch (Exception ex) {
                // should never be thrown here
                throw new RuntimeException(ex);
            } finally {
                if (sessionConfiguration != null && !retry) {
                    senderConfigs.offer(sessionConfiguration);
                }
            }
        } while (retry);
    }

    @Override
    public synchronized void subscribe(@Nonnull CacheEventMessageConsumer consumer) {
        if (this.consumerConfiguration == null) {
            throw new MessageChannelException("Channel not activated");
        } else if (this.subscribingTask != null) {
            throw new MessageChannelException("Already subscribed to channel");
        }

        final JmsCacheBusMessageChannelConfiguration sharedConfiguration = this.consumerConfiguration.sharedConfiguration;
        this.subscribingTask = sharedConfiguration.subscribingPool().submit(() -> listenUntilNotClosed(consumer));
    }

    @Override
    public synchronized void close() {
        logger.info("Channel closure was called");

        if (this.producerConfigurations == null || this.consumerConfiguration == null) {
            throw new MessageChannelException("Already in closed state");
        }

        final JmsConsumerSessionConfiguration receiverSessionConfiguration = this.consumerConfiguration;
        this.consumerConfiguration = null;
        receiverSessionConfiguration.close();

        final ConcurrentLinkedBlockingQueue<JmsProducerSessionConfiguration> senderSessionConfigurations = this.producerConfigurations;
        this.producerConfigurations = null;
        senderSessionConfigurations.forEach(JmsSessionConfiguration::close);

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
        if (channelState == null || this.consumerConfiguration == null || this.producerConfigurations == null) {
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

    @Override
    public void setMetrics(@Nonnull CacheBusMetricsRegistry registry) {
        if (this.subscribingTask != null || this.consumerConfiguration != null) {
            throw new MessageChannelException("Not allowed if channel already activated");
        }

        this.metrics = Objects.requireNonNull(registry, "registry");
    }

    private JmsProducerSessionConfiguration retrieveConnection(final ConcurrentLinkedBlockingQueue<JmsProducerSessionConfiguration> sessions) {

        JmsProducerSessionConfiguration session = null;
        try {
            session = sessions.poll(POLL_CHANNEL_TIMEOUT, POLL_CHANNEL_TIMEOUT_UNITS);
            if (session == null) {
                logger.info("Could not retrieve session in %d seconds timeout, maybe you should increase count of available connections in JMS channel configuration?".formatted(POLL_CHANNEL_TIMEOUT));
                session = waitForConnectionUntilAvailable(sessions);
            }
        } catch (InterruptedException ex) {
            handleInterruptionOfThread(ex);
        }

        return session;
    }

    private JmsProducerSessionConfiguration waitForConnectionUntilAvailable(final ConcurrentLinkedBlockingQueue<JmsProducerSessionConfiguration> producerConfigurations) throws InterruptedException {

        final ChannelState channelState = this.channelState;
        if (channelState != null) {
            channelState.increaseCountOfProducersInBusyPollingOfConnections();
        }

        try {
            return producerConfigurations.take();
        } finally {
            if (channelState != null) {
                channelState.decreaseCountOfProducersInBusyPollingOfConnections();
            }
        }
    }

    private void recoverProducerSession(
            final Exception ex,
            final JmsProducerSessionConfiguration sessionConfiguration) {

        final ChannelState channelState = this.channelState;
        if (sessionConfiguration == null || channelState == null) {
            return;
        }

        channelState.increaseCountOfProducersInRecoveryState();
        this.metrics.incrementCounter(KnownMetrics.PRODUCERS_IN_RECOVERY_COUNT);

        final ChannelRecoveryProcessor recoveryProcessor = new ChannelRecoveryProcessor(
                sessionConfiguration::close,
                () -> this.producerConfigurations.offer(new JmsProducerSessionConfiguration(sessionConfiguration.sharedConfiguration)),
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

    private void sendMessage(
            final CacheEntryOutputMessage eventOutputMessage,
            final JmsProducerSessionConfiguration sessionConfiguration) {

        final JMSContext context = sessionConfiguration.session;
        final JMSProducer producer = context.createProducer()
                                                .setJMSType(MESSAGE_TYPE)
                                                .setDisableMessageID(true)
                                                .setDisableMessageTimestamp(true)
                                                .setDeliveryMode(DeliveryMode.PERSISTENT);
        injectProperties(producer, eventOutputMessage, sessionConfiguration);

        final Topic endpoint = sessionConfiguration.endpoint;
        final byte[] body = eventOutputMessage.cacheEntryMessageBody();
        producer.send(endpoint, body);

        logger.debug("Message {} was sent to topic: {}", eventOutputMessage, endpoint);
    }

    private void listenUntilNotClosed(final CacheEventMessageConsumer consumer) {

        logger.info("Subscribe was called");

        JmsConsumerSessionConfiguration sessionConfiguration;
        while ((sessionConfiguration = this.consumerConfiguration) != null) {

            try {
                final Message message = sessionConfiguration.jmsConsumer.receive(0);
                if (!(message instanceof final BytesMessage bytesMessage)) {
                    continue;
                }

                final byte[] messageBody = bytesMessage.getBody(byte[].class);
                final int messageHash = bytesMessage.getIntProperty(HASH_KEY_PROPERTY);

                // the error will never happen, so we can use auto ack
                consumer.accept(messageHash, messageBody);

            } catch (JMSRuntimeException | JMSException ex) {
                recoverConsumerSession(ex);
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

    private void recoverConsumerSession(final Exception ex) {

        // Thread was interrupted
        final JmsConsumerSessionConfiguration sessionConfiguration = this.consumerConfiguration;
        final ChannelState channelState = this.channelState;
        if (sessionConfiguration == null || channelState == null) {
            return;
        }

        channelState.increaseCountOfConsumersInRecoveryState();
        this.metrics.incrementCounter(KnownMetrics.CONSUMERS_IN_RECOVERY_COUNT);

        final ChannelRecoveryProcessor recoveryProcessor = new ChannelRecoveryProcessor(
                sessionConfiguration::close,
                () -> this.consumerConfiguration = new JmsConsumerSessionConfiguration(sessionConfiguration.sharedConfiguration),
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

    private void injectProperties(
            final JMSProducer producer,
            final CacheEntryOutputMessage outputMessage,
            final JmsProducerSessionConfiguration senderSessionConfiguration) {
        producer.setProperty(HOST_PROPERTY, senderSessionConfiguration.hostName);
        producer.setProperty(HASH_KEY_PROPERTY, outputMessage.messageHashKey());
    }

    private void handleInterruptionOfThread(final InterruptedException ex) {
        logger.info("Thread was interrupted", ex);

        final ChannelState channelState = this.channelState;
        if (channelState != null) {
            channelState.increaseCountOfInterruptedThreads();
        }

        Thread.currentThread().interrupt();
    }

    private static abstract class JmsSessionConfiguration implements AutoCloseable {

        protected final JmsCacheBusMessageChannelConfiguration sharedConfiguration;
        protected final String hostName;
        protected final Topic endpoint;
        protected final JMSContext session;

        private JmsSessionConfiguration(final JmsCacheBusMessageChannelConfiguration sharedConfiguration) {
            this.sharedConfiguration = sharedConfiguration;
            this.hostName = sharedConfiguration.hostNameResolver().resolve();
            this.session = sharedConfiguration.connectionFactory().createContext();
            this.endpoint = this.session.createTopic(sharedConfiguration.channel());
        }

        @Override
        public void close() {
            this.session.close();
        }
    }

    private static class JmsConsumerSessionConfiguration extends JmsSessionConfiguration {

        private final JMSConsumer jmsConsumer;

        private JmsConsumerSessionConfiguration(@Nonnull JmsCacheBusMessageChannelConfiguration configuration) {
            super(configuration);
            this.jmsConsumer = this.session.createConsumer(this.endpoint, createMessageSelector());
        }

        private String createMessageSelector() {
            return "JMSType='" + MESSAGE_TYPE + "' AND " + HOST_PROPERTY + "<>'" + this.hostName + "'";
        }
    }

    private static class JmsProducerSessionConfiguration extends JmsSessionConfiguration {

        private JmsProducerSessionConfiguration(@Nonnull JmsCacheBusMessageChannelConfiguration sharedConfiguration) {
            super(sharedConfiguration);
        }
    }
}
