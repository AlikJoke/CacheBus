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
 * Конфигурация для канала сообщений на основе Apache Kafka.
 *
 * @param producerProperties свойства производителя сообщений Kafka, не может быть {@code null};
 *                           обязательно наличие {@link org.apache.kafka.clients.producer.ProducerConfig#BOOTSTRAP_SERVERS_CONFIG};
 *                           eсли используется аутентификация / авторизация на брокере, то должны
 *                           быть переданы необходимые параметры; eсли необходимо, клиентские
 *                           приложения могут производить дополнительную настройку параметров
 *                           производителя исходя из своих потребностей (однако часть параметров
 *                           может быть перезаписана реализацией канала).
 * @param consumerProperties свойства потребителя сообщений Kafka, не может быть {@code null};
 *                           обязательно наличие {@link org.apache.kafka.clients.consumer.ConsumerConfig#BOOTSTRAP_SERVERS_CONFIG};
 *                           если используется аутентификация / авторизация на брокере, то должны быть
 *                           переданы необходимые параметры; если необходимо, клиентские приложения
 *                           могут производить дополнительную настройку параметров потребителя исходя
 *                           из своих потребностей (однако часть параметров может быть перезаписана реализацией канала).
 * @param reconnectTimeoutMs тайм-аут переподключения в миллисекундах при разрыве соединений,
 *                           используемых для отправки сообщений в канал; по тайм-ауту
 *                           произойдет прекращение попытки восстановить соединение и будет
 *                           сгенерировано исключение; по-умолчанию используется значение
 *                           {@link ChannelConstants#RECONNECT_RETRY_TIMEOUT} в минутах;
 *                           не может быть отрицательным.
 * @param channel            имя канала (топика Kafka), не может быть {@code null}.
 * @param subscribingPool    пул потоков, на котором производится получение сообщений из канала, не может быть {@code null}.
 * @param hostNameResolver   определитель хоста текущего сервера, не может быть {@code null}.
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
     * Фабричный метод создания построителя для формирования конфигурации для Kafka-канала шины.
     *
     * @return не может быть {@code null}.
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
         * Устанавливает основные свойства производителя Kafka.<br>
         * Обязательно наличие {@link org.apache.kafka.clients.producer.ProducerConfig#BOOTSTRAP_SERVERS_CONFIG}.
         * Если используется аутентификация / авторизация на брокере, то должны быть переданы необходимые параметры.
         * Если необходимо, клиентские приложения могут производить дополнительную настройку параметров
         * производителя исходя из своих потребностей (однако часть параметров может быть перезаписана реализацией канала).
         *
         * @param producerProperties свойства производителя, не может быть {@code null}.
         * @return не может быть {@code null}.
         */
        @Nonnull
        public Builder setProducerProperties(@Nonnull Map<String, Object> producerProperties) {
            this.producerProperties.clear();
            this.producerProperties.putAll(producerProperties);
            return this;
        }

        /**
         * Устанавливает основные свойства потребителя Kafka.<br>
         * Обязательно наличие {@link org.apache.kafka.clients.consumer.ConsumerConfig#BOOTSTRAP_SERVERS_CONFIG}.
         * Если используется аутентификация / авторизация на брокере, то должны быть переданы необходимые параметры.
         * Если необходимо, клиентские приложения могут производить дополнительную настройку параметров
         * потребителя исходя из своих потребностей (однако часть параметров может быть перезаписана реализацией канала).
         *
         * @param consumerProperties свойства производителя, не может быть {@code null}.
         * @return не может быть {@code null}.
         */
        @Nonnull
        public Builder setConsumerProperties(@Nonnull Map<String, Object> consumerProperties) {
            this.consumerProperties.clear();
            this.consumerProperties.putAll(consumerProperties);
            return this;
        }

        /**
         * Устанавливает тайм-аут в миллисекундах для переподключения при разрыве соединения.<br>
         * См. описание параметра в Java-doc к {@link KafkaCacheBusMessageChannelConfiguration}.<br>
         * Если не установить значение явно, то будет использоваться значение по-умолчанию
         * {@link ChannelConstants#RECONNECT_RETRY_TIMEOUT} в единицах
         * {@link ChannelConstants#RECONNECT_RETRY_TIMEOUT_UNITS}.
         *
         * @param reconnectTimeoutMs максимально допустимое для использования количество
         *                           соединений с каналом, не может быть {@code < 2}.
         * @return не может быть {@code null}.
         */
        @Nonnull
        public Builder setReconnectTimeoutMs(@Nonnegative long reconnectTimeoutMs) {
            this.reconnectTimeoutMs = reconnectTimeoutMs;
            return this;
        }

        /**
         * Устанавливает имя Kafka-топика, из которого извлекаются и в который отправляются сообщения.
         *
         * @param channel имя JMS-топика, не может быть {@code null}.
         * @return не может быть {@code null}.
         */
        @Nonnull
        public Builder setChannel(@Nonnull String channel) {
            this.channel = channel;
            return this;
        }

        /**
         * Устанавливает определитель хоста. Если не задавать, то используется реализация по-умолчанию {@link StdHostNameResolver}.
         *
         * @param hostNameResolver определитель хоста, не может быть {@code null}, если метод вызывается, а не используется реализация по-умолчанию.
         * @return не может быть {@code null}
         */
        @Nonnull
        public Builder setHostNameResolver(@Nonnull HostNameResolver hostNameResolver) {
            this.hostNameResolver = hostNameResolver;
            return this;
        }

        /**
         * Устанавливает пул, на котором должно производиться получение сообщений из канала.
         *
         * @param subscribingPool пул потоков, не может быть {@code null}.
         * @return не может быть {@code null}.
         */
        @Nonnull
        public Builder setSubscribingPool(@Nonnull ExecutorService subscribingPool) {
            this.subscribingPool = subscribingPool;
            return this;
        }

        /**
         * Формирует на основе данных, переданных при построении объект конфигурации канала {@link net.cache.bus.kafka.channel.KafkaCacheBusMessageChannel}.
         *
         * @return не может быть {@code null}.
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
