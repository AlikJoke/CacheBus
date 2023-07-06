package ehcache2.adapters;

import net.cache.bus.core.Cache;
import net.cache.bus.core.CacheAlreadyDefinedAsClusteredException;
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

public final class EhCache2CacheAdapter implements Cache<Serializable, Serializable> {

    private final net.sf.ehcache.Cache cache;

    public EhCache2CacheAdapter(@Nonnull net.sf.ehcache.Cache cache) {
        this.cache = Objects.requireNonNull(cache, "cache");

        if (cache.isTerracottaClustered()) {
            throw new CacheAlreadyDefinedAsClusteredException(cache.getName());
        }
    }

    @Override
    public String getName() {
        return this.cache.getName();
    }

    @Nonnull
    @Override
    public Optional<Serializable> get(@Nonnull Serializable key) {
        final Element value = this.cache.get(Objects.requireNonNull(key, "key"));
        return Optional.ofNullable(value == null ? null : (Serializable) value.getObjectValue());
    }

    @Override
    public void evict(@Nonnull Serializable key) {
        this.cache.remove(Objects.requireNonNull(key, "key"));
    }

    @Nonnull
    @Override
    public Optional<Serializable> remove(@Nonnull Serializable key) {
        final Element value = this.cache.get(Objects.requireNonNull(key, "key"));
        return Optional.ofNullable(value == null ? null : (Serializable) value.getObjectValue());
    }

    @Override
    public void put(@Nonnull Serializable key, @Nullable Serializable value) {
        this.cache.put(new Element(key, value), true);
    }

    @Override
    public void putIfAbsent(@Nonnull Serializable key, @Nullable Serializable value) {
        this.cache.putIfAbsent(new Element(key, value), true);
    }

    @Override
    public void clear() {
        this.cache.removeAll(true);
    }

    @Override
    public void merge(@Nonnull Serializable key, @Nonnull Serializable value, @Nonnull BiFunction<? super Serializable, ? super Serializable, ? extends Serializable> mergeFunction) {

        Objects.requireNonNull(mergeFunction, "mergeFunction");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");

        this.cache.acquireWriteLockOnKey(key);
        try {
            final Optional<Serializable> oldValue = get(key);
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
    public Optional<Serializable> computeIfAbsent(@Nonnull Serializable key, @Nonnull Function<? super Serializable, ? extends Serializable> valueFunction) {

        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(valueFunction, "valueFunction");

        this.cache.acquireWriteLockOnKey(key);
        try {
            final Element v = this.cache.get(key);
            if (v == null || v.getObjectValue() == null) {
                final Serializable newValue = valueFunction.apply(key);
                if (newValue != null) {
                    this.cache.put(new Element(key, newValue), true);
                    return Optional.of(newValue);
                }
            }

            return Optional.ofNullable(v == null ? null : (Serializable) v.getObjectValue());
        } finally {
            this.cache.releaseWriteLockOnKey(key);
        }
    }

    @Override
    public void registerEventListener(@Nonnull net.cache.bus.core.CacheEventListener<Serializable, Serializable> listener) {

        if (listener instanceof CacheEventListener eventListener) {
            this.cache.getCacheEventNotificationService().registerListener(eventListener, NotificationScope.LOCAL);
        } else {
            throw new ClassCastException("Cache listener implementation must implement " + CacheEventListener.class.getCanonicalName());
        }
    }
}
