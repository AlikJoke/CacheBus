package net.cache.bus.core.metrics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.Callable;

public interface CacheBusMetricsRegistry {

    void registerCounter(@Nonnull Metrics.Counter counter);

    void incrementCounter(@Nonnull KnownMetrics metric);

    void increaseCounter(@Nonnull KnownMetrics metric, int incValue);

    void decrementCounter(@Nonnull KnownMetrics metric);

    void decreaseCounter(@Nonnull KnownMetrics metric, int decValue);

    void registerSummary(@Nonnull Metrics.Summary summary);

    void putToSummary(@Nonnull KnownMetrics metric, double value);

    <T> void registerGauge(@Nonnull Metrics.Gauge<T> gauge);

    void registerTimer(@Nonnull Metrics.Timer timer);

    void recordExecutionTime(@Nonnull KnownMetrics metric, @Nonnull Runnable action);

    @Nullable
    <T> T recordExecutionTime(@Nonnull KnownMetrics metric, @Nonnull Callable<T> action) throws Exception;
}
