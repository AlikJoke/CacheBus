package net.cache.bus.core.transport;

import net.cache.bus.core.CacheEntryEvent;

import javax.annotation.Nonnull;

/**
 * Отправитель сообщений об изменении элементов кэша на другие сервера.
 *
 * @author Alik
 * @see CacheEntryEvent
 * @see CacheEntryOutputMessage
 */
public interface CacheEntryEventMessageSender {

    /**
     * Выполняет отправку сериализованного события об изменении элемента кэша на другие сервера.
     *
     * @param eventOutputMessage исходящее сообщение с информацией об изменении элемента кэша, не может быть {@code null}.
     * @param targetEndpoint     целевая конечная точка для отправки событий об изменении элемента кэша, не может быть {@code null}.
     */
    void send(@Nonnull CacheEntryOutputMessage eventOutputMessage, @Nonnull String targetEndpoint);
}
