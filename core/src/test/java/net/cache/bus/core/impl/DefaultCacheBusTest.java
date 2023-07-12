package net.cache.bus.core.impl;

import net.cache.bus.core.*;
import net.cache.bus.core.configuration.*;
import net.cache.bus.core.impl.configuration.CacheProviderConfigurationTemplate;
import net.cache.bus.core.impl.configuration.ImmutableCacheBusConfiguration;
import net.cache.bus.core.impl.configuration.ImmutableCacheBusTransportConfiguration;
import net.cache.bus.core.impl.configuration.ImmutableCacheConfiguration;
import net.cache.bus.core.impl.internal.ImmutableCacheEntryOutputMessage;
import net.cache.bus.core.impl.test.FakeCache;
import net.cache.bus.core.impl.test.FakeCacheBusMessageChannel;
import net.cache.bus.core.impl.test.FakeCacheManager;
import net.cache.bus.core.transport.CacheEntryEventConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DefaultCacheBusTest {

    private static final String INV_CACHE = "testInv";
    private static final String INV_CACHE_ALIAS = "testInv_1";
    private static final String REPL_CACHE = "testRepl";

    @Mock
    private CacheBusMessageChannelConfiguration messageChannelConfiguration;
    @Mock
    private CacheEntryEventConverter eventConverter;
    @Mock
    private CacheEventListener<String, String> eventListener;

    private CacheBusConfiguration configuration;

    @BeforeEach
    public void preparation() {
        this.configuration = createCacheBusConfiguration();
    }

    @Test
    @Order(1)
    public void testNotStartedBus() {
        // preparation
        final ExtendedCacheBus cacheBus = new DefaultCacheBus(configuration);

        //checks
        assertThrows(LifecycleException.class, cacheBus::getConfiguration, "Configuration available only after start");
        assertDoesNotThrow(() -> cacheBus.send(mock(CacheEntryEvent.class)), "Send should not be happen");
        assertDoesNotThrow(() -> cacheBus.receive(new byte[0]), "Receive should not be happen");
        assertThrows(ConfigurationException.class, () -> cacheBus.setConfiguration(configuration), "Default implementation configurable only via constructor");
        assertThrows(LifecycleException.class, cacheBus::stop, "Stop available only for started bus");
    }

    @Test
    @Order(1)
    public void testStartOfBus() {
        // preparation
        final ExtendedCacheBus cacheBus = new DefaultCacheBus(configuration);

        // action
        cacheBus.start();

        // checks
        final CacheManager cacheManager = configuration.providerConfiguration().cacheManager();
        @SuppressWarnings("unchecked")
        final FakeCache<String, String> cache = cacheManager.getCache(INV_CACHE)
                                                            .map(FakeCache.class::cast)
                                                            .orElseThrow();
        assertEquals(this.eventListener, cache.getRegisteredEventListener(), "Event listener must be registered");
        final FakeCacheBusMessageChannel channel = (FakeCacheBusMessageChannel) configuration.transportConfiguration().messageChannel();
        assertNotNull(channel.getConfiguration(), "Channel must be activated (configuration will be set when activation be happen)");
        assertNotNull(channel.getConsumer(), "Subscription must be called");

        // clearing
        cacheBus.stop();
        assertTrue(channel.isUnsubscribeCalled(), "Unsubscribe must be called for channel");
    }

    @Test
    @Order(2)
    public void testSendingOfEventsByBus() {
        // preparation
        final ExtendedCacheBus cacheBus = new DefaultCacheBus(configuration);
        final byte[] binaryEventValue = new byte[] {2, 32};
        when(eventConverter.toBinary(any(), anyBoolean())).thenReturn(binaryEventValue);

        // action
        cacheBus.start();

        final var event1 = new ImmutableCacheEntryEvent<>("1", "v1", "v2", CacheEntryEventType.UPDATED, INV_CACHE);
        cacheBus.send(event1);

        final var event2 = new ImmutableCacheEntryEvent<>("2", "v2", null, CacheEntryEventType.EXPIRED, INV_CACHE);
        cacheBus.send(event2);

        final var event3 = new ImmutableCacheEntryEvent<>("3", null, "v3", CacheEntryEventType.ADDED, INV_CACHE);
        cacheBus.send(event3);

        final var event4 = new ImmutableCacheEntryEvent<>("4", "v4", null, CacheEntryEventType.EVICTED, INV_CACHE);
        cacheBus.send(event4);

        final var event5 = new ImmutableCacheEntryEvent<>("unk", "unk-1", "unk-2", CacheEntryEventType.UPDATED, "unknownCache");
        cacheBus.send(event5);

        final var event6 = new ImmutableCacheEntryEvent<>("1", null, "v1", CacheEntryEventType.ADDED, REPL_CACHE);
        cacheBus.send(event6);

        final var event7 = new ImmutableCacheEntryEvent<>("2", "v2", "v3", CacheEntryEventType.UPDATED, REPL_CACHE);
        cacheBus.send(event7);

        final var event8 = new ImmutableCacheEntryEvent<>("3", "v3", null, CacheEntryEventType.EVICTED, REPL_CACHE);
        cacheBus.send(event8);

        final var event9 = new ImmutableCacheEntryEvent<>("4", "v4", null, CacheEntryEventType.EXPIRED, REPL_CACHE);
        cacheBus.send(event9);

        // checks
        final FakeCacheBusMessageChannel channel = (FakeCacheBusMessageChannel) configuration.transportConfiguration().messageChannel();
        assertNotNull(channel.getConfiguration(), "Channel must be activated (configuration will be set when activation be happen)");
        assertEquals(6, channel.getMessages().size(), "Only two messages should be sent");
        assertEquals(
                new ImmutableCacheEntryOutputMessage(event1, binaryEventValue),
                channel.getMessages().get(0),
                "Update event must be sent to invalidation cache"
        );

        assertEquals(
                new ImmutableCacheEntryOutputMessage(event4, binaryEventValue),
                channel.getMessages().get(1),
                "Eviction event must be sent to invalidation cache"
        );

        assertEquals(
                new ImmutableCacheEntryOutputMessage(event6, binaryEventValue),
                channel.getMessages().get(2),
                "Add event must be sent to replication cache"
        );

        assertEquals(
                new ImmutableCacheEntryOutputMessage(event7, binaryEventValue),
                channel.getMessages().get(3),
                "Update event must be sent to replication cache"
        );

        assertEquals(
                new ImmutableCacheEntryOutputMessage(event8, binaryEventValue),
                channel.getMessages().get(4),
                "Eviction event must be sent to replication cache"
        );

        assertEquals(
                new ImmutableCacheEntryOutputMessage(event9, binaryEventValue),
                channel.getMessages().get(5),
                "Expiration event must be sent to replication cache"
        );

        // clearing
        cacheBus.stop();
        assertTrue(channel.isUnsubscribeCalled(), "Unsubscribe must be called for channel");
    }

    @Test
    @Order(2)
    public void testReceivingOfEventsByBus() {
        // preparation
        final ExtendedCacheBus cacheBus = new DefaultCacheBus(configuration);

        final byte[] binaryEventValue1 = new byte[] {2, 32};
        final CacheEntryEvent<Serializable, Serializable> event1 = new ImmutableCacheEntryEvent<>("1", "v1", "v2", CacheEntryEventType.UPDATED, INV_CACHE);
        when(eventConverter.fromBinary(binaryEventValue1)).thenReturn(event1);

        final byte[] binaryEventValue2 = new byte[] {3, 42};
        final CacheEntryEvent<Serializable, Serializable> event2 = new ImmutableCacheEntryEvent<>("2", null, null, CacheEntryEventType.EVICTED, INV_CACHE);
        when(eventConverter.fromBinary(binaryEventValue2)).thenReturn(event2);

        final byte[] binaryEventValue3 = new byte[] {4, 42};
        final CacheEntryEvent<Serializable, Serializable> event3 = new ImmutableCacheEntryEvent<>("1", null, null, CacheEntryEventType.EVICTED, REPL_CACHE);
        when(eventConverter.fromBinary(binaryEventValue3)).thenReturn(event3);

        final byte[] binaryEventValue4 = new byte[] {5, 42};
        final CacheEntryEvent<Serializable, Serializable> event4 = new ImmutableCacheEntryEvent<>("2", null, "v2", CacheEntryEventType.ADDED, REPL_CACHE);
        when(eventConverter.fromBinary(binaryEventValue4)).thenReturn(event4);

        final byte[] binaryEventValue5 = new byte[] {6, 42};
        final CacheEntryEvent<Serializable, Serializable> event5 = new ImmutableCacheEntryEvent<>("3", "v3", "v4", CacheEntryEventType.UPDATED, REPL_CACHE);
        when(eventConverter.fromBinary(binaryEventValue5)).thenReturn(event5);

        final byte[] binaryEventValue6 = new byte[] {9, 32};
        final CacheEntryEvent<Serializable, Serializable> event6 = new ImmutableCacheEntryEvent<>("6", "v6", "v7", CacheEntryEventType.UPDATED, INV_CACHE_ALIAS);
        when(eventConverter.fromBinary(binaryEventValue6)).thenReturn(event6);

        // action
        cacheBus.start();

        cacheBus.receive(binaryEventValue1);
        cacheBus.receive(binaryEventValue2);
        cacheBus.receive(binaryEventValue3);
        cacheBus.receive(binaryEventValue4);
        cacheBus.receive(binaryEventValue5);
        cacheBus.receive(binaryEventValue6);

        // checks
        final CacheManager cacheManager = configuration.providerConfiguration().cacheManager();
        @SuppressWarnings("unchecked")
        final FakeCache<Serializable, Serializable> invCache = cacheManager.getCache(INV_CACHE)
                                                                            .map(FakeCache.class::cast)
                                                                            .orElseThrow();
        assertTrue(invCache.get(event1.key()).isEmpty(), "Value must be evicted after applying any event to invalidation cache");
        assertTrue(invCache.get(event2.key()).isEmpty(), "Value must be evicted after applying any event to invalidation cache");
        assertTrue(invCache.get(event6.key()).isEmpty(), "Value must be evicted after applying any event to invalidation cache by alias");

        @SuppressWarnings("unchecked")
        final FakeCache<Serializable, Serializable> replCache = cacheManager.getCache(REPL_CACHE)
                                                                            .map(FakeCache.class::cast)
                                                                            .orElseThrow();
        assertTrue(replCache.get(event3.key()).isEmpty(), "Value must be evicted after applying eviction event to replicated cache");
        assertTrue(replCache.get(event4.key()).filter(v -> v.equals(event4.newValue())).isPresent(), "Value must be added after applying added event to replicated cache");
        assertTrue(replCache.get(event5.key()).isEmpty(), "Value must be evicted after applying update event with not matching old value to replicated cache");

        // clearing
        cacheBus.stop();
    }

    @AfterEach
    public void tearDown() {
        this.configuration.transportConfiguration().processingPool().close();
    }

    private CacheBusConfiguration createCacheBusConfiguration() {
        final FakeCache<String, String> cache1 = new FakeCache<>(DefaultCacheBusTest.INV_CACHE);
        cache1.put("1", "v1");
        cache1.put("2", "v2");
        cache1.put("3", "v3");
        cache1.put("6", "v3");

        final FakeCache<String, String> cache2 = new FakeCache<>(DefaultCacheBusTest.REPL_CACHE);
        cache2.put("1", "v1");
        cache2.put("3", "v5");

        final CacheBusTransportConfiguration transportConfiguration =
                ImmutableCacheBusTransportConfiguration
                        .builder()
                            .setMaxConcurrentReceivingThreads(1)
                            .setMaxProcessingThreadBufferCapacity(10)
                            .setProcessingPool(Executors.newSingleThreadExecutor())
                            .setMessageChannel(new FakeCacheBusMessageChannel())
                            .setMessageChannelConfiguration(this.messageChannelConfiguration)
                            .setConverter(this.eventConverter)
                        .build();
        final FakeCacheManager cacheManager = new FakeCacheManager(Map.of(cache1.getName(), cache1, cache2.getName(), cache2));
        final CacheEventListenerRegistrar eventListenerRegistrar = new CacheEventListenerRegistrar() {
            @Override
            public <K extends Serializable, V extends Serializable> void registerFor(@Nonnull CacheBus cacheBus, @Nonnull Cache<K, V> cache) {
                @SuppressWarnings("unchecked")
                final CacheEventListener<K, V> listener = (CacheEventListener<K, V>) eventListener;
                cache.registerEventListener(listener);
            }
        };
        final CacheProviderConfiguration providerConfiguration = new CacheProviderConfigurationTemplate(cacheManager, eventListenerRegistrar) {};
        return ImmutableCacheBusConfiguration
                    .builder()
                        .setCacheConfigurationBuilder(
                                CacheConfigurationSource.createDefault()
                                        .add(new ImmutableCacheConfiguration(INV_CACHE, CacheType.INVALIDATED, Set.of(INV_CACHE_ALIAS)))
                                        .add(new ImmutableCacheConfiguration(REPL_CACHE, CacheType.REPLICATED))
                        )
                        .setProviderConfiguration(providerConfiguration)
                        .setTransportConfiguration(transportConfiguration)
                    .build();
    }
}
