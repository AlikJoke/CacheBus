package ru.joke.cache.bus.core.impl;

import ru.joke.cache.bus.core.impl.resolvers.StdHostNameResolver;
import ru.joke.cache.bus.core.transport.HostNameResolver;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StdHostNameResolverTest {

    @Test
    public void testDynamicHostNameResolving() throws UnknownHostException {
        final HostNameResolver hostNameResolver = new StdHostNameResolver();
        final String localHostName = InetAddress.getLocalHost().getHostName();

        assertEquals(localHostName, hostNameResolver.resolve(), "Host name must be equal");
    }
}
