package net.cache.bus.core.impl.internal;

import net.cache.bus.core.CacheBus;
import net.cache.bus.core.impl.internal.util.RingBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Objects;

/**
 * Задача обработки поступивших с других серверов сообщений об изменении элементов кэша.
 * Сообщения для обработки задача получает из кольцевого буфера, куда поток получения из канала кладет их.
 * Если сообщений в буфере нет, то поток блокируется (контракт данной реализации кольцевого буфера.
 *
 * @author Alik
 * @see CacheBus#receive(byte[])
 * @see RingBuffer#poll()
 */
@ThreadSafe
@Immutable
final class CacheEventMessageProcessingTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(CacheEventMessageProcessingTask.class);

    private final CacheBus cacheBus;
    private final RingBuffer<byte[]> messageBuffer;

    CacheEventMessageProcessingTask(
            @Nonnull final CacheBus cacheBus,
            @Nonnull final RingBuffer<byte[]> messageBuffer) {
        this.messageBuffer = Objects.requireNonNull(messageBuffer, "messageBuffer");
        this.cacheBus = Objects.requireNonNull(cacheBus, "cacheBus");
    }

    @Override
    public void run() {

        while (!Thread.currentThread().isInterrupted()) {
            try {
                final byte[] message = this.messageBuffer.poll();
                this.cacheBus.receive(message);
            } catch (InterruptedException ex) {
                logger.info("Thread was interrupted", ex);
                return;
            }
        }
    }
}
