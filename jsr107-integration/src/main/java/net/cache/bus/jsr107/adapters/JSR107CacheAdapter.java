package net.cache.bus.jsr107.adapters;

import net.cache.bus.core.Cache;
import net.cache.bus.core.impl.ConcurrentActionExecutor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class JSR107CacheAdapter<K, V> implements Cache<K, V> {

    private final javax.cache.Cache<K, V> cache;
    private final ConcurrentActionExecutor concurrentActionExecutor;

    public JSR107CacheAdapter(
            @Nonnull javax.cache.Cache<K, V> cache,
            @Nonnull ConcurrentActionExecutor concurrentActionExecutor) {
        this.cache = Objects.requireNonNull(cache, "cache");
        this.concurrentActionExecutor = Objects.requireNonNull(concurrentActionExecutor, "concurrentActionExecutor");
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

        return Objects.requireNonNull(this.concurrentActionExecutor.execute(key, () -> {
            final Optional<V> oldValue = get(key);
            final V newValue = oldValue.isEmpty() ? value : mergeFunction.apply(oldValue.get(), value);
            if (newValue == null) {
                this.cache.remove(key);
            } else {
                this.cache.put(key, newValue);
            }

            return Optional.ofNullable(newValue);
        }));
    }

    @Nonnull
    @Override
    public Optional<V> computeIfAbsent(@Nonnull K key, @Nonnull Function<? super K, ? extends V> valueFunction) {

        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(valueFunction, "valueFunction");

        return Objects.requireNonNull(this.concurrentActionExecutor.execute(key, () -> {
            final V v = this.cache.get(key);
            if (v == null) {

                final V newValue = valueFunction.apply(key);
                if (newValue != null) {
                    this.cache.put(key, newValue);
                    return Optional.of(newValue);
                }
            }

            return Optional.ofNullable(v);
        }));
    }
}
