package ru.joke.cache.bus.infinispan.listeners;

import ru.joke.cache.bus.core.CacheBus;
import ru.joke.cache.bus.core.CacheEventListener;
import ru.joke.cache.bus.infinispan.adapters.InfinispanCacheAdapter;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ClusteringConfiguration;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.notifications.Listener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class InfinispanCacheEventListenerRegistrarTest {

    @Mock
    private CacheBus cacheBus;
    @Mock
    private org.infinispan.Cache<String, String> cache;
    @Captor
    private ArgumentCaptor<CacheEventListener<String, String>> captor;
    @Mock
    private Configuration configuration;
    @Mock
    private ClusteringConfiguration clusteringConfiguration;

    @BeforeEach
    public void preparation() {
        lenient().when(this.cache.getCacheConfiguration()).thenReturn(this.configuration);
        lenient().when(this.configuration.clustering()).thenReturn(this.clusteringConfiguration);
        lenient().when(this.clusteringConfiguration.cacheMode()).thenReturn(CacheMode.LOCAL);
    }

    @Test
    public void testRegistration() {
        final InfinispanCacheEventListenerRegistrar registrar = new InfinispanCacheEventListenerRegistrar();
        final InfinispanCacheAdapter<String, String> cacheAdapter = new InfinispanCacheAdapter<>(this.cache);
        doNothing().when(this.cache).addListener(captor.capture());

        registrar.registerFor(this.cacheBus, cacheAdapter);

        assertNotNull(captor.getValue(), "Listener must be not null");
        assertTrue(captor.getValue().getClass().isAnnotationPresent(Listener.class), "Listener must be annotated with @Listener annotation");
    }

    @Test
    public void testRemoveRegistration() {
        final InfinispanCacheEventListenerRegistrar registrar = new InfinispanCacheEventListenerRegistrar();
        final InfinispanCacheAdapter<String, String> cacheAdapter = new InfinispanCacheAdapter<>(this.cache);
        doNothing().when(this.cache).addListener(captor.capture());
        doNothing().when(this.cache).removeListener(captor.capture());

        registrar.registerFor(this.cacheBus, cacheAdapter);
        registrar.unregisterFor(this.cacheBus, cacheAdapter);

        assertEquals(2, captor.getAllValues().size(), "Must be captured 2 listeners");
        assertEquals(captor.getAllValues().get(0), captor.getAllValues().get(1), "Registered listener and unregistered must be equal");
    }
}