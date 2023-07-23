package net.cache.bus.core;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * An abstract representation of a cache, independent of the specific caching provider implementation.<br>
 * Supports basic operations for working with the cache:
 *
 * <ul>
 * <li>Retrieving an object from the cache by key</li>
 * <li>Removing an object from the cache by key</li>
 * <li>Adding an item to the cache</li>
 * <li>Clearing the cache contents</li>
 * <ul>
 * <br>
 * The concrete implementation can be a local cache, an invalidation cache, or a replicated cache.
 *
 * @param <K> the type of the cache keys, must be serializable
 * @param <V> the type of the cache values, must be serializable
 * @author Alik
 */
public interface Cache<K extends Serializable, V extends Serializable> {

    /**
     * Returns the name of the cache.
     *
     * @return cannot be {@code null}.
     */
    String getName();

    /**
     * Retrieves the value from the cache based on the key, if it exists in the cache.
     * To ensure stricter typing, the "type token" pattern is used. If the value type is unknown,
     * {@link Object} can be used.
     *
     * @param key the key of the element in the cache, cannot be {@code null}.
     * @return the value the cache associated with the specified key, wrapped in {@link Optional};
     * the value may be absent.
     */
    @Nonnull
    @CheckReturnValue
    Optional<V> get(@Nonnull K key);

    /**
     * Removes an element from the cache based on the key. If an element with the given key
     * does not exist, nothing happens.
     *
     * @param key the key of the element in the cache, cannot be {@code null}.
     */
    void evict(@Nonnull K key);

    /**
     * Removes an element from the cache based on the key. Returns the value previously associated with the key.
     * If an element with the given key does not exist, returns {@code null}.
     *
     * @param key the key of the element in the cache, cannot be {@code null}.
     * @return the value previously associated with the key, wrapped in {@link Optional}. The value may be absent.
     */
    @Nonnull
    @CheckReturnValue
    Optional<V> remove(@Nonnull K key);

    /**
     * Adds an element to the cache. Replaces the existing element with a new value if the element already exists the cache.
     *
     * @param key   the key of the element, cannot {@code null}.
     * @param value the value of the element, can be {@code null}.
     */
    void put(@Nonnull K key, @Nullable V value);

    /**
     * Adds an element the cache if there is no value associated with the given key.
     *
     * @param key   the key of the element, cannot be {@code null}.
     * @param value the value of the element, can be {@code null}.
     */
    void putIfAbsent(@Nonnull K key, @Nullable V value);

    /**
     * Clears this cache.
     */
    void clear();

    /**
     * Merges an existing element in the cache with a new value according to the provided merge function.
     * This operation allows avoiding conflicts when multiple threads modify the same element
     * (e.g., when modifying an object that contains a collection or an associative array) concurrently.<br>
     * If the element does not exist in the cache, it will be added to the cache without merging. <br>
     * The merge function should return {@code null} if the value for the key should be removed from the cache. <br>
     * The implementation of this operation should be thread-safe and take into account the possibility of
     * concurrent modification of the element in the cache by another thread.
     *
     * @param key           the key of the element in the cache; cannot be {@code null}.
     * @param value         the new value of the element to add or modify; cannot be {@code null}.
     * @param mergeFunction the merge function to merge the new value with the existing value in the cache; cannot be {@code null}.
     */
    void merge(@Nonnull K key, @Nonnull V value, @Nonnull BiFunction<? super V, ? super V, ? extends V> mergeFunction);

    /**
     * Computes the value of an element in the cache if it does not already exist.
     * If an element with the given key already exists, it will be returned and no computation will occur.
     * The semantics of this operation are similar to the {@linkplain ConcurrentMap#computeIfAbsent(Object, Function)} method.
     *
     * @param key           the key of the element in the cache; cannot {@code null}.
     * @param valueFunction the function to compute the value of the element if it is absent in the cache; cannot be {@code null}.
     * @return the value of the element from the cache (either existing the time of the call or added as a result), wrapped in {@link Optional}.
     */
    @CheckReturnValue
    @Nonnull
    Optional<V> computeIfAbsent(@Nonnull K key, @Nonnull Function<? super K, ? extends V> valueFunction);

    /**
     * Registers a cache event listener.
     *
     * @param listener the event listener; cannot be {@code null}.
     * @see CacheEventListener
     */
    void registerEventListener(@Nonnull CacheEventListener<K, V> listener);

    /**
     * Removes the specified event listener from the cache if it was previously registered.
     *
     * @param listener the event listener that was previously registered for the cache, cannot be {@code null}.
     * @see CacheEventListener
     */
    void unregisterEventListener(@Nonnull CacheEventListener<K, V> listener);
}