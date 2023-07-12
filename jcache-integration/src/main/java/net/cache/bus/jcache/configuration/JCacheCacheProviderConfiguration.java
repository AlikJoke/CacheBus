package net.cache.bus.jcache.configuration;

import net.cache.bus.core.configuration.CacheProviderConfiguration;
import net.cache.bus.core.impl.configuration.CacheProviderConfigurationTemplate;
import net.cache.bus.jcache.adapters.JCacheCacheManagerAdapter;
import net.cache.bus.jcache.listeners.JCacheCacheEventListenerRegistrar;

import javax.annotation.Nonnull;

/**
 * Реализация конфигурации провайдера кэширования, совместимого со спецификацией JSR-107 (JCache),
 * упрощающая настройку шины событий.
 *
 * @author Alik
 * @see CacheProviderConfiguration
 * @see net.cache.bus.core.CacheBus
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
