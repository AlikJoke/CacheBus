package net.cache.bus.core;

/**
 * Тип изменения элемента кэша.
 *
 * @author Alik
 * @see CacheEntryEvent
 */
public enum CacheEntryEventType {

    /**
     * Добавление элемента в кэш
     */
    ADDED,

    /**
     * Удаление элемента из кэша
     */
    EVICTED,

    /**
     * Значение имеющегося в кэше элемента изменено
     */
    UPDATED
}
