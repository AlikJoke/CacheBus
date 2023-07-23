package ru.joke.cache.bus.core;

import ru.joke.cache.bus.core.configuration.CacheConfiguration;

import javax.annotation.Nonnull;

/**
 * Storage for modification timestamps of cache elements, used to track concurrent updates of local cache
 * elements by application threads and bus threads applying changes cache elements from other servers.
 *
 * @author Alik
 */
public interface CacheEntryEventTimestampStore {

    /**
     * Stores the timestamp information from the event in the storage if at the time of storing
     * there is no more recent timestamp in the storage than the event. If such a timestamp exists
     * in the storage (or appears at the time storing), the timestamp is not stored
     * (in other words, this situation means that applying this event the local cache is not required).
     *
     * @param event the event to apply and store the timestamp from; cannot be {@code null}.
     * @return {@code true} if there no more recent timestamp in the storage at the moment
     * and the timestamp is successfully stored, {@code false} otherwise (i.e., the timestamp was not stored).
     * @see CacheEntryEvent#eventTime()
     * @see CacheConfiguration#useTimestampBasedComparison()
     */
    boolean save(@Nonnull CacheEntryEvent<?, ?> event);
}
