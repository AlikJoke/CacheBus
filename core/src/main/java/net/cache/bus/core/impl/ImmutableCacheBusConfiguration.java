package net.cache.bus.core.impl;

import net.cache.bus.core.CacheEventListenerFactory;
import net.cache.bus.core.CacheManager;
import net.cache.bus.core.configuration.CacheBusConfiguration;
import net.cache.bus.core.configuration.CacheConfiguration;
import net.cache.bus.core.transport.CacheEntryEventMessageSender;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@ThreadSafe
@Immutable
public final class ImmutableCacheBusConfiguration implements CacheBusConfiguration {

    private final Set<CacheConfiguration> cacheConfigurations;
    private final Map<String, CacheConfiguration> cacheConfigurationsMap;
    private final CacheEntryEventMessageSender messageSender;
    private final CacheManager cacheManager;
    private final CacheEventListenerFactory cacheEventListenerFactory;

    public ImmutableCacheBusConfiguration(@Nonnull CacheBusConfiguration configuration) {
        this(configuration.cacheConfigurations(), configuration.messageSender(), configuration.cacheManager(), configuration.cacheEventListenerFactory());
    }

    public ImmutableCacheBusConfiguration(
            @Nonnull Set<CacheConfiguration> cacheConfigurations,
            @Nonnull CacheEntryEventMessageSender messageSender,
            @Nonnull CacheManager cacheManager,
            @Nonnull CacheEventListenerFactory cacheEventListenerFactory) {
        this.cacheConfigurations = Collections.unmodifiableSet(Objects.requireNonNull(cacheConfigurations, "cacheConfigurations"));
        this.cacheConfigurationsMap = cacheConfigurations
                                            .stream()
                                            .collect(Collectors.toUnmodifiableMap(CacheConfiguration::cacheName, Function.identity()));

        this.cacheEventListenerFactory = Objects.requireNonNull(cacheEventListenerFactory, "cacheEventListenerFactory");
        this.messageSender = Objects.requireNonNull(messageSender, "messageSender");
        this.cacheManager = Objects.requireNonNull(cacheManager, "cacheManager");
    }

    @Nonnull
    @Override
    public Set<CacheConfiguration> cacheConfigurations() {
        return this.cacheConfigurations;
    }

    @Nonnull
    @Override
    public Optional<CacheConfiguration> getCacheConfigurationByName(@Nonnull String cacheName) {
        return Optional.ofNullable(this.cacheConfigurationsMap.get(cacheName));
    }

    @Nonnull
    @Override
    public CacheEntryEventMessageSender messageSender() {
        return this.messageSender;
    }

    @Nonnull
    @Override
    public CacheManager cacheManager() {
        return this.cacheManager;
    }

    @Nonnull
    @Override
    public CacheEventListenerFactory cacheEventListenerFactory() {
        return this.cacheEventListenerFactory;
    }

    @Override
    public String toString() {
        return "ImmutableCacheBusConfiguration{" +
                "cacheConfigurations=" + cacheConfigurations +
                ", messageSender=" + messageSender +
                ", cacheManager=" + cacheManager +
                ", cacheEventListenerFactory=" + cacheEventListenerFactory +
                '}';
    }

    @Nonnull
    public static Builder builder() {
        return new Builder();
    }

    @NotThreadSafe
    public static class Builder {

        private final Set<CacheConfiguration> cacheConfigurations = new HashSet<>();
        private CacheEntryEventMessageSender messageSender;
        private CacheManager cacheManager;
        private CacheEventListenerFactory cacheEventListenerFactory;

        public Builder addCacheConfiguration(@Nonnull CacheConfiguration cacheConfiguration) {
            this.cacheConfigurations.add(Objects.requireNonNull(cacheConfiguration, "cacheConfiguration"));
            return this;
        }

        public Builder setCacheConfigurations(@Nonnull Set<CacheConfiguration> cacheConfigurations) {
            this.cacheConfigurations.clear();
            this.cacheConfigurations.addAll(cacheConfigurations);
            return this;
        }

        public Builder setMessageSender(@Nonnull CacheEntryEventMessageSender messageSender) {
            this.messageSender = messageSender;
            return this;
        }

        public Builder setCacheManager(@Nonnull CacheManager cacheManager) {
            this.cacheManager = cacheManager;
            return this;
        }

        public Builder setCacheEventListenerFactory(@Nonnull CacheEventListenerFactory cacheEventListenerFactory) {
            this.cacheEventListenerFactory = cacheEventListenerFactory;
            return this;
        }

        @Nonnull
        public CacheBusConfiguration build() {
            return new ImmutableCacheBusConfiguration(
                    Collections.unmodifiableSet(this.cacheConfigurations),
                    this.messageSender,
                    this.cacheManager,
                    this.cacheEventListenerFactory
            );
        }
    }
}
