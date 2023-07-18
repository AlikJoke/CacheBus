package net.cache.bus.infinispan.adapters;

import net.cache.bus.core.Cache;
import net.cache.bus.core.configuration.CacheAlreadyDefinedAsClusteredException;
import net.cache.bus.core.CacheEventListener;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class InfinispanCacheAdapter<K extends Serializable, V extends Serializable> implements Cache<K, V> {

    private final org.infinispan.Cache<K, V> cache;

    public InfinispanCacheAdapter(@Nonnull org.infinispan.Cache<K, V> cache) {
        this.cache = Objects.requireNonNull(cache, "cache");

        final Configuration cacheConfig = cache.getCacheConfiguration();
        if (cacheConfig.clustering().cacheMode() != CacheMode.LOCAL) {
            throw new CacheAlreadyDefinedAsClusteredException(cache.getName());
        }
    }

    @Override
    public String getName() {
        return this.cache.getName();
    }

    @Nonnull
    @Override
    public Optional<V> get(@Nonnull K key) {
        return Optional.ofNullable(this.cache.get(key));
    }

    @Override
    public void evict(@Nonnull K key) {
        this.cache.evict(key);
    }

    @Nonnull
    @Override
    public Optional<V> remove(@Nonnull K key) {
        return Optional.ofNullable(this.cache.remove(key));
    }

    @Override
    public void put(@Nonnull K key, @Nullable V value) {
        this.cache.put(key, value);
    }

    @Override
    public void putIfAbsent(@Nonnull K key, @Nullable V value) {
        this.cache.putIfAbsent(key, value);
    }

    @Override
    public void clear() {
        this.cache.clear();
    }

    @Override
    public void merge(@Nonnull K key, @Nonnull V value, @Nonnull BiFunction<? super V, ? super V, ? extends V> mergeFunction) {
        this.cache.merge(key, value, mergeFunction);
    }

    @Nonnull
    @Override
    public Optional<V> computeIfAbsent(@Nonnull K key, @Nonnull Function<? super K, ? extends V> valueFunction) {
        return Optional.ofNullable(this.cache.computeIfAbsent(key, valueFunction));
    }

    @Override
    public void registerEventListener(@Nonnull CacheEventListener<K, V> listener) {
        this.cache.addListener(listener);
    }

    @Override
    public void unregisterEventListener(@Nonnull CacheEventListener<K, V> listener) {
        this.cache.removeListener(listener);
    }
}
