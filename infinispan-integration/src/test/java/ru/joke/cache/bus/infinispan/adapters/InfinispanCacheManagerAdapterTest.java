package ru.joke.cache.bus.infinispan.adapters;

import ru.joke.cache.bus.core.CacheManager;
import ru.joke.cache.bus.core.state.ComponentState;
import ru.joke.cache.bus.core.testing.BaseCacheManagerTest;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ClusteringConfiguration;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.EmbeddedCacheManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

public class InfinispanCacheManagerAdapterTest extends BaseCacheManagerTest {

    @Mock
    private EmbeddedCacheManager infinispanCacheManager;
    @Mock
    private Cache<Object, Object> cache;
    @Mock
    private Configuration configuration;
    @Mock
    private ClusteringConfiguration clusteringConfiguration;

    @Test
    public void testStateConversion() {
        final CacheManager cacheManagerAdapter = configureCacheManager(CACHE);
        when(this.infinispanCacheManager.getStatus())
                .thenReturn(ComponentStatus.INITIALIZING, ComponentStatus.FAILED,
                        ComponentStatus.INSTANTIATED, ComponentStatus.STOPPING,
                        ComponentStatus.RUNNING, ComponentStatus.TERMINATED);

        assertEquals(ComponentState.Status.UP_NOT_READY, cacheManagerAdapter.state().status(), "When Infinispan status is INITIALIZING then bus state must be UP_NOT_READY");
        assertEquals(ComponentState.Status.UP_FATAL_BROKEN, cacheManagerAdapter.state().status(), "When Infinispan status is FAILED then bus state must be UP_FATAL_BROKEN");
        assertEquals(ComponentState.Status.DOWN, cacheManagerAdapter.state().status(), "When Infinispan status is INSTANTIATED then bus state must be DOWN");
        assertEquals(ComponentState.Status.DOWN, cacheManagerAdapter.state().status(), "When Infinispan status is STOPPING then bus state must be DOWN");
        assertEquals(ComponentState.Status.UP_OK, cacheManagerAdapter.state().status(), "When Infinispan status is RUNNING then bus state must be UP_OK");
        assertEquals(ComponentState.Status.DOWN, cacheManagerAdapter.state().status(), "When Infinispan status is TERMINATED then bus state must be DOWN");

    }

    @Override
    protected CacheManager configureCacheManager(String cacheName) {
        lenient().when(this.clusteringConfiguration.cacheMode()).thenReturn(CacheMode.LOCAL);
        lenient().when(this.configuration.clustering()).thenReturn(this.clusteringConfiguration);
        lenient().when(cache.getCacheConfiguration()).thenReturn(this.configuration);

        lenient().when(this.infinispanCacheManager.getCache(cacheName, false)).thenReturn(this.cache);

        return new InfinispanCacheManagerAdapter(this.infinispanCacheManager);
    }

    @Override
    protected Class<?> getOriginalCacheManagerClass() {
        return EmbeddedCacheManager.class;
    }
}
