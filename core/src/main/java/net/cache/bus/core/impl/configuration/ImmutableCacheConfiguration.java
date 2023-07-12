package net.cache.bus.core.impl.configuration;

import net.cache.bus.core.configuration.CacheConfiguration;
import net.cache.bus.core.configuration.CacheType;
import net.cache.bus.core.configuration.InvalidCacheConfigurationException;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Objects;

/**
 * Неизменяемая реализация конфигурации одного кэша для шины.
 *
 * @param cacheName имя кэша, к которому относится конфигурация, не может быть {@code null}.
 * @param cacheType тип кэша, не может быть {@code null}.
 * @author Alik
 * @see CacheConfiguration
 */
@Immutable
@ThreadSafe
public record ImmutableCacheConfiguration(
        @Nonnull String cacheName,
        @Nonnull CacheType cacheType) implements CacheConfiguration {

    public ImmutableCacheConfiguration {
        Objects.requireNonNull(cacheType, "cacheType");

        if (cacheName == null || cacheName.isEmpty()) {
            throw new InvalidCacheConfigurationException("cacheName must be not empty");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ImmutableCacheConfiguration that = (ImmutableCacheConfiguration) o;
        return cacheName.equals(that.cacheName);
    }

    @Override
    public int hashCode() {
        return cacheName.hashCode();
    }

    @Override
    public String toString() {
        return "ImmutableCacheConfiguration{" +
                "cacheName='" + cacheName + '\'' +
                ", cacheType=" + cacheType +
                '}';
    }
}
