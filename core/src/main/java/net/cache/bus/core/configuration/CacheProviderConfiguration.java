package net.cache.bus.core.configuration;

import net.cache.bus.core.CacheEventListenerRegistrar;
import net.cache.bus.core.CacheManager;

import javax.annotation.Nonnull;

/**
 * Конфигурация, специфичная провайдеру (реализации) кэширования,
 * которая необходима для работы шины кэшей.
 *
 * @author Alik
 * @see CacheManager
 * @see CacheEventListenerRegistrar
 */
public interface CacheProviderConfiguration {

    /**
     * Возвращает менеджер кэшей.
     *
     * @return не может быть {@code null}.
     * @see CacheManager
     */
    @Nonnull
    CacheManager cacheManager();

    /**
     * Возвращает регистратор слушателей событий изменения для кэшей.
     *
     * @return не может быть {@code null}.
     * @see CacheEventListenerRegistrar
     */
    @Nonnull
    CacheEventListenerRegistrar cacheEventListenerRegistrar();
}
