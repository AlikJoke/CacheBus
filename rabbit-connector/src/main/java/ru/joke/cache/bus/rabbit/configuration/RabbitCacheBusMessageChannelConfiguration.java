package ru.joke.cache.bus.rabbit.configuration;

import com.rabbitmq.client.ConnectionFactory;
import ru.joke.cache.bus.core.configuration.CacheBusMessageChannelConfiguration;
import ru.joke.cache.bus.core.configuration.ConfigurationException;
import ru.joke.cache.bus.core.impl.resolvers.StdHostNameResolver;
import ru.joke.cache.bus.core.transport.HostNameResolver;
import ru.joke.cache.bus.transport.ChannelConstants;
import ru.joke.cache.bus.rabbit.channel.RabbitCacheBusMessageChannel;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.Objects;

import static ru.joke.cache.bus.transport.ChannelConstants.RECONNECT_RETRY_TIMEOUT;
import static ru.joke.cache.bus.transport.ChannelConstants.RECONNECT_RETRY_TIMEOUT_UNITS;

/**
 * Configuration for a JMS-compatible message channel.
 *
 * @param connectionFactory      the connection factory to the broker, cannot be {@code null}.
 * @param availableChannelsCount the maximum number of channels with RabbitMQ
 *                               that can be used over the broker connection;
 *                               the value should not be low, as a low value will result in high
 *                               competition for the connection when sending data; cannot be
 *                               less than {@code 2}; the default value is 9
 *                               (8 connections for sending messages, i.e., 8 threads can
 *                               simultaneously send data without competing for the connection
 *                               + 1 connection for receiving messages from other servers).
 * @param reconnectTimeoutMs     the reconnection timeout in milliseconds when the connections used
 *                               for sending messages in the channel are disconnected;
 *                               after the timeout, the attempt to reconnect will be stopped and
 *                               an exception will be thrown; the default value is
 *                               {@link ChannelConstants#RECONNECT_RETRY_TIMEOUT} in minutes;
 *                               cannot be negative.
 * @param channel                the channel name, cannot be {@code null}.
 * @param hostNameResolver       the resolver of the current server's host, cannot be {@code null}.
 * @author Alik
 * @see Builder
 * @see RabbitCacheBusMessageChannel
 */
public record RabbitCacheBusMessageChannelConfiguration(
        @Nonnull ConnectionFactory connectionFactory,
        @Nonnegative int availableChannelsCount,
        @Nonnegative long reconnectTimeoutMs,
        @Nonnull String channel,
        @Nonnull HostNameResolver hostNameResolver) implements CacheBusMessageChannelConfiguration {

    /**
     * The minimum required number of RabbitMQ channels (1 for sending + 1 for reading from RabbitMQ).
     */
    private static final int MIN_CHANNELS_COUNT = 2;
    /**
     * The number of channels within the RabbitMQ connection by default (8 for sending to RabbitMQ + 1 for reading from RabbitMQ).
     */
    private static final int DEFAULT_CHANNELS_COUNT = 9;

    public RabbitCacheBusMessageChannelConfiguration {
        Objects.requireNonNull(connectionFactory, "connectionFactory");
        Objects.requireNonNull(hostNameResolver, "hostNameResolver");

        if (channel == null || channel.isEmpty()) {
            throw new ConfigurationException("Channel must be not empty");
        }

        if (availableChannelsCount < MIN_CHANNELS_COUNT) {
            throw new ConfigurationException("Available channels count must be >= " + MIN_CHANNELS_COUNT);
        }

        if (reconnectTimeoutMs < 0) {
            throw new ConfigurationException("Reconnect timeout must be positive: " + reconnectTimeoutMs);
        }
    }

    public RabbitCacheBusMessageChannelConfiguration(
            @Nonnull ConnectionFactory connectionFactory,
            @Nonnull String channel,
            @Nonnull HostNameResolver hostNameResolver) {
        this(connectionFactory, DEFAULT_CHANNELS_COUNT, RECONNECT_RETRY_TIMEOUT_UNITS.toMillis(RECONNECT_RETRY_TIMEOUT), channel, hostNameResolver);
    }

    /**
     * Factory method for creating a builder to construct the configuration for the JMS bus channel.
     *
     * @return cannot be {@code null}.
     */
    @Nonnull
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private ConnectionFactory connectionFactory;
        private int availableChannelsCount = DEFAULT_CHANNELS_COUNT;
        private long reconnectTimeoutMs = RECONNECT_RETRY_TIMEOUT_UNITS.toMillis(RECONNECT_RETRY_TIMEOUT);
        private String channel;
        private HostNameResolver hostNameResolver = new StdHostNameResolver();

        /**
         * Sets the connection factory through which interaction with the broker is performed.
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
         * Sets the maximum number of connections allowed for the cache bus message channel.<br>
         * See the parameter description in the JavaDoc for {@link RabbitCacheBusMessageChannelConfiguration}.<br>
         * If not explicitly set, the default value {@link RabbitCacheBusMessageChannelConfiguration#DEFAULT_CHANNELS_COUNT} will be used.
         *
         * @param availableChannelsCount the maximum number of connections allowed for the channel, cannot be {@code < 2}.
         * @return cannot be {@code null}.
         */
        @Nonnull
        public Builder setAvailableChannelsCount(@Nonnegative int availableChannelsCount) {
            this.availableChannelsCount = availableChannelsCount;
            return this;
        }

        /**
         * Sets the timeout in milliseconds for reconnecting in case of connection failure.<br>
         * See the parameter description in the JavaDoc for {@link CacheBusMessageChannelConfiguration#reconnectTimeoutMs()}.<br>
         * If not explicitly set, the default value {@link ChannelConstants#RECONNECT_RETRY_TIMEOUT} in units
         * {@link ChannelConstants#RECONNECT_RETRY_TIMEOUT_UNITS} will be used.
         *
         * @param reconnectTimeoutMs the timeout for reconnecting in case of connection failure,
         *                           cannot be negative.
         * @return cannot be {@code null}.
         */
        @Nonnull
        public Builder setReconnectTimeoutMs(@Nonnegative long reconnectTimeoutMs) {
            this.reconnectTimeoutMs = reconnectTimeoutMs;
            return this;
        }

        /**
         * Sets the name of the channel from which messages are retrieved and to which they are sent.
         *
         * @param channel the Rabbit channel name, cannot be {@code null}.
         * @return cannot be {@code null}.
         */
        @Nonnull
        public Builder setChannel(@Nonnull String channel) {
            this.channel = channel;
            return this;
        }

        /**
         * Sets the host name resolver. If not specified, the default implementation {@link StdHostNameResolver} is used.
         *
         * @param hostNameResolver the host name resolver, cannot be {@code null} if the method is called and not using the default implementation.
         * @return cannot be {@code null}
         */
        @Nonnull
        public Builder setHostNameResolver(@Nonnull HostNameResolver hostNameResolver) {
            this.hostNameResolver = hostNameResolver;
            return this;
        }

        /**
         * Constructs a {@link RabbitCacheBusMessageChannelConfiguration} object based on the data provided during object construction.
         *
         * @return cannot be {@code null}.
         * @see RabbitCacheBusMessageChannelConfiguration
         */
        @Nonnull
        public RabbitCacheBusMessageChannelConfiguration build() {
            return new RabbitCacheBusMessageChannelConfiguration(
                    this.connectionFactory,
                    this.availableChannelsCount,
                    this.reconnectTimeoutMs,
                    this.channel,
                    this.hostNameResolver
            );
        }
    }
}
