package net.cache.bus.kafka.channel;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;

import java.util.List;
import java.util.Map;

/**
 * Реализация распределителя сообщений по партициям топика Kafka в зависимости от числового хэша
 * события для гарантии того, что сообщения с одними хэшем (т.е. имеющие один ключ в рамках
 * одного кэша) попадут в одну партицию и порядок между ними будет сохранен.
 *
 * @see Partitioner
 * @author Alik
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
