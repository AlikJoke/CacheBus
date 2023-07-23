package ru.joke.cache.bus.ehcache2.listeners;

import ru.joke.cache.bus.core.CacheBus;
import ru.joke.cache.bus.core.CacheEntryEvent;
import ru.joke.cache.bus.core.CacheEntryEventType;
import ru.joke.cache.bus.core.CacheEventListener;
import ru.joke.cache.bus.core.impl.ImmutableCacheEntryEvent;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListenerAdapter;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.Objects;

@ThreadSafe
@Immutable
final class EhCache2CacheEntryEventListener<K extends Serializable, V extends Serializable> extends CacheEventListenerAdapter implements CacheEventListener<K, V> {

    private final String listenerId;
    private final CacheBus cacheBus;

    public EhCache2CacheEntryEventListener(@Nonnull String listenerId, @Nonnull CacheBus cacheBus) {
        this.cacheBus = Objects.requireNonNull(cacheBus, "cacheBus");
        this.listenerId = Objects.requireNonNull(listenerId, "listenerId");
    }

    @Override
    public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
        notifyElementEvicted(cache, element);
    }

    @Override
    public void notifyElementPut(@Nonnull Ehcache cache, @Nonnull Element element) throws CacheException {
        @SuppressWarnings("unchecked")
        final V newVal = (V) element.getObjectValue();
        final CacheEntryEvent<?, V> event = composeCacheEntryEvent(
                (Serializable) element.getObjectKey(),
                null,
                newVal,
                CacheEntryEventType.ADDED,
                cache.getName()
        );
        this.cacheBus.send(event);
    }

    @Override
    public void notifyElementUpdated(@Nonnull Ehcache cache, @Nonnull Element element) throws CacheException {
        @SuppressWarnings("unchecked")
        final V newVal = (V) element.getObjectValue();
        final CacheEntryEvent<?, V> event = composeCacheEntryEvent(
                (Serializable) element.getObjectKey(),
                null,
                newVal,
                CacheEntryEventType.UPDATED,
                cache.getName()
        );
        this.cacheBus.send(event);
    }

    @Override
    public void notifyElementExpired(@Nonnull Ehcache cache, @Nonnull Element element) {
        @SuppressWarnings("unchecked")
        final V oldVal = (V) element.getObjectValue();
        final CacheEntryEvent<?, V> event = composeCacheEntryEvent(
                (Serializable) element.getObjectKey(),
                oldVal,
                null,
                CacheEntryEventType.EXPIRED,
                cache.getName()
        );
        this.cacheBus.send(event);
    }

    @Override
    public void notifyElementEvicted(@Nonnull Ehcache cache, @Nonnull Element element) {
        @SuppressWarnings("unchecked")
        final V oldVal = (V) element.getObjectValue();
        final CacheEntryEvent<?, V> event = composeCacheEntryEvent(
                (Serializable) element.getObjectKey(),
                oldVal,
                null,
                CacheEntryEventType.EVICTED,
                cache.getName()
        );
        this.cacheBus.send(event);
    }

    @Override
    public void notifyRemoveAll(@Nonnull Ehcache cache) {
        final CacheEntryEvent<?, V> event = composeCacheEntryEvent(
                CacheEntryEvent.ALL_ENTRIES_KEY,
                null,
                null,
                CacheEntryEventType.EVICTED,
                cache.getName()
        );
        this.cacheBus.send(event);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final EhCache2CacheEntryEventListener<?, ?> that = (EhCache2CacheEntryEventListener<?, ?>) o;
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
        return "EhCache2CacheEntryEventListener{" +
                "listenerId='" + listenerId +
                '}';
    }

    private CacheEntryEvent<Serializable, V> composeCacheEntryEvent(
            final Serializable key,
            final V oldValue,
            final V newValue,
            final CacheEntryEventType eventType,
            final String cacheName) {

        return new ImmutableCacheEntryEvent<>(
                key,
                oldValue,
                newValue,
                System.currentTimeMillis(),
                eventType,
                cacheName
        );
    }
}
