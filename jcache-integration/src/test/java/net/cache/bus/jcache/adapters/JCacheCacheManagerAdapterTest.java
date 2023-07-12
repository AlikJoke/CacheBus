package net.cache.bus.jcache.adapters;

import net.cache.bus.core.CacheManager;
import net.cache.bus.core.testing.BaseCacheManagerTest;
import org.mockito.Mock;

import javax.cache.Cache;

import static org.mockito.Mockito.lenient;

public class JCacheCacheManagerAdapterTest extends BaseCacheManagerTest {

    @Mock
    private javax.cache.CacheManager cacheManager;
    @Mock
    private Cache<Object, Object> cache;

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
