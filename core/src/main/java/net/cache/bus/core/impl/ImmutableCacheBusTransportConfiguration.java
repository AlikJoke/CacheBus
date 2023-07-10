package net.cache.bus.core.impl;

import net.cache.bus.core.configuration.CacheBusMessageChannelConfiguration;
import net.cache.bus.core.configuration.CacheBusTransportConfiguration;
import net.cache.bus.core.transport.CacheBusMessageChannel;
import net.cache.bus.core.transport.CacheEntryEventConverter;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

@ThreadSafe
@Immutable
public record ImmutableCacheBusTransportConfiguration(
        @Nonnull CacheEntryEventConverter converter,
        @Nonnull CacheBusMessageChannel<CacheBusMessageChannelConfiguration> messageChannel,
        @Nonnull CacheBusMessageChannelConfiguration messageChannelConfiguration,
        @Nonnull ExecutorService processingPool,
        @Nonnegative int maxConcurrentProcessingThreads) implements CacheBusTransportConfiguration {

    public ImmutableCacheBusTransportConfiguration {
        Objects.requireNonNull(converter, "converter");
        Objects.requireNonNull(messageChannel, "messageChannel");
        Objects.requireNonNull(messageChannelConfiguration, "messageChannelConfiguration");
        Objects.requireNonNull(processingPool, "processingPool");

        if (maxConcurrentProcessingThreads < 0) {
            throw new IllegalArgumentException("maxConcurrentProcessingThreads can not be negative");
        }
    }

    @Nonnull
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private CacheEntryEventConverter converter;
        private CacheBusMessageChannel<CacheBusMessageChannelConfiguration> messageChannel;
        private CacheBusMessageChannelConfiguration messageChannelConfiguration;
        private ExecutorService processingPool;
        private int maxConcurrentProcessingThreads = 1;

        public Builder setConverter(@Nonnull final CacheEntryEventConverter converter) {
            this.converter = converter;
            return this;
        }

        public Builder setMessageChannel(@Nonnull final CacheBusMessageChannel<CacheBusMessageChannelConfiguration> messageChannel) {
            this.messageChannel = messageChannel;
            return this;
        }

        public Builder setMessageChannelConfiguration(@Nonnull final CacheBusMessageChannelConfiguration messageChannelConfiguration) {
            this.messageChannelConfiguration = messageChannelConfiguration;
            return this;
        }

        public Builder setProcessingPool(@Nonnull final ExecutorService processingPool) {
            this.processingPool = processingPool;
            return this;
        }

        /**
         * Устанавливает максимальное количество потоков обработки поступающих сообщений с других серверов.
         * По-умолчанию используется значение {@literal 1}, т.е. обработка производится независимо от
         * потока получения (производитель и потребитель "развязаны") и выполняется в один поток.
         * @param maxConcurrentProcessingThreads максимальное количество потоков, которое может использоваться для обработки поступающих сообщений, не может быть {@code отрицательным.}
         * @return не может быть {@code null}
         */
        public Builder setMaxConcurrentReceivingThreads(@Nonnegative final int maxConcurrentProcessingThreads) {
            this.maxConcurrentProcessingThreads = maxConcurrentProcessingThreads;
            return this;
        }

        @Nonnull
        public CacheBusTransportConfiguration build() {
            return new ImmutableCacheBusTransportConfiguration(
                    this.converter,
                    this.messageChannel,
                    this.messageChannelConfiguration,
                    this.processingPool,
                    this.maxConcurrentProcessingThreads
            );
        }
    }
}
