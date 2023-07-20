package net.cache.bus.core.impl.configuration;

import net.cache.bus.core.configuration.*;
import net.cache.bus.core.metrics.CacheBusMetricsRegistry;
import net.cache.bus.core.metrics.NoOpCacheBusMetricsRegistry;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Objects;

/**
 * Реализация неизменяемой конфигурации шины кэшей. Для формирования рекомендуется использовать построитель,
 * получаемый с помощью фабричного метода {@linkplain ImmutableCacheBusConfiguration#builder()}.
 *
 * @author Alik
 * @see CacheBusConfiguration
 * @see Builder
 */
@ThreadSafe
@Immutable
public record ImmutableCacheBusConfiguration(
        @Nonnull CacheConfigurationSource cacheConfigurationSource,
        @Nonnull CacheBusTransportConfiguration transportConfiguration,
        @Nonnull CacheProviderConfiguration providerConfiguration,
        @Nonnull CacheBusMetricsRegistry metricsRegistry) implements CacheBusConfiguration {

    public ImmutableCacheBusConfiguration {
        Objects.requireNonNull(cacheConfigurationSource, "cacheConfigurationBuilder");
        Objects.requireNonNull(providerConfiguration, "providerConfiguration");
        Objects.requireNonNull(transportConfiguration, "transportConfiguration");
        Objects.requireNonNull(transportConfiguration, "metricsRegistry");
    }

    public ImmutableCacheBusConfiguration(
            @Nonnull CacheConfigurationSource cacheConfigurationSource,
            @Nonnull CacheBusTransportConfiguration transportConfiguration,
            @Nonnull CacheProviderConfiguration providerConfiguration) {
        this(cacheConfigurationSource, transportConfiguration, providerConfiguration, new NoOpCacheBusMetricsRegistry());
    }
    /**
     * Возвращает объект строителя для формирования объекта конфигурации.
     *
     * @return не может быть {@code null}.
     */
    @Nonnull
    public static Builder builder() {
        return new Builder();
    }

    @NotThreadSafe
    public static class Builder {

        private CacheConfigurationSource cacheConfigurationSource;
        private CacheBusTransportConfiguration transportConfiguration;
        private CacheProviderConfiguration providerConfiguration;
        private CacheBusMetricsRegistry metricsRegistry = new NoOpCacheBusMetricsRegistry();

        /**
         * Устанавливает построитель для формирования конфигурация кэшей, подключенных к шине.
         *
         * @param cacheConfigurationSource построитель конфигураций кэшей шины, не может быть {@code null}.
         * @return не может быть {@code null}.
         * @see CacheConfiguration
         * @see CacheConfigurationSource
         */
        @Nonnull
        public Builder setCacheConfigurationBuilder(@Nonnull CacheConfigurationSource cacheConfigurationSource) {
            this.cacheConfigurationSource = cacheConfigurationSource;
            return this;
        }

        /**
         * Устанавливает конфигурацию транспорта шины событий.
         *
         * @param transportConfiguration конфигурация транспорта шины, не может быть {@code null}.
         * @return не может быть {@code null}.
         * @see CacheBusTransportConfiguration
         */
        @Nonnull
        public Builder setTransportConfiguration(@Nonnull CacheBusTransportConfiguration transportConfiguration) {
            this.transportConfiguration = transportConfiguration;
            return this;
        }

        /**
         * Устанавливает конфигурацию используемого провайдера кэширования для шины.
         *
         * @param providerConfiguration конфигурация провайдера кэширования, не может быть {@code null}.
         * @return не может быть {@code null}.
         * @see CacheProviderConfiguration
         */
        @Nonnull
        public Builder setProviderConfiguration(@Nonnull CacheProviderConfiguration providerConfiguration) {
            this.providerConfiguration = providerConfiguration;
            return this;
        }

        /**
         * Устанавливает используемый реестр метрик. Если не задан, то используется No-Op реализация, которая не регистрирует метрики.
         *
         * @param metricsRegistry реестр метрик для шины, не может быть {@code null}.
         * @return не может быть {@code null}.
         * @see CacheBusMetricsRegistry
         */
        @Nonnull
        public Builder setMetricsRegistry(@Nonnull CacheBusMetricsRegistry metricsRegistry) {
            this.metricsRegistry = metricsRegistry;
            return this;
        }

        /**
         * Формирует объект конфигурации шины кэшей на основе переданных данных.
         *
         * @return не может быть {@code null}.
         * @see CacheBusConfiguration
         */
        @Nonnull
        public CacheBusConfiguration build() {
            return new ImmutableCacheBusConfiguration(
                    this.cacheConfigurationSource,
                    this.transportConfiguration,
                    this.providerConfiguration,
                    this.metricsRegistry
            );
        }
    }
}
