package net.cache.bus.infinispan.listeners;

import net.cache.bus.core.Cache;
import net.cache.bus.core.CacheBus;
import net.cache.bus.core.CacheEventListener;
import net.cache.bus.core.CacheEventListenerRegistrar;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Objects;

@ThreadSafe
@Immutable
public final class InfinispanCacheEventListenerRegistrar implements CacheEventListenerRegistrar {

    private final CacheBus cacheBus;

    public InfinispanCacheEventListenerRegistrar(@Nonnull CacheBus cacheBus) {
        this.cacheBus = Objects.requireNonNull(cacheBus, "cacheBus");
    }

    @Override
    public <K, V> void registerFor(@Nonnull Cache<K, V> cache) {
        final CacheEventListener<K, V> listener = new InfinispanCacheEntryEventListener<>(this.cacheBus);
        cache.registerEventListener(listener);
    }
}
