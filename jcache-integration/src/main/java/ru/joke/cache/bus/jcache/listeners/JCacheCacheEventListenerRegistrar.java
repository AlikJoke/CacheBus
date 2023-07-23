package ru.joke.cache.bus.jcache.listeners;

import ru.joke.cache.bus.core.Cache;
import ru.joke.cache.bus.core.CacheBus;
import ru.joke.cache.bus.core.CacheEventListener;
import ru.joke.cache.bus.core.CacheEventListenerRegistrar;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@ThreadSafe
@Immutable
public final class JCacheCacheEventListenerRegistrar implements CacheEventListenerRegistrar {

    private final String listenerId;

    public JCacheCacheEventListenerRegistrar() {
        this(UUID.randomUUID().toString());
    }

    public JCacheCacheEventListenerRegistrar(@Nonnull String listenerId) {
        this.listenerId = Objects.requireNonNull(listenerId, "listenerId");
    }

    @Override
    public <K extends Serializable, V extends Serializable> void registerFor(
            @Nonnull CacheBus cacheBus,
            @Nonnull Cache<K, V> cache) {

        final CacheEventListener<K, V> listener = new JCacheCacheEntryEventListener<>(this.listenerId, cacheBus);
        cache.registerEventListener(listener);
    }

    @Override
    public <K extends Serializable, V extends Serializable> void unregisterFor(
            @Nonnull CacheBus cacheBus,
            @Nonnull Cache<K, V> cache) {

        final CacheEventListener<K, V> listener = new JCacheCacheEntryEventListener<>(this.listenerId, cacheBus);
        cache.unregisterEventListener(listener);
    }
}
