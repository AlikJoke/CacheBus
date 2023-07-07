package net.cache.bus.infinispan.adapters;

import net.cache.bus.core.Cache;
import net.cache.bus.core.CacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InfinispanCacheManagerAdapter implements CacheManager {

    private final EmbeddedCacheManager cacheManager;
    private final Map<String, Optional<Cache<Serializable, Serializable>>> cachesMap;

    public InfinispanCacheManagerAdapter(@Nonnull EmbeddedCacheManager cacheManager) {
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
        final org.infinispan.Cache<K, V> cache = this.cacheManager.getCache(cacheName, false);
        return Optional.ofNullable(cache)
                        .map(InfinispanCacheAdapter::new);
    }

    private <K extends Serializable, V extends Serializable> Cache<K, V> cast(Cache<Serializable, Serializable> cache) {
        @SuppressWarnings("unchecked")
        final Cache<K, V> result = (Cache<K, V>) cache;
        return result;
    }
}
