package net.cache.bus.core.impl;

import net.cache.bus.core.transport.HostNameResolver;

import javax.annotation.Nonnull;
import java.net.InetAddress;
import java.net.UnknownHostException;

public final class StdHostNameResolver implements HostNameResolver {

    @Nonnull
    public String resolve() {
        try {
            return InetAddress.getLocalHost().getHostName().toLowerCase();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
