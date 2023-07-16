package net.cache.bus.spring.adapters;

import net.cache.bus.core.CacheBus;
import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.ExtendedCacheBus;
import net.cache.bus.core.LifecycleException;
import net.cache.bus.core.configuration.CacheBusConfiguration;
import net.cache.bus.core.impl.DefaultCacheBus;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;

import javax.annotation.Nonnull;
import java.beans.ConstructorProperties;
import java.io.Serializable;
import java.util.Objects;

/**
 * Реализация шины кэшей ({@link CacheBus}) для настройки и управления шиной кэшей в среде Spring Framework.
 * В Spring-контексте приложения необходимо создать singleton-бин данного класса.
 * При {@code isAutoStart==true} данный бин активирует шину после завершения построения Spring-контекста ({@link ContextStartedEvent}),
 * в котором данный бин объявлен и останавливает шину при закрытии Spring-контекста ({@link ContextStoppedEvent}). <br/>
 * При {@code isAutoStart==false} приложение должно явно вызывать методы {@link #start()} и {@link #stop()}.<br>
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
     * Создает реализацию шины с включенным автоматическим управлением жизненным циклом бина.<br>
     * Обязательно после этого должен последовать вызов {@link SpringCacheBusBean#setConfiguration(CacheBusConfiguration)}.
     */
    public SpringCacheBusBean() {
        this(true);
    }

    /**
     * Создает реализацию шины с заданным параметром признаком автоматического управления жизненным циклом бина.<br>
     * Обязательно после этого должен последовать вызов {@link SpringCacheBusBean#setConfiguration(CacheBusConfiguration)}.
     *
     * @param isAutoStart если {@code true}, то шина активируется после завершения построения Spring-контекста;
     *                    если {@code false}, то для запуска шины приложение должно явно вызывать методы {@link #start()} и {@link #stop()}.
     */
    @ConstructorProperties("isAutoStart")
    public SpringCacheBusBean(boolean isAutoStart) {
        this.isAutoStart = isAutoStart;
    }

    /**
     * Создает реализацию шины с заданным параметром признаком автоматического управления жизненным циклом бина и заданной конфигурацией.
     *
     * @param isAutoStart   если {@code true}, то шина активируется после завершения построения Spring-контекста;
     *                      если {@code false}, то для запуска шины приложение должно явно вызывать методы {@link #start()} и {@link #stop()}.
     * @param configuration конфигурация шины кэшей, не может быть {@code null}.
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
     * @throws LifecycleException если {@code isRunning == true}, т.е. если шина уже была запущена
     */
    @Override
    public void setConfiguration(@Nonnull CacheBusConfiguration configuration) {
        if (this.isRunning) {
            throw new LifecycleException("Configuration changing isn't allowed in running state");
        }

        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    @Override
    public CacheBusConfiguration getConfiguration() {
        return this.configuration;
    }

    /**
     * {@inheritDoc}
     *
     * @throws LifecycleException если {@code isAutoStart == true}, т.е. если используется автоматическое управление жизненным циклом бина
     * @throws LifecycleException если {@code isRunning == true}, т.е. если шина уже была запущена
     */
    @Override
    public synchronized void start() {

        if (this.isAutoStart) {
            throw new LifecycleException("Start isn't allowed when auto start enabled");
        } else if (this.isRunning) {
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

    /**
     * {@inheritDoc}
     *
     * @throws LifecycleException если {@code isAutoStart == true}
     */
    @Override
    public synchronized void stop() {

        if (this.isAutoStart) {
            throw new LifecycleException("Stop isn't allowed when auto start enabled");
        }

        if (this.isRunning) {
            this.delegateCacheBus.stop();
            this.isRunning = false;
        }
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }
}
