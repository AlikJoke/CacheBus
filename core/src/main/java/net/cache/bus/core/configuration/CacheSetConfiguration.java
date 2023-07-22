package net.cache.bus.core.configuration;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * Конфигурация кэшей данной шины кэшей.
 *
 * @author Alik
 * @see CacheConfiguration
 * @see CacheConfigurationSource
 */
public interface CacheSetConfiguration {

    /**
     * Возвращает конфигурации кэшей, кластеризацией которых должна заниматься шина кэшей.
     *
     * @return не может быть {@code null}.
     * @see CacheConfiguration
     */
    @Nonnull
    Set<CacheConfiguration> cacheConfigurations();

    /**
     * Задает необходимость использовать асинхронный пул для очистки старых временных меток
     * (см. {@linkplain CacheConfiguration.TimestampCacheConfiguration#timestampExpiration()}.<br>
     * В случае, если метод возвращает {@code false}, то очистка будет проводиться в момент, когда
     * очередная метка будет сохраняться в хранилище меток (не каждый раз, очистка может запускаться
     * на основании некоторого признака или эвристики, например, если количество хранимых меток
     * превысило определенное количество).<br>
     * В случае использования асинхронной очистки будет использоваться общий пул,
     * на основе которого работает {@linkplain java.util.concurrent.CompletableFuture}.<br>
     * Используется только в случае, если есть хотя бы один кэш, использующий временные метки,
     * т.е. если {@code cacheConfigurations().stream().anyMatch(CacheConfiguration::useTimestampBasedComparison) == true}.
     *
     * @return {@code true}, если для очистки надо использовать асинхронный пул;
     * {@code false}, если очистка должна производиться в потоках модификации хранилища меток.
     */
    boolean useAsyncCleaning();
}
