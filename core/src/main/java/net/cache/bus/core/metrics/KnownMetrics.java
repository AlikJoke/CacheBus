package net.cache.bus.core.metrics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public enum KnownMetrics {

    MANAGED_CACHES_COUNT("cb.managed.caches.count", "Count of managed by cache bus caches", "cache-bus", "local", "cache"),

    LOCAL_EVENTS_COMMON_COUNT("cb.local.events.count", "Count of local cache events produced by listeners", "cache-bus", "local", "cache.events"),

    FILTERED_INV_LOCAL_EVENTS_COUNT("cb.local.filtered.inv.events.count", "Count of local invalidation events produced by listeners", "cache-bus", "local", "cache.events", "invalidation"),

    FILTERED_REPL_LOCAL_EVENTS_COUNT("cb.local.filtered.repl.events.count", "Count of local replication events produced by listeners", "cache-bus", "local", "cache.events", "replication"),

    REMOTE_EVENTS_COMMON_COUNT("cb.remote.events.count", "Count of received remote cache events", "cache-bus", "remote", "cache.events"),

    ERROR_EVENTS_COUNT("cb.remote.error.events.count", "Count of received cache events failed on deserialization", "cache-bus", "remote", "cache.events", "errors"),

    APPLIED_INV_EVENTS_COUNT("cb.remote.inv.events.applied.count", "Count of applied remote invalidation cache events", "cache-bus", "remote", "cache.events", "invalidation"),

    APPLIED_REPL_EVENTS_COUNT("cb.remote.repl.events.applied.count", "Count of applied remote replication cache events", "cache-bus", "remote", "cache.events", "replication"),

    APPLIED_AS_INV_EVENT_FALLBACK_COUNT("cb.remote.inv.events.applied.as.fallback.count", "Count of application remote events that applied as invalidation events due to errors", "cache-bus", "remote", "cache.events", "invalidation", "errors"),

    PRODUCED_BYTES("cb.channel.produced.bytes.summary", "Summary of produced to channel bytes", "cache-bus", "local", "channel", "producer"),

    CONSUMED_BYTES("cb.channel.consumed.bytes.summary", "Summary of consumed from channel bytes", "cache-bus", "local", "channel", "consumer"),

    PRODUCER_BUFFER_BLOCKING_OFFER_TIME("cb.producer.buffer.blocking.time", "Time of producer's blocking while offering messages to buffer for output sending", "cache-bus", "producer", "buffers"),

    CONSUMER_BUFFER_BLOCKING_OFFER_TIME("cb.consumer.buffer.blocking.time", "Time of consumer's blocking while offering messages to buffer for input processing", "cache-bus", "consumer", "buffers"),

    BUFFER_READ_POSITION("cb.buffer.read.position", "Buffer read position", "cache-bus", "producer", "consumer", "buffers"),

    BUFFER_WRITE_POSITION("cb.buffer.write.position", "Buffer write position", "cache-bus", "producer", "consumer", "buffers"),

    PRODUCER_INTERRUPTED_THREADS("cb.producer.interrupted.threads.count", "Count of message producer's interrupted threads", "cache-bus", "producer", "threads"),

    CONSUMER_INTERRUPTED_THREADS("cb.consumer.interrupted.threads.count", "Count of message consumer's (processing threads) interrupted threads", "cache-bus", "consumer", "threads"),

    PRODUCER_CONNECTION_WAIT_TIME("cb.channel.producer.conn.wait.time", "Time of waiting to retrieve producer's connection to channel", "cache-bus", "producer", "channel", "connection"),

    PRODUCER_CONNECTION_RECOVERY_TIME("cb.channel.producer.conn.recovery.time", "Time of producer's connection recovery", "cache-bus", "producer", "channel", "connection", "errors"),

    CONSUMER_CONNECTION_RECOVERY_TIME("cb.channel.consumer.conn.recovery.time", "Time of waiting to retrieve consumer's connection to channel", "cache-bus", "consumer", "channel", "connection"),

    PRODUCERS_IN_RECOVERY_COUNT("cb.channel.producers.in.recovery.count", "Count of channel producers in recovery", "cache-bus", "producer", "channel", "connection", "errors"),

    CONSUMERS_IN_RECOVERY_COUNT("cb.channel.consumers.in.recovery.count", "Count of channel consumers in recovery", "cache-bus", "consumer", "channel", "connection", "errors");

    private final String id;
    private final String description;
    private final List<String> tags;

    KnownMetrics(@Nonnull String id, @Nonnull String description, @Nonnull String... tags) {
        this.id = id;
        this.description = description;
        this.tags = List.of(tags);
    }

    @Nonnull
    public String getId() {
        return this.id;
    }

    @Nonnull
    public String getDescription() {
        return this.description;
    }

    @Nonnull
    public List<String> getTags() {
        return this.tags;
    }
}
