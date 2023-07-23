package ru.joke.cache.bus.core.configuration;

import javax.annotation.Nonnull;

/**
 * Exception indicating that the cache configuration is invalid.
 *
 * @author Alik
 */
public final class InvalidCacheConfigurationException extends ConfigurationException {

    public InvalidCacheConfigurationException(@Nonnull String message) {
        super(message);
    }

    public InvalidCacheConfigurationException(@Nonnull Exception source) {
        super(source);
    }
}
