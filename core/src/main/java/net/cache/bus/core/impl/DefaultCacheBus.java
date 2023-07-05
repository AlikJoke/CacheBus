package net.cache.bus.core.impl;

import net.cache.bus.core.*;
import net.cache.bus.core.configuration.CacheBusConfiguration;
import net.cache.bus.core.configuration.CacheConfiguration;
import net.cache.bus.core.configuration.CacheTransportConfiguration;
import net.cache.bus.core.configuration.CacheType;
import net.cache.bus.core.transport.CacheEntryEventMessageSender;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

@ThreadSafe
public final class DefaultCacheBus implements ExtendedCacheBus {

    private static final Logger logger = Logger.getLogger(DefaultCacheBus.class.getCanonicalName());
    private static final ThreadLocal<Boolean> locked = new ThreadLocal<>();

    private CacheBusConfiguration configuration;
    private volatile boolean started;

    public DefaultCacheBus(@Nonnull CacheBusConfiguration configuration) {
        setConfiguration(configuration);
    }

    @Override
    public <K, V> void send(@Nonnull CacheEntryEvent<K, V> event) {
        // To prevent calls from the receiving execution thread
        if (!this.started || locked.get() != null && locked.get()) {
            return;
        }

        final Optional<CacheConfiguration> cacheConfiguration = this.configuration.getCacheConfigurationByName(event.cacheName());
        cacheConfiguration
                .filter(cacheConfig -> needToSendEvent(cacheConfig, event.eventType()))
                .ifPresent(cacheConfig -> sendToEndpoints(cacheConfig, event));
    }

    @Override
    public <T> void receive(
            @Nonnull String endpoint,
            @Nonnull String cacheName,
            @Nonnull T event) {

        if (!this.started) {
            return;
        }

        final Optional<CacheConfiguration> cacheConfiguration = this.configuration.getCacheConfigurationByName(cacheName);
        cacheConfiguration
                .flatMap(cacheConfig -> convertFromSerializedEvent(cacheConfig, endpoint, event))
                .ifPresent(cacheEvent -> applyEvent(cacheEvent, cacheConfiguration.get()));
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

    private void sendToEndpoints(final CacheConfiguration cacheConfig, final CacheEntryEvent<?, ?> event) {

        final CacheEntryEventMessageSender messageSender = this.configuration.messageSender();
        final CacheTransportConfiguration transportConfiguration = cacheConfig.transportConfiguration();

        transportConfiguration
                .targetConfigurations()
                .forEach(targetConfig -> {

                    logger.fine(() -> "Event %s will be sent to %s".formatted(event.toString(), targetConfig.endpoint()));

                    final Object serializedEvent = targetConfig.serializer().serialize(event);
                    messageSender.send(serializedEvent, targetConfig.endpoint());
                });
    }

    private boolean needToSendEvent(final CacheConfiguration cacheConfiguration, final CacheEntryEventType eventType) {
        return eventType != CacheEntryEventType.EVICTED && eventType != CacheEntryEventType.ADDED || cacheConfiguration.cacheType() != CacheType.INVALIDATED;
    }

    private void applyEvent(final CacheEntryEvent<Object, Object> event, final CacheConfiguration configuration) {

        final Optional<Cache<Object, Object>> cache = this.configuration.cacheManager().getCache(event.cacheName());
        cache.ifPresent(c -> processEvent(configuration.cacheType(), c, event));
    }

    private void processEvent(
            final CacheType cacheType,
            final Cache<Object, Object> cache,
            final CacheEntryEvent<Object, Object> event) {

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

    private Optional<CacheEntryEvent<Object, Object>> convertFromSerializedEvent(
            final CacheConfiguration cacheConfiguration,
            final String endpoint,
            final Object event) {

        return cacheConfiguration
                .transportConfiguration()
                .getSourceConfigurationByEndpointName(endpoint)
                .map(sourceConfig -> sourceConfig.deserializer().deserialize(event));
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
