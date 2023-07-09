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

    private final CacheBus cacheBus;

    public InfinispanCacheEntryEventListener(@Nonnull CacheBus cacheBus) {
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
                    CacheEntryEventType.EVICTED,
                    event.getCache().getName()
            );
            this.cacheBus.send(busEvent);
        });
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
                eventType,
                event.getCache().getName()
        );
        this.cacheBus.send(busEvent);
    }
}
