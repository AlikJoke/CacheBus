package net.cache.bus.core.configuration;

import net.cache.bus.core.metrics.CacheBusMetricsRegistry;

import javax.annotation.Nonnull;

/**
 * Конфигурация шины кэшей.
 *
 * @author Alik
 * @see CacheConfiguration
 * @see CacheBusTransportConfiguration
 * @see CacheProviderConfiguration
 * @see CacheBusMetricsRegistry
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

    /**
     * Возвращает реестр метрик, используемый для сбора статистики (метрик) шины кэшей.
     *
     * @return реестр метрик, не может быть {@code null}.
     * @see CacheBusMetricsRegistry
     */
    CacheBusMetricsRegistry metricsRegistry();
}
