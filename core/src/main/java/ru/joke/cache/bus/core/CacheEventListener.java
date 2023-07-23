package ru.joke.cache.bus.core;

import java.io.Serializable;

/**
 * Cache change event listener.
 *
 * @param <K> the cache key type, must be serializable
 * @param <V> the cache value type, must be serializable
 * @author Alik
 */
public interface CacheEventListener<K extends Serializable, V extends Serializable> {
}
