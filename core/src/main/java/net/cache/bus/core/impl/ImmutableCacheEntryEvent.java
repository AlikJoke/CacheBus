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
 * Неизменяемая реализация события об изменении элемента кэша.
 *
 * @param key       ключ измененного элемента, не может быть {@code null}.
 * @param oldValue  старое значение измененного элемента, может быть {@code null}.
 * @param newValue  новое значение измененного элемента, может быть {@code null}.
 * @param eventTime время изменения элемента кэша в миллисекундах, не может быть отрицательным.
 * @param eventType тип события (изменения), не может быть {@code null}.
 * @param cacheName имя кэша, в котором произошло изменение, не может быть {@code null}.
 * @param <K>       тип ключа элемента кэша
 * @param <V>       тип значения элемента кэша
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
