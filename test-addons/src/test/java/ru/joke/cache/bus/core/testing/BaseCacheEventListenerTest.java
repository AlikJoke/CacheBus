package ru.joke.cache.bus.core.testing;

import ru.joke.cache.bus.core.CacheBus;
import ru.joke.cache.bus.core.CacheEntryEvent;
import ru.joke.cache.bus.core.CacheEntryEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.CONCURRENT)
public abstract class BaseCacheEventListenerTest<T, E> {

    protected static final String CACHE_NAME = "test";

    protected static final String KEY_1 = "1";
    protected static final String KEY_2 = "2";
    protected static final String VAL_1 = "v1";
    protected static final String VAL_2 = "v2";

    @Mock
    protected CacheBus cacheBus;
    @Captor
    protected ArgumentCaptor<CacheEntryEvent<String, String>> busEventCaptor;

    @BeforeEach
    public void prepareMocks() {
        doNothing().when(this.cacheBus).send(this.busEventCaptor.capture());
        makePreparationActions();
    }

    @Test
    public void testCreatedEvents() {
        // prepare data
        final var event1 = composeCacheEvent(KEY_1, null, VAL_1, createdEventType());
        final var event2 = composeCacheEvent(KEY_2, null, VAL_2, createdEventType());

        // action
        callListener(createdEventType(), List.of(event1, event2));

        // checks
        assertEquals(2, this.busEventCaptor.getAllValues().size(), "Count of sent to bus events must be 2");

        final var busEvent1 = this.busEventCaptor.getAllValues().get(0);
        makeResultBusEventChecks(busEvent1, KEY_1, VAL_1, null, CacheEntryEventType.ADDED);

        final var busEvent2 = this.busEventCaptor.getAllValues().get(1);
        makeResultBusEventChecks(busEvent2, KEY_2, VAL_2, null, CacheEntryEventType.ADDED);
    }

    @Test
    public void testUpdatedEvents() {
        // prepare data
        final var event1 = composeCacheEvent(KEY_1, VAL_1, VAL_2, modifiedEventType());
        final var event2 = composeCacheEvent(KEY_2, VAL_2, VAL_1, modifiedEventType());

        // action
        callListener(modifiedEventType(), List.of(event1, event2));

        // checks
        assertEquals(2, this.busEventCaptor.getAllValues().size(), "Count of sent to bus events must be 2");

        final var busEvent1 = this.busEventCaptor.getAllValues().get(0);
        makeResultBusEventChecks(busEvent1, KEY_1, VAL_2, VAL_1, CacheEntryEventType.UPDATED);

        final var busEvent2 = this.busEventCaptor.getAllValues().get(1);
        makeResultBusEventChecks(busEvent2, KEY_2, VAL_1, VAL_2, CacheEntryEventType.UPDATED);
    }

    @Test
    public void testExpiredEvents() {
        testPurgeFromCacheEvents(CacheEntryEventType.EXPIRED, expiredEventType());
    }

    @Test
    public void testRemovedEvents() {
        testPurgeFromCacheEvents(CacheEntryEventType.EVICTED, removedEventType());
    }

    @Test
    public void testEvictedEvents() {
        testPurgeFromCacheEvents(CacheEntryEventType.EVICTED, evictedEventType());
    }

    protected void testPurgeFromCacheEvents(final CacheEntryEventType busEventType, final E eventType) {
        // prepare data
        final var event1 = composeCacheEvent(KEY_1, VAL_1, null, eventType);
        final var event2 = composeCacheEvent(KEY_2, VAL_2, null, eventType);

        // action
        callListener(eventType, List.of(event1, event2));

        // checks
        assertEquals(2, this.busEventCaptor.getAllValues().size(), "Count of sent to bus events must be 2");

        final var busEvent1 = this.busEventCaptor.getAllValues().get(0);
        makeResultBusEventChecks(busEvent1, KEY_1, null, VAL_1, busEventType);

        final var busEvent2 = this.busEventCaptor.getAllValues().get(1);
        makeResultBusEventChecks(busEvent2, KEY_2, null, VAL_2, busEventType);
    }

    protected void makeResultBusEventChecks(
            final CacheEntryEvent<String, String> busEvent,
            final String key,
            final String newValue,
            final String oldValue,
            final CacheEntryEventType eventType) {

        assertEquals(key, busEvent.key(), "Keys must be equal");
        assertEquals(newValue, busEvent.newValue(), "New values must be equal");

        final String oldValueToCompare = oldValueUnavailableForUpdateEvent() && eventType == CacheEntryEventType.UPDATED ? null : oldValue;
        assertEquals(oldValueToCompare, busEvent.oldValue(), "Old values must be equal");
        assertEquals(eventType, busEvent.eventType(), "Event types must be equal");
        assertEquals(CACHE_NAME, busEvent.cacheName(), "Cache names must be equal");
    }

    protected abstract T composeCacheEvent(
            final String key,
            final String oldVal,
            final String newVal,
            final E eventType
    );

    protected abstract void makePreparationActions();

    protected abstract void callListener(E eventType, List<T> events);

    protected abstract E createdEventType();

    protected abstract E modifiedEventType();

    protected abstract E removedEventType();

    protected abstract E evictedEventType();

    protected abstract E expiredEventType();

    protected boolean oldValueUnavailableForUpdateEvent() {
        return false;
    }
}
