package net.cache.bus.core.impl.internal;

import net.cache.bus.core.CacheBus;
import net.cache.bus.core.CacheEventMessageConsumer;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Objects;

/**
 * Синхронная реализация потребителя сообщений из канала, которая сразу
 * в том же потоке вызывает обработку полученного сообщения.
 *
 * @author Alik
 * @see CacheBus#receive(byte[])
 */
@ThreadSafe
@Immutable
public final class SynchronousCacheEventMessageConsumer implements CacheEventMessageConsumer {

    private final CacheBus cacheBus;

    public SynchronousCacheEventMessageConsumer(@Nonnull final CacheBus cacheBus) {
        this.cacheBus = Objects.requireNonNull(cacheBus, "cacheBus");
    }

    @Override
    public void accept(int messageHash, @Nonnull byte[] messageBody) {
        this.cacheBus.receive(messageBody);
    }
}
