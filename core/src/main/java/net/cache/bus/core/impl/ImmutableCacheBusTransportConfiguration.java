package net.cache.bus.core.impl;

import net.cache.bus.core.configuration.CacheBusMessageChannelConfiguration;
import net.cache.bus.core.configuration.CacheBusTransportConfiguration;
import net.cache.bus.core.transport.CacheBusMessageChannel;
import net.cache.bus.core.transport.CacheEntryEventConverter;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Objects;

@ThreadSafe
@Immutable
public final class ImmutableCacheBusTransportConfiguration implements CacheBusTransportConfiguration {

    private final CacheEntryEventConverter converter;
    private final CacheBusMessageChannel<CacheBusMessageChannelConfiguration> messageChannel;
    private final CacheBusMessageChannelConfiguration messageChannelConfiguration;

    public ImmutableCacheBusTransportConfiguration(
            @Nonnull CacheEntryEventConverter converter,
            @Nonnull CacheBusMessageChannel<CacheBusMessageChannelConfiguration> messageChannel,
            @Nonnull CacheBusMessageChannelConfiguration messageChannelConfiguration) {
        this.converter = Objects.requireNonNull(converter, "converter");
        this.messageChannel = Objects.requireNonNull(messageChannel, "messageChannel");
        this.messageChannelConfiguration = Objects.requireNonNull(messageChannelConfiguration, "messageChannelConfiguration");
    }

    @Nonnull
    @Override
    public CacheEntryEventConverter converter() {
        return this.converter;
    }

    @Nonnull
    @Override
    public CacheBusMessageChannel<CacheBusMessageChannelConfiguration> messageChannel() {
        return this.messageChannel;
    }

    @Nonnull
    @Override
    public CacheBusMessageChannelConfiguration messageChannelConfiguration() {
        return this.messageChannelConfiguration;
    }

    @Nonnull
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private CacheEntryEventConverter converter;
        private CacheBusMessageChannel<CacheBusMessageChannelConfiguration> messageChannel;
        private CacheBusMessageChannelConfiguration messageChannelConfiguration;

        public Builder setConverter(@Nonnull final CacheEntryEventConverter converter) {
            this.converter = converter;
            return this;
        }

        public Builder setMessageChannel(@Nonnull final CacheBusMessageChannel<CacheBusMessageChannelConfiguration> messageChannel) {
            this.messageChannel = messageChannel;
            return this;
        }

        public Builder setMessageChannelConfiguration(@Nonnull final CacheBusMessageChannelConfiguration messageChannelConfiguration) {
            this.messageChannelConfiguration = messageChannelConfiguration;
            return this;
        }

        @Nonnull
        public CacheBusTransportConfiguration build() {
            return new ImmutableCacheBusTransportConfiguration(this.converter, this.messageChannel, this.messageChannelConfiguration);
        }
    }
}
