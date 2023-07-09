package net.cache.bus.ehcache3.listeners;

import net.cache.bus.core.testing.BaseCacheEventListenerTest;
import org.ehcache.Cache;
import org.ehcache.event.CacheEvent;
import org.ehcache.event.EventType;
import org.mockito.Mock;

import java.util.List;

public class EhCache3CacheEntryEventListenerTest extends BaseCacheEventListenerTest<CacheEvent<? extends String, ? extends String>, EventType> {

    @Mock
    private Cache<String, String> cache;

    private EhCache3CacheEntryEventListener<String, String> listener;

    @Override
    protected CacheEvent<? extends String, ? extends String> composeCacheEvent(
            final String key,
            final String oldVal,
            final String newVal,
            final EventType eventType) {

        return new CacheEvent<>() {

            @Override
            public String getOldValue() {
                return oldVal;
            }

            @Override
            public Cache<String, String> getSource() {
                return cache;
            }

            @Override
            public EventType getType() {
                return eventType;
            }

            @Override
            public String getKey() {
                return key;
            }

            @Override
            public String getNewValue() {
                return newVal;
            }
        };
    }

    @Override
    protected final void callListener(EventType eventType, List<CacheEvent<? extends String, ? extends String>> events) {
        events.forEach(listener::onEvent);
    }

    @Override
    protected EventType createdEventType() {
        return EventType.CREATED;
    }

    @Override
    protected EventType modifiedEventType() {
        return EventType.UPDATED;
    }

    @Override
    protected EventType removedEventType() {
        return EventType.REMOVED;
    }

    @Override
    protected EventType evictedEventType() {
        return EventType.EVICTED;
    }

    @Override
    protected EventType expiredEventType() {
        return EventType.EXPIRED;
    }

    @Override
    protected void makePreparationActions() {
        this.listener = new EhCache3CacheEntryEventListener<>(this.cacheBus, CACHE_NAME);
    }
}
