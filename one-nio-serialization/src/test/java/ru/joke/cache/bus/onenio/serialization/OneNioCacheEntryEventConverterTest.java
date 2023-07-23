package ru.joke.cache.bus.onenio.serialization;

import ru.joke.cache.bus.core.testing.transport.BaseCacheEntryEventConverterTest;
import ru.joke.cache.bus.core.transport.CacheEntryEventConverter;

import javax.annotation.Nonnull;

public class OneNioCacheEntryEventConverterTest extends BaseCacheEntryEventConverterTest {

    @Nonnull
    @Override
    protected CacheEntryEventConverter createConverter() {
        return new OneNioCacheEntryEventConverter();
    }
}
