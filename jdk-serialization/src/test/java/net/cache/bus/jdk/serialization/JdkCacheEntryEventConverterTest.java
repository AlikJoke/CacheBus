package net.cache.bus.jdk.serialization;

import net.cache.bus.core.testing.transport.BaseCacheEntryEventConverterTest;
import net.cache.bus.core.transport.CacheEntryEventConverter;

import javax.annotation.Nonnull;

public class JdkCacheEntryEventConverterTest extends BaseCacheEntryEventConverterTest {

    @Nonnull
    @Override
    protected CacheEntryEventConverter createConverter() {
        return new JdkCacheEntryEventConverter();
    }
}
