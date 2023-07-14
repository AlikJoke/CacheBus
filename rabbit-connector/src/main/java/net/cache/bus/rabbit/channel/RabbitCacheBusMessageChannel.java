package net.cache.bus.rabbit.channel;

import com.rabbitmq.client.*;
import net.cache.bus.core.CacheEventMessageConsumer;
import net.cache.bus.core.transport.CacheBusMessageChannel;
import net.cache.bus.core.transport.CacheEntryOutputMessage;
import net.cache.bus.core.transport.MessageChannelException;
import net.cache.bus.rabbit.configuration.RabbitCacheBusMessageChannelConfiguration;
import net.cache.bus.transport.addons.ChannelRecoveryProcessor;
import net.cache.bus.transport.addons.ConcurrentLinkedBlockingQueue;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.cache.bus.transport.ChannelConstants.MESSAGE_TYPE;
import static net.cache.bus.transport.ChannelConstants.POLL_CHANNEL_TIMEOUT;

/**
 * Реализация канала сообщений на основе RabbitMQ.
 *
 * @author Alik
 * @see RabbitCacheBusMessageChannelConfiguration
 * @see CacheBusMessageChannel
 */
@ThreadSafe
public final class RabbitCacheBusMessageChannel implements CacheBusMessageChannel<RabbitCacheBusMessageChannelConfiguration> {

    private static final Logger logger = Logger.getLogger(RabbitCacheBusMessageChannel.class.getCanonicalName());

    private static final String HOST_PROPERTY = "host";
    private static final String HASH_KEY_PROPERTY = "hash";

    private volatile RabbitConsumerSessionConfiguration consumerSessionConfiguration;
    private volatile RabbitProducerSessionConfiguration producerSessionConfiguration;
    private Future<?> subscribingTask;

    @Override
    public synchronized void activate(@Nonnull RabbitCacheBusMessageChannelConfiguration rabbitConfiguration) {
        logger.info(() -> "Activation of channel was called");

        if (this.producerSessionConfiguration != null || this.consumerSessionConfiguration != null) {
            throw new MessageChannelException("Channel already activated");
        }

        initializeConsumerSession(rabbitConfiguration);
        initializeProducerSession(rabbitConfiguration);
    }

    @Override
    public void send(@Nonnull CacheEntryOutputMessage eventOutputMessage) {

        final RabbitProducerSessionConfiguration sessionConfiguration = this.producerSessionConfiguration;
        if (sessionConfiguration == null) {
            throw new MessageChannelException("Channel not activated");
        }

        final AMQP.BasicProperties basicProperties = createProducerProperties(sessionConfiguration, eventOutputMessage.messageHashKey());

        boolean retry;
        do {
            retry = false;
            Channel channel = null;
            try {
                channel = sessionConfiguration.sendingChannels.poll(POLL_CHANNEL_TIMEOUT, TimeUnit.SECONDS);
                if (channel == null) {
                    logger.info("Could not retrieve channel in %d seconds timeout, maybe you should increase count of available channels in rabbit channel configuration?".formatted(POLL_CHANNEL_TIMEOUT));
                    channel = sessionConfiguration.sendingChannels.take();
                }

                final String channelName = sessionConfiguration.sharedConfiguration.channel();
                channel.basicPublish(
                        "",
                        channelName,
                        basicProperties,
                        eventOutputMessage.cacheEntryMessageBody()
                );

                logger.fine(() -> "Message %s was sent to topic: %s".formatted(eventOutputMessage, channelName));
            } catch (IOException ex) {
                recoverProducerSession(ex, sessionConfiguration);
                retry = true;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                if (channel != null && !retry) {
                    sessionConfiguration.sendingChannels.offer(channel);
                }
            }
        } while (retry);
    }

    @Override
    public synchronized void subscribe(@Nonnull CacheEventMessageConsumer consumer) {
        if (this.consumerSessionConfiguration != null) {
            throw new MessageChannelException("Channel not activated");
        } else if (this.subscribingTask != null) {
            throw new MessageChannelException("Already subscribed to channel");
        }

        final var sharedConfig = this.consumerSessionConfiguration.sharedConfiguration;
        this.subscribingTask = sharedConfig.subscribingPool().submit(() -> listenUntilNotClosed(consumer));
    }

    @Override
    public synchronized void close() {
        logger.info(() -> "Channel closure was called");

        if (this.consumerSessionConfiguration == null || this.producerSessionConfiguration == null) {
            throw new MessageChannelException("Already in closed state");
        }

        final RabbitConsumerSessionConfiguration consumerSessionConfiguration = this.consumerSessionConfiguration;
        this.consumerSessionConfiguration = null;
        consumerSessionConfiguration.close();

        final RabbitProducerSessionConfiguration producerSessionConfiguration = this.producerSessionConfiguration;
        this.producerSessionConfiguration = null;
        producerSessionConfiguration.close();

        if (this.subscribingTask != null) {
            this.subscribingTask.cancel(true);
            this.subscribingTask = null;
        }
    }

    private void listenUntilNotClosed(final CacheEventMessageConsumer consumer) {

        logger.info(() -> "Subscribe was called");

        RabbitConsumerSessionConfiguration sessionConfiguration;
        while ((sessionConfiguration = this.consumerSessionConfiguration) != null) {

            try {
                final String hostName = sessionConfiguration.hostName;
                final var rabbitConsumer = new DefaultConsumer(sessionConfiguration.inputChannel) {
                    @Override
                    public void handleDelivery(
                            final String consumerTag,
                            final Envelope envelope,
                            final AMQP.BasicProperties properties,
                            final byte[] body) {

                        final Map<String, Object> headers = properties.getHeaders();
                        if (!MESSAGE_TYPE.equals(properties.getType())
                                || !hostName.equals(headers.get(HOST_PROPERTY))) {
                            return;
                        }

                        final Integer messageHash = (Integer) headers.get(HASH_KEY_PROPERTY);
                        consumer.accept(messageHash, body);
                    }
                };

                // the error will never happen, so we can use auto ack
                sessionConfiguration.inputChannel.basicConsume(
                        sessionConfiguration.sharedConfiguration.channel(),
                        true,
                        null,
                        true,
                        false,
                        Collections.emptyMap(),
                        rabbitConsumer
                );
            } catch (IOException ex) {
                recoverConsumerSession(ex);
            }
        }
    }

    private AMQP.BasicProperties createProducerProperties(
            final RabbitSessionConfiguration sessionConfiguration,
            final int messageHashKey) {

        return new AMQP.BasicProperties()
                            .builder()
                            .headers(
                                    Map.of(
                                            HOST_PROPERTY, sessionConfiguration.hostName,
                                            HASH_KEY_PROPERTY, messageHashKey
                                    )
                            )
                            .type(MESSAGE_TYPE)
                            .deliveryMode(2)
                            .build();
    }

    private void initializeConsumerSession(final RabbitCacheBusMessageChannelConfiguration rabbitConfiguration) {
        try {
            this.consumerSessionConfiguration = new RabbitConsumerSessionConfiguration(rabbitConfiguration);
        } catch (IOException | TimeoutException ex) {
            logger.log(Level.ALL, "Unable to activate channel", ex);
            throw new MessageChannelException(ex);
        }
    }

    private void initializeProducerSession(final RabbitCacheBusMessageChannelConfiguration rabbitConfiguration) {
        try {
            this.producerSessionConfiguration = new RabbitProducerSessionConfiguration(rabbitConfiguration);
        } catch (IOException | TimeoutException ex) {
            logger.log(Level.ALL, "Unable to activate channel", ex);
            throw new MessageChannelException(ex);
        }
    }

    private void recoverProducerSession(
            final Exception ex,
            final RabbitProducerSessionConfiguration sessionConfiguration) {

        // Синхронизация на объекте конфигурации на случай параллельных потоков отправки сообщений
        synchronized (sessionConfiguration) {

            // Если не совпало, значит другой поток уже восстановил соединение => можно пытаться отправить сообщение
            if (this.producerSessionConfiguration != sessionConfiguration) {
                return;
            }

            // С высокой вероятностью, если есть проблема с одним каналом, то будут проблемы и с другими
            // из-за общего соединения, поэтому полностью пересоздаем каналы
            final ChannelRecoveryProcessor recoveryProcessor = new ChannelRecoveryProcessor(
                    sessionConfiguration::close,
                    () -> initializeProducerSession(sessionConfiguration.sharedConfiguration),
                    sessionConfiguration.sharedConfiguration.reconnectTimeoutMs()
            );

            try {
                recoveryProcessor.recover(ex);
            } catch (RuntimeException e) {
                throw new MessageChannelException(e);
            }
        }
    }

    private void recoverConsumerSession(final Exception ex) {

        // Поток прервали
        final RabbitSessionConfiguration sessionConfiguration = this.consumerSessionConfiguration;
        if (sessionConfiguration == null) {
            return;
        }

        final ChannelRecoveryProcessor recoveryProcessor = new ChannelRecoveryProcessor(
                sessionConfiguration::close,
                () -> initializeConsumerSession(sessionConfiguration.sharedConfiguration),
                Integer.MAX_VALUE
        );

        try {
            recoveryProcessor.recover(ex);
        } catch (RuntimeException e) {
            throw new MessageChannelException(e);
        }
    }

    private static abstract class RabbitSessionConfiguration implements AutoCloseable {

        protected final Connection rabbitConnection;
        protected final String hostName;
        protected final RabbitCacheBusMessageChannelConfiguration sharedConfiguration;

        private RabbitSessionConfiguration(@Nonnull final RabbitCacheBusMessageChannelConfiguration sharedConfiguration) throws IOException, TimeoutException {
            this.sharedConfiguration = sharedConfiguration;
            this.rabbitConnection = sharedConfiguration.connectionFactory().newConnection();
            this.hostName = sharedConfiguration.hostNameResolver().resolve();
        }

        @Override
        public void close() {
            try {
                this.rabbitConnection.close();
            } catch (IOException ex) {
                logger.log(Level.ALL, "Unable to close connection to rabbit", ex);
            }
        }
    }

    private static class RabbitProducerSessionConfiguration extends RabbitSessionConfiguration {

        private final ConcurrentLinkedBlockingQueue<Channel> sendingChannels;

        private RabbitProducerSessionConfiguration(@Nonnull RabbitCacheBusMessageChannelConfiguration sharedConfiguration) throws IOException, TimeoutException {
            super(sharedConfiguration);
            this.sendingChannels = new ConcurrentLinkedBlockingQueue<>();
            for (int i = 0; i < sharedConfiguration.availableChannelsCount() - 1; i++) {
                this.sendingChannels.offer(this.rabbitConnection.createChannel());
            }
        }
    }

    private static class RabbitConsumerSessionConfiguration extends RabbitSessionConfiguration {

        private final Channel inputChannel;

        private RabbitConsumerSessionConfiguration(@Nonnull RabbitCacheBusMessageChannelConfiguration sharedConfiguration) throws IOException, TimeoutException {
            super(sharedConfiguration);
            this.inputChannel = this.rabbitConnection.createChannel();
            this.inputChannel.queueDeclare(sharedConfiguration.channel(), false, false, false, Collections.emptyMap());
        }
    }
}
