package net.cache.bus.core;

import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cache element change type.
 *
 * @author Alik
 * @see CacheEntryEvent
 */
public enum CacheEntryEventType {

    /**
     * Addition of element to the cache
     */
    ADDED(1),

    /**
     * Expiration-based removal of a cache element
     */
    EXPIRED(2),

    /**
     * Removal of an element from the cache
     */
    EVICTED(3),

    /**
     * Modification of an existing cache element
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
