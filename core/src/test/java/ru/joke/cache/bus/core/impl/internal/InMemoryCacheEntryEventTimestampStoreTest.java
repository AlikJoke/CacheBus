package ru.joke.cache.bus.core.impl.internal;

import ru.joke.cache.bus.core.CacheEntryEvent;
import ru.joke.cache.bus.core.CacheEntryEventType;
import ru.joke.cache.bus.core.configuration.CacheConfiguration;
import ru.joke.cache.bus.core.configuration.CacheType;
import ru.joke.cache.bus.core.impl.ImmutableCacheEntryEvent;
import ru.joke.cache.bus.core.impl.configuration.ImmutableCacheConfiguration;
import ru.joke.cache.bus.core.impl.configuration.ImmutableTimestampCacheConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InMemoryCacheEntryEventTimestampStoreTest {

    private static final String CACHE_NAME = "test";

    @Test
    public void testSave() {

        final InMemoryCacheEntryEventTimestampStore store = createTestStore(false);
        final CacheEntryEvent<Integer, String> addEvent = new ImmutableCacheEntryEvent<>(1, null, "v1", System.currentTimeMillis(), CacheEntryEventType.ADDED, CACHE_NAME);

        assertTrue(store.save(addEvent), "Timestamp must be saved to store");
        assertFalse(store.save(addEvent), "Timestamp must not be updated in store");

        final CacheEntryEvent<Integer, String> updateEvent = new ImmutableCacheEntryEvent<>(1, "v1", "v2", System.currentTimeMillis() + 10, CacheEntryEventType.UPDATED, CACHE_NAME);
        assertTrue(store.save(updateEvent), "Timestamp must be updated in store");
        assertFalse(store.save(updateEvent), "Timestamp must not be updated in store");
        assertFalse(store.save(addEvent), "Timestamp must not be updated in store");
    }

    @Test
    public void testSaveSpecialAllKey() {
        final InMemoryCacheEntryEventTimestampStore store = createTestStore(false);
        final CacheEntryEvent<String, String> event = new ImmutableCacheEntryEvent<>("*", null, null, System.currentTimeMillis(), CacheEntryEventType.EVICTED, CACHE_NAME);

        assertTrue(store.save(event), "Save must return true");
        assertTrue(store.save(event), "Save must return true");
    }

    @Test
    public void testSyncCleaning() throws InterruptedException {

        final InMemoryCacheEntryEventTimestampStore store = createTestStore(false);
        for (int i = 0; i < 256; i++) {
            final CacheEntryEvent<Integer, String> event = new ImmutableCacheEntryEvent<>(i, null, null, System.currentTimeMillis(), CacheEntryEventType.ADDED, CACHE_NAME);

            assertTrue(store.save(event), "Timestamp must be stored");
        }

        Thread.sleep(101);

        for (int i = 0; i < 256; i++) {
            final CacheEntryEvent<Integer, String> event = new ImmutableCacheEntryEvent<>(i, null, null, System.currentTimeMillis(), CacheEntryEventType.ADDED, CACHE_NAME);

            assertTrue(store.save(event), "Timestamp must be stored");
        }
    }

    @Test
    public void testAsyncCleaning() throws InterruptedException {
        final InMemoryCacheEntryEventTimestampStore store = createTestStore(true);
        for (int i = 0; i < 256; i++) {
            final CacheEntryEvent<Integer, String> event = new ImmutableCacheEntryEvent<>(i, null, null, System.currentTimeMillis(), CacheEntryEventType.ADDED, CACHE_NAME);

            assertTrue(store.save(event), "Timestamp must be stored");
        }

        Thread.sleep(150);

        for (int i = 0; i < 256; i++) {
            final CacheEntryEvent<Integer, String> event = new ImmutableCacheEntryEvent<>(i, null, null, System.currentTimeMillis(), CacheEntryEventType.ADDED, CACHE_NAME);

            assertTrue(store.save(event), "Timestamp must be stored");
        }
    }

    private InMemoryCacheEntryEventTimestampStore createTestStore(boolean asyncCleaning) {

        final CacheConfiguration cacheConfiguration =
                ImmutableCacheConfiguration
                        .builder()
                            .setCacheName(CACHE_NAME)
                            .setCacheType(CacheType.REPLICATED)
                            .useTimestampBasedComparison(asyncCleaning)
                            .setTimestampConfiguration(new ImmutableTimestampCacheConfiguration(32, 100))
                        .build();
        return new InMemoryCacheEntryEventTimestampStore(Set.of(cacheConfiguration), true);
    }
}
