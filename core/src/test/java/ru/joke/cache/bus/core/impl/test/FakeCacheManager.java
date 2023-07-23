package ru.joke.cache.bus.core.impl.test;

import ru.joke.cache.bus.core.Cache;
import ru.joke.cache.bus.core.CacheManager;
import ru.joke.cache.bus.core.impl.ImmutableComponentState;
import ru.joke.cache.bus.core.state.ComponentState;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

public class FakeCacheManager implements CacheManager {

    private final Map<String, Cache<? extends Serializable, ? extends Serializable>> caches;

    public FakeCacheManager(Map<String, Cache<? extends Serializable, ? extends Serializable>> caches) {
        this.caches = caches;
    }

    @Nonnull
    @Override
    public <T> T getUnderlyingCacheManager(@Nonnull Class<T> managerType) {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public ComponentState state() {
        return new ImmutableComponentState("fake-cache-manager", ComponentState.Status.UP_OK);
    }

    @Nonnull
    @Override
    public <K extends Serializable, V extends Serializable> Optional<Cache<K, V>> getCache(@Nonnull String cacheName) {
        @SuppressWarnings("unchecked")
        final Cache<K, V> result = (Cache<K, V>) this.caches.get(cacheName);
        return Optional.ofNullable(result);
    }
}
