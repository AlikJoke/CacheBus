package net.cache.bus.transport.addons;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class ConcurrentLinkedBlockingQueueTest {

    @Test
    public void testTakeFromQueue() throws InterruptedException {
        final var queue = new ConcurrentLinkedBlockingQueue<Integer>();
        for (int i = 0; i < 5; i++) {
            queue.offer(i);
        }

        for (int i = 0; i < 5; i++) {
            assertEquals(i, queue.take(), "Taken from queue must be equals to previously offered items");
        }

        try (final ExecutorService executorService = Executors.newSingleThreadExecutor()) {

            final Future<Integer> future = executorService.submit(queue::take);
            Thread.sleep(Duration.ofMillis(100));
            future.cancel(true);
            assertThrows(CancellationException.class, future::get, "Taken thread must be cancelled");

            executorService.submit(() -> {
                Thread.sleep(Duration.ofMillis(100));
                queue.offer(2);
                return true;
            });

            assertEquals(2, queue.take(), "Offered in another thread element must be taken by current thread");
        }
    }

    @Test
    public void testClearQueue() throws InterruptedException {

        final var queue = new ConcurrentLinkedBlockingQueue<Integer>();
        for (int i = 0; i < 5; i++) {
            queue.offer(i);
        }

        queue.clear();

        assertNull(queue.poll(0, TimeUnit.MILLISECONDS), "Queue must be empty");
    }

    @Test
    public void testPollFromQueue() throws InterruptedException {

        final var queue = new ConcurrentLinkedBlockingQueue<Integer>();
        for (int i = 0; i < 5; i++) {
            queue.offer(i);
        }

        for (int i = 0; i < 5; i++) {
            assertEquals(i, queue.poll(0, TimeUnit.MILLISECONDS), "Polled from queue must be equals to previously offered items");
        }

        assertNull(queue.poll(0, TimeUnit.MILLISECONDS), "Element from queue must be null");
        try (final ExecutorService executorService = Executors.newSingleThreadExecutor()) {

            executorService.submit(() -> {
                Thread.sleep(Duration.ofMillis(100));
                queue.offer(2);
                return true;
            });

            assertEquals(2, queue.poll(1, TimeUnit.MINUTES), "Offered in another thread element must be polled by current thread");
        }
    }
}
