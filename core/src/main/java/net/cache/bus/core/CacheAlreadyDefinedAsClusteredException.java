package net.cache.bus.core;

public final class CacheAlreadyDefinedAsClusteredException extends IllegalStateException {

    public CacheAlreadyDefinedAsClusteredException(final String cacheName) {
        super("Cache must be local in provider configuration: " + cacheName);
    }
}
