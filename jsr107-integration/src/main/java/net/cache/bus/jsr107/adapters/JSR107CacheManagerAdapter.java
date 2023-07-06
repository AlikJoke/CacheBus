package net.cache.bus.jsr107.adapters;

import net.cache.bus.core.Cache;
import net.cache.bus.core.CacheManager;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class JSR107CacheManagerAdapter implements CacheManager {

    private final javax.cache.CacheManager cacheManager;
    private final Map<String, Optional<Cache<Serializable, Serializable>>> cachesMap;

    public JSR107CacheManagerAdapter(@Nonnull javax.cache.CacheManager cacheManager) {
        this.cacheManager = Objects.requireNonNull(cacheManager, "cacheManager");
        this.cachesMap = new ConcurrentHashMap<>();
    }

    @Nonnull
    @Override
    public <T> T getUnderlyingCacheManager(@Nonnull Class<T> managerType) {
        return Objects.requireNonNull(managerType, "managerType").cast(this.cacheManager);
    }

    @Nonnull
    @Override
    public <K extends Serializable, V extends Serializable> Optional<Cache<K, V>> getCache(@Nonnull String cacheName) {
        return this.cachesMap.computeIfAbsent(cacheName, this::composeCacheAdapter)
                                .map(this::cast);
    }

    private <K extends Serializable, V extends Serializable> Optional<Cache<K, V>> composeCacheAdapter(@Nonnull String cacheName) {
        final javax.cache.Cache<K, V> cache = this.cacheManager.getCache(cacheName);
        return Optional.ofNullable(cache)
                        .map(JSR107CacheAdapter::new);
    }

    private <K extends Serializable, V extends Serializable> Cache<K, V> cast(Cache<Serializable, Serializable> cache) {
        @SuppressWarnings("unchecked")
        final Cache<K, V> result = (Cache<K, V>) cache;
        return result;
    }
}
