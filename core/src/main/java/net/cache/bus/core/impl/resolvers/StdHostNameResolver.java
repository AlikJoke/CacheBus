package net.cache.bus.core.impl.resolvers;

import net.cache.bus.core.configuration.ConfigurationException;
import net.cache.bus.core.transport.HostNameResolver;

import javax.annotation.Nonnull;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Реализация определителя хоста сервера по-умолчанию на основе {@link InetAddress#getLocalHost()}.
 *
 * @author Alik
 * @see HostNameResolver
 */
public final class StdHostNameResolver implements HostNameResolver {

    @Nonnull
    @Override
    public String resolve() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new ConfigurationException(e);
        }
    }
}
