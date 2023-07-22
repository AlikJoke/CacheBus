package net.cache.bus.core.impl.configuration;

import net.cache.bus.core.configuration.CacheConfiguration;

import javax.annotation.Nonnegative;

/**
 * Неизменяемая реализация конфигурации работы с временными метками элементов кэшей.
 *
 * @param probableAverageElementsCount вероятное количество элементов в кэше (в среднем).
 * @param timestampExpiration          временной интервал, через который метка может быть удалена
 *                                     из списка хранимых меток в случае, если за этот интервал
 *                                     метка не обновлялась.
 * @author Alik
 * @see net.cache.bus.core.configuration.CacheConfiguration.TimestampCacheConfiguration
 */
public record ImmutableTimestampCacheConfiguration(
        @Nonnegative int probableAverageElementsCount,
        @Nonnegative long timestampExpiration) implements CacheConfiguration.TimestampCacheConfiguration {

}
