package net.cache.bus.core.impl;

import net.cache.bus.core.configuration.CacheTransportConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@ThreadSafe
@Immutable
public final class ImmutableCacheTransportConfiguration implements CacheTransportConfiguration {

    private final Set<TargetEndpointConfiguration> targetEndpointConfigurations;
    private final Set<SourceEndpointConfiguration> sourceEndpointConfigurations;
    private final Map<String, TargetEndpointConfiguration> targetEndpointConfigurationsMap;
    private final Map<String, SourceEndpointConfiguration> sourceEndpointConfigurationMap;

    public ImmutableCacheTransportConfiguration(
            @Nonnull Set<TargetEndpointConfiguration> targetEndpointConfigurations,
            @Nonnull Set<SourceEndpointConfiguration> sourceEndpointConfigurations) {
        this.targetEndpointConfigurations = Collections.unmodifiableSet(targetEndpointConfigurations);
        this.sourceEndpointConfigurations = Collections.unmodifiableSet(sourceEndpointConfigurations);

        this.targetEndpointConfigurationsMap = targetEndpointConfigurations
                                                .stream()
                                                .collect(Collectors.toUnmodifiableMap(TargetEndpointConfiguration::endpoint, Function.identity()));
        this.sourceEndpointConfigurationMap = sourceEndpointConfigurations
                                                .stream()
                                                .collect(Collectors.toUnmodifiableMap(SourceEndpointConfiguration::endpoint, Function.identity()));
    }

    @Nonnull
    @Override
    public Set<TargetEndpointConfiguration> targetConfigurations() {
        return this.targetEndpointConfigurations;
    }

    @Nonnull
    @Override
    public Optional<TargetEndpointConfiguration> getTargetConfigurationByEndpointName(@Nonnull String endpointName) {
        return Optional.ofNullable(this.targetEndpointConfigurationsMap.get(endpointName));
    }

    @Nonnull
    @Override
    public Set<SourceEndpointConfiguration> sourceConfigurations() {
        return this.sourceEndpointConfigurations;
    }

    @Nonnull
    @Override
    public Optional<SourceEndpointConfiguration> getSourceConfigurationByEndpointName(@Nonnull String endpointName) {
        return Optional.ofNullable(this.sourceEndpointConfigurationMap.get(endpointName));
    }

    @Override
    public String toString() {
        return "ImmutableCacheTransportConfiguration{" +
                "targetEndpointConfigurations=" + targetEndpointConfigurations +
                ", sourceEndpointConfigurations=" + sourceEndpointConfigurations +
                '}';
    }

    @Nonnull
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Set<TargetEndpointConfiguration> targetEndpointConfigurations = new HashSet<>();
        private final Set<SourceEndpointConfiguration> sourceEndpointConfigurations = new HashSet<>();

        public Builder addTargetEndpointConfiguration(@Nonnull final TargetEndpointConfiguration targetEndpointConfiguration) {
            this.targetEndpointConfigurations.add(Objects.requireNonNull(targetEndpointConfiguration, "targetEndpointConfiguration"));
            return this;
        }

        public Builder addSourceEndpointConfiguration(@Nonnull final SourceEndpointConfiguration sourceEndpointConfiguration) {
            this.sourceEndpointConfigurations.add(Objects.requireNonNull(sourceEndpointConfiguration, "sourceEndpointConfiguration"));
            return this;
        }

        public Builder setTargetEndpointConfigurations(@Nonnull final Set<TargetEndpointConfiguration> targetEndpointConfigurations) {
            this.targetEndpointConfigurations.clear();
            this.targetEndpointConfigurations.addAll(targetEndpointConfigurations);
            return this;
        }

        public Builder addSourceEndpointConfigurations(@Nonnull final Set<SourceEndpointConfiguration> sourceEndpointConfigurations) {
            this.sourceEndpointConfigurations.clear();
            this.sourceEndpointConfigurations.addAll(sourceEndpointConfigurations);
            return this;
        }

        @Nonnull
        public CacheTransportConfiguration build() {
            return new ImmutableCacheTransportConfiguration(this.targetEndpointConfigurations, this.sourceEndpointConfigurations);
        }
    }
}
