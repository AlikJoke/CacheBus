package net.cache.bus.core.impl.resolvers;

import net.cache.bus.core.transport.HostNameResolver;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Implementation of a server host resolver based on a static name as a string.
 *
 * @author Alik
 * @see HostNameResolver
 * @see StdHostNameResolver
 */
public final class StaticHostNameResolver implements HostNameResolver {

    private final String hostName;

    public StaticHostNameResolver(@Nonnull String hostName) {
        this.hostName = Objects.requireNonNull(hostName, "hostName");
    }

    @Nonnull
    @Override
    public String resolve() {
        return this.hostName;
    }

    @Override
    public String toString() {
        return "StaticHostNameResolver{" +
                "hostName='" + hostName + '\'' +
                '}';
    }
}
