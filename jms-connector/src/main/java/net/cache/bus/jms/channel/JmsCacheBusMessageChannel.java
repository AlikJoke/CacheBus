package net.cache.bus.jms.channel;

import net.cache.bus.core.transport.CacheBusMessageChannel;
import net.cache.bus.core.transport.CacheEntryOutputMessage;
import net.cache.bus.jms.configuration.CacheBusJmsMessageChannelConfiguration;
import net.cache.bus.transport.addons.ChannelRecoveryProcessor;

import javax.annotation.Nonnull;
import javax.jms.*;
import java.lang.IllegalStateException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class JmsCacheBusMessageChannel implements CacheBusMessageChannel<CacheBusJmsMessageChannelConfiguration> {

    static final String MESSAGE_TYPE = "CacheEvent";
    static final String HOST_PROPERTY = "host";
    static final String CACHE_PROPERTY = "cache";
    static final String EVENT_TYPE_PROPERTY = "eventType";

    static final Logger logger = Logger.getLogger(JmsCacheBusMessageChannel.class.getCanonicalName());

    private volatile CacheBusJmsMessageChannelConfiguration configuration;
    private volatile JmsSessionConfiguration jmsSessionConfiguration;

    @Override
    public synchronized void activate(@Nonnull CacheBusJmsMessageChannelConfiguration jmsConfiguration) {
        this.configuration = jmsConfiguration;
        this.jmsSessionConfiguration = new JmsSessionConfiguration(jmsConfiguration);
    }

    @Override
    public void send(@Nonnull CacheEntryOutputMessage eventOutputMessage) {

        if (this.jmsSessionConfiguration == null) {
            return;
        }

        try (final JMSContext context = this.jmsSessionConfiguration.connectionFactory.createContext()) {
            final JMSProducer producer = context.createProducer()
                                                    .setJMSType(MESSAGE_TYPE)
                                                    .setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            injectProperties(producer, eventOutputMessage);

            producer.send(this.jmsSessionConfiguration.endpoint, eventOutputMessage.cacheEntryMessageBody());

            logger.fine(() -> "Message %s was sent to topic: %s".formatted(eventOutputMessage, this.jmsSessionConfiguration.endpoint));
        }
    }

    @Override
    public void subscribe(@Nonnull Consumer<byte[]> consumer) {
        this.jmsSessionConfiguration.receivingPool.submit(() -> listenUntilNotClosed(consumer));
    }

    @Override
    public synchronized void unsubscribe() {
        if (this.jmsSessionConfiguration == null) {
            throw new IllegalStateException("Already unsubscribed");
        }

        this.jmsSessionConfiguration.close();
        this.jmsSessionConfiguration = null;
    }

    private void listenUntilNotClosed(final Consumer<byte[]> consumer) {

        while (this.jmsSessionConfiguration != null) {

            try {
                final byte[] messageBody = this.jmsSessionConfiguration.jmsConsumer.receiveBody(byte[].class, 0);

                if (messageBody == null) {
                    continue;
                }
                // the error will never happen, so we can use auto ack
                if (this.configuration.preserveOrder()) {
                    consumer.accept(messageBody);
                } else {
                    this.jmsSessionConfiguration.receivingPool.submit(() -> consumer.accept(messageBody));
                }

            } catch (JMSRuntimeException ex) {
                onException(ex);
            }
        }
    }

    private void onException(final JMSRuntimeException ex) {

        final ChannelRecoveryProcessor recoveryProcessor = new ChannelRecoveryProcessor(
                this.jmsSessionConfiguration::close,
                () -> this.activate(this.configuration),
                Integer.MAX_VALUE
        );

        recoveryProcessor.recover(ex);
    }

    private void injectProperties(final JMSProducer producer, final CacheEntryOutputMessage outputMessage) {
        producer.setProperty(HOST_PROPERTY, this.jmsSessionConfiguration.hostName);
        producer.setProperty(CACHE_PROPERTY, outputMessage.cacheName());
        producer.setProperty(EVENT_TYPE_PROPERTY, outputMessage.eventType().name());
    }

    private static class JmsSessionConfiguration implements AutoCloseable {

        private final ConnectionFactory connectionFactory;
        private final String hostName;
        private final JMSContext receivingSession;
        private final JMSConsumer jmsConsumer;
        private final Topic endpoint;
        private final ExecutorService receivingPool;

        private JmsSessionConfiguration(final CacheBusJmsMessageChannelConfiguration configuration) {
            this.connectionFactory = configuration.connectionFactory();
            this.hostName = configuration.hostNameResolver().resolve();
            this.receivingSession = this.connectionFactory.createContext();
            this.endpoint = this.receivingSession.createTopic(configuration.channel());
            this.jmsConsumer = this.receivingSession.createConsumer(this.endpoint, createMessageSelector());
            this.receivingPool = configuration.receivingPool();
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
