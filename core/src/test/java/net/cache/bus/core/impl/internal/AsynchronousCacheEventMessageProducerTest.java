package net.cache.bus.core.impl.internal;

import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.CacheEntryEventType;
import net.cache.bus.core.configuration.CacheBusMessageChannelConfiguration;
import net.cache.bus.core.configuration.CacheBusTransportConfiguration;
import net.cache.bus.core.configuration.CacheConfiguration;
import net.cache.bus.core.configuration.CacheType;
import net.cache.bus.core.impl.ImmutableCacheEntryEvent;
import net.cache.bus.core.impl.configuration.ImmutableCacheBusTransportConfiguration;
import net.cache.bus.core.impl.configuration.ImmutableCacheConfiguration;
import net.cache.bus.core.impl.internal.util.StripedRingBuffersContainer;
import net.cache.bus.core.impl.test.FakeCacheBusMessageChannel;
import net.cache.bus.core.state.ComponentState;
import net.cache.bus.core.transport.CacheEntryEventConverter;
import net.cache.bus.core.transport.CacheEntryOutputMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import static net.cache.bus.core.impl.internal.AsyncMessageProcessingState.THREADS_WAITING_ON_OFFER_LABEL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AsynchronousCacheEventMessageProducerTest {

    private static final String CACHE_NAME = "test-cache";
    private static final int ASYNC_THREADS = 5;

    @Mock
    private CacheBusMessageChannelConfiguration messageChannelConfiguration;
    @Mock
    private CacheEntryEventConverter eventConverter;

    @Test
    public void testAsyncProducingWhenBufferNonFull() throws InterruptedException {

        final int messageCount = 100;
        final CacheBusTransportConfiguration transportConfiguration = createTransportConfiguration();
        final StripedRingBuffersContainer<CacheEntryEvent<?, ?>> buffersContainer = new StripedRingBuffersContainer<>(transportConfiguration.maxAsyncSendingThreads(), 32);

        final CacheConfiguration cacheConfiguration = new ImmutableCacheConfiguration(CACHE_NAME, CacheType.INVALIDATED);
        final Map<String, CacheConfiguration> cacheConfigurations = Map.of(CACHE_NAME, cacheConfiguration);
        final FakeCacheBusMessageChannelByThreads messageChannel = (FakeCacheBusMessageChannelByThreads) transportConfiguration.messageChannel();

        when(this.eventConverter.toBinary(any(), eq(cacheConfiguration.cacheType().serializeValueFields()))).thenReturn(new byte[] {2, 3});

        final var producer = new AsynchronousCacheEventMessageProducer(transportConfiguration, cacheConfigurations, buffersContainer);
        try (final var ignored1 = transportConfiguration.processingPool();
             final var ignored2 = transportConfiguration.asyncSendingPool();
             producer) {

            // action
            for (int i = 0; i < messageCount; i++) {
                final CacheEntryEvent<String, String> event = new ImmutableCacheEntryEvent<>(String.valueOf(i), null, "v1", CacheEntryEventType.ADDED, CACHE_NAME);
                producer.produce(cacheConfiguration, event);
            }

            Thread.sleep(Duration.ofMillis(100));
        }

        assertEquals(ComponentState.Status.DOWN, producer.state().status(), "Component must be in DOWN state");
        assertFalse(producer.state().componentId().isBlank(), "Component id must be not blank");

        // checks
        assertEquals(buffersContainer.size(), messageChannel.messagesByThread.size(), "Events must be produced in " + buffersContainer.size() + " threads");
        for (int i = 0; i < buffersContainer.size(); i++) {
            messageChannel.messagesByThread.values().forEach(messagesByThread -> assertTrue(messagesByThread.size() > 0, "Messages must be divided into threads"));
        }
    }

    @Test
    public void testStateOfAsyncConsumerWhenBufferIsFull() {
        // preparation
        final int messageCount = 300;
        final CacheBusTransportConfiguration transportConfiguration = createTransportConfiguration();
        final StripedRingBuffersContainer<CacheEntryEvent<?, ?>> buffersContainer = new StripedRingBuffersContainer<>(transportConfiguration.maxAsyncSendingThreads(), 32);

        final CacheConfiguration cacheConfiguration = new ImmutableCacheConfiguration(CACHE_NAME, CacheType.INVALIDATED);
        final Map<String, CacheConfiguration> cacheConfigurations = Map.of(CACHE_NAME, cacheConfiguration);

        when(this.eventConverter.toBinary(any(), eq(cacheConfiguration.cacheType().serializeValueFields()))).thenReturn(new byte[] {2, 3});

        final var producer = new AsynchronousCacheEventMessageProducer(transportConfiguration, cacheConfigurations, buffersContainer);
        try (final var ignored1 = transportConfiguration.processingPool();
             final var ignored2 = transportConfiguration.asyncSendingPool();
             producer) {

            // action
            for (int i = 0; i < messageCount; i++) {
                final CacheEntryEvent<String, String> event = new ImmutableCacheEntryEvent<>(String.valueOf(i), null, "v1", CacheEntryEventType.ADDED, CACHE_NAME);
                producer.produce(cacheConfiguration, event);
            }

            // checks
            assertEquals(ComponentState.Status.UP_OK, producer.state().status(), "Component must be in OK state");
            assertEquals(THREADS_WAITING_ON_OFFER_LABEL, producer.state().severities().get(0).asString(), "Severities must contain info about waiting queues of threads");
        }

        assertEquals(ComponentState.Status.DOWN, producer.state().status(), "Component must be in DOWN state");
    }

    private CacheBusTransportConfiguration createTransportConfiguration() {
        return ImmutableCacheBusTransportConfiguration
                .builder()
                    .setMaxAsyncSendingThreads(ASYNC_THREADS)
                    .setMaxProcessingThreadBufferCapacity(32)
                    .setAsyncSendingPool(Executors.newFixedThreadPool(ASYNC_THREADS))
                    .useAsyncSending(true)
                    .setProcessingPool(Executors.newSingleThreadExecutor())
                    .setMessageChannel(new FakeCacheBusMessageChannelByThreads())
                    .setMessageChannelConfiguration(this.messageChannelConfiguration)
                    .setConverter(this.eventConverter)
                .build();
    }

    static class FakeCacheBusMessageChannelByThreads extends FakeCacheBusMessageChannel {

        private final Map<String, List<CacheEntryOutputMessage>> messagesByThread = new ConcurrentHashMap<>();

        @Override
        public void send(@Nonnull CacheEntryOutputMessage eventOutputMessage) {
            messagesByThread.computeIfAbsent(Thread.currentThread().getName(), k -> new ArrayList<>()).add(eventOutputMessage);
        }
    }
}
