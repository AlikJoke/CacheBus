package net.cache.bus.core.impl.configuration;

import net.cache.bus.core.configuration.CacheConfiguration;
import net.cache.bus.core.configuration.CacheSetConfiguration;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * Неизменяемая реализация конфигурации кэшей, подключенных к шине.
 *
 * @param cacheConfigurations набор конфигураций конкретных кэшей, не может быть {@code null}.
 * @param useAsyncCleaning    признак использования асинхронной очистки хранимых временных меток
 *                            изменения элементов кэша (применяется только в случае наличия хотя
 *                            бы одного кэша с используемым сравнением изменений элементов кэша
 *                            по временным меткам).
 * @author Alik
 * @see CacheSetConfiguration
 */
public record ImmutableCacheSetConfiguration(
        @Nonnull Set<CacheConfiguration> cacheConfigurations,
        boolean useAsyncCleaning) implements CacheSetConfiguration {
}
