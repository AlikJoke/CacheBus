package ru.joke.cache.bus.core.impl.configuration;

import ru.joke.cache.bus.core.CacheEventListenerRegistrar;
import ru.joke.cache.bus.core.CacheManager;
import ru.joke.cache.bus.core.configuration.CacheProviderConfiguration;

import javax.annotation.Nonnull;

/**
 * Skeletal implementation of the caching provider configuration.
 *
 * @author Alik
 * @see CacheProviderConfiguration
 */
public abstract class CacheProviderConfigurationTemplate implements CacheProviderConfiguration {

    private final CacheManager cacheManager;
    private final CacheEventListenerRegistrar cacheEventListenerRegistrar;

    protected CacheProviderConfigurationTemplate(
            @Nonnull final CacheManager cacheManager,
            @Nonnull final CacheEventListenerRegistrar cacheEventListenerRegistrar) {
        this.cacheManager = cacheManager;
        this.cacheEventListenerRegistrar = cacheEventListenerRegistrar;
    }

    @Nonnull
    @Override
    public CacheManager cacheManager() {
        return this.cacheManager;
    }

    @Nonnull
    @Override
    public CacheEventListenerRegistrar cacheEventListenerRegistrar() {
        return this.cacheEventListenerRegistrar;
    }

    @Override
    public String toString() {
        return "CacheProviderConfigurationTemplate{" +
                "cacheManager=" + cacheManager +
                ", cacheEventListenerRegistrar=" + cacheEventListenerRegistrar +
                '}';
    }
}
