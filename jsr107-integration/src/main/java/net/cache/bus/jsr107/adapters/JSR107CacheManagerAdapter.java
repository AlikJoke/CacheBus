package net.cache.bus.jsr107.adapters;

import net.cache.bus.core.Cache;
import net.cache.bus.core.CacheManager;
import net.cache.bus.core.impl.ConcurrentActionExecutor;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

public final class JSR107CacheManagerAdapter implements CacheManager {

    private final javax.cache.CacheManager cacheManager;
    private final ConcurrentActionExecutor concurrentActionExecutor;

    public JSR107CacheManagerAdapter(
            @Nonnull javax.cache.CacheManager cacheManager,
            @Nonnull ConcurrentActionExecutor concurrentActionExecutor) {
        this.cacheManager = Objects.requireNonNull(cacheManager, "cacheManager");
        this.concurrentActionExecutor = Objects.requireNonNull(concurrentActionExecutor, "concurrentActionExecutor");
    }

    @Nonnull
    @Override
    public <T> T getUnderlyingCacheManager(@Nonnull Class<T> managerType) {
        return Objects.requireNonNull(managerType, "managerType").cast(this.cacheManager);
    }

    @Nonnull
    @Override
    public <K, V> Optional<Cache<K, V>> getCache(@Nonnull String cacheName) {
        final javax.cache.Cache<K, V> cache = this.cacheManager.getCache(cacheName);
        return Optional.ofNullable(cache)
                        .map(c -> new JSR107CacheAdapter<>(c, this.concurrentActionExecutor));
    }
}
