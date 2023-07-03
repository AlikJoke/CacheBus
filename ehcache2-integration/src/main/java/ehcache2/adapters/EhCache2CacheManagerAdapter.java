package ehcache2.adapters;

import net.cache.bus.core.Cache;
import net.cache.bus.core.CacheManager;
import net.cache.bus.core.impl.ConcurrentActionExecutor;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

public final class EhCache2CacheManagerAdapter implements CacheManager {

    private final net.sf.ehcache.CacheManager ehcacheManager;
    private final ConcurrentActionExecutor concurrentActionExecutor;

    public EhCache2CacheManagerAdapter(
            @Nonnull net.sf.ehcache.CacheManager ehcacheManager,
            @Nonnull ConcurrentActionExecutor concurrentActionExecutor) {
        this.ehcacheManager = Objects.requireNonNull(ehcacheManager, "ehcacheManager");
        this.concurrentActionExecutor = Objects.requireNonNull(concurrentActionExecutor, "concurrentActionExecutor");
    }

    @Nonnull
    @Override
    public <T> T getUnderlyingCacheManager(@Nonnull Class<T> managerType) {
        return Objects.requireNonNull(managerType, "managerType").cast(this.ehcacheManager);
    }

    @Nonnull
    @Override
    public <K, V> Optional<Cache<K, V>> getCache(@Nonnull String cacheName) {
        @SuppressWarnings("unchecked")
        final Optional<Cache<K, V>> result = Optional.ofNullable(this.ehcacheManager.getCache(cacheName))
                                                        .map(c -> new EhCache2CacheAdapter(c, this.concurrentActionExecutor))
                                                        .map(Cache.class::cast);

        return result;
    }
}
