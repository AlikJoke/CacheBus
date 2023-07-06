package net.cache.bus.core.impl;

import net.cache.bus.core.configuration.CacheBusTransportConfiguration;
import net.cache.bus.core.transport.CacheEntryEventDeserializer;
import net.cache.bus.core.transport.CacheEntryEventMessageSender;
import net.cache.bus.core.transport.CacheEntryEventSerializer;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Objects;

@ThreadSafe
@Immutable
public final class ImmutableCacheBusTransportConfiguration implements CacheBusTransportConfiguration {

    private final String sourceEndpoint;
    private final String targetEndpoint;
    private final CacheEntryEventSerializer serializer;
    private final CacheEntryEventDeserializer deserializer;
    private final CacheEntryEventMessageSender messageSender;

    public ImmutableCacheBusTransportConfiguration(
            @Nonnull String sourceEndpoint,
            @Nonnull CacheEntryEventSerializer serializer,
            @Nonnull String targetEndpoint,
            @Nonnull CacheEntryEventDeserializer deserializer,
            @Nonnull CacheEntryEventMessageSender messageSender) {
        this.sourceEndpoint = Objects.requireNonNull(sourceEndpoint, "sourceEndpoint");
        this.targetEndpoint = Objects.requireNonNull(targetEndpoint, "targetEndpoint");
        this.serializer = Objects.requireNonNull(serializer, "serializer");
        this.deserializer = Objects.requireNonNull(deserializer, "deserializer");
        this.messageSender = Objects.requireNonNull(messageSender, "messageSender");
    }

    @Nonnull
    @Override
    public String targetEndpoint() {
        return this.targetEndpoint;
    }

    @Nonnull
    @Override
    public CacheEntryEventSerializer serializer() {
        return this.serializer;
    }

    @Nonnull
    @Override
    public String sourceEndpoint() {
        return this.sourceEndpoint;
    }

    @Nonnull
    @Override
    public CacheEntryEventDeserializer deserializer() {
        return this.deserializer;
    }

    @Nonnull
    @Override
    public CacheEntryEventMessageSender messageSender() {
        return this.messageSender;
    }

    @Nonnull
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String sourceEndpoint;
        private String targetEndpoint;
        private CacheEntryEventSerializer serializer;
        private CacheEntryEventDeserializer deserializer;
        private CacheEntryEventMessageSender messageSender;

        public Builder setTargetEndpoint(@Nonnull final String targetEndpoint) {
            this.targetEndpoint = targetEndpoint;
            return this;
        }

        public Builder setSourceEndpoint(@Nonnull final String sourceEndpoint) {
            this.sourceEndpoint = sourceEndpoint;
            return this;
        }

        public Builder setSerializer(@Nonnull final CacheEntryEventSerializer serializer) {
            this.serializer = serializer;
            return this;
        }

        public Builder setDeserializer(@Nonnull final CacheEntryEventDeserializer deserializer) {
            this.deserializer = deserializer;
            return this;
        }

        public Builder setMessageSender(@Nonnull final CacheEntryEventMessageSender messageSender) {
            this.messageSender = messageSender;
            return this;
        }

        @Nonnull
        public CacheBusTransportConfiguration build() {
            return new ImmutableCacheBusTransportConfiguration(this.sourceEndpoint, this.serializer, this.targetEndpoint, this.deserializer, this.messageSender);
        }
    }
}
