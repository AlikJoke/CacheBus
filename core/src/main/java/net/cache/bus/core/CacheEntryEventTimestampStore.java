package net.cache.bus.core;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

public interface CacheEntryEventTimestampStore {

    void save(@Nonnull String cache, @Nonnull Object key, @Nonnegative long timestamp);

    void save(@Nonnull CacheEntryEvent<?, ?> event);

    @Nonnegative
    long load(@Nonnull String cache, @Nonnull Object key);
}
