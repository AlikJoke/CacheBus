package net.cache.bus.core.impl.internal;

import net.cache.bus.core.configuration.CacheBusTransportConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Реализация синхронного отправителя событий в канал без каких-либо промежуточных этапов.
 *
 * @author Alik
 * @see net.cache.bus.core.transport.CacheBusMessageChannel
 */
@ThreadSafe
@Immutable
public final class SynchronousCacheEventMessageProducer extends CacheEventMessageProducer {

    public SynchronousCacheEventMessageProducer(@Nonnull CacheBusTransportConfiguration transportConfiguration) {
        super(transportConfiguration);
    }
}
