package net.cache.bus.core;

/**
 * Event bus supporting lifecycle management.<br>
 * Extends the standard {@link Cache}.
 *
 * @author Alik
 * @see CacheBus
 */
public interface ExtendedCacheBus extends CacheBus {

    /**
     * Activates the caching bus.
     */
    void start();

    /**
     * Stops the caching bus.
     */
    void stop();
}
