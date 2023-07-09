package net.cache.bus.jsr107.adapters;

import net.cache.bus.core.Cache;
import net.cache.bus.core.CacheEventListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.CacheEntryListenerException;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class JSR107CacheAdapter<K extends Serializable, V extends Serializable> implements Cache<K, V> {

    private final javax.cache.Cache<K, V> cache;

    public JSR107CacheAdapter(@Nonnull javax.cache.Cache<K, V> cache) {
        this.cache = Objects.requireNonNull(cache, "cache");
    }

    @Override
    public String getName() {
        return this.cache.getName();
    }

    @Nonnull
    @Override
    public Optional<V> get(@Nonnull K key) {
        final V value = this.cache.get(Objects.requireNonNull(key, "key"));
        return Optional.ofNullable(value);
    }

    @Override
    public void evict(@Nonnull K key) {
        this.cache.remove(Objects.requireNonNull(key, "key"));
    }

    @Nonnull
    @Override
    public Optional<V> remove(@Nonnull K key) {
        final V value = this.cache.get(Objects.requireNonNull(key, "key"));
        this.cache.remove(key, value);
        return Optional.ofNullable(value);
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

        Objects.requireNonNull(mergeFunction, "mergeFunction");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");

        final Optional<V> oldValue = get(key);
        final V newValue = oldValue.isEmpty() ? value : mergeFunction.apply(oldValue.get(), value);
        if (newValue == null) {
            this.cache.remove(key, oldValue.get());
        } else if (oldValue.isEmpty()) {
            this.cache.putIfAbsent(key, newValue);
        } else {
            this.cache.replace(key, oldValue.get(), newValue);
        }

    }

    @Nonnull
    @Override
    public Optional<V> computeIfAbsent(@Nonnull K key, @Nonnull Function<? super K, ? extends V> valueFunction) {

        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(valueFunction, "valueFunction");

        final V v = this.cache.get(key);
        if (v == null) {

            final V newValue = valueFunction.apply(key);
            if (newValue != null) {
                return Optional.ofNullable(this.cache.putIfAbsent(key, newValue) ? newValue : this.cache.get(key));
            }
        }

        return Optional.ofNullable(v);
    }

    @Override
    public void registerEventListener(@Nonnull CacheEventListener<K, V> listener) {

        if (!(listener instanceof CacheEntryListener<?,?>)) {
            throw new ClassCastException("Cache listener implementation must implement " + CacheEntryListener.class.getCanonicalName());
        }

        @SuppressWarnings("unchecked")
        final CacheEntryListener<K, V> eventListener = (CacheEntryListener<K, V>) listener;
        final CacheEntryListenerConfiguration<K, V> configuration = new MutableCacheEntryListenerConfiguration<>(
                () -> eventListener,
                AcceptAllFilter::new,
                true,
                true
        );
        this.cache.registerCacheEntryListener(configuration);
    }

    private static class AcceptAllFilter<K, V> implements CacheEntryEventFilter<K, V> {

        @Override
        public boolean evaluate(CacheEntryEvent<? extends K, ? extends V> cacheEntryEvent) throws CacheEntryListenerException {
            return true;
        }
    }
}
