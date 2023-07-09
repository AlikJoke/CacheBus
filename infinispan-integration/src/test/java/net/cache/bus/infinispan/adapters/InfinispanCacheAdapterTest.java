package net.cache.bus.infinispan.adapters;

import net.cache.bus.core.Cache;
import net.cache.bus.core.testing.BaseCacheTest;
import org.infinispan.AdvancedCache;
import org.infinispan.CacheCollection;
import org.infinispan.CacheSet;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ClusteringConfiguration;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.cachelistener.filter.CacheEventConverter;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilter;
import org.mockito.Mock;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.mockito.Mockito.lenient;

public class InfinispanCacheAdapterTest extends BaseCacheTest {

    @Mock
    private Configuration configuration;
    @Mock
    private ClusteringConfiguration clusteringConfiguration;

    @Override
    protected Cache<String, String> createCacheAdapter(String cacheName, Map<String, String> valuesMap) {

        lenient().when(clusteringConfiguration.cacheMode()).thenReturn(CacheMode.LOCAL);
        lenient().when(configuration.clustering()).thenReturn(clusteringConfiguration);

        return new InfinispanCacheAdapter<>(new org.infinispan.Cache<>() {

            private final Map<String, String> map = new HashMap<>(valuesMap);
            
            @Override
            public CompletionStage<Void> addListenerAsync(Object listener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletionStage<Void> removeListenerAsync(Object listener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<Object> getListeners() {
                throw new UnsupportedOperationException();
            }

            @Override
            public <C> CompletionStage<Void> addListenerAsync(Object listener, CacheEventFilter<? super String, ? super String> filter, CacheEventConverter<? super String, ? super String, C> converter) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <C> CompletionStage<Void> addFilteredListenerAsync(Object listener, CacheEventFilter<? super String, ? super String> filter, CacheEventConverter<? super String, ? super String, C> converter, Set<Class<? extends Annotation>> filterAnnotations) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <C> CompletionStage<Void> addStorageFormatFilteredListenerAsync(Object listener, CacheEventFilter<? super String, ? super String> filter, CacheEventConverter<? super String, ? super String, C> converter, Set<Class<? extends Annotation>> filterAnnotations) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean startBatch() {
                return false;
            }

            @Override
            public void endBatch(boolean successful) {

            }

            @Override
            public CompletableFuture<String> putAsync(String key, String value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<String> putAsync(String key, String value, long lifespan, TimeUnit unit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<String> putAsync(String key, String value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<Void> putAllAsync(Map<? extends String, ? extends String> data) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<Void> putAllAsync(Map<? extends String, ? extends String> data, long lifespan, TimeUnit unit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<Void> putAllAsync(Map<? extends String, ? extends String> data, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<Void> clearAsync() {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<Long> sizeAsync() {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<String> putIfAbsentAsync(String key, String value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<String> putIfAbsentAsync(String key, String value, long lifespan, TimeUnit unit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<String> putIfAbsentAsync(String key, String value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<String> removeAsync(Object key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<Boolean> removeAsync(Object key, Object value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<String> replaceAsync(String key, String value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<String> replaceAsync(String key, String value, long lifespan, TimeUnit unit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<String> replaceAsync(String key, String value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<Boolean> replaceAsync(String key, String oldValue, String newValue) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<Boolean> replaceAsync(String key, String oldValue, String newValue, long lifespan, TimeUnit unit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<Boolean> replaceAsync(String key, String oldValue, String newValue, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<String> getAsync(String key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<String> computeAsync(String key, BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<String> computeAsync(String key, BiFunction<? super String, ? super String, ? extends String> remappingFunction, long lifespan, TimeUnit lifespanUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<String> computeAsync(String key, BiFunction<? super String, ? super String, ? extends String> remappingFunction, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<String> computeIfAbsentAsync(String key, Function<? super String, ? extends String> mappingFunction) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<String> computeIfAbsentAsync(String key, Function<? super String, ? extends String> mappingFunction, long lifespan, TimeUnit lifespanUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<String> computeIfAbsentAsync(String key, Function<? super String, ? extends String> mappingFunction, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<String> computeIfPresentAsync(String key, BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<String> computeIfPresentAsync(String key, BiFunction<? super String, ? super String, ? extends String> remappingFunction, long lifespan, TimeUnit lifespanUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<String> computeIfPresentAsync(String key, BiFunction<? super String, ? super String, ? extends String> remappingFunction, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<String> mergeAsync(String key, String value, BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<String> mergeAsync(String key, String value, BiFunction<? super String, ? super String, ? extends String> remappingFunction, long lifespan, TimeUnit lifespanUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<String> mergeAsync(String key, String value, BiFunction<? super String, ? super String, ? extends String> remappingFunction, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getName() {
                return cacheName;
            }

            @Override
            public String getVersion() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String put(String key, String value) {
                return map.put(key, value);
            }

            @Override
            public String put(String key, String value, long lifespan, TimeUnit unit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String putIfAbsent(String key, String value, long lifespan, TimeUnit unit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void putAll(Map<? extends String, ? extends String> map, long lifespan, TimeUnit unit) {

            }

            @Override
            public String replace(String key, String value, long lifespan, TimeUnit unit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean replace(String key, String oldValue, String value, long lifespan, TimeUnit unit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String put(String key, String value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String putIfAbsent(String key, String value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void putAll(Map<? extends String, ? extends String> map, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String replace(String key, String value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean replace(String key, String oldValue, String value, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
                return false;
            }

            @Override
            public String merge(String key, String value, BiFunction<? super String, ? super String, ? extends String> remappingFunction, long lifespan, TimeUnit lifespanUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String merge(String key, String value, BiFunction<? super String, ? super String, ? extends String> remappingFunction, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String compute(String key, BiFunction<? super String, ? super String, ? extends String> remappingFunction, long lifespan, TimeUnit lifespanUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String compute(String key, BiFunction<? super String, ? super String, ? extends String> remappingFunction, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String computeIfPresent(String key, BiFunction<? super String, ? super String, ? extends String> remappingFunction, long lifespan, TimeUnit lifespanUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String computeIfPresent(String key, BiFunction<? super String, ? super String, ? extends String> remappingFunction, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String computeIfAbsent(String key, Function<? super String, ? extends String> mappingFunction, long lifespan, TimeUnit lifespanUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String computeIfAbsent(String key, Function<? super String, ? extends String> mappingFunction, long lifespan, TimeUnit lifespanUnit, long maxIdleTime, TimeUnit maxIdleTimeUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String remove(Object key) {
                return map.remove(key);
            }

            @Override
            public void putAll(@Nonnull Map<? extends String, ? extends String> m) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void putForExternalRead(String key, String value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void putForExternalRead(String key, String value, long lifespan, TimeUnit unit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void putForExternalRead(String key, String value, long lifespan, TimeUnit lifespanUnit, long maxIdle, TimeUnit maxIdleUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void evict(String key) {
                map.remove(key);
            }

            @Override
            public Configuration getCacheConfiguration() {
                return configuration;
            }

            @Override
            public EmbeddedCacheManager getCacheManager() {
                throw new UnsupportedOperationException();
            }

            @Override
            public AdvancedCache<String, String> getAdvancedCache() {
                throw new UnsupportedOperationException();
            }

            @Override
            public ComponentStatus getStatus() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int size() {
                return map.size();
            }

            @Override
            public boolean isEmpty() {
                return size() == 0;
            }

            @Override
            public boolean containsKey(Object key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean containsValue(Object value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String get(Object key) {
                return map.get(key);
            }

            @Override
            public CacheSet<String> keySet() {
                throw new UnsupportedOperationException();
            }

            @Override
            public CacheCollection<String> values() {
                throw new UnsupportedOperationException();
            }

            @Override
            public CacheSet<Entry<String, String>> entrySet() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void clear() {
                map.clear();
            }

            @Override
            public void start() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void stop() {
                throw new UnsupportedOperationException();
            }

            @Override
            public String putIfAbsent(@Nonnull String key, String value) {
                return map.putIfAbsent(key, value);
            }

            @Override
            public boolean remove(@Nonnull Object key, Object value) {
                return map.remove(key, value);
            }

            @Override
            public boolean replace(@Nonnull String key, @Nonnull String oldValue, @Nonnull String newValue) {
                return map.replace(key, oldValue, newValue);
            }

            @Override
            public String replace(@Nonnull String key, @Nonnull String value) {
                return map.replace(key, value);
            }

            @Override
            public String computeIfAbsent(String key, Function<? super String, ? extends String> mappingFunction) {
                return map.computeIfAbsent(key, mappingFunction);
            }

            @Override
            public String computeIfPresent(String key, BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String compute(String key, BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String merge(String key, String value, BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
                return map.merge(key, value, remappingFunction);
            }
        });
    }
}
