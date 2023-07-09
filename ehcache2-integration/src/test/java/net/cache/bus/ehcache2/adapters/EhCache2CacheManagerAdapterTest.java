package net.cache.bus.ehcache2.adapters;

import net.cache.bus.core.CacheManager;
import net.cache.bus.core.testing.BaseCacheManagerTest;
import net.sf.ehcache.Cache;
import org.mockito.Mock;

import static org.mockito.Mockito.lenient;

public class EhCache2CacheManagerAdapterTest extends BaseCacheManagerTest {

    @Mock
    private net.sf.ehcache.CacheManager cacheManager;
    @Mock
    private Cache cache;

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
