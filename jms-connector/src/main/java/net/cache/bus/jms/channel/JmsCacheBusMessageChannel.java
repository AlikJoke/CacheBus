package net.cache.bus.jms.channel;

import net.cache.bus.core.CacheEventMessageConsumer;
import net.cache.bus.core.impl.ImmutableComponentState;
import net.cache.bus.core.state.ComponentState;
import net.cache.bus.core.transport.CacheBusMessageChannel;
import net.cache.bus.core.transport.CacheEntryOutputMessage;
import net.cache.bus.core.transport.MessageChannelException;
import net.cache.bus.jms.configuration.JmsCacheBusMessageChannelConfiguration;
import net.cache.bus.transport.addons.ChannelRecoveryProcessor;
import net.cache.bus.transport.addons.ChannelState;
import net.cache.bus.transport.addons.ConcurrentLinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.jms.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import static net.cache.bus.transport.ChannelConstants.*;

/**
 * Реализация канала сообщений на основе JMS.
 *
 * @author Alik
 * @see JmsCacheBusMessageChannelConfiguration
 * @see CacheBusMessageChannel
 */
@ThreadSafe
public final class JmsCacheBusMessageChannel implements CacheBusMessageChannel<JmsCacheBusMessageChannelConfiguration> {

    static final String HOST_PROPERTY = "host";
    static final String HASH_KEY_PROPERTY = "hash";

    private static final String CHANNEL_ID = "jms-channel";

    private static final Logger logger = LoggerFactory.getLogger(JmsCacheBusMessageChannel.class);

    private volatile ChannelState channelState;
    private volatile JmsConsumerSessionConfiguration consumerConfiguration;
    private volatile ConcurrentLinkedBlockingQueue<JmsProducerSessionConfiguration> producerConfigurations;

    private Future<?> subscribingTask;

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
                sessionConfiguration = senderConfigs.poll(POLL_CHANNEL_TIMEOUT, POLL_CHANNEL_TIMEOUT_UNITS);
                if (sessionConfiguration == null) {
                    logger.info("Could not retrieve session in %d seconds timeout, maybe you should increase count of available connections in JMS channel configuration?".formatted(POLL_CHANNEL_TIMEOUT));
                    sessionConfiguration = waitForConnectionUntilAvailable(senderConfigs);
                }

                sendMessage(eventOutputMessage, sessionConfiguration);
            } catch (JMSRuntimeException ex) {
                recoverProducerSession(ex, sessionConfiguration);
                retry = true;
            } catch (InterruptedException ex) {
                handleInterruptionOfThread(ex);
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

        final ChannelRecoveryProcessor recoveryProcessor = new ChannelRecoveryProcessor(
                sessionConfiguration::close,
                () -> this.producerConfigurations.offer(new JmsProducerSessionConfiguration(sessionConfiguration.sharedConfiguration)),
                sessionConfiguration.sharedConfiguration.reconnectTimeoutMs()
        );

        try {
            recoveryProcessor.recover(ex);
        } catch (RuntimeException e) {
            channelState.increaseCountOfUnrecoverableProducers();
            throw new MessageChannelException(e);
        } finally {
            channelState.decreaseCountProducersInRecoveryState();
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
        producer.send(endpoint, eventOutputMessage.cacheEntryMessageBody());

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

    private void recoverConsumerSession(final Exception ex) {

        // Поток прервали
        final JmsConsumerSessionConfiguration sessionConfiguration = this.consumerConfiguration;
        final ChannelState channelState = this.channelState;
        if (sessionConfiguration == null || channelState == null) {
            return;
        }

        channelState.increaseCountOfConsumersInRecoveryState();

        final ChannelRecoveryProcessor recoveryProcessor = new ChannelRecoveryProcessor(
                sessionConfiguration::close,
                () -> this.consumerConfiguration = new JmsConsumerSessionConfiguration(sessionConfiguration.sharedConfiguration),
                Integer.MAX_VALUE
        );

        try {
            recoveryProcessor.recover(ex);
        } catch (RuntimeException e) {
            channelState.increaseCountOfUnrecoverableConsumers();
            throw new MessageChannelException(e);
        } finally {
            channelState.decreaseCountConsumersInRecoveryState();
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
