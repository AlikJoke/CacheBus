package net.cache.bus.ehcache2.adapters;

import net.cache.bus.core.Cache;
import net.cache.bus.core.CacheAlreadyDefinedAsClusteredException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.NotificationScope;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class EhCache2CacheAdapter<K extends Serializable, V extends Serializable> implements Cache<K, V> {

    private final Ehcache cache;

    public EhCache2CacheAdapter(@Nonnull Ehcache cache) {
        this.cache = Objects.requireNonNull(cache, "cache");

        if (cache instanceof net.sf.ehcache.Cache c && c.isTerracottaClustered()) {
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
        final Element value = this.cache.get(Objects.requireNonNull(key, "key"));
        return Optional.ofNullable(value == null ? null : castValue(value.getObjectValue()));
    }

    @Override
    public void evict(@Nonnull K key) {
        this.cache.remove(Objects.requireNonNull(key, "key"), true);
    }

    @Nonnull
    @Override
    public Optional<V> remove(@Nonnull K key) {
        final Element value = this.cache.get(Objects.requireNonNull(key, "key"));
        if (value != null) {
            this.cache.removeElement(value);
        }

        return Optional.ofNullable(value == null ? null : castValue(value.getObjectValue()));
    }

    @Override
    public void put(@Nonnull K key, @Nullable V value) {
        this.cache.put(new Element(key, value), true);
    }

    @Override
    public void putIfAbsent(@Nonnull K key, @Nullable V value) {
        this.cache.putIfAbsent(new Element(key, value), true);
    }

    @Override
    public void clear() {
        this.cache.removeAll(true);
    }

    @Override
    public void merge(@Nonnull K key, @Nonnull V value, @Nonnull BiFunction<? super V, ? super V, ? extends V> mergeFunction) {

        Objects.requireNonNull(mergeFunction, "mergeFunction");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");

        this.cache.acquireWriteLockOnKey(key);
        try {
            final Optional<V> oldValue = get(key);
            final Object newValue = oldValue.isEmpty() ? value : mergeFunction.apply(oldValue.get(), value);
            if (newValue == null) {
                this.cache.remove(key);
            } else {
                this.cache.put(new Element(key, newValue), true);
            }

        } finally {
            this.cache.releaseWriteLockOnKey(key);
        }
    }

    @Nonnull
    @Override
    public Optional<V> computeIfAbsent(@Nonnull K key, @Nonnull Function<? super K, ? extends V> valueFunction) {

        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(valueFunction, "valueFunction");

        this.cache.acquireWriteLockOnKey(key);
        try {
            final Element v = this.cache.get(key);
            if (v == null || v.getObjectValue() == null) {
                final V newValue = valueFunction.apply(key);
                if (newValue != null) {
                    this.cache.put(new Element(key, newValue), true);
                    return Optional.of(newValue);
                }
            }

            return Optional.ofNullable(v == null ? null : castValue(v.getObjectValue()));
        } finally {
            this.cache.releaseWriteLockOnKey(key);
        }
    }

    @Override
    public void registerEventListener(@Nonnull net.cache.bus.core.CacheEventListener<K, V> listener) {

        if (listener instanceof CacheEventListener eventListener) {
            this.cache.getCacheEventNotificationService().registerListener(eventListener, NotificationScope.LOCAL);
        } else {
            throw new ClassCastException("Cache listener implementation must implement " + CacheEventListener.class.getCanonicalName());
        }
    }

    private V castValue(final Object value) {
        @SuppressWarnings("unchecked")
        final V result = (V) value;
        return result;
    }
}
