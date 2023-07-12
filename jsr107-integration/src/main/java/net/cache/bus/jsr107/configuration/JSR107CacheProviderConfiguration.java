package net.cache.bus.jsr107.configuration;

import net.cache.bus.core.configuration.CacheProviderConfiguration;
import net.cache.bus.core.impl.configuration.CacheProviderConfigurationTemplate;
import net.cache.bus.jsr107.adapters.JSR107CacheManagerAdapter;
import net.cache.bus.jsr107.listeners.JSR107CacheEventListenerRegistrar;

import javax.annotation.Nonnull;

/**
 * Реализация конфигурации провайдера кэширования, совместимого со спецификацией JSR-107, упрощающая настройку шины событий.
 *
 * @author Alik
 * @see CacheProviderConfiguration
 * @see net.cache.bus.core.CacheBus
 */
public final class JSR107CacheProviderConfiguration extends CacheProviderConfigurationTemplate {

    public JSR107CacheProviderConfiguration(@Nonnull javax.cache.CacheManager cacheManager) {
        super(
                new JSR107CacheManagerAdapter(cacheManager),
                new JSR107CacheEventListenerRegistrar()
        );
    }

    @Nonnull
    public static CacheProviderConfiguration create(@Nonnull javax.cache.CacheManager cacheManager) {
        return new JSR107CacheProviderConfiguration(cacheManager);
    }
}
