package net.cache.bus.core.impl.configuration;

import net.cache.bus.core.configuration.CacheBusMessageChannelConfiguration;
import net.cache.bus.core.configuration.CacheBusTransportConfiguration;
import net.cache.bus.core.configuration.ConfigurationException;
import net.cache.bus.core.transport.CacheBusMessageChannel;
import net.cache.bus.core.transport.CacheEntryEventConverter;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Immutable implementation of cache bus transport configuration.<br>
 * To create a configuration, use the builder through the factory method {@linkplain ImmutableCacheBusTransportConfiguration#builder()}.
 *
 * @param converter                      the converter used to transform data before sending it over the message channel and when receiving it, cannot be {@code null}.
 * @param messageChannel                 the implementation of the message channel for interacting with remote caches, cannot be {@code null}.
 * @param messageChannelConfiguration    the configuration of the message channel, cannot {@code null}.
 * @param processingPool                 the thread pool on which the received messages from other servers should be processed, cannot be {@code null}.
 * @param maxConcurrentProcessingThreads the maximum number of threads that can be used to process messages from other servers, cannot be negative.
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
        @Nonnegative int maxProcessingThreadBufferCapacity,
        boolean useAsyncSending,
        @Nullable ExecutorService asyncSendingPool,
        int maxAsyncSendingThreads,
        int maxAsyncSendingThreadBufferCapacity) implements CacheBusTransportConfiguration {

    public ImmutableCacheBusTransportConfiguration {
        Objects.requireNonNull(converter, "converter");
        Objects.requireNonNull(messageChannel, "messageChannel");
        Objects.requireNonNull(messageChannelConfiguration, "messageChannelConfiguration");
        Objects.requireNonNull(processingPool, "processingPool");

        if (maxConcurrentProcessingThreads < 0) {
            throw new ConfigurationException("maxConcurrentProcessingThreads cannot be negative");
        }

        if (maxProcessingThreadBufferCapacity < 0) {
            throw new ConfigurationException("maxProcessingThreadBufferCapacity cannot be negative");
        }

        if (useAsyncSending) {
            if (maxAsyncSendingThreads <= 0) {
                throw new ConfigurationException("Async max threads count must be positive when async sending enabled");
            }

            if (maxAsyncSendingThreadBufferCapacity < 0) {
                throw new ConfigurationException("maxAsyncSendingThreadBufferCapacity cannot be negative");
            }

            Objects.requireNonNull(asyncSendingPool, "Async sending thread pool must be not null when async sending enabled");
        }
    }

    /**
     * Returns a builder for creating a configuration object.
     *
     * @return cannot be {@code null}.
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
        private boolean useAsyncSending;
        private int maxAsyncSendingThreads = 1;
        private ExecutorService asyncSendingPool;
        private int maxAsyncSendingThreadBufferCapacity = 0;

        /**
         * Sets the implementation of the message converter for messages transmitted over the bus.
         *
         * @param converter the message converter, cannot be {@code null}.
         * @return cannot be {@code null}.
         * @see CacheEntryEventConverter
         */
        @Nonnull
        public Builder setConverter(@Nonnull final CacheEntryEventConverter converter) {
            this.converter = converter;
            return this;
        }

        /**
         * Sets the implementation of the message channel for the bus.
         *
         * @param messageChannel the bus message channel, cannot be {@code null}.
         * @return cannot be {@code null}.
         * @see CacheBusMessageChannel
         */
        @Nonnull
        public Builder setMessageChannel(@Nonnull final CacheBusMessageChannel<CacheBusMessageChannelConfiguration> messageChannel) {
            this.messageChannel = messageChannel;
            return this;
        }

        /**
         * Sets the configuration of the message channel for the bus.
         *
         * @param messageChannelConfiguration the message channel configuration, cannot be {@code null}.
         * @return cannot be {@code null}.
         * @see CacheBusMessageChannelConfiguration
         */
        @Nonnull
        public Builder setMessageChannelConfiguration(@Nonnull final CacheBusMessageChannelConfiguration messageChannelConfiguration) {
            this.messageChannelConfiguration = messageChannelConfiguration;
            return this;
        }

        /**
         * Sets the thread pool on which the messages from other servers should be processed.
         *
         * @param processingPool the thread pool, cannot be {@code null}.
         * @return cannot be {@code null}.
         */
        @Nonnull
        public Builder setProcessingPool(@Nonnull final ExecutorService processingPool) {
            this.processingPool = processingPool;
            return this;
        }

        /**
         * Sets the maximum number of threads for processing incoming messages from other servers.
         * By default, the value {@literal 1} is used, meaning that processing is done independently of
         * the receiving thread (producer and consumer are "decoupled") and executed in a single thread.
         *
         * @param maxConcurrentProcessingThreads the maximum number of threads that can be used to process
         *                                       incoming messages, cannot be {@code maxConcurrentProcessingThreads < 0}.
         * @return cannot be {@code null}.
         */
        @Nonnull
        public Builder setMaxConcurrentReceivingThreads(@Nonnegative final int maxConcurrentProcessingThreads) {
            this.maxConcurrentProcessingThreads = maxConcurrentProcessingThreads;
            return this;
        }

        /**
         * Sets the maximum buffer size for a single processing thread handling incoming messages received from the channel.
         * By default, the value {@code 0} is used, which is interpreted as using the default value ({@code 256}).
         *
         * @param maxProcessingThreadBufferCapacity the maximum buffer size for a single thread, cannot be {@code maxProcessingThreadBufferCapacity < 0}.
         * @return cannot be {@code null}.
         * @see CacheBusTransportConfiguration#maxProcessingThreadBufferCapacity()
         */
        @Nonnull
        public Builder setMaxProcessingThreadBufferCapacity(@Nonnegative final int maxProcessingThreadBufferCapacity) {
            this.maxProcessingThreadBufferCapacity = maxProcessingThreadBufferCapacity;
            return this;
        }

        /**
         * Sets the flag indicating whether to use asynchronous sending of cache element change messages to the channel.<br>
         * Before using this feature, all risks should be considered, see {@linkplain CacheBusTransportConfiguration#useAsyncSending()}.
         *
         * @param useAsyncSending the flag indicating whether to use asynchronous sending.
         * @return cannot be {@code null}.
         */
        @Nonnull
        public Builder useAsyncSending(final boolean useAsyncSending) {
            this.useAsyncSending = useAsyncSending;
            return this;
        }

        /**
         * Sets the thread pool on which the asynchronous sending of messages the channel should be performed.<br>
         * Only applicable if asynchronous sending is enabled, {@code useAsyncSending(true)}.
         *
         * @param asyncSendingPool the thread pool, cannot be {@code null}.
         * @return cannot be {@code null}.
         */
        @Nonnull
        public Builder setAsyncSendingPool(@Nonnull final ExecutorService asyncSendingPool) {
            this.asyncSendingPool = asyncSendingPool;
            return this;
        }

        /**
         * Sets the maximum number of threads for sending outgoing messages to the channel.
         * By default, the value {@code 1} used, meaning that sending is done independently of
         * the modification thread (producer and consumer are "decoupled", where the producer is the thread
         * modifying data in the cache, and the consumer is the thread sending the channel) and executed in a single thread.
         *
         * @param maxAsyncSendingThreads the maximum number of threads that can be used for sending messages,
         *                               cannot be {@code maxAsyncSendingThreads < 0}.
         * @return cannot be {@code null}.
         */
        @Nonnull
        public Builder setMaxAsyncSendingThreads(@Nonnegative final int maxAsyncSendingThreads) {
            this.maxAsyncSendingThreads = maxAsyncSendingThreads;
            return this;
        }

        /**
         * Sets the maximum buffer size for a single sending thread handling outgoing messages received
         * from the cache modification threads (main application threads).<br>
         * By default, the value {@code 0} is used, which is interpreted as using the default value ({@code 32}).
         *
         * @param maxAsyncSendingThreadBufferCapacity the maximum buffer size for a single sending thread, cannot {@code maxProcessingThreadBufferCapacity < 0}.
         * @return cannot be {@code null}.
         * @see CacheBusTransportConfiguration#maxAsyncSendingThreadBufferCapacity()
         */
        @Nonnull
        public Builder setMaxAsyncSendingThreadBufferCapacity(@Nonnegative final int maxAsyncSendingThreadBufferCapacity) {
            this.maxAsyncSendingThreadBufferCapacity = maxAsyncSendingThreadBufferCapacity;
            return this;
        }

        /**
         * Creates a transport bus configuration object based on the provided data.
         *
         * @return cannot be {@code null}.
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
                    this.maxProcessingThreadBufferCapacity,
                    this.useAsyncSending,
                    this.asyncSendingPool,
                    this.maxAsyncSendingThreads,
                    this.maxAsyncSendingThreadBufferCapacity
            );
        }
    }
}
