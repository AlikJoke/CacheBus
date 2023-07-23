package net.cache.bus.core;

import net.cache.bus.core.configuration.CacheBusConfiguration;
import net.cache.bus.core.state.CacheBusState;
import net.cache.bus.core.state.ComponentState;

import javax.annotation.Nonnull;
import java.io.Serializable;

/**
 * The basic abstraction of the cache change bus that propagates events across servers.
 * It is responsible for transmitting changes in the local cache of a server other servers,
 * as well as receiving changes from the local caches of other servers and applying them to the current local server and its caches.
 *
 * @author Alik
 * @see CacheEntryEvent
 * @see CacheManager
 * @see Cache
 */
public interface CacheBus {

    /**
     * Sends events about changes in local cache items to other servers interested in the changes.
     *
     * @param event the event of changing a local cache item, cannot be {@code null}.
     * @param <K>   the type of the cache key
     * @param <V>   the type of the cache value
     */
    <K extends Serializable, V extends Serializable> void send(@Nonnull CacheEntryEvent<K, V> event);

    /**
     * Retrieves the serialized binary representation of the cache item change event from other servers
     * and applies it to the local cache.
     *
     * @param binaryEventData the serialized binary representation of the remote cache item change event, cannot be {@code null}.
     */
    void receive(@Nonnull byte[] binaryEventData);

    /**
     * Sets the cache bus configuration.
     *
     * @param configuration the cache bus configuration, cannot be {@code null}.
     */
    void withConfiguration(@Nonnull CacheBusConfiguration configuration);

    /**
     * Returns the cache bus configuration being used.
     *
     * @return the cache bus configuration, cannot be {@code null}.
     * @see CacheBusConfiguration
     */
    CacheBusConfiguration configuration();

    /**
     * Returns the state this cache bus and other components.
     *
     * @return cannot be {@code null}.
     * @see CacheBusState
     * @see ComponentState
     */
    @Nonnull
    CacheBusState state();
}
