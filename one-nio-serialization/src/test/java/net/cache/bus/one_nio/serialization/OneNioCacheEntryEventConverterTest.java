package net.cache.bus.one_nio.serialization;

import net.cache.bus.core.testing.transport.BaseCacheEntryEventConverterTest;
import net.cache.bus.core.transport.CacheEntryEventConverter;

import javax.annotation.Nonnull;

public class OneNioCacheEntryEventConverterTest extends BaseCacheEntryEventConverterTest {

    @Nonnull
    @Override
    protected CacheEntryEventConverter createConverter() {
        return new OneNioCacheEntryEventConverter();
    }
}
