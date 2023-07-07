package net.cache.bus.core.impl;

import net.cache.bus.core.configuration.CacheBusConfiguration;
import net.cache.bus.core.configuration.CacheBusTransportConfiguration;
import net.cache.bus.core.configuration.CacheConfiguration;
import net.cache.bus.core.configuration.CacheProviderConfiguration;

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
    private final CacheBusTransportConfiguration transportConfiguration;
    private final CacheProviderConfiguration providerConfiguration;

    public ImmutableCacheBusConfiguration(@Nonnull CacheBusConfiguration configuration) {
        this(configuration.cacheConfigurations(), configuration.transportConfiguration(), configuration.providerConfiguration());
    }

    public ImmutableCacheBusConfiguration(
            @Nonnull Set<CacheConfiguration> cacheConfigurations,
            @Nonnull CacheBusTransportConfiguration transportConfiguration,
            @Nonnull CacheProviderConfiguration providerConfiguration) {
        this.cacheConfigurations = Collections.unmodifiableSet(Objects.requireNonNull(cacheConfigurations, "cacheConfigurations"));
        this.cacheConfigurationsMap = cacheConfigurations
                                            .stream()
                                            .collect(Collectors.toUnmodifiableMap(CacheConfiguration::cacheName, Function.identity()));

        this.providerConfiguration = Objects.requireNonNull(providerConfiguration, "providerConfiguration");
        this.transportConfiguration = Objects.requireNonNull(transportConfiguration, "transportConfiguration");
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
    public CacheBusTransportConfiguration transportConfiguration() {
        return this.transportConfiguration;
    }

    @Nonnull
    @Override
    public CacheProviderConfiguration providerConfiguration() {
        return this.providerConfiguration;
    }

    @Override
    public String toString() {
        return "ImmutableCacheBusConfiguration{" +
                "cacheConfigurations=" + cacheConfigurations +
                ", transportConfiguration=" + transportConfiguration +
                ", providerConfiguration=" + providerConfiguration +
                '}';
    }

    @Nonnull
    public static Builder builder() {
        return new Builder();
    }

    @NotThreadSafe
    public static class Builder {

        private final Set<CacheConfiguration> cacheConfigurations = new HashSet<>();
        private CacheBusTransportConfiguration transportConfiguration;
        private CacheProviderConfiguration providerConfiguration;

        public Builder addCacheConfiguration(@Nonnull CacheConfiguration cacheConfiguration) {
            this.cacheConfigurations.add(Objects.requireNonNull(cacheConfiguration, "cacheConfiguration"));
            return this;
        }

        public Builder setCacheConfigurations(@Nonnull Set<CacheConfiguration> cacheConfigurations) {
            this.cacheConfigurations.clear();
            this.cacheConfigurations.addAll(cacheConfigurations);
            return this;
        }

        public Builder setTransportConfiguration(@Nonnull CacheBusTransportConfiguration transportConfiguration) {
            this.transportConfiguration = transportConfiguration;
            return this;
        }

        public Builder setProviderConfiguration(@Nonnull CacheProviderConfiguration providerConfiguration) {
            this.providerConfiguration = providerConfiguration;
            return this;
        }

        @Nonnull
        public CacheBusConfiguration build() {
            return new ImmutableCacheBusConfiguration(
                    Collections.unmodifiableSet(this.cacheConfigurations),
                    this.transportConfiguration,
                    this.providerConfiguration
            );
        }
    }
}
