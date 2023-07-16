package net.cache.bus.rabbit.configuration;

import com.rabbitmq.client.ConnectionFactory;
import net.cache.bus.core.configuration.CacheBusMessageChannelConfiguration;
import net.cache.bus.core.configuration.ConfigurationException;
import net.cache.bus.core.impl.resolvers.StdHostNameResolver;
import net.cache.bus.core.transport.HostNameResolver;
import net.cache.bus.transport.ChannelConstants;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.Objects;

import static net.cache.bus.transport.ChannelConstants.RECONNECT_RETRY_TIMEOUT;
import static net.cache.bus.transport.ChannelConstants.RECONNECT_RETRY_TIMEOUT_UNITS;

/**
 * Конфигурация для JMS-совместимого канала сообщений.
 *
 * @param connectionFactory      фабрика соединений с брокером, не может быть {@code null}.
 * @param availableChannelsCount максимальное количество каналов с RabbitMQ,
 *                               которое может использоваться поверх соединения с брокером;
 *                               значение не должно быть низким, т.к. низкое значение повлечет высокую
 *                               конкуренцию за соединение при отправке данных; не может быть
 *                               меньше {@code 2}; значение по-умолчанию составляет 9
 *                               (8 соединений для отправки сообщений, т.е. 8 потоков могут
 *                               одновременно отправлять данные без конкуренции за соединение
 *                               + 1 соединение на получение сообщений с других серверов).
 * @param reconnectTimeoutMs     тайм-аут переподключения в миллисекундах при разрыве соединений,
 *                               используемых для отправки сообщений в канал; по тайм-ауту
 *                               произойдет прекращение попытки восстановить соединение и будет
 *                               сгенерировано исключение; по-умолчанию используется значение
 *                               {@link ChannelConstants#RECONNECT_RETRY_TIMEOUT} в минутах;
 *                               не может быть отрицательным.
 * @param channel                имя канала, не может быть {@code null}.
 * @param hostNameResolver       определитель хоста текущего сервера, не может быть {@code null}.
 * @author Alik
 * @see Builder
 * @see net.cache.bus.rabbit.channel.RabbitCacheBusMessageChannel
 */
public record RabbitCacheBusMessageChannelConfiguration(
        @Nonnull ConnectionFactory connectionFactory,
        @Nonnegative int availableChannelsCount,
        @Nonnegative long reconnectTimeoutMs,
        @Nonnull String channel,
        @Nonnull HostNameResolver hostNameResolver) implements CacheBusMessageChannelConfiguration {

    /**
     * Минимально необходимое количество каналов RabbitMQ (1 для отправки + 1 для чтения из RabbitMQ).
     */
    private static final int MIN_CHANNELS_COUNT = 2;

    /**
     * Количество каналов в рамках соединения с RabbitMQ по-умолчанию (8 для отправки в RabbitMQ + 1 для чтения из RabbitMQ).
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
     * Фабричный метод создания построителя для формирования конфигурации для JMS-канала шины.
     *
     * @return не может быть {@code null}.
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
         * Устанавливает фабрика соединений, через которую производится взаимодействие с брокером.
         *
         * @param connectionFactory фабрика соединений, не может быть {@code null}.
         * @return не может быть {@code null}.
         */
        @Nonnull
        public Builder setConnectionFactory(@Nonnull ConnectionFactory connectionFactory) {
            this.connectionFactory = connectionFactory;
            return this;
        }

        /**
         * Устанавливает максимально допустимое для использования количество
         * соединений с каналом для шины кэшей.<br>
         * См. описание параметра в Java-doc к {@link RabbitCacheBusMessageChannelConfiguration}.<br>
         * Если не установить значение явно, то будет использоваться значение по-умолчанию
         * {@link RabbitCacheBusMessageChannelConfiguration#DEFAULT_CHANNELS_COUNT}.
         *
         * @param availableChannelsCount максимально допустимое для использования количество
         *                                  соединений с каналом, не может быть {@code < 2}.
         * @return не может быть {@code null}.
         */
        @Nonnull
        public Builder setAvailableChannelsCount(@Nonnegative int availableChannelsCount) {
            this.availableChannelsCount = availableChannelsCount;
            return this;
        }

        /**
         * Устанавливает тайм-аут в миллисекундах для переподключения при разрыве соединения.<br>
         * См. описание параметра в Java-doc к {@link CacheBusMessageChannelConfiguration#reconnectTimeoutMs()}.<br>
         * Если не установить значение явно, то будет использоваться значение по-умолчанию
         * {@link ChannelConstants#RECONNECT_RETRY_TIMEOUT} в единицах
         * {@link ChannelConstants#RECONNECT_RETRY_TIMEOUT_UNITS}.
         *
         * @param reconnectTimeoutMs тайм-аут для переподключения при разрыве соединения,
         *                           не может быть отрицательным.
         * @return не может быть {@code null}.
         */
        @Nonnull
        public Builder setReconnectTimeoutMs(@Nonnegative long reconnectTimeoutMs) {
            this.reconnectTimeoutMs = reconnectTimeoutMs;
            return this;
        }

        /**
         * Устанавливает имя канала, из которого извлекаются и в который отправляются сообщения.
         *
         * @param channel имя канала Rabbit, не может быть {@code null}.
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
         * Формирует на основе данных, переданных при построении объект конфигурации канала {@link RabbitCacheBusMessageChannelConfiguration}.
         *
         * @return не может быть {@code null}.
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
