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
     * Возвращает пул потоков, на котором производится получение и обработка сообщений с других серверов.
     *
     * @return не может быть {@code null}.
     */
    @Nonnull
    ExecutorService receivingPool();

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
     * Обязательно ли сохранение порядка обрабатываемых сообщений.
     * В конфигурациях, когда используются только инвалидационные кэши и порядок не важен,
     * позволяет повысить производительность обработки, если не сохранять порядок.
     *
     * @return {@code true} - если нужно сохранять порядок, {@code false} - иначе.
     */
    boolean preserveOrder();
}
