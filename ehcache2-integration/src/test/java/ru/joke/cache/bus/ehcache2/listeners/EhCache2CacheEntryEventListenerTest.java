package ru.joke.cache.bus.ehcache2.listeners;

import ru.joke.cache.bus.core.CacheEntryEvent;
import ru.joke.cache.bus.core.CacheEntryEventType;
import ru.joke.cache.bus.core.testing.BaseCacheEventListenerTest;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class EhCache2CacheEntryEventListenerTest extends BaseCacheEventListenerTest<Element, EhCache2CacheEntryEventListenerTest.ActionType> {

    @Mock
    private Ehcache cache;

    private EhCache2CacheEntryEventListener<String, String> listener;

    @Test
    public void testRemoveAllElements() {
        // action
        callListener(ActionType.REMOVED_ALL, Collections.singletonList(new Element(null, null)));

        // checks
        assertEquals(1, this.busEventCaptor.getAllValues().size(), "Count of sent to bus events must be 1");

        final var busEvent1 = this.busEventCaptor.getAllValues().get(0);
        makeResultBusEventChecks(busEvent1, CacheEntryEvent.ALL_ENTRIES_KEY, null, null, CacheEntryEventType.EVICTED);
    }

    @Override
    protected Element composeCacheEvent(
            final String key,
            final String oldVal,
            final String newVal,
            final ActionType eventType) {

        return new Element(key, eventType == ActionType.ADDED || eventType == ActionType.UPDATED ? newVal : oldVal);
    }

    @Override
    protected void callListener(ActionType eventType, List<Element> events) {
        events.forEach(e -> {
            switch (eventType) {
                case ADDED -> this.listener.notifyElementPut(this.cache, e);
                case UPDATED -> this.listener.notifyElementUpdated(this.cache, e);
                case EVICTED -> this.listener.notifyElementEvicted(this.cache, e);
                case EXPIRED -> this.listener.notifyElementExpired(this.cache, e);
                case REMOVED -> this.listener.notifyElementRemoved(this.cache, e);
                case REMOVED_ALL -> this.listener.notifyRemoveAll(this.cache);
            }
        });
    }

    @Override
    protected ActionType createdEventType() {
        return ActionType.ADDED;
    }

    @Override
    protected ActionType modifiedEventType() {
        return ActionType.UPDATED;
    }

    @Override
    protected ActionType removedEventType() {
        return ActionType.REMOVED;
    }

    @Override
    protected ActionType evictedEventType() {
        return ActionType.EVICTED;
    }

    @Override
    protected ActionType expiredEventType() {
        return ActionType.EXPIRED;
    }

    @Override
    protected void makePreparationActions() {
        when(this.cache.getName()).thenReturn(CACHE_NAME);
        this.listener = new EhCache2CacheEntryEventListener<>(UUID.randomUUID().toString(), this.cacheBus);
    }

    @Override
    protected boolean oldValueUnavailableForUpdateEvent() {
        return true;
    }

    enum ActionType {

        EXPIRED,

        REMOVED,

        EVICTED,

        ADDED,

        UPDATED,

        REMOVED_ALL
    }
}
