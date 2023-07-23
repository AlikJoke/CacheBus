package net.cache.bus.core.transport;

import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.CacheEventMessageConsumer;
import net.cache.bus.core.configuration.CacheBusMessageChannelConfiguration;
import net.cache.bus.core.state.ComponentState;

import javax.annotation.Nonnull;

/**
 * Abstraction of a cache element change message channel.
 *
 * @author Alik
 * @see CacheEntryEvent
 * @see CacheEntryOutputMessage
 * @see CacheBusMessageChannelConfiguration
 */
public interface CacheBusMessageChannel<T extends CacheBusMessageChannelConfiguration> extends AutoCloseable {

    /**
     * Activates the cache bus message channel.
     *
     * @param configuration configuration of channel, cannot be {@code null}.
     * @see CacheBusMessageChannelConfiguration
     */
    void activate(@Nonnull T configuration);

    /**
     * Sends a serialized cache element change event message to other servers.
     *
     * @param eventOutputMessage the outgoing message with information about the cache element change cannot be {@code null}.
     */
    void send(@Nonnull CacheEntryOutputMessage eventOutputMessage);

    /**
     * Creates a subscription to the incoming message stream of the channel on a dedicated thread pool.
     *
     * @param consumer the message processing function cannot be {@code null}.
     */
    void subscribe(@Nonnull CacheEventMessageConsumer consumer);

    /**
     * Closes the message channel, after which the channel becomes unavailable for sending/receiving messages.
     */
    @Override
    void close();

    /**
     * Returns information about the state of the interaction channel with other servers.
     *
     * @return cannot be {@code null}.
     * @see ComponentState
     */
    @Nonnull
    ComponentState state();
}
