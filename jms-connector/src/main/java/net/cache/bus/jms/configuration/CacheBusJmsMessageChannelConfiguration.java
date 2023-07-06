package net.cache.bus.jms.configuration;

import net.cache.bus.core.configuration.CacheBusMessageChannelConfiguration;
import net.cache.bus.core.impl.StdHostNameResolver;
import net.cache.bus.core.transport.HostNameResolver;

import javax.annotation.Nonnull;
import javax.jms.ConnectionFactory;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

public record CacheBusJmsMessageChannelConfiguration(
        @Nonnull ConnectionFactory connectionFactory,
        @Nonnull String channel,
        @Nonnull ExecutorService receivingPool,
        @Nonnull HostNameResolver hostNameResolver,
        boolean preserveOrder) implements CacheBusMessageChannelConfiguration {

    public CacheBusJmsMessageChannelConfiguration {
        Objects.requireNonNull(connectionFactory, "connectionFactory");
        Objects.requireNonNull(receivingPool, "receivingPool");
        Objects.requireNonNull(hostNameResolver, "hostNameResolver");

        if (channel == null || channel.isEmpty()) {
            throw new IllegalArgumentException("Channel must be not empty");
        }
    }

    @Override
    public boolean preserveOrder() {
        return this.preserveOrder;
    }

    public static class Builder {

        private ConnectionFactory connectionFactory;
        private String channel;
        private ExecutorService receivingPool;
        private boolean preserveOrder;
        private HostNameResolver hostNameResolver = new StdHostNameResolver();

        public Builder setConnectionFactory(@Nonnull ConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
            return this;
        }

        public Builder setChannel(@Nonnull String channel) {
            this.channel = channel;
            return this;
        }

        public Builder setReceivingPool(@Nonnull ExecutorService receivingPool) {
            this.receivingPool = receivingPool;
            return this;
        }

        public Builder setHostNameResolver(@Nonnull HostNameResolver hostNameResolver) {
            this.hostNameResolver = hostNameResolver;
            return this;
        }

        public Builder setPreserveOrder(boolean preserveOrder) {
            this.preserveOrder = preserveOrder;
            return this;
        }

        @Nonnull
        public CacheBusMessageChannelConfiguration build() {
            return new CacheBusJmsMessageChannelConfiguration(
                    this.connectionFactory,
                    this.channel,
                    this.receivingPool,
                    this.hostNameResolver,
                    this.preserveOrder
            );
        }
    }
}
