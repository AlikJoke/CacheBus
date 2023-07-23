package ru.joke.cache.bus.core.metrics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.Callable;

/**
 * Registry of cache bus metrics.
 *
 * @author Alik
 * @see KnownMetrics
 * @see Metrics
 */
public interface CacheBusMetricsRegistry {

    /**
     * Registers a "Counter" metric descriptor ({@linkplain Metrics.Counter}) in the registry.
     *
     * @param counter the metric descriptor, cannot be {@code null}.
     * @see Metrics.Counter
     */
    void registerCounter(@Nonnull Metrics.Counter counter);

    /**
     * Increases the counter value by {@code +1}.<br>
     * The metric must be registered using
     * {@linkplain CacheBusMetricsRegistry#registerCounter(Metrics.Counter)} before use.
     *
     * @param metric the metric description, cannot be {@code null}.
     */
    void incrementCounter(@Nonnull KnownMetrics metric);

    /**
     * Increases the counter value by the specified {@code incValue}.<br>
     * The metric must be registered using {@linkplain CacheBusMetricsRegistry#registerCounter(Metrics.Counter)} before use.
     *
     * @param metric   the metric description, cannot be {@code null}.
     * @param incValue the value by which the counter is increased.
     */
    void increaseCounter(@Nonnull KnownMetrics metric, double incValue);

    /**
     * Decreases the counter value by {@code -1}.<br>
     * The metric must be registered using {@linkplain CacheBusMetricsRegistry#registerCounter(Metrics.Counter)} before use.
     *
     * @param metric the metric description, cannot be {@code null}.
     */
    void decrementCounter(@Nonnull KnownMetrics metric);

    /**
     * Decreases the counter value the specified {@code decValue}.<br>
     * The metric must be registered using {@linkplain CacheBusMetricsRegistry#registerCounter(Metrics.Counter)} before use.
     *
     * @param metric   the metric description, cannot be {@code null}.
     * @param decValue the value by which the counter decreased.
     */
    void decreaseCounter(@Nonnull KnownMetrics metric, double decValue);

    /**
     * Registers a {@linkplain Metrics.Summary} metric descriptor in the registry.
     *
     * @param summary the metric descriptor, cannot be {@code null}.
     * @see Metrics.Summary
     */
    void registerSummary(@Nonnull Metrics.Summary summary);

    /**
     * Adds a new value to the distribution of a quantity.
     *
     * @param metric the metric, cannot be {@code null}.
     * @param value  the new value to be added to the distribution.
     */
    void putToSummary(@Nonnull KnownMetrics metric, double value);

    /**
     * Registers a {@linkplain Metrics.Gauge} metric descriptor in the registry.
     *
     * @param gauge the metric descriptor, cannot be {@code null}.
     * @see Metrics.Gauge
     */
    <T> void registerGauge(@Nonnull Metrics.Gauge<T> gauge);

    /**
     * Registers a {@linkplain Metrics.Timer} metric descriptor in the registry.
     *
     * @param timer the metric descriptor, cannot {@code null}.
     * @see Metrics.Timer
     */
    void registerTimer(@Nonnull Metrics.Timer timer);

    /**
     * Measures the execution time of an action.
     *
     * @param metric the metric descriptor, cannot be {@code null}.
     * @param action the action whose execution time is measured, cannot be {@code null}.
     */
    void recordExecutionTime(@Nonnull KnownMetrics metric, @Nonnull Runnable action);

    /**
     * Measures the execution time of an action.
     *
     * @param metric the metric descriptor, cannot be {@code null}.
     * @param action the action whose execution time is measured, cannot be {@code null}.
     * @return the value returned by the action, can be {@code null}.
     */
    @Nullable
    <T> T recordExecutionTime(@Nonnull KnownMetrics metric, @Nonnull Callable<T> action) throws Exception;
}
