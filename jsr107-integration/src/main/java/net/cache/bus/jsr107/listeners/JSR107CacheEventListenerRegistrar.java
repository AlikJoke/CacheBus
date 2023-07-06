package net.cache.bus.jsr107.listeners;

import net.cache.bus.core.Cache;
import net.cache.bus.core.CacheBus;
import net.cache.bus.core.CacheEventListener;
import net.cache.bus.core.CacheEventListenerRegistrar;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Objects;

public final class JSR107CacheEventListenerRegistrar implements CacheEventListenerRegistrar {

    private final CacheBus cacheBus;

    public JSR107CacheEventListenerRegistrar(@Nonnull CacheBus cacheBus) {
        this.cacheBus = Objects.requireNonNull(cacheBus, "cacheBus");
    }

    @Override
    public <K extends Serializable, V extends Serializable> void registerFor(@Nonnull Cache<K, V> cache) {
        final CacheEventListener<K, V> listener = new JSR107CacheEntryEventListener<>(this.cacheBus);
        cache.registerEventListener(listener);
    }
}
