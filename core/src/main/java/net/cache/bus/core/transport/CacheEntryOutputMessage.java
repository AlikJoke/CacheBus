package net.cache.bus.core.transport;

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
     * Возвращает бинарное представление события об изменении элемента кэша
     *
     * @return не может быть {@code null}.
     */
    @Nonnull
    byte[] cacheEntryMessageBody();

    /**
     * Возвращает хэш-ключ сообщения.<br>
     * Хэш должен высчитываться на основе двух полей: имени кэша и ключа, для которого произошло изменение.
     *
     * @return хэш-ключ соытия
     */
    int messageHashKey();
}
