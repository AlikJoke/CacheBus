package net.cache.bus.core.impl.internal;

import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.CacheEntryEventTimestampStore;
import net.cache.bus.core.configuration.CacheConfiguration;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryCacheEntryEventTimestampStore implements CacheEntryEventTimestampStore {

    private final Map<String, Map<Object, Long>> map;

    public InMemoryCacheEntryEventTimestampStore(@Nonnull Set<CacheConfiguration> configurations) {
        final int cachesCount = configurations.size();
        final Map<String, Map<Object, Long>> cachesMap = new HashMap<>(cachesCount + 1, 1);
        configurations.forEach(
                config -> cachesMap.put(
                        config.cacheName(),
                        new ConcurrentHashMap<>(128, 0.75f, config.probableConcurrentModificationThreads())
                )
        );

        this.map = Collections.unmodifiableMap(cachesMap);
    }

    @Override
    public void save(@Nonnull String cache, @Nonnull Object key, @Nonnegative long timestamp) {
        this.map.get(cache).merge(key, timestamp, (v1, v2) -> v1.compareTo(v2) > 0 ? v1 : v2);
    }

    @Override
    public void save(@Nonnull CacheEntryEvent<?, ?> event) {
        save(event.cacheName(), event.key(), event.eventTime());
    }

    @Override
    @Nonnegative
    public long load(@Nonnull String cache, @Nonnull Object key) {
        final Long timestamp = this.map.get(cache).get(key);
        return timestamp == null ? 0 : timestamp;
    }
}
