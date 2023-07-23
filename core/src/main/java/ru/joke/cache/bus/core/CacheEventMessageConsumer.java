package ru.joke.cache.bus.core;

import ru.joke.cache.bus.core.state.ComponentState;

import javax.annotation.Nonnull;
import java.io.Closeable;

/**
 * The consumer of cache element change messages from other servers,
 * responsible for applying the changes to the local cache.
 *
 * @author Alik
 * @see CacheBus
 */
public interface CacheEventMessageConsumer extends Closeable {

    /**
     * Consumes the message hash key and body in binary format and applies the change to the local cache.
     *
     * @param messageHash the hash key of the message
     * @param messageBody the message body in binary format, cannot be {@code null}.
     */
    void accept(int messageHash, @Nonnull byte[] messageBody);

    /**
     * Returns information about the state of the consumer of incoming messages from other servers.
     *
     * @return cannot be {@code null}.
     * @see ComponentState
     */
    @Nonnull
    ComponentState state();

    /**
     * Closes the message consumer and associated resources if necessary.
     */
    @Override
    default void close() {
    }
}
