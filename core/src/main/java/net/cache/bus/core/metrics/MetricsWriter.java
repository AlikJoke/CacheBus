package net.cache.bus.core.metrics;

import javax.annotation.Nonnull;

/**
 * Компонент шины, ведущий учет метрик своего функционирования.
 *
 * @author Alik
 * @see CacheBusMetricsRegistry
 */
public interface MetricsWriter {

    /**
     * Устанавливает используемый для ведения метрик реестр.
     *
     * @param registry реестр, используемый для ведения метрик, не может быть {@code null}.
     */
    void setMetrics(@Nonnull CacheBusMetricsRegistry registry);
}
