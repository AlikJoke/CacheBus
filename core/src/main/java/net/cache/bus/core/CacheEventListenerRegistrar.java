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
     * Регистрирует слушатели событий по изменению элементов кэша для данного кэша.
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
}
