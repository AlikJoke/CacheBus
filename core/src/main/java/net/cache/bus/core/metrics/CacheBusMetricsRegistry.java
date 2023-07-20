package net.cache.bus.core.metrics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.Callable;

/**
 * Реестр метрик шины кэшей.
 *
 * @author Alik
 * @see KnownMetrics
 * @see Metrics
 */
public interface CacheBusMetricsRegistry {

    /**
     * Регистрирует дескриптор метрики типа "Счетчик" ({@linkplain net.cache.bus.core.metrics.Metrics.Counter}) в реестре.
     *
     * @param counter дескриптор метрики, не может быть {@code null}.
     * @see Metrics.Counter
     */
    void registerCounter(@Nonnull Metrics.Counter counter);

    /**
     * Увеличивает значение счетчика на {@code +1}.<br>
     * Перед использованием метрика должна быть зарегистрирована с помощью {@linkplain CacheBusMetricsRegistry#registerCounter(Metrics.Counter)}.
     *
     * @param metric описание метрики, не может быть {@code null}.
     */
    void incrementCounter(@Nonnull KnownMetrics metric);

    /**
     * Увеличивает значение счетчика на заданное значение {@code +incValue}.<br>
     * Перед использованием метрика должна быть зарегистрирована с помощью {@linkplain CacheBusMetricsRegistry#registerCounter(Metrics.Counter)}.
     *
     * @param metric   описание метрики, не может быть {@code null}.
     * @param incValue значение, на которое увеличивается счетчик.
     */
    void increaseCounter(@Nonnull KnownMetrics metric, double incValue);

    /**
     * Уменьшает значение счетчика на {@code -1}.<br>
     * Перед использованием метрика должна быть зарегистрирована с помощью {@linkplain CacheBusMetricsRegistry#registerCounter(Metrics.Counter)}.
     *
     * @param metric описание метрики, не может быть {@code null}.
     */
    void decrementCounter(@Nonnull KnownMetrics metric);

    /**
     * Уменьшает значение счетчика на заданное значение {@code -decValue}.<br>
     * Перед использованием метрика должна быть зарегистрирована с помощью {@linkplain CacheBusMetricsRegistry#registerCounter(Metrics.Counter)}.
     *
     * @param metric   описание метрики, не может быть {@code null}.
     * @param decValue значение, на которое уменьшается счетчик.
     */
    void decreaseCounter(@Nonnull KnownMetrics metric, double decValue);

    /**
     * Регистрирует дескриптор метрики типа {@linkplain net.cache.bus.core.metrics.Metrics.Summary} в реестре.
     *
     * @param summary дескриптор метрики, не может быть {@code null}.
     * @see Metrics.Summary
     */
    void registerSummary(@Nonnull Metrics.Summary summary);

    /**
     * Добавляет новое значение в распределение величины.
     *
     * @param metric метрика, не может быть {@code null}.
     * @param value  новое значение, добавляемое в распределение.
     */
    void putToSummary(@Nonnull KnownMetrics metric, double value);

    /**
     * Регистрирует дескриптор метрики типа {@linkplain net.cache.bus.core.metrics.Metrics.Gauge} в реестре.
     *
     * @param gauge дескриптор метрики, не может быть {@code null}.
     * @see Metrics.Gauge
     */
    <T> void registerGauge(@Nonnull Metrics.Gauge<T> gauge);

    /**
     * Регистрирует дескриптор метрики типа {@linkplain net.cache.bus.core.metrics.Metrics.Timer} в реестре.
     *
     * @param timer дескриптор метрики, не может быть {@code null}.
     * @see Metrics.Timer
     */
    void registerTimer(@Nonnull Metrics.Timer timer);

    /**
     * Выполняет замеры времени выполнения действия.
     *
     * @param metric дескриптор метрики, не может быть {@code null}.
     * @param action действие, время выполнения которого замеряется, не может быть {@code null}.
     */
    void recordExecutionTime(@Nonnull KnownMetrics metric, @Nonnull Runnable action);

    /**
     * Выполняет замеры времени выполнения действия.
     *
     * @param metric дескриптор метрики, не может быть {@code null}.
     * @param action действие, время выполнения которого замеряется, не может быть {@code null}.
     * @return возвращаемое действием значение, может быть {@code null}.
     */
    @Nullable
    <T> T recordExecutionTime(@Nonnull KnownMetrics metric, @Nonnull Callable<T> action) throws Exception;
}
