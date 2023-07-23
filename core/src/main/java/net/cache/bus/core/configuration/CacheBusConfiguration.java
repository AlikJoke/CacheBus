package net.cache.bus.core.configuration;

import net.cache.bus.core.metrics.CacheBusMetricsRegistry;

import javax.annotation.Nonnull;

/**
 * Configuration of cache bus.
 *
 * @author Alik
 * @see CacheConfiguration
 * @see CacheBusTransportConfiguration
 * @see CacheProviderConfiguration
 * @see CacheBusMetricsRegistry
 */
public interface CacheBusConfiguration {

    /**
     * Returns the configuration source of caches connected to the cache bus.
     *
     * @return cannot be {@code null}.
     * @see CacheConfiguration
     */
    @Nonnull
    CacheConfigurationSource cacheConfigurationSource();

    /**
     * Returns the transport configuration for the given cache bus.
     *
     * @return configuration of transport, cannot be {@code null}.
     * @see CacheBusTransportConfiguration
     */
    @Nonnull
    CacheBusTransportConfiguration transportConfiguration();

    /**
     * Returns the provider-specific caching configuration.
     *
     * @return cannot be {@code null}.
     * @see CacheProviderConfiguration
     */
    @Nonnull
    CacheProviderConfiguration providerConfiguration();

    /**
     * Returns the metrics registry used for collecting cache bus statistics (metrics).
     *
     * @return metrics registry, cannot be {@code null}.
     * @see CacheBusMetricsRegistry
     */
    CacheBusMetricsRegistry metricsRegistry();
}
