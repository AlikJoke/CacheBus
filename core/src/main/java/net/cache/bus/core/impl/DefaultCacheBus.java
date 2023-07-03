package net.cache.bus.core.impl;

import lombok.extern.slf4j.Slf4j;
import net.cache.bus.core.Cache;
import net.cache.bus.core.CacheBus;
import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.configuration.CacheBusConfiguration;
import net.cache.bus.core.configuration.CacheConfiguration;
import net.cache.bus.core.configuration.CacheTransportConfiguration;
import net.cache.bus.core.configuration.CacheType;
import net.cache.bus.core.transport.CacheEntryEventMessageSender;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public final class DefaultCacheBus implements CacheBus {

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
        cacheConfiguration.ifPresent(cacheConfig -> {
            final CacheEntryEventMessageSender messageSender = this.configuration.messageSender();
            final CacheTransportConfiguration transportConfiguration = cacheConfig.transportConfiguration();

            transportConfiguration
                    .targetConfigurations()
                    .forEach(targetConfig -> {

                        log.debug("Event {} will be sent to {}", event, targetConfig.endpoint());

                        final Object serializedEvent = targetConfig.serializer().serialize(event);
                        messageSender.send(serializedEvent, targetConfig.endpoint());
                    });
        });
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

    private void applyEvent(final CacheEntryEvent<Object, ?> event, final CacheConfiguration configuration) {

        final Optional<Cache<Object>> cache = this.configuration.cacheManager().getCache(event.cacheName());
        cache.ifPresent(c -> processEvent(configuration.cacheType(), c, event));
    }

    private void processEvent(final CacheType cacheType, final Cache<Object> cache, final CacheEntryEvent<Object, ?> event) {

        log.debug("Process event {} with cacheType {}", event, cacheType);

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

    private Optional<CacheEntryEvent<Object, ?>> convertFromSerializedEvent(
            final CacheConfiguration cacheConfiguration,
            final String endpoint,
            final Object event) {

        return cacheConfiguration
                .transportConfiguration()
                .getSourceConfigurationByEndpointName(endpoint)
                .map(sourceConfig -> sourceConfig.deserializer().deserialize(event));
    }
}
