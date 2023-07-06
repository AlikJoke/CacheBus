package net.cache.bus.jsr107.listeners;

import net.cache.bus.core.CacheBus;
import net.cache.bus.core.CacheEntryEventType;
import net.cache.bus.core.CacheEventListener;
import net.cache.bus.core.impl.ImmutableCacheEntryEvent;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import javax.cache.event.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@ThreadSafe
@Immutable
final class JSR107CacheEntryEventListener<K extends Serializable, V extends Serializable>
        implements CacheEntryCreatedListener<K, V>, CacheEntryUpdatedListener<K, V>,
        CacheEntryExpiredListener<K, V>, CacheEntryRemovedListener<K, V>, CacheEventListener<K, V> {

    private final CacheBus cacheBus;

    public JSR107CacheEntryEventListener(@Nonnull CacheBus cacheBus) {
        this.cacheBus = Objects.requireNonNull(cacheBus, "cacheBus");
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

    private void sendToBus(final Iterable<CacheEntryEvent<? extends K, ? extends V>> iterable) {

        iterable.forEach(cacheEvent -> {

            final net.cache.bus.core.CacheEntryEvent<K, V> busEvent = new ImmutableCacheEntryEvent<>(
                    cacheEvent.getKey(),
                    cacheEvent.getOldValue(),
                    cacheEvent.getValue(),
                    Instant.now(),
                    convertJSR107CacheEventType2BusType(cacheEvent.getEventType()),
                    cacheEvent.getSource().getName()
            );
            this.cacheBus.send(busEvent);
        });
    }

    private CacheEntryEventType convertJSR107CacheEventType2BusType(final EventType eventType) {
        return switch (eventType) {
            case REMOVED -> CacheEntryEventType.EVICTED;
            case EXPIRED -> CacheEntryEventType.EXPIRED;
            case CREATED -> CacheEntryEventType.ADDED;
            case UPDATED -> CacheEntryEventType.UPDATED;
        };
    }
}
