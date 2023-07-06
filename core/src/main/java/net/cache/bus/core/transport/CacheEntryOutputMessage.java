package net.cache.bus.core.transport;

import net.cache.bus.core.CacheEntryEventType;

import javax.annotation.Nonnull;

/**
 * Исходящее сообщение об изменении элемента кэша, содержащее бинарное
 * представление события изменения элемента кэша и метаинформацию об изменении.
 *
 * @author Alik
 * @see net.cache.bus.core.CacheEntryEvent
 */
public interface CacheEntryOutputMessage {

    /**
     * Возвращает имя кэша, в котором произошло изменение.
     *
     * @return не может быть {@code null}.
     */
    @Nonnull
    String cacheName();

    /**
     * Возвращает тип изменения элемента кэша.
     *
     * @return не может быть {@code null}.
     */
    @Nonnull
    CacheEntryEventType eventType();

    /**
     * Возвращает имя хоста, на котором произошло изменение.
     *
     * @return не может быть {@code null}.
     */
    @Nonnull
    String hostName();

    /**
     * Возвращает бинарное представление события об изменении элемента кэша
     *
     * @return не может быть {@code null}.
     */
    @Nonnull
    byte[] cacheEntryMessageBody();
}
