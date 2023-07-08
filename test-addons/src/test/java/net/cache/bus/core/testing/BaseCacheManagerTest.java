package net.cache.bus.core.testing;

import net.cache.bus.core.Cache;
import net.cache.bus.core.CacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public abstract class BaseCacheManagerTest {

    protected static final String CACHE = "test";

    private CacheManager cacheManager;

    @BeforeEach
    public void preparation() {
        this.cacheManager = configureCacheManager(CACHE);
    }

    @Test
    public void testWhenGetExistingCacheByNameThenResultNotEmpty() {
        assertTrue(cacheManager.getCache(BaseCacheManagerTest.CACHE).isPresent(), "Cache must present");
    }

    @Test
    public void testWhenGetNotExistingCacheByNameThenResultEmpty() {
        assertTrue(cacheManager.getCache("!" + BaseCacheManagerTest.CACHE).isEmpty(), "Cache must not present");
    }

    @Test
    public void testWhenGetUnderlyingCacheManagerWithRightTypeThenResultNotNull() {
        assertNotNull(cacheManager.getUnderlyingCacheManager(getOriginalCacheManagerClass()), "Underlying CacheManager must be not null");
    }

    @Test
    public void testWhenGetUnderlyingCacheManagerWithNotRightTypeThenCCTException() {
        assertThrows(ClassCastException.class, () -> cacheManager.getUnderlyingCacheManager(Cache.class));
    }

    protected abstract CacheManager configureCacheManager(String cacheName);

    protected abstract Class<?> getOriginalCacheManagerClass();
}
