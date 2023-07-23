package ru.joke.cache.bus.core;

import ru.joke.cache.bus.core.state.ComponentState;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Optional;

/**
 * Cache manager. Provides access to caches various types, as well as general caching settings.
 *
 * @author Alik
 * @see Cache
 */
public interface CacheManager {

    /**
     * Returns the "underlying" cache manager on which this manager is based.
     * The underlying manager may not exist if the manager's implementation is not based on an existing implementation
     * of a caching provider - in such cases, the implementation should throw
     * an {@linkplain UnsupportedOperationException}.
     *
     * @param managerType the type of the source cache manager, cannot be {@code null}.
     * @return the underlying cache manager of a caching provider, if available; cannot {@code null}.
     * @throws UnsupportedOperationException if the underlying cache manager does not exist
     */
    @Nonnull
    <T> T getUnderlyingCacheManager(@Nonnull Class<T> managerType);

    /**
     * Returns information about the state of the cache manager.
     *
     * @return cannot be {@code null}.
     * @see ComponentState
     */
    @Nonnull
    ComponentState state();

    /**
     * Returns a cache by its name.
     *
     * @param cacheName the name the cache, cannot be {@code null}.
     * @return the cache object or {linkplain Optional#empty()} if a cache with the given name does not exist.
     * @see Cache
     */
    @Nonnull
    <K extends Serializable, V extends Serializable> Optional<Cache<K, V>> getCache(@Nonnull String cacheName);
}