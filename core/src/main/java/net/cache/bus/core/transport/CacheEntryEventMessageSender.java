package net.cache.bus.core.transport;

import net.cache.bus.core.CacheEntryEvent;

import javax.annotation.Nonnull;

/**
 * Отправитель сообщений об изменении элементов кэша на другие сервера.
 *
 * @author Alik
 * @see CacheEntryEvent
 */
public interface CacheEntryEventMessageSender {

    /**
     * Выполняет отправку сериализованного события об изменении элемента кэша на другие сервера.
     *
     * @param serializedEvent событие об изменении элемента кэша в сериализованном виде, не может быть {@code null}.
     * @param targetEndpoint  целевая конечная точка для отправки событий об изменении элемента кэша, не может быть {@code null}.
     * @param <T>             тип сериализованного представления события об изменении элемента кэша
     */
    <T> void send(@Nonnull T serializedEvent, @Nonnull String targetEndpoint);
}
