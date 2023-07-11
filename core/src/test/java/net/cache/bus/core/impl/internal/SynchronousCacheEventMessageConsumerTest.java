package net.cache.bus.core.impl.internal;

import net.cache.bus.core.CacheBus;
import net.cache.bus.core.CacheEventMessageConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
