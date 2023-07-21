package net.cache.bus.core.impl.configuration;

import net.cache.bus.core.configuration.CacheConfiguration;
import net.cache.bus.core.configuration.CacheType;
import net.cache.bus.core.configuration.InvalidCacheConfigurationException;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Неизменяемая реализация конфигурации одного кэша для шины.
 *
 * @param cacheName                    имя кэша, к которому относится конфигурация, не может быть {@code null}.
 * @param cacheType                    тип кэша, не может быть {@code null}.
 * @param cacheAliases                 дополнительные алиасы кэша, не может быть {@code null}.
 * @param useStampBasedComparison      нужно ли использовать метки времени для определения необходимости применения изменений к локальному кэшу
 * @param probableAverageElementsCount вероятное количество элементов в кэше (в среднем).
 *                                     не может быть отрицательным; если {@code useStampBasedComparison == false}, то данное свойство задать не требуется.
 * @author Alik
 * @see CacheConfiguration
 */
@Immutable
@ThreadSafe
public record ImmutableCacheConfiguration(
        @Nonnull String cacheName,
        @Nonnull CacheType cacheType,
        @Nonnull Set<String> cacheAliases,
        boolean useStampBasedComparison,
        @Nonnegative int probableAverageElementsCount) implements CacheConfiguration {

    public ImmutableCacheConfiguration(@Nonnull String cacheName, @Nonnull CacheType cacheType) {
        this(cacheName, cacheType, Collections.emptySet(), false, 0);
    }

    public ImmutableCacheConfiguration {
        Objects.requireNonNull(cacheType, "cacheType");

        if (cacheName == null || cacheName.isEmpty()) {
            throw new InvalidCacheConfigurationException("cacheName must be not empty");
        }

        if (cacheType == CacheType.REPLICATED && !cacheAliases.isEmpty()) {
            throw new InvalidCacheConfigurationException("Aliases allowed only for invalidation cache");
        }

        if (useStampBasedComparison && probableAverageElementsCount <= 0) {
            throw new InvalidCacheConfigurationException("When stamp based comparison enabled then probable average cache elements count must be positive");
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
        private boolean useStampBasedComparison;
        private int probableAverageElementsCount = 128;
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
         * Перед установкой значения нужно внимательно ознакомиться с документацией к {@linkplain CacheConfiguration#useStampBasedComparison()}.<br>
         * По-умолчанию {@code false}.
         *
         * @param useStampBasedComparison признак использования меток времени.
         * @return не может быть {@code null}.
         */
        @Nonnull
        public Builder useStampBasedComparison(final boolean useStampBasedComparison) {
            this.useStampBasedComparison = useStampBasedComparison;
            return this;
        }

        /**
         * Устанавливает ожидаемое (предполагаемое) количество элементов в кэше в среднем.
         * Значение не должно быть точным, достаточно приблизительного значения.
         * По-умолчанию используется значение {@code 128}, если явно не задано.<br>
         * Если {@code useStampBasedComparison == false}, то значение игнорируется.
         *
         * @param probableAverageElementsCount вероятное количество элементов в кэше в среднем.
         * @return не может быть {@code null}.
         */
        @Nonnull
        public Builder setProbableAverageElementsCount(@Nonnegative final int probableAverageElementsCount) {
            this.probableAverageElementsCount = probableAverageElementsCount;
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
                    this.useStampBasedComparison,
                    this.probableAverageElementsCount
            );
        }
    }
}
