package net.cache.bus.ehcache3.listeners;

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
public final class EhCache3CacheEventListenerFactory implements CacheEventListenerFactory {

    private final CacheBus cacheBus;

    public EhCache3CacheEventListenerFactory(@Nonnull CacheBus cacheBus) {
        this.cacheBus = Objects.requireNonNull(cacheBus, "cacheBus");
    }

    @Nonnull
    @Override
    public <K, V> CacheEventListener<K, V> create(@Nonnull Cache<K, V> cache) {
        return new EhCache3CacheEntryEventListener<>(this.cacheBus, cache.getName());
    }
}
