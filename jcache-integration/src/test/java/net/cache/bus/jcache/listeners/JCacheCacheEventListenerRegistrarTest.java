package net.cache.bus.jcache.listeners;

import net.cache.bus.core.CacheBus;
import net.cache.bus.jcache.adapters.JCacheCacheAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.cache.Cache;
import javax.cache.configuration.CacheEntryListenerConfiguration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
public class JCacheCacheEventListenerRegistrarTest {

    @Mock
    private CacheBus cacheBus;
    @Mock
    private Cache<String, String> cache;
    @Captor
    private ArgumentCaptor<CacheEntryListenerConfiguration<String, String>> captor;

    @Test
    public void testRegistration() {
        final JCacheCacheEventListenerRegistrar registrar = new JCacheCacheEventListenerRegistrar();
        final JCacheCacheAdapter<String, String> cacheAdapter = new JCacheCacheAdapter<>(this.cache);
        doNothing().when(this.cache).registerCacheEntryListener(captor.capture());

        registrar.registerFor(this.cacheBus, cacheAdapter);

        assertNotNull(captor.getValue(), "Listener must be not null");
        assertTrue(captor.getValue().isOldValueRequired(), "Listener must be configured to save old value for event");
        assertTrue(captor.getValue().isSynchronous(), "Listener must be configured as sync");
        assertNotNull(captor.getValue().getCacheEntryEventFilterFactory(), "Event filter factory must be not null");

        final var listenerFactory = captor.getValue().getCacheEntryListenerFactory();
        assertNotNull(listenerFactory, "Event listener factory must be not null");
        final var listener = listenerFactory.create();
        assertNotNull(listener, "Event listener must be not null");
        assertEquals(JCacheCacheEntryEventListener.class, listener.getClass(), "Event listener must be not null");
    }
}
