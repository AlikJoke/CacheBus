package net.cache.bus.ehcache3.adapters;

import net.cache.bus.core.Cache;
import net.cache.bus.core.testing.BaseCacheTest;
import org.ehcache.config.CacheRuntimeConfiguration;
import org.ehcache.spi.loaderwriter.CacheLoadingException;
import org.ehcache.spi.loaderwriter.CacheWritingException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class EhCache3CacheAdapterTest extends BaseCacheTest {

    @Override
    protected Cache<String, String> createCacheAdapter(String cacheName, Map<String, String> valuesMap) {
        return new EhCache3CacheAdapter<>(new org.ehcache.Cache<String, String>() {

            private final Map<String, String> map = new HashMap<>(valuesMap);

            @Override
            public String get(String s) {
                return map.get(s);
            }

            @Override
            public Map<String, String> getAll(Set<? extends String> set) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean containsKey(String s) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void put(String s, String s2) {
                this.map.put(s, s2);
            }

            @Override
            public void putAll(Map<? extends String, ? extends String> map) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String putIfAbsent(String s, String s2) {
                return this.map.putIfAbsent(s, s2);
            }

            @Override
            public void remove(String s) {
                map.remove(s);
            }

            @Override
            public boolean remove(String s, String s2) {
                return map.remove(s, s2);
            }

            @Override
            public String replace(String s, String s2) throws CacheLoadingException, CacheWritingException {
                return this.map.replace(s, s2);
            }

            @Override
            public boolean replace(String s, String s2, String v1) {
                return map.replace(s, s2, v1);
            }

            @Override
            public CacheRuntimeConfiguration<String, String> getRuntimeConfiguration() {
                return null;
            }

            @Override
            public void removeAll(Set<? extends String> set) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void clear() {
                this.map.clear();
            }

            @Override
            public Iterator<Entry<String, String>> iterator() {
                throw new UnsupportedOperationException();
            }
        }, cacheName);
    }
}
