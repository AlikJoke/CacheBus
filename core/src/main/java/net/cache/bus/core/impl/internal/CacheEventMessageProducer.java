package net.cache.bus.core.impl.internal;

import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.configuration.CacheBusMessageChannelConfiguration;
import net.cache.bus.core.configuration.CacheBusTransportConfiguration;
import net.cache.bus.core.configuration.CacheConfiguration;
import net.cache.bus.core.state.ComponentState;
import net.cache.bus.core.transport.CacheBusMessageChannel;
import net.cache.bus.core.transport.CacheEntryEventConverter;
import net.cache.bus.core.transport.CacheEntryOutputMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Objects;

/**
 * Базовый отправитель событий в канал.
 *
 * @author Alik
 * @see CacheBusMessageChannel
 */
@ThreadSafe
@Immutable
public abstract class CacheEventMessageProducer implements AutoCloseable {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final CacheBusTransportConfiguration transportConfiguration;

    protected CacheEventMessageProducer(@Nonnull CacheBusTransportConfiguration transportConfiguration) {
        this.transportConfiguration = Objects.requireNonNull(transportConfiguration, "transportConfiguration");
    }

    public void produce(
            @Nonnull final CacheConfiguration cacheConfiguration,
            @Nonnull final CacheEntryEvent<?, ?> event) {

        final CacheEntryEventConverter converter = this.transportConfiguration.converter();
        final byte[] binaryEventData = converter.toBinary(event, cacheConfiguration.cacheType().serializeValueFields());

        final CacheEntryOutputMessage outputMessage = new ImmutableCacheEntryOutputMessage(event, binaryEventData);
        final CacheBusMessageChannel<CacheBusMessageChannelConfiguration> messageChannel = this.transportConfiguration.messageChannel();

        messageChannel.send(outputMessage);
    }

    /**
     * Возвращает информацию о состоянии производителя сообщений в канал.
     *
     * @return не может быть {@code null}.
     * @see ComponentState
     */
    @Nonnull
    public abstract ComponentState state();

    @Override
    public abstract void close();
}
