package net.cache.bus.core.impl;

import net.cache.bus.core.*;
import net.cache.bus.core.configuration.*;
import net.cache.bus.core.impl.internal.*;
import net.cache.bus.core.impl.internal.util.StripedRingBuffersContainer;
import net.cache.bus.core.transport.CacheBusMessageChannel;
import net.cache.bus.core.transport.CacheEntryEventConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Реализация шины кэшей, используемая по-умолчанию. Содержит основную логику обработки событий об
 * изменении элементов кэша и служит посредником между локальными и удаленными кэшами.
 *
 * @author Alik
 * @see CacheBus
 * @see ExtendedCacheBus
 * @see CacheBusConfiguration
 */
@ThreadSafe
public final class DefaultCacheBus implements ExtendedCacheBus {

    private static final Logger logger = LoggerFactory.getLogger(DefaultCacheBus.class);
    private static final ThreadLocal<Boolean> locked = new ThreadLocal<>();

    private final CacheBusConfiguration configuration;
    private final Map<String, CacheConfiguration> cacheConfigurationsByName;
    private final Map<String, Set<String>> cachesByAliases;

    private volatile boolean started;
    private volatile CacheEventMessageConsumer messageConsumer;
    private volatile CacheEventMessageProducer cacheEventMessageProducer;

    public DefaultCacheBus(@Nonnull CacheBusConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        final Set<CacheConfiguration> cacheConfigurations = configuration.cacheConfigurationSource().pull();
        this.cacheConfigurationsByName = cacheConfigurations
                                            .stream()
                                            .collect(Collectors.toUnmodifiableMap(CacheConfiguration::cacheName, Function.identity()));
        this.cachesByAliases =
                cacheConfigurations
                        .stream()
                        .filter(config -> !config.cacheAliases().isEmpty())
                        .flatMap(config ->
                                config.cacheAliases()
                                        .stream()
                                        .map(alias -> new String[] { alias, config.cacheName() })
                        )
                        .collect(Collectors.groupingBy(
                                info -> info[0],
                                Collectors.mapping(
                                        info -> info[1],
                                        Collectors.toSet())
                                )
                        );
    }

    @Override
    public <K extends Serializable, V extends Serializable> void send(@Nonnull CacheEntryEvent<K, V> event) {
        // To prevent calls from the receiving execution thread
        if (!this.started || locked.get() != null && locked.get()) {
            return;
        }

        final CacheConfiguration cacheConfiguration = this.cacheConfigurationsByName.get(event.cacheName());
        if (cacheConfiguration == null || !needToSendEvent(cacheConfiguration, event.eventType())) {
            return;
        }

        logger.debug("Event {} will be sent to endpoint", event);

        this.cacheEventMessageProducer.produce(cacheConfiguration, event);
    }

    @Override
    public void receive(@Nonnull byte[] binaryEventData) {

        if (!this.started) {
            return;
        }

        final CacheEntryEvent<Serializable, Serializable> event = convertFromSerializedEvent(binaryEventData);
        if (event == null) {
            return;
        }

        final CacheConfiguration cacheConfiguration = this.cacheConfigurationsByName.get(event.cacheName());
        if (cacheConfiguration != null) {
            applyEvent(event, cacheConfiguration);
        }

        // Обработка изменений по кэшам из доп. алиасов инвалидационных кэшей
        final Set<String> cachesByAlias = this.cachesByAliases.getOrDefault(event.cacheName(), Collections.emptySet());
        if (cachesByAlias.isEmpty()) {
            return;
        }

        cachesByAlias
                .stream()
                .map(this.cacheConfigurationsByName::get)
                .filter(Objects::nonNull)
                .filter(config -> config.cacheType() == CacheType.INVALIDATED)
                .forEach(config -> applyEvent(event, config));
    }

    @Override
    public void setConfiguration(@Nonnull CacheBusConfiguration configuration) {
        throw new ConfigurationException("Configuration can be set only via constructor in this implementation of CacheBus");
    }

    @Override
    @Nonnull
    public CacheBusConfiguration getConfiguration() {
        if (!this.started) {
            throw new LifecycleException("CacheBus must be in started state");
        }

        return this.configuration;
    }

    @Override
    public synchronized void start() {
        if (this.started) {
            throw new LifecycleException("Bus already started");
        }

        logger.info("Cache bus starting with configuration {}", this.configuration);

        initializeCacheEventProducer();
        initializeInputMessageChannelSubscriber();
        initializeCacheEventListeners();
        this.started = true;

        logger.info("Cache bus started");
    }

    @Override
    public synchronized void stop() {
        if (!this.started) {
            throw new LifecycleException("Bus isn't started");
        }

        logger.info("Cache bus stopping...");

        final CacheBusTransportConfiguration transportConfiguration = this.configuration.transportConfiguration();
        final CacheBusMessageChannel<CacheBusMessageChannelConfiguration> messageChannel = transportConfiguration.messageChannel();

        this.cacheEventMessageProducer.close();
        messageChannel.close();
        this.messageConsumer.close();

        this.started = false;

        logger.info("Cache bus stopped");
    }

    private boolean needToSendEvent(final CacheConfiguration cacheConfiguration, final CacheEntryEventType eventType) {
        return eventType != CacheEntryEventType.EXPIRED && eventType != CacheEntryEventType.ADDED || cacheConfiguration.cacheType() != CacheType.INVALIDATED;
    }

    private void applyEvent(final CacheEntryEvent<Serializable, Serializable> event, final CacheConfiguration cacheConfiguration) {

        final CacheProviderConfiguration providerConfiguration = this.configuration.providerConfiguration();
        final Optional<Cache<Serializable, Serializable>> cache = providerConfiguration.cacheManager().getCache(cacheConfiguration.cacheName());
        cache.ifPresent(c -> processEvent(cacheConfiguration.cacheType(), c, event));
    }

    private void processEvent(
            final CacheType cacheType,
            final Cache<Serializable, Serializable> cache,
            final CacheEntryEvent<Serializable, Serializable> event) {

        logger.debug("Process event {} with cacheType {}", event, cacheType.name());

        locked.set(Boolean.TRUE);

        try {
            switch (cacheType) {
                case INVALIDATED -> event.applyToInvalidatedCache(cache);
                case REPLICATED -> event.applyToReplicatedCache(cache);
            }
        } catch (RuntimeException ex) {
            logger.info("Exception while processing of event, will be applied like to invalidated cache; event: " + event, ex);
            // If we fail then remove value from cache by key and ack receiving of message
            event.applyToInvalidatedCache(cache);
        } finally {
            locked.remove();
        }
    }

    private CacheEntryEvent<Serializable, Serializable> convertFromSerializedEvent(final byte[] binaryEventData) {

        final CacheBusTransportConfiguration transportConfiguration = this.configuration.transportConfiguration();
        final CacheEntryEventConverter converter = transportConfiguration.converter();
        try {
            return converter.fromBinary(binaryEventData);
        } catch (RuntimeException ex) {
            logger.warn("Unable to deserialize message", ex);
            return null;
        }
    }

    private void initializeCacheEventProducer() {

        logger.debug("Cache event producer initializing...");

        final CacheBusTransportConfiguration transportConfiguration = this.configuration.transportConfiguration();
        if (transportConfiguration.useAsyncSending()) {
            final var eventBuffers = new StripedRingBuffersContainer<CacheEntryEvent<?, ?>>(
                    transportConfiguration.maxAsyncSendingThreads(),
                    transportConfiguration.maxAsyncSendingThreadBufferCapacity()
            );
            this.cacheEventMessageProducer = new AsynchronousCacheEventMessageProducer(transportConfiguration, this.cacheConfigurationsByName, eventBuffers);
            logger.debug(
                    "Cache event producer initialized with async mode: threads = {}, buffers capacity = {}",
                    eventBuffers.size(),
                    transportConfiguration.maxAsyncSendingThreadBufferCapacity()
            );
        } else {
            this.cacheEventMessageProducer = new SynchronousCacheEventMessageProducer(transportConfiguration);
            logger.debug("Cache event producer initialized with sync mode");
        }

        logger.debug("Cache event producer initialized");
    }

    private void initializeCacheEventListeners() {

        logger.debug("Cache event listeners initializing...");

        /*
         * Регистрируем подписчиков на кэшах
         */
        final CacheProviderConfiguration providerConfiguration = this.configuration.providerConfiguration();
        final CacheManager cacheManager = providerConfiguration.cacheManager();
        final CacheEventListenerRegistrar cacheEventListenerRegistrar = providerConfiguration.cacheEventListenerRegistrar();
        this.cacheConfigurationsByName
                .values()
                .stream()
                .map(CacheConfiguration::cacheName)
                .map(cacheManager::getCache)
                .flatMap(Optional::stream)
                .peek(cache -> logger.debug("Listeners will be registered for cache {}", cache))
                .forEach(cache -> cacheEventListenerRegistrar.registerFor(this, cache));

        logger.debug("Cache event listeners initialized");
    }

    private void initializeInputMessageChannelSubscriber() {

        logger.debug("Message channel initializing...");

        /*
         * Активируем канал сообщений
         */
        final CacheBusTransportConfiguration transportConfiguration = this.configuration.transportConfiguration();
        final CacheBusMessageChannel<CacheBusMessageChannelConfiguration> channel = transportConfiguration.messageChannel();

        channel.activate(transportConfiguration.messageChannelConfiguration());
        logger.debug("Message channel activated with configuration: {}", transportConfiguration.messageChannelConfiguration());

        /*
         *  Формируем обработчик сообщений и подписываемся на входящий поток сообщений об изменениях элементов кэшей
         */
        final int buffersCount = transportConfiguration.maxConcurrentProcessingThreads();
        final int bufferCapacity = transportConfiguration.maxProcessingThreadBufferCapacity();
        final ExecutorService processingPool = transportConfiguration.processingPool();

        logger.debug("Message channel consumer will be {}", transportConfiguration.useSynchronousProcessing() ? "sync" : "async");

        this.messageConsumer = transportConfiguration.useSynchronousProcessing()
                ? new SynchronousCacheEventMessageConsumer(this)
                : new AsynchronousCacheEventMessageConsumer(this, new StripedRingBuffersContainer<>(buffersCount, bufferCapacity), processingPool);

        channel.subscribe(this.messageConsumer);

        logger.debug("Message channel {} initialized", channel);
    }
}
