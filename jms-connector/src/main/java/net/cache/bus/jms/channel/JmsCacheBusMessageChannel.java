package net.cache.bus.jms.channel;

import net.cache.bus.core.CacheEventMessageConsumer;
import net.cache.bus.core.LifecycleException;
import net.cache.bus.core.transport.CacheBusMessageChannel;
import net.cache.bus.core.transport.CacheEntryOutputMessage;
import net.cache.bus.jms.configuration.JmsCacheBusMessageChannelConfiguration;
import net.cache.bus.transport.addons.ChannelRecoveryProcessor;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.jms.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * Реализация канала сообщений на основе JMS.
 *
 * @author Alik
 * @see JmsCacheBusMessageChannelConfiguration
 * @see CacheBusMessageChannel
 */
@ThreadSafe
public final class JmsCacheBusMessageChannel implements CacheBusMessageChannel<JmsCacheBusMessageChannelConfiguration> {

    static final String MESSAGE_TYPE = "CacheEvent";
    static final String HOST_PROPERTY = "host";
    static final String HASH_KEY_PROPERTY = "hash";

    static final Logger logger = Logger.getLogger(JmsCacheBusMessageChannel.class.getCanonicalName());

    private volatile JmsCacheBusMessageChannelConfiguration configuration;
    private volatile JmsSessionConfiguration jmsSessionConfiguration;
    private Future<?> subscribingTask;

    @Override
    public synchronized void activate(@Nonnull JmsCacheBusMessageChannelConfiguration jmsConfiguration) {
        logger.info(() -> "Activation of channel was called");

        this.configuration = jmsConfiguration;
        this.jmsSessionConfiguration = new JmsSessionConfiguration(jmsConfiguration);
    }

    @Override
    public void send(@Nonnull CacheEntryOutputMessage eventOutputMessage) {

        if (this.jmsSessionConfiguration == null) {
            throw new LifecycleException("Channel not activated");
        }

        try (final JMSContext context = this.jmsSessionConfiguration.connectionFactory.createContext()) {
            final JMSProducer producer = context.createProducer()
                                                    .setJMSType(MESSAGE_TYPE)
                                                    .setDisableMessageID(true)
                                                    .setDisableMessageTimestamp(true)
                                                    .setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            injectProperties(producer, eventOutputMessage);

            producer.send(this.jmsSessionConfiguration.endpoint, eventOutputMessage.cacheEntryMessageBody());

            logger.fine(() -> "Message %s was sent to topic: %s".formatted(eventOutputMessage, this.jmsSessionConfiguration.endpoint));
        }
    }

    @Override
    public synchronized void subscribe(@Nonnull CacheEventMessageConsumer consumer) {
        if (this.configuration == null) {
            throw new LifecycleException("Channel not activated");
        } else if (this.subscribingTask != null) {
            throw new LifecycleException("Already subscribed to channel");
        }

        this.subscribingTask = this.jmsSessionConfiguration.subscribingPool.submit(() -> listenUntilNotClosed(consumer));
    }

    @Override
    public synchronized void unsubscribe() {
        logger.info(() -> "Unsubscribe was called");

        if (this.jmsSessionConfiguration == null) {
            throw new LifecycleException("Already in unsubscribed state");
        }

        this.jmsSessionConfiguration.close();
        this.jmsSessionConfiguration = null;

        if (this.subscribingTask != null) {
            this.subscribingTask.cancel(true);
            this.subscribingTask = null;
        }
    }

    private void listenUntilNotClosed(final CacheEventMessageConsumer consumer) {

        logger.info(() -> "Subscribe was called");

        JmsSessionConfiguration jmsSessionConfiguration;
        while ((jmsSessionConfiguration = this.jmsSessionConfiguration) != null) {

            try {
                final Message message = jmsSessionConfiguration.jmsConsumer.receive(0);
                if (!(message instanceof final BytesMessage bytesMessage)) {
                    continue;
                }

                final byte[] messageBody = bytesMessage.getBody(byte[].class);
                final int messageHash = bytesMessage.getIntProperty(HASH_KEY_PROPERTY);

                // the error will never happen, so we can use auto ack
                consumer.accept(messageHash, messageBody);

            } catch (JMSRuntimeException | JMSException ex) {
                onException(ex);
            }
        }
    }

    private synchronized void onException(final Exception ex) {

        // Поток прервали
        JmsSessionConfiguration jmsSessionConfiguration = this.jmsSessionConfiguration;
        if (jmsSessionConfiguration == null) {
            return;
        }

        final ChannelRecoveryProcessor recoveryProcessor = new ChannelRecoveryProcessor(
                jmsSessionConfiguration::close,
                () -> this.activate(this.configuration),
                Integer.MAX_VALUE
        );

        recoveryProcessor.recover(ex);
    }

    private void injectProperties(final JMSProducer producer, final CacheEntryOutputMessage outputMessage) {
        producer.setProperty(HOST_PROPERTY, this.jmsSessionConfiguration.hostName);
        producer.setProperty(HASH_KEY_PROPERTY, outputMessage.messageHashKey());
    }

    private static class JmsSessionConfiguration implements AutoCloseable {

        private final ConnectionFactory connectionFactory;
        private final String hostName;
        private final JMSContext receivingSession;
        private final JMSConsumer jmsConsumer;
        private final Topic endpoint;
        private final ExecutorService subscribingPool;

        private JmsSessionConfiguration(final JmsCacheBusMessageChannelConfiguration configuration) {
            this.connectionFactory = configuration.connectionFactory();
            this.hostName = configuration.hostNameResolver().resolve();
            this.receivingSession = this.connectionFactory.createContext();
            this.endpoint = this.receivingSession.createTopic(configuration.channel());
            this.jmsConsumer = this.receivingSession.createConsumer(this.endpoint, createMessageSelector());
            this.subscribingPool = configuration.subscribingPool();
        }

        private String createMessageSelector() {
            return "JMSType='" + MESSAGE_TYPE + "' AND " + HOST_PROPERTY + "<>'" + this.hostName + "'";
        }

        @Override
        public void close() {
            this.receivingSession.close();
        }
    }
}
