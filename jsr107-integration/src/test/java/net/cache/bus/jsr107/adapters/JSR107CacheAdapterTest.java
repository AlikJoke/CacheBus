package net.cache.bus.jsr107.adapters;

import net.cache.bus.core.BaseCacheTest;
import net.cache.bus.core.Cache;

import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class JSR107CacheAdapterTest extends BaseCacheTest {

    @Override
    protected Cache<String, String> createCacheAdapter(String cacheName, Map<String, String> valuesMap) {
        return new JSR107CacheAdapter<>(new javax.cache.Cache<>() {

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
            public void loadAll(Set<? extends String> set, boolean b, CompletionListener completionListener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void put(String s, String s2) {
                this.map.put(s, s2);
            }

            @Override
            public String getAndPut(String s, String s2) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void putAll(Map<? extends String, ? extends String> map) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean putIfAbsent(String s, String s2) {
                return this.map.putIfAbsent(s, s2) == null;
            }

            @Override
            public boolean remove(String s) {
                return map.remove(s) != null;
            }

            @Override
            public boolean remove(String s, String s2) {
                return map.remove(s, s2);
            }

            @Override
            public String getAndRemove(String s) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean replace(String s, String s2, String v1) {
                return map.replace(s, s2, v1);
            }

            @Override
            public boolean replace(String s, String s2) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getAndReplace(String s, String s2) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void removeAll(Set<? extends String> set) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void removeAll() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void clear() {
                this.map.clear();
            }

            @Override
            public <C extends Configuration<String, String>> C getConfiguration(Class<C> aClass) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> T invoke(String s, EntryProcessor<String, String, T> entryProcessor, Object... objects) throws EntryProcessorException {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> Map<String, EntryProcessorResult<T>> invokeAll(Set<? extends String> set, EntryProcessor<String, String, T> entryProcessor, Object... objects) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getName() {
                return cacheName;
            }

            @Override
            public CacheManager getCacheManager() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void close() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isClosed() {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> T unwrap(Class<T> aClass) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void registerCacheEntryListener(CacheEntryListenerConfiguration<String, String> cacheEntryListenerConfiguration) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<String, String> cacheEntryListenerConfiguration) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Iterator<Entry<String, String>> iterator() {
                throw new UnsupportedOperationException();
            }
        });
    }
}
