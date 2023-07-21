package net.cache.bus.core.impl.internal;

import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.CacheEntryEventTimestampStore;
import net.cache.bus.core.configuration.CacheConfiguration;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранилище меток изменений элементов кэшей, основанное на {@linkplain ConcurrentHashMap} в памяти.<br>
 * Требования к реализации хранилища см. в документации к {@linkplain CacheEntryEventTimestampStore#save(CacheEntryEvent)}.
 *
 * @author Alik
 * @see CacheEntryEventTimestampStore
 */
public final class InMemoryCacheEntryEventTimestampStore implements CacheEntryEventTimestampStore {

    private final Map<String, Map<Object, Long>> map;

    public InMemoryCacheEntryEventTimestampStore(@Nonnull Set<CacheConfiguration> configurations) {
        final int cachesCount = configurations.size();
        final Map<String, Map<Object, Long>> cachesMap = new HashMap<>(cachesCount + 1, 1);
        configurations.forEach(
                config -> cachesMap.put(
                        config.cacheName(),
                        new ConcurrentHashMap<>(config.probableAverageElementsCount())
                )
        );

        this.map = Collections.unmodifiableMap(cachesMap);
    }

    @Override
    public boolean save(@Nonnull CacheEntryEvent<?, ?> event) {

        final Map<Object, Long> timestampsMap = this.map.get(event.cacheName());

        final Object key = event.key();
        // Случай массовой очистки кэша обрабатываем особым образом, т.к. тут ключ фиктивный:
        // возвращаем всегда true, т.к. все равно дальше будет полная очистка
        if (key.equals(CacheEntryEvent.ALL_ENTRIES_KEY)) {
            return true;
        }

        final Long oldValue = timestampsMap.get(key);
        final Long newValue = event.eventTime();
        if (oldValue == null) {
            return timestampsMap.putIfAbsent(key, newValue) == null;
        }

        return newValue.compareTo(oldValue) > 0 && timestampsMap.replace(key, oldValue, newValue);
    }
}
