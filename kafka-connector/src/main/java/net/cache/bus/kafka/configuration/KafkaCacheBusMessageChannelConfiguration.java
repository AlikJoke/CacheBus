package net.cache.bus.kafka.configuration;

import net.cache.bus.core.configuration.CacheBusMessageChannelConfiguration;
import net.cache.bus.core.configuration.ConfigurationException;
import net.cache.bus.core.impl.resolvers.StdHostNameResolver;
import net.cache.bus.core.transport.HostNameResolver;
import net.cache.bus.transport.ChannelConstants;
import org.apache.kafka.clients.CommonClientConfigs;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import static net.cache.bus.transport.ChannelConstants.RECONNECT_RETRY_TIMEOUT;
import static net.cache.bus.transport.ChannelConstants.RECONNECT_RETRY_TIMEOUT_UNITS;

/**
 * Configuration for a message channel based on Apache Kafka.
 *
 * @param producerProperties Kafka message producer properties, cannot be {@code null};
 *                           must include {@link org.apache.kafka.clients.producer.ProducerConfig#BOOTSTRAP_SERVERS_CONFIG};
 *                           if authentication/authorization is used on the broker, the necessary parameters must be provided;
 *                           if needed, client applications can further customize the producer properties based on their needs
 *                           (however, some properties may be overwritten by the channel implementation).
 * @param consumerProperties Kafka message consumer properties, cannot be {@code null};
 *                           must include {@link org.apache.kafka.clients.consumer.ConsumerConfig#BOOTSTRAP_SERVERS_CONFIG};
 *                           if authentication/authorization is used on the broker, the necessary parameters must be provided;
 *                           if needed, client applications can further customize the consumer properties based on their needs
 *                           (however, some properties may be overwritten by the channel implementation).
 * @param reconnectTimeoutMs reconnection timeout in milliseconds for the connections used to send messages to the channel;
 *                           after the timeout, the reconnection attempts will stop and an exception will be thrown;
 *                           by default, the value from {@link ChannelConstants#RECONNECT_RETRY_TIMEOUT} in minutes is used;
 *                           cannot be negative.
 * @param channel            channel name (Kafka topic), cannot be {@code null}.
 * @param subscribingPool    thread pool on which message retrieval from the channel is performed, cannot be {@code null}.
 * @param hostNameResolver   resolver for the current server's host, cannot be {@code null}.
 * @author Alik
 * @see net.cache.bus.kafka.channel.KafkaCacheBusMessageChannel
 * @see Builder
 */
public record KafkaCacheBusMessageChannelConfiguration(
        @Nonnull Map<String, Object> producerProperties,
        @Nonnull Map<String, Object> consumerProperties,
        @Nonnegative long reconnectTimeoutMs,
        @Nonnull String channel,
        @Nonnull ExecutorService subscribingPool,
        @Nonnull HostNameResolver hostNameResolver) implements CacheBusMessageChannelConfiguration {

    public KafkaCacheBusMessageChannelConfiguration {
        if (channel == null || channel.isBlank()) {
            throw new ConfigurationException("Topic name must be not empty");
        }

        Objects.requireNonNull(hostNameResolver, "hostNameResolver");
        Objects.requireNonNull(subscribingPool, "subscribingPool");
        if (producerProperties == null || producerProperties.isEmpty()) {
            throw new ConfigurationException("Producer properties must be not empty");
        }

        if (consumerProperties == null || consumerProperties.isEmpty()) {
            throw new ConfigurationException("Consumer properties must be not empty");
        }

        if (reconnectTimeoutMs < 0) {
            throw new ConfigurationException("Reconnect timeout must be positive: " + reconnectTimeoutMs);
        }

        checkBootstrapServersPropertyExist(consumerProperties, "producer");
        checkBootstrapServersPropertyExist(consumerProperties, "consumer");
    }

    public KafkaCacheBusMessageChannelConfiguration(@Nonnull Map<String, Object> producerProperties, @Nonnull Map<String, Object> consumerProperties, @Nonnull String channel, @Nonnull ExecutorService subscribingPool, @Nonnull HostNameResolver hostNameResolver) {
        this(producerProperties, consumerProperties, RECONNECT_RETRY_TIMEOUT_UNITS.toMillis(RECONNECT_RETRY_TIMEOUT), channel, subscribingPool, hostNameResolver);
    }

    private void checkBootstrapServersPropertyExist(final Map<String, Object> properties, final String type) {
        if (properties.get(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG) == null || properties.get(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG).toString().isBlank()) {
            throw new ConfigurationException("Property '" + CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG + "' must be exist and not blank in " + type + " properties");
        }
    }

    /**
     * Factory method for creating a builder to construct the configuration for a Kafka bus channel.
     *
     * @return cannot be {@code null}.
     */
    @Nonnull
    public static Builder builder() {
        return new Builder();
    }

    @NotThreadSafe
    public static class Builder {

        private final Map<String, Object> producerProperties = new HashMap<>();
        private final Map<String, Object> consumerProperties = new HashMap<>();
        private long reconnectTimeoutMs = RECONNECT_RETRY_TIMEOUT_UNITS.toMillis(RECONNECT_RETRY_TIMEOUT);
        private String channel;
        private HostNameResolver hostNameResolver;
        private ExecutorService subscribingPool;

        /**
         * Sets the main properties for the Kafka producer.<br>
         * Must include {@link org.apache.kafka.clients.producer.ProducerConfig#BOOTSTRAP_SERVERS_CONFIG}.
         * If authentication/authorization is used on the broker, the necessary parameters must be provided.
         * If needed, client applications can further customize the producer properties based on their needs
         * (however, some properties may be overwritten by the channel implementation).
         *
         * @param producerProperties producer properties, cannot be {@code null}.
         * @return cannot be {@code null}.
         */
        @Nonnull
        public Builder setProducerProperties(@Nonnull Map<String, Object> producerProperties) {
            this.producerProperties.clear();
            this.producerProperties.putAll(producerProperties);
            return this;
        }

        /**
         * Sets the main properties for the Kafka consumer.<br>
         * Must include {@link org.apache.kafka.clients.consumer.ConsumerConfig#BOOTSTRAP_SERVERS_CONFIG}.
         * If authentication/authorization is used on the broker, the necessary parameters must be provided.
         * If needed, client applications can further customize the consumer properties based on their needs
         * (however, some properties may be overwritten by the channel implementation).
         *
         * @param consumerProperties consumer properties, cannot be {@code null}.
         * @return cannot be {@code null}.
         */
        @Nonnull
        public Builder setConsumerProperties(@Nonnull Map<String, Object> consumerProperties) {
            this.consumerProperties.clear();
            this.consumerProperties.putAll(consumerProperties);
            return this;
        }

        /**
         * Sets the reconnection timeout in milliseconds.<br>
         * See the parameter description in the JavaDoc of {@link KafkaCacheBusMessageChannelConfiguration}.<br>
         * If not set explicitly, the default value from {@link ChannelConstants#RECONNECT_RETRY_TIMEOUT}
         * in {@link ChannelConstants#RECONNECT_RETRY_TIMEOUT_UNITS} units will be used.
         *
         * @param reconnectTimeoutMs maximum number of connections allowed to be used with the channel, cannot be {@code < 2}.
         * @return cannot be {@code null}.
         */
        @Nonnull
        public Builder setReconnectTimeoutMs(@Nonnegative long reconnectTimeoutMs) {
            this.reconnectTimeoutMs = reconnectTimeoutMs;
            return this;
        }

        /**
         * Sets the Kafka topic name from which messages are retrieved and to which messages are sent.
         *
         * @param channel JMS topic name, cannot be {@code null}.
         * @return cannot be {@code null}.
         */
        @Nonnull
        public Builder setChannel(@Nonnull String channel) {
            this.channel = channel;
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
         * Constructs a {@link net.cache.bus.kafka.channel.KafkaCacheBusMessageChannel} object based on the data provided during the object's construction.
         *
         * @return cannot be {@code null}.
         * @see KafkaCacheBusMessageChannelConfiguration
         */
        @Nonnull
        public KafkaCacheBusMessageChannelConfiguration build() {
            return new KafkaCacheBusMessageChannelConfiguration(
                    new HashMap<>(this.producerProperties),
                    new HashMap<>(this.consumerProperties),
                    this.reconnectTimeoutMs,
                    this.channel,
                    this.subscribingPool,
                    this.hostNameResolver
            );
        }
    }
}
