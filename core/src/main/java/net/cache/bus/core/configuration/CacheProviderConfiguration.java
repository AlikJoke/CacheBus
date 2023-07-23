package net.cache.bus.core.configuration;

import net.cache.bus.core.CacheEventListenerRegistrar;
import net.cache.bus.core.CacheManager;

import javax.annotation.Nonnull;

/**
 * Configuration specific to the caching provider (implementation) that is required for the cache bus to function.
 *
 * @author Alik
 * @see CacheManager
 * @see CacheEventListenerRegistrar
 */
public interface CacheProviderConfiguration {

    /**
     * Returns the cache manager.
     *
     * @return cannot be {@code null}.
     * @see CacheManager
     */
    @Nonnull
    CacheManager cacheManager();

    /**
     * The method returns a listener registrar for cache change events.
     *
     * @return cannot be {@code null}.
     * @see CacheEventListenerRegistrar
     */
    @Nonnull
    CacheEventListenerRegistrar cacheEventListenerRegistrar();
}
