package ru.joke.cache.bus.core.impl.internal.util;

import ru.joke.cache.bus.core.configuration.ConfigurationException;

import javax.annotation.Nonnull;
import java.util.concurrent.Semaphore;

/**
 * Implementation of a circular buffer data structure for handling single producer vs single consumer interaction.
 * The peculiarity of this implementation is that it blocks if an addition is being made to the buffer and
 * the buffer is full, or if a read is being made from the buffer and the buffer is empty.
 * The methods only return control in these cases when there is space in the buffer (during addition)
 * or when data appears in the buffer (during reading).
 *
 * @param <E> the type of data stored in the buffer
 * @author Alik
 */
public final class RingBuffer<E> {

    private final Semaphore readSemaphore;
    private final Semaphore writeSemaphore;
    private final E[] elements;
    private final int capacity;

    private volatile int writeCounter;
    private volatile int readCounter;

    public RingBuffer(int capacity) {

        if (capacity <= 0) {
            throw new ConfigurationException("Buffer capacity must be positive: " + capacity);
        }

        this.capacity = capacity;
        @SuppressWarnings("unchecked") final E[] elements = (E[]) new Object[capacity];
        this.elements = elements;
        this.writeCounter = -1;
        this.readCounter = 0;
        this.readSemaphore = new Semaphore(1);
        this.writeSemaphore = new Semaphore(1);
    }

    /**
     * Adds an element to the buffer. The method blocks if the buffer is full.
     *
     * @param elem the element to add to the buffer, cannot be {@code null}.
     * @return a flag indicating whether blocking was required during addition to the buffer (i.e., the buffer was full).
     */
    public boolean offer(@Nonnull final E elem) throws InterruptedException {

        int currentWriteValue;
        boolean isFull = false;
        while ((currentWriteValue = this.writeCounter) - this.readCounter == this.capacity - 1) {
            isFull = true;
            this.writeSemaphore.acquire();
        }

        final int nextCounter = currentWriteValue + 1;
        this.elements[nextCounter % this.capacity] = elem;
        this.writeCounter = nextCounter;
        this.readSemaphore.release();

        return isFull;
    }

    /**
     * Retrieves an element from the buffer. The method blocks if there is no data in the buffer.
     *
     * @return the data element from the buffer, cannot be {@code null}.
     */
    @Nonnull
    public E poll() throws InterruptedException {
        int currentReadPosition;
        while (this.writeCounter < (currentReadPosition = this.readCounter)) {
            this.readSemaphore.acquire();
        }

        final E elem = this.elements[currentReadPosition % this.capacity];
        this.readCounter = currentReadPosition + 1;
        this.writeSemaphore.release();
        return elem;
    }

    /**
     * Returns the current read index of the buffer.
     *
     * @return the current read index of the buffer.
     */
    public int currentReadIndex() {
        return this.readCounter;
    }

    /**
     * Returns the current write index of the buffer.
     *
     * @return the current write index of the buffer.
     */
    public int currentWritePosition() {
        return this.writeCounter;
    }
}
