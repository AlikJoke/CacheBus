package net.cache.bus.jms.output;

import net.cache.bus.core.transport.CacheEntryEventMessageSender;
import net.cache.bus.core.transport.CacheEntryOutputMessage;
import net.cache.bus.jms.configuration.JmsConfiguration;

import javax.annotation.Nonnull;
import javax.jms.*;
import java.util.logging.Logger;

import static net.cache.bus.jms.configuration.JmsConfiguration.*;

public final class JmsCacheEntryEventMessageSender implements CacheEntryEventMessageSender {

    private static final Logger logger = Logger.getLogger(JmsCacheEntryEventMessageSender.class.getCanonicalName());

    private final ConnectionFactory connectionFactory;

    public JmsCacheEntryEventMessageSender(@Nonnull final JmsConfiguration jmsConfiguration) {
        this.connectionFactory = jmsConfiguration.connectionFactory();
    }

    @Override
    public void send(@Nonnull CacheEntryOutputMessage eventOutputMessage, @Nonnull String targetEndpoint) {

        try (final JMSContext context = this.connectionFactory.createContext()) {
            final JMSProducer producer = context.createProducer()
                                                    .setJMSType(MESSAGE_TYPE)
                                                    .setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            injectProperties(producer, eventOutputMessage);

            final Topic targetTopic = context.createTopic(targetEndpoint);
            producer.send(targetTopic, eventOutputMessage.cacheEntryMessageBody());

            logger.fine(() -> "Message %s was sent to topic: %s".formatted(eventOutputMessage, targetEndpoint));
        }
    }

    private void injectProperties(final JMSProducer producer, final CacheEntryOutputMessage outputMessage) {
        producer.setProperty(HOST_PROPERTY, outputMessage.hostName());
        producer.setProperty(CACHE_PROPERTY, outputMessage.cacheName());
        producer.setProperty(EVENT_TYPE_PROPERTY, outputMessage.eventType().name());
    }
}
