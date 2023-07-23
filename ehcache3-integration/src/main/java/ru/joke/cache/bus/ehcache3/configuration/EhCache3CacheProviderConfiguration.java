package ru.joke.cache.bus.ehcache3.configuration;

import ru.joke.cache.bus.core.CacheBus;
import ru.joke.cache.bus.core.configuration.CacheProviderConfiguration;
import ru.joke.cache.bus.core.impl.configuration.CacheProviderConfigurationTemplate;
import ru.joke.cache.bus.ehcache3.adapters.EhCache3CacheManagerAdapter;
import ru.joke.cache.bus.ehcache3.listeners.EhCache3CacheEventListenerRegistrar;
import org.ehcache.core.EhcacheManager;

import javax.annotation.Nonnull;

/**
 * Implementation of the EhCache (v3) caching provider configuration that simplifies event bus setup.
 *
 * @author Alik
 * @see CacheProviderConfiguration
 * @see CacheBus
 */
public final class EhCache3CacheProviderConfiguration extends CacheProviderConfigurationTemplate {

    public EhCache3CacheProviderConfiguration(@Nonnull EhcacheManager cacheManager) {
        super(
                new EhCache3CacheManagerAdapter(cacheManager),
                new EhCache3CacheEventListenerRegistrar()
        );
    }

    @Nonnull
    public static CacheProviderConfiguration create(@Nonnull EhcacheManager cacheManager) {
        return new EhCache3CacheProviderConfiguration(cacheManager);
    }
}
