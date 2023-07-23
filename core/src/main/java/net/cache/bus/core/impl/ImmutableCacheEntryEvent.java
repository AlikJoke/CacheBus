package net.cache.bus.core.impl;

import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.CacheEntryEventType;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable implementation of a cache element change event*
 *
 * @param key       the key of the modified element, cannot be {@code null}.
 * @param oldValue  the old value the modified element, can be {@code null}.
 * @param newValue  the new value of the modified element, can be {@code null}.
 * @param eventTime the time of cache element change in milliseconds, cannot be negative.
 * @param eventType the type of event (change), cannot be {@code null}.
 * @param cacheName the name of the cache where the change occurred, cannot be {@code null}.
 * @param <K>       the type of the cache element key
 * @param <V>       the type of the cache element value
 * @author Alik
 * @see CacheEntryEvent
 */
@Immutable
@ThreadSafe
public record ImmutableCacheEntryEvent<K extends Serializable, V extends Serializable>(
        @Nonnull K key,
        @Nullable V oldValue,
        @Nullable V newValue,
        @Nonnegative long eventTime,
        @Nonnull CacheEntryEventType eventType,
        @Nonnull String cacheName) implements CacheEntryEvent<K, V> {

    public ImmutableCacheEntryEvent(
            @Nonnull K key,
            @Nullable V oldValue,
            @Nullable V newValue,
            @Nonnull CacheEntryEventType eventType,
            @Nonnull String cacheName) {
        this(key, oldValue, newValue, System.currentTimeMillis(), eventType, cacheName);
    }

    public ImmutableCacheEntryEvent {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(eventType, "eventType");

        if (cacheName == null || cacheName.isEmpty()) {
            throw new IllegalArgumentException("cacheName must be not empty");
        }

        if (eventTime <= 0) {
            throw new IllegalArgumentException("eventTime must be positive");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ImmutableCacheEntryEvent<?, ?> that = (ImmutableCacheEntryEvent<?, ?>) o;
        return key.equals(that.key)
                && eventType == that.eventType
                && eventTime == that.eventTime
                && cacheName.equals(that.cacheName)
                && Objects.equals(oldValue, that.oldValue)
                && Objects.equals(newValue, that.newValue);
    }

    @Override
    public int hashCode() {

        final int prime = 31;
        int result = 1;
        result = prime * result + key.hashCode();
        result = prime * result + eventType.hashCode();
        result = prime * result + cacheName.hashCode();
        result = prime * result + Long.hashCode(eventTime);
        result = prime * result + (oldValue == null ? 0 : oldValue.hashCode());
        result = prime * result + (newValue == null ? 0 : newValue.hashCode());

        return result;
    }

    @Override
    public String toString() {
        return "ImmutableCacheEntryEvent{" +
                "key=" + key +
                ", oldValue=" + oldValue +
                ", newValue=" + newValue +
                ", eventTime=" + eventTime +
                ", eventType=" + eventType +
                ", cacheName='" + cacheName + '\'' +
                '}';
    }
}
