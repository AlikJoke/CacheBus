package net.cache.bus.ehcache3.listeners;

import net.cache.bus.core.Cache;
import net.cache.bus.core.CacheBus;
import net.cache.bus.core.CacheEventListener;
import net.cache.bus.core.CacheEventListenerRegistrar;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;

@ThreadSafe
@Immutable
public final class EhCache3CacheEventListenerRegistrar implements CacheEventListenerRegistrar {

    @Override
    public <K extends Serializable, V extends Serializable> void registerFor(
            @Nonnull CacheBus cacheBus,
            @Nonnull Cache<K, V> cache) {

        final CacheEventListener<K, V> listener = new EhCache3CacheEntryEventListener<>(cacheBus, cache.getName());
        cache.registerEventListener(listener);
    }
}
