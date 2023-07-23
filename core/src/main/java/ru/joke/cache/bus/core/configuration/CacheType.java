package ru.joke.cache.bus.core.configuration;

/**
 * Distributed cache type.
 *
 * @author Alik
 */
public enum CacheType {

    /**
     * Invalidation cache
     */
    INVALIDATED(false),

    /**
     * Replicable cache
     */
    REPLICATED(true);

    private final boolean serializeValueFields;

    CacheType(final boolean serializeValueFields) {
        this.serializeValueFields = serializeValueFields;
    }

    /**
     * Returns a flag indicating whether serialize fields with the new and old values of the cache item.
     *
     * @return flag indicating whether to serialize fields with the new and old values of the cache item.
     */
    public boolean serializeValueFields() {
        return this.serializeValueFields;
    }
}
