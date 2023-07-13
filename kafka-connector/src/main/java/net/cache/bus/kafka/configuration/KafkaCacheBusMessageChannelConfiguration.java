package net.cache.bus.kafka.configuration;

import net.cache.bus.core.configuration.CacheBusMessageChannelConfiguration;
import net.cache.bus.core.configuration.ConfigurationException;
import net.cache.bus.core.transport.HostNameResolver;
import org.apache.kafka.clients.CommonClientConfigs;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Конфигурация для канала сообщений на основе Apache Kafka.
 *
 * @param producerProperties свойства производителя сообщений Kafka, не может быть {@code null}.
 *                           @param consumerProperties свойства потребителя сообщений Kafka, не может быть {@code null}.
 * @param channel           имя канала (топика Kafka), не может быть {@code null}.
 * @param subscribingPool   пул потоков, на котором производится получение сообщений из канала, не может быть {@code null}.
 * @param hostNameResolver  определитель хоста текущего сервера, не может быть {@code null}.
 * @author Alik
 * @see Builder
 */
public record KafkaCacheBusMessageChannelConfiguration(
        @Nonnull Map<String, Object> producerProperties,
        @Nonnull Map<String, Object> consumerProperties,
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

        checkBootstrapServersPropertyExist(consumerProperties, "producer");
        checkBootstrapServersPropertyExist(consumerProperties, "consumer");
    }

    private void checkBootstrapServersPropertyExist(final Map<String, Object> properties, final String type) {
        if (properties.get(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG) == null || properties.get(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG).toString().isBlank()) {
            throw new ConfigurationException("Property '" + CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG + "' must be exist and not blank in " + type + " properties");
        }
    }

    @Nonnull
    public static Builder builder() {
        return new Builder();
    }

    @NotThreadSafe
    public static class Builder {

        private final Map<String, Object> producerProperties = new HashMap<>();
        private final Map<String, Object> consumerProperties = new HashMap<>();
        private String channel;
        private HostNameResolver hostNameResolver;
        private ExecutorService subscribingPool;

        @Nonnull
        public Builder setProducerProperties(@Nonnull Map<String, Object> producerProperties) {
            this.producerProperties.putAll(producerProperties);
            return this;
        }

        @Nonnull
        public Builder setConsumerProperties(@Nonnull Map<String, Object> consumerProperties) {
            this.consumerProperties.putAll(consumerProperties);
            return this;
        }

        @Nonnull
        public Builder setChannel(@Nonnull String channel) {
            this.channel = channel;
            return this;
        }

        @Nonnull
        public Builder setHostNameResolver(@Nonnull HostNameResolver hostNameResolver) {
            this.hostNameResolver = hostNameResolver;
            return this;
        }

        @Nonnull
        public Builder setSubscribingPool(@Nonnull ExecutorService subscribingPool) {
            this.subscribingPool = subscribingPool;
            return this;
        }

        @Nonnull
        public KafkaCacheBusMessageChannelConfiguration build() {
            return new KafkaCacheBusMessageChannelConfiguration(
                    new HashMap<>(this.producerProperties),
                    new HashMap<>(this.consumerProperties),
                    this.channel,
                    this.subscribingPool,
                    this.hostNameResolver
            );
        }
    }
}
