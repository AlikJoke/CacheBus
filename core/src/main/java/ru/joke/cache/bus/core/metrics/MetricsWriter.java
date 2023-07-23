package ru.joke.cache.bus.core.metrics;

import javax.annotation.Nonnull;

/**
 * A bus component that tracks metrics of its operation.
 *
 * @author Alik
 * @see CacheBusMetricsRegistry
 */
public interface MetricsWriter {

    /**
     * Sets the registry used for tracking metrics.
     *
     * @param registry the registry used for tracking metrics, cannot be {@code null}.
     */
    void setMetrics(@Nonnull CacheBusMetricsRegistry registry);
}
