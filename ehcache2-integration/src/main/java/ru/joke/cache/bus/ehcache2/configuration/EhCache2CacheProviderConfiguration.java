package ru.joke.cache.bus.ehcache2.configuration;

import ru.joke.cache.bus.ehcache2.adapters.EhCache2CacheManagerAdapter;
import ru.joke.cache.bus.ehcache2.listeners.EhCache2CacheEventListenerRegistrar;
import ru.joke.cache.bus.core.CacheBus;
import ru.joke.cache.bus.core.configuration.CacheProviderConfiguration;
import ru.joke.cache.bus.core.impl.configuration.CacheProviderConfigurationTemplate;
import net.sf.ehcache.CacheManager;

import javax.annotation.Nonnull;

/**
 * Implementation of the EhCache (v2) caching provider configuration that simplifies event bus setup.
 *
 * @author Alik
 * @see CacheProviderConfiguration
 * @see CacheBus
 */
public final class EhCache2CacheProviderConfiguration extends CacheProviderConfigurationTemplate {

    public EhCache2CacheProviderConfiguration(@Nonnull CacheManager cacheManager) {
        super(
                new EhCache2CacheManagerAdapter(cacheManager),
                new EhCache2CacheEventListenerRegistrar()
        );
    }

    @Nonnull
    public static CacheProviderConfiguration create(@Nonnull CacheManager cacheManager) {
        return new EhCache2CacheProviderConfiguration(cacheManager);
    }
}
