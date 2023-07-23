package ru.joke.cache.bus.ehcache3.adapters;

import ru.joke.cache.bus.core.CacheManager;
import ru.joke.cache.bus.core.state.ComponentState;
import ru.joke.cache.bus.core.testing.BaseCacheManagerTest;
import org.ehcache.Cache;
import org.ehcache.Status;
import org.ehcache.config.CacheRuntimeConfiguration;
import org.ehcache.config.Configuration;
import org.ehcache.core.EhcacheManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

public class EhCache3CacheManagerAdapterTest extends BaseCacheManagerTest {

    @Mock
    private EhcacheManager cacheManager;
    @Mock
    private Cache<String, String> cache;
    @Mock
    private Configuration runtimeConfiguration;
    @Mock
    private CacheRuntimeConfiguration<String, String> cacheRuntimeConfiguration;

    @Test
    public void testStateConversion() {
        final CacheManager cacheManagerAdapter = configureCacheManager(CACHE);
        when(this.cacheManager.getStatus()).thenReturn(Status.UNINITIALIZED, Status.AVAILABLE, Status.MAINTENANCE);

        assertEquals(ComponentState.Status.DOWN, cacheManagerAdapter.state().status(), "When EhCache status is UNINITIALIZED then bus state must be DOWN");
        assertEquals(ComponentState.Status.UP_OK, cacheManagerAdapter.state().status(), "When EhCache status is AVAILABLE then bus state must be UP_OK");
        assertEquals(ComponentState.Status.UP_NOT_READY, cacheManagerAdapter.state().status(), "When EhCache status is MAINTENANCE then bus state must be UP_NOT_READY");
    }

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
