package net.cache.bus.core.impl;

import net.cache.bus.core.*;
import net.cache.bus.core.configuration.*;
import net.cache.bus.core.impl.internal.AsynchronousCacheEventMessageConsumer;
import net.cache.bus.core.impl.internal.ImmutableCacheEntryOutputMessage;
import net.cache.bus.core.impl.internal.SynchronousCacheEventMessageConsumer;
import net.cache.bus.core.impl.internal.util.StripedRingBuffersContainer;
import net.cache.bus.core.transport.CacheBusMessageChannel;
import net.cache.bus.core.transport.CacheEntryEventConverter;
import net.cache.bus.core.transport.CacheEntryOutputMessage;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    private static final Logger logger = Logger.getLogger(DefaultCacheBus.class.getCanonicalName());
    private static final ThreadLocal<Boolean> locked = new ThreadLocal<>();

    private final CacheBusConfiguration configuration;
    private final Map<String, CacheConfiguration> cacheConfigurationsByName;
    private final Map<String, Set<String>> cachesByAliases;

    private volatile boolean started;
    private volatile CacheEventMessageConsumer messageConsumer;

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

        sendToEndpoint(cacheConfiguration, event);
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
        initializeCacheEventListeners();
        this.started = true;
        initializeInputMessageChannelSubscriber();
    }

    @Override
    public synchronized void stop() {
        if (!this.started) {
            throw new LifecycleException("Bus isn't started");
        }

        final CacheBusTransportConfiguration transportConfiguration = this.configuration.transportConfiguration();
        final CacheBusMessageChannel<CacheBusMessageChannelConfiguration> messageChannel = transportConfiguration.messageChannel();

        messageChannel.close();
        this.messageConsumer.close();

        this.started = false;
    }

    private void sendToEndpoint(final CacheConfiguration cacheConfiguration, final CacheEntryEvent<?, ?> event) {

        final CacheBusTransportConfiguration transportConfiguration = this.configuration.transportConfiguration();

        logger.fine(() -> "Event %s will be sent to endpoint".formatted(event));

        final CacheEntryEventConverter converter = transportConfiguration.converter();
        final byte[] binaryEventData = converter.toBinary(event, cacheConfiguration.cacheType().serializeValueFields());

        final CacheEntryOutputMessage outputMessage = new ImmutableCacheEntryOutputMessage(event, binaryEventData);
        final CacheBusMessageChannel<CacheBusMessageChannelConfiguration> messageChannel = transportConfiguration.messageChannel();

        messageChannel.send(outputMessage);
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

        logger.fine(() -> "Process event %s with cacheType %s".formatted(event.toString(), cacheType.name()));

        locked.set(Boolean.TRUE);

        try {
            switch (cacheType) {
                case INVALIDATED -> event.applyToInvalidatedCache(cache);
                case REPLICATED -> event.applyToReplicatedCache(cache);
            }
        } catch (RuntimeException ex) {
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
            logger.log(Level.WARNING, "Unable to deserialize message", ex);
            return null;
        }
    }

    private void initializeCacheEventListeners() {

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
                .forEach(cache -> cacheEventListenerRegistrar.registerFor(this, cache));
    }

    private void initializeInputMessageChannelSubscriber() {

        /*
         * Активируем канал сообщений
         */
        final CacheBusTransportConfiguration transportConfiguration = this.configuration.transportConfiguration();
        final CacheBusMessageChannel<CacheBusMessageChannelConfiguration> channel = transportConfiguration.messageChannel();

        channel.activate(transportConfiguration.messageChannelConfiguration());

        /*
         *  Формируем обработчик сообщений и подписываемся на входящий поток сообщений об изменениях элементов кэшей
         */
        final int buffersCount = transportConfiguration.maxConcurrentProcessingThreads();
        final int bufferCapacity = transportConfiguration.maxProcessingThreadBufferCapacity();
        final ExecutorService processingPool = transportConfiguration.processingPool();

        this.messageConsumer = transportConfiguration.useSynchronousProcessing()
                ? new SynchronousCacheEventMessageConsumer(this)
                : new AsynchronousCacheEventMessageConsumer(this, new StripedRingBuffersContainer<>(buffersCount, bufferCapacity), processingPool);

        channel.subscribe(this.messageConsumer);
    }
}
