package net.cache.bus.transport.addons;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Состояние канала взаимодействия с другими серверами для локальной шины.
 *
 * @author Alik
 */
public final class ChannelState {

    private static final long HIGH_FREQUENCY_THRESHOLD_MS = 1_000;

    private final AtomicInteger unrecoverableProducers = new AtomicInteger(0);
    private final AtomicInteger unrecoverableConsumers = new AtomicInteger(0);
    private final AtomicInteger producersInRecovery = new AtomicInteger(0);
    private final AtomicInteger consumersInRecovery = new AtomicInteger(0);
    private final AtomicInteger producersInBusyPollingOfConnections = new AtomicInteger(0);
    private final AtomicInteger interruptedThreads = new AtomicInteger(0);

    private volatile long lastBusyPollingOfConnections;
    private volatile boolean highFrequencyOfBusyPollingOfConnections;

    public void increaseCountOfProducersInRecoveryState() {
        this.producersInRecovery.incrementAndGet();
    }

    public void decreaseCountProducersInRecoveryState() {
        this.producersInRecovery.decrementAndGet();
    }

    @Nonnegative
    public int producersInRecovery() {
        return this.producersInRecovery.get();
    }

    public void increaseCountOfConsumersInRecoveryState() {
        this.consumersInRecovery.incrementAndGet();
    }

    public void decreaseCountConsumersInRecoveryState() {
        this.consumersInRecovery.decrementAndGet();
    }

    @Nonnegative
    public int consumersInRecovery() {
        return this.consumersInRecovery.get();
    }

    public void increaseCountOfInterruptedThreads() {
        this.interruptedThreads.incrementAndGet();
    }

    @Nonnegative
    public int interruptedThreads() {
        return this.interruptedThreads.get();
    }

    public void increaseCountOfProducersInBusyPollingOfConnections() {
        final long lastBusyPollingOfConnections = this.lastBusyPollingOfConnections;
        // Небольшая погрешность в мс при записи из разных потоков роли не играет
        this.lastBusyPollingOfConnections = System.currentTimeMillis();
        if (System.currentTimeMillis() - lastBusyPollingOfConnections <= HIGH_FREQUENCY_THRESHOLD_MS) {
            this.highFrequencyOfBusyPollingOfConnections = true;
        }

        this.producersInBusyPollingOfConnections.incrementAndGet();
    }

    public void decreaseCountOfProducersInBusyPollingOfConnections() {
        this.producersInBusyPollingOfConnections.decrementAndGet();
    }

    @Nonnegative
    public int producersInBusyPollingOfConnections() {
        return this.producersInBusyPollingOfConnections.get();
    }

    public boolean highFrequencyOfBusyPollingByProducers() {
        return this.highFrequencyOfBusyPollingOfConnections
                && System.currentTimeMillis() - this.lastBusyPollingOfConnections <= HIGH_FREQUENCY_THRESHOLD_MS;
    }

    public void increaseCountOfUnrecoverableProducers() {
        this.unrecoverableProducers.incrementAndGet();
    }

    @Nonnegative
    public int unrecoverableProducers() {
        return this.unrecoverableProducers.get();
    }

    public void increaseCountOfUnrecoverableConsumers() {
        this.unrecoverableConsumers.incrementAndGet();
    }

    @Nonnegative
    public int unrecoverableConsumers() {
        return this.unrecoverableConsumers.get();
    }

    @Nonnull
    public List<String> summarizeStats() {
        final int consumersInRecovery = consumersInRecovery();
        final int producersInRecovery = producersInRecovery();
        final int unrecoverableConsumers = unrecoverableConsumers();
        final int unrecoverableProducers = unrecoverableProducers();
        final int threadsInterrupted = interruptedThreads();
        final boolean highFrequencyOfBusyPollingByProducers = highFrequencyOfBusyPollingByProducers();

        final List<String> result = new ArrayList<>(6);
        if (consumersInRecovery > 0) {
            result.add("Count of consumers in recovery: %d".formatted(consumersInRecovery));
        }
        if (producersInRecovery > 0) {
            result.add("Count of producers in recovery: %d".formatted(producersInRecovery));
        }
        if (threadsInterrupted > 0) {
            result.add("Count of interrupted threads: %d".formatted(threadsInterrupted));
        }
        if (highFrequencyOfBusyPollingByProducers) {
            result.add("High frequency of producers in busy polling of connections");
        }
        if (unrecoverableConsumers > 0) {
            result.add("Count of consumers failed in recovery: %d".formatted(unrecoverableConsumers));
        }
        if (unrecoverableProducers > 0) {
            result.add("Count of producers failed in recovery: %d".formatted(unrecoverableProducers));
        }

        return result;
    }
}
