package ru.joke.cache.bus.ehcache2.adapters;

import ru.joke.cache.bus.core.CacheManager;
import ru.joke.cache.bus.core.state.ComponentState;
import ru.joke.cache.bus.core.testing.BaseCacheManagerTest;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Status;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

public class EhCache2CacheManagerAdapterTest extends BaseCacheManagerTest {

    @Mock
    private net.sf.ehcache.CacheManager cacheManager;
    @Mock
    private Cache cache;

    @Test
    public void testStateConversion() {
        final CacheManager cacheManagerAdapter = configureCacheManager(CACHE);
        when(this.cacheManager.getStatus()).thenReturn(Status.STATUS_UNINITIALISED, Status.STATUS_ALIVE, Status.STATUS_SHUTDOWN);

        assertEquals(ComponentState.Status.DOWN, cacheManagerAdapter.state().status(), "When EhCache status is STATUS_UNINITIALISED then bus state must be DOWN");
        assertEquals(ComponentState.Status.UP_OK, cacheManagerAdapter.state().status(), "When EhCache status is STATUS_ALIVE then bus state must be UP_OK");
        assertEquals(ComponentState.Status.DOWN, cacheManagerAdapter.state().status(), "When EhCache status is STATUS_SHUTDOWN then bus state must be DOWN");
    }

    @Override
    protected CacheManager configureCacheManager(String cacheName) {
        lenient().when(this.cacheManager.getCache(cacheName)).thenReturn(this.cache);

        return new EhCache2CacheManagerAdapter(this.cacheManager);
    }

    @Override
    protected Class<?> getOriginalCacheManagerClass() {
        return net.sf.ehcache.CacheManager.class;
    }
}
