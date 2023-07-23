package net.cache.bus.core.impl.internal.util;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

/**
 * A container that holds striped ring buffers. Buffers are accessed by index.<br>
 * The index is referred to as a "stripe" because the container is used to divide data into "stripes"
 * that are processed in different threads.
 *
 * @param <E> the type of data stored in the buffers
 * @author Alik
 * @see RingBuffer
 */
public final class StripedRingBuffersContainer<E> {

    private static final int RING_BUFFER_DEFAULT_CAPACITY = 256;

    private final RingBuffer<E>[] buffers;

    /**
     * Container constructor that creates a set of buffers with the default size {@linkplain StripedRingBuffersContainer#RING_BUFFER_DEFAULT_CAPACITY}.
     *
     * @param stripes the number of stripes (number of buffers).
     */
    public StripedRingBuffersContainer(final int stripes) {
        this(stripes, RING_BUFFER_DEFAULT_CAPACITY);
    }

    /**
     * Container constructor that creates a set of buffers with the specified size.
     *
     * @param stripes        the number stripes (number of buffers); rounded down to the nearest even number if odd.
     * @param bufferCapacity the capacity (size) of each buffer.
     */
    public StripedRingBuffersContainer(final int stripes, final int bufferCapacity) {
        final int evenStripes = stripes % 2 == 1 ? stripes - 1 : stripes;
        @SuppressWarnings("unchecked") final RingBuffer<E>[] buffers = new RingBuffer[evenStripes];
        this.buffers = buffers;

        final int capacity = bufferCapacity <= 0 ? RING_BUFFER_DEFAULT_CAPACITY : bufferCapacity;
        for (int i = 0; i < evenStripes; i++) {
            this.buffers[i] = new RingBuffer<>(capacity);
        }
    }

    /**
     * Returns the circular buffer based on its index (stripe).
     *
     * @param index the buffer index; must be non-negative.
     * @return the circular buffer with the specified index, cannot be {@code null}.
     */
    @Nonnull
    public RingBuffer<E> get(final int index) {
        return this.buffers[index];
    }

    /**
     * Returns the size of the container (number of stripes).
     *
     * @return the number of stripes, cannot be {@code null}.
     */
    @Nonnegative
    public int size() {
        return this.buffers.length;
    }
}
