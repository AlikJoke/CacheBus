package net.cache.bus.core.configuration;

import net.cache.bus.core.transport.CacheEntryEventDeserializer;
import net.cache.bus.core.transport.CacheEntryEventMessageSender;
import net.cache.bus.core.transport.CacheEntryEventSerializer;

import javax.annotation.Nonnull;

/**
 * Конфигурация транспорта для данной шины кэшей.
 *
 * @author Alik
 * @see CacheEntryEventSerializer
 * @see CacheEntryEventDeserializer
 * @see CacheBusConfiguration
 */
public interface CacheBusTransportConfiguration {

    /**
     * Возвращает имя конечной точки для отправки данных об изменениях элементов кэша.
     *
     * @return не может быть {@code null}.
     */
    @Nonnull
    String targetEndpoint();

    /**
     * Возвращает сериализатор для формирования "транспортного" представления события об изменении элемента кэша.
     *
     * @return не может быть {@code null}.
     * @see CacheEntryEventSerializer
     */
    @Nonnull
    CacheEntryEventSerializer serializer();

    /**
     * Возвращает имя конечной точки для получения данных об изменениях элементов кэша.
     *
     * @return не может быть {@code null}.
     */
    @Nonnull
    String sourceEndpoint();

    /**
     * Возвращает десериализатор для формирования объекта {@link net.cache.bus.core.CacheEntryEvent} из
     * "транспортного" представления события об изменении элемента кэша.
     *
     * @return не может быть {@code null}.
     * @see CacheEntryEventDeserializer
     */
    @Nonnull
    CacheEntryEventDeserializer deserializer();

    /**
     * Возвращает отправителя сообщений, используемого для шины кэшей.
     *
     * @return отправитель сообщений, не может быть {@code null}.
     */
    @Nonnull
    CacheEntryEventMessageSender messageSender();
}
