package net.cache.bus.core.configuration;

import net.cache.bus.core.transport.CacheBusMessageChannel;
import net.cache.bus.core.transport.CacheEntryEventConverter;

import javax.annotation.Nonnull;

/**
 * Конфигурация транспорта для данной шины кэшей.
 *
 * @author Alik
 * @see CacheEntryEventConverter
 * @see CacheBusConfiguration
 * @see CacheBusMessageChannel
 */
public interface CacheBusTransportConfiguration {

    /**
     * Возвращает конвертер для формирования "транспортного" представления события
     * об изменении элемента кэша и обратного преобразования из бинарного представления
     * в объект типа {@link net.cache.bus.core.CacheEntryEvent}.
     *
     * @return не может быть {@code null}.
     * @see CacheEntryEventConverter
     */
    @Nonnull
    CacheEntryEventConverter converter();

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
