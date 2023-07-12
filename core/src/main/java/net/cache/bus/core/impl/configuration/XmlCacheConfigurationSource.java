package net.cache.bus.core.impl.configuration;

import net.cache.bus.core.configuration.CacheConfiguration;
import net.cache.bus.core.configuration.CacheConfigurationSource;
import net.cache.bus.core.configuration.CacheType;
import net.cache.bus.core.configuration.InvalidCacheConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Реализация источника конфигурации кэшей шины на основе конфигурационного XML-файла.
 *
 * @author Alik
 * @see CacheConfigurationSource
 */
@ThreadSafe
@Immutable
public final class XmlCacheConfigurationSource implements CacheConfigurationSource {

    private static final String SCHEMA_PATH = "/configuration/configuration.xsd";

    private static final String CACHE_ELEMENT = "cache";
    private static final String CACHE_NAME_ATTR = "name";
    private static final String CACHE_TYPE_ATTR = "type";
    private static final String CACHE_ALIASES_ELEMENT = "aliases";
    private static final String CACHE_ALIAS_ELEMENT = "alias";

    private final File configurationFile;
    private final String resourceConfigurationFilePath;

    public XmlCacheConfigurationSource(@Nonnull File configurationFile) {
        this.configurationFile = Objects.requireNonNull(configurationFile, "configurationFile");
        this.resourceConfigurationFilePath = null;
    }

    public XmlCacheConfigurationSource(@Nonnull String resourceConfigurationFilePath) {
        this.resourceConfigurationFilePath = Objects.requireNonNull(resourceConfigurationFilePath, "resourceConfigurationFilePath");
        this.configurationFile = null;
    }

    @Nonnull
    @Override
    public Set<CacheConfiguration> pull() {

        try (final InputStream xmlStream = openConfigurationStream()) {

            validateConfiguration();
            final Document doc = parseConfiguration(xmlStream);

            return buildConfigurationsFromDocument(doc);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            throw new InvalidCacheConfigurationException(ex);
        }
    }

    private Set<CacheConfiguration> buildConfigurationsFromDocument(final Document document) {

        final Set<CacheConfiguration> result = new HashSet<>();

        final NodeList caches = document.getElementsByTagName(CACHE_ELEMENT);
        for (int cacheIndex = 0; cacheIndex < caches.getLength(); cacheIndex++) {

            final Node node = caches.item(cacheIndex);

            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            final Element cacheElement = (Element) node;
            final String cacheName = cacheElement.getAttribute(CACHE_NAME_ATTR);
            final String cacheTypeString = cacheElement.getAttribute(CACHE_TYPE_ATTR);
            final CacheType cacheType = CacheType.valueOf(cacheTypeString.toUpperCase());
            final Set<String> aliases = parseAliases(cacheElement);

            final CacheConfiguration cacheConfiguration = ImmutableCacheConfiguration
                                                                .builder()
                                                                    .setCacheName(cacheName)
                                                                    .setCacheType(cacheType)
                                                                    .setCacheAliases(aliases)
                                                                .build();
            result.add(cacheConfiguration);
        }

        return Collections.unmodifiableSet(result);
    }

    private Set<String> parseAliases(final Element cacheElement) {

        final Set<String> result = new HashSet<>();

        final NodeList aliasesNode = cacheElement.getElementsByTagName(CACHE_ALIASES_ELEMENT);
        if (aliasesNode.getLength() == 0) {
            return result;
        }

        final Element aliasesElement = (Element) aliasesNode.item(0);
        final NodeList aliases = aliasesElement.getElementsByTagName(CACHE_ALIAS_ELEMENT);

        for (int aliasIndex = 0; aliasIndex < aliases.getLength(); aliasIndex++) {
            final Node cacheAlias = aliases.item(aliasIndex);
            result.add(cacheAlias.getTextContent());
        }

        return result;
    }

    private Document parseConfiguration(final InputStream configurationStream) throws ParserConfigurationException, IOException, SAXException {

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        final DocumentBuilder builder = factory.newDocumentBuilder();

        final Document doc = builder.parse(configurationStream);
        doc.getDocumentElement().normalize();

        return doc;
    }

    private InputStream openConfigurationStream() throws FileNotFoundException {
        if (this.configurationFile == null) {
            return openInputStream(this.resourceConfigurationFilePath);
        } else {
            return new FileInputStream(this.configurationFile);
        }
    }

    private InputStream openInputStream(final String configurationFilePath) {
        return XmlCacheConfigurationSource.class.getResourceAsStream(configurationFilePath);
    }

    private void validateConfiguration() throws IOException, SAXException {
        try (final InputStream xsdStream = openInputStream(SCHEMA_PATH);
             final InputStream xmlStream = openConfigurationStream()) {
            final Validator validator = createValidator(xsdStream);
            validator.validate(new StreamSource(xmlStream));
        }
    }

    private Validator createValidator(final InputStream xsdStream) throws SAXException {
        final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        final Source schemaFile = new StreamSource(xsdStream);
        final Schema schema = factory.newSchema(schemaFile);
        return schema.newValidator();
    }
}
