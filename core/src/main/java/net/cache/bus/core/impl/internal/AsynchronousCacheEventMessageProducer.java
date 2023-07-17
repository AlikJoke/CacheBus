package net.cache.bus.core.impl.internal;

import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.configuration.CacheBusTransportConfiguration;
import net.cache.bus.core.configuration.CacheConfiguration;
import net.cache.bus.core.impl.internal.util.RingBuffer;
import net.cache.bus.core.impl.internal.util.StripedRingBuffersContainer;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Реализация асинхронного отправителя событий в канал на основе кольцевых буферов.
 * Поток отправки в канал кладет сообщение в соответствующий кольцевой буфер, вычисляемый по хэш-ключу
 * сообщения (для обеспечения гарантии последовательной отправки событий с одним ключом элемента в одном кэше).
 *
 * @author Alik
 * @see RingBuffer
 * @see StripedRingBuffersContainer
 */
@ThreadSafe
@Immutable
public final class AsynchronousCacheEventMessageProducer extends CacheEventMessageProducer {

    private final StripedRingBuffersContainer<CacheEntryEvent<?, ?>> eventBuffers;
    private final List<Future<?>> sendingTasks;

    public AsynchronousCacheEventMessageProducer(
            @Nonnull final CacheBusTransportConfiguration transportConfiguration,
            @Nonnull final Map<String, CacheConfiguration> cacheConfigurations,
            @Nonnull final StripedRingBuffersContainer<CacheEntryEvent<?, ?>> eventBuffers) {
        super(transportConfiguration);
        this.eventBuffers = Objects.requireNonNull(eventBuffers, "eventBuffers");
        this.sendingTasks = startProcessingTasks(cacheConfigurations, eventBuffers);
    }

    @Override
    public void produce(@Nonnull CacheConfiguration cacheConfiguration, @Nonnull CacheEntryEvent<?, ?> event) {

        final int bufferIndex = computeBufferIndexByHash(event.computeEventHashKey());
        final RingBuffer<CacheEntryEvent<?, ?>> ringBuffer = this.eventBuffers.get(bufferIndex);

        try {
            ringBuffer.offer(event);
        } catch (InterruptedException ex) {
            logger.info("Thread was interrupted", ex);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        logger.info("Producer closure was called");
        this.sendingTasks.forEach(future -> future.cancel(true));
    }

    private int computeBufferIndexByHash(final int hash) {
        return hash & (this.eventBuffers.size() - 1);
    }

    private List<Future<?>> startProcessingTasks(
            @Nonnull final Map<String, CacheConfiguration> cacheConfigurations,
            @Nonnull final StripedRingBuffersContainer<CacheEntryEvent<?, ?>> eventBuffers) {

        final ExecutorService sendingPool = this.transportConfiguration.asyncSendingPool();
        final List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < eventBuffers.size(); i++) {
            final RingBuffer<CacheEntryEvent<?, ?>> eventBuffer = eventBuffers.get(i);

            final Future<?> future = sendingPool.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        final CacheEntryEvent<?, ?> event = eventBuffer.poll();
                        super.produce(cacheConfigurations.get(event.cacheName()), event);
                    } catch (InterruptedException ex) {
                        logger.info("Thread was interrupted", ex);
                        return;
                    }
                }
            });
            futures.add(future);
        }

        return futures;
    }
}
