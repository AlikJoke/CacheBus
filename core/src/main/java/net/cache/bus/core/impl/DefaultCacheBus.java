package net.cache.bus.core.impl;

import net.cache.bus.core.Cache;
import net.cache.bus.core.CacheBus;
import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.CacheEntryEventType;
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
public final class DefaultCacheBus implements CacheBus {

    private static final Logger logger = Logger.getLogger(DefaultCacheBus.class.getCanonicalName());
    private static final ThreadLocal<Boolean> locked = new ThreadLocal<>();

    private CacheBusConfiguration configuration;

    public DefaultCacheBus(@Nonnull CacheBusConfiguration configuration) {
        setConfiguration(configuration);
    }

    @Override
    public <K, V> void send(@Nonnull CacheEntryEvent<K, V> event) {
        // To prevent calls from the receiving execution thread
        if (locked.get() != null && locked.get()) {
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

        final Optional<CacheConfiguration> cacheConfiguration = this.configuration.getCacheConfigurationByName(cacheName);
        cacheConfiguration
                .flatMap(cacheConfig -> convertFromSerializedEvent(cacheConfig, endpoint, event))
                .ifPresent(cacheEvent -> applyEvent(cacheEvent, cacheConfiguration.get()));
    }

    @Override
    public void setConfiguration(@Nonnull CacheBusConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
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
        return eventType != CacheEntryEventType.EVICTED || cacheConfiguration.cacheType() != CacheType.INVALIDATED;
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
}
