package net.cache.bus.jackson.serialization;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.transport.CacheEntryEventConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

/**
 * Implementation of a converter for serializing/deserializing cache entry change events
 * based on the Jackson library. The conversion is done in JSON format.<br>
 * The converter requires specific settings from {@link ObjectMapper}, and if a pre-configured
 * {@link ObjectMapper} is needed, which is used for serializing objects used as cache keys and values,
 * the converter can be instantiated by calling {@linkplain JacksonCacheEntryEventConverter#create(ObjectMapper)}.
 * This factory method will create a copy of the {@linkplain ObjectMapper} and then configure it to the required state.
 * If the key and value objects are not serialized according to specific rules defined in the application's
 * {@link ObjectMapper}, it is better to use the method {@link JacksonCacheEntryEventConverter#create()}
 * to instantiate the converter.
 *
 * @author Alik
 * @see CacheEntryEventConverter
 */
@Immutable
@ThreadSafe
public final class JacksonCacheEntryEventConverter implements CacheEntryEventConverter {

    private static final Logger logger = LoggerFactory.getLogger(JacksonCacheEntryEventConverter.class);

    private static final String SKIP_VALUES_FILTER = "skipValuesFields";

    private final ObjectWriter objectWriterStd;
    private final ObjectWriter objectWriterCompact;
    private final ObjectReader objectReader;

    private JacksonCacheEntryEventConverter(@Nonnull ObjectMapper objectMapper) {
        this.objectWriterStd = objectMapper.writerFor(CacheEntryEvent.class)
                                            .with(createStdFilterProvider());
        this.objectWriterCompact = objectMapper.writerFor(CacheEntryEvent.class)
                                                .with(createCompactFilterProvider());
        this.objectReader = objectMapper.readerFor(CacheEntryEvent.class);
    }

    @Nonnull
    @Override
    public <K extends Serializable, V extends Serializable> byte[] toBinary(@Nonnull CacheEntryEvent<K, V> event, boolean serializeValueFields) {
        try {
            return serializeValueFields ? this.objectWriterStd.writeValueAsBytes(event) : this.objectWriterCompact.writeValueAsBytes(event);
        } catch (IOException ex) {
            logger.error("Unable to serialize event: " + event, ex);
            throw new RuntimeException(ex);
        }
    }

    @Nonnull
    @Override
    public <K extends Serializable, V extends Serializable> CacheEntryEvent<K, V> fromBinary(@Nonnull byte[] data) {
        try {
            return this.objectReader.readValue(data);
        } catch (IOException ex) {
            logger.error("Unable to deserialize event", ex);
            throw new RuntimeException(ex);
        }
    }

    private FilterProvider createStdFilterProvider() {
        return new SimpleFilterProvider()
                    .setDefaultFilter(new SimpleBeanPropertyFilter.SerializeExceptFilter(Collections.emptySet()));
    }

    private FilterProvider createCompactFilterProvider() {
        return new SimpleFilterProvider()
                    .addFilter(SKIP_VALUES_FILTER, new SimpleBeanPropertyFilter.SerializeExceptFilter(Set.of("oldValue", "newValue")));
    }

    /**
     * Creates an instance of the converter with "default" {@link ObjectMapper} settings.
     *
     * @return cannot be {@code null}.
     */
    @Nonnull
    public static CacheEntryEventConverter create() {
        return new JacksonCacheEntryEventConverter(configureObjectMapper(new ObjectMapper()));
    }

    /**
     * Creates an instance of the converter with a pre-configured {@link ObjectMapper},
     * which is further configured to the required state (a copy is made, the original parameter is not modified).
     *
     * @param mapper pre-configured {@link ObjectMapper}, cannot be {@code null}.
     * @return cannot be {@code null}.
     */
    @Nonnull
    public static CacheEntryEventConverter create(@Nonnull ObjectMapper mapper) {
        return new JacksonCacheEntryEventConverter(configureObjectMapper(mapper.copy()));
    }

    private static ObjectMapper configureObjectMapper(final ObjectMapper mapper) {

        return mapper
                .findAndRegisterModules()
                .registerModule(new SimpleModule("CacheBus"))
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .enable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .enable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .setPropertyNamingStrategy(new PropertyNamingStrategy())
                .addMixIn(CacheEntryEvent.class, DynamicMixIn.class)
                .activateDefaultTyping(mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.EVERYTHING);
    }

    @JsonFilter(SKIP_VALUES_FILTER)
    public static class DynamicMixIn {
    }
}
