package net.cache.bus.core;

import net.cache.bus.core.configuration.CacheBusConfiguration;
import net.cache.bus.core.state.CacheBusState;
import net.cache.bus.core.state.ComponentState;

import javax.annotation.Nonnull;
import java.io.Serializable;

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
    <K extends Serializable, V extends Serializable> void send(@Nonnull CacheEntryEvent<K, V> event);

    /**
     * Выполняет получение сериализованного бинарного представления события об изменении элемента кэша с других серверов
     * и применяет его к локальному кэшу.
     *
     * @param binaryEventData сериализованное бинарное представление события изменения элемента удаленного кэша, не может быть {@code null}.
     */
    void receive(@Nonnull byte[] binaryEventData);

    /**
     * Устанавливает конфигурацию шины кэшей.
     *
     * @param configuration конфигурация шины кэшей, не может быть {@code null}.
     */
    void withConfiguration(@Nonnull CacheBusConfiguration configuration);

    /**
     * Возвращает используемую конфигурацию шины кэшей.
     *
     * @return конфигурацию шины кэшей, не может быть {@code null}.
     * @see CacheBusConfiguration
     */
    CacheBusConfiguration configuration();

    /**
     * Возвращает состояние данной шины кэшей и еще компонентов.
     *
     * @return не может быть {@code null}.
     * @see CacheBusState
     * @see ComponentState
     */
    @Nonnull
    CacheBusState state();
}
