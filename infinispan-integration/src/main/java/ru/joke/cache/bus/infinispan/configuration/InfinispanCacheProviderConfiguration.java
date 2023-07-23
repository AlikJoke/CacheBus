package ru.joke.cache.bus.infinispan.configuration;

import ru.joke.cache.bus.core.CacheBus;
import ru.joke.cache.bus.core.configuration.CacheProviderConfiguration;
import ru.joke.cache.bus.core.impl.configuration.CacheProviderConfigurationTemplate;
import ru.joke.cache.bus.infinispan.adapters.InfinispanCacheManagerAdapter;
import ru.joke.cache.bus.infinispan.listeners.InfinispanCacheEventListenerRegistrar;
import org.infinispan.manager.EmbeddedCacheManager;

import javax.annotation.Nonnull;

/**
 * Implementation of the Infinispan caching provider configuration that simplifies event bus setup.
 *
 * @author Alik
 * @see CacheProviderConfiguration
 * @see CacheBus
 */
public final class InfinispanCacheProviderConfiguration extends CacheProviderConfigurationTemplate {

    public InfinispanCacheProviderConfiguration(@Nonnull EmbeddedCacheManager cacheManager) {
        super(
                new InfinispanCacheManagerAdapter(cacheManager),
                new InfinispanCacheEventListenerRegistrar()
        );
    }

    @Nonnull
    public static CacheProviderConfiguration create(@Nonnull EmbeddedCacheManager cacheManager) {
        return new InfinispanCacheProviderConfiguration(cacheManager);
    }
}
