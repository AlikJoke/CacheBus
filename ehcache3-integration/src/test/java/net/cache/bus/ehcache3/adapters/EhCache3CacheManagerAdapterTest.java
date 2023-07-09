package net.cache.bus.ehcache3.adapters;

import net.cache.bus.core.CacheManager;
import net.cache.bus.core.testing.BaseCacheManagerTest;
import org.ehcache.Cache;
import org.ehcache.config.CacheRuntimeConfiguration;
import org.ehcache.config.Configuration;
import org.ehcache.core.EhcacheManager;
import org.mockito.Mock;

import java.util.Collections;

import static org.mockito.Mockito.lenient;

public class EhCache3CacheManagerAdapterTest extends BaseCacheManagerTest {

    @Mock
    private EhcacheManager cacheManager;
    @Mock
    private Cache<String, String> cache;
    @Mock
    private Configuration runtimeConfiguration;
    @Mock
    private CacheRuntimeConfiguration<String, String> cacheRuntimeConfiguration;

    @Override
    protected CacheManager configureCacheManager(String cacheName) {
        lenient().when(this.cacheManager.getCache(cacheName, String.class, String.class)).thenReturn(this.cache);
        lenient().when(this.cacheManager.getRuntimeConfiguration()).thenReturn(this.runtimeConfiguration);
        lenient().when(this.runtimeConfiguration.getCacheConfigurations()).thenReturn(Collections.singletonMap(cacheName, this.cacheRuntimeConfiguration));
        lenient().when(this.cacheRuntimeConfiguration.getValueType()).thenReturn(String.class);
        lenient().when(this.cacheRuntimeConfiguration.getKeyType()).thenReturn(String.class);

        return new EhCache3CacheManagerAdapter(this.cacheManager);
    }

    @Override
    protected Class<?> getOriginalCacheManagerClass() {
        return EhcacheManager.class;
    }
}
