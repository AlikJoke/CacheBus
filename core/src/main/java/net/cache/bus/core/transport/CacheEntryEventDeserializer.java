package net.cache.bus.core.transport;

import net.cache.bus.core.CacheEntryEvent;

import javax.annotation.Nonnull;

/**
 * Десериализатор сообщений об изменении элементов кэша, используемый при получении сообщений с других серверов.
 * Определяет правила преобразования из "транспортного" представления данных в событие типа {@link CacheEntryEvent}.
 * Должен быть симметричен логике преобразования из {@link CacheEntryEventSerializer}.
 *
 * @param <T> тип передаваемых данных
 * @author Alik
 * @see CacheEntryEvent
 * @see CacheEntryEventSerializer
 */
public interface CacheEntryEventDeserializer<T> {

    /**
     * Выполняет десериализацию события об изменении элемента кэша из "транспортного" представления в объект типа {@link CacheEntryEvent}.
     *
     * @param data событие об изменении элемента кэша в виде "транспортного" представления, не может быть {@code null}.
     * @param <K>  тип ключа элемента кэша
     * @param <V>  тип значения элемента кэша
     * @return объект типа {@link CacheEntryEvent}, не может быть {@code null}.
     */
    @Nonnull
    <K, V> CacheEntryEvent<K, V> deserialize(@Nonnull T data);
}
