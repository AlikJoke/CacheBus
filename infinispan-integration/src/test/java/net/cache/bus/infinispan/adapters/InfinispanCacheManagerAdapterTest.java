package net.cache.bus.infinispan.adapters;

import net.cache.bus.core.CacheManager;
import net.cache.bus.core.testing.BaseCacheManagerTest;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ClusteringConfiguration;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.manager.EmbeddedCacheManager;
import org.mockito.Mock;

import static org.mockito.Mockito.lenient;

public class InfinispanCacheManagerAdapterTest extends BaseCacheManagerTest {

    @Mock
    private EmbeddedCacheManager infinispanCacheManager;
    @Mock
    private Cache<Object, Object> cache;
    @Mock
    private Configuration configuration;
    @Mock
    private ClusteringConfiguration clusteringConfiguration;

    @Override
    protected CacheManager configureCacheManager(String cacheName) {
        lenient().when(this.clusteringConfiguration.cacheMode()).thenReturn(CacheMode.LOCAL);
        lenient().when(this.configuration.clustering()).thenReturn(this.clusteringConfiguration);
        lenient().when(cache.getCacheConfiguration()).thenReturn(this.configuration);

        lenient().when(this.infinispanCacheManager.getCache(cacheName, false)).thenReturn(this.cache);
        lenient().when(this.infinispanCacheManager.getCache("1", false)).thenReturn(this.cache);

        return new InfinispanCacheManagerAdapter(this.infinispanCacheManager);
    }

    @Override
    protected Class<?> getOriginalCacheManagerClass() {
        return EmbeddedCacheManager.class;
    }
}
