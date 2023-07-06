package net.cache.bus.core.transport;

import net.cache.bus.core.CacheEntryEvent;

import javax.annotation.Nonnull;
import java.io.Serializable;

/**
 * Сериализатор сообщений об изменении элементов кэша, используемый при передаче сообщения на другие сервера.
 * Определяет правила преобразования в "транспортное" представления данных из событий типа {@link CacheEntryEvent}.
 * Должен быть симметричен логике преобразования из {@link CacheEntryEventDeserializer}.
 *
 * @author Alik
 * @see CacheEntryEvent
 * @see CacheEntryEventDeserializer
 */
public interface CacheEntryEventSerializer {

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
    <K extends Serializable, V extends Serializable> byte[] serialize(
            @Nonnull CacheEntryEvent<K, V> event,
            boolean serializeValueFields
    );
}
