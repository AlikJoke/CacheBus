package net.cache.bus.core;

import javax.annotation.Nonnull;
import java.io.Serializable;

/**
 * Регистратор слушателей событий по изменению элементов кэша.
 *
 * @author Alik
 * @see Cache
 * @see CacheEventListener
 */
public interface CacheEventListenerRegistrar {

    /**
     * Регистрирует слушатели событий по изменению элементов кэша для заданного кэша.
     *
     * @param cacheBus шина кэшей, для которой регистрируется слушатель, не может быть {@code null}.
     * @param cache    кэш, для которого производится регистрация слушателей, не может быть {@code null}.
     * @param <K>      тип ключа кэша, должен быть сериализуемым
     * @param <V>      тип значения кэша, должен быть сериализуемым
     */
    <K extends Serializable, V extends Serializable> void registerFor(
            @Nonnull CacheBus cacheBus,
            @Nonnull Cache<K, V> cache
    );

    /**
     * Снимает зарегистрированные ранее слушатели событий по изменению элементов кэша для заданного кэша.
     *
     * @param cacheBus шина кэшей, для которой ранее были зарегистрированы слушатели, не может быть {@code null}.
     * @param cache    кэш, для которого ранее были зарегистрированы слушатели, не может быть {@code null}.
     * @param <K>      тип ключа кэша, должен быть сериализуемым
     * @param <V>      тип значения кэша, должен быть сериализуемым
     */
    <K extends Serializable, V extends Serializable> void unregisterFor(
            @Nonnull CacheBus cacheBus,
            @Nonnull Cache<K, V> cache
    );
}
