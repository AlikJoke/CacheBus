package net.cache.bus.core.impl;

import net.cache.bus.core.transport.HostNameResolver;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Реализация определителя хоста сервера на основе статического имени в виде строки.
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
}
