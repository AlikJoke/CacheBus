package net.cache.bus.core.configuration;

import javax.annotation.Nonnull;

/**
 * Конфигурация шины кэшей.
 *
 * @author Alik
 * @see CacheConfiguration
 * @see CacheBusTransportConfiguration
 * @see CacheProviderConfiguration
 */
public interface CacheBusConfiguration {

    /**
     * Возвращает источник конфигурации кэшей, подключенных к шине.
     *
     * @return не может быть {@code null}.
     * @see CacheConfiguration
     */
    @Nonnull
    CacheConfigurationSource cacheConfigurationSource();

    /**
     * Возвращает конфигурацию транспорта для данной шины кэшей.
     *
     * @return конфигурацию транспорта, не может быть {@code null}.
     * @see CacheBusTransportConfiguration
     */
    @Nonnull
    CacheBusTransportConfiguration transportConfiguration();

    /**
     * Возвращает конфигурацию, специфичную провайдеру кэширования.
     *
     * @return не может быть {@code null}.
     * @see CacheProviderConfiguration
     */
    @Nonnull
    CacheProviderConfiguration providerConfiguration();
}
