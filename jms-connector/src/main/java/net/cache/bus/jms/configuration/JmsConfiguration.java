package net.cache.bus.jms.configuration;

import javax.annotation.Nonnull;
import javax.jms.ConnectionFactory;
import java.util.Objects;

public record JmsConfiguration(@Nonnull ConnectionFactory connectionFactory) {

    public static final String MESSAGE_TYPE = "CacheEvent";
    public static final String HOST_PROPERTY = "host";
    public static final String CACHE_PROPERTY = "cache";
    public static final String EVENT_TYPE_PROPERTY = "eventType";

    public JmsConfiguration {
        Objects.requireNonNull(connectionFactory, "connectionFactory");
    }

    @Override
    @Nonnull
    public ConnectionFactory connectionFactory() {
        return this.connectionFactory;
    }
}
