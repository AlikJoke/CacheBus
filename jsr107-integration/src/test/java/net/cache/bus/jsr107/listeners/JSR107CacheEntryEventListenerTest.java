package net.cache.bus.jsr107.listeners;

import net.cache.bus.core.CacheBus;
import net.cache.bus.core.CacheEntryEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.cache.Cache;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.EventType;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JSR107CacheEntryEventListenerTest {

    private static final String CACHE_NAME = "test";

    private static final String KEY_1 = "1";
    private static final String KEY_2 = "2";
    private static final String VAL_1 = "v1";
    private static final String VAL_2 = "v2";

    @Mock
    private CacheBus cacheBus;
    @Mock
    private Cache<String, String> cache;
    @Captor
    private ArgumentCaptor<net.cache.bus.core.CacheEntryEvent<String, String>> busEventCaptor;

    private JSR107CacheEntryEventListener<String, String> listener;

    @BeforeEach
    public void prepareMocks() {
        doNothing().when(this.cacheBus).send(this.busEventCaptor.capture());
        when(this.cache.getName()).thenReturn(CACHE_NAME);

        this.listener = new JSR107CacheEntryEventListener<>(this.cacheBus);
    }

    @Test
    public void testCreatedEvents() {
        // prepare data
        final var event1 = composeCacheEvent(KEY_1, null, VAL_1, EventType.CREATED);
        final var event2 = composeCacheEvent(KEY_2, null, VAL_2, EventType.CREATED);

        // action
        listener.onCreated(List.of(event1, event2));

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
        final var event1 = composeCacheEvent(KEY_1, VAL_1, VAL_2, EventType.UPDATED);
        final var event2 = composeCacheEvent(KEY_2, VAL_2, VAL_1, EventType.UPDATED);

        // action
        listener.onUpdated(List.of(event1, event2));

        // checks
        assertEquals(2, this.busEventCaptor.getAllValues().size(), "Count of sent to bus events must be 2");

        final var busEvent1 = this.busEventCaptor.getAllValues().get(0);
        makeResultBusEventChecks(busEvent1, KEY_1, VAL_2, VAL_1, CacheEntryEventType.UPDATED);

        final var busEvent2 = this.busEventCaptor.getAllValues().get(1);
        makeResultBusEventChecks(busEvent2, KEY_2, VAL_1, VAL_2, CacheEntryEventType.UPDATED);
    }

    @Test
    public void testExpiredEvents() {
        testPurgeFromCacheEvents(CacheEntryEventType.EXPIRED, EventType.EXPIRED);
    }

    @Test
    public void testRemovedEvents() {
        testPurgeFromCacheEvents(CacheEntryEventType.EVICTED, EventType.REMOVED);
    }

    public void testPurgeFromCacheEvents(final CacheEntryEventType busEventType, final EventType eventType) {
        // prepare data
        final var event1 = composeCacheEvent(KEY_1, VAL_1, null, eventType);
        final var event2 = composeCacheEvent(KEY_2, VAL_2, null, eventType);

        // action
        listener.onExpired(List.of(event1, event2));

        // checks
        assertEquals(2, this.busEventCaptor.getAllValues().size(), "Count of sent to bus events must be 2");

        final var busEvent1 = this.busEventCaptor.getAllValues().get(0);
        makeResultBusEventChecks(busEvent1, KEY_1, null, VAL_1, busEventType);

        final var busEvent2 = this.busEventCaptor.getAllValues().get(1);
        makeResultBusEventChecks(busEvent2, KEY_2, null, VAL_2, busEventType);
    }

    private void makeResultBusEventChecks(
            final net.cache.bus.core.CacheEntryEvent<String, String> busEvent,
            final String key,
            final String newValue,
            final String oldValue,
            final CacheEntryEventType eventType) {

        assertEquals(key, busEvent.key(), "Keys must be equal");
        assertEquals(newValue, busEvent.newValue(), "New values must be equal");
        assertEquals(oldValue, busEvent.oldValue(), "Old values must be equal");
        assertEquals(eventType, busEvent.eventType(), "Event types must be equal");
        assertNotNull(busEvent.eventTime(), "Event time must be not null");
        assertEquals(CACHE_NAME, busEvent.cacheName(), "Cache names must be equal");
    }

    private CacheEntryEvent<? extends String, ? extends String> composeCacheEvent(
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
}
