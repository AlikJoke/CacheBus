package net.cache.bus.infinispan.listeners;

import net.cache.bus.core.Cache;
import net.cache.bus.core.CacheBus;
import net.cache.bus.core.CacheEventListener;
import net.cache.bus.core.CacheEventListenerRegistrar;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@ThreadSafe
@Immutable
public final class InfinispanCacheEventListenerRegistrar implements CacheEventListenerRegistrar {

    private final String listenerId;

    public InfinispanCacheEventListenerRegistrar() {
        this(UUID.randomUUID().toString());
    }

    public InfinispanCacheEventListenerRegistrar(@Nonnull String listenerId) {
        this.listenerId = Objects.requireNonNull(listenerId, "listenerId");
    }

    @Override
    public <K extends Serializable, V extends Serializable> void registerFor(
            @Nonnull CacheBus cacheBus,
            @Nonnull Cache<K, V> cache) {

        final CacheEventListener<K, V> listener = new InfinispanCacheEntryEventListener<>(this.listenerId, cacheBus);
        cache.registerEventListener(listener);
    }

    @Override
    public <K extends Serializable, V extends Serializable> void unregisterFor(
            @Nonnull CacheBus cacheBus,
            @Nonnull Cache<K, V> cache) {

        final CacheEventListener<K, V> listener = new InfinispanCacheEntryEventListener<>(this.listenerId, cacheBus);
        cache.unregisterEventListener(listener);
    }
}
