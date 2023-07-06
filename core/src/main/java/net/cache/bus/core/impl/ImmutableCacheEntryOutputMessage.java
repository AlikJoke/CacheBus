package net.cache.bus.core.impl;

import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.CacheEntryEventType;
import net.cache.bus.core.transport.CacheEntryOutputMessage;

import javax.annotation.Nonnull;

final class ImmutableCacheEntryOutputMessage implements CacheEntryOutputMessage {

    private final String cacheName;
    private final CacheEntryEventType eventType;
    private final byte[] messageBody;

    public ImmutableCacheEntryOutputMessage(
            @Nonnull final CacheEntryEvent<?, ?> sourceEvent,
            @Nonnull final byte[] messageBody) {
        this.cacheName = sourceEvent.cacheName();
        this.eventType = sourceEvent.eventType();
        this.messageBody = messageBody;
    }

    @Nonnull
    @Override
    public String cacheName() {
        return this.cacheName;
    }

    @Nonnull
    @Override
    public CacheEntryEventType eventType() {
        return this.eventType;
    }

    @Nonnull
    @Override
    public byte[] cacheEntryMessageBody() {
        return this.messageBody;
    }
}
