package net.cache.bus.core.configuration;

import net.cache.bus.core.transport.CacheEntryEventMessageSender;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.Set;

/**
 * Конфигурация шины кэшей.
 *
 * @author Alik
 * @see CacheConfiguration
 * @see CacheTransportConfiguration
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
     * Возвращает отправителя сообщений, используемого для шины кэшей.
     * @return отправитель сообщений, не может быть {@code null}.
     */
    @Nonnull
    CacheEntryEventMessageSender messageSender();
}
