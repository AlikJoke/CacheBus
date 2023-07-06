package net.cache.bus.infinispan.listeners;

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
public final class InfinispanCacheEventListenerRegistrar implements CacheEventListenerRegistrar {

    @Override
    public <K extends Serializable, V extends Serializable> void registerFor(
            @Nonnull CacheBus cacheBus,
            @Nonnull Cache<K, V> cache) {

        final CacheEventListener<K, V> listener = new InfinispanCacheEntryEventListener<>(cacheBus);
        cache.registerEventListener(listener);
    }
}
