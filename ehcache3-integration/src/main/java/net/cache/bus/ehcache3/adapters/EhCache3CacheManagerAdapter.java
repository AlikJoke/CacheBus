package net.cache.bus.ehcache3.adapters;

import net.cache.bus.core.Cache;
import net.cache.bus.core.CacheManager;
import net.cache.bus.core.impl.ImmutableComponentState;
import net.cache.bus.core.state.ComponentState;
import org.ehcache.Status;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.Configuration;
import org.ehcache.core.EhcacheManager;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class EhCache3CacheManagerAdapter implements CacheManager {

    private static final String CACHE_MANAGER_ID = "ehcache3-cache-manager";

    private final EhcacheManager ehcacheManager;
    private final Map<String, Optional<Cache<Serializable, Serializable>>> cachesMap;

    public EhCache3CacheManagerAdapter(@Nonnull EhcacheManager ehcacheManager) {
        this.ehcacheManager = Objects.requireNonNull(ehcacheManager, "ehcacheManager");
        this.cachesMap = new ConcurrentHashMap<>();
    }

    @Nonnull
    @Override
    public <T> T getUnderlyingCacheManager(@Nonnull Class<T> managerType) {
        return Objects.requireNonNull(managerType, "managerType").cast(this.ehcacheManager);
    }

    @Nonnull
    @Override
    public <K extends Serializable, V extends Serializable> Optional<Cache<K, V>> getCache(@Nonnull String cacheName) {
        return this.cachesMap.computeIfAbsent(cacheName, this::composeCacheAdapter)
                                .map(this::cast);
    }

    @Nonnull
    @Override
    public ComponentState state() {
        final Status status = this.ehcacheManager.getStatus();
        final ComponentState.Status busStatus = switch (status) {
            case UNINITIALIZED -> ComponentState.Status.DOWN;
            case AVAILABLE -> ComponentState.Status.UP_OK;
            case MAINTENANCE -> ComponentState.Status.UP_NOT_READY;
        };

        return new ImmutableComponentState(CACHE_MANAGER_ID, busStatus);
    }

    private <K extends Serializable, V extends Serializable> Optional<Cache<K, V>> composeCacheAdapter(@Nonnull String cacheName) {
        final Configuration runtimeConfiguration = this.ehcacheManager.getRuntimeConfiguration();
        final CacheConfiguration<?, ?> cacheConfig = runtimeConfiguration.getCacheConfigurations().get(cacheName);
        if (cacheConfig == null) {
            return Optional.empty();
        }

        @SuppressWarnings("unchecked")
        final org.ehcache.Cache<K, V> cache = (org.ehcache.Cache<K, V>) this.ehcacheManager.getCache(cacheName, cacheConfig.getKeyType(), cacheConfig.getValueType());
        return Optional.ofNullable(cache)
                        .map(c -> new EhCache3CacheAdapter<>(c, cacheName));
    }

    private <K extends Serializable, V extends Serializable> Cache<K, V> cast(Cache<Serializable, Serializable> cache) {
        @SuppressWarnings("unchecked")
        final Cache<K, V> result = (Cache<K, V>) cache;
        return result;
    }
}
