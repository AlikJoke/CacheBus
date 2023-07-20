package net.cache.bus.core.impl.internal;

import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.configuration.CacheBusTransportConfiguration;
import net.cache.bus.core.configuration.CacheConfiguration;
import net.cache.bus.core.impl.internal.util.RingBuffer;
import net.cache.bus.core.impl.internal.util.StripedRingBuffersContainer;
import net.cache.bus.core.metrics.CacheBusMetricsRegistry;
import net.cache.bus.core.metrics.KnownMetrics;
import net.cache.bus.core.metrics.Metrics;
import net.cache.bus.core.state.ComponentState;

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

    private static final String PRODUCER_ID = "async-message-producer";

    private final StripedRingBuffersContainer<CacheEntryEvent<?, ?>> eventBuffers;
    private final List<Future<?>> sendingTasks;
    private final AsyncMessageProcessingState state;

    public AsynchronousCacheEventMessageProducer(
            @Nonnull final CacheBusMetricsRegistry metrics,
            @Nonnull final CacheBusTransportConfiguration transportConfiguration,
            @Nonnull final Map<String, CacheConfiguration> cacheConfigurations,
            @Nonnull final StripedRingBuffersContainer<CacheEntryEvent<?, ?>> eventBuffers) {
        super(metrics, transportConfiguration);
        this.eventBuffers = Objects.requireNonNull(eventBuffers, "eventBuffers");
        this.state = new AsyncMessageProcessingState(PRODUCER_ID, "Count of interrupted on produce to channel threads: %d", eventBuffers.size());
        this.sendingTasks = startProcessingTasks(cacheConfigurations, eventBuffers);

        this.metrics.registerCounter(new Metrics.Counter(KnownMetrics.PRODUCER_INTERRUPTED_THREADS));
        this.metrics.registerTimer(new Metrics.Timer(KnownMetrics.PRODUCER_BUFFER_BLOCKING_OFFER_TIME));
    }

    @Override
    public void produce(@Nonnull CacheConfiguration cacheConfiguration, @Nonnull CacheEntryEvent<?, ?> event) {

        final int bufferIndex = computeBufferIndexByHash(event.computeEventHashKey());
        final RingBuffer<CacheEntryEvent<?, ?>> ringBuffer = this.eventBuffers.get(bufferIndex);

        this.metrics.recordExecutionTime(
                KnownMetrics.PRODUCER_BUFFER_BLOCKING_OFFER_TIME,
                () -> offerToBuffer(ringBuffer, event)
        );
    }

    @Nonnull
    @Override
    public ComponentState state() {
        return this.state;
    }

    @Override
    public void close() {
        logger.info("Async producer closure was called");
        this.sendingTasks.forEach(future -> future.cancel(true));
        this.state.toStoppedState();
    }

    private int computeBufferIndexByHash(final int hash) {
        return hash & (this.eventBuffers.size() - 1);
    }

    private void offerToBuffer(final RingBuffer<CacheEntryEvent<?, ?>> ringBuffer, final CacheEntryEvent<?, ?> event) {

        try {
            if (ringBuffer.offer(event)) {
                logger.info("Buffer of messages for producing to channel is full: maybe you should increase count of threads or buffers capacity?");
                this.state.onBufferFull();
            }
        } catch (InterruptedException ex) {
            this.logger.info("Thread was interrupted", ex);
            Thread.currentThread().interrupt();
        }
    }

    private List<Future<?>> startProcessingTasks(
            @Nonnull final Map<String, CacheConfiguration> cacheConfigurations,
            @Nonnull final StripedRingBuffersContainer<CacheEntryEvent<?, ?>> eventBuffers) {

        final ExecutorService sendingPool = this.transportConfiguration.asyncSendingPool();
        final List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < eventBuffers.size(); i++) {
            final RingBuffer<CacheEntryEvent<?, ?>> eventBuffer = eventBuffers.get(i);

            registerBuffersGauge(i, eventBuffer);

            final Future<?> future = sendingPool.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        final CacheEntryEvent<?, ?> event = eventBuffer.poll();
                        super.produce(cacheConfigurations.get(event.cacheName()), event);
                    } catch (InterruptedException ex) {
                        this.logger.info("Thread was interrupted", ex);
                        this.state.increaseCountOfInterruptedThreads();
                        this.metrics.incrementCounter(KnownMetrics.PRODUCER_INTERRUPTED_THREADS);

                        return;
                    }
                }
            });
            futures.add(future);
        }

        return futures;
    }

    private void registerBuffersGauge(final int bufferIdx, final RingBuffer<CacheEntryEvent<?, ?>> buffer) {

        final Metrics.Gauge<RingBuffer<CacheEntryEvent<?, ?>>> gaugeReadIndex = new Metrics.Gauge<>(
                KnownMetrics.BUFFER_READ_POSITION.id() + ".producer." + bufferIdx,
                buffer,
                RingBuffer::currentReadIndex,
                KnownMetrics.BUFFER_WRITE_POSITION.description(),
                KnownMetrics.BUFFER_WRITE_POSITION.tags()
        );
        this.metrics.registerGauge(gaugeReadIndex);

        final Metrics.Gauge<RingBuffer<CacheEntryEvent<?, ?>>> gaugeWriteIndex = new Metrics.Gauge<>(
                KnownMetrics.BUFFER_WRITE_POSITION.id() + ".producer." + bufferIdx,
                buffer,
                RingBuffer::currentWritePosition,
                KnownMetrics.BUFFER_WRITE_POSITION.description(),
                KnownMetrics.BUFFER_WRITE_POSITION.tags()
        );
        this.metrics.registerGauge(gaugeWriteIndex);
    }
}
