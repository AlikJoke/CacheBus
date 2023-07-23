package ru.joke.cache.bus.metrics.micrometer;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import ru.joke.cache.bus.core.metrics.CacheBusMetricsRegistry;
import ru.joke.cache.bus.core.metrics.KnownMetrics;
import ru.joke.cache.bus.core.metrics.Metrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class MicrometerCacheBusMetricsRegistryTest {

    private MeterRegistry meterRegistry;
    private CacheBusMetricsRegistry registry;

    @BeforeEach
    public void preparation() {
        this.meterRegistry = new SimpleMeterRegistry();
        this.registry = new MicrometerCacheBusMetricsRegistry(this.meterRegistry);
    }

    @Test
    public void testCounterRegistration() {
        final Metrics.Counter counter = new Metrics.Counter(KnownMetrics.MANAGED_CACHES_COUNT);
        registry.registerCounter(counter);

        final Counter micrometerCounter = meterRegistry.get(KnownMetrics.MANAGED_CACHES_COUNT.id()).counter();
        makeCommonMeterDescriptorChecks(KnownMetrics.MANAGED_CACHES_COUNT, micrometerCounter);
    }

    @Test
    public void testTimerRegistration() {
        final Metrics.Timer timer = new Metrics.Timer(KnownMetrics.CONSUMER_BUFFER_BLOCKING_OFFER_TIME);
        registry.registerTimer(timer);

        final Timer micrometerTimer = meterRegistry.get(KnownMetrics.CONSUMER_BUFFER_BLOCKING_OFFER_TIME.id()).timer();
        makeCommonMeterDescriptorChecks(KnownMetrics.CONSUMER_BUFFER_BLOCKING_OFFER_TIME, micrometerTimer);
    }

    @Test
    public void testGaugeRegistration() {
        final Metrics.Gauge<List<Integer>> gauge = new Metrics.Gauge<>(KnownMetrics.BUFFER_READ_POSITION, List.of(1, 2, 3), List::size);
        registry.registerGauge(gauge);

        final Gauge micrometerGauge = meterRegistry.get(KnownMetrics.BUFFER_READ_POSITION.id()).gauge();
        makeCommonMeterDescriptorChecks(KnownMetrics.BUFFER_READ_POSITION, micrometerGauge);
    }

    @Test
    public void testSummaryRegistration() {
        final MeterRegistry meterRegistry = new SimpleMeterRegistry();
        final CacheBusMetricsRegistry registry = new MicrometerCacheBusMetricsRegistry(meterRegistry);

        final Metrics.Summary summary = new Metrics.Summary(KnownMetrics.CONSUMED_BYTES, "bytes");
        registry.registerSummary(summary);

        final DistributionSummary micrometerSummary = meterRegistry.get(KnownMetrics.CONSUMED_BYTES.id()).summary();
        makeCommonMeterDescriptorChecks(KnownMetrics.CONSUMED_BYTES, micrometerSummary);
        assertEquals("bytes", micrometerSummary.getId().getBaseUnit(), "Unit must be equal");
    }

    @Test
    public void testSummaryPut() {

        assertThrows(NullPointerException.class, () -> registry.incrementCounter(KnownMetrics.PRODUCED_BYTES), "Summary isn't registered yet");

        final Metrics.Summary summary = new Metrics.Summary(KnownMetrics.PRODUCED_BYTES, "bytes");
        registry.registerSummary(summary);

        final DistributionSummary micrometerSummary = this.meterRegistry.get(KnownMetrics.PRODUCED_BYTES.id()).summary();

        registry.putToSummary(KnownMetrics.PRODUCED_BYTES, 2);
        registry.putToSummary(KnownMetrics.PRODUCED_BYTES, 4);
        registry.putToSummary(KnownMetrics.PRODUCED_BYTES, 8);
        registry.putToSummary(KnownMetrics.PRODUCED_BYTES, 9);

        assertEquals(2 + 4 + 8 + 9, micrometerSummary.totalAmount(), "Amount must be equal");
        assertEquals(4, micrometerSummary.count(), "Count must be equal");
        assertEquals(9, micrometerSummary.max(), "Max value must be equal");
    }

    @Test
    public void testTimerRecordOperations() throws Exception {

        assertThrows(NullPointerException.class, () -> registry.incrementCounter(KnownMetrics.CONSUMER_CONNECTION_RECOVERY_TIME), "Timer isn't registered yet");

        final Metrics.Timer timer = new Metrics.Timer(KnownMetrics.CONSUMER_CONNECTION_RECOVERY_TIME);
        registry.registerTimer(timer);

        final Timer micrometerTimer = this.meterRegistry.get(KnownMetrics.CONSUMER_CONNECTION_RECOVERY_TIME.id()).timer();

        final int sleepTime = new Random().nextInt(10, 200);
        registry.recordExecutionTime(
                KnownMetrics.CONSUMER_CONNECTION_RECOVERY_TIME,
                () -> {
                    Thread.sleep(sleepTime);
                    return sleepTime;
                });

        registry.recordExecutionTime(
                KnownMetrics.CONSUMER_CONNECTION_RECOVERY_TIME,
                () -> {
                    int counter = 0;
                    for (int i = 0; i < sleepTime; i++) {
                        counter += i;
                    }

                    assertTrue(counter > 0, "Counter must be positive");
                });

        final double maxExecutionTimeToCompare = micrometerTimer.max(TimeUnit.MILLISECONDS);
        final int probableExecutionTimeWithMeasurementError = sleepTime + 50;
        assertTrue(sleepTime <= maxExecutionTimeToCompare && probableExecutionTimeWithMeasurementError > maxExecutionTimeToCompare, "Execution time of longest task must be approximately equal, taking into account the error");
        assertEquals(2, micrometerTimer.count(), "Count of invocations must be equal");
    }

    @Test
    public void testCounterOperations() {

        assertThrows(NullPointerException.class, () -> registry.incrementCounter(KnownMetrics.MANAGED_CACHES_COUNT), "Counter isn't registered yet");

        final Metrics.Counter counter = new Metrics.Counter(KnownMetrics.MANAGED_CACHES_COUNT);
        registry.registerCounter(counter);

        final Counter micrometerCounter = this.meterRegistry.get(KnownMetrics.MANAGED_CACHES_COUNT.id()).counter();

        int rnd = new Random().nextInt(0, 10);
        for (int i = 0; i < rnd; i++) {
            registry.incrementCounter(KnownMetrics.MANAGED_CACHES_COUNT);
        }

        assertEquals(rnd, micrometerCounter.count(), "Value must be equal after increments");

        for (int i = 0; i < rnd; i++) {
            registry.decrementCounter(KnownMetrics.MANAGED_CACHES_COUNT);
        }

        assertEquals(0, micrometerCounter.count(), "Value must be equal to origin (0) after decrements");

        rnd = new Random().nextInt(0, 10);
        registry.increaseCounter(KnownMetrics.MANAGED_CACHES_COUNT, rnd);

        assertEquals(rnd, micrometerCounter.count(), "Value must be equal after increasing");
        registry.decreaseCounter(KnownMetrics.MANAGED_CACHES_COUNT, rnd);

        assertEquals(0, micrometerCounter.count(), "Value must be equal to origin (0) after decreasing");
    }

    private void makeCommonMeterDescriptorChecks(final KnownMetrics metric, final Meter meter) {

        assertNotNull(meter, "Meter must be registered");
        assertEquals(metric.description(), meter.getId().getDescription(), "Meter description must be equal");
        assertFalse(meter.getId().getTags().isEmpty(), "Tags must be not empty");
        int i = 0;
        for (final Tag tag : meter.getId().getTags()) {
            assertEquals(metric.tags().get(i), tag.getKey(), "Meter tag key must be equal");
            assertEquals(metric.tags().get(i + 1), tag.getValue(), "Meter tag value must be equal");
            i += 2;
        }
    }
}
