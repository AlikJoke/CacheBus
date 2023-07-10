package net.cache.bus.core.configuration;

import net.cache.bus.core.transport.HostNameResolver;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutorService;

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
     * Возвращает пул потоков, на котором производится получение сообщений с других серверов.
     *
     * @return не может быть {@code null}.
     */
    @Nonnull
    ExecutorService subscribingPool();
}
