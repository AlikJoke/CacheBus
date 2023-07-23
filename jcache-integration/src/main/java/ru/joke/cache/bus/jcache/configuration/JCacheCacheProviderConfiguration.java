package ru.joke.cache.bus.jcache.configuration;

import ru.joke.cache.bus.core.CacheBus;
import ru.joke.cache.bus.core.configuration.CacheProviderConfiguration;
import ru.joke.cache.bus.core.impl.configuration.CacheProviderConfigurationTemplate;
import ru.joke.cache.bus.jcache.adapters.JCacheCacheManagerAdapter;
import ru.joke.cache.bus.jcache.listeners.JCacheCacheEventListenerRegistrar;

import javax.annotation.Nonnull;

/**
 * Implementation of a caching provider configuration compatible with the JSR-107 (JCache) specification,
 * simplifying event bus setup.
 *
 * @author Alik
 * @see CacheProviderConfiguration
 * @see CacheBus
 */
public final class JCacheCacheProviderConfiguration extends CacheProviderConfigurationTemplate {

    public JCacheCacheProviderConfiguration(@Nonnull javax.cache.CacheManager cacheManager) {
        super(
                new JCacheCacheManagerAdapter(cacheManager),
                new JCacheCacheEventListenerRegistrar()
        );
    }

    @Nonnull
    public static CacheProviderConfiguration create(@Nonnull javax.cache.CacheManager cacheManager) {
        return new JCacheCacheProviderConfiguration(cacheManager);
    }
}
