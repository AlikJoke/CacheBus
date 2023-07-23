package net.cache.bus.core.state;

import javax.annotation.Nonnull;

/**
 * Cache bus state. Contains information about the state of the cache bus (overall) and its individual components.
 *
 * @author Alik
 * @see ComponentState
 */
public interface CacheBusState extends ComponentState {

    /**
     * Returns information about the state of the channel for incoming/outgoing messages.
     *
     * @return cannot be {@code null}.
     */
    @Nonnull
    ComponentState channelState();

    /**
     * Returns information about the state of the processing queue for incoming messages from other servers
     * regarding cache item changes.
     *
     * @return cannot be {@code null}.
     */
    @Nonnull
    ComponentState processingQueueState();

    /**
     * Returns information about the state of the sending queue for outgoing messages regarding cache item changes.
     *
     * @return cannot be {@code null}.
     */
    @Nonnull
    ComponentState sendingQueueState();

    /**
     * Returns information about the state of the cache manager.
     *
     * @return cannot be {@code null}.
     */
    @Nonnull
    ComponentState cacheManagerState();
}
