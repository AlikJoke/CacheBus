package net.cache.bus.core.impl.internal;

import net.cache.bus.core.CacheBus;
import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.impl.internal.util.StripedRingBuffersContainer;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AsynchronousCacheEventMessageConsumerTest {

    @Test
    public void testAsyncConsuming() throws InterruptedException {
        // preparation
        final TestCacheBus cacheBus = new TestCacheBus();
        final int stripes = 5, messageCount = 100;
        final StripedRingBuffersContainer<byte[]> buffersContainer = new StripedRingBuffersContainer<>(stripes);
        final int threadsCount = buffersContainer.size();
        final var processingPool = Executors.newFixedThreadPool(threadsCount);
        try (final var consumer = new AsynchronousCacheEventMessageConsumer(cacheBus, buffersContainer, processingPool)) {

            // action
            final byte[] message = new byte[]{1, 2, 3};
            for (int i = 0; i < messageCount; i++) {
                consumer.accept(i, message);
            }

            Thread.sleep(Duration.ofMillis(10));
        }

        processingPool.close();

        // checks
        final int messagesByThreadCount = messageCount / threadsCount;
        assertEquals(threadsCount, cacheBus.eventsByThread.size(), "Events must be consumer in " + threadsCount + " threads");
        for (int i = 0; i < threadsCount; i++) {
            cacheBus.eventsByThread.values().forEach(messagesByThread -> assertEquals(messagesByThreadCount, messagesByThread.size(), "Messages must be divided into threads evenly"));
        }
    }

    static class TestCacheBus implements CacheBus {

        private final Map<String, List<byte[]>> eventsByThread = new ConcurrentHashMap<>();

        @Override
        public <K extends Serializable, V extends Serializable> void send(@Nonnull CacheEntryEvent<K, V> event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void receive(@Nonnull byte[] binaryEventData) {
            this.eventsByThread.computeIfAbsent(Thread.currentThread().getName(), t -> new ArrayList<>()).add(binaryEventData);
        }
    }
}
