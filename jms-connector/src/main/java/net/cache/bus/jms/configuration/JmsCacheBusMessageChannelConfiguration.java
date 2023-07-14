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
 * Конфигурация для JMS-совместимого канала сообщений.
 *
 * @param connectionFactory         фабрика соединений с брокером, не может быть {@code null}.
 * @param availableConnectionsCount максимальное количество соединений с брокером JMS,
 *                                  которое может использоваться каналом; значение не должно
 *                                  быть низким, т.к. низкое значение повлечет высокую
 *                                  конкуренцию за соединение при отправке данных; не может быть
 *                                  меньше {@code 2}; значение по-умолчанию составляет 9
 *                                  (8 соединений для отправки сообщений, т.е. 8 потоков могут
 *                                  одновременно отправлять данные без конкуренции за соединение
 *                                  + 1 соединение на получение сообщений с других серверов).
 * @param reconnectTimeoutMs        тайм-аут переподключения в миллисекундах при разрыве соединений,
 *                                  используемых для отправки сообщений в канал; по тайм-ауту
 *                                  произойдет прекращение попытки восстановить соединение и будет
 *                                  сгенерировано исключение; по-умолчанию используется значение
 *                                  {@link ChannelConstants#RECONNECT_RETRY_TIMEOUT} в минутах;
 *                                  не может быть отрицательным.
 * @param channel                   имя канала (в терминах JMS - топика), не может быть {@code null}.
 * @param subscribingPool           пул потоков, на котором производится получение сообщений из канала, не может быть {@code null}.
 * @param hostNameResolver          определитель хоста текущего сервера, не может быть {@code null}.
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
     * Минимально необходимое количество соединений с каналом (1 для отправки + 1 для чтения из канала).
     */
    private static final int MIN_CONNECTIONS_COUNT = 2;

    /**
     * Количество соединений с каналом по-умолчанию (8 для отправки в канал + 1 для чтения из канала).
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
        this(connectionFactory, DEFAULT_CONNECTIONS_COUNT, RECONNECT_RETRY_TIMEOUT_UNITS.toMillis(RECONNECT_RETRY_TIMEOUT), channel, subscribingPool, hostNameResolver);
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
        private int availableConnectionsCount = DEFAULT_CONNECTIONS_COUNT;
        private long reconnectTimeoutMs = RECONNECT_RETRY_TIMEOUT_UNITS.toMillis(RECONNECT_RETRY_TIMEOUT);
        private String channel;
        private ExecutorService subscribingPool;
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
         * См. описание параметра в Java-doc к {@link JmsCacheBusMessageChannelConfiguration}.<br>
         * Если не установить значение явно, то будет использоваться значение по-умолчанию
         * {@link JmsCacheBusMessageChannelConfiguration#DEFAULT_CONNECTIONS_COUNT}.
         *
         * @param availableConnectionsCount максимально допустимое для использования количество
         *                                  соединений с каналом, не может быть {@code < 2}.
         * @return не может быть {@code null}.
         */
        @Nonnull
        public Builder setAvailableConnectionsCount(@Nonnegative int availableConnectionsCount) {
            this.availableConnectionsCount = availableConnectionsCount;
            return this;
        }

        /**
         * Устанавливает тайм-аут в миллисекундах для переподключения при разрыве соединения.<br>
         * См. описание параметра в Java-doc к {@link JmsCacheBusMessageChannelConfiguration}.<br>
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
         * Устанавливает имя JMS-топика, из которого извлекаются и в который отправляются сообщения.
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
         * Формирует на основе данных, переданных при построении объект конфигурации канала {@link JmsCacheBusMessageChannelConfiguration}.
         *
         * @return не может быть {@code null}.
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
