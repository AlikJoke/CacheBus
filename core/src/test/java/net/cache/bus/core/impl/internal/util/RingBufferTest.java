package net.cache.bus.core.impl.internal.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class RingBufferTest {

    @Test
    public void testWhenOfferInNotFullBufferAndPoolFromNotEmptyBufferThenOk() throws InterruptedException {
        final RingBuffer<Integer> buffer = new RingBuffer<>(3);
        assertFalse(buffer.offer(1), "Offering must be without blocking to non-full buffer");
        assertFalse(buffer.offer(2), "Offering must be without blocking to non-full buffer");
        assertFalse(buffer.offer(3), "Offering must be without blocking to non-full buffer");

        assertEquals(1, buffer.poll());
        assertEquals(2, buffer.poll());
        assertEquals(3, buffer.poll());

        assertFalse(buffer.offer(3), "Offering must be without blocking to non-full buffer");

        assertEquals(3, buffer.poll());
    }

    @Test
    public void testWhenPollFromEmptyBufferThenBlockUntilDataNotAvailable() throws InterruptedException, ExecutionException, TimeoutException {
        final RingBuffer<Integer> buffer = new RingBuffer<>(2);
        buffer.offer(1);

        try (final ExecutorService executorService = Executors.newSingleThreadExecutor()) {
            final Future<Boolean> future = executorService.submit(() -> {
                try {
                    assertEquals(1, buffer.poll());
                    assertEquals(2, buffer.poll());

                    return true;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });

            Thread.sleep(Duration.ofMillis(10));
            assertFalse(future.isDone(), "Read thread must be blocked because buffer is empty");
            buffer.offer(2);

            final Boolean result = future.get(1, TimeUnit.SECONDS);
            assertTrue(result != null && result, "Read thread must be finished because buffer is not empty after poll");
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
                    assertTrue(buffer.offer(3), "Offering must be with blocking to full buffer");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });

            Thread.sleep(Duration.ofMillis(10));
            assertFalse(future.isDone(), "Write thread must be blocked because buffer is full");
            assertEquals(1, buffer.poll());

            Thread.sleep(Duration.ofMillis(10));
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

            final long stampTime = System.currentTimeMillis();
            while (!offerWasInterrupted.get()) {
                Thread.sleep(Duration.ofMillis(5));
                if (System.currentTimeMillis() - stampTime > 60000) {
                    assertTrue(offerWasInterrupted.get(), "Thread should be interrupted");
                }
            }

            assertTrue(offerWasInterrupted.get(), "Thread should be interrupted");
        }
    }
}
