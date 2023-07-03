package net.cache.bus.core.transport;

import net.cache.bus.core.CacheEntryEvent;

import javax.annotation.Nonnull;

/**
 * Сериализатор сообщений об изменении элементов кэша, используемый при передаче сообщения на другие сервера.
 * Определяет правила преобразования в "транспортное" представления данных из событий типа {@link CacheEntryEvent}.
 * Должен быть симметричен логике преобразования из {@link CacheEntryEventDeserializer}.
 *
 * @param <T> тип передаваемых данных
 * @author Alik
 * @see CacheEntryEvent
 * @see CacheEntryEventDeserializer
 */
public interface CacheEntryEventSerializer<T> {

    /**
     * Выполняет сериализацию события об изменении элемента кэша в "транспортное" представление.
     *
     * @param event событие об изменении элемента кэша, не может быть {@code null}.
     * @param <K>   тип ключа элемента кэша
     * @param <V>   тип значения элемента кэша
     * @return сериализованное представление события об изменении элемента кэша, не может быть {@code null}.
     */
    @Nonnull
    <K, V> T serialize(@Nonnull CacheEntryEvent<K, V> event);
}
