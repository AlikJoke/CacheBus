package net.cache.bus.core;

import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Тип изменения элемента кэша.
 *
 * @author Alik
 * @see CacheEntryEvent
 */
public enum CacheEntryEventType {

    /**
     * Добавление элемента в кэш
     */
    ADDED(1),

    /**
     * Удаление элемента кэша по TTL
     */
    EXPIRED(2),

    /**
     * Удаление элемента из кэша
     */
    EVICTED(3),

    /**
     * Значение имеющегося в кэше элемента изменено
     */
    UPDATED(4);

    private static final Map<Integer, CacheEntryEventType> eventsById =
            Arrays.stream(values())
                    .collect(Collectors.toMap(CacheEntryEventType::getId, Function.identity()));

    private final int id;

    CacheEntryEventType(final int id) {
        this.id = id;
    }

    @Nonnegative
    public int getId() {
        return this.id;
    }

    @Nullable
    public static CacheEntryEventType valueOf(@Nonnegative final int id) {
        return eventsById.getOrDefault(id, null);
    }
}
