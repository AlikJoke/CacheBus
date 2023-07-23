package net.cache.bus.metrics.micrometer;

import io.micrometer.core.instrument.*;
import net.cache.bus.core.metrics.CacheBusMetricsRegistry;
import net.cache.bus.core.metrics.KnownMetrics;
import net.cache.bus.core.metrics.Metrics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of a metrics registry for the cache bus based on the Micrometer Metrics library.
 *
 * @author Alik
 * @see CacheBusMetricsRegistry
 * @see MeterRegistry
 */
public final class MicrometerCacheBusMetricsRegistry implements CacheBusMetricsRegistry {

    private final Map<String, Counter> countersMap = new ConcurrentHashMap<>();
    private final Map<String, DistributionSummary> summariesMap = new ConcurrentHashMap<>();
    private final Map<String, Timer> timersMap = new ConcurrentHashMap<>();

    private final MeterRegistry meterRegistry;

    public MicrometerCacheBusMetricsRegistry(@Nonnull MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
    }

    @Override
    public void registerCounter(@Nonnull Metrics.Counter counter) {
        final Counter registeredCounter =
                Counter
                        .builder(counter.id())
                            .description(counter.description())
                            .tags(counter.tags().toArray(new String[0]))
                        .register(this.meterRegistry);
        this.countersMap.put(counter.id(), registeredCounter);
    }

    @Override
    public void incrementCounter(@Nonnull KnownMetrics metric) {
        findCounter(metric.id()).increment();
    }

    @Override
    public void increaseCounter(@Nonnull KnownMetrics metric, double incValue) {
        findCounter(metric.id()).increment(incValue);
    }

    @Override
    public void decrementCounter(@Nonnull KnownMetrics metric) {
        findCounter(metric.id()).increment(-1);
    }

    @Override
    public void decreaseCounter(@Nonnull KnownMetrics metric, double decValue) {
        findCounter(metric.id()).increment(-decValue);
    }

    @Override
    public void registerSummary(@Nonnull Metrics.Summary summary) {
        final DistributionSummary registeredSummary =
                DistributionSummary
                    .builder(summary.id())
                        .baseUnit(summary.getBaseUnit())
                        .description(summary.description())
                        .tags(summary.tags().toArray(new String[0]))
                    .register(this.meterRegistry);
        this.summariesMap.put(summary.id(), registeredSummary);
    }

    @Override
    public void putToSummary(@Nonnull KnownMetrics metric, double value) {
        Objects.requireNonNull(this.summariesMap.get(metric.id()), "Summary must be registered before use").record(value);
    }

    @Override
    public <T> void registerGauge(@Nonnull Metrics.Gauge<T> gauge) {
        Gauge
                .builder(gauge.id(), gauge.getMeterObj(), gauge.getMeterValueFunc())
                    .description(gauge.description())
                    .tags(gauge.tags().toArray(new String[0]))
                .register(this.meterRegistry);
    }

    @Override
    public void registerTimer(@Nonnull Metrics.Timer timer) {
        final Timer registeredTimer =
                Timer
                        .builder(timer.id())
                            .description(timer.description())
                            .tags(timer.tags().toArray(new String[0]))
                        .register(this.meterRegistry);
        this.timersMap.put(timer.id(), registeredTimer);
    }

    @Override
    public void recordExecutionTime(@Nonnull KnownMetrics metric, @Nonnull Runnable action) {
        findTimer(metric.id()).record(action);
    }

    @Nullable
    @Override
    public <T> T recordExecutionTime(@Nonnull KnownMetrics metric, @Nonnull Callable<T> action) throws Exception {
        return findTimer(metric.id()).recordCallable(action);
    }

    private Timer findTimer(final String timerId) {
        return Objects.requireNonNull(this.timersMap.get(timerId), "Timer must be registered before use");
    }

    private Counter findCounter(final String counterId) {
        return Objects.requireNonNull(this.countersMap.get(counterId), "Counter must be registered before use");
    }
}
