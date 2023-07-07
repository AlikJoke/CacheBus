package ehcache2.configuration;

import ehcache2.adapters.EhCache2CacheManagerAdapter;
import ehcache2.listeners.EhCache2CacheEventListenerRegistrar;
import net.cache.bus.core.configuration.CacheProviderConfiguration;
import net.cache.bus.core.impl.CacheProviderConfigurationTemplate;
import net.sf.ehcache.CacheManager;

import javax.annotation.Nonnull;

/**
 * Реализация конфигурации провайдера кэширования EhCache (v2), упрощающая настройку шины событий.
 *
 * @author Alik
 * @see CacheProviderConfiguration
 * @see net.cache.bus.core.CacheBus
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
