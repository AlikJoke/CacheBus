package net.cache.bus.rabbit.channel;

import com.rabbitmq.client.*;
import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.CacheEntryEventType;
import net.cache.bus.core.CacheEventMessageConsumer;
import net.cache.bus.core.impl.ImmutableCacheEntryEvent;
import net.cache.bus.core.impl.internal.ImmutableCacheEntryOutputMessage;
import net.cache.bus.core.impl.resolvers.StaticHostNameResolver;
import net.cache.bus.core.transport.CacheEntryOutputMessage;
import net.cache.bus.core.transport.MessageChannelException;
import net.cache.bus.rabbit.configuration.RabbitCacheBusMessageChannelConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static net.cache.bus.rabbit.channel.RabbitCacheBusMessageChannel.HASH_KEY_PROPERTY;
import static net.cache.bus.rabbit.channel.RabbitCacheBusMessageChannel.HOST_PROPERTY;
import static net.cache.bus.transport.ChannelConstants.MESSAGE_TYPE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.CONCURRENT)
public class RabbitCacheBusMessageChannelTest {

    private static final int CHANNELS_COUNT = 5;
    private static final String CHANNEL_NAME = "test";
    private static final String HOST_NAME = "test-host";

    @Mock
    private ConnectionFactory connectionFactory;
    @Mock
    private Connection connection;
    @Mock
    private Channel channel;
    @Captor
    private ArgumentCaptor<Consumer> consumerArgumentCaptor;
    @Captor
    private ArgumentCaptor<ShutdownListener> shutdownListenerCaptor;

    @BeforeEach
    public void configureMocks() throws IOException, TimeoutException {
        lenient().when(this.connectionFactory.newConnection()).thenReturn(this.connection);
        lenient().when(this.connection.createChannel()).thenReturn(this.channel);

        lenient().doNothing().when(this.connection).addShutdownListener(shutdownListenerCaptor.capture());
    }

    @Test
    public void testChannelMethodsWhenChannelIsNotActivated() {
        final RabbitCacheBusMessageChannel channel = new RabbitCacheBusMessageChannel();

        assertThrows(MessageChannelException.class, () -> channel.send(mock(CacheEntryOutputMessage.class)));
        assertThrows(MessageChannelException.class, channel::close, "Closure available only after activation");
        assertThrows(MessageChannelException.class, () -> channel.subscribe(new TestMessageConsumer()), "Subscribing available only after activation of channel");
    }

    @Test
    public void testActivationOfChannel() throws IOException {
        // preparation
        final RabbitCacheBusMessageChannel channel = new RabbitCacheBusMessageChannel();

        // action
        activateChannel(channel);

        //check
        verify(this.channel, times(1)).queueDeclare(CHANNEL_NAME, false, false, false, Collections.emptyMap());
    }

    @Test
    public void testProducerConnectionRecoveryOnShutdownListener() throws IOException, TimeoutException {
        // preparation
        final RabbitCacheBusMessageChannel channel = new RabbitCacheBusMessageChannel();
        activateChannel(channel);

        final CacheEntryEvent<String, String> event = new ImmutableCacheEntryEvent<>("1", null, "v1", CacheEntryEventType.ADDED, "test1");
        final byte[] binaryEvent = event.key().getBytes();
        final CacheEntryOutputMessage outputMessage = new ImmutableCacheEntryOutputMessage(event, binaryEvent);

        // checks
        assertEquals(2, this.shutdownListenerCaptor.getAllValues().size(), "Must be registered 2 shutdown listeners");

        // actions
        this.shutdownListenerCaptor.getAllValues().forEach(l -> l.shutdownCompleted(new ShutdownSignalException(false, false, mock(Method.class), new Object())));
        channel.send(outputMessage);

        // checks
        makeSuccessSendChecks(outputMessage, 1);
        verify(this.connectionFactory, times(2 * 2)).newConnection();

        // actions
        this.shutdownListenerCaptor.getAllValues().forEach(l -> l.shutdownCompleted(new ShutdownSignalException(false, true, mock(Method.class), new Object())));
        assertEquals(4, this.shutdownListenerCaptor.getAllValues().size(), "Must be registered 4 shutdown listeners (2 old and 2 new)");

        // check that no new connections were allocated
        verify(this.connectionFactory, times(2 * 2)).newConnection();
    }

    @Test
    public void testConsumerConnectionRecoveryOnShutdownListener() throws IOException, TimeoutException {
        // preparation
        final RabbitCacheBusMessageChannel channel = new RabbitCacheBusMessageChannel();
        activateChannel(channel);

        // checks
        assertEquals(2, this.shutdownListenerCaptor.getAllValues().size(), "Must be registered 2 shutdown listeners");

        // actions
        channel.subscribe(new TestMessageConsumer());
        this.shutdownListenerCaptor.getAllValues().forEach(l -> l.shutdownCompleted(new ShutdownSignalException(false, false, mock(Method.class), new Object())));

        // checks
        verify(this.channel, times(2)).basicConsume(eq(CHANNEL_NAME), eq(true), nullable(String.class), eq(true), eq(false), eq(Collections.emptyMap()), any());
        verify(this.connectionFactory, times(2 * 2)).newConnection();

        // actions
        this.shutdownListenerCaptor.getAllValues().forEach(l -> l.shutdownCompleted(new ShutdownSignalException(false, true, mock(Method.class), new Object())));
        assertEquals(4, this.shutdownListenerCaptor.getAllValues().size(), "Must be registered 4 shutdown listeners (2 old and 2 new)");

        // check that no new connections were allocated
        verify(this.connectionFactory, times(2 * 2)).newConnection();
    }

    @Test
    public void testRecoveryOfSendingConnectionAfterFailure() throws IOException, TimeoutException {
        // preparation
        final RabbitCacheBusMessageChannel channel = new RabbitCacheBusMessageChannel();
        activateChannel(channel);

        final CacheEntryEvent<String, String> event = new ImmutableCacheEntryEvent<>("1", null, "v1", CacheEntryEventType.ADDED, "test1");
        final byte[] binaryEvent = event.key().getBytes();
        final CacheEntryOutputMessage outputMessage = new ImmutableCacheEntryOutputMessage(event, binaryEvent);

        final int retries = 2;
        final AtomicInteger failCount = new AtomicInteger(retries);
        doAnswer(i -> {
            if (failCount.getAndDecrement() != 0) {
                throw new IOException();
            }

            return null;
        }).when(this.channel).basicPublish(anyString(), anyString(), any(), any());

        // action
        channel.send(outputMessage);

        // checks
        makeSuccessSendChecks(outputMessage, retries + 1);
        verify(this.connectionFactory, times(2 + retries)).newConnection();
        verify(this.connection, times(1 + (CHANNELS_COUNT - 1) * (retries + 1))).createChannel();
        verify(this.connection, times(retries)).close();
    }

    @Test
    public void testSubscribingFail() throws IOException {

        // preparation
        final RabbitCacheBusMessageChannel channel = new RabbitCacheBusMessageChannel();
        activateChannel(channel);

        doThrow(IOException.class).when(this.channel).basicConsume(eq(CHANNEL_NAME), eq(true), nullable(String.class), eq(true), eq(false), eq(Collections.emptyMap()), any());

        // action
        assertThrows(MessageChannelException.class, () -> channel.subscribe(new TestMessageConsumer()));
    }

    @Test
    public void testSubscribingToChannelAfterActivation() throws IOException {

        // preparation
        final RabbitCacheBusMessageChannel channel = new RabbitCacheBusMessageChannel();
        activateChannel(channel);

        when(this.channel.basicConsume(eq(CHANNEL_NAME), eq(true), nullable(String.class), eq(true), eq(false), eq(Collections.emptyMap()), this.consumerArgumentCaptor.capture())).thenReturn("");

        try (final TestMessageConsumer consumer = new TestMessageConsumer()) {

            final byte[] body = "1".getBytes();
            final int hash = Arrays.hashCode(body);

            // action
            channel.subscribe(consumer);
            assertNotNull(this.consumerArgumentCaptor.getValue(), "Consumer must be registered");

            this.consumerArgumentCaptor.getValue().handleDelivery(null, null,
                    new AMQP.BasicProperties()
                        .builder()
                            .headers(
                                    Map.of(
                                            HOST_PROPERTY, HOST_NAME,
                                            HASH_KEY_PROPERTY, hash
                                    )
                            )
                            .type(MESSAGE_TYPE)
                            .deliveryMode(2)
                        .build(), body);

            // checks
            assertNotNull(consumer.bodyMap.get(hash), "Messages by hash must be not null");
            final Queue<byte[]> messagesQueue = consumer.bodyMap.get(hash);
            assertEquals(body, messagesQueue.poll(), "Message body must be equal");
            assertNull(messagesQueue.poll(), "No any messages must present");

            channel.close();
        }
    }

    @Test
    public void testClosureFromChannelAfterActivationAndSubscribing() throws IOException {
        // preparation
        final RabbitCacheBusMessageChannel channel = new RabbitCacheBusMessageChannel();
        activateChannel(channel);
        channel.subscribe(new TestMessageConsumer());

        // action
        channel.close();

        // checks
        verify(this.connection, times(2)).close();
    }

    private void makeSuccessSendChecks(final CacheEntryOutputMessage outputMessage, final int publishCount) throws IOException {

        verify(this.channel, times(publishCount)).basicPublish(eq(""), eq(CHANNEL_NAME), assertArg(props -> {
            assertEquals(MESSAGE_TYPE, props.getType(), "Message type must be " + MESSAGE_TYPE);
            assertEquals(2, props.getDeliveryMode(), "Delivery mode must be persistent");
            assertEquals(HOST_NAME, props.getHeaders().get(HOST_PROPERTY), "Host name must be equal");
            assertEquals(outputMessage.messageHashKey(), props.getHeaders().get(HASH_KEY_PROPERTY), "Hash key must be equal");
        }), eq(outputMessage.cacheEntryMessageBody()));
    }

    private void activateChannel(final RabbitCacheBusMessageChannel channel) {
        final RabbitCacheBusMessageChannelConfiguration configuration =
                RabbitCacheBusMessageChannelConfiguration.builder()
                        .setChannel(CHANNEL_NAME)
                        .setAvailableChannelsCount(CHANNELS_COUNT)
                        .setReconnectTimeoutMs(1_000)
                        .setConnectionFactory(this.connectionFactory)
                        .setHostNameResolver(new StaticHostNameResolver(HOST_NAME))
                        .build();
        channel.activate(configuration);
    }

    private static class TestMessageConsumer implements CacheEventMessageConsumer {

        private final Map<Integer, Queue<byte[]>> bodyMap = new ConcurrentHashMap<>();

        @Override
        public void accept(int messageHash, @Nonnull byte[] messageBody) {
            this.bodyMap.computeIfAbsent(messageHash, k -> new ConcurrentLinkedQueue<>()).add(messageBody);
        }
    }
}
