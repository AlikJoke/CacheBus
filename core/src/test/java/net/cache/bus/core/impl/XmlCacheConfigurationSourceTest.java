package net.cache.bus.core.impl;

import net.cache.bus.core.configuration.CacheConfiguration;
import net.cache.bus.core.configuration.CacheType;
import net.cache.bus.core.configuration.InvalidCacheConfigurationException;
import net.cache.bus.core.impl.configuration.ImmutableCacheConfiguration;
import net.cache.bus.core.impl.configuration.XmlCacheConfigurationSource;
import org.junit.jupiter.api.Test;

import java.io.File;
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

        final Set<CacheConfiguration> configurations = source.pull();

        assertEquals(3, configurations.size(), "Configurations count must be equal");
        assertTrue(configurations.contains(new ImmutableCacheConfiguration("test1", CacheType.INVALIDATED)), "Configurations from xml file must contain predefined cache config");
        assertTrue(configurations.contains(new ImmutableCacheConfiguration("test2", CacheType.INVALIDATED)), "Configurations from xml file must contain predefined cache config");
        assertTrue(configurations.contains(new ImmutableCacheConfiguration("test3", CacheType.REPLICATED)), "Configurations from xml file must contain predefined cache config");
    }
}
