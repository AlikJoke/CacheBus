package ru.joke.cache.bus.kafka.channel;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;

import java.util.List;
import java.util.Map;

/**
 * Implementation of a message distributor for Kafka topic partitions based on a numeric hash.
 * Ensures that messages with the same hash (i.e., having the same key within a cache)
 * are routed to the same partition and their order is preserved.
 *
 * @author Alik
 * @see Partitioner
 */
public final class MessageIntHashKeyPartitioner implements Partitioner {

    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        final List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
        final int numPartitions = partitions.size();
        final int messageHash = (Integer) key;

        return Math.abs(messageHash) % numPartitions;
    }

    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> configs) {

    }
}
