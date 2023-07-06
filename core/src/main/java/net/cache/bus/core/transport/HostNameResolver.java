package net.cache.bus.core.transport;

import javax.annotation.Nonnull;

/**
 * Определитель имени текущего хоста.
 *
 * @author Alik
 */
public interface HostNameResolver {

    /**
     * Определяет имя хоста.
     *
     * @return не может быть {@code null}.
     */
    @Nonnull
    String resolve();
}
