package ru.joke.cache.bus.core.metrics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.function.ToDoubleFunction;

/**
 * A container class for descriptors of different metric types.
 *
 * @author Alik
 */
public abstract class Metrics {

    /**
     * A descriptor for a "counter" metric, which represents a single positive or negative number.
     *
     * @author Alik
     */
    public static class Counter extends Metric {

        public Counter(@Nonnull KnownMetrics counterMetric) {
            this(counterMetric.id(), counterMetric.description(), counterMetric.tags());
        }

        public Counter(@Nonnull String id,
                       @Nonnull String description,
                       @Nonnull List<String> tags) {
            super(id, description, tags);
        }
    }

    /**
     * Descriptor for a metric that allows retrieving the current value of a measurable quantity of an object.
     *
     * @param <T> the type of the object whose current value being measured
     * @author Alik
     */
    public static class Gauge<T> extends Metric {

        private final T meterObj;
        private final ToDoubleFunction<T> meterValueFunc;

        public Gauge(@Nonnull KnownMetrics gaugeMetric,
                     @Nonnull T meterObj,
                     @Nonnull ToDoubleFunction<T> meterValueFunc) {
            this(gaugeMetric.id(), meterObj, meterValueFunc, gaugeMetric.description(), gaugeMetric.tags());
        }

        public Gauge(@Nonnull String id,
                     @Nonnull T meterObj,
                     @Nonnull ToDoubleFunction<T> meterValueFunc,
                     @Nonnull String description,
                     @Nonnull List<String> tags) {
            super(id, description, tags);
            this.meterObj = meterObj;
            this.meterValueFunc = meterValueFunc;
        }

        @Nonnull
        public T getMeterObj() {
            return meterObj;
        }

        @Nonnull
        public ToDoubleFunction<T> getMeterValueFunc() {
            return meterValueFunc;
        }
    }

    /**
     * A metric of type "Distribution Summary".
     *
     * @author Alik
     */
    public static class Summary extends Metric {

        private final String baseUnit;

        public Summary(@Nonnull KnownMetrics summaryMetric,
                       @Nullable String baseUnit) {
            this(summaryMetric.id(), baseUnit, summaryMetric.description(), summaryMetric.tags());
        }

        public Summary(@Nonnull String id,
                       @Nullable String baseUnit,
                       @Nonnull String description,
                       @Nonnull List<String> tags) {
            super(id, description, tags);
            this.baseUnit = baseUnit;
        }

        @Nullable
        public String getBaseUnit() {
            return baseUnit;
        }
    }

    /**
     * A "Timer" metric based on measuring the execution time of a task.
     *
     * @author Alik
     */
    public static class Timer extends Metric {

        public Timer(@Nonnull KnownMetrics timerMetric) {
            this(timerMetric.id(), timerMetric.description(), timerMetric.tags());
        }

        public Timer(@Nonnull String id,
                     @Nonnull String description,
                     @Nonnull List<String> tags) {
            super(id, description, tags);
        }
    }

    private static class Metric {

        private final String id;
        private final String description;
        private final List<String> tags;

        private Metric(@Nonnull String id,
                       @Nonnull String description,
                       @Nonnull List<String> tags) {
            this.id = Objects.requireNonNull(id, "id");
            this.description = Objects.requireNonNull(description, "description");
            this.tags = Objects.requireNonNull(tags, "tags");
        }

        @Nonnull
        public String id() {
            return id;
        }

        @Nonnull
        public String description() {
            return description;
        }

        @Nonnull
        public List<String> tags() {
            return tags;
        }

        @Override
        public String toString() {
            return "Metric{" +
                    "id='" + id + '\'' +
                    ", description='" + description + '\'' +
                    ", tags=" + tags + '\'' +
                    ", type=" + getClass().getSimpleName() +
                    '}';
        }
    }
}
