package net.cache.bus.core;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;

/**
 * Событие изменения элемента кэша.
 *
 * @param <K> тип ключа кэша
 * @param <V> тип значения кэша
 * @author Alik
 * @see CacheEntryEventType
 */
public interface CacheEntryEvent<K, V> {

    /**
     * Возвращает ключ измененного элемента кэша.
     * Если значение ключа соответствует {@literal *}, то изменение применяется ко всем элементам кэша.
     *
     * @return не может быть {@code null}.
     */
    @Nonnull
    K key();

    /**
     * Возвращает старое значение элемента кэша.
     *
     * @return старое значение элемента кэша до модификации, может быть {@code null}.
     */
    @Nullable
    V oldValue();

    /**
     * Возвращает новое значение элемента кэша.
     *
     * @return новое значение элемента кэша после модификации, может быть {@code null}.
     */
    @Nullable
    V newValue();

    /**
     * Возвращает время изменения элемента кэша.
     *
     * @return не может быть {@code null}.
     */
    @Nonnull
    Instant eventTime();

    /**
     * Возвращает тип изменения элемента кэша.
     *
     * @return не может быть {@code null}.
     * @see CacheEntryEventType
     */
    @Nonnull
    CacheEntryEventType eventType();

    /**
     * Возвращает имя кэша, в котором произошло изменение элемента кэша.
     *
     * @return не может быть {@code null}.
     */
    @Nonnull
    String cacheName();
}
