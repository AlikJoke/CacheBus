package net.cache.bus.jms.configuration;

import net.cache.bus.core.configuration.CacheBusMessageChannelConfiguration;
import net.cache.bus.core.configuration.ConfigurationException;
import net.cache.bus.core.impl.resolvers.StdHostNameResolver;
import net.cache.bus.core.transport.HostNameResolver;
import net.cache.bus.transport.ChannelConstants;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.jms.ConnectionFactory;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import static net.cache.bus.transport.ChannelConstants.RECONNECT_RETRY_TIMEOUT;
import static net.cache.bus.transport.ChannelConstants.RECONNECT_RETRY_TIMEOUT_UNITS;

/**
 * Configuration for a JMS-compatible message channel.
 *
 * @param connectionFactory         the connection factory connect to the broker, cannot be {@code null}.
 * @param availableConnectionsCount the maximum number of connections to the JMS broker
 *                                  that can be used by the channel; the value should not be too low,
 *                                  as a low value will result in high competition for connections when sending data;
 *                                  cannot be less than {@code 2}; the default value is 9
 *                                  (8 connections for sending messages, i.e., 8 threads can
 *                                  simultaneously send data without competing for a connection
 *                                  + 1 connection for receiving messages from other servers).
 * @param reconnectTimeoutMs        the reconnection timeout in milliseconds when connections used for sending messages
 *                                  in the channel are disconnected; after the timeout,
 *                                  the reconnection attempt will be stopped and an exception will be thrown;
 *                                  the default value is {@link ChannelConstants#RECONNECT_RETRY_TIMEOUT} in minutes;
 *                                  cannot be negative.
 * @param channel                   the name of the channel (in JMS terms - topic), cannot be {@code null}.
 * @param subscribingPool           the thread pool on which messages are received from the channel, cannot be {@code null}.
 * @param hostNameResolver          the resolver for the current server's host, cannot be {@code null}.
 * @author Alik
 * @see Builder
 * @see net.cache.bus.jms.channel.JmsCacheBusMessageChannel
 */
public record JmsCacheBusMessageChannelConfiguration(
        @Nonnull ConnectionFactory connectionFactory,
        @Nonnegative int availableConnectionsCount,
        @Nonnegative long reconnectTimeoutMs,
        @Nonnull String channel,
        @Nonnull ExecutorService subscribingPool,
        @Nonnull HostNameResolver hostNameResolver) implements CacheBusMessageChannelConfiguration {

    /**
     * The minimum required number of connections the channel (1 for sending + 1 for reading from the channel).
     */
    private static final int MIN_CONNECTIONS_COUNT = 2;
    /**
     * The default number of connections to the channel (8 for sending to the channel + 1 for reading from the channel).
     */
    private static final int DEFAULT_CONNECTIONS_COUNT = 9;

    public JmsCacheBusMessageChannelConfiguration {
        Objects.requireNonNull(connectionFactory, "connectionFactory");
        Objects.requireNonNull(subscribingPool, "subscribingPool");
        Objects.requireNonNull(hostNameResolver, "hostNameResolver");

        if (channel == null || channel.isEmpty()) {
            throw new ConfigurationException("Channel must be not empty");
        }

        if (availableConnectionsCount < MIN_CONNECTIONS_COUNT) {
            throw new ConfigurationException("Available connections count must be >= " + MIN_CONNECTIONS_COUNT);
        }

        if (reconnectTimeoutMs < 0) {
            throw new ConfigurationException("Reconnect timeout must be positive: " + reconnectTimeoutMs);
        }
    }

    public JmsCacheBusMessageChannelConfiguration(
            @Nonnull ConnectionFactory connectionFactory,
            @Nonnull String channel,
            @Nonnull ExecutorService subscribingPool,
            @Nonnull HostNameResolver hostNameResolver) {
        this(
                connectionFactory,
                DEFAULT_CONNECTIONS_COUNT, RECONNECT_RETRY_TIMEOUT_UNITS.toMillis(RECONNECT_RETRY_TIMEOUT),
                channel,
                subscribingPool,
                hostNameResolver
        );
    }

    /**
     * Factory method for creating a builder to construct the configuration for a JMS bus channel.
     *
     * @return cannot be {@code null}.
     */
    @Nonnull
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private ConnectionFactory connectionFactory;
        private int availableConnectionsCount = DEFAULT_CONNECTIONS_COUNT;
        private long reconnectTimeoutMs = RECONNECT_RETRY_TIMEOUT_UNITS.toMillis(RECONNECT_RETRY_TIMEOUT);
        private String channel;
        private ExecutorService subscribingPool;
        private HostNameResolver hostNameResolver = new StdHostNameResolver();

        /**
         * Sets the connection factory used for interacting with the broker.
         *
         * @param connectionFactory the connection factory, cannot be {@code null}.
         * @return cannot be {@code null}.
         */
        @Nonnull
        public Builder setConnectionFactory(@Nonnull ConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
            return this;
        }

        /**
         * Sets the maximum number of connections allowed to be used by the channel for caching.<br>
         * See the parameter description in the JavaDoc for {@link JmsCacheBusMessageChannelConfiguration}.<br>
         * If not set explicitly, the default value {@link JmsCacheBusMessageChannelConfiguration#DEFAULT_CONNECTIONS_COUNT} will be used.
         *
         * @param availableConnectionsCount the maximum number of connections allowed to be used by the channel,
         *                                  cannot be {@code < 2}.
         * @return cannot be {@code null}.
         */
        @Nonnull
        public Builder setAvailableConnectionsCount(@Nonnegative int availableConnectionsCount) {
            this.availableConnectionsCount = availableConnectionsCount;
            return this;
        }

        /**
         * Sets the reconnection timeout in milliseconds when a connection is disconnected.<br>
         * See the parameter description the JavaDoc for {@link JmsCacheBusMessageChannelConfiguration}.<br>
         * If not set explicitly, the default value {@link ChannelConstants#RECONNECT_RETRY_TIMEOUT} units
         * {@link ChannelConstants#RECONNECT_RETRY_TIMEOUT_UNITS} will be used.
         *
         * @param reconnectTimeoutMs the reconnection timeout when a connection is disconnected, cannot be negative.
         * @return cannot be {@code null}.
         */
        @Nonnull
        public Builder setReconnectTimeoutMs(@Nonnegative long reconnectTimeoutMs) {
            this.reconnectTimeoutMs = reconnectTimeoutMs;
            return this;
        }

        /**
         * Sets the name of the JMS topic from which messages are retrieved and to which messages are sent.
         *
         * @param channel the name of the JMS topic, cannot be {@code null}.
         * @return cannot be {@code null}.
         */
        @Nonnull
        public Builder setChannel(@Nonnull String channel) {
            this.channel = channel;
            return this;
        }

        /**
         * Sets the thread pool on which message retrieval from the channel should occur.
         *
         * @param subscribingPool the thread pool, cannot be {@code null}.
         * @return cannot be {@code null}.
         */
        @Nonnull
        public Builder setSubscribingPool(@Nonnull ExecutorService subscribingPool) {
            this.subscribingPool = subscribingPool;
            return this;
        }

        /**
         * Sets the host resolver. If not specified, the default implementation {@link StdHostNameResolver} will be used.
         *
         * @param hostNameResolver the host resolver, cannot be {@code null} when the method is called and not using the default implementation.
         * @return cannot be {@code null}.
         */
        @Nonnull
        public Builder setHostNameResolver(@Nonnull HostNameResolver hostNameResolver) {
            this.hostNameResolver = hostNameResolver;
            return this;
        }

        /**
         * Constructs a {@link JmsCacheBusMessageChannelConfiguration} object based on the data provided during the object's construction.
         *
         * @return cannot be {@code null}.
         * @see JmsCacheBusMessageChannelConfiguration
         */
        @Nonnull
        public JmsCacheBusMessageChannelConfiguration build() {
            return new JmsCacheBusMessageChannelConfiguration(
                    this.connectionFactory,
                    this.availableConnectionsCount,
                    this.reconnectTimeoutMs,
                    this.channel,
                    this.subscribingPool,
                    this.hostNameResolver
            );
        }
    }
}
