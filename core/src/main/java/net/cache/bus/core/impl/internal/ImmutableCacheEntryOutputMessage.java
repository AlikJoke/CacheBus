package net.cache.bus.core.impl.internal;

import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.transport.CacheEntryOutputMessage;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Arrays;

@ThreadSafe
@Immutable
public final class ImmutableCacheEntryOutputMessage implements CacheEntryOutputMessage {

    private final String cacheName;
    private final byte[] messageBody;
    private final int hashKey;

    public ImmutableCacheEntryOutputMessage(
            @Nonnull final CacheEntryEvent<?, ?> sourceEvent,
            @Nonnull final byte[] messageBody) {
        this.cacheName = sourceEvent.cacheName();
        this.messageBody = messageBody;
        this.hashKey = computeMessageHash(sourceEvent.cacheName(), sourceEvent.key());
    }

    @Nonnull
    @Override
    public String cacheName() {
        return this.cacheName;
    }

    @Nonnull
    @Override
    public byte[] cacheEntryMessageBody() {
        return this.messageBody;
    }

    @Override
    public int messageHashKey() {
        return this.hashKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ImmutableCacheEntryOutputMessage that = (ImmutableCacheEntryOutputMessage) o;

        if (hashKey != that.hashKey) {
            return false;
        }
        if (!cacheName.equals(that.cacheName)) {
            return false;
        }

        return Arrays.equals(messageBody, that.messageBody);
    }

    @Override
    public int hashCode() {
        int result = cacheName.hashCode();
        result = 31 * result + Arrays.hashCode(messageBody);
        result = 31 * result + hashKey;
        return result;
    }

    private <K> int computeMessageHash(final String cacheName, final K key) {
        int result = 31 + cacheName.hashCode();
        return 31 * result + key.hashCode();
    }
}
