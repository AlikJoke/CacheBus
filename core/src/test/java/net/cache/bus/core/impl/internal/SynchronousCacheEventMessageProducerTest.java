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
import net.cache.bus.core.impl.test.FakeCacheBusMessageChannel;
import net.cache.bus.core.state.ComponentState;
import net.cache.bus.core.transport.CacheEntryEventConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SynchronousCacheEventMessageProducerTest {

    private static final String CACHE_NAME = "test";

    @Mock
    private CacheBusMessageChannelConfiguration messageChannelConfiguration;
    @Mock
    private CacheEntryEventConverter eventConverter;

    @Test
    public void testSyncProducing() {
        // preparation
        final CacheBusTransportConfiguration configuration = createTransportConfiguration();
        final SynchronousCacheEventMessageProducer producer = new SynchronousCacheEventMessageProducer(configuration);

        final CacheConfiguration cacheConfiguration =
                ImmutableCacheConfiguration
                        .builder()
                            .setCacheName(CACHE_NAME)
                            .setCacheType(CacheType.INVALIDATED)
                        .build();

        final CacheEntryEvent<String, String> event = new ImmutableCacheEntryEvent<>("1", null, "v1", CacheEntryEventType.ADDED, CACHE_NAME);
        final byte[] body = new byte[] {2, 3};
        when(this.eventConverter.toBinary(event, cacheConfiguration.cacheType().serializeValueFields())).thenReturn(body);

        // action
        producer.produce(cacheConfiguration, event);

        // checks
        final FakeCacheBusMessageChannel channel = (FakeCacheBusMessageChannel) configuration.messageChannel();
        assertEquals(1, channel.getMessages().size(), "Channel must contain 1 output message");
        assertEquals(event.computeEventHashKey(), channel.getMessages().get(0).messageHashKey(), "Hash key must be equal");
        assertArrayEquals(body, channel.getMessages().get(0).cacheEntryMessageBody(), "Message must be equal");
    }

    @Test
    public void testState() {
        final CacheBusTransportConfiguration configuration = createTransportConfiguration();
        SynchronousCacheEventMessageProducer messageProducer = new SynchronousCacheEventMessageProducer(configuration);
        try (messageProducer) {
            assertEquals(ComponentState.Status.UP_OK, messageProducer.state().status(), "Status of sync message producer before closure must be UP_OK");
        }

        assertEquals(ComponentState.Status.DOWN, messageProducer.state().status(), "Status of sync message producer after closure must be DOWN");
    }

    private CacheBusTransportConfiguration createTransportConfiguration() {
        return ImmutableCacheBusTransportConfiguration
                        .builder()
                            .setMaxConcurrentReceivingThreads(1)
                            .setMaxAsyncSendingThreads(2)
                            .useAsyncSending(false)
                            .setMaxProcessingThreadBufferCapacity(10)
                            .setProcessingPool(Executors.newSingleThreadExecutor())
                            .setMessageChannel(new FakeCacheBusMessageChannel())
                            .setMessageChannelConfiguration(this.messageChannelConfiguration)
                            .setConverter(this.eventConverter)
                        .build();
    }
}
