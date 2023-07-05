package net.cache.bus.core;

import javax.annotation.Nonnull;

/**
 * Регистратор слушателей событий по изменению элементов кэша.
 *
 * @author Alik
 * @see Cache
 * @see CacheEventListener
 */
public interface CacheEventListenerRegistrar {

    /**
     * Регистрирует слушатели событий по изменению элементов кэша для данного кэша.
     *
     * @param cache кэш, для которого производится регистрация слушателей, не может быть {@code null}.
     * @param <K>   тип ключа кэша
     * @param <V>   тип значения кэша
     */
    <K, V> void registerFor(@Nonnull Cache<K, V> cache);
}
