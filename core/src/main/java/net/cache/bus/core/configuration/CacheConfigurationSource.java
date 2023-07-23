package net.cache.bus.core.configuration;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashSet;
import java.util.Set;

/**
 * Cache configuration source connected to the bus.
 *
 * @author Alik
 * @see CacheConfiguration
 * @see CacheBusConfiguration
 * @see CacheSetConfiguration
 * @see SimpleCacheConfigurationSource
 */
public interface CacheConfigurationSource {

    /**
     * Returns the cache configuration from the source.
     *
     * @return cannot be {@code null}.
     */
    @Nonnull
    CacheSetConfiguration pull();

    /**
     * Returns the default cache configuration source, manually configurable.
     *
     * @return cannot be {@code null}.
     * @see SimpleCacheConfigurationSource
     */
    @Nonnull
    static SimpleCacheConfigurationSource createDefault() {
        return new SimpleCacheConfigurationSource();
    }

    /**
     * Manually configurable cache configuration source.
     *
     * @author Alik
     * @implSpec The implementation is not thread-safe, so the reference to the source can only be passed
     * to the bus configuration object. When publishing the reference externally, the result is undefined
     * if the source is modified in other threads.
     * @see CacheConfigurationSource
     */
    @NotThreadSafe
    final class SimpleCacheConfigurationSource implements CacheConfigurationSource {

        private final Set<CacheConfiguration> configurations = new HashSet<>();
        private boolean useAsyncCleaning;

        @Nonnull
        @Override
        public CacheSetConfiguration pull() {
            final Set<CacheConfiguration> cacheConfigurations = Set.copyOf(this.configurations);
            return new CacheSetConfiguration() {
                @Nonnull
                @Override
                public Set<CacheConfiguration> cacheConfigurations() {
                    return cacheConfigurations;
                }

                @Override
                public boolean useAsyncCleaning() {
                    return useAsyncCleaning;
                }
            };
        }

        /**
         * Adds the configuration of a single cache to the source.
         *
         * @param configuration the cache configuration, cannot be {@code null}.
         * @return the source for further building, cannot be {@code null}.
         */
        @Nonnull
        public SimpleCacheConfigurationSource add(@Nonnull CacheConfiguration configuration) {
            this.configurations.add(configuration);
            return this;
        }

        /**
         * Adds multiple cache configurations to the source.
         *
         * @param configurations the cache configurations, cannot be {@code null}.
         * @return the source for further building, cannot be {@code null}.
         */
        @Nonnull
        public SimpleCacheConfigurationSource addAll(@Nonnull Set<CacheConfiguration> configurations) {
            this.configurations.addAll(configurations);
            return this;
        }

        /**
         * Clears the cache source.
         *
         * @return the source for further building, cannot be {@code null}.
         */
        @Nonnull
        public SimpleCacheConfigurationSource clear() {
            this.configurations.clear();
            return this;
        }

        /**
         * Sets the flag indicating the use of an asynchronous pool for cleaning outdated temporal
         * change timestamps of cache elements.
         *
         * @param useAsyncCleaning the flag indicating the use of an asynchronous pool for cleaning
         *                         outdated temporal timestamps.
         * @return cannot be {@code null}.
         */
        @Nonnull
        public SimpleCacheConfigurationSource useAsyncCleaning(boolean useAsyncCleaning) {
            this.useAsyncCleaning = useAsyncCleaning;
            return this;
        }

        @Override
        public String toString() {
            return "SimpleCacheSetConfigurationSource{" +
                    "configurations=" + configurations +
                    ", useAsyncCleaning=" + useAsyncCleaning +
                    '}';
        }
    }
}
