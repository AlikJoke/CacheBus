package net.cache.bus.core;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * Cache element change event.
 *
 * @param <K> the cache key type, must be serializable
 * @param <V> the cache value type, must serializable
 * @author Alik
 * @see CacheEntryEventType
 */
public interface CacheEntryEvent<K extends Serializable, V extends Serializable> {

    String ALL_ENTRIES_KEY = "*";

    /**
     * Returns the key of the changed cache element.
     * If the key value matches {@linkplain CacheEntryEvent#ALL_ENTRIES_KEY}, the change applies to all cache elements.
     *
     * @return cannot be {@code null}.
     */
    @Nonnull
    K key();

    /**
     * Returns the old value of the cache element.
     *
     * @return the old value of the cache element before modification, can be {@code null}.
     */
    @Nullable
    V oldValue();

    /**
     * Returns the new value the cache element.
     *
     * @return the new value of the cache element after modification, can be {@code null}.
     */
    @Nullable
    V newValue();

    /**
     * Returns the timestamp of the cache element change (in milliseconds relative to UTC).
     *
     * @return the timestamp of the cache element change in milliseconds
     */
    long eventTime();

    /**
     * Returns the type of the cache element change.
     *
     * @return cannot be {@code null}.
     * @see CacheEntryEventType
     */
    @Nonnull
    CacheEntryEventType eventType();

    /**
     * Returns the name of the cache in which the cache element change occurred.
     *
     * @return cannot be {@code null}.
     */
    @Nonnull
    String cacheName();

    /**
     * Returns the hash key of the event.
     *
     * @return the hash key of the event
     */
    default int computeEventHashKey() {
        int result = 31 + cacheName().hashCode();
        return 31 * result + key().hashCode();
    }

    /**
     * Applies the cache item change event to the invalidation cache.<br>
     * The logic of applying the event involves removing the item with the key from the local cache.
     *
     * @param cache the invalidation cache, cannot be {@code null}.
     */
    default void applyToInvalidatedCache(@Nonnull Cache<K, V> cache) {
        processEviction(cache);
    }

    /**
     * Applies the cache change event to the replicated cache.<br>
     *
     * <ul>
     * <li>If the event is an add event, the replacement value (new value) is always taken from the event.
     * Then, during the merge, we only replace the value in the local cache if there is value in the cache on the current server;
     * if there is, we compare the value in the local cache with the value in the event: if they match, we keep the existing value in the cache,
     * otherwise, we clear the data from the local cache to resolve the conflict and avoid inconsistency.</li>
     * <li>If the event is a modification event of a cache item, the replacement value is taken from the new event
     * only if the old value from the event matches the value of the item on this server;
     * otherwise, if the value in the local cache matches the new value in the event, we keep the value from the local cache unchanged;
     * otherwise, there is a conflict and we clear the data from the cache to remove the item from the cache on this server.
     * This incurs some overhead if such situations are frequent, but it ensures data integrity in the cache.
     * In normal situations, this should be a rare scenario.</li>
     * <li>If the event is a delete or expiration event (i.e., the new value is absent general),
     * we remove the item from the current local cache.</li>
     * <ul>
     *
     * @param cache the replicated cache, cannot be {@code null}.
     */
    default void applyToReplicatedCache(@Nonnull Cache<K, V> cache) {
        final V newVal = newValue();
        if (newVal == null) {
            processEviction(cache);
        } else {
            final V oldValueFromEvent = oldValue();
            cache.merge(
                    key(),
                    newVal,
                    (oldLocalValue, newValueFromEvent) ->
                            newValueFromEvent.equals(oldLocalValue)
                                    ? oldLocalValue
                                    : oldLocalValue.equals(oldValueFromEvent)
                                        ? newValueFromEvent
                                        : null
            );
        }
    }

    private void processEviction(@Nonnull Cache<K, V> cache) {
        if (ALL_ENTRIES_KEY.equals(key())) {
            cache.clear();
        } else {
            cache.evict(key());
        }
    }
}
