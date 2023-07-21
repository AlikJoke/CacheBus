package net.cache.bus.jcache.listeners;

import net.cache.bus.core.CacheBus;
import net.cache.bus.core.CacheEntryEventType;
import net.cache.bus.core.CacheEventListener;
import net.cache.bus.core.impl.ImmutableCacheEntryEvent;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import javax.cache.event.*;
import java.io.Serializable;
import java.util.Objects;

@ThreadSafe
@Immutable
final class JCacheCacheEntryEventListener<K extends Serializable, V extends Serializable>
        implements CacheEntryCreatedListener<K, V>, CacheEntryUpdatedListener<K, V>,
        CacheEntryExpiredListener<K, V>, CacheEntryRemovedListener<K, V>, CacheEventListener<K, V> {

    private final String listenerId;
    private final CacheBus cacheBus;

    public JCacheCacheEntryEventListener(@Nonnull String listenerId, @Nonnull CacheBus cacheBus) {
        this.cacheBus = Objects.requireNonNull(cacheBus, "cacheBus");
        this.listenerId = Objects.requireNonNull(listenerId, "listenerId");
    }

    @Override
    public void onCreated(@Nonnull Iterable<CacheEntryEvent<? extends K, ? extends V>> iterable) throws CacheEntryListenerException {
        sendToBus(iterable);
    }

    @Override
    public void onExpired(Iterable<CacheEntryEvent<? extends K, ? extends V>> iterable) throws CacheEntryListenerException {
        sendToBus(iterable);
    }

    @Override
    public void onRemoved(Iterable<CacheEntryEvent<? extends K, ? extends V>> iterable) throws CacheEntryListenerException {
        sendToBus(iterable);
    }

    @Override
    public void onUpdated(Iterable<CacheEntryEvent<? extends K, ? extends V>> iterable) throws CacheEntryListenerException {
        sendToBus(iterable);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final JCacheCacheEntryEventListener<?, ?> that = (JCacheCacheEntryEventListener<?, ?>) o;
        return listenerId.equals(that.listenerId) && cacheBus.equals(that.cacheBus);
    }

    @Override
    public int hashCode() {
        int result = listenerId.hashCode();
        result = 31 * result + cacheBus.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "JCacheCacheEntryEventListener{" +
                "listenerId='" + listenerId +
                '}';
    }

    private void sendToBus(final Iterable<CacheEntryEvent<? extends K, ? extends V>> iterable) {

        iterable.forEach(cacheEvent -> {

            final net.cache.bus.core.CacheEntryEvent<K, V> busEvent = new ImmutableCacheEntryEvent<>(
                    cacheEvent.getKey(),
                    cacheEvent.getOldValue(),
                    cacheEvent.getValue(),
                    System.currentTimeMillis(),
                    convertJCacheEventType2BusType(cacheEvent.getEventType()),
                    cacheEvent.getSource().getName()
            );
            this.cacheBus.send(busEvent);
        });
    }

    private CacheEntryEventType convertJCacheEventType2BusType(final EventType eventType) {
        return switch (eventType) {
            case REMOVED -> CacheEntryEventType.EVICTED;
            case EXPIRED -> CacheEntryEventType.EXPIRED;
            case CREATED -> CacheEntryEventType.ADDED;
            case UPDATED -> CacheEntryEventType.UPDATED;
        };
    }
}
