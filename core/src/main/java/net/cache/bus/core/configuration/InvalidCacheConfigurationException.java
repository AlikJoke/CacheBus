package net.cache.bus.core.configuration;

import javax.annotation.Nonnull;

/**
 * Исключение, сообщающее о том, что конфигурация кэша является некорректной.
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
