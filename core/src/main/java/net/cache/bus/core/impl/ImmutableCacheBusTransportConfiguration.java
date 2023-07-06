package net.cache.bus.core.impl;

import net.cache.bus.core.configuration.CacheBusMessageChannelConfiguration;
import net.cache.bus.core.configuration.CacheBusTransportConfiguration;
import net.cache.bus.core.transport.CacheEntryEventDeserializer;
import net.cache.bus.core.transport.CacheBusMessageChannel;
import net.cache.bus.core.transport.CacheEntryEventSerializer;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Objects;

@ThreadSafe
@Immutable
public final class ImmutableCacheBusTransportConfiguration implements CacheBusTransportConfiguration {

    private final CacheEntryEventSerializer serializer;
    private final CacheEntryEventDeserializer deserializer;
    private final CacheBusMessageChannel<CacheBusMessageChannelConfiguration> messageChannel;
    private final CacheBusMessageChannelConfiguration messageChannelConfiguration;

    public ImmutableCacheBusTransportConfiguration(
            @Nonnull CacheEntryEventSerializer serializer,
            @Nonnull CacheEntryEventDeserializer deserializer,
            @Nonnull CacheBusMessageChannel<CacheBusMessageChannelConfiguration> messageChannel,
            @Nonnull CacheBusMessageChannelConfiguration messageChannelConfiguration) {
        this.serializer = Objects.requireNonNull(serializer, "serializer");
        this.deserializer = Objects.requireNonNull(deserializer, "deserializer");
        this.messageChannel = Objects.requireNonNull(messageChannel, "messageChannel");
        this.messageChannelConfiguration = Objects.requireNonNull(messageChannelConfiguration, "messageChannelConfiguration");
    }

    @Nonnull
    @Override
    public CacheEntryEventSerializer serializer() {
        return this.serializer;
    }

    @Nonnull
    @Override
    public CacheEntryEventDeserializer deserializer() {
        return this.deserializer;
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

        private CacheEntryEventSerializer serializer;
        private CacheEntryEventDeserializer deserializer;
        private CacheBusMessageChannel<CacheBusMessageChannelConfiguration> messageChannel;
        private CacheBusMessageChannelConfiguration messageChannelConfiguration;

        public Builder setSerializer(@Nonnull final CacheEntryEventSerializer serializer) {
            this.serializer = serializer;
            return this;
        }

        public Builder setDeserializer(@Nonnull final CacheEntryEventDeserializer deserializer) {
            this.deserializer = deserializer;
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
            return new ImmutableCacheBusTransportConfiguration(this.serializer, this.deserializer, this.messageChannel, this.messageChannelConfiguration);
        }
    }
}
