package net.cache.bus.core.configuration;

import net.cache.bus.core.transport.CacheEntryEventDeserializer;
import net.cache.bus.core.transport.CacheEntryEventSerializer;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.Set;

/**
 * Конфигурация транспорта для одного кэша.
 *
 * @author Alik
 * @see TargetEndpointConfiguration
 * @see SourceEndpointConfiguration
 * @see CacheBusConfiguration
 */
public interface CacheTransportConfiguration {

    /**
     * Возвращает набор конфигураций транспорта для отправки данных об изменениях элементов локального кэша.
     *
     * @return набор конфигураций транспорта для отправки данных об изменениях элементов кэша, не может быть {@code null}.
     * @see TargetEndpointConfiguration
     */
    @Nonnull
    Set<TargetEndpointConfiguration> targetConfigurations();

    /**
     * Возвращает конфигурацию транспорта для отправки данных об изменениях элементов локального кэша по имени конечной точки.
     *
     * @param endpointName идентификатор конечной точки, не может быть {@code null}.
     * @return не может быть {@code null}.
     * @see TargetEndpointConfiguration
     */
    @Nonnull
    Optional<TargetEndpointConfiguration> getTargetConfigurationByEndpointName(@Nonnull String endpointName);

    /**
     * Возвращает набор конфигураций транспорта для получения данных об изменениях элементов удаленных экземпляров кэша.
     *
     * @return набор конфигураций транспорта для получения данных об изменениях элементов кэша, не может быть {@code null}.
     * @see SourceEndpointConfiguration
     */
    @Nonnull
    Set<SourceEndpointConfiguration> sourceConfigurations();

    /**
     * Возвращает конфигурацию транспорта для получения данных об изменениях элементов удаленных экземпляров кэша по имени конечной точки.
     *
     * @param endpointName идентификатор конечной точки, не может быть {@code null}.
     * @return не может быть {@code null}.
     * @see SourceEndpointConfiguration
     */
    @Nonnull
    Optional<SourceEndpointConfiguration> getSourceConfigurationByEndpointName(@Nonnull String endpointName);

    /**
     * Конфигурация конечной точки для отправки данных об изменениях элементов кэша.
     *
     * @author Alik
     * @see CacheEntryEventSerializer
     */
    interface TargetEndpointConfiguration {

        /**
         * Возвращает имя конечной точки для отправки данных об изменениях элементов кэша.
         *
         * @return не может быть {@code null}.
         */
        @Nonnull
        String endpoint();

        /**
         * Возвращает сериализатор для формирования "транспортного" представления события об изменении элемента кэша.
         *
         * @param <T> тип "транспортного" представления события об изменении элемента кэша
         * @return не может быть {@code null}.
         * @see CacheEntryEventSerializer
         */
        @Nonnull
        <T> CacheEntryEventSerializer<T> serializer();
    }

    /**
     * Конфигурация конечной точки для получения данных об изменениях элементов кэша.
     *
     * @author Alik
     * @see CacheEntryEventSerializer
     */
    interface SourceEndpointConfiguration {

        /**
         * Возвращает имя конечной точки для получения данных об изменениях элементов кэша.
         *
         * @return не может быть {@code null}.
         */
        @Nonnull
        String endpoint();

        /**
         * Возвращает десериализатор для формирования объекта {@link net.cache.bus.core.CacheEntryEvent} из
         * "транспортного" представления события об изменении элемента кэша.
         *
         * @param <T> тип "транспортного" представления события об изменении элемента кэша
         * @return не может быть {@code null}.
         * @see CacheEntryEventDeserializer
         */
        @Nonnull
        <T> CacheEntryEventDeserializer<T> deserializer();
    }
}
