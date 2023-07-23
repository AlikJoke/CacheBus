package net.cache.bus.core.transport;

import net.cache.bus.core.CacheEntryEvent;

import javax.annotation.Nonnull;
import java.io.Serializable;

/**
 * Cache element change message converter used for transmitting messages to other servers
 * and receiving messages from other servers.<br>
 * Defines the rules for converting data from {@link CacheEntryEvent} events into a "transport" binary representation
 * and vice versa, converting from binary representation to an object of type {@link CacheEntryEvent}.
 *
 * @author Alik
 * @see CacheEntryEvent
 */
public interface CacheEntryEventConverter {

    /**
     * Serializes the cache element change event into a binary transport representation.
     *
     * @param event                the cache element change event, cannot be {@code null}.
     * @param serializeValueFields indicates whether value fields should be serialized
     * @param <K>                  the key type of the cache element, must be serializable
     * @param <V>                  the value type of the cache element, must be serializable
     * @return the serialized binary representation of the cache element change event, cannot be {@code null}.
     */
    @Nonnull
    <K extends Serializable, V extends Serializable> byte[] toBinary(
            @Nonnull CacheEntryEvent<K, V> event,
            boolean serializeValueFields
    );

    /**
     * Deserializes the cache element change event from a "transport" binary representation to an object of type {@link CacheEntryEvent}.
     *
     * @param data the cache element change event in binary format, cannot be {@code null}.
     * @param <K>  the key type of the cache element, must be serializable
     * @param <V>  the value type of the cache element, must be serializable
     * @return an object of type {@link CacheEntryEvent}, cannot be {@code null}.
     */
    @Nonnull
    <K extends Serializable, V extends Serializable> CacheEntryEvent<K, V> fromBinary(@Nonnull byte[] data);
}
