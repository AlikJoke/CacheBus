package net.cache.bus.core.configuration;

import net.cache.bus.core.transport.HostNameResolver;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

/**
 * Конфигурация канала сообщений шины кэшей.
 *
 * @author Alik
 * @see net.cache.bus.core.transport.CacheBusMessageChannel
 */
public interface CacheBusMessageChannelConfiguration {

    /**
     * Возвращает идентификатор канала сообщений.
     *
     * @return не может быть {@code null}.
     */
    @Nonnull
    String channel();

    /**
     * Возвращает определитель имени текущего хоста для фильтрации сообщений с локального сервера.
     *
     * @return не может быть {@code null}.
     * @see HostNameResolver
     */
    @Nonnull
    HostNameResolver hostNameResolver();

    /**
     * Возвращает тайм-аут переподключения в миллисекундах при разрыве соединений,
     * используемых для отправки сообщений в канал; по тайм-ауту
     * произойдет прекращение попытки восстановить соединение и будет
     * сгенерировано исключение; по-умолчанию используется значение
     * {@code 5} в минутах.
     *
     * @return не может быть отрицательным.
     */
    @Nonnegative
    long reconnectTimeoutMs();
}
