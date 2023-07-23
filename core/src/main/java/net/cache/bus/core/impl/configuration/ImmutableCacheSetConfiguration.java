package net.cache.bus.core.impl.configuration;

import net.cache.bus.core.configuration.CacheConfiguration;
import net.cache.bus.core.configuration.CacheSetConfiguration;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * Immutable implementation of cache bus configuration.
 *
 * @param cacheConfigurations a set of configurations for specific caches, cannot be {@code null}.
 * @param useAsyncCleaning    indicates whether asynchronous cleaning of stored timestamps
 *                            for cache element changes should be used (applies only if at least one cache
 *                            uses timestamp-based comparison for cache element changes).
 * @author Alik
 * @see CacheSetConfiguration
 */
public record ImmutableCacheSetConfiguration(
        @Nonnull Set<CacheConfiguration> cacheConfigurations,
        boolean useAsyncCleaning) implements CacheSetConfiguration {
}
