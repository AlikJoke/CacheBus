package net.cache.bus.infinispan.listeners;

import net.cache.bus.core.Cache;
import net.cache.bus.core.CacheBus;
import net.cache.bus.core.CacheEventListener;
import net.cache.bus.core.CacheEventListenerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Objects;

@ThreadSafe
@Immutable
public final class InfinispanCacheEventListenerFactory implements CacheEventListenerFactory {

    private final CacheBus cacheBus;

    public InfinispanCacheEventListenerFactory(@Nonnull CacheBus cacheBus) {
        this.cacheBus = Objects.requireNonNull(cacheBus, "cacheBus");
    }

    @Nonnull
    @Override
    public <K, V> CacheEventListener<K, V> create(@Nonnull Cache<K, V> cache) {
        return new InfinispanCacheEntryEventListener<>(this.cacheBus);
    }
}
