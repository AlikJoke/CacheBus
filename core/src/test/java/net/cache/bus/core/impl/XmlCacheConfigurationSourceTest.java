package net.cache.bus.core.impl;

import net.cache.bus.core.configuration.CacheConfiguration;
import net.cache.bus.core.configuration.CacheSetConfiguration;
import net.cache.bus.core.configuration.CacheType;
import net.cache.bus.core.configuration.InvalidCacheConfigurationException;
import net.cache.bus.core.impl.configuration.ImmutableCacheConfiguration;
import net.cache.bus.core.impl.configuration.ImmutableTimestampCacheConfiguration;
import net.cache.bus.core.impl.configuration.XmlCacheConfigurationSource;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class XmlCacheConfigurationSourceTest {

    private static final String INVALID_FILE_NAME = "/test-invalid-configuration.xml";
    private static final String VALID_FILE_NAME = "/test-configuration.xml";
    private static final String RELATIVE_PATH = "src/test/resources";

    @Test
    public void testWhenPullFromInvalidXmlExternalFileThenException() {
        final File xmlFile = new File(RELATIVE_PATH + INVALID_FILE_NAME);
        final XmlCacheConfigurationSource source = new XmlCacheConfigurationSource(xmlFile);
        makeChecksInvalid(source);
    }

    @Test
    public void testWhenPullFromInvalidXmlByResourceConfigFilePathThenException() {
        final XmlCacheConfigurationSource source = new XmlCacheConfigurationSource(INVALID_FILE_NAME);
        makeChecksInvalid(source);
    }

    @Test
    public void testWhenPullFromValidXmlFileThenOk() {
        final File xmlFile = new File(RELATIVE_PATH + VALID_FILE_NAME);
        final XmlCacheConfigurationSource source = new XmlCacheConfigurationSource(xmlFile);
        makeChecksValid(source);
    }

    @Test
    public void testWhenPullFromValidXmlByResourceConfigFilePathThenOk() {
        final XmlCacheConfigurationSource source = new XmlCacheConfigurationSource(VALID_FILE_NAME);
        makeChecksValid(source);
    }

    private void makeChecksInvalid(final XmlCacheConfigurationSource source) {
        assertThrows(InvalidCacheConfigurationException.class, source::pull, "Exception must be thrown when the file is invalid");
    }

    private void makeChecksValid(final XmlCacheConfigurationSource source) {

        final CacheSetConfiguration configurations = source.pull();

        assertTrue(configurations.useAsyncCleaning(), "Async cleaning must be enabled");
        assertEquals(3, configurations.cacheConfigurations().size(), "Configurations count must be equal");

        final List<CacheConfiguration> configsToCompare = new ArrayList<>();
        final var cacheConfig1 =
                ImmutableCacheConfiguration
                    .builder()
                        .setCacheName("test1")
                        .setCacheType(CacheType.INVALIDATED)
                        .setCacheAliases(Set.of("test1_1", "test1_2"))
                        .useTimestampBasedComparison(true)
                        .setTimestampConfiguration(new ImmutableTimestampCacheConfiguration(256, 60000))
                    .build();
        configsToCompare.add(cacheConfig1);
        configsToCompare.add(buildCacheConfig("test2", CacheType.INVALIDATED));
        configsToCompare.add(buildCacheConfig("test3", CacheType.REPLICATED));

        configsToCompare.forEach(cc -> {
            final CacheConfiguration config = configurations
                                                    .cacheConfigurations()
                                                    .stream()
                                                    .filter(cc::equals)
                                                    .findAny()
                                                    .orElseThrow();
            assertEquals(cc.cacheName(), config.cacheName(), "Cache name must be equal");
            assertEquals(cc.cacheType(), config.cacheType(), "Cache type must be equal");
            assertEquals(cc.cacheAliases(), config.cacheAliases(), "Cache aliases must be equal");
        });

        final CacheConfiguration configForTest1Cache =
                configurations
                        .cacheConfigurations()
                        .stream()
                        .filter(configsToCompare.get(0)::equals)
                        .findAny()
                        .orElseThrow();
        assertTrue(configForTest1Cache.useTimestampBasedComparison(), "Stamp based comparison should be enabled for test1 cache config");
        assertTrue(configForTest1Cache.timestampConfiguration().isPresent(), "Stamp configuration must present");
        configForTest1Cache.timestampConfiguration().ifPresent(config -> {
            assertEquals(256, config.probableAverageElementsCount(), "Probable avg elements count must be equal");
            assertEquals(60000, config.timestampExpiration(), "Timestamp expiration must be equal");
        });

        final CacheConfiguration configForTest2Cache =
                configurations
                        .cacheConfigurations()
                        .stream()
                        .filter(configsToCompare.get(1)::equals)
                        .findAny()
                        .orElseThrow();
        assertTrue(configForTest2Cache.useTimestampBasedComparison(), "Stamp based comparison should be enabled for test2 cache config");
        assertTrue(configForTest2Cache.timestampConfiguration().isPresent(), "Stamp configuration must present");
        configForTest2Cache.timestampConfiguration().ifPresent(config -> {
            assertEquals(128, config.probableAverageElementsCount(), "Probable avg elements count must be equal to default value");
            assertEquals(1800000, config.timestampExpiration(), "Timestamp expiration must be equal to default");
        });
    }

    private CacheConfiguration buildCacheConfig(String cacheName, CacheType cacheType) {
        return ImmutableCacheConfiguration
                        .builder()
                            .setCacheName(cacheName)
                            .setCacheType(cacheType)
                        .build();
    }
}
