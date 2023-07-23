package ru.joke.cache.bus.core.configuration;

import javax.annotation.Nonnull;

/**
 * Base exception indicating that an exceptional situation related to configuration has occurred.
 *
 * @author Alik
 */
public class ConfigurationException extends IllegalStateException {

    public ConfigurationException(@Nonnull String message) {
        super(message);
    }

    public ConfigurationException(@Nonnull Exception source) {
        super("Configuration isn't valid", source);
    }
}
