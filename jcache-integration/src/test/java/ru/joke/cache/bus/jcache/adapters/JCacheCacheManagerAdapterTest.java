package ru.joke.cache.bus.jcache.adapters;

import ru.joke.cache.bus.core.CacheManager;
import ru.joke.cache.bus.core.state.ComponentState;
import ru.joke.cache.bus.core.testing.BaseCacheManagerTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import javax.cache.Cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

public class JCacheCacheManagerAdapterTest extends BaseCacheManagerTest {

    @Mock
    private javax.cache.CacheManager cacheManager;
    @Mock
    private Cache<Object, Object> cache;

    @Test
    public void testStateConversion() {
        final CacheManager cacheManagerAdapter = configureCacheManager(CACHE);
        when(this.cacheManager.isClosed()).thenReturn(false, true);

        assertEquals(ComponentState.Status.DOWN, cacheManagerAdapter.state().status(), "When JCache CacheManager is closed then bus state must be DOWN");
        assertEquals(ComponentState.Status.UP_OK, cacheManagerAdapter.state().status(), "When JCache CacheManager is closed then bus state must be UP");
    }

    @Override
    protected CacheManager configureCacheManager(String cacheName) {
        lenient().when(this.cacheManager.getCache(cacheName)).thenReturn(this.cache);

        return new JCacheCacheManagerAdapter(this.cacheManager);
    }

    @Override
    protected Class<?> getOriginalCacheManagerClass() {
        return javax.cache.CacheManager.class;
    }
}
