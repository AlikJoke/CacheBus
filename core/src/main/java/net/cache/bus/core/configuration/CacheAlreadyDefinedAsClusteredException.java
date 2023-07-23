package net.cache.bus.core.configuration;

/**
 * An exception indicating that the cache connected to the cache bus is declared as distributed
 * in the caching provider configuration.
 *
 * @author Alik
 */
public final class CacheAlreadyDefinedAsClusteredException extends ConfigurationException {

    public CacheAlreadyDefinedAsClusteredException(final String cacheName) {
        super("Cache must be local in provider configuration: " + cacheName);
    }
}
