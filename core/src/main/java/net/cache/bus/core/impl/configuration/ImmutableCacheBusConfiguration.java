package net.cache.bus.core.impl.configuration;

import net.cache.bus.core.configuration.*;

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
public final class ImmutableCacheBusConfiguration implements CacheBusConfiguration {

    private final CacheConfigurationSource cacheConfigurationSource;
    private final CacheBusTransportConfiguration transportConfiguration;
    private final CacheProviderConfiguration providerConfiguration;

    public ImmutableCacheBusConfiguration(@Nonnull CacheBusConfiguration configuration) {
        this(configuration.cacheConfigurationSource(), configuration.transportConfiguration(), configuration.providerConfiguration());
    }

    public ImmutableCacheBusConfiguration(
            @Nonnull CacheConfigurationSource cacheConfigurationSource,
            @Nonnull CacheBusTransportConfiguration transportConfiguration,
            @Nonnull CacheProviderConfiguration providerConfiguration) {
        this.cacheConfigurationSource = Objects.requireNonNull(cacheConfigurationSource, "cacheConfigurationBuilder");
        this.providerConfiguration = Objects.requireNonNull(providerConfiguration, "providerConfiguration");
        this.transportConfiguration = Objects.requireNonNull(transportConfiguration, "transportConfiguration");
    }

    @Nonnull
    @Override
    public CacheConfigurationSource cacheConfigurationSource() {
        return this.cacheConfigurationSource;
    }

    @Nonnull
    @Override
    public CacheBusTransportConfiguration transportConfiguration() {
        return this.transportConfiguration;
    }

    @Nonnull
    @Override
    public CacheProviderConfiguration providerConfiguration() {
        return this.providerConfiguration;
    }

    @Override
    public String toString() {
        return "ImmutableCacheBusConfiguration{" +
                ", transportConfiguration=" + transportConfiguration +
                ", providerConfiguration=" + providerConfiguration +
                ", cacheConfigurationSource=" + cacheConfigurationSource +
                '}';
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
                    this.providerConfiguration
            );
        }
    }
}
