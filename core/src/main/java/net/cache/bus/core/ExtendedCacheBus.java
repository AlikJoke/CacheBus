package net.cache.bus.core;

/**
 * Шина событий, поддерживающая управление жизненным циклом.
 * Является расширением стандартного {@link CacheBus}.
 *
 * @author Alik
 * @see CacheBus
 */
public interface ExtendedCacheBus extends CacheBus {

    /**
     * Выполняет активацию шины кэширования.
     */
    void start();

    /**
     * Выполняет остановку шины кэширования.
     */
    void stop();
}
