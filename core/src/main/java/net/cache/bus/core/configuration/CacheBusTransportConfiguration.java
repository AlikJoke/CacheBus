package net.cache.bus.core.configuration;

import net.cache.bus.core.transport.CacheEntryEventDeserializer;
import net.cache.bus.core.transport.CacheBusMessageChannel;
import net.cache.bus.core.transport.CacheEntryEventSerializer;

import javax.annotation.Nonnull;

/**
 * Конфигурация транспорта для данной шины кэшей.
 *
 * @author Alik
 * @see CacheEntryEventSerializer
 * @see CacheEntryEventDeserializer
 * @see CacheBusConfiguration
 * @see CacheBusMessageChannel
 */
public interface CacheBusTransportConfiguration {

    /**
     * Возвращает сериализатор для формирования "транспортного" представления события об изменении элемента кэша.
     *
     * @return не может быть {@code null}.
     * @see CacheEntryEventSerializer
     */
    @Nonnull
    CacheEntryEventSerializer serializer();

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
     * Возвращает канал сообщений, используемый для взаимодействия с другими серверами.
     *
     * @return канал сообщений, не может быть {@code null}.
     * @see CacheBusMessageChannel
     */
    @Nonnull
    CacheBusMessageChannel<CacheBusMessageChannelConfiguration> messageChannel();

    /**
     * Возвращает конфигурацию канала сообщений шины кэшей.
     *
     * @return не может быть {@code null}.
     * @see CacheBusMessageChannelConfiguration
     */
    @Nonnull
    CacheBusMessageChannelConfiguration messageChannelConfiguration();
}
