package net.cache.bus.core;

import javax.annotation.Nonnull;

public interface CacheEventListenerFactory {

    @Nonnull
    <K, V> CacheEventListener<K, V> create(@Nonnull Cache<K, V> cache);
}
