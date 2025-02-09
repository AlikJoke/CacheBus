package ru.joke.cache.bus.core.impl;

import ru.joke.cache.bus.core.impl.resolvers.StaticHostNameResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StaticHostNameResolverTest {

    @Test
    public void testStaticHostNameResolving() {
        final String predefinedHostName = "local1";
        final StaticHostNameResolver resolver = new StaticHostNameResolver(predefinedHostName);

        assertEquals(predefinedHostName, resolver.resolve(), "Host name must be equal");
    }
}
