package net.cache.bus.jms.channel;

import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.CacheEntryEventType;
import net.cache.bus.core.CacheEventMessageConsumer;
import net.cache.bus.core.impl.ImmutableComponentState;
import net.cache.bus.core.impl.ImmutableCacheEntryEvent;
import net.cache.bus.core.impl.internal.ImmutableCacheEntryOutputMessage;
import net.cache.bus.core.impl.resolvers.StaticHostNameResolver;
import net.cache.bus.core.state.ComponentState;
import net.cache.bus.core.transport.CacheEntryOutputMessage;
import net.cache.bus.core.transport.MessageChannelException;
import net.cache.bus.jms.configuration.JmsCacheBusMessageChannelConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.annotation.Nonnull;
import javax.jms.*;
import java.io.Serializable;
import java.time.Duration;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;

import static net.cache.bus.transport.ChannelConstants.MESSAGE_TYPE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.CONCURRENT)
public class JmsCacheBusMessageChannelTest {

    private static final int CONNECTIONS_COUNT = 5;
    private static final String CHANNEL_NAME = "test";
    private static final String HOST_NAME = "test-host";

    @Mock
    private ConnectionFactory connectionFactory;
    @Mock
    private JMSContext jmsContext;
    @Mock
    private Topic destination;

    private final TestJMSProducer producer = new TestJMSProducer();
    private final TestJMSConsumer consumer = new TestJMSConsumer();

    @BeforeEach
    public void configureMocks() {
        lenient().when(this.connectionFactory.createContext()).thenReturn(this.jmsContext);
        lenient().when(this.jmsContext.createTopic(CHANNEL_NAME)).thenReturn(this.destination);
        lenient().when(this.jmsContext.createProducer()).thenReturn(this.producer);
        lenient().when(this.jmsContext.createConsumer(eq(this.destination), anyString())).thenReturn(this.consumer);
    }

    @Test
    public void testChannelMethodsWhenChannelIsNotActivated() {
        final JmsCacheBusMessageChannel channel = new JmsCacheBusMessageChannel();

        assertThrows(MessageChannelException.class, () -> channel.send(mock(CacheEntryOutputMessage.class)));
        assertThrows(MessageChannelException.class, channel::close, "Closure available only after activation");
        assertThrows(MessageChannelException.class, () -> channel.subscribe(new TestMessageConsumer()), "Subscribing available only after activation of channel");
        assertNotNull(channel.state(), "State must be not null");
        assertEquals(ComponentState.Status.DOWN, channel.state().status(), "State must be DOWN");
    }

    @Test
    public void testSendToChannelAfterActivation() {
        // preparation
        final JmsCacheBusMessageChannel channel = new JmsCacheBusMessageChannel();
        activateChannel(channel);

        final CacheEntryEvent<String, String> event = new ImmutableCacheEntryEvent<>("1", null, "v1", CacheEntryEventType.ADDED, "test1");
        final byte[] binaryEvent = event.key().getBytes();
        final CacheEntryOutputMessage outputMessage = new ImmutableCacheEntryOutputMessage(event, binaryEvent);

        // action
        channel.send(outputMessage);

        // checks
        makeSuccessSendChecks(outputMessage);
        assertEquals(ComponentState.Status.UP_NOT_READY, channel.state().status(), "State must be UP_NOT_READY (no subscribing detected)");
    }

    @Test
    public void testRecoveryOfSendingConnectionAfterFailure() {
        // preparation
        final JmsCacheBusMessageChannel channel = new JmsCacheBusMessageChannel();
        activateChannel(channel);

        final CacheEntryEvent<String, String> event = new ImmutableCacheEntryEvent<>("1", null, "v1", CacheEntryEventType.ADDED, "test1");
        final byte[] binaryEvent = event.key().getBytes();
        final CacheEntryOutputMessage outputMessage = new ImmutableCacheEntryOutputMessage(event, binaryEvent);

        // action
        // Флаг сбросится в методе send
        this.producer.errorOnSend = true;
        channel.send(outputMessage);

        // checks
        makeSuccessSendChecks(outputMessage);
        // CONNECTIONS_COUNT - количество изначально созданных соединений,
        // и +1 - пересозданное при ошибке при получении данных из канала
        verify(this.connectionFactory, times(CONNECTIONS_COUNT + 1)).createContext();
        verify(this.jmsContext, times(2)).createProducer();
        verify(this.jmsContext, times(1)).close();
    }

    @Test
    public void testSubscribingRecoveryAfterConnectionFailure() throws InterruptedException {

        // preparation
        final JmsCacheBusMessageChannel channel = new JmsCacheBusMessageChannel();
        activateChannel(channel);
        this.consumer.errorOnReceive = true;

        try (final TestMessageConsumer consumer = new TestMessageConsumer()) {
            // action
            channel.subscribe(consumer);

            Thread.sleep(Duration.ofMillis(300));

            // checks
            assertEquals(ComponentState.Status.UP_OK, channel.state().status(), "State must be UP_OK");
            // CONNECTIONS_COUNT - количество изначально созданных соединений,
            // и +1 - пересозданное при ошибке при получении данных из канала
            verify(this.connectionFactory, times(CONNECTIONS_COUNT + 1)).createContext();
            verify(this.jmsContext, times(2)).createConsumer(eq(this.destination), anyString());
            verify(this.jmsContext, times(1)).close();
        }
    }

    @Test
    public void testSubscribingToChannelAfterActivation() throws JMSException, InterruptedException {

        // preparation
        final JmsCacheBusMessageChannel channel = new JmsCacheBusMessageChannel();
        activateChannel(channel);
        try (final TestMessageConsumer consumer = new TestMessageConsumer()) {

            final byte[] body = "1".getBytes();
            final int hash = Arrays.hashCode(body);

            final BytesMessage message = createMockMessage(body, hash);

            // action
            channel.subscribe(consumer);
            this.consumer.messages.offer(message);
            while (consumer.bodyMap.isEmpty()) {
                Thread.sleep(Duration.ofMillis(1));
            }

            // checks
            verify(message, never()).acknowledge();
            final byte[] receivedBody = consumer.bodyMap.get(hash);
            assertEquals(body, receivedBody, "Message body must be equal");

            channel.close();
        }
    }

    @Test
    public void testClosureFromChannelAfterActivationAndSubscribing() {
        // preparation
        final JmsCacheBusMessageChannel channel = new JmsCacheBusMessageChannel();
        activateChannel(channel);
        channel.subscribe(new TestMessageConsumer());

        // action
        channel.close();

        // checks
        assertEquals(ComponentState.Status.DOWN, channel.state().status(), "State must be DOWN");
        verify(this.jmsContext, times(CONNECTIONS_COUNT)).close();
    }

    private BytesMessage createMockMessage(final byte[] body, final int hash) throws JMSException {
        final BytesMessage message = mock(BytesMessage.class);
        when(message.getBody(byte[].class)).thenReturn(body);
        when(message.getIntProperty(JmsCacheBusMessageChannel.HASH_KEY_PROPERTY)).thenReturn(hash);

        return message;
    }

    private void makeSuccessSendChecks(final CacheEntryOutputMessage outputMessage) {

        assertTrue(this.producer.getDisableMessageID(), "MessageID generation must be disabled for producer");
        assertTrue(this.producer.getDisableMessageTimestamp(), "Timestamp generation must be disabled for producer");
        assertEquals(MESSAGE_TYPE, this.producer.getJMSType(), "JMSType must be equal");
        assertEquals(DeliveryMode.PERSISTENT, this.producer.getDeliveryMode(), "Delivery mode must be NON_PERSISTENT");
        assertEquals(outputMessage.messageHashKey(), this.producer.getIntProperty(JmsCacheBusMessageChannel.HASH_KEY_PROPERTY), "Hash property must be equal");
        assertEquals(HOST_NAME, this.producer.getStringProperty(JmsCacheBusMessageChannel.HOST_PROPERTY), "Host property must be equal");

        assertFalse(this.producer.binaryMessages.isEmpty(), "Sent messages must be not empty");
        final List<byte[]> binaryMessages = this.producer.binaryMessages.get(this.destination);
        assertFalse(binaryMessages.isEmpty(), "Sent binary messages must be not empty");
        assertEquals(outputMessage.cacheEntryMessageBody(), binaryMessages.get(0), "Sent message body must be not null");
    }

    private void activateChannel(final JmsCacheBusMessageChannel channel) {
        final JmsCacheBusMessageChannelConfiguration configuration =
                JmsCacheBusMessageChannelConfiguration.builder()
                                                        .setChannel(CHANNEL_NAME)
                                                        .setAvailableConnectionsCount(CONNECTIONS_COUNT)
                                                        .setReconnectTimeoutMs(1_000)
                                                        .setConnectionFactory(this.connectionFactory)
                                                        .setSubscribingPool(Executors.newSingleThreadExecutor())
                                                        .setHostNameResolver(new StaticHostNameResolver(HOST_NAME))
                                                      .build();
        channel.activate(configuration);
    }

    private static class TestMessageConsumer implements CacheEventMessageConsumer {

        private final Map<Integer, byte[]> bodyMap = new ConcurrentHashMap<>();

        @Override
        public void accept(int messageHash, @Nonnull byte[] messageBody) {
            this.bodyMap.put(messageHash, messageBody);
        }

        @Nonnull
        @Override
        public ComponentState state() {
            return new ImmutableComponentState("test-consumer", ComponentState.Status.UP_OK);
        }
    }

    private static class TestJMSConsumer implements JMSConsumer {

        private final Queue<Message> messages = new ConcurrentLinkedQueue<>();
        private volatile boolean errorOnReceive;

        @Override
        public String getMessageSelector() {
            throw new UnsupportedOperationException();
        }

        @Override
        public MessageListener getMessageListener() throws JMSRuntimeException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setMessageListener(MessageListener listener) throws JMSRuntimeException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Message receive() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Message receive(long timeout) {
            if (this.errorOnReceive) {
                this.errorOnReceive = false;
                throw new JMSRuntimeException("Connection failure");
            }

            final var result = this.messages.poll();
            if (result == null) {
                try {
                    Thread.sleep(Duration.ofMillis(timeout));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            return result;
        }

        @Override
        public Message receiveNoWait() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {

        }

        @Override
        public <T> T receiveBody(Class<T> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T receiveBody(Class<T> c, long timeout) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T receiveBodyNoWait(Class<T> c) {
            throw new UnsupportedOperationException();
        }
    }

    private static class TestJMSProducer implements JMSProducer {

        private boolean errorOnSend;
        private boolean disableMessageId;
        private boolean disableMessageTimestamp;
        private int deliveryMode;
        private final Map<String, Object> properties = new HashMap<>();
        private String jmsType;
        private final Map<Destination, List<Message>> messages = new HashMap<>();
        private final Map<Destination, List<byte[]>> binaryMessages = new HashMap<>();

        @Override
        public JMSProducer send(Destination destination, Message message) {
            this.messages.computeIfAbsent(destination, k -> new ArrayList<>()).add(message);
            return this;
        }

        @Override
        public JMSProducer send(Destination destination, String body) {
            throw new UnsupportedOperationException();
        }

        @Override
        public JMSProducer send(Destination destination, Map<String, Object> body) {
            throw new UnsupportedOperationException();
        }

        @Override
        public JMSProducer send(Destination destination, byte[] body) {
            if (this.errorOnSend) {
                this.errorOnSend = false;
                throw new JMSRuntimeException("Unexpected error");
            }
            this.binaryMessages.computeIfAbsent(destination, k -> new ArrayList<>()).add(body);
            return this;
        }

        @Override
        public JMSProducer send(Destination destination, Serializable body) {
            throw new UnsupportedOperationException();
        }

        @Override
        public JMSProducer setDisableMessageID(boolean value) {
            this.disableMessageId = value;
            return this;
        }

        @Override
        public boolean getDisableMessageID() {
            return this.disableMessageId;
        }

        @Override
        public JMSProducer setDisableMessageTimestamp(boolean value) {
            this.disableMessageTimestamp = value;
            return this;
        }

        @Override
        public boolean getDisableMessageTimestamp() {
            return this.disableMessageTimestamp;
        }

        @Override
        public JMSProducer setDeliveryMode(int deliveryMode) {
            this.deliveryMode = deliveryMode;
            return this;
        }

        @Override
        public int getDeliveryMode() {
            return this.deliveryMode;
        }

        @Override
        public JMSProducer setPriority(int priority) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getPriority() {
            throw new UnsupportedOperationException();
        }

        @Override
        public JMSProducer setTimeToLive(long timeToLive) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getTimeToLive() {
            throw new UnsupportedOperationException();
        }

        @Override
        public JMSProducer setDeliveryDelay(long deliveryDelay) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getDeliveryDelay() {
            throw new UnsupportedOperationException();
        }

        @Override
        public JMSProducer setAsync(CompletionListener completionListener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletionListener getAsync() {
            throw new UnsupportedOperationException();
        }

        @Override
        public JMSProducer setProperty(String name, boolean value) {
            this.properties.put(name, value);
            return this;
        }

        @Override
        public JMSProducer setProperty(String name, byte value) {
            this.properties.put(name, value);
            return this;
        }

        @Override
        public JMSProducer setProperty(String name, short value) {
            this.properties.put(name, value);
            return this;
        }

        @Override
        public JMSProducer setProperty(String name, int value) {
            this.properties.put(name, value);
            return this;
        }

        @Override
        public JMSProducer setProperty(String name, long value) {
            this.properties.put(name, value);
            return this;
        }

        @Override
        public JMSProducer setProperty(String name, float value) {
            this.properties.put(name, value);
            return this;
        }

        @Override
        public JMSProducer setProperty(String name, double value) {
            this.properties.put(name, value);
            return this;
        }

        @Override
        public JMSProducer setProperty(String name, String value) {
            this.properties.put(name, value);
            return this;
        }

        @Override
        public JMSProducer setProperty(String name, Object value) {
            this.properties.put(name, value);
            return this;
        }

        @Override
        public JMSProducer clearProperties() {
            this.properties.clear();
            return this;
        }

        @Override
        public boolean propertyExists(String name) {
            return this.properties.containsKey(name);
        }

        @Override
        public boolean getBooleanProperty(String name) {
            final Boolean result = (Boolean) this.properties.get(name);
            return result != null && result;
        }

        @Override
        public byte getByteProperty(String name) {
            final Byte result = (Byte) this.properties.get(name);
            return result == null ? 0 : result;
        }

        @Override
        public short getShortProperty(String name) {
            final Short result = (Short) this.properties.get(name);
            return result == null ? 0 : result;
        }

        @Override
        public int getIntProperty(String name) {
            final Integer result = (Integer) this.properties.get(name);
            return result == null ? 0 : result;
        }

        @Override
        public long getLongProperty(String name) {
            final Long result = (Long) this.properties.get(name);
            return result == null ? 0 : result;
        }

        @Override
        public float getFloatProperty(String name) {
            final Float result = (Float) this.properties.get(name);
            return result == null ? 0 : result;
        }

        @Override
        public double getDoubleProperty(String name) {
            final Double result = (Double) this.properties.get(name);
            return result == null ? 0 : result;
        }

        @Override
        public String getStringProperty(String name) {
            return (String) this.properties.get(name);
        }

        @Override
        public Object getObjectProperty(String name) {
            return this.properties.get(name);
        }

        @Override
        public Set<String> getPropertyNames() {
            return this.properties.keySet();
        }

        @Override
        public JMSProducer setJMSCorrelationIDAsBytes(byte[] correlationID) {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] getJMSCorrelationIDAsBytes() {
            throw new UnsupportedOperationException();
        }

        @Override
        public JMSProducer setJMSCorrelationID(String correlationID) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getJMSCorrelationID() {
            throw new UnsupportedOperationException();
        }

        @Override
        public JMSProducer setJMSType(String type) {
            this.jmsType = type;
            return this;
        }

        @Override
        public String getJMSType() {
            return this.jmsType;
        }

        @Override
        public JMSProducer setJMSReplyTo(Destination replyTo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Destination getJMSReplyTo() {
            throw new UnsupportedOperationException();
        }
    }
}
