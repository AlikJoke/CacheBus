package net.cache.bus.core.impl;

import net.cache.bus.core.*;
import net.cache.bus.core.configuration.CacheBusConfiguration;
import net.cache.bus.core.configuration.CacheBusTransportConfiguration;
import net.cache.bus.core.configuration.CacheConfiguration;
import net.cache.bus.core.configuration.CacheType;
import net.cache.bus.core.transport.CacheEntryEventDeserializer;
import net.cache.bus.core.transport.CacheEntryEventMessageSender;
import net.cache.bus.core.transport.CacheEntryEventSerializer;
import net.cache.bus.core.transport.CacheEntryOutputMessage;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

@ThreadSafe
public final class DefaultCacheBus implements ExtendedCacheBus {

    private static final Logger logger = Logger.getLogger(DefaultCacheBus.class.getCanonicalName());
    private static final ThreadLocal<Boolean> locked = new ThreadLocal<>();

    private final String hostName;

    private CacheBusConfiguration configuration;
    private volatile boolean started;

    public DefaultCacheBus(@Nonnull CacheBusConfiguration configuration) {
        setConfiguration(configuration);
        this.hostName = resolveHostName();
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
        this.configuration.getCacheConfigurationByName(event.cacheName())
                            .ifPresent(cacheConfiguration -> applyEvent(event, cacheConfiguration));
    }

    @Override
    public void setConfiguration(@Nonnull CacheBusConfiguration configuration) {
        if (this.started) {
            throw new IllegalStateException("Changing of configuration is forbidden in active state");
        }

        this.configuration = Objects.requireNonNull(configuration, "configuration");
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

        if (this.configuration == null) {
            throw new IllegalStateException("Activation of bus is forbidden while configuration is not set");
        }

        initialize();
        this.started = true;
    }

    @Override
    public synchronized void stop() {
        this.started = false;
    }

    private void sendToEndpoint(final CacheConfiguration cacheConfiguration, final CacheEntryEvent<?, ?> event) {

        final CacheBusTransportConfiguration transportConfiguration = this.configuration.transportConfiguration();

        logger.fine(() -> "Event %s will be sent to %s".formatted(event, transportConfiguration.targetEndpoint()));

        final CacheEntryEventSerializer serializer = transportConfiguration.serializer();
        final byte[] binaryEventData = serializer.serialize(event, cacheConfiguration.cacheType().serializeValueFields());

        final CacheEntryOutputMessage outputMessage = new ImmutableCacheEntryOutputMessage(event, binaryEventData, this.hostName);
        final CacheEntryEventMessageSender sender = transportConfiguration.messageSender();

        sender.send(outputMessage, transportConfiguration.targetEndpoint());
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
        } finally {
            locked.remove();
        }
    }

    private CacheEntryEvent<Serializable, Serializable> convertFromSerializedEvent(final byte[] binaryEventData) {

        final CacheBusTransportConfiguration transportConfiguration = this.configuration.transportConfiguration();
        final CacheEntryEventDeserializer deserializer = transportConfiguration.deserializer();
        return deserializer.deserialize(binaryEventData);
    }

    private String resolveHostName() {
        try {
            return InetAddress.getLocalHost().getHostName().toLowerCase();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private void initialize() {

        final CacheManager cacheManager = this.configuration.cacheManager();
        final CacheEventListenerRegistrar cacheEventListenerRegistrar = this.configuration.cacheEventListenerRegistrar();
        this.configuration.cacheConfigurations()
                .stream()
                .map(CacheConfiguration::cacheName)
                .map(cacheManager::getCache)
                .flatMap(Optional::stream)
                .forEach(cacheEventListenerRegistrar::registerFor);
    }
}
