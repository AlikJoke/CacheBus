package net.cache.bus.core.impl;

import net.cache.bus.core.configuration.CacheConfiguration;
import net.cache.bus.core.configuration.CacheConfigurationSource;
import net.cache.bus.core.configuration.CacheType;
import net.cache.bus.core.impl.configuration.ImmutableCacheConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimpleCacheConfigurationSourceTest {

    @Test
    public void testOperations() {
        final var source = CacheConfigurationSource.createDefault();

        final CacheConfiguration cache1 = new ImmutableCacheConfiguration("1", CacheType.INVALIDATED);
        source.add(cache1);
        final CacheConfiguration cache2 = new ImmutableCacheConfiguration("2", CacheType.REPLICATED);
        source.add(cache2);

        final Set<CacheConfiguration> configurations = source.pull();
        assertEquals(2, configurations.size(), "Configurations size after add must be equal");
        assertTrue(configurations.contains(cache1), "Configurations must contain added cache");
        assertTrue(configurations.contains(cache2), "Configurations must contain added cache");

        source.clear();
        assertTrue(source.pull().isEmpty(), "Configurations must be empty after clear");
    }
}
