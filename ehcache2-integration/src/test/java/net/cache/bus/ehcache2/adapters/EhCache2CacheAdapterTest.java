package net.cache.bus.ehcache2.adapters;

import net.cache.bus.core.Cache;
import net.cache.bus.core.testing.BaseCacheTest;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.EhcacheDecoratorAdapter;
import org.mockito.Mock;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class EhCache2CacheAdapterTest extends BaseCacheTest {

    @Mock
    private Ehcache cache;

    @Override
    protected Cache<String, String> createCacheAdapter(String cacheName, Map<String, String> valuesMap) {
        final Ehcache cacheWrapper = new EhcacheDecoratorAdapter(this.cache) {

            private final Map<String, String> map = new HashMap<>(valuesMap);

            @Override
            public Element get(Object key) throws IllegalStateException, CacheException {
                if (map.get((String) key) == null) {
                    return null;
                }

                return new Element(key, map.get((String) key));
            }

            @Override
            public Element get(Serializable key) throws IllegalStateException, CacheException {
                return get((Object) key);
            }

            @Override
            public void put(Element element, boolean doNotNotifyCacheReplicators) throws IllegalArgumentException, IllegalStateException, CacheException {
                map.put((String) element.getObjectKey(), (String) element.getObjectValue());
            }

            @Override
            public boolean remove(Object key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
                return map.remove((String) key) != null;
            }

            @Override
            public boolean remove(Object key) throws IllegalStateException {
                return remove(key, true);
            }

            @Override
            public boolean remove(Serializable key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
                return remove((Object) key, true);
            }

            @Override
            public boolean remove(Serializable key) throws IllegalStateException {
                return remove((Object) key, true);
            }

            @Override
            public void removeAll(boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException {
                map.clear();
            }

            @Override
            public String getName() {
                return cacheName;
            }

            @Override
            public Element putIfAbsent(Element element) throws NullPointerException {
                final String value = map.putIfAbsent((String) element.getObjectKey(), (String) element.getObjectValue());
                return value == null ? null : new Element(element.getObjectKey(), value);
            }

            @Override
            public Element putIfAbsent(Element element, boolean doNotNotifyCacheReplicators) throws NullPointerException {
                return putIfAbsent(element);
            }

            @Override
            public boolean removeElement(Element element) throws NullPointerException {
                return map.remove((String) element.getObjectKey()) != null;
            }

            @Override
            public boolean replace(Element old, Element element) throws NullPointerException, IllegalArgumentException {
                return map.replace((String) old.getObjectKey(), (String) old.getObjectValue(), (String) element.getObjectValue());
            }

            @Override
            public void acquireWriteLockOnKey(Object key) {
            }

            @Override
            public void releaseWriteLockOnKey(Object key) {
            }
        };
        return new EhCache2CacheAdapter<>(cacheWrapper);
    }
}
