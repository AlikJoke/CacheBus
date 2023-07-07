package net.cache.bus.core.transport;

import net.cache.bus.core.CacheEntryEvent;

import javax.annotation.Nonnull;
import java.io.Serializable;

/**
 * Конвертер сообщений об изменении элементов кэша, используемый при передаче сообщения на другие сервера
 * и при получении сообщений с других серверов.
 * Определяет правила преобразования в "транспортное" бинарное представление данных из событий типа {@link CacheEntryEvent}
 * и обратного преобразования из бинарного представления в объект типа {@link CacheEntryEvent}.
 *
 * @author Alik
 * @see CacheEntryEvent
 */
public interface CacheEntryEventConverter {

    /**
     * Выполняет сериализацию события об изменении элемента кэша в бинарное транспортное представление.
     *
     * @param event                событие об изменении элемента кэша, не может быть {@code null}.
     * @param serializeValueFields признак, нужно ли сериализовывать поля со значениями
     * @param <K>                  тип ключа элемента кэша, должен быть сериализуемым
     * @param <V>                  тип значения элемента кэша, должен быть сериализуемым
     * @return сериализованное бинарное представление события об изменении элемента кэша, не может быть {@code null}.
     */
    @Nonnull
    <K extends Serializable, V extends Serializable> byte[] toBinary(
            @Nonnull CacheEntryEvent<K, V> event,
            boolean serializeValueFields
    );

    /**
     * Выполняет десериализацию события об изменении элемента кэша из "транспортного" бинарного представления в объект типа {@link CacheEntryEvent}.
     *
     * @param data событие об изменении элемента кэша в бинарном формате, не может быть {@code null}.
     * @param <K>  тип ключа элемента кэша, должен быть сериализуемым
     * @param <V>  тип значения элемента кэша, должен быть сериализуемым
     * @return объект типа {@link CacheEntryEvent}, не может быть {@code null}.
     */
    @Nonnull
    <K extends Serializable, V extends Serializable> CacheEntryEvent<K, V> fromBinary(@Nonnull byte[] data);
}
