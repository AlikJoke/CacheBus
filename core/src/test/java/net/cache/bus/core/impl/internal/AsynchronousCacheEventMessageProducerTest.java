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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    public void testAsyncProducing() throws InterruptedException {

        final int messageCount = 100;
        final CacheBusTransportConfiguration transportConfiguration = createTransportConfiguration();
        final StripedRingBuffersContainer<CacheEntryEvent<?, ?>> buffersContainer = new StripedRingBuffersContainer<>(transportConfiguration.maxAsyncSendingThreads(), 32);

        final CacheConfiguration cacheConfiguration = new ImmutableCacheConfiguration(CACHE_NAME, CacheType.INVALIDATED);
        final Map<String, CacheConfiguration> cacheConfigurations = Map.of(CACHE_NAME, cacheConfiguration);
        final FakeCacheBusMessageChannelByThreads messageChannel = (FakeCacheBusMessageChannelByThreads) transportConfiguration.messageChannel();

        when(this.eventConverter.toBinary(any(), eq(cacheConfiguration.cacheType().serializeValueFields()))).thenReturn(new byte[] {2, 3});

        try (final var ignored1 = transportConfiguration.processingPool();
             final var ignored2 = transportConfiguration.asyncSendingPool();
             final var producer = new AsynchronousCacheEventMessageProducer(transportConfiguration, cacheConfigurations, buffersContainer)) {

            // action
            for (int i = 0; i < messageCount; i++) {
                final CacheEntryEvent<String, String> event = new ImmutableCacheEntryEvent<>(String.valueOf(i), null, "v1", CacheEntryEventType.ADDED, CACHE_NAME);
                producer.produce(cacheConfiguration, event);
            }

            Thread.sleep(Duration.ofMillis(10));
        }

        // checks
        assertEquals(buffersContainer.size(), messageChannel.messagesByThread.size(), "Events must be produced in " + buffersContainer.size() + " threads");
        for (int i = 0; i < buffersContainer.size(); i++) {
            messageChannel.messagesByThread.values().forEach(messagesByThread -> assertTrue(messagesByThread.size() > 0, "Messages must be divided into threads"));
        }
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
