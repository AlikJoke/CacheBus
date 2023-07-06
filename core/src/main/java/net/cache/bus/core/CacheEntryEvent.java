package net.cache.bus.core;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.time.Instant;

/**
 * Событие изменения элемента кэша.
 *
 * @param <K> тип ключа кэша, должен быть сериализуемым
 * @param <V> тип значения кэша, должен быть сериализуемым
 * @author Alik
 * @see CacheEntryEventType
 */
public interface CacheEntryEvent<K extends Serializable, V extends Serializable> {

    String ALL_ENTRIES_KEY = "*";

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

    /**
     * Применяет событие изменения элемента кэша к инвалидационному кэшу.
     *
     * @param cache инвалидационный кэш, не может быть {@code null}.
     */
    default void applyToInvalidatedCache(@Nonnull Cache<K, V> cache) {
        processEviction(cache);
    }

    /**
     * Применяет событие изменения кэша к реплицируемому кэшу.
     *
     * @param cache реплицируемый кэш, не может быть {@code null}.
     */
    default void applyToReplicatedCache(@Nonnull Cache<K, V> cache) {
        final V newVal = newValue();
        if (newVal == null) {
            processEviction(cache);
        } else {
            // TODO implementation based on timestamp of event
            cache.merge(key(), newVal, (v1, v2) -> oldValue() == null || v1.equals(oldValue()) ? v2 : null);
        }
    }

    private void processEviction(@Nonnull Cache<K, V> cache) {
        if (ALL_ENTRIES_KEY.equals(key())) {
            cache.clear();
        } else {
            cache.evict(key());
        }
    }
}
