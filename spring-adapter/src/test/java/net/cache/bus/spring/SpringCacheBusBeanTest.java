package net.cache.bus.spring;

import net.cache.bus.core.CacheBus;
import net.cache.bus.core.CacheEventListenerRegistrar;
import net.cache.bus.core.CacheManager;
import net.cache.bus.core.LifecycleException;
import net.cache.bus.core.configuration.*;
import net.cache.bus.core.impl.configuration.CacheProviderConfigurationTemplate;
import net.cache.bus.core.impl.configuration.ImmutableCacheBusConfiguration;
import net.cache.bus.core.impl.configuration.ImmutableCacheBusTransportConfiguration;
import net.cache.bus.core.transport.CacheBusMessageChannel;
import net.cache.bus.core.transport.CacheEntryEventConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SpringCacheBusBeanTest.TestConfig.class)
public class SpringCacheBusBeanTest {

    @Autowired
    @Qualifier("auto")
    private SpringCacheBusBean cacheBusAuto;

    @Autowired
    @Qualifier("manual")
    private SpringCacheBusBean cacheBusManual;

    @Test
    public void testAutoBeanLifecycleManagement() {

        assertTrue(this.cacheBusAuto.isRunning(), "Spring bean must be in started state (auto start on context start)");
        assertTrue(this.cacheBusAuto.isAutoStartup(), "Spring bean must has auto startup property");
        assertThrows(LifecycleException.class, this.cacheBusAuto::start, "Start isn't allowed in running state");
        assertNotNull(this.cacheBusAuto.getConfiguration(), "Configuration must be not null");
        assertThrows(LifecycleException.class, () -> this.cacheBusAuto.setConfiguration(mock(CacheBusConfiguration.class)), "Configuration changing isn't allowed in running state");
    }

    @Test
    public void testManualBeanLifecycleManagement() {

        // pre action checks
        assertNotNull(this.cacheBusManual.getConfiguration(), "Configuration must be not null");
        assertFalse(this.cacheBusManual.isRunning(), "Spring bean must not be in started state");
        assertFalse(this.cacheBusManual.isAutoStartup(), "Spring bean must not has auto startup property");
        //action
        this.cacheBusManual.start();
        // post action checks
        assertTrue(this.cacheBusManual.isRunning(), "Spring bean must be in started state");
        assertThrows(LifecycleException.class, this.cacheBusAuto::start, "Start isn't allowed in running state");
        assertThrows(LifecycleException.class, () -> this.cacheBusManual.setConfiguration(mock(CacheBusConfiguration.class)), "Configuration changing isn't allowed in running state");

        // action
        this.cacheBusManual.stop();
        // checks
        assertFalse(this.cacheBusManual.isRunning(), "Spring bean must not be in started state");
    }

    private static CacheBusConfiguration createCacheBusConfiguration() {

        final CacheBusTransportConfiguration transportConfiguration =
                ImmutableCacheBusTransportConfiguration
                        .builder()
                            .setMaxConcurrentReceivingThreads(1)
                            .setMaxProcessingThreadBufferCapacity(10)
                            .setProcessingPool(Executors.newSingleThreadExecutor())
                            .setMessageChannel(mock(CacheBusMessageChannel.class))
                            .setMessageChannelConfiguration(mock(CacheBusMessageChannelConfiguration.class))
                            .setConverter(mock(CacheEntryEventConverter.class))
                        .build();

        final CacheProviderConfiguration providerConfiguration = new CacheProviderConfigurationTemplate(mock(CacheManager.class), mock(CacheEventListenerRegistrar.class)) {};
        return ImmutableCacheBusConfiguration
                .builder()
                .setCacheConfigurationBuilder(CacheConfigurationSource.createDefault())
                .setProviderConfiguration(providerConfiguration)
                .setTransportConfiguration(transportConfiguration)
                .build();
    }

    @Configuration
    static class TestConfig {

        @Bean("auto")
        CacheBus cacheBusAuto() {
            final CacheBus cacheBus = new SpringCacheBusBean(true);
            cacheBus.setConfiguration(createCacheBusConfiguration());

            return cacheBus;
        }

        @Bean("manual")
        CacheBus cacheBusManual() {
            final CacheBus cacheBus = new SpringCacheBusBean(false);
            cacheBus.setConfiguration(createCacheBusConfiguration());

            return cacheBus;
        }
    }
}
