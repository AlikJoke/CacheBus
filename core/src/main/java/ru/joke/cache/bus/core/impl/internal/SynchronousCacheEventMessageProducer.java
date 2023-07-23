package ru.joke.cache.bus.core.impl.internal;

import ru.joke.cache.bus.core.configuration.CacheBusTransportConfiguration;
import ru.joke.cache.bus.core.impl.ImmutableComponentState;
import ru.joke.cache.bus.core.metrics.CacheBusMetricsRegistry;
import ru.joke.cache.bus.core.state.ComponentState;
import ru.joke.cache.bus.core.transport.CacheBusMessageChannel;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Synchronous implementation of an event sender to a channel without any intermediate stages.
 *
 * @author Alik
 * @see CacheBusMessageChannel
 */
@ThreadSafe
@Immutable
public final class SynchronousCacheEventMessageProducer extends CacheEventMessageProducer {

    private static final String PRODUCER_ID = "sync-message-producer";

    private volatile ComponentState state;

    public SynchronousCacheEventMessageProducer(
            @Nonnull CacheBusMetricsRegistry metrics,
            @Nonnull CacheBusTransportConfiguration transportConfiguration) {
        super(metrics, transportConfiguration);
        this.state = new ImmutableComponentState(PRODUCER_ID, ComponentState.Status.UP_OK);
    }

    @Nonnull
    @Override
    public ComponentState state() {
        return this.state;
    }

    @Override
    public void close() {
        this.state = new ImmutableComponentState(PRODUCER_ID, ComponentState.Status.DOWN);
    }
}
