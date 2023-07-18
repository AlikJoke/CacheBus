package net.cache.bus.infinispan.listeners;

import net.cache.bus.core.CacheEntryEventType;
import net.cache.bus.core.testing.BaseCacheEventListenerTest;
import org.infinispan.notifications.cachelistener.annotation.*;
import org.infinispan.notifications.cachelistener.event.Event;
import org.infinispan.notifications.cachelistener.event.impl.EventImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class InfinispanCacheEventListenerTest extends BaseCacheEventListenerTest<EventImpl<String, String>, Event.Type> {

    private static final String CACHE_NAME = "test";

    @Mock
    private org.infinispan.Cache<String, String> cache;

    private InfinispanCacheEntryEventListener<String, String> listener;

    @Test
    public void testInvalidatedEvents() {
        testPurgeFromCacheEvents(CacheEntryEventType.EVICTED, Event.Type.CACHE_ENTRY_INVALIDATED);
    }

    @Override
    protected void makePreparationActions() {
        when(this.cache.getName()).thenReturn(CACHE_NAME);

        this.listener = new InfinispanCacheEntryEventListener<>(UUID.randomUUID().toString(), this.cacheBus);
    }

    @Override
    protected final void callListener(Event.Type eventType, List<EventImpl<String, String>> events) {
        final Class<? extends Annotation> eventAnnotation = switch (eventType) {
            case CACHE_ENTRY_EVICTED -> CacheEntriesEvicted.class;
            case CACHE_ENTRY_CREATED -> CacheEntryCreated.class;
            case CACHE_ENTRY_REMOVED -> CacheEntryRemoved.class;
            case CACHE_ENTRY_MODIFIED -> CacheEntryModified.class;
            case CACHE_ENTRY_INVALIDATED -> CacheEntryInvalidated.class;
            case CACHE_ENTRY_EXPIRED -> CacheEntryExpired.class;
            default -> throw new IllegalStateException("Unexpected value: " + eventType);
        };
        callHandlerViaReflection(eventAnnotation, events);
    }

    @Override
    protected Event.Type createdEventType() {
        return Event.Type.CACHE_ENTRY_CREATED;
    }

    @Override
    protected Event.Type modifiedEventType() {
        return Event.Type.CACHE_ENTRY_MODIFIED;
    }

    @Override
    protected Event.Type removedEventType() {
        return Event.Type.CACHE_ENTRY_REMOVED;
    }

    @Override
    protected Event.Type evictedEventType() {
        return Event.Type.CACHE_ENTRY_EVICTED;
    }

    @Override
    protected Event.Type expiredEventType() {
        return Event.Type.CACHE_ENTRY_EXPIRED;
    }

    @Override
    protected EventImpl<String, String> composeCacheEvent(
            final String key,
            final String oldVal,
            final String newVal,
            final Event.Type eventType) {

        final EventImpl<String, String> event = EventImpl.createEvent(this.cache, eventType);
        event.setKey(key);
        event.setOldValue(oldVal);

        final boolean isPre = eventType == Event.Type.CACHE_ENTRY_REMOVED
                || eventType == Event.Type.CACHE_ENTRY_EVICTED
                || eventType == Event.Type.CACHE_ENTRY_INVALIDATED
                || eventType == Event.Type.CACHE_ENTRY_EXPIRED;
        event.setValue(isPre ? oldVal : newVal);
        if (eventType == Event.Type.CACHE_ENTRY_EVICTED) {
            event.setEntries(Map.of(key, oldVal));
        }

        return event;
    }

    private void callHandlerViaReflection(
            final Class<? extends Annotation> annotationCls,
            final List<EventImpl<String, String>> events) {
        final List<Method> methods =
                Arrays.stream(this.listener.getClass().getDeclaredMethods())
                        .filter(m -> m.isAnnotationPresent(annotationCls))
                        .toList();
        assertEquals(1, methods.size(), "Listener should has only one handler to each event type");

        try {
            for (EventImpl<String, String> event : events) {
                methods.get(0).invoke(listener, event);
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}