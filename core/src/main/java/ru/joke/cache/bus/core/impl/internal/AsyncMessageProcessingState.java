package ru.joke.cache.bus.core.impl.internal;

import ru.joke.cache.bus.core.state.ComponentState;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

final class AsyncMessageProcessingState implements ComponentState {

    static final String THREADS_WAITING_ON_OFFER_LABEL = "Writing threads are waiting";
    private static final int BUSY_WAITING_THRESHOLD_MS = 1_000;

    private final String componentId;
    private final String severityInterruptedThreadsTemplate;
    private final int maxInterruptedThreadsToBrokenState;
    private final AtomicInteger interruptedThreads;

    private volatile long lastTimeWhenBufferIsFull;
    private volatile boolean writingThreadsInWaiting;
    private volatile Status status;

    AsyncMessageProcessingState(
            @Nonnull String componentId,
            @Nonnull String severityInterruptedThreadsTemplate,
            @Nonnegative int maxInterruptedThreadsToBrokenState) {
        this.componentId = Objects.requireNonNull(componentId, "componentId");
        this.severityInterruptedThreadsTemplate = Objects.requireNonNull(severityInterruptedThreadsTemplate, "severityInterruptedThreadsTemplate");
        this.maxInterruptedThreadsToBrokenState = maxInterruptedThreadsToBrokenState;
        this.interruptedThreads = new AtomicInteger(0);
        this.status = Status.UP_OK;
    }

    @Nonnull
    @Override
    public String componentId() {
        return this.componentId;
    }

    @Nonnull
    @Override
    public Status status() {
        return this.status;
    }

    @Nonnull
    @Override
    public List<SeverityInfo> severities() {

        final List<SeverityInfo> result = new ArrayList<>(2);
        final int interruptedOnProduceToChannelThreads = this.interruptedThreads.get();
        if (interruptedOnProduceToChannelThreads > 0) {
            result.add(() -> this.severityInterruptedThreadsTemplate.formatted(interruptedOnProduceToChannelThreads));
        }

        if (this.writingThreadsInWaiting && System.currentTimeMillis() - this.lastTimeWhenBufferIsFull <= BUSY_WAITING_THRESHOLD_MS) {
            result.add(() -> THREADS_WAITING_ON_OFFER_LABEL);
        }

        return result;
    }

    public void toStoppedState() {
        this.interruptedThreads.set(0);
        this.status = Status.DOWN;
    }

    public void increaseCountOfInterruptedThreads() {
        if (this.interruptedThreads.incrementAndGet() == maxInterruptedThreadsToBrokenState && this.status != Status.DOWN) {
            this.status = ComponentState.Status.UP_FATAL_BROKEN;
        }
    }

    public void onBufferFull() {
        final long lastTimeWhenBufferIsFull = this.lastTimeWhenBufferIsFull;
        final long currentTime = System.currentTimeMillis();

        this.lastTimeWhenBufferIsFull = currentTime;
        this.writingThreadsInWaiting = currentTime - lastTimeWhenBufferIsFull <= BUSY_WAITING_THRESHOLD_MS;
    }
}
