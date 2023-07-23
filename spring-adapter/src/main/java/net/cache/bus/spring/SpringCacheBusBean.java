package net.cache.bus.spring;

import net.cache.bus.core.CacheBus;
import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.ExtendedCacheBus;
import net.cache.bus.core.LifecycleException;
import net.cache.bus.core.configuration.CacheBusConfiguration;
import net.cache.bus.core.impl.DefaultCacheBus;
import net.cache.bus.core.state.CacheBusState;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;

import javax.annotation.Nonnull;
import java.beans.ConstructorProperties;
import java.io.Serializable;
import java.util.Objects;

/**
 * Implementation of a cache bus ({@link CacheBus}) for configuring and managing the cache bus in the
 * Spring Framework environment. In the application's Spring context, a singleton bean of this class needs to be created.
 * When {@code isAutoStart==true}, this bean activates the bus after the completion of the Spring context
 * construction ({@link ContextStartedEvent}) where this bean is declared, and stops the bus when the Spring
 * context is closed ({@link ContextStoppedEvent}).<br>
 * When {@code isAutoStart==false}, the application needs to explicitly call the {@link #start()} and {@link #stop()} methods.
 *
 * @author Alik
 * @see SmartLifecycle
 * @see CacheBus
 * @see CacheBusConfiguration
 */
public class SpringCacheBusBean implements CacheBus, SmartLifecycle {

    private final boolean isAutoStart;

    private volatile boolean isRunning;
    private ExtendedCacheBus delegateCacheBus;
    private CacheBusConfiguration configuration;

    /**
     * Creates an instance of the bus with automatic bean lifecycle management enabled.<br>
     * Following this, a call to {@link SpringCacheBusBean#withConfiguration(CacheBusConfiguration)} must be made.
     */
    public SpringCacheBusBean() {
        this(true);
    }

    /**
     * Creates an instance of the bus with the specified parameter for automatic bean lifecycle management.<br>
     * Following this, a call to {@link SpringCacheBusBean#withConfiguration(CacheBusConfiguration)} must be made.
     *
     * @param isAutoStart if {@code true}, the bus is activated after the completion of the Spring context construction;
     *                    if {@code false}, the application needs to explicitly call the {@link #start()} and {@link #stop()} methods.
     */
    @ConstructorProperties("isAutoStart")
    public SpringCacheBusBean(boolean isAutoStart) {
        this.isAutoStart = isAutoStart;
    }

    /**
     * Creates an instance of the bus with the specified parameter for automatic bean lifecycle management and the given configuration.
     *
     * @param isAutoStart   if {@code true}, the bus is activated after the completion of the Spring context construction;
     *                      if {@code false}, the application needs to explicitly call the {@link #start()} and {@link #stop()} methods.
     * @param configuration cache bus configuration, cannot be {@code null}.
     * @see CacheBusConfiguration
     * @see net.cache.bus.core.impl.configuration.ImmutableCacheBusConfiguration
     */
    @ConstructorProperties({"isAutoStart", "configuration"})
    public SpringCacheBusBean(boolean isAutoStart, @Nonnull CacheBusConfiguration configuration) {
        this.isAutoStart = isAutoStart;
        this.configuration = configuration;
    }

    @Override
    public <K extends Serializable, V extends Serializable> void send(@Nonnull CacheEntryEvent<K, V> event) {
        this.delegateCacheBus.send(event);
    }

    @Override
    public void receive(@Nonnull byte[] binaryEventData) {
        this.delegateCacheBus.receive(binaryEventData);
    }

    /**
     * {@inheritDoc}
     *
     * @throws LifecycleException if {@code isRunning == true}, i.e., if the bus has already been started.
     */
    @Override
    public void withConfiguration(@Nonnull CacheBusConfiguration configuration) {
        if (this.isRunning) {
            throw new LifecycleException("Configuration changing isn't allowed in running state");
        }

        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    @Override
    public CacheBusConfiguration configuration() {
        return this.configuration;
    }

    @Nonnull
    @Override
    public CacheBusState state() {
        if (!this.isRunning && this.delegateCacheBus == null) {
            throw new LifecycleException("Cache bus isn't running yet");
        }

        return this.delegateCacheBus.state();
    }

    /**
     * {@inheritDoc}
     *
     * @throws LifecycleException if {@code isRunning == true}, i.e., if the bus has already been started.
     */
    @Override
    public synchronized void start() {

        if (this.isRunning) {
            throw new LifecycleException("CacheBus already started");
        } else if (this.configuration == null) {
            throw new LifecycleException("Configuration must be set before start");
        }

        if (!this.isRunning) {
            this.delegateCacheBus = new DefaultCacheBus(this.configuration);
            this.delegateCacheBus.start();

            this.isRunning = true;
        }
    }

    @Override
    public synchronized void stop() {

        if (this.isRunning) {
            this.delegateCacheBus.stop();
            this.isRunning = false;
        }
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }

    @Override
    public boolean isAutoStartup() {
        return this.isAutoStart;
    }

}
