package net.cache.bus.core.impl.internal;

import net.cache.bus.core.CacheBus;
import net.cache.bus.core.CacheEventMessageConsumer;
import net.cache.bus.core.impl.internal.util.RingBuffer;
import net.cache.bus.core.impl.internal.util.StripedRingBuffersContainer;
import net.cache.bus.core.metrics.CacheBusMetricsRegistry;
import net.cache.bus.core.metrics.KnownMetrics;
import net.cache.bus.core.metrics.Metrics;
import net.cache.bus.core.state.ComponentState;
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
 * Implementation of an asynchronous message consumer from a channel based on circular buffers.
 * The receiving thread from the channel puts the message into the corresponding circular buffer,
 * calculated based on the hash key of the message (to ensure sequential processing of messages with
 * the same key in one cache).
 *
 * @author Alik
 * @see RingBuffer
 * @see StripedRingBuffersContainer
 */
@ThreadSafe
@Immutable
public final class AsynchronousCacheEventMessageConsumer implements CacheEventMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(AsynchronousCacheEventMessageConsumer.class);

    private static final String CONSUMER_ID = "async-message-consumer";

    private final CacheBusMetricsRegistry metrics;
    private final StripedRingBuffersContainer<byte[]> messageBuffers;
    private final List<Future<?>> processingTasks;
    private final AsyncMessageProcessingState state;

    public AsynchronousCacheEventMessageConsumer(
            @Nonnull final CacheBus cacheBus,
            @Nonnull CacheBusMetricsRegistry metrics,
            @Nonnull final StripedRingBuffersContainer<byte[]> messageBuffers,
            @Nonnull final ExecutorService processingPool) {
        this.messageBuffers = Objects.requireNonNull(messageBuffers, "messageBuffers");
        this.metrics = metrics;
        this.state = new AsyncMessageProcessingState(CONSUMER_ID, "Count of interrupted threads on processing messages from channel: %d", messageBuffers.size());
        this.processingTasks = startProcessingTasks(cacheBus, messageBuffers, processingPool);

        this.metrics.registerTimer(new Metrics.Timer(KnownMetrics.CONSUMER_BUFFER_BLOCKING_OFFER_TIME));
        this.metrics.registerCounter(new Metrics.Counter(KnownMetrics.CONSUMER_INTERRUPTED_THREADS));
    }

    @Override
    public void accept(int messageHash, @Nonnull byte[] messageBody) {
        final int bufferIndex = computeBufferIndexByHash(messageHash);
        final RingBuffer<byte[]> ringBuffer = this.messageBuffers.get(bufferIndex);

        this.metrics.recordExecutionTime(
                KnownMetrics.CONSUMER_BUFFER_BLOCKING_OFFER_TIME,
                () -> offerToBuffer(ringBuffer, messageBody)
        );
    }

    @Nonnull
    @Override
    public ComponentState state() {
        return this.state;
    }

    private int computeBufferIndexByHash(final int hash) {
        return hash & (this.messageBuffers.size() - 1);
    }

    private void offerToBuffer(final RingBuffer<byte[]> ringBuffer, final byte[] messageBody) {

        try {
            if (ringBuffer.offer(messageBody)) {
                logger.info("Buffer of messages to processing is full: maybe you should increase count of threads or buffers capacity?");
                this.state.onBufferFull();
            }
        } catch (InterruptedException ex) {
            logger.info("Thread was interrupted", ex);
            this.metrics.incrementCounter(KnownMetrics.CONSUMER_INTERRUPTED_THREADS);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        logger.info("Consumer closure was called");
        this.processingTasks.forEach(future -> future.cancel(true));
        this.state.toStoppedState();
    }

    private List<Future<?>> startProcessingTasks(
            @Nonnull final CacheBus cacheBus,
            @Nonnull final StripedRingBuffersContainer<byte[]> messageBuffers,
            @Nonnull final ExecutorService processingPool) {

        final List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < messageBuffers.size(); i++) {
            final RingBuffer<byte[]> messageBuffer = messageBuffers.get(i);

            registerBuffersGauge(i, messageBuffer);

            final Runnable processingTask = new CacheEventMessageProcessingTask(cacheBus, messageBuffer, this.state::increaseCountOfInterruptedThreads);

            final Future<?> future = processingPool.submit(processingTask);
            futures.add(future);
        }

        return futures;
    }

    private void registerBuffersGauge(final int bufferIdx, final RingBuffer<byte[]> buffer) {

        final Metrics.Gauge<RingBuffer<byte[]>> gaugeReadIndex = new Metrics.Gauge<>(
                KnownMetrics.BUFFER_READ_POSITION.id() + ".consumer." + bufferIdx,
                buffer,
                RingBuffer::currentReadIndex,
                KnownMetrics.BUFFER_READ_POSITION.description(),
                KnownMetrics.BUFFER_READ_POSITION.tags()
        );
        this.metrics.registerGauge(gaugeReadIndex);

        final Metrics.Gauge<RingBuffer<byte[]>> gaugeWriteIndex = new Metrics.Gauge<>(
                KnownMetrics.BUFFER_READ_POSITION.id() + ".consumer." + bufferIdx,
                buffer,
                RingBuffer::currentWritePosition,
                KnownMetrics.BUFFER_READ_POSITION.description(),
                KnownMetrics.BUFFER_READ_POSITION.tags()
        );
        this.metrics.registerGauge(gaugeWriteIndex);
    }
}
