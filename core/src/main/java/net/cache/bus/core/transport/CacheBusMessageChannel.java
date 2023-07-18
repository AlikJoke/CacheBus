package net.cache.bus.core.transport;

import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.CacheEventMessageConsumer;
import net.cache.bus.core.configuration.CacheBusMessageChannelConfiguration;
import net.cache.bus.core.state.ComponentState;

import javax.annotation.Nonnull;

/**
 * Абстракция канала сообщений об изменении элементов кэша.
 *
 * @author Alik
 * @see CacheEntryEvent
 * @see CacheEntryOutputMessage
 * @see CacheBusMessageChannelConfiguration
 */
public interface CacheBusMessageChannel<T extends CacheBusMessageChannelConfiguration> extends AutoCloseable {

    /**
     * Производит активацию канала сообщений шины кэшей.
     *
     * @param configuration конфигурация канала, не может быть {@code null}.
     * @see CacheBusMessageChannelConfiguration
     */
    void activate(@Nonnull T configuration);

    /**
     * Выполняет отправку сериализованного события об изменении элемента кэша на другие сервера.
     *
     * @param eventOutputMessage исходящее сообщение с информацией об изменении элемента кэша, не может быть {@code null}.
     */
    void send(@Nonnull CacheEntryOutputMessage eventOutputMessage);

    /**
     * Создает подписку на входящий поток сообщений канала на выделенном пуле потоков.
     *
     * @param consumer функция обработки сообщений, не может быть {@code null}.
     */
    void subscribe(@Nonnull CacheEventMessageConsumer consumer);

    /**
     * Производит закрытие канала сообщений, после которого канал становится недоступен для отправок / получения сообщений.
     */
    @Override
    void close();

    /**
     * Возвращает информацию о состоянии канала взаимодействия с другими серверами.
     *
     * @return не может быть {@code null}.
     * @see ComponentState
     */
    @Nonnull
    ComponentState state();
}
