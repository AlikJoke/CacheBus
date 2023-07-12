package net.cache.bus.core.impl.configuration;

import net.cache.bus.core.configuration.CacheBusMessageChannelConfiguration;
import net.cache.bus.core.configuration.CacheBusTransportConfiguration;
import net.cache.bus.core.configuration.ConfigurationException;
import net.cache.bus.core.transport.CacheBusMessageChannel;
import net.cache.bus.core.transport.CacheEntryEventConverter;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Неизменяемая реализация конфигурации транспорта для шины кэшей.<br>
 * Для формирования конфигурации необходимо использовать построитель через фабричный метод {@linkplain ImmutableCacheBusTransportConfiguration#builder()}.
 *
 * @param converter                      конвертер, используемый для преобразования данных перед передачей в канал сообщений и при получении из него, не может быть {@code null}.
 * @param messageChannel                 реализация канала сообщений для взаимодействия с удаленными кэшами, не может быть {@code null}.
 * @param messageChannelConfiguration    конфигурация канала сообщений, не может быть {@code null}.
 * @param processingPool                 пул потоков, на котором должна производиться обработка полученных от других серверов сообщений, не может быть {@code null}.
 * @param maxConcurrentProcessingThreads максимальное количество потоков, которое может использоваться для обработки сообщений с других серверов, не может быть отрицательным.
 * @author Alik
 * @see CacheBusTransportConfiguration
 * @see CacheBusTransportConfiguration
 * @see CacheBusMessageChannelConfiguration
 * @see CacheEntryEventConverter
 * @see CacheBusMessageChannel
 */
@ThreadSafe
@Immutable
public record ImmutableCacheBusTransportConfiguration(
        @Nonnull CacheEntryEventConverter converter,
        @Nonnull CacheBusMessageChannel<CacheBusMessageChannelConfiguration> messageChannel,
        @Nonnull CacheBusMessageChannelConfiguration messageChannelConfiguration,
        @Nonnull ExecutorService processingPool,
        @Nonnegative int maxConcurrentProcessingThreads,
        @Nonnegative int maxProcessingThreadBufferCapacity) implements CacheBusTransportConfiguration {

    public ImmutableCacheBusTransportConfiguration {
        Objects.requireNonNull(converter, "converter");
        Objects.requireNonNull(messageChannel, "messageChannel");
        Objects.requireNonNull(messageChannelConfiguration, "messageChannelConfiguration");
        Objects.requireNonNull(processingPool, "processingPool");

        if (maxConcurrentProcessingThreads < 0) {
            throw new ConfigurationException("maxConcurrentProcessingThreads can not be negative");
        }

        if (maxProcessingThreadBufferCapacity < 0) {
            throw new ConfigurationException("maxProcessingThreadBufferCapacity can not be negative");
        }
    }

    /**
     * Возвращает построитель для формирования объекта конфигурации.
     *
     * @return не может быть {@code null}.
     */
    @Nonnull
    public static Builder builder() {
        return new Builder();
    }

    @NotThreadSafe
    public static class Builder {

        private CacheEntryEventConverter converter;
        private CacheBusMessageChannel<CacheBusMessageChannelConfiguration> messageChannel;
        private CacheBusMessageChannelConfiguration messageChannelConfiguration;
        private ExecutorService processingPool;
        private int maxConcurrentProcessingThreads = 1;
        private int maxProcessingThreadBufferCapacity = 0;

        /**
         * Устанавливает используемую реализацию конвертера для сообщений, передаваемых через шину.
         *
         * @param converter конвертер сообщений, не может быть {@code null}.
         * @return не может быть {@code null}
         * @see CacheEntryEventConverter
         */
        @Nonnull
        public Builder setConverter(@Nonnull final CacheEntryEventConverter converter) {
            this.converter = converter;
            return this;
        }

        /**
         * Устанавливает используемую реализацию канала сообщений для шины.
         *
         * @param messageChannel канал сообщений шины, не может быть {@code null}.
         * @return не может быть {@code null}
         * @see CacheBusMessageChannel
         */
        @Nonnull
        public Builder setMessageChannel(@Nonnull final CacheBusMessageChannel<CacheBusMessageChannelConfiguration> messageChannel) {
            this.messageChannel = messageChannel;
            return this;
        }

        /**
         * Устанавливает конфигурацию канала сообщений для шины.
         *
         * @param messageChannelConfiguration конфигурация канала сообщения, не может быть {@code null}.
         * @return не может быть {@code null}
         * @see CacheBusMessageChannelConfiguration
         */
        @Nonnull
        public Builder setMessageChannelConfiguration(@Nonnull final CacheBusMessageChannelConfiguration messageChannelConfiguration) {
            this.messageChannelConfiguration = messageChannelConfiguration;
            return this;
        }

        /**
         * Устанавливает пул потоков, на котором должна производиться обработка сообщений с других серверов.
         *
         * @param processingPool пул потоков, не может быть {@code null}.
         * @return не может быть {@code null}
         */
        @Nonnull
        public Builder setProcessingPool(@Nonnull final ExecutorService processingPool) {
            this.processingPool = processingPool;
            return this;
        }

        /**
         * Устанавливает максимальное количество потоков обработки поступающих сообщений с других серверов.
         * По-умолчанию используется значение {@literal 1}, т.е. обработка производится независимо от
         * потока получения (производитель и потребитель "развязаны") и выполняется в один поток.
         *
         * @param maxConcurrentProcessingThreads максимальное количество потоков, которое может использоваться для обработки поступающих сообщений, не может быть {@code maxConcurrentProcessingThreads < 0}
         * @return не может быть {@code null}
         */
        @Nonnull
        public Builder setMaxConcurrentReceivingThreads(@Nonnegative final int maxConcurrentProcessingThreads) {
            this.maxConcurrentProcessingThreads = maxConcurrentProcessingThreads;
            return this;
        }

        /**
         * Устанавливает максимальный размер буфера одного потока обработки поступающих сообщений, полученных из канала.
         * По-умолчанию используется значение {@code 0}, которое интерпретируется как использование значения по-умолчанию ({@code 256}.
         *
         * @param maxProcessingThreadBufferCapacity максимальный размер буфера одного потока, не может быть {@code maxProcessingThreadBufferCapacity < 0}
         * @return не может быть {@code null}
         * @see CacheBusTransportConfiguration#maxProcessingThreadBufferCapacity()
         */
        @Nonnull
        public Builder setMaxProcessingThreadBufferCapacity(@Nonnegative final int maxProcessingThreadBufferCapacity) {
            this.maxProcessingThreadBufferCapacity = maxProcessingThreadBufferCapacity;
            return this;
        }

        /**
         * Формирует объект конфигурации транспорта шины на основе переданных данных.
         *
         * @return не может быть {@code null}.
         * @see CacheBusTransportConfiguration
         */
        @Nonnull
        public CacheBusTransportConfiguration build() {
            return new ImmutableCacheBusTransportConfiguration(
                    this.converter,
                    this.messageChannel,
                    this.messageChannelConfiguration,
                    this.processingPool,
                    this.maxConcurrentProcessingThreads,
                    this.maxProcessingThreadBufferCapacity
            );
        }
    }
}
