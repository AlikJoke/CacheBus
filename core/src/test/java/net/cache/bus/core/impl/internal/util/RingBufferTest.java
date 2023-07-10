package net.cache.bus.core.impl.internal.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class RingBufferTest {

    @Test
    public void testWhenOfferInNotFullBufferAndPoolFromNotEmptyBufferThenOk() throws InterruptedException {
        final RingBuffer<Integer> buffer = new RingBuffer<>(3);
        buffer.offer(1);
        buffer.offer(2);
        buffer.offer(3);

        assertEquals(1, buffer.poll());
        assertEquals(2, buffer.poll());
        assertEquals(3, buffer.poll());

        buffer.offer(3);

        assertEquals(3, buffer.poll());
    }

    @Test
    public void testWhenPollFromEmptyBufferThenBlockUntilDataNotAvailable() throws InterruptedException {
        final RingBuffer<Integer> buffer = new RingBuffer<>(2);
        buffer.offer(1);

        try (final ExecutorService executorService = Executors.newSingleThreadExecutor()) {
            final Future<?> future = executorService.submit(() -> {
                try {
                    assertEquals(1, buffer.poll());
                    assertEquals(2, buffer.poll());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });

            Thread.sleep(Duration.ofMillis(10));
            assertFalse(future.isDone(), "Read thread must be blocked because buffer is empty");
            buffer.offer(2);

            Thread.sleep(Duration.ofMillis(1));
            assertTrue(future.isDone(), "Read thread must be finished because buffer is not empty after poll");
        }
    }

    @Test
    public void testWhenOfferInFullBufferThenBlockUntilDataNotRead() throws InterruptedException {

        final RingBuffer<Integer> buffer = new RingBuffer<>(2);
        buffer.offer(1);
        buffer.offer(2);

        try (final ExecutorService executorService = Executors.newSingleThreadExecutor()) {
            final Future<?> future = executorService.submit(() -> {
                try {
                    buffer.offer(3);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });

            Thread.sleep(Duration.ofMillis(10));
            assertFalse(future.isDone(), "Write thread must be blocked because buffer is full");
            assertEquals(1, buffer.poll());

            Thread.sleep(Duration.ofMillis(1));
            assertTrue(future.isDone(), "Write thread must be finished because buffer is not full after poll");
        }
    }

    @Test
    public void testWhenInterruptOfOfferThread() throws InterruptedException {

        final RingBuffer<Integer> buffer = new RingBuffer<>(2);
        buffer.offer(1);
        buffer.offer(2);

        try (final ExecutorService executorService = Executors.newSingleThreadExecutor()) {
            final AtomicBoolean offerWasInterrupted = new AtomicBoolean(false);
            final Future<?> future = executorService.submit(() -> {
                try {
                    buffer.offer(3);
                } catch (InterruptedException e) {
                    offerWasInterrupted.set(true);
                    throw new RuntimeException(e);
                }
            });

            Thread.sleep(Duration.ofMillis(2));
            future.cancel(true);
            Thread.sleep(Duration.ofMillis(2));
            assertTrue(offerWasInterrupted.get(), "Thread should be interrupted");
        }
    }
}
