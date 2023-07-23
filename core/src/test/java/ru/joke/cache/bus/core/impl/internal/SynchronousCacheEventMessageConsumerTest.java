package ru.joke.cache.bus.core.impl.internal;

import ru.joke.cache.bus.core.CacheBus;
import ru.joke.cache.bus.core.CacheEventMessageConsumer;
import ru.joke.cache.bus.core.state.ComponentState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SynchronousCacheEventMessageConsumerTest {

    @Mock
    private CacheBus cacheBus;

    @Test
    public void testSyncConsuming() {
        final CacheEventMessageConsumer messageConsumer = new SynchronousCacheEventMessageConsumer(this.cacheBus);

        final int messagesCount = 5;
        for (int i = 0; i < messagesCount; i++) {
            messageConsumer.accept(i, new byte[1]);
        }

        verify(this.cacheBus, times(messagesCount)).receive(new byte[1]);
    }

    @Test
    public void testState() {
        CacheEventMessageConsumer messageConsumer = new SynchronousCacheEventMessageConsumer(this.cacheBus);
        try (messageConsumer) {
            Assertions.assertEquals(ComponentState.Status.UP_OK, messageConsumer.state().status(), "Status of sync message consumer before closure must be UP_OK");
        }

        Assertions.assertEquals(ComponentState.Status.DOWN, messageConsumer.state().status(), "Status of sync message consumer after closure must be DOWN");
    }
}
