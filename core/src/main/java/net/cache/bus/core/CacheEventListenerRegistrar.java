package net.cache.bus.core;

import javax.annotation.Nonnull;
import java.io.Serializable;

/**
 * Registers event listeners for cache element changes.
 *
 * @author Alik
 * @see Cache
 * @see CacheEventListener
 */
public interface CacheEventListenerRegistrar {

    /**
     * Registers event listeners for cache element changes for the specified cache     *
     *
     * @param cacheBus the cache bus for which the listener is being registered, cannot be {@code null}.
     * @param cache    the cache for which the listeners are being registered, cannot be {@code null}.
     * @param <K>      the cache key type, must be serializable
     * @param <V>      the cache value type, must be serializable
     */
    <K extends Serializable, V extends Serializable> void registerFor(
            @Nonnull CacheBus cacheBus,
            @Nonnull Cache<K, V> cache
    );

    /**
     * Unregisters previously registered event listeners for cache element changes for the specified cache.
     *
     * @param cacheBus the cache bus for which the listeners were previously registered, cannot be {@code null}.
     * @param cache    the cache for which the listeners were previously registered, cannot {@code null}.
     * @param <K>      the cache key type, must be serializable
     * @param <V>      the cache value type, must be serializable
     */
    <K extends Serializable, V extends Serializable> void unregisterFor(
            @Nonnull CacheBus cacheBus,
            @Nonnull Cache<K, V> cache
    );
}
