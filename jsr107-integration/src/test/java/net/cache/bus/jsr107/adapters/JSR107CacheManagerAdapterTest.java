package net.cache.bus.jsr107.adapters;

import net.cache.bus.core.BaseCacheManagerTest;
import net.cache.bus.core.CacheManager;
import org.mockito.Mock;

import javax.cache.Cache;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

public class JSR107CacheManagerAdapterTest extends BaseCacheManagerTest {

    @Mock
    private javax.cache.CacheManager cacheManager;
    @Mock
    private Cache<Object, Object> cache;

    @Override
    protected CacheManager configureCacheManager(String cacheName) {
        lenient().when(this.cacheManager.getCache(cacheName)).thenReturn(this.cache);
        lenient().when(this.cacheManager.getCache("1")).thenReturn(this.cache);

        return new JSR107CacheManagerAdapter(this.cacheManager);
    }

    @Override
    protected Class<?> getOriginalCacheManagerClass() {
        return javax.cache.CacheManager.class;
    }
}
