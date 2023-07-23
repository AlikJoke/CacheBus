package ru.joke.cache.bus.jdk.serialization;

import ru.joke.cache.bus.core.testing.transport.BaseCacheEntryEventConverterTest;
import ru.joke.cache.bus.core.transport.CacheEntryEventConverter;

import javax.annotation.Nonnull;

public class JdkCacheEntryEventConverterTest extends BaseCacheEntryEventConverterTest {

    @Nonnull
    @Override
    protected CacheEntryEventConverter createConverter() {
        return new JdkCacheEntryEventConverter();
    }
}
