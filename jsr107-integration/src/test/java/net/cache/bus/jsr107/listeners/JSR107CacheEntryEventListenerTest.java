package net.cache.bus.jsr107.listeners;

import net.cache.bus.core.testing.BaseCacheEventListenerTest;
import org.mockito.Mock;

import javax.cache.Cache;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.EventType;
import java.util.List;

import static org.mockito.Mockito.when;

public class JSR107CacheEntryEventListenerTest extends BaseCacheEventListenerTest<CacheEntryEvent<? extends String, ? extends String>, EventType> {

    @Mock
    private Cache<String, String> cache;

    private JSR107CacheEntryEventListener<String, String> listener;

    @Override
    protected CacheEntryEvent<? extends String, ? extends String> composeCacheEvent(
            final String key,
            final String oldVal,
            final String newVal,
            final EventType eventType) {

        return new CacheEntryEvent<>(this.cache, eventType) {
            @Override
            public String getValue() {
                return newVal;
            }

            @Override
            public String getOldValue() {
                return oldVal;
            }

            @Override
            public boolean isOldValueAvailable() {
                return oldVal != null;
            }

            @Override
            public String getKey() {
                return key;
            }

            @Override
            public <T> T unwrap(Class<T> aClass) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    protected final void callListener(EventType eventType, List<CacheEntryEvent<? extends String, ? extends String>> events) {

        switch (eventType) {
            case CREATED -> listener.onCreated(events);
            case UPDATED -> listener.onUpdated(events);
            case REMOVED -> listener.onRemoved(events);
            case EXPIRED -> listener.onExpired(events);
        }
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
        return EventType.REMOVED;
    }

    @Override
    protected EventType expiredEventType() {
        return EventType.EXPIRED;
    }

    @Override
    protected void makePreparationActions() {
        when(this.cache.getName()).thenReturn(CACHE_NAME);

        this.listener = new JSR107CacheEntryEventListener<>(this.cacheBus);
    }
}
