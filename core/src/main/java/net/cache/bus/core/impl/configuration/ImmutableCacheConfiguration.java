package net.cache.bus.core.impl.configuration;

import net.cache.bus.core.configuration.CacheConfiguration;
import net.cache.bus.core.configuration.CacheType;
import net.cache.bus.core.configuration.InvalidCacheConfigurationException;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Неизменяемая реализация конфигурации одного кэша для шины.
 *
 * @param cacheName                   имя кэша, к которому относится конфигурация, не может быть {@code null}.
 * @param cacheType                   тип кэша, не может быть {@code null}.
 * @param cacheAliases                дополнительные алиасы кэша, не может быть {@code null}.
 * @param useTimestampBasedComparison нужно ли использовать метки времени для определения необходимости применения изменений к локальному кэшу.
 * @param timestampConfiguration      конфигурация работы с временными метками элементов кэшей, не может отсутствовать, если {@code useTimestampBasedComparison == true}.
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
     * Возвращает построитель для формирования объекта конфигурации кэша.
     *
     * @return не может быть {@code null}.
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
         * Устанавливает имя локального кэша.
         *
         * @param cacheName имя кэша, не может быть {@code null}.
         * @return построитель для дальнейшего формирования конфигурации, не может быть {@code null}.
         */
        @Nonnull
        public Builder setCacheName(@Nonnull String cacheName) {
            this.cacheName = cacheName;
            return this;
        }

        /**
         * Устанавливает тип кэша.
         *
         * @param cacheType тип кэша, не может быть {@code null}.
         * @return построитель для дальнейшего формирования конфигурации, не может быть {@code null}.
         */
        @Nonnull
        public Builder setCacheType(@Nonnull CacheType cacheType) {
            this.cacheType = cacheType;
            return this;
        }

        /**
         * Добавляет алиас кэша в список алиасов.<br>
         * См. информацию об алиасах в документации к {@linkplain CacheConfiguration#cacheAliases()}.
         *
         * @param cacheAlias алиас кэша, не может быть {@code null}.
         * @return построитель для дальнейшего формирования конфигурации, не может быть {@code null}.
         * @see CacheConfiguration#cacheAliases()
         */
        @Nonnull
        public Builder addCacheAlias(@Nonnull String cacheAlias) {
            this.cacheAliases.add(cacheAlias);
            return this;
        }

        /**
         * Устанавливает список алиасов для кэша.<br>
         * См. информацию об алиасах в документации к {@linkplain CacheConfiguration#cacheAliases()}.
         *
         * @param cacheAliases алиасы кэша, не может быть {@code null}.
         * @return построитель для дальнейшего формирования конфигурации, не может быть {@code null}.
         * @see CacheConfiguration#cacheAliases()
         */
        @Nonnull
        public Builder setCacheAliases(@Nonnull Set<String> cacheAliases) {
            this.cacheAliases.clear();
            this.cacheAliases.addAll(cacheAliases);
            return this;
        }

        /**
         * Устанавливает признак использования меток времени при применении изменений с удаленных серверов к локальному кэшу.<br>
         * Перед установкой значения нужно внимательно ознакомиться с документацией к {@linkplain CacheConfiguration#useTimestampBasedComparison()}.<br>
         * По-умолчанию {@code false}.
         *
         * @param useTimestampBasedComparison признак использования меток времени.
         * @return не может быть {@code null}.
         */
        @Nonnull
        public Builder useTimestampBasedComparison(final boolean useTimestampBasedComparison) {
            this.useTimestampBasedComparison = useTimestampBasedComparison;
            return this;
        }

        /**
         * Устанавливает конфигурацию, используемую для работы с временными метками элементов кэша.
         * По-умолчанию используется значение {@code 128} в качестве {@linkplain TimestampCacheConfiguration#probableAverageElementsCount()}
         * и 30 минут в качестве {@linkplain TimestampCacheConfiguration#timestampExpiration()}, если явно не задано.<br>
         * Если {@code useTimestampBasedComparison == false}, то значение игнорируется.
         *
         * @param timestampConfiguration конфигурация работы с временными метками элементов кэша, может быть {@code null}.
         * @return не может быть {@code null}.
         */
        @Nonnull
        public Builder setTimestampConfiguration(final TimestampCacheConfiguration timestampConfiguration) {
            this.timestampConfiguration = timestampConfiguration;
            return this;
        }

        /**
         * Формирует объект конфигурации кэша на основе переданных данных.
         *
         * @return построитель для дальнейшего формирования конфигурации, не может быть {@code null}.
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
