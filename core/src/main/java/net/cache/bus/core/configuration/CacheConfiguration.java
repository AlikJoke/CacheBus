package net.cache.bus.core.configuration;

import javax.annotation.Nonnull;

/**
 * Конфигурация распределенного кэша.
 *
 * @author Alik
 */
public interface CacheConfiguration {

    /**
     * Возвращает имя кэша, к которому применяется конфигурация.
     *
     * @return имя кэша, не может быть {@code null}.
     */
    @Nonnull
    String cacheName();

    /**
     * Возвращает тип распределенного кэша.
     *
     * @return тип кэша, не может быть {@code null}.
     * @see CacheType
     */
    @Nonnull
    CacheType cacheType();
}
