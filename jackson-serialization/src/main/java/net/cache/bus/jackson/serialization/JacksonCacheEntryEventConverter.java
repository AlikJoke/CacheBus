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
 * Реализация конвертера для сериализации / десериализации событий об изменении элементов кэшей
 * на основе библиотеки Jackson. Преобразование происходит в формат JSON.
 * <br/>
 * Конвертер требует специфичных настроек {@link ObjectMapper} и если требуется использовать
 * преднастроенный {@link ObjectMapper}, который используется для сериализации объектов, используемых
 * в качестве ключей и значений кэшей, для создания экземпляра конвертера необходимо
 * вызвать {@linkplain JacksonCacheEntryEventConverter#create(ObjectMapper)}.
 * Этот фабричный метод произведет копирование {@linkplain ObjectMapper}, после чего донастроит его до
 * необходимого состояния.
 * Если объекты ключей и значений не сериализуются по специальным правилам имеющимся в приложении
 * {@link ObjectMapper}, то лучше использовать метод {@link JacksonCacheEntryEventConverter#create()}
 * для создания экземпляра конвертера.
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
     * Создает экземпляр конвертера со "стандартными" настройками {@link ObjectMapper}.
     *
     * @return не может быть {@code null}.
     */
    @Nonnull
    public static CacheEntryEventConverter create() {
        return new JacksonCacheEntryEventConverter(configureObjectMapper(new ObjectMapper()));
    }

    /**
     * Создает экземпляр конвертера с преднастроенным {@link ObjectMapper},
     * который донастраивается до необходимого состояния (его копия, исходный параметр не меняется).
     *
     * @param mapper преднастроенный {@link ObjectMapper}, не может быть {@code null}.
     * @return не может быть {@code null}.
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
