package net.cache.bus.core.testing;

import net.cache.bus.core.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.CONCURRENT)
public abstract class BaseCacheTest {

    private static final String CACHE_NAME = "test";
    private static final String KEY_1 = "1";
    private static final String VAL_1 = "v1";
    private static final String KEY_2 = "2";
    private static final String VAL_2 = "v2";
    private static final String KEY_3_NO_VALUE = "3";

    private Cache<String, String> cache;

    @BeforeEach
    public void preparation() {
        this.cache = createCacheAdapter(CACHE_NAME, Map.of(KEY_1, VAL_1, KEY_2, VAL_2));
    }

    @Test
    public void testWhenGetCacheNameThenResultIsOriginalCacheName() {
        assertEquals(CACHE_NAME, cache.getName(), "Cache name from adapter must be equal to original name");
    }

    @Test
    public void testGetByKeyFromCache() {
        assertTrue(cache.get(KEY_1).filter(VAL_1::equals).isPresent(), "Value must be returned by key");
        assertTrue(cache.get(KEY_2).filter(VAL_2::equals).isPresent(), "Value must be returned by key");
        assertTrue(cache.get(KEY_3_NO_VALUE).isEmpty(), "Value must absent in cache");
    }

    @Test
    public void testPutValueToCache() {
        assertTrue(cache.get(KEY_3_NO_VALUE).isEmpty(), "Value must absent in cache");
        this.cache.put(KEY_3_NO_VALUE, VAL_2);
        assertTrue(this.cache.get(KEY_3_NO_VALUE).filter(VAL_2::equals).isPresent(), "Value must be returned by key");
    }

    @Test
    public void testRemoveValueFromCache() {
        assertTrue(this.cache.remove(KEY_1).filter(VAL_1::equals).isPresent(), "Removed value must be returned by key");
        assertTrue(this.cache.get(KEY_1).isEmpty(), "Value must be empty (removed)");
    }

    @Test
    public void testClearAllFromCache() {
        assertTrue(this.cache.get(KEY_1).isPresent(), "Value must be returned by key");
        this.cache.clear();
        assertTrue(this.cache.get(KEY_1).isEmpty(), "Value must be empty (removed)");
    }

    @Test
    public void testEvictValueFromCache() {
        assertTrue(this.cache.get(KEY_1).filter(VAL_1::equals).isPresent(), "Value must be returned by key");
        this.cache.evict(KEY_1);
        assertTrue(this.cache.get(KEY_1).isEmpty(), "Value must be empty (evicted)");
    }

    @Test
    public void testPutIfAbsentValueToCache() {
        this.cache.putIfAbsent(KEY_3_NO_VALUE, VAL_2);
        assertTrue(this.cache.get(KEY_3_NO_VALUE).filter(VAL_2::equals).isPresent(), "Value must be returned by key");
        this.cache.putIfAbsent(KEY_3_NO_VALUE, VAL_1);
        assertTrue(this.cache.get(KEY_3_NO_VALUE).filter(VAL_2::equals).isPresent(), "Old value must be returned by key");
    }

    @Test
    public void testComputeIfAbsentOperation() {
        assertTrue(this.cache.computeIfAbsent(KEY_1, key -> VAL_2).filter(VAL_1::equals).isPresent(), "Old bounded value must be returned");
        assertTrue(this.cache.computeIfAbsent(KEY_3_NO_VALUE, key -> VAL_1).filter(VAL_1::equals).isPresent(), "New value must be returned");
    }

    @Test
    public void testMergeOperation() {
        this.cache.merge(KEY_3_NO_VALUE, VAL_2, (v1, v2) -> VAL_1.equals(v1) ? v2 : null);
        assertTrue(this.cache.get(KEY_3_NO_VALUE).filter(VAL_2::equals).isPresent(), "New value must be returned");

        this.cache.merge(KEY_3_NO_VALUE, VAL_1, (v1, v2) -> VAL_1.equals(v1) ? v2 : null);
        assertTrue(this.cache.get(KEY_3_NO_VALUE).isEmpty(), "Empty value must be returned");
    }

    protected abstract Cache<String, String> createCacheAdapter(String cacheName, Map<String, String> valuesMap);
}
