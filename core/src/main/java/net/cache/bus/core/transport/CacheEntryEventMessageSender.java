package net.cache.bus.core.transport;

import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.configuration.CacheBusConfiguration;

import javax.annotation.Nonnull;

/**
 * Отправитель сообщений об изменении элементов кэша на другие сервера.
 *
 * @author Alik
 * @see CacheEntryEvent
 * @see CacheEntryEventSerializer
 */
public interface CacheEntryEventMessageSender {

    /**
     * Выполняет отправку событий об изменении элементов кэша на другие сервера.
     *
     * @param event событие об изменении элемента кэша, не может быть {@code null}.
     * @param <K>   тип ключа элемента кэша
     * @param <V>   тип значения элемента кэша
     */
    <K, V> void send(@Nonnull CacheEntryEvent<K, V> event);

    /**
     * Устанавливает конфигурацию шины кэшей, содержащую информацию о транспортных настройках для отправки событий.
     *
     * @param cacheConfigurations конфигурация шины, не может быть {@code null}.
     * @see CacheBusConfiguration
     */
    void setCacheConfigurations(@Nonnull CacheBusConfiguration cacheConfigurations);
}
