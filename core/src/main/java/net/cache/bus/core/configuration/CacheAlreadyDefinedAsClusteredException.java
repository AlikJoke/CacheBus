package net.cache.bus.core.configuration;

/**
 * Исключение, сообщающее о том, что кэш, подключенный к шине кэшей, объявлен
 * как распределенный в конфигурации провайдера кэширования.
 *
 * @author Alik
 */
public final class CacheAlreadyDefinedAsClusteredException extends ConfigurationException {

    public CacheAlreadyDefinedAsClusteredException(final String cacheName) {
        super("Cache must be local in provider configuration: " + cacheName);
    }
}
