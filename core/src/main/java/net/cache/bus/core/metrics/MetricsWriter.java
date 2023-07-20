package net.cache.bus.core.metrics;

import javax.annotation.Nonnull;

public interface MetricsWriter {

    void setMetrics(@Nonnull CacheBusMetricsRegistry registry);
}
