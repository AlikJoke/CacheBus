package net.cache.bus.ehcache2.adapters;

import net.cache.bus.core.Cache;
import net.cache.bus.core.CacheManager;
import net.cache.bus.core.impl.ImmutableComponentState;
import net.cache.bus.core.state.ComponentState;
import net.sf.ehcache.Status;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class EhCache2CacheManagerAdapter implements CacheManager {

    private static final String CACHE_MANAGER_ID = "ehcache2-cache-manager";

    private final net.sf.ehcache.CacheManager ehcacheManager;
    private final Map<String, Optional<Cache<Serializable, Serializable>>> cachesMap;

    public EhCache2CacheManagerAdapter(@Nonnull net.sf.ehcache.CacheManager ehcacheManager) {
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
    public ComponentState state() {
        final Status status = this.ehcacheManager.getStatus();
        final ComponentState.Status busStatus =
                status == Status.STATUS_ALIVE
                        ? ComponentState.Status.UP_OK
                        : ComponentState.Status.DOWN;

        return new ImmutableComponentState(CACHE_MANAGER_ID, busStatus);
    }

    @Nonnull
    @Override
    public <K extends Serializable, V extends Serializable> Optional<Cache<K, V>> getCache(@Nonnull String cacheName) {
        return this.cachesMap.computeIfAbsent(cacheName, this::composeCacheAdapter)
                                .map(this::cast);
    }

    private Optional<Cache<Serializable, Serializable>> composeCacheAdapter(final String cacheName) {
        return Optional.ofNullable(this.ehcacheManager.getCache(cacheName))
                        .map(EhCache2CacheAdapter::new);
    }

    private <K extends Serializable, V extends Serializable> Cache<K, V> cast(Cache<Serializable, Serializable> cache) {
        @SuppressWarnings("unchecked")
        final Cache<K, V> result = (Cache<K, V>) cache;
        return result;
    }
}
