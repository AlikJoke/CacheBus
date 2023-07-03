package ehcache2.adapters;

import net.cache.bus.core.Cache;
import net.cache.bus.core.CacheAlreadyDefinedAsClusteredException;
import net.cache.bus.core.impl.ConcurrentActionExecutor;
import net.sf.ehcache.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class EhCache2CacheAdapter implements Cache<Object, Object> {

    private final net.sf.ehcache.Cache cache;
    private final ConcurrentActionExecutor concurrentActionExecutor;

    public EhCache2CacheAdapter(
            @Nonnull net.sf.ehcache.Cache cache,
            @Nonnull ConcurrentActionExecutor concurrentActionExecutor) {
        this.cache = Objects.requireNonNull(cache, "cache");

        if (cache.isTerracottaClustered()) {
            throw new CacheAlreadyDefinedAsClusteredException(cache.getName());
        }

        this.concurrentActionExecutor = Objects.requireNonNull(concurrentActionExecutor, "concurrentActionExecutor");
    }

    @Nonnull
    @Override
    public Optional<Object> get(@Nonnull Object key) {
        final Element value = this.cache.get(Objects.requireNonNull(key, "key"));
        return Optional.ofNullable(value == null ? null : value.getObjectValue());
    }

    @Override
    public void evict(@Nonnull Object key) {
        this.cache.remove(Objects.requireNonNull(key, "key"));
    }

    @Nonnull
    @Override
    public Optional<Object> remove(@Nonnull Object key) {
        final Element value = this.cache.get(Objects.requireNonNull(key, "key"));
        return Optional.ofNullable(value == null ? null : value.getObjectValue());
    }

    @Override
    public void put(@Nonnull Object key, @Nullable Object value) {
        this.cache.put(new Element(key, value), true);
    }

    @Override
    public void putIfAbsent(@Nonnull Object key, @Nullable Object value) {
        this.cache.putIfAbsent(new Element(key, value), true);
    }

    @Override
    public void clear() {
        this.cache.removeAll(true);
    }

    @Nonnull
    @Override
    public Optional<Object> merge(@Nonnull Object key, @Nonnull Object value, @Nonnull BiFunction<? super Object, ? super Object, ?> mergeFunction) {

        Objects.requireNonNull(mergeFunction, "mergeFunction");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");

        return Objects.requireNonNull(this.concurrentActionExecutor.execute(key, () -> {
            final Optional<Object> oldValue = get(key);
            final Object newValue = oldValue.isEmpty() ? value : mergeFunction.apply(oldValue.get(), value);
            if (newValue == null) {
                this.cache.remove(key);
            } else {
                this.cache.put(new Element(key, newValue), true);
            }

            return Optional.ofNullable(newValue);
        }));
    }

    @Nonnull
    @Override
    public Optional<Object> computeIfAbsent(@Nonnull Object key, @Nonnull Function<? super Object, ?> valueFunction) {

        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(valueFunction, "valueFunction");

        return Objects.requireNonNull(this.concurrentActionExecutor.execute(key, () -> {
            final Element v = this.cache.get(key);
            if (v == null || v.getObjectValue() == null) {
                final Object newValue = valueFunction.apply(key);
                if (newValue != null) {
                    this.cache.put(new Element(key, newValue), true);
                    return Optional.of(newValue);
                }
            }

            return Optional.ofNullable(v == null ? null : v.getObjectValue());
        }));
    }
}
