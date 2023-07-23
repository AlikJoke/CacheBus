package ru.joke.cache.bus.core.metrics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.Callable;

/**
 * Implementation of a metrics registry that does not perform any actions.
 * Used when there is no real metrics provider specified (Dropwizard, Micrometer, etc).
 *
 * @author Alik
 * @see CacheBusMetricsRegistry
 */
public final class NoOpCacheBusMetricsRegistry implements CacheBusMetricsRegistry {

    @Override
    public void registerCounter(@Nonnull Metrics.Counter counter) {

    }

    @Override
    public void incrementCounter(@Nonnull KnownMetrics metric) {

    }

    @Override
    public void increaseCounter(@Nonnull KnownMetrics metric, double incValue) {

    }

    @Override
    public void decrementCounter(@Nonnull KnownMetrics metric) {

    }

    @Override
    public void decreaseCounter(@Nonnull KnownMetrics metric, double decValue) {

    }

    @Override
    public void registerSummary(@Nonnull Metrics.Summary summary) {

    }

    @Override
    public void putToSummary(@Nonnull KnownMetrics metric, double value) {

    }

    @Override
    public <T> void registerGauge(@Nonnull Metrics.Gauge<T> gauge) {

    }

    @Override
    public void registerTimer(@Nonnull Metrics.Timer timer) {

    }

    @Override
    public void recordExecutionTime(@Nonnull KnownMetrics metric, @Nonnull Runnable action) {
        action.run();
    }

    @Nullable
    @Override
    public <T> T recordExecutionTime(@Nonnull KnownMetrics metric, @Nonnull Callable<T> action) throws Exception {
        return action.call();
    }
}
