package ru.joke.cache.bus.jackson.serialization;

import ru.joke.cache.bus.core.testing.transport.BaseCacheEntryEventConverterTest;
import ru.joke.cache.bus.core.transport.CacheEntryEventConverter;

import javax.annotation.Nonnull;

public class JacksonCacheEntryEventConverterTest extends BaseCacheEntryEventConverterTest {

    @Nonnull
    @Override
    protected CacheEntryEventConverter createConverter() {
        return JacksonCacheEntryEventConverter.create();
    }
}