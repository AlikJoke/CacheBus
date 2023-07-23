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
 * Task of processing incoming messages from other servers about cache element changes.
 * The task retrieves messages for processing from the circular buffer where the receiving thread from the channel puts them.
 * If there are no messages in the buffer, the thread blocks (contract of this circular buffer implementation).
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
    private final Runnable interruptionHandler;

    CacheEventMessageProcessingTask(
            @Nonnull final CacheBus cacheBus,
            @Nonnull final RingBuffer<byte[]> messageBuffer,
            @Nonnull final Runnable interruptionHandler) {
        this.messageBuffer = Objects.requireNonNull(messageBuffer, "messageBuffer");
        this.cacheBus = Objects.requireNonNull(cacheBus, "cacheBus");
        this.interruptionHandler = Objects.requireNonNull(interruptionHandler, "interruptionHandler");
    }

    @Override
    public void run() {

        while (!Thread.currentThread().isInterrupted()) {
            try {
                final byte[] message = this.messageBuffer.poll();
                this.cacheBus.receive(message);
            } catch (InterruptedException ex) {
                logger.info("Thread was interrupted", ex);
                this.interruptionHandler.run();
                return;
            }
        }
    }
}
