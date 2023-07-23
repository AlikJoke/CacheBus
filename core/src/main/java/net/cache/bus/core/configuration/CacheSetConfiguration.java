package net.cache.bus.core.configuration;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * The configuration of caches for this cache bus.
 *
 * @author Alik
 * @see CacheConfiguration
 * @see CacheConfigurationSource
 */
public interface CacheSetConfiguration {

    /**
     * Returns the configurations of caches that should be clustered by the cache bus.
     *
     * @return cannot be {@code null}.
     * @see CacheConfiguration
     */
    @Nonnull
    Set<CacheConfiguration> cacheConfigurations();

    /**
     * Specifies whether an asynchronous pool should be used for evicting old timestamps
     * (see {@linkplain CacheConfiguration.TimestampCacheConfiguration#timestampExpiration()}).<br>
     * If the method returns {@code false}, eviction will occur when the next timestamp is stored in the timestamp
     * store (not every time, eviction may be triggered based on certain criteria or heuristics, such as exceeding
     * a certain number of stored timestamps).<br>
     * If asynchronous eviction is used, a shared pool will be utilized, based on which
     * {@linkplain java.util.concurrent.CompletableFuture} operates.<br>
     * This is only applicable if there is at least one cache that uses timestamps, i.e.,
     * if {@code cacheConfigurations().stream().anyMatch(CacheConfiguration::useTimestampBasedComparison) == true}.
     *
     * @return {@code true}, if asynchronous pool should be used for eviction;
     * {@code false} if eviction should be performed in the modification threads of the timestamp store.
     */
    boolean useAsyncCleaning();
}
