package net.cache.bus.kafka.channel;

import io.github.embeddedkafka.EmbeddedK;
import io.github.embeddedkafka.EmbeddedKafka;
import io.github.embeddedkafka.EmbeddedKafkaConfig;
import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.CacheEntryEventType;
import net.cache.bus.core.CacheEventMessageConsumer;
import net.cache.bus.core.LifecycleException;
import net.cache.bus.core.impl.ImmutableCacheEntryEvent;
import net.cache.bus.core.impl.internal.ImmutableCacheEntryOutputMessage;
import net.cache.bus.core.impl.resolvers.StaticHostNameResolver;
import net.cache.bus.core.transport.CacheEntryOutputMessage;
import net.cache.bus.kafka.configuration.KafkaCacheBusMessageChannelConfiguration;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@Disabled(value = "Slow and unstable due to embedded kafka")
public class KafkaCacheBusMessageChannelTest {

    private static EmbeddedKafkaConfig config;
    private static EmbeddedK kafka;

    @BeforeAll
    public static void startEmbeddedKafka() {
        config = EmbeddedKafkaConfig.defaultConfig();
        kafka = EmbeddedKafka.start(config);
    }

    @AfterAll
    public static void stopEmbeddedKafka() {
        kafka.stop(true);
    }

    @Test
    public void testChannelMethodsWhenChannelIsNotActivated() {
        final KafkaCacheBusMessageChannel channel = new KafkaCacheBusMessageChannel();

        assertThrows(LifecycleException.class, () -> channel.send(mock(CacheEntryOutputMessage.class)));
        assertThrows(LifecycleException.class, channel::unsubscribe, "Unsubscribing available only after subscribing");
        assertThrows(LifecycleException.class, () -> channel.subscribe(new TestMessageConsumer()), "Subscribing available only after activation of channel");
    }

    @Test
    public void testSendAndReceiveAfterActivation() throws InterruptedException {
        // preparation
        final KafkaCacheBusMessageChannel sendChannel = new KafkaCacheBusMessageChannel();
        activateChannel(sendChannel, "h1");

        final KafkaCacheBusMessageChannel receiveChannel = new KafkaCacheBusMessageChannel();
        activateChannel(receiveChannel, "h2");

        final CacheEntryEvent<String, String> event1 = new ImmutableCacheEntryEvent<>("1", null, "v1", CacheEntryEventType.ADDED, "test1");
        final byte[] binaryEvent1 = event1.key().getBytes();
        final CacheEntryOutputMessage outputMessage1 = new ImmutableCacheEntryOutputMessage(event1, binaryEvent1);

        final CacheEntryEvent<String, String> event2 = new ImmutableCacheEntryEvent<>("2", null, "v1", CacheEntryEventType.ADDED, "test1");
        final byte[] binaryEvent2 = event2.key().getBytes();
        final CacheEntryOutputMessage outputMessage2 = new ImmutableCacheEntryOutputMessage(event2, binaryEvent2);

        final TestMessageConsumer consumerHost2 = new TestMessageConsumer();

        // action
        receiveChannel.subscribe(consumerHost2);
        TimeUnit.SECONDS.sleep(5);

        sendChannel.send(outputMessage1);
        sendChannel.send(outputMessage2);
        TimeUnit.SECONDS.sleep(1);

        assertEquals(2, consumerHost2.bodyMap.size(), "Count of received messages on host h2 must be 2");
        assertArrayEquals(binaryEvent1, consumerHost2.bodyMap.get(outputMessage1.messageHashKey()), "Event body must be equal");
        assertArrayEquals(binaryEvent2, consumerHost2.bodyMap.get(outputMessage2.messageHashKey()), "Event body must be equal");

        sendChannel.unsubscribe();
        receiveChannel.unsubscribe();
    }

    private void activateChannel(final KafkaCacheBusMessageChannel channel, final String host) {
        final KafkaCacheBusMessageChannelConfiguration configuration =
                KafkaCacheBusMessageChannelConfiguration.builder()
                        .setChannel("test1")
                        .setConsumerProperties(Collections.singletonMap(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:" + config.kafkaPort()))
                        .setProducerProperties(Collections.singletonMap(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:" + config.kafkaPort()))
                        .setSubscribingPool(new ForkJoinPool())
                        .setHostNameResolver(new StaticHostNameResolver(host))
                        .build();
        channel.activate(configuration);
    }

    private static class TestMessageConsumer implements CacheEventMessageConsumer {

        private final Map<Integer, byte[]> bodyMap = new ConcurrentHashMap<>();

        @Override
        public void accept(int messageHash, @Nonnull byte[] messageBody) {
            this.bodyMap.put(messageHash, messageBody);
        }
    }
}
