package ru.joke.cache.bus.core.impl.internal;

import ru.joke.cache.bus.core.CacheBus;
import ru.joke.cache.bus.core.CacheEntryEvent;
import ru.joke.cache.bus.core.configuration.CacheBusConfiguration;
import ru.joke.cache.bus.core.configuration.CacheBusTransportConfiguration;
import ru.joke.cache.bus.core.configuration.CacheProviderConfiguration;
import ru.joke.cache.bus.core.configuration.CacheConfigurationSource;
import ru.joke.cache.bus.core.impl.configuration.ImmutableCacheBusConfiguration;
import ru.joke.cache.bus.core.impl.internal.util.StripedRingBuffersContainer;
import ru.joke.cache.bus.core.metrics.NoOpCacheBusMetricsRegistry;
import ru.joke.cache.bus.core.state.CacheBusState;
import ru.joke.cache.bus.core.state.ComponentState;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import static ru.joke.cache.bus.core.impl.internal.AsyncMessageProcessingState.THREADS_WAITING_ON_OFFER_LABEL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

public class AsynchronousCacheEventMessageConsumerTest {

    @Test
    public void testAsyncConsumingWhenBufferNonFull() throws InterruptedException {
        // preparation
        final TestCacheBus cacheBus = new TestCacheBus();
        final int stripes = 5, messageCount = 100;
        final StripedRingBuffersContainer<byte[]> buffersContainer = new StripedRingBuffersContainer<>(stripes);
        final int threadsCount = buffersContainer.size();
        final var processingPool = Executors.newFixedThreadPool(threadsCount);
        final var consumer = new AsynchronousCacheEventMessageConsumer(cacheBus, new NoOpCacheBusMetricsRegistry(), buffersContainer, processingPool);
        try (processingPool; consumer) {

            // action
            final byte[] message = new byte[]{1, 2, 3};
            for (int i = 0; i < messageCount; i++) {
                consumer.accept(i, message);
            }

            Thread.sleep(Duration.ofMillis(100));
        }

        // checks
        assertEquals(ComponentState.Status.DOWN, consumer.state().status(), "Component must be in DOWN state");
        assertFalse(consumer.state().componentId().isBlank(), "Component id must be not blank");

        final int messagesByThreadCount = messageCount / threadsCount;
        assertEquals(threadsCount, cacheBus.eventsByThread.size(), "Events must be consumed in " + threadsCount + " threads");
        for (int i = 0; i < threadsCount; i++) {
            cacheBus.eventsByThread.values().forEach(messagesByThread -> assertEquals(messagesByThreadCount, messagesByThread.size(), "Messages must be divided into threads evenly"));
        }
    }

    @Test
    public void testStateOfAsyncConsumerWhenBufferIsFull() {
        // preparation
        final TestCacheBus cacheBus = new TestCacheBus();
        final int stripes = 5, messageCount = 200;
        final StripedRingBuffersContainer<byte[]> buffersContainer = new StripedRingBuffersContainer<>(stripes, 2);
        final int threadsCount = buffersContainer.size();
        final var processingPool = Executors.newFixedThreadPool(threadsCount);
        final var consumer = new AsynchronousCacheEventMessageConsumer(cacheBus, new NoOpCacheBusMetricsRegistry(), buffersContainer, processingPool);
        try (processingPool; consumer) {

            // action
            final byte[] message = new byte[]{1, 2, 3};
            for (int i = 0; i < messageCount; i++) {
                consumer.accept(i, message);
            }

            // checks
            assertEquals(ComponentState.Status.UP_OK, consumer.state().status(), "Component must be in OK state");
            assertEquals(THREADS_WAITING_ON_OFFER_LABEL, consumer.state().severities().get(0).asString(), "Severities must contain info about waiting queues of threads");
        }

        // checks
        assertEquals(ComponentState.Status.DOWN, consumer.state().status(), "Component must be in DOWN state");
    }

    static class TestCacheBus implements CacheBus {

        private final Map<String, List<byte[]>> eventsByThread = new ConcurrentHashMap<>();
        private final CacheBusConfiguration configuration =
                ImmutableCacheBusConfiguration
                        .builder()
                            .setProviderConfiguration(mock(CacheProviderConfiguration.class))
                            .setCacheConfigurationSource(mock(CacheConfigurationSource.class))
                            .setTransportConfiguration(mock(CacheBusTransportConfiguration.class))
                        .build();

        @Override
        public <K extends Serializable, V extends Serializable> void send(@Nonnull CacheEntryEvent<K, V> event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void receive(@Nonnull byte[] binaryEventData) {
            this.eventsByThread.computeIfAbsent(Thread.currentThread().getName(), t -> new ArrayList<>()).add(binaryEventData);
        }

        @Override
        public void withConfiguration(@Nonnull CacheBusConfiguration configuration) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CacheBusConfiguration configuration() {
            return configuration;
        }

        @Nonnull
        @Override
        public CacheBusState state() {
            throw new UnsupportedOperationException();
        }
    }
}
