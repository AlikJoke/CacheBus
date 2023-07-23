package net.cache.bus.core.impl.internal;

import net.cache.bus.core.CacheBus;
import net.cache.bus.core.CacheEventMessageConsumer;
import net.cache.bus.core.impl.ImmutableComponentState;
import net.cache.bus.core.state.ComponentState;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Objects;

/**
 * Synchronous implementation of a message consumer from a channel that immediately
 * processes the received message in the same thread.
 *
 * @author Alik
 * @see CacheBus#receive(byte[])
 */
@ThreadSafe
@Immutable
public final class SynchronousCacheEventMessageConsumer implements CacheEventMessageConsumer {

    private static final String CONSUMER_ID = "sync-message-consumer";

    private final CacheBus cacheBus;
    private volatile ComponentState state;

    public SynchronousCacheEventMessageConsumer(@Nonnull final CacheBus cacheBus) {
        this.cacheBus = Objects.requireNonNull(cacheBus, "cacheBus");
        this.state = new ImmutableComponentState(CONSUMER_ID, ComponentState.Status.UP_OK);
    }

    @Override
    public void accept(int messageHash, @Nonnull byte[] messageBody) {
        this.cacheBus.receive(messageBody);
    }

    @Nonnull
    @Override
    public ComponentState state() {
        return this.state;
    }

    @Override
    public void close() {
        this.state = new ImmutableComponentState(CONSUMER_ID, ComponentState.Status.DOWN);
    }
}
