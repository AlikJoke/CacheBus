package net.cache.bus.core.configuration;

import javax.annotation.Nonnull;

/**
 * Базовое исключение, сообщающее о том, что произошла исключительная ситуация, связанная с конфигурацией.
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
