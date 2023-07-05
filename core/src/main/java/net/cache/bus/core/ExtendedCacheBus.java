package net.cache.bus.core;

import net.cache.bus.core.configuration.CacheBusConfiguration;

import javax.annotation.Nonnull;

/**
 * Шина событий, поддерживающая управление жизненным циклом и конфигурацией.
 * Является расширением стандартного {@link CacheBus}.
 *
 * @author Alik
 * @see CacheBus
 */
public interface ExtendedCacheBus extends CacheBus {

    /**
     * Устанавливает конфигурацию шины кэшей.
     *
     * @param configuration конфигурация шины кэшей, не может быть {@code null}.
     */
    void setConfiguration(@Nonnull CacheBusConfiguration configuration);

    /**
     * Возвращает используемую конфигурацию шины кэшей.
     *
     * @return конфигурацию шины кэшей, не может быть {@code null}.
     * @see CacheBusConfiguration
     */
    CacheBusConfiguration getConfiguration();

    /**
     * Выполняет активацию шины кэширования.
     */
    void start();

    /**
     * Выполняет остановку шины кэширования.
     */
    void stop();
}
