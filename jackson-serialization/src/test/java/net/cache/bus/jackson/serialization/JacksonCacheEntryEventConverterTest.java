package net.cache.bus.jackson.serialization;

import net.cache.bus.core.testing.transport.BaseCacheEntryEventConverterTest;
import net.cache.bus.core.transport.CacheEntryEventConverter;

import javax.annotation.Nonnull;

public class JacksonCacheEntryEventConverterTest extends BaseCacheEntryEventConverterTest {

    @Nonnull
    @Override
    protected CacheEntryEventConverter createConverter() {
        return JacksonCacheEntryEventConverter.create();
    }
}