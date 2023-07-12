package net.cache.bus.jms.configuration;

import net.cache.bus.core.configuration.CacheBusMessageChannelConfiguration;
import net.cache.bus.core.configuration.ConfigurationException;
import net.cache.bus.core.impl.resolvers.StdHostNameResolver;
import net.cache.bus.core.transport.HostNameResolver;

import javax.annotation.Nonnull;
import javax.jms.ConnectionFactory;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Конфигурация для JMS-совместимого канала сообщений.
 *
 * @param connectionFactory фабрика соединений с брокером, не может быть {@code null}.
 * @param channel           имя канала (в терминах JMS - топика), не может быть {@code null}.
 * @param subscribingPool   пул потоков, на котором производится получение сообщений из канала, не может быть {@code null}.
 * @param hostNameResolver  определитель хоста текущего сервера, не может быть {@code null}.
 * @author Alik
 * @see Builder
 */
public record CacheBusJmsMessageChannelConfiguration(
        @Nonnull ConnectionFactory connectionFactory,
        @Nonnull String channel,
        @Nonnull ExecutorService subscribingPool,
        @Nonnull HostNameResolver hostNameResolver) implements CacheBusMessageChannelConfiguration {

    public CacheBusJmsMessageChannelConfiguration {
        Objects.requireNonNull(connectionFactory, "connectionFactory");
        Objects.requireNonNull(subscribingPool, "subscribingPool");
        Objects.requireNonNull(hostNameResolver, "hostNameResolver");

        if (channel == null || channel.isEmpty()) {
            throw new ConfigurationException("Channel must be not empty");
        }
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
         * Формирует на основе данных, переданных при построении объект конфигурации канала {@link CacheBusJmsMessageChannelConfiguration}.
         *
         * @return не может быть {@code null}.
         * @see CacheBusJmsMessageChannelConfiguration
         */
        @Nonnull
        public CacheBusJmsMessageChannelConfiguration build() {
            return new CacheBusJmsMessageChannelConfiguration(
                    this.connectionFactory,
                    this.channel,
                    this.subscribingPool,
                    this.hostNameResolver
            );
        }
    }
}
