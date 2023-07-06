package net.cache.bus.core.transport;

import net.cache.bus.core.CacheEntryEvent;

import javax.annotation.Nonnull;
import java.io.Serializable;

/**
 * Десериализатор сообщений об изменении элементов кэша, используемый при получении сообщений с других серверов.
 * Определяет правила преобразования из "транспортного" представления данных в событие типа {@link CacheEntryEvent}.
 * Должен быть симметричен логике преобразования из {@link CacheEntryEventSerializer}.
 *
 * @author Alik
 * @see CacheEntryEvent
 * @see CacheEntryEventSerializer
 */
public interface CacheEntryEventDeserializer {

    /**
     * Выполняет десериализацию события об изменении элемента кэша из "транспортного" бинарного представления в объект типа {@link CacheEntryEvent}.
     *
     * @param data событие об изменении элемента кэша в бинарном формате, не может быть {@code null}.
     * @param <K>  тип ключа элемента кэша, должен быть сериализуемым
     * @param <V>  тип значения элемента кэша, должен быть сериализуемым
     * @return объект типа {@link CacheEntryEvent}, не может быть {@code null}.
     */
    @Nonnull
    <K extends Serializable, V extends Serializable> CacheEntryEvent<K, V> deserialize(@Nonnull byte[] data);
}
