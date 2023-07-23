package ru.joke.cache.bus.core.impl.configuration;

import ru.joke.cache.bus.core.configuration.CacheConfiguration;

import javax.annotation.Nonnegative;

/**
 * Immutable implementation of cache element timestamp configuration.
 *
 * @param probableAverageElementsCount the probable number of elements in the cache (on average).
 * @param timestampExpiration          the time interval after which a timestamp can be removed
 *                                     from the list of stored timestamps if it hasn't been updated within this interval.
 * @author Alik
 * @see CacheConfiguration.TimestampCacheConfiguration
 */
public record ImmutableTimestampCacheConfiguration(
        @Nonnegative int probableAverageElementsCount,
        @Nonnegative long timestampExpiration) implements CacheConfiguration.TimestampCacheConfiguration {

}
