package ru.joke.cache.bus.transport.addons;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Simplified queue structure based on the thread-safe queue {@link ConcurrentLinkedQueue}.
 * Complements {@link ConcurrentLinkedQueue} with methods that block when retrieving data from the queue
 * when the queue is empty.
 *
 * @param <E> the type of elements stored in the queue
 * @author Alik
 * @see ConcurrentLinkedQueue
 */
@ThreadSafe
public final class ConcurrentLinkedBlockingQueue<E> {

    private final Semaphore emptySemaphore = new Semaphore(1);
    private final Queue<E> sourceQueue = new ConcurrentLinkedQueue<>();

    /**
     * Retrieves and removes the element at the head of the queue. If the queue empty, it blocks and waits for data to appear in the queue
     * for the specified timeout. If the queue is still empty after waiting for the timeout, {@code null} will be returned.
     *
     * @param timeout the timeout for waiting for new elements to appear in an empty queue; must not be negative.
     * @param unit    the time units for waiting for new elements to appear in an empty queue; must not be {@code null}.
     * @return the data element from the head of the queue, or {@code null} if the queue is empty (after the timeout).
     */
    @Nullable
    public E poll(@Nonnegative long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        E result;
        while ((result = this.sourceQueue.poll()) == null
                && this.emptySemaphore.tryAcquire(timeout, unit)) ;

        return result;
    }

    /**
     * Removes and returns the element at the head of the queue.
     * If the queue is empty, it blocks until a new element appears in the queue that can be returned.
     *
     * @return the element from the head of the queue, cannot be {@code null}.
     * @throws InterruptedException if the thread was interrupted from outside
     */
    @Nonnull
    public E take() throws InterruptedException {

        while (true) {
            final E result = this.sourceQueue.poll();
            if (result != null) {
                return result;
            }

            this.emptySemaphore.acquire();
        }
    }

    /**
     * Adds element of data to the tail of the queue.
     *
     * @param elem the data element, cannot be {@code null}.
     */
    public void offer(@Nonnull E elem) {
        this.sourceQueue.offer(elem);
        this.emptySemaphore.release();
    }

    /**
     * Performs the given action each data element in the queue in a loop.
     *
     * @param action the action to be performed on each data element, cannot be {@code null}.
     */
    public void forEach(@Nonnull Consumer<? super E> action) {
        this.sourceQueue.forEach(action);
    }

    /**
     * Clears the queue.
     */
    public void clear() {
        this.sourceQueue.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ConcurrentLinkedBlockingQueue<?> that = (ConcurrentLinkedBlockingQueue<?>) o;
        return sourceQueue.equals(that.sourceQueue);
    }

    @Override
    public int hashCode() {
        return sourceQueue.hashCode();
    }
}
