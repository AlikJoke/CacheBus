package net.cache.bus.core.impl.internal;

import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.configuration.CacheBusMessageChannelConfiguration;
import net.cache.bus.core.configuration.CacheBusTransportConfiguration;
import net.cache.bus.core.configuration.CacheConfiguration;
import net.cache.bus.core.transport.CacheBusMessageChannel;
import net.cache.bus.core.transport.CacheEntryEventConverter;
import net.cache.bus.core.transport.CacheEntryOutputMessage;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Базовый отправитель событий в канал.
 *
 * @author Alik
 * @see CacheBusMessageChannel
 */
@ThreadSafe
@Immutable
public abstract class CacheEventMessageProducer implements AutoCloseable {

    protected final Logger logger = Logger.getLogger(getClass().getCanonicalName());

    protected final CacheBusTransportConfiguration transportConfiguration;

    protected CacheEventMessageProducer(@Nonnull CacheBusTransportConfiguration transportConfiguration) {
        this.transportConfiguration = Objects.requireNonNull(transportConfiguration, "transportConfiguration");
    }

    public void produce(
            @Nonnull final CacheConfiguration cacheConfiguration,
            @Nonnull final CacheEntryEvent<?, ?> event) {

        logger.fine(() -> "Event %s will be sent to endpoint".formatted(event));

        final CacheEntryEventConverter converter = this.transportConfiguration.converter();
        final byte[] binaryEventData = converter.toBinary(event, cacheConfiguration.cacheType().serializeValueFields());

        final CacheEntryOutputMessage outputMessage = new ImmutableCacheEntryOutputMessage(event, binaryEventData);
        final CacheBusMessageChannel<CacheBusMessageChannelConfiguration> messageChannel = this.transportConfiguration.messageChannel();

        messageChannel.send(outputMessage);
    }

    @Override
    public void close() {
    }
}
