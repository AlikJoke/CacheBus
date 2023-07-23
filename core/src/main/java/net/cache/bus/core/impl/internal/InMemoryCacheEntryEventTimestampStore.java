package net.cache.bus.core.impl.internal;

import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.CacheEntryEventTimestampStore;
import net.cache.bus.core.configuration.CacheConfiguration;
import net.cache.bus.core.configuration.InvalidCacheConfigurationException;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Storage of cache element change timestamps based on {@linkplain ConcurrentHashMap} in memory.<br>
 * See the documentation of {@linkplain CacheEntryEventTimestampStore#save(CacheEntryEvent)} for requirements
 * on the storage implementation.
 *
 * @author Alik
 * @implNote When the threshold ({@code 128}) of the associative array size for each cache is exceeded
 * and more than 30 seconds have passed since the last cleaning, it triggers cleanup
 * of expired timestamps according to the settings of the corresponding cache.
 * @see CacheEntryEventTimestampStore
 */
public final class InMemoryCacheEntryEventTimestampStore implements CacheEntryEventTimestampStore {

    private static final int CLEANING_SIZE_THRESHOLD = 128;
    private static final int CLEANING_TIME_THRESHOLD = 30_000;

    private final Map<String, Map<Object, Long>> timestampsByCacheMap;
    private final Map<String, Long> timestampExpirationsByCacheMap;
    private final boolean useAsyncCleaning;

    private volatile long lastCleaningTime;
    private volatile boolean cleaningInProgress;

    public InMemoryCacheEntryEventTimestampStore(@Nonnull Set<CacheConfiguration> configurations, boolean useAsyncCleaning) {
        final int cachesCount = configurations.size();
        final Map<String, Map<Object, Long>> cachesMap = new HashMap<>(cachesCount + 1, 1);
        configurations.forEach(
                config -> cachesMap.put(config.cacheName(), createTimestampMap(config))
        );

        this.timestampExpirationsByCacheMap = createTimestampExpirationsMap(configurations);
        this.timestampsByCacheMap = Collections.unmodifiableMap(cachesMap);
        this.useAsyncCleaning = useAsyncCleaning;
    }

    @Override
    public boolean save(@Nonnull CacheEntryEvent<?, ?> event) {

        final Map<Object, Long> timestampsMap = this.timestampsByCacheMap.get(event.cacheName());
        callCleaningIfNeed(timestampsMap, event.cacheName());

        final Object key = event.key();
        // The case of mass clearing the cache is handled in a special way, because here the key is fictitious:
        // we always return true, because will still be a complete cleanup
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

    private void callCleaningIfNeed(final Map<Object, Long> timestampsMap, final String cacheName) {

        if (timestampsMap.size() > CLEANING_SIZE_THRESHOLD
                && !this.cleaningInProgress
                && (this.cleaningInProgress = (System.currentTimeMillis() - this.lastCleaningTime) >= CLEANING_TIME_THRESHOLD)) {

            if (this.useAsyncCleaning) {
                CompletableFuture.runAsync(() -> clear(timestampsMap, cacheName));
            } else {
                clear(timestampsMap, cacheName);
            }
        }
    }

    private void clear(final Map<Object, Long> timestampsMap, final String cacheName) {

        final Long expiration = this.timestampExpirationsByCacheMap.get(cacheName);
        final Long currentExpirationTime = (this.lastCleaningTime = System.currentTimeMillis()) - expiration;
        timestampsMap.forEach((key, ts) -> {
            if (currentExpirationTime.compareTo(ts) > 0) {
                timestampsMap.remove(key, ts);
            }
        });

        this.cleaningInProgress = false;
    }

    private Map<String, Long> createTimestampExpirationsMap(final Set<CacheConfiguration> cacheConfigurations) {

        final Map<String, Long> result = new HashMap<>();
        cacheConfigurations.forEach(config -> {
            final CacheConfiguration.TimestampCacheConfiguration timestampConfiguration = config.timestampConfiguration().orElseThrow();
            result.put(config.cacheName(), timestampConfiguration.timestampExpiration());
        });

        return result;
    }

    private Map<Object, Long> createTimestampMap(final CacheConfiguration cacheConfiguration) {
        final CacheConfiguration.TimestampCacheConfiguration timestampCacheConfiguration =
                cacheConfiguration.timestampConfiguration()
                        .orElseThrow(() -> new InvalidCacheConfigurationException("Timestamp configuration must present in cache with timestamp based comparison"));
        return new ConcurrentHashMap<>(timestampCacheConfiguration.probableAverageElementsCount());
    }
}
