package net.cache.bus.ehcache3.listeners;

import net.cache.bus.core.CacheBus;
import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.CacheEntryEventType;
import net.cache.bus.core.impl.ImmutableCacheEntryEvent;
import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;
import org.ehcache.event.EventType;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.Objects;

@ThreadSafe
@Immutable
final class EhCache3CacheEntryEventListener<K extends Serializable, V extends Serializable> implements CacheEventListener<K, V>, net.cache.bus.core.CacheEventListener<K, V> {

    private final String listenerId;
    private final CacheBus cacheBus;
    private final String cacheName;

    public EhCache3CacheEntryEventListener(
            @Nonnull String listenerId,
            @Nonnull CacheBus cacheBus,
            @Nonnull String cacheName) {
        this.listenerId = Objects.requireNonNull(listenerId, "listenerId");
        this.cacheBus = Objects.requireNonNull(cacheBus, "cacheBus");
        this.cacheName = Objects.requireNonNull(cacheName, "cacheName");
    }

    @Override
    public void onEvent(@Nonnull CacheEvent<? extends K, ? extends V> cacheEvent) {
        final CacheEntryEvent<K, V> busEvent = new ImmutableCacheEntryEvent<>(
                cacheEvent.getKey(),
                cacheEvent.getOldValue(),
                cacheEvent.getNewValue(),
                System.currentTimeMillis(),
                convertEhCacheEventType2BusType(cacheEvent.getType()),
                this.cacheName
        );
        this.cacheBus.send(busEvent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final EhCache3CacheEntryEventListener<?, ?> that = (EhCache3CacheEntryEventListener<?, ?>) o;

        return listenerId.equals(that.listenerId)
                && cacheName.equals(that.cacheName)
                && cacheBus.equals(that.cacheBus);
    }

    @Override
    public int hashCode() {
        int result = listenerId.hashCode();
        result = 31 * result + cacheBus.hashCode();
        result = 31 * result + cacheName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "EhCache3CacheEntryEventListener{" +
                "listenerId='" + listenerId + '\'' +
                ", cacheName='" + cacheName +
                '}';
    }

    private CacheEntryEventType convertEhCacheEventType2BusType(final EventType eventType) {
        return switch (eventType) {
            case EVICTED, REMOVED -> CacheEntryEventType.EVICTED;
            case EXPIRED -> CacheEntryEventType.EXPIRED;
            case CREATED -> CacheEntryEventType.ADDED;
            case UPDATED -> CacheEntryEventType.UPDATED;
        };
    }
}
