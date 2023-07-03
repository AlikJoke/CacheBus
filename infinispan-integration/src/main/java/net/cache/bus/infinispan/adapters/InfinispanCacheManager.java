package net.cache.bus.infinispan.adapters;

import net.cache.bus.core.Cache;
import net.cache.bus.core.CacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

public final class InfinispanCacheManager implements CacheManager {

    private final EmbeddedCacheManager cacheManager;

    public InfinispanCacheManager(@Nonnull EmbeddedCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Nonnull
    @Override
    public <T> T getUnderlyingCacheManager(@Nonnull Class<T> managerType) {
        return Objects.requireNonNull(managerType, "managerType").cast(this.cacheManager);
    }

    @Nonnull
    @Override
    public <K, V> Optional<Cache<K, V>> getCache(@Nonnull String cacheName) {
        final org.infinispan.Cache<K, V> cache = this.cacheManager.getCache(cacheName, false);
        return Optional.ofNullable(cache)
                        .map(InfinispanCacheAdapter::new);
    }
}
