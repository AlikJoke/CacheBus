package net.cache.bus.infinispan.configuration;

import net.cache.bus.core.configuration.CacheProviderConfiguration;
import net.cache.bus.core.impl.configuration.CacheProviderConfigurationTemplate;
import net.cache.bus.infinispan.adapters.InfinispanCacheManagerAdapter;
import net.cache.bus.infinispan.listeners.InfinispanCacheEventListenerRegistrar;
import org.infinispan.manager.EmbeddedCacheManager;

import javax.annotation.Nonnull;

/**
 * Implementation of the Infinispan caching provider configuration that simplifies event bus setup.
 *
 * @author Alik
 * @see CacheProviderConfiguration
 * @see net.cache.bus.core.CacheBus
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
