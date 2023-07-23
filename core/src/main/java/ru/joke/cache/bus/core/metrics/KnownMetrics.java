package ru.joke.cache.bus.core.metrics;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Known metrics related to bus components that the bus and its components register.
 *
 * @author Alik
 */
public enum KnownMetrics {

    MANAGED_CACHES_COUNT("cb.managed.caches.count", "Count of managed by cache bus caches", "module", "cache-bus", "origin", "local", "value", "cache"),

    LOCAL_EVENTS_COMMON_COUNT("cb.local.events.count", "Count of local cache events produced by listeners", "module", "cache-bus", "origin", "local", "value", "cache.events"),

    FILTERED_INV_LOCAL_EVENTS_COUNT("cb.local.filtered.inv.events.count", "Count of local invalidation events produced by listeners", "module", "cache-bus", "origin", "local", "value", "cache.events", "event.type", "invalidation"),

    FILTERED_REPL_LOCAL_EVENTS_COUNT("cb.local.filtered.repl.events.count", "Count of local replication events produced by listeners", "module", "cache-bus", "origin", "local", "value", "cache.events", "event.type", "replication"),

    REMOTE_EVENTS_COMMON_COUNT("cb.remote.events.count", "Count of received remote cache events", "module", "cache-bus", "origin", "remote", "value", "cache.events"),

    ERROR_EVENTS_COUNT("cb.remote.error.events.count", "Count of received cache events failed on deserialization", "module", "cache-bus", "origin", "remote", "value", "cache.events", "value", "errors"),

    APPLIED_INV_EVENTS_COUNT("cb.remote.inv.events.applied.count", "Count of applied remote invalidation cache events", "module", "cache-bus", "origin", "remote", "value", "cache.events", "event.type", "invalidation"),

    APPLIED_REPL_EVENTS_COUNT("cb.remote.repl.events.applied.count", "Count of applied remote replication cache events", "module", "cache-bus", "origin", "remote", "value", "event.events", "event.type", "replication"),

    APPLIED_AS_INV_EVENT_FALLBACK_COUNT("cb.remote.inv.events.applied.as.fallback.count", "Count of application remote events that applied as invalidation events due to errors", "module", "cache-bus", "origin", "remote", "event.type", "invalidation", "value", "error.event.events"),

    PRODUCED_BYTES("cb.channel.produced.bytes.summary", "Summary of produced to channel bytes", "module", "cache-bus", "origin", "local", "source", "channel.producer"),

    CONSUMED_BYTES("cb.channel.consumed.bytes.summary", "Summary of consumed from channel bytes", "module", "cache-bus", "origin", "local", "source", "channel.consumer"),

    PRODUCER_BUFFER_BLOCKING_OFFER_TIME("cb.producer.buffer.blocking.time", "Time of producer's blocking while offering messages to buffer for output sending", "module", "cache-bus", "source", "producer", "value", "buffers.size"),

    CONSUMER_BUFFER_BLOCKING_OFFER_TIME("cb.consumer.buffer.blocking.time", "Time of consumer's blocking while offering messages to buffer for input processing", "module", "cache-bus", "source", "consumer", "value", "buffers.size"),

    BUFFER_READ_POSITION("cb.buffer.read.position", "Buffer read position", "module", "cache-bus", "source", "producer/consumer", "value", "buffers.size"),

    BUFFER_WRITE_POSITION("cb.buffer.write.position", "Buffer write position", "module", "cache-bus", "source", "producer/consumer", "value", "buffers.size"),

    PRODUCER_INTERRUPTED_THREADS("cb.producer.interrupted.threads.count", "Count of message producer's interrupted threads", "module", "cache-bus", "source", "producer", "value", "threads"),

    CONSUMER_INTERRUPTED_THREADS("cb.consumer.interrupted.threads.count", "Count of message consumer's (processing threads) interrupted threads", "module", "cache-bus", "source", "consumer", "value", "threads"),

    PRODUCER_CONNECTION_WAIT_TIME("cb.channel.producer.conn.wait.time", "Time of waiting to retrieve producer's connection to channel", "module", "cache-bus", "source", "channel.producer", "value", "connection"),

    PRODUCER_CONNECTION_RECOVERY_TIME("cb.channel.producer.conn.recovery.time", "Time of producer's connection recovery", "module", "cache-bus", "source", "channel.producer", "value", "connection.errors"),

    CONSUMER_CONNECTION_RECOVERY_TIME("cb.channel.consumer.conn.recovery.time", "Time of waiting to retrieve consumer's connection to channel", "module", "cache-bus", "source", "channel.consumer", "value", "connection"),

    PRODUCERS_IN_RECOVERY_COUNT("cb.channel.producers.in.recovery.count", "Count of channel producers in recovery", "module", "cache-bus", "source", "channel.producer", "value", "connection.errors"),

    CONSUMERS_IN_RECOVERY_COUNT("cb.channel.consumers.in.recovery.count", "Count of channel consumers in recovery", "module", "cache-bus", "source", "channel.consumer", "value", "connection.errors");

    private final String id;
    private final String description;
    private final List<String> tags;

    KnownMetrics(@Nonnull String id, @Nonnull String description, @Nonnull String... tags) {
        this.id = id;
        this.description = description;
        this.tags = List.of(tags);
    }

    @Nonnull
    public String id() {
        return this.id;
    }

    @Nonnull
    public String description() {
        return this.description;
    }

    @Nonnull
    public List<String> tags() {
        return this.tags;
    }
}
