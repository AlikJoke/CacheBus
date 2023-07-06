package net.cache.bus.core.impl;

import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.CacheEntryEventType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Immutable
@ThreadSafe
public record ImmutableCacheEntryEvent<K extends Serializable, V extends Serializable>(
        @Nonnull K key,
        @Nullable V oldValue,
        @Nullable V newValue,
        @Nonnull Instant eventTime,
        @Nonnull CacheEntryEventType eventType,
        @Nonnull String cacheName) implements CacheEntryEvent<K, V> {

    public ImmutableCacheEntryEvent {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(eventTime, "eventTime");
        Objects.requireNonNull(eventType, "eventType");

        if (cacheName == null || cacheName.isEmpty()) {
            throw new IllegalArgumentException("cacheName must be not empty");
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
                && cacheName.equals(that.cacheName)
                && Objects.equals(oldValue, that.oldValue)
                && Objects.equals(newValue, that.newValue)
                && eventTime.equals(that.eventTime);
    }

    @Override
    public int hashCode() {

        final int prime = 31;
        int result = 1;
        result = prime * result + key.hashCode();
        result = prime * result + eventType.hashCode();
        result = prime * result + cacheName.hashCode();
        result = prime * result + (oldValue == null ? 0 : oldValue.hashCode());
        result = prime * result + (newValue == null ? 0 : newValue.hashCode());
        result = prime * result + eventTime.hashCode();

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
