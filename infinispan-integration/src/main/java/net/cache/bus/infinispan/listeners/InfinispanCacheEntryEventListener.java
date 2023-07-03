package net.cache.bus.infinispan.listeners;

import net.cache.bus.core.CacheBus;
import net.cache.bus.core.CacheEntryEventType;
import net.cache.bus.core.impl.ImmutableCacheEntryEvent;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.*;
import org.infinispan.notifications.cachelistener.event.*;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Objects;

@Listener
public final class InfinispanCacheEntryEventListener {

    private final CacheBus cacheBus;

    public InfinispanCacheEntryEventListener(@Nonnull CacheBus cacheBus) {
        this.cacheBus = Objects.requireNonNull(cacheBus, "cacheBus");
    }

    @CacheEntryRemoved
    public void onEntryEvicted(@Nonnull CacheEntryRemovedEvent<?, ?> event) {
        sendToBus(event, event.getOldValue(), null, CacheEntryEventType.EVICTED);
    }

    @CacheEntryExpired
    public void onEntryExpired(@Nonnull CacheEntryExpiredEvent<?, ?> event) {
        sendToBus(event, event.getValue(), null, CacheEntryEventType.EXPIRED);
    }

    @CacheEntryCreated
    public void onEntryCreated(@Nonnull CacheEntryCreatedEvent<?, ?> event) {
        sendToBus(event, null, event.getValue(), CacheEntryEventType.ADDED);
    }

    @CacheEntryModified
    public void onEntryModified(@Nonnull CacheEntryModifiedEvent<?, ?> event) {
        sendToBus(event, event.getOldValue(), event.getNewValue(), CacheEntryEventType.UPDATED);
    }

    @CacheEntryInvalidated
    public void onEntryInvalidated(@Nonnull CacheEntryInvalidatedEvent<?, ?> event) {
        sendToBus(event, event.getValue(), null, CacheEntryEventType.EVICTED);
    }

    @CacheEntriesEvicted
    public void onEntriesEvicted(@Nonnull CacheEntriesEvictedEvent<?, ?> event) {
        final Instant eventTime = Instant.now();
        event.getEntries().forEach((key, value) -> {

            final net.cache.bus.core.CacheEntryEvent<?, ?> busEvent = new ImmutableCacheEntryEvent<>(
                    key,
                    value,
                    null,
                    eventTime,
                    CacheEntryEventType.EVICTED,
                    event.getCache().getName()
            );
            this.cacheBus.send(busEvent);
        });
    }

    private void sendToBus(
            final CacheEntryEvent<?, ?> event,
            final Object oldValue,
            final Object newValue,
            final CacheEntryEventType eventType) {

        final net.cache.bus.core.CacheEntryEvent<?, ?> busEvent = new ImmutableCacheEntryEvent<>(
                event.getKey(),
                oldValue,
                newValue,
                Instant.now(),
                eventType,
                event.getCache().getName()
        );
        this.cacheBus.send(busEvent);
    }
}
