package net.cache.bus.core.impl.internal;

import net.cache.bus.core.CacheBus;
import net.cache.bus.core.CacheEventMessageConsumer;
import net.cache.bus.core.impl.internal.util.RingBuffer;
import net.cache.bus.core.impl.internal.util.StripedRingBuffersContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Реализация асинхронного потребителя сообщений из канала на основе кольцевых буферов.
 * Поток получения из канала кладет сообщение в соответствующий кольцевой буфер, вычисляемый по хэш-ключу
 * сообщения (для обеспечения гарантии последовательной обработки сообщений с одним ключом элемента в одном кэше).
 *
 * @author Alik
 * @see RingBuffer
 * @see StripedRingBuffersContainer
 */
@ThreadSafe
@Immutable
public final class AsynchronousCacheEventMessageConsumer implements CacheEventMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(AsynchronousCacheEventMessageConsumer.class);

    private final StripedRingBuffersContainer<byte[]> messageBuffers;
    private final List<Future<?>> processingTasks;

    public AsynchronousCacheEventMessageConsumer(
            @Nonnull final CacheBus cacheBus,
            @Nonnull final StripedRingBuffersContainer<byte[]> messageBuffers,
            @Nonnull final ExecutorService processingPool) {
        this.messageBuffers = Objects.requireNonNull(messageBuffers, "messageBuffers");
        this.processingTasks = startProcessingTasks(cacheBus, messageBuffers, processingPool);
    }

    @Override
    public void accept(int messageHash, @Nonnull byte[] messageBody) {
        final int bufferIndex = computeBufferIndexByHash(messageHash);
        final RingBuffer<byte[]> ringBuffer = this.messageBuffers.get(bufferIndex);

        try {
            ringBuffer.offer(messageBody);
        } catch (InterruptedException ex) {
            logger.info("Thread was interrupted", ex);
            Thread.currentThread().interrupt();
        }
    }

    private int computeBufferIndexByHash(final int hash) {
        return hash & (this.messageBuffers.size() - 1);
    }

    private List<Future<?>> startProcessingTasks(
            @Nonnull final CacheBus cacheBus,
            @Nonnull final StripedRingBuffersContainer<byte[]> messageBuffers,
            @Nonnull final ExecutorService processingPool) {

        final List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < messageBuffers.size(); i++) {
            final RingBuffer<byte[]> messageBuffer = messageBuffers.get(i);
            final Runnable processingTask = new CacheEventMessageProcessingTask(cacheBus, messageBuffer);

            final Future<?> future = processingPool.submit(processingTask);
            futures.add(future);
        }

        return futures;
    }

    @Override
    public void close() {
        logger.info("Consumer closure was called");
        this.processingTasks.forEach(future -> future.cancel(true));
    }
}
