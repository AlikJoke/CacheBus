package net.cache.bus.core.configuration;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.Set;

/**
 * Конфигурация шины кэшей.
 *
 * @author Alik
 * @see CacheConfiguration
 * @see CacheBusTransportConfiguration
 * @see CacheProviderConfiguration
 */
public interface CacheBusConfiguration {

    /**
     * Возвращает список конфигураций кэшей.
     *
     * @return не может быть {@code null}.
     */
    @Nonnull
    Set<CacheConfiguration> cacheConfigurations();

    /**
     * Возвращает конфигурацию кэша по имени кэша.
     *
     * @param cacheName имя кэша, не может быть {@code null}.
     * @return конфигурация кэша с заданным именем, обернутая в {@link Optional}.
     */
    @Nonnull
    Optional<CacheConfiguration> getCacheConfigurationByName(@Nonnull String cacheName);

    /**
     * Возвращает конфигурацию транспорта для данной шины кэшей.
     *
     * @return конфигурацию транспорта, не может быть {@code null}.
     * @see CacheBusTransportConfiguration
     */
    @Nonnull
    CacheBusTransportConfiguration transportConfiguration();

    /**
     * Возвращает конфигурацию, специфичную провайдеру кэширования.
     *
     * @return не может быть {@code null}.
     * @see CacheProviderConfiguration
     */
    @Nonnull
    CacheProviderConfiguration providerConfiguration();
}
