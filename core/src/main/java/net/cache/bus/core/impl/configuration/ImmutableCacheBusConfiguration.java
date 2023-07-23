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
 * Implementation of an immutable cache bus configuration.<br>
 * It is recommended to use the builder obtained through the factory method
 * {@linkplain ImmutableCacheBusConfiguration#builder()} for construction.
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
        Objects.requireNonNull(cacheConfigurationSource, "cacheConfigurationSource");
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
     * Returns a builder object for constructing a configuration object.
     *
     * @return cannot be {@code null}.
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
         * Sets the builder for constructing cache configurations connected to the bus.
         *
         * @param cacheConfigurationSource cache bus configuration builder, cannot be {@code null}.
         * @return cannot be {@code null}.
         * @see CacheConfiguration
         * @see CacheConfigurationSource
         */
        @Nonnull
        public Builder setCacheConfigurationSource(@Nonnull CacheConfigurationSource cacheConfigurationSource) {
            this.cacheConfigurationSource = cacheConfigurationSource;
            return this;
        }

        /**
         * Sets the event bus transport configuration.
         *
         * @param transportConfiguration event bus transport configuration, cannot be {@code null}.
         * @return cannot be {@code null}.
         * @see CacheBusTransportConfiguration
         */
        @Nonnull
        public Builder setTransportConfiguration(@Nonnull CacheBusTransportConfiguration transportConfiguration) {
            this.transportConfiguration = transportConfiguration;
            return this;
        }

        /**
         * Sets the cache provider configuration used by the bus.
         *
         * @param providerConfiguration cache provider configuration, cannot be {@code null}.
         * @return cannot be {@code null}.
         * @see CacheProviderConfiguration
         */
        @Nonnull
        public Builder setProviderConfiguration(@Nonnull CacheProviderConfiguration providerConfiguration) {
            this.providerConfiguration = providerConfiguration;
            return this;
        }

        /**
         * Sets the metrics registry used. If not specified, a No-Op implementation
         * ({@linkplain NoOpCacheBusMetricsRegistry}) is used, which does not register any metrics.
         *
         * @param metricsRegistry metrics registry for the bus, cannot be {@code null}.
         * @return cannot be {@code null}.
         * @see CacheBusMetricsRegistry
         */
        @Nonnull
        public Builder setMetricsRegistry(@Nonnull CacheBusMetricsRegistry metricsRegistry) {
            this.metricsRegistry = metricsRegistry;
            return this;
        }

        /**
         * Constructs a cache bus configuration object based the provided data.
         *
         * @return cannot be {@code null}.
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
