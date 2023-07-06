package net.cache.bus.core.impl;

import net.cache.bus.core.*;
import net.cache.bus.core.configuration.*;
import net.cache.bus.core.transport.CacheBusMessageChannel;
import net.cache.bus.core.transport.CacheEntryEventDeserializer;
import net.cache.bus.core.transport.CacheEntryEventSerializer;
import net.cache.bus.core.transport.CacheEntryOutputMessage;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@ThreadSafe
public final class DefaultCacheBus implements ExtendedCacheBus {

    private static final Logger logger = Logger.getLogger(DefaultCacheBus.class.getCanonicalName());
    private static final ThreadLocal<Boolean> locked = new ThreadLocal<>();

    private final CacheBusConfiguration configuration;
    private volatile boolean started;

    public DefaultCacheBus(@Nonnull CacheBusConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    @Override
    public <K extends Serializable, V extends Serializable> void send(@Nonnull CacheEntryEvent<K, V> event) {
        // To prevent calls from the receiving execution thread
        if (!this.started || locked.get() != null && locked.get()) {
            return;
        }

        final Optional<CacheConfiguration> cacheConfiguration = this.configuration.getCacheConfigurationByName(event.cacheName());
        cacheConfiguration
                .filter(cacheConfig -> needToSendEvent(cacheConfig, event.eventType()))
                .ifPresent(cacheConfig -> sendToEndpoint(cacheConfig, event));
    }

    @Override
    public void receive(@Nonnull byte[] binaryEventData) {

        if (!this.started) {
            return;
        }

        final CacheEntryEvent<Serializable, Serializable> event = convertFromSerializedEvent(binaryEventData);
        if (event == null) {
            return;
        }

        this.configuration.getCacheConfigurationByName(event.cacheName())
                            .ifPresent(cacheConfiguration -> applyEvent(event, cacheConfiguration));
    }

    @Override
    public void setConfiguration(@Nonnull CacheBusConfiguration configuration) {
        throw new UnsupportedOperationException("Configuration can be set only via constructor in this implementation of CacheBus");
    }

    @Override
    @Nonnull
    public CacheBusConfiguration getConfiguration() {
        if (!this.started) {
            throw new IllegalStateException("CacheBus must be in started state");
        }

        return this.configuration;
    }

    @Override
    public synchronized void start() {
        initializeCacheEventListeners();
        this.started = true;
        initializeInputMessageChannelSubscriber();
    }

    @Override
    public synchronized void stop() {
        final CacheBusTransportConfiguration transportConfiguration = this.configuration.transportConfiguration();
        final CacheBusMessageChannel<CacheBusMessageChannelConfiguration> messageChannel = transportConfiguration.messageChannel();

        messageChannel.unsubscribe();

        this.started = false;
    }

    private void sendToEndpoint(final CacheConfiguration cacheConfiguration, final CacheEntryEvent<?, ?> event) {

        final CacheBusTransportConfiguration transportConfiguration = this.configuration.transportConfiguration();

        logger.fine(() -> "Event %s will be sent to endpoint".formatted(event));

        final CacheEntryEventSerializer serializer = transportConfiguration.serializer();
        final byte[] binaryEventData = serializer.serialize(event, cacheConfiguration.cacheType().serializeValueFields());

        final CacheEntryOutputMessage outputMessage = new ImmutableCacheEntryOutputMessage(event, binaryEventData);
        final CacheBusMessageChannel<CacheBusMessageChannelConfiguration> messageChannel = transportConfiguration.messageChannel();

        messageChannel.send(outputMessage);
    }

    private boolean needToSendEvent(final CacheConfiguration cacheConfiguration, final CacheEntryEventType eventType) {
        return eventType != CacheEntryEventType.EVICTED && eventType != CacheEntryEventType.ADDED || cacheConfiguration.cacheType() != CacheType.INVALIDATED;
    }

    private void applyEvent(final CacheEntryEvent<Serializable, Serializable> event, final CacheConfiguration cacheConfiguration) {

        final Optional<Cache<Serializable, Serializable>> cache = this.configuration.cacheManager().getCache(event.cacheName());
        cache.ifPresent(c -> processEvent(cacheConfiguration.cacheType(), c, event));
    }

    private void processEvent(
            final CacheType cacheType,
            final Cache<Serializable, Serializable> cache,
            final CacheEntryEvent<Serializable, Serializable> event) {

        logger.fine(() -> "Process event %s with cacheType %s".formatted(event.toString(), cacheType.name()));

        locked.set(Boolean.TRUE);

        try {
            switch (cacheType) {
                case INVALIDATED -> event.applyToInvalidatedCache(cache);
                case REPLICATED -> event.applyToReplicatedCache(cache);
            }
        } catch (RuntimeException ex) {
            // If we fail then remove value from cache by key and ack receiving of message
            event.applyToInvalidatedCache(cache);
        } finally {
            locked.remove();
        }
    }

    private CacheEntryEvent<Serializable, Serializable> convertFromSerializedEvent(final byte[] binaryEventData) {

        final CacheBusTransportConfiguration transportConfiguration = this.configuration.transportConfiguration();
        final CacheEntryEventDeserializer deserializer = transportConfiguration.deserializer();
        try {
            return deserializer.deserialize(binaryEventData);
        } catch (RuntimeException ex) {
            logger.log(Level.WARNING, "Unable to deserialize message", ex);
            return null;
        }
    }

    private void initializeCacheEventListeners() {

        /*
         * Регистрируем подписчиков на кэшах
         */
        final CacheManager cacheManager = this.configuration.cacheManager();
        final CacheEventListenerRegistrar cacheEventListenerRegistrar = this.configuration.cacheEventListenerRegistrar();
        this.configuration.cacheConfigurations()
                .stream()
                .map(CacheConfiguration::cacheName)
                .map(cacheManager::getCache)
                .flatMap(Optional::stream)
                .forEach(cache -> cacheEventListenerRegistrar.registerFor(this, cache));
    }

    private void initializeInputMessageChannelSubscriber() {

        /*
         * Активируем канал сообщений и подписываемся на входящий поток сообщений об изменениях элементов кэшей
         */
        final CacheBusTransportConfiguration transportConfiguration = this.configuration.transportConfiguration();
        final CacheBusMessageChannel<CacheBusMessageChannelConfiguration> channel = transportConfiguration.messageChannel();

        channel.activate(transportConfiguration.messageChannelConfiguration());
        channel.subscribe(this::receive);
    }
}
