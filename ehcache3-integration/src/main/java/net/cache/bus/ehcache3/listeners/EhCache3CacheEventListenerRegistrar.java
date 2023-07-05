package net.cache.bus.ehcache3.listeners;

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
public final class EhCache3CacheEventListenerRegistrar implements CacheEventListenerRegistrar {

    private final CacheBus cacheBus;

    public EhCache3CacheEventListenerRegistrar(@Nonnull CacheBus cacheBus) {
        this.cacheBus = Objects.requireNonNull(cacheBus, "cacheBus");
    }

    @Override
    public <K, V> void registerFor(@Nonnull Cache<K, V> cache) {
        final CacheEventListener<K, V> listener = new EhCache3CacheEntryEventListener<>(this.cacheBus, cache.getName());
        cache.registerEventListener(listener);
    }
}
