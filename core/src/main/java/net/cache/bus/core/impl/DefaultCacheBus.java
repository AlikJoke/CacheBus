package net.cache.bus.core.impl;

import net.cache.bus.core.*;
import net.cache.bus.core.configuration.*;
import net.cache.bus.core.impl.internal.*;
import net.cache.bus.core.impl.internal.util.StripedRingBuffersContainer;
import net.cache.bus.core.metrics.CacheBusMetricsRegistry;
import net.cache.bus.core.metrics.KnownMetrics;
import net.cache.bus.core.metrics.Metrics;
import net.cache.bus.core.metrics.MetricsWriter;
import net.cache.bus.core.state.CacheBusState;
import net.cache.bus.core.state.ComponentState;
import net.cache.bus.core.transport.CacheBusMessageChannel;
import net.cache.bus.core.transport.CacheEntryEventConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
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

    private static final String CACHE_BUS_LABEL = "cache-bus";
    private static final ThreadLocal<Boolean> locked = new ThreadLocal<>();

    private final String id;
    private final CacheBusConfiguration configuration;
    private final Map<String, CacheConfiguration> cacheConfigurationsByName;
    private final Map<String, Set<String>> cachesByAliases;
    private final CompositeCacheBusState state;
    private final CacheBusMetricsRegistry metrics;
    private final CacheEntryEventTimestampStore eventTimestampStore;

    private volatile boolean started;
    private volatile CacheEventMessageConsumer messageConsumer;
    private volatile CacheEventMessageProducer cacheEventMessageProducer;

    public DefaultCacheBus(@Nonnull CacheBusConfiguration configuration) {
        this.id = CACHE_BUS_LABEL + "_" + UUID.randomUUID();
        this.state = new CompositeCacheBusState(this);
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.metrics = configuration.metricsRegistry();

        final Set<CacheConfiguration> cacheConfigurations = configuration.cacheConfigurationSource().pull();
        final Set<CacheConfiguration> cacheConfigurationsWithStampBasedComparison =
                cacheConfigurations
                        .stream()
                        .filter(CacheConfiguration::useStampBasedComparison)
                        .collect(Collectors.toSet());
        this.eventTimestampStore = new InMemoryCacheEntryEventTimestampStore(cacheConfigurationsWithStampBasedComparison);
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

        final CacheConfiguration cacheConfiguration = this.cacheConfigurationsByName.get(event.cacheName());
        if (!started || cacheConfiguration == null) {
            return;
        }

        if (cacheConfiguration.useStampBasedComparison()) {
            this.eventTimestampStore.save(event);
        }

        // To prevent calls from the receiving execution thread
        if (locked.get() != null && locked.get()) {
            return;
        }

        this.metrics.incrementCounter(KnownMetrics.LOCAL_EVENTS_COMMON_COUNT);
        if (!needToSendEvent(cacheConfiguration, event.eventType())) {
            return;
        }

        logger.debug("Event {} will be sent to endpoint", event);

        this.metrics.incrementCounter(
                cacheConfiguration.cacheType() == CacheType.INVALIDATED
                        ? KnownMetrics.FILTERED_INV_LOCAL_EVENTS_COUNT
                        : KnownMetrics.FILTERED_REPL_LOCAL_EVENTS_COUNT
        );

        this.cacheEventMessageProducer.produce(cacheConfiguration, event);
    }

    @Override
    public void receive(@Nonnull byte[] binaryEventData) {

        if (!this.started) {
            return;
        }

        this.metrics.putToSummary(KnownMetrics.CONSUMED_BYTES, binaryEventData.length);
        this.metrics.incrementCounter(KnownMetrics.REMOTE_EVENTS_COMMON_COUNT);

        final CacheEntryEvent<Serializable, Serializable> event = convertFromSerializedEvent(binaryEventData);
        if (event == null) {
            this.metrics.incrementCounter(KnownMetrics.ERROR_EVENTS_COUNT);
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
    public void withConfiguration(@Nonnull CacheBusConfiguration configuration) {
        throw new ConfigurationException("Configuration can be set only via constructor in this implementation of CacheBus");
    }

    @Override
    @Nonnull
    public CacheBusConfiguration configuration() {
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

        registerMetrics();
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

        unregisterCacheEventListeners();

        final CacheBusTransportConfiguration transportConfiguration = this.configuration.transportConfiguration();
        final CacheBusMessageChannel<CacheBusMessageChannelConfiguration> messageChannel = transportConfiguration.messageChannel();

        this.cacheEventMessageProducer.close();
        messageChannel.close();
        this.messageConsumer.close();

        this.started = false;

        logger.info("Cache bus stopped");
    }

    @Nonnull
    @Override
    public CacheBusState state() {
        return this.state;
    }

    private boolean needToSendEvent(final CacheConfiguration cacheConfiguration, final CacheEntryEventType eventType) {
        return eventType != CacheEntryEventType.EXPIRED && eventType != CacheEntryEventType.ADDED || cacheConfiguration.cacheType() != CacheType.INVALIDATED;
    }

    private void applyEvent(final CacheEntryEvent<Serializable, Serializable> event, final CacheConfiguration cacheConfiguration) {

        final CacheProviderConfiguration providerConfiguration = this.configuration.providerConfiguration();
        final Optional<Cache<Serializable, Serializable>> cache = providerConfiguration.cacheManager().getCache(cacheConfiguration.cacheName());
        cache.ifPresent(c -> processEvent(cacheConfiguration, c, event));
    }

    private void processEvent(
            final CacheConfiguration cacheConfiguration,
            final Cache<Serializable, Serializable> cache,
            final CacheEntryEvent<Serializable, Serializable> event) {

        logger.debug("Process event {} with cacheType {}", event, cacheConfiguration.cacheType().name());

        locked.set(Boolean.TRUE);

        final long storedLocalTimestamp =
                cacheConfiguration.useStampBasedComparison()
                    ? this.eventTimestampStore.load(cache.getName(), event.key())
                    : -1;
        if (event.eventTime() <= storedLocalTimestamp) {
            return;
        }

        try {
            switch (cacheConfiguration.cacheType()) {
                case INVALIDATED -> {
                    event.applyToInvalidatedCache(cache);
                    this.metrics.incrementCounter(KnownMetrics.APPLIED_INV_EVENTS_COUNT);
                }
                case REPLICATED -> {
                    event.applyToReplicatedCache(cache);
                    this.metrics.incrementCounter(KnownMetrics.APPLIED_REPL_EVENTS_COUNT);
                }
            }
        } catch (RuntimeException ex) {
            logger.info("Exception while processing of event, will be applied like to invalidated cache; event: " + event, ex);
            // If we fail then remove value from cache by key and ack receiving of message
            event.applyToInvalidatedCache(cache);
            this.metrics.incrementCounter(KnownMetrics.APPLIED_AS_INV_EVENT_FALLBACK_COUNT);
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
            this.cacheEventMessageProducer = new AsynchronousCacheEventMessageProducer(this.metrics, transportConfiguration, this.cacheConfigurationsByName, eventBuffers);

            logger.debug(
                    "Cache event producer initialized with async mode: threads = {}, buffers capacity = {}",
                    eventBuffers.size(),
                    transportConfiguration.maxAsyncSendingThreadBufferCapacity()
            );
        } else {
            this.cacheEventMessageProducer = new SynchronousCacheEventMessageProducer(this.metrics, transportConfiguration);
            logger.debug("Cache event producer initialized with sync mode");
        }

        logger.debug("Cache event producer initialized");
    }

    private void initializeCacheEventListeners() {

        logger.debug("Cache event listeners initializing...");

        // Регистрируем подписчиков на кэшах
        executeWithCacheEventListeners(
                (registrar, cache) -> {
                    registrar.registerFor(this, cache);
                    this.metrics.incrementCounter(KnownMetrics.MANAGED_CACHES_COUNT);
                },
                "registered"
        );

        logger.debug("Cache event listeners initialized");
    }

    private void initializeInputMessageChannelSubscriber() {

        logger.debug("Message channel initializing...");

        /*
         * Активируем канал сообщений
         */
        final CacheBusTransportConfiguration transportConfiguration = this.configuration.transportConfiguration();
        final CacheBusMessageChannel<CacheBusMessageChannelConfiguration> channel = transportConfiguration.messageChannel();

        if (channel instanceof MetricsWriter channelWithMetrics) {
            channelWithMetrics.setMetrics(this.metrics);
        }

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
                : new AsynchronousCacheEventMessageConsumer(this, this.metrics, new StripedRingBuffersContainer<>(buffersCount, bufferCapacity), processingPool);

        channel.subscribe(this.messageConsumer);

        logger.debug("Message channel {} initialized", channel);
    }

    private void unregisterCacheEventListeners() {

        logger.debug("Cache event listeners unregistering...");

        executeWithCacheEventListeners(
                (registrar, cache) -> {
                    registrar.unregisterFor(this, cache);
                    this.metrics.decrementCounter(KnownMetrics.MANAGED_CACHES_COUNT);
                },
                "unregistered");

        logger.debug("Cache event listeners unregistered");
    }

    private void executeWithCacheEventListeners(
            final BiConsumer<CacheEventListenerRegistrar, Cache<?, ?>> action,
            final String logLabel) {

        final CacheProviderConfiguration providerConfiguration = this.configuration.providerConfiguration();
        final CacheManager cacheManager = providerConfiguration.cacheManager();
        final CacheEventListenerRegistrar cacheEventListenerRegistrar = providerConfiguration.cacheEventListenerRegistrar();
        this.cacheConfigurationsByName
                .values()
                .stream()
                .map(CacheConfiguration::cacheName)
                .map(cacheManager::getCache)
                .flatMap(Optional::stream)
                .peek(cache -> logger.debug("Listeners will be {} for cache {}", logLabel, cache))
                .forEach(cache -> action.accept(cacheEventListenerRegistrar, cache));
    }

    private void registerMetrics() {
        this.metrics.registerCounter(new Metrics.Counter(KnownMetrics.MANAGED_CACHES_COUNT));
        this.metrics.registerCounter(new Metrics.Counter(KnownMetrics.LOCAL_EVENTS_COMMON_COUNT));
        this.metrics.registerCounter(new Metrics.Counter(KnownMetrics.FILTERED_INV_LOCAL_EVENTS_COUNT));
        this.metrics.registerCounter(new Metrics.Counter(KnownMetrics.FILTERED_REPL_LOCAL_EVENTS_COUNT));
        this.metrics.registerCounter(new Metrics.Counter(KnownMetrics.REMOTE_EVENTS_COMMON_COUNT));
        this.metrics.registerCounter(new Metrics.Counter(KnownMetrics.ERROR_EVENTS_COUNT));
        this.metrics.registerCounter(new Metrics.Counter(KnownMetrics.APPLIED_INV_EVENTS_COUNT));
        this.metrics.registerCounter(new Metrics.Counter(KnownMetrics.APPLIED_REPL_EVENTS_COUNT));
        this.metrics.registerSummary(new Metrics.Summary(KnownMetrics.CONSUMED_BYTES, "bytes"));
    }

    private record CompositeCacheBusState(@Nonnull DefaultCacheBus cacheBus) implements CacheBusState {

        @Nonnull
        @Override
        public String componentId() {
            return this.cacheBus.id;
        }

        @Nonnull
        @Override
        public Status status() {
            if (!this.cacheBus.started) {
                return Status.DOWN;
            }

            // Если один из компонентов упал / не запустился / попал в неисправимое состояние,
            // то шина считается активной, но не способной продолжать нормальную работу
            if (channelState().status() == Status.DOWN
                    || channelState().status() == Status.UP_FATAL_BROKEN
                    || processingQueueState().status() == Status.DOWN
                    || processingQueueState().status() == Status.UP_FATAL_BROKEN
                    || sendingQueueState().status() == Status.DOWN
                    || sendingQueueState().status() == Status.UP_FATAL_BROKEN
                    || cacheManagerState().status() == Status.DOWN
                    || cacheManagerState().status() == Status.UP_FATAL_BROKEN) {
                return Status.UP_FATAL_BROKEN;
            }

            return Status.UP_OK;
        }

        @Nonnull
        @Override
        public List<SeverityInfo> severities() {
            final var channelSeverities = channelState().severities();
            final var processingQueueSeverities = processingQueueState().severities();
            final var sendingQueueSeverities = sendingQueueState().severities();
            final var cacheManagerSeverities = cacheManagerState().severities();

            final List<SeverityInfo> severities = new ArrayList<>(channelSeverities.size() + processingQueueSeverities.size() + cacheManagerSeverities.size() + sendingQueueSeverities.size());
            severities.addAll(channelSeverities);
            severities.addAll(cacheManagerSeverities);
            severities.addAll(sendingQueueSeverities);
            severities.addAll(processingQueueSeverities);

            return severities;
        }

        @Nonnull
        @Override
        public ComponentState channelState() {
            final var transportConfig = this.cacheBus.configuration.transportConfiguration();
            return transportConfig.messageChannel().state();
        }

        @Nonnull
        @Override
        public ComponentState processingQueueState() {
            return this.cacheBus.messageConsumer.state();
        }

        @Nonnull
        @Override
        public ComponentState sendingQueueState() {
            return this.cacheBus.cacheEventMessageProducer.state();
        }

        @Override
        @Nonnull
        public ComponentState cacheManagerState() {
            final var providerConfig = this.cacheBus.configuration.providerConfiguration();
            return providerConfig.cacheManager().state();
        }
    }
}
