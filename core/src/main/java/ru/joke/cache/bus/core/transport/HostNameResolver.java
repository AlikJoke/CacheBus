package ru.joke.cache.bus.core.transport;

import javax.annotation.Nonnull;

/**
 * Determines the name of the current host.
 *
 * @author Alik
 */
public interface HostNameResolver {

    /**
     * Determines the host name.
     *
     * @return cannot be {@code null}.
     */
    @Nonnull
    String resolve();
}
