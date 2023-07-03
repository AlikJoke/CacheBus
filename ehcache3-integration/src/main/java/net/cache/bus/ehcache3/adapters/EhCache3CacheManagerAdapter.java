package net.cache.bus.ehcache3.adapters;

import net.cache.bus.core.Cache;
import net.cache.bus.core.CacheManager;
import net.cache.bus.core.impl.ConcurrentActionExecutor;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.Configuration;
import org.ehcache.core.EhcacheManager;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

public final class EhCache3CacheManagerAdapter implements CacheManager {

    private final EhcacheManager ehcacheManager;
    private final ConcurrentActionExecutor concurrentActionExecutor;

    public EhCache3CacheManagerAdapter(
            @Nonnull EhcacheManager ehcacheManager,
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
        final Configuration runtimeConfiguration = this.ehcacheManager.getRuntimeConfiguration();
        final CacheConfiguration<?, ?> cacheConfig = runtimeConfiguration.getCacheConfigurations().get(cacheName);
        if (cacheConfig == null) {
            return Optional.empty();
        }

        @SuppressWarnings("unchecked")
        final org.ehcache.Cache<K, V> cache = (org.ehcache.Cache<K, V>) this.ehcacheManager.getCache(cacheName, cacheConfig.getKeyType(), cacheConfig.getValueType());
        return Optional.ofNullable(cache)
                        .map(c -> new EhCache3CacheAdapter<>(c, this.concurrentActionExecutor));
    }
}
