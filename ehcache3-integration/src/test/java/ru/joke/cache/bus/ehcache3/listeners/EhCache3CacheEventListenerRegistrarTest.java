package ru.joke.cache.bus.ehcache3.listeners;

import ru.joke.cache.bus.core.CacheBus;
import ru.joke.cache.bus.ehcache3.adapters.EhCache3CacheAdapter;
import org.ehcache.Cache;
import org.ehcache.config.CacheRuntimeConfiguration;
import org.ehcache.event.CacheEventListener;
import org.ehcache.event.EventFiring;
import org.ehcache.event.EventOrdering;
import org.ehcache.event.EventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EhCache3CacheEventListenerRegistrarTest {

    @Mock
    private CacheBus cacheBus;
    @Mock
    private Cache<String, String> cache;
    @Mock
    private CacheRuntimeConfiguration<String, String> cacheConfiguration;
    @Captor
    private ArgumentCaptor<CacheEventListener<String, String>> listenerCaptor;
    @Captor
    private ArgumentCaptor<EventFiring> firingCaptor;
    @Captor
    private ArgumentCaptor<EventOrdering> orderingCaptor;
    @Captor
    private ArgumentCaptor<Set<EventType>> eventTypesCaptor;

    @Test
    public void testRegistration() {
        final EhCache3CacheEventListenerRegistrar registrar = new EhCache3CacheEventListenerRegistrar();
        final EhCache3CacheAdapter<String, String> cacheAdapter = new EhCache3CacheAdapter<>(this.cache, "test");

        when(this.cache.getRuntimeConfiguration()).thenReturn(this.cacheConfiguration);
        doNothing().when(this.cacheConfiguration).registerCacheEventListener(
                listenerCaptor.capture(),
                orderingCaptor.capture(),
                firingCaptor.capture(),
                eventTypesCaptor.capture()
        );

        registrar.registerFor(this.cacheBus, cacheAdapter);

        assertNotNull(listenerCaptor.getValue(), "Listener must be not null");
        assertEquals(EventOrdering.ORDERED, orderingCaptor.getValue(), "Events must be ordered");
        assertEquals(EventFiring.SYNCHRONOUS, firingCaptor.getValue(), "Events listening must be synchronous");
        assertEquals(Set.of(EventType.values()), eventTypesCaptor.getValue(), "Events types to listen must be equals");
    }

    @Test
    public void testRemoveRegistration() {
        final EhCache3CacheEventListenerRegistrar registrar = new EhCache3CacheEventListenerRegistrar();
        final EhCache3CacheAdapter<String, String> cacheAdapter = new EhCache3CacheAdapter<>(this.cache, "test");

        when(this.cache.getRuntimeConfiguration()).thenReturn(this.cacheConfiguration);
        doNothing().when(this.cacheConfiguration).registerCacheEventListener(
                listenerCaptor.capture(),
                orderingCaptor.capture(),
                firingCaptor.capture(),
                eventTypesCaptor.capture()
        );
        doNothing().when(this.cacheConfiguration).deregisterCacheEventListener(listenerCaptor.capture());

        registrar.registerFor(this.cacheBus, cacheAdapter);
        registrar.unregisterFor(this.cacheBus, cacheAdapter);

        assertEquals(2, listenerCaptor.getAllValues().size(), "Must be captured 2 listeners");
        assertEquals(listenerCaptor.getAllValues().get(0), listenerCaptor.getAllValues().get(1), "Registered listener and unregistered must be equal");
    }
}
