package net.cache.bus.core;

import java.io.Serializable;

/**
 * Слушатель событий изменения кэша.
 *
 * @param <K> тип ключа кэша, должен быть сериализуемым
 * @param <V> тип значения кэша, должен быть сериализуемым
 * @author Alik
 */
public interface CacheEventListener<K extends Serializable, V extends Serializable> {
}
