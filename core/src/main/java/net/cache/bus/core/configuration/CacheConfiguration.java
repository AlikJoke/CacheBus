package net.cache.bus.core.configuration;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.Set;

/**
 * Distributed cache configuration.
 *
 * @author Alik
 * @see TimestampCacheConfiguration
 */
public interface CacheConfiguration {

    /**
     * Returns the name of the cache to which the configuration applies.
     *
     * @return the cache name, cannot be {@code null}.
     */
    @Nonnull
    String cacheName();

    /**
     * Returns the type of the distributed cache.
     *
     * @return the cache type, cannot be {@code null}.
     * @see CacheType
     */
    @Nonnull
    CacheType cacheType();

    /**
     * Returns additional aliases for the cache. Used when processing messages from other servers.<br>
     * Aliases are useful in a heterogeneous environment where communication between different applications
     * is possible (e.g., in a microservices environment). One application may work with one cache,
     * while another application works with a different cache but with the same key, and therefore may be interested
     * in changes to the cache of the first application.
     *
     * @return optional additional cache aliases, cannot be {@code null}.
     * @implSpec Should only be supported for invalidation caches, as the structure of the stored value may differ.
     */
    @Nonnull
    Set<String> cacheAliases();

    /**
     * Returns whether to use timestamp-based comparison of cache element changes
     * when applying events from remote cache versions.<br>
     * This setting implies maintaining timestamps for cache keys (modification time).
     * If an operation a remote cache event was performed before the key value was changed
     * on the local (current) server, the event will not be applied to the local cache.<br>
     * It should be noted that using this mode for a cache incurs overhead in the form of
     * additional storage of a timestamp for each key.<br>
     * Additionally, time synchronization between servers must be ensured.
     *
     * @return whether to use timestamps to determine the need to apply remote
     * changes to the local cache, {@code true} if timestamps are used, {@code false} otherwise.
     */
    boolean useTimestampBasedComparison();

    /**
     * Returns the configuration of cache item timestamps if timestamp-based comparison mode is used
     * ({@code useTimestampBasedComparison() == true}).
     *
     * @return the timestamp configuration, cannot be {@code null}.
     * @see TimestampCacheConfiguration
     */
    @Nonnull
    Optional<TimestampCacheConfiguration> timestampConfiguration();

    /**
     * Configuration timestamps of cache item changes.
     *
     * @author Alik
     */
    interface TimestampCacheConfiguration {

        /**
         * Returns the estimated average number of elements in the cache (on average).<br>
         * The more accurate the value, the more efficient the processing of timestamps
         * for cache elements when comparing changes to cache elements on local and remote servers.
         *
         * @return the estimated number of elements in the cache, cannot be negative;
         * if {@code useTimestampBasedComparison() == false}, the value is ignored.
         */
        @Nonnegative
        int probableAverageElementsCount();

        /**
         * Returns the expiration time in milliseconds after which a stored cache element change timestamp is
         * considered expired and can be removed from the timestamp store.<br>
         * This expiration time is required to prevent unnecessary timestamps from accumulating in the store,
         * occupying memory. For example, if {@code timestampExpiration() 60_000}, then during the next run of
         * the timestamp cleanup, timestamps that are older
         * than {@code java.lang.System.currentTimeMillis() - timestampExpiration()} will be deleted.<br>
         * Only used if {@code useTimestampBasedComparison() == true}; not necessary to specify this parameter otherwise.
         *
         * @return the expiration time in milliseconds, after which a stored timestamp is considered
         * expired and can be removed, cannot be negative.
         */
        @Nonnegative
        long timestampExpiration();
    }
}
