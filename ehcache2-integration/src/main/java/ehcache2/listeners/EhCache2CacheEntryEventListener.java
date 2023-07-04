package ehcache2.listeners;

import net.cache.bus.core.CacheBus;
import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.CacheEntryEventType;
import net.cache.bus.core.CacheEventListener;
import net.cache.bus.core.impl.ImmutableCacheEntryEvent;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListenerAdapter;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.Objects;

@ThreadSafe
@Immutable
final class EhCache2CacheEntryEventListener<K, V> extends CacheEventListenerAdapter implements CacheEventListener<K, V> {

    private final CacheBus cacheBus;

    public EhCache2CacheEntryEventListener(@Nonnull CacheBus cacheBus) {
        this.cacheBus = Objects.requireNonNull(cacheBus, "cacheBus");
    }

    @Override
    public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
        notifyElementEvicted(cache, element);
    }

    @Override
    public void notifyElementPut(@Nonnull Ehcache cache, @Nonnull Element element) throws CacheException {
        final CacheEntryEvent<?, ?> event = composeCacheEntryEvent(
                element.getObjectKey(),
                null,
                element.getObjectValue(),
                CacheEntryEventType.ADDED,
                cache.getName()
        );
        this.cacheBus.send(event);
    }

    @Override
    public void notifyElementUpdated(@Nonnull Ehcache cache, @Nonnull Element element) throws CacheException {
        final CacheEntryEvent<?, ?> event = composeCacheEntryEvent(
                element.getObjectKey(),
                null,
                element.getObjectValue(),
                CacheEntryEventType.UPDATED,
                cache.getName()
        );
        this.cacheBus.send(event);
    }

    @Override
    public void notifyElementExpired(@Nonnull Ehcache cache, @Nonnull Element element) {
        final CacheEntryEvent<?, ?> event = composeCacheEntryEvent(
                element.getObjectKey(),
                element.getObjectValue(),
                null,
                CacheEntryEventType.EXPIRED,
                cache.getName()
        );
        this.cacheBus.send(event);
    }

    @Override
    public void notifyElementEvicted(@Nonnull Ehcache cache, @Nonnull Element element) {
        final CacheEntryEvent<?, ?> event = composeCacheEntryEvent(
                element.getObjectKey(),
                element.getObjectValue(),
                null,
                CacheEntryEventType.EVICTED,
                cache.getName()
        );
        this.cacheBus.send(event);
    }

    @Override
    public void notifyRemoveAll(@Nonnull Ehcache cache) {
        final CacheEntryEvent<?, ?> event = composeCacheEntryEvent(
                CacheEntryEvent.ALL_ENTRIES_KEY,
                null,
                null,
                CacheEntryEventType.EVICTED,
                cache.getName()
        );
        this.cacheBus.send(event);
    }

    private CacheEntryEvent<?, ?> composeCacheEntryEvent(
            final Object key,
            final Object oldValue,
            final Object newValue,
            final CacheEntryEventType eventType,
            final String cacheName) {

        return new ImmutableCacheEntryEvent<>(
                key,
                oldValue,
                newValue,
                Instant.now(),
                eventType,
                cacheName
        );
    }
}
