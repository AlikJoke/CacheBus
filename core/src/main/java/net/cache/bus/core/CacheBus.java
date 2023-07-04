package net.cache.bus.core;

import net.cache.bus.core.configuration.CacheBusConfiguration;

import javax.annotation.Nonnull;

/**
 * Базовая абстракция шины изменений в кэше, выполняющей распространение событий по серверам.
 * Берет на себя ответственность за передачу изменений в локальном кэше сервера на другие сервера,
 * а также получение изменений из локальных кэшей других серверов и применение их к текущему локальному серверу и его кэшам.
 *
 * @author Alik
 * @see CacheEntryEvent
 * @see CacheManager
 * @see Cache
 */
public interface CacheBus {

    /**
     * Выполняет отправку событий об изменении элементов локального кэша на другие сервера,
     * заинтересованные в изменениях данного кэша.
     *
     * @param event событие изменения элемента локального кэша, не может быть {@code null}.
     * @param <K>   тип ключа кэша
     * @param <V>   тип значения кэша
     */
    <K, V> void send(@Nonnull CacheEntryEvent<K, V> event);

    /**
     * Выполняет получение сериализованного представления события об изменении элемента кэша с других серверов
     * и применяет его к локальному кэшу.
     *
     * @param sourceEndpoint конечная точка, откуда было получено событие об изменении элемента кэша, не может быть {@code null}.
     * @param cacheName      имя удаленного кэша, в котором произошло изменение, не может быть {@code null}.
     * @param event          сериализованное представление события изменения элемента удаленного кэша, не может быть {@code null}.
     * @param <T>            тип сериализованного представления события изменения элемента кэша
     */
    <T> void receive(
            @Nonnull String sourceEndpoint,
            @Nonnull String cacheName,
            @Nonnull T event
    );

    /**
     * Устанавливает конфигурацию шины кэшей.
     *
     * @param configuration конфигурация шины кэшей, не может быть {@code null}.
     */
    void setConfiguration(@Nonnull CacheBusConfiguration configuration);

    /**
     * Выполняет активацию шины кэширования.
     */
    void start();

    /**
     * Выполняет остановку шины кэширования.
     */
    void stop();
}
