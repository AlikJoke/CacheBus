package net.cache.bus.core.impl.internal;

import net.cache.bus.core.configuration.CacheBusTransportConfiguration;
import net.cache.bus.core.impl.ImmutableComponentState;
import net.cache.bus.core.metrics.CacheBusMetricsRegistry;
import net.cache.bus.core.state.ComponentState;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Реализация синхронного отправителя событий в канал без каких-либо промежуточных этапов.
 *
 * @author Alik
 * @see net.cache.bus.core.transport.CacheBusMessageChannel
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
