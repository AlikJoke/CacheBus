package net.cache.bus.core;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;

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
     * Возвращает метку времени изменения элемента кэша (в миллисекундах относительно зоны UTC).
     *
     * @return метка времени изменения элемента кэша в миллисекундах
     */
    long eventTime();

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
     * Возвращает хэш ключ события.
     * @return хэш ключ события
     */
    default int computeEventHashKey() {
        int result = 31 + cacheName().hashCode();
        return 31 * result + key().hashCode();
    }

    /**
     * Применяет событие изменения элемента кэша к инвалидационному кэшу.<br>
     * Логика применения события заключается в удалении из локального кэша элемента с ключом из события.
     *
     * @param cache инвалидационный кэш, не может быть {@code null}.
     */
    default void applyToInvalidatedCache(@Nonnull Cache<K, V> cache) {
        processEviction(cache);
    }

    /**
     * Применяет событие изменения кэша к реплицируемому кэшу.<br>
     * <ul>
     * <li>Если получили событие добавления в кэш, то значение для замены (новое) всегда берем из события.
     * Далее при слиянии заменяем значение в локальном кэше только в случае, если на текущем сервере
     * значения в кэше нет; если есть, то сравниваем значение в локальном кэше и в событии: если совпали,
     * то оставляем имеющееся в кэше, иначе вычищаем данные из локального кэша для решения конфликта и
     * чтобы избежать неконсистентности.</li>
     * <li>Если получили событие модификации элемента кэша, то значение для замены берем из нового события
     * только в случае, если старое значение из события совпадает со значением элемента на данном сервере;
     * иначе, если значение в локальном кэше совпадает с новым значением в событии, то оставляем значение из
     * локального кэша неизменным; иначе конфликт и вычищаем данные из кэша, чтобы произошло удаление элемента
     * из кэша на данном сервере. Это повлечет некоторые накладные расходы, если такие ситуации будут частыми,
     * но зато безопаснее для целостного состояния данных в кэше.
     * В обычной ситуации это должно быть редким сценарием.</li>
     * <li>Если получили события удаления или просроченности (т.е. новое значение отсутствует в общем случае),
     * то удаляем элемент из текущего кэша локального сервера.</li>
     * </ul>
     * @param cache реплицируемый кэш, не может быть {@code null}.
     */
    default void applyToReplicatedCache(@Nonnull Cache<K, V> cache) {
        final V newVal = newValue();
        if (newVal == null) {
            processEviction(cache);
        } else {
            final V oldValueFromEvent = oldValue();
            cache.merge(
                    key(),
                    newVal,
                    (oldLocalValue, newValueFromEvent) ->
                            newValueFromEvent.equals(oldLocalValue)
                                    ? oldLocalValue
                                    : oldLocalValue.equals(oldValueFromEvent)
                                        ? newValueFromEvent
                                        : null
            );
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
