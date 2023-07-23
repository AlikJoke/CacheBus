package net.cache.bus.core.transport;

import javax.annotation.Nonnull;

/**
 * Outgoing cache element change message containing the binary
 * representation of the cache element change event and metadata about the change.
 *
 * @author Alik
 * @see net.cache.bus.core.CacheEntryEvent
 */
public interface CacheEntryOutputMessage {

    /**
     * Returns the name of the cache where the change occurred.
     *
     * @return cannot be {@code null}.
     */
    @Nonnull
    String cacheName();

    /**
     * Returns the binary representation of the cache element change event.
     *
     * @return cannot be {@code null}.
     */
    @Nonnull
    byte[] cacheEntryMessageBody();

    /**
     * Returns the message hash key.<br>
     * The hash should be calculated based on two fields: the cache name and the key for which the change occurred.
     *
     * @return message hash key
     */
    int messageHashKey();
}
