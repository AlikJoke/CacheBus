package ru.joke.cache.bus.core.impl.configuration;

import ru.joke.cache.bus.core.configuration.CacheConfiguration;
import ru.joke.cache.bus.core.configuration.CacheType;
import ru.joke.cache.bus.core.configuration.InvalidCacheConfigurationException;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Immutable implementation of a cache configuration for the bus.
 *
 * @param cacheName                   the name of the cache to which the configuration belongs, cannot be {@code null}.
 * @param cacheType                   the type of the cache, cannot be {@code null}.
 * @param cacheAliases                additional aliases for the cache, cannot be {@code null}.
 * @param useTimestampBasedComparison indicates whether timestamps should be used to determine the need for applying changes to the local cache.
 * @param timestampConfiguration      configuration for working with cache element timestamps, cannot be omitted
 *                                    if {@code useTimestampBasedComparison == true}.
 * @author Alik
 * @see CacheConfiguration
 */
@Immutable
@ThreadSafe
public record ImmutableCacheConfiguration(
        @Nonnull String cacheName,
        @Nonnull CacheType cacheType,
        @Nonnull Set<String> cacheAliases,
        boolean useTimestampBasedComparison,
        @Nonnegative Optional<TimestampCacheConfiguration> timestampConfiguration) implements CacheConfiguration {

    public ImmutableCacheConfiguration(@Nonnull String cacheName, @Nonnull CacheType cacheType) {
        this(cacheName, cacheType, Collections.emptySet(), false, Optional.empty());
    }

    public ImmutableCacheConfiguration {
        Objects.requireNonNull(cacheType, "cacheType");

        if (cacheName == null || cacheName.isEmpty()) {
            throw new InvalidCacheConfigurationException("cacheName must be not empty");
        }

        if (cacheType == CacheType.REPLICATED && !cacheAliases.isEmpty()) {
            throw new InvalidCacheConfigurationException("Aliases allowed only for invalidation cache");
        }

        if (useTimestampBasedComparison && timestampConfiguration.isEmpty()) {
            throw new InvalidCacheConfigurationException("When stamp based comparison enabled then timestamp configuration must present");
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

    /**
     * Returns a builder for constructing a cache configuration object.
     *
     * @return cannot be {@code null}.
     * @see Builder
     */
    @Nonnull
    public static Builder builder() {
        return new Builder();
    }

    @NotThreadSafe
    public static class Builder {

        private String cacheName;
        private CacheType cacheType;
        private boolean useTimestampBasedComparison;
        private TimestampCacheConfiguration timestampConfiguration = new ImmutableTimestampCacheConfiguration(128, TimeUnit.MINUTES.toMillis(30));
        private final Set<String> cacheAliases = new HashSet<>();

        /**
         * Sets the name of the local cache.
         *
         * @param cacheName name of the cache, cannot be {@code null}.
         * @return the builder for further configuration, cannot be {@code null}.
         */
        @Nonnull
        public Builder setCacheName(@Nonnull String cacheName) {
            this.cacheName = cacheName;
            return this;
        }

        /**
         * Sets the type of the cache.
         *
         * @param cacheType type of cache, cannot be {@code null}.
         * @return the builder for further configuration, cannot be {@code null}.
         */
        @Nonnull
        public Builder setCacheType(@Nonnull CacheType cacheType) {
            this.cacheType = cacheType;
            return this;
        }

        /**
         * Adds a cache alias to the list of aliases.<br>
         * See the documentation for {@linkplain CacheConfiguration#cacheAliases()} for more information on aliases.
         *
         * @param cacheAlias alias of the cache, cannot be {@code null}.
         * @return the builder for further configuration, cannot be {@code null}.
         * @see CacheConfiguration#cacheAliases()
         */
        @Nonnull
        public Builder addCacheAlias(@Nonnull String cacheAlias) {
            this.cacheAliases.add(cacheAlias);
            return this;
        }

        /**
         * Sets the list of aliases for the cache.<br>
         * See the documentation for {@linkplain CacheConfiguration#cacheAliases()} for more information on aliases..
         *
         * @param cacheAliases aliases of the cache, cannot be {@code null}.
         * @return the builder for further configuration, cannot be {@code null}.
         * @see CacheConfiguration#cacheAliases()
         */
        @Nonnull
        public Builder setCacheAliases(@Nonnull Set<String> cacheAliases) {
            this.cacheAliases.clear();
            this.cacheAliases.addAll(cacheAliases);
            return this;
        }

        /**
         * Sets the flag indicating whether timestamps should be used when applying changes from remote servers to the local cache.<br>
         * Before setting the value, carefully review the documentation for {@linkplain CacheConfiguration#useTimestampBasedComparison()}.<br>
         * By default, {@code false}.
         *
         * @param useTimestampBasedComparison the flag indicating whether timestamps should be used.
         * @return cannot be {@code null}.
         */
        @Nonnull
        public Builder useTimestampBasedComparison(final boolean useTimestampBasedComparison) {
            this.useTimestampBasedComparison = useTimestampBasedComparison;
            return this;
        }

        /**
         * Sets the configuration used for working with cache element timestamps.
         * By default, a value of {@code 128} is used for {@linkplain TimestampCacheConfiguration#probableAverageElementsCount()}
         * and 30 minutes for {@linkplain TimestampCacheConfiguration#timestampExpiration()}, not explicitly specified.<br>
         * If {@code useTimestampBasedComparison == false}, the value is ignored.
         *
         * @param timestampConfiguration configuration for working with cache element timestamps, can be {@code null}.
         * @return cannot be {@code null}.
         */
        @Nonnull
        public Builder setTimestampConfiguration(final TimestampCacheConfiguration timestampConfiguration) {
            this.timestampConfiguration = timestampConfiguration;
            return this;
        }

        /**
         * Creates a cache configuration object based on the provided data.
         *
         * @return the builder for further configuration, cannot be {@code null}.
         */
        @Nonnull
        public CacheConfiguration build() {
            return new ImmutableCacheConfiguration(
                    this.cacheName,
                    this.cacheType,
                    new HashSet<>(this.cacheAliases),
                    this.useTimestampBasedComparison,
                    Optional.ofNullable(this.timestampConfiguration)
            );
        }
    }
}
