package net.cache.bus.core.impl;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class ConcurrentActionExecutor {

    private final Map<Object, AtomicInteger> monitors;

    public ConcurrentActionExecutor(@Nonnegative int concurrencyLevel) {
        if (concurrencyLevel < 1 || concurrencyLevel > 512) {
            throw new IllegalArgumentException("Concurrency level is out of range " + concurrencyLevel);
        }

        this.monitors = new ConcurrentHashMap<>(256, 0.75f, concurrencyLevel);
    }

    @Nullable
    public <T> T execute(@Nonnull final Object lockKey, @Nonnull final Supplier<T> action) {

        final AtomicInteger monitor = monitors.compute(lockKey, (k, v) -> {
            final AtomicInteger counter = v == null ? new AtomicInteger(0) : v;
            counter.incrementAndGet();
            return counter;
        });

        try {
            synchronized (monitor) {
                return action.get();
            }
        } finally {

            monitors.computeIfPresent(lockKey, (k, v) -> {
                if (v.decrementAndGet() == 0) {
                    return null;
                }

                return v;
            });
        }
    }
}