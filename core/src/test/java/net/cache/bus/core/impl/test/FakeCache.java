package net.cache.bus.core.impl.test;

import net.cache.bus.core.Cache;
import net.cache.bus.core.CacheEventListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

public class FakeCache<K extends Serializable, V extends Serializable> implements Cache<K, V> {

    private final Map<K, V> map;
    private final String cacheName;
    private CacheEventListener<K, V> cacheEventListener;

    public FakeCache(String cacheName) {
        this.map = new ConcurrentHashMap<>();
        this.cacheName = cacheName;
    }

    @Override
    public String getName() {
        return this.cacheName;
    }

    @Nonnull
    @Override
    public Optional<V> get(@Nonnull K key) {
        return Optional.ofNullable(map.get(key));
    }

    @Override
    public void evict(@Nonnull K key) {
        map.remove(key);
    }

    @Nonnull
    @Override
    public Optional<V> remove(@Nonnull K key) {
        return Optional.ofNullable(map.remove(key));
    }

    @Override
    public void put(@Nonnull K key, @Nullable V value) {
        map.put(key, value);
    }

    @Override
    public void putIfAbsent(@Nonnull K key, @Nullable V value) {
        map.putIfAbsent(key, value);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public void merge(@Nonnull K key, @Nonnull V value, @Nonnull BiFunction<? super V, ? super V, ? extends V> mergeFunction) {
        map.merge(key, value, mergeFunction);
    }

    @Nonnull
    @Override
    public Optional<V> computeIfAbsent(@Nonnull K key, @Nonnull Function<? super K, ? extends V> valueFunction) {
        return Optional.ofNullable(map.computeIfAbsent(key, valueFunction));
    }

    @Override
    public void registerEventListener(@Nonnull CacheEventListener<K, V> listener) {
        this.cacheEventListener = listener;
    }

    @Override
    public void unregisterEventListener(@Nonnull CacheEventListener<K, V> listener) {
        this.cacheEventListener = null;
    }

    public CacheEventListener<K, V> getRegisteredEventListener() {
        return this.cacheEventListener;
    }
}
