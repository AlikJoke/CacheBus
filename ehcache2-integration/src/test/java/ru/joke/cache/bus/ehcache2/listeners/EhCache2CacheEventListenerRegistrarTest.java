package ru.joke.cache.bus.ehcache2.listeners;

import ru.joke.cache.bus.core.CacheBus;
import ru.joke.cache.bus.ehcache2.adapters.EhCache2CacheAdapter;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.NotificationScope;
import net.sf.ehcache.event.RegisteredEventListeners;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EhCache2CacheEventListenerRegistrarTest {

    @Mock
    private CacheBus cacheBus;
    @Mock
    private Ehcache cache;
    @Mock
    private RegisteredEventListeners registeredEventListeners;
    @Captor
    private ArgumentCaptor<CacheEventListener> listenerCaptor;
    @Captor
    private ArgumentCaptor<NotificationScope> scopeCaptor;

    @Test
    public void testRegistration() {
        final EhCache2CacheEventListenerRegistrar registrar = new EhCache2CacheEventListenerRegistrar();
        final EhCache2CacheAdapter<String, String> cacheAdapter = new EhCache2CacheAdapter<>(this.cache);

        when(this.cache.getCacheEventNotificationService()).thenReturn(this.registeredEventListeners);
        when(this.registeredEventListeners.registerListener(
                listenerCaptor.capture(),
                scopeCaptor.capture()
        )).thenReturn(true);

        registrar.registerFor(this.cacheBus, cacheAdapter);

        assertNotNull(listenerCaptor.getValue(), "Listener must be not null");
        assertEquals(NotificationScope.LOCAL, scopeCaptor.getValue(), "Scope must be local");
    }

    @Test
    public void testRemoveRegistration() {
        final EhCache2CacheEventListenerRegistrar registrar = new EhCache2CacheEventListenerRegistrar();
        final EhCache2CacheAdapter<String, String> cacheAdapter = new EhCache2CacheAdapter<>(this.cache);

        when(this.cache.getCacheEventNotificationService()).thenReturn(this.registeredEventListeners);
        when(this.registeredEventListeners.registerListener(
                listenerCaptor.capture(),
                scopeCaptor.capture()
        )).thenReturn(true);
        when(this.registeredEventListeners.unregisterListener(listenerCaptor.capture())).thenReturn(true);

        registrar.registerFor(this.cacheBus, cacheAdapter);
        registrar.unregisterFor(this.cacheBus, cacheAdapter);

        assertEquals(2, listenerCaptor.getAllValues().size(), "Must be captured 2 listeners");
        assertEquals(listenerCaptor.getAllValues().get(0), listenerCaptor.getAllValues().get(1), "Registered listener and unregistered must be equal");
    }
}
