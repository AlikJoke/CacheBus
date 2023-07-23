package net.cache.bus.core.configuration;

import net.cache.bus.core.transport.HostNameResolver;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

/**
 * Cache bus message channel configuration.
 *
 * @author Alik
 * @see net.cache.bus.core.transport.CacheBusMessageChannel
 */
public interface CacheBusMessageChannelConfiguration {

    /**
     * Returns the message channel identifier.
     *
     * @return cannot be {@code null}.
     */
    @Nonnull
    String channel();

    /**
     * Returns the host name filter determinant for filtering messages from the local server.
     *
     * @return cannot be {@code null}.
     * @see HostNameResolver
     */
    @Nonnull
    HostNameResolver hostNameResolver();

    /**
     * Returns the reconnection timeout in milliseconds when connections used for sending messages
     * to the channel are disconnected; a timeout will result in the cessation of reconnection
     * attempts and an exception will be thrown; by default, the value of {@code 5} minutes is used.
     *
     * @return cannot be negative.
     */
    @Nonnegative
    long reconnectTimeoutMs();
}
