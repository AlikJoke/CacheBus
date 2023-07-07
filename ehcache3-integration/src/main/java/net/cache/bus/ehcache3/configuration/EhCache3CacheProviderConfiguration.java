package net.cache.bus.ehcache3.configuration;

import net.cache.bus.core.configuration.CacheProviderConfiguration;
import net.cache.bus.core.impl.CacheProviderConfigurationTemplate;
import net.cache.bus.ehcache3.adapters.EhCache3CacheManagerAdapter;
import net.cache.bus.ehcache3.listeners.EhCache3CacheEventListenerRegistrar;
import org.ehcache.core.EhcacheManager;

import javax.annotation.Nonnull;

/**
 * Реализация конфигурации провайдера кэширования EhCache (v3), упрощающая настройку шины событий.
 *
 * @author Alik
 * @see CacheProviderConfiguration
 * @see net.cache.bus.core.CacheBus
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
