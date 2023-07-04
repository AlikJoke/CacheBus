package net.cache.bus.ehcache3.adapters;

import net.cache.bus.core.Cache;
import org.ehcache.event.CacheEventListener;
import org.ehcache.event.EventFiring;
import org.ehcache.event.EventOrdering;
import org.ehcache.event.EventType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class EhCache3CacheAdapter<K, V> implements Cache<K, V> {

    private final org.ehcache.Cache<K, V> cache;
    private final String name;

    public EhCache3CacheAdapter(@Nonnull org.ehcache.Cache<K, V> cache, @Nonnull String name) {
        this.cache = Objects.requireNonNull(cache, "cache");
        this.name = Objects.requireNonNull(name, "name");
    }

    @Override
    public String getName() {
        return this.name;
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

    @Nonnull
    @Override
    public Optional<V> merge(@Nonnull K key, @Nonnull V value, @Nonnull BiFunction<? super V, ? super V, ? extends V> mergeFunction) {

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

        return Optional.ofNullable(newValue);
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
                final V result = this.cache.putIfAbsent(key, newValue);
                return Optional.of(result == null ? newValue : result);
            }
        }

        return Optional.ofNullable(v);
    }

    @Override
    public void registerEventListener(@Nonnull net.cache.bus.core.CacheEventListener<K, V> listener) {

        if (listener instanceof CacheEventListener<?, ?>) {
            @SuppressWarnings("unchecked")
            final CacheEventListener<K, V> eventListener = (CacheEventListener<K, V>) listener;
            this.cache.getRuntimeConfiguration().registerCacheEventListener(eventListener, EventOrdering.ORDERED, EventFiring.SYNCHRONOUS, Set.of(EventType.values()));
        } else {
            throw new ClassCastException("Cache listener implementation must implement " + CacheEventListener.class.getCanonicalName());
        }
    }
}
