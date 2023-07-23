package ru.joke.cache.bus.core.configuration;

import ru.joke.cache.bus.core.transport.CacheBusMessageChannel;
import ru.joke.cache.bus.core.transport.CacheEntryEventConverter;
import ru.joke.cache.bus.core.CacheEntryEvent;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.concurrent.ExecutorService;

/**
 * Configuration of the transport for the cache bus.
 *
 * @author Alik
 * @see CacheEntryEventConverter
 * @see CacheBusConfiguration
 * @see CacheBusMessageChannel
 */
public interface CacheBusTransportConfiguration {

    /**
     * Returns the converter for creating the "transport" representation of a cache entry event
     * and converting back from binary representation to an object
     * of type {@link CacheEntryEvent}.
     *
     * @return cannot be {@code null}.
     * @see CacheEntryEventConverter
     */
    @Nonnull
    CacheEntryEventConverter converter();

    /**
     * Returns the message channel used for interaction with other servers.
     *
     * @return message channel, cannot be {@code null}.
     * @see CacheBusMessageChannel
     */
    @Nonnull
    CacheBusMessageChannel<CacheBusMessageChannelConfiguration> messageChannel();

    /**
     * Returns the message channel configuration used for interaction with other servers.
     *
     * @return cannot be {@code null}.
     * @see CacheBusMessageChannelConfiguration
     */
    @Nonnull
    CacheBusMessageChannelConfiguration messageChannelConfiguration();

    /**
     * Returns the thread pool on which message processing (deserialization and application
     * to local caches) from other servers is performed.
     *
     * @return cannot be {@code null}.
     */
    @Nonnull
    ExecutorService processingPool();

    /**
     * Returns the maximum number of threads that can be used for processing messages received from other servers.
     *
     * @return the maximum number of processing threads, cannot be negative;
     * if the value is {@code 0}, then message retrieval from the channel and processing are performed in a single thread.
     */
    @Nonnegative
    int maxConcurrentProcessingThreads();

    /**
     * Returns the maximum buffer size of a single message processing thread.
     * Each thread has its own buffer of this size.
     * The buffer size affects the throughput of message processing from the channel. The larger the buffer,
     * the higher the throughput (lower the chance of the write thread (also the read thread from the channel)
     * being blocked because the message processing threads cannot keep up with processing the messages).
     * On the other hand, the larger the buffer size, the higher the memory consumption.<br>
     * If {@code maxConcurrentProcessingThreads() == 0}, then buffers are not used and the value will be ignored.
     *
     * @return the maximum buffer size of a single message processing thread, cannot be negative;
     * if the value is {@code 0}, the default value ({@code 256}) will be used.
     */
    @Nonnegative
    int maxProcessingThreadBufferCapacity();

    /**
     * Returns whether synchronous processing should be used for messages received from the channel.
     *
     * @return {@code true} if synchronous processing should be used, otherwise {@code false}.
     */
    default boolean useSynchronousProcessing() {
        return maxConcurrentProcessingThreads() == 0;
    }

    /**
     * Returns whether asynchronous message sending to the channel should be used.
     * If enabled, message sending to the channel will be performed in a separate thread,
     * without blocking the data modification thread in the cache.<br>
     * It should be noted that using this mode reduces the level of data consistency in the cache
     * and increases the risk of conflicts, as the probability of unordered processing increases,
     * although it improves processing performance.<br>
     * If mainly invalidation caches are used, the risks are minimal and asynchronous sending can be used
     * without any loss, but for replicated caches with a high data modification rate,
     * this mode is not recommended.<br>
     * If enabled, the thread pool {@linkplain CacheBusTransportConfiguration#asyncSendingPool()} must be specified.
     *
     * @return {@code true} asynchronous sending is used, {@code false} otherwise.
     */
    boolean useAsyncSending();

    /**
     * Returns the pool on which asynchronous message sending is performed,
     * if {@code useAsyncSending() == true}.
     *
     * @return can be {@code null} if {@code useAsyncSending() == false}.
     */
    ExecutorService asyncSendingPool();

    /**
     * Returns the maximum number threads that can be used for asynchronous message sending
     * to the channel, if {@code useAsyncSending() == true}.
     *
     * @return the number of asynchronous sending threads, must be positive; the default value is
     * {@code 1}, i.e., messages will be sent asynchronously, but only by one thread.
     */
    @Nonnegative
    int maxAsyncSendingThreads();

    /**
     * Returns the maximum size of the buffer for a single message sending thread.
     * Each thread has its own buffer of this size.<br>
     * The buffer size affects the throughput of message sending in the channel. The larger the buffer,
     * the higher the throughput (lower probability of thread blocking due to asynchronous message sending threads
     * not being able to send messages to the channel in time). On the other hand, the larger the buffer size,
     * the higher the memory consumption and the higher the probability that changes will not propagate to other
     * servers in case of application termination.<br>
     * If {@code useAsyncSending() == false}, then asynchronous sending buffers are not used and
     * the value will be ignored.
     *
     * @return the maximum size the buffer for a single message sending thread, cannot be negative;
     * a value of {@code 0} will use the default value ({@code 32}).
     */
    @Nonnegative
    int maxAsyncSendingThreadBufferCapacity();
}
