package net.cache.bus.infinispan.listeners;

import net.cache.bus.core.CacheBus;
import net.cache.bus.core.CacheEntryEventType;
import net.cache.bus.core.CacheEventListener;
import net.cache.bus.core.impl.ImmutableCacheEntryEvent;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.*;
import org.infinispan.notifications.cachelistener.event.*;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Objects;

@Listener
final class InfinispanCacheEntryEventListener<K extends Serializable, V extends Serializable> implements CacheEventListener<K, V> {

    private final String listenerId;
    private final CacheBus cacheBus;

    public InfinispanCacheEntryEventListener(@Nonnull String listenerId, @Nonnull CacheBus cacheBus) {
        this.listenerId = Objects.requireNonNull(listenerId, "listenerId");
        this.cacheBus = Objects.requireNonNull(cacheBus, "cacheBus");
    }

    @CacheEntryRemoved
    public void onEntryEvicted(@Nonnull CacheEntryRemovedEvent<K, V> event) {
        sendToBus(event, event.getOldValue(), null, CacheEntryEventType.EVICTED);
    }

    @CacheEntryExpired
    public void onEntryExpired(@Nonnull CacheEntryExpiredEvent<K, V> event) {
        sendToBus(event, event.getValue(), null, CacheEntryEventType.EXPIRED);
    }

    @CacheEntryCreated
    public void onEntryCreated(@Nonnull CacheEntryCreatedEvent<K, V> event) {
        sendToBus(event, null, event.getValue(), CacheEntryEventType.ADDED);
    }

    @CacheEntryModified
    public void onEntryModified(@Nonnull CacheEntryModifiedEvent<K, V> event) {
        sendToBus(event, event.getOldValue(), event.getNewValue(), CacheEntryEventType.UPDATED);
    }

    @CacheEntryInvalidated
    public void onEntryInvalidated(@Nonnull CacheEntryInvalidatedEvent<K, V> event) {
        sendToBus(event, event.getValue(), null, CacheEntryEventType.EVICTED);
    }

    @CacheEntriesEvicted
    public void onEntriesEvicted(@Nonnull CacheEntriesEvictedEvent<K, V> event) {
        event.getEntries().forEach((key, value) -> {

            final net.cache.bus.core.CacheEntryEvent<K, V> busEvent = new ImmutableCacheEntryEvent<>(
                    key,
                    value,
                    null,
                    System.currentTimeMillis(),
                    CacheEntryEventType.EVICTED,
                    event.getCache().getName()
            );
            this.cacheBus.send(busEvent);
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final InfinispanCacheEntryEventListener<?, ?> that = (InfinispanCacheEntryEventListener<?, ?>) o;
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
        return "InfinispanCacheEntryEventListener{" +
                "listenerId='" + listenerId +
                '}';
    }

    private void sendToBus(
            final CacheEntryEvent<K, V> event,
            final V oldValue,
            final V newValue,
            final CacheEntryEventType eventType) {

        final net.cache.bus.core.CacheEntryEvent<K, V> busEvent = new ImmutableCacheEntryEvent<>(
                event.getKey(),
                oldValue,
                newValue,
                System.currentTimeMillis(),
                eventType,
                event.getCache().getName()
        );
        this.cacheBus.send(busEvent);
    }
}
