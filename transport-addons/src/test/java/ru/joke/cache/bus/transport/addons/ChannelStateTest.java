package ru.joke.cache.bus.transport.addons;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChannelStateTest {

    @Test
    public void testSummarize() throws InterruptedException {
        final ChannelState state = new ChannelState();

        state.increaseCountOfUnrecoverableConsumers();

        state.increaseCountOfProducersInRecoveryState();
        state.increaseCountOfProducersInRecoveryState();

        state.increaseCountOfUnrecoverableProducers();
        state.increaseCountOfUnrecoverableProducers();

        state.increaseCountOfInterruptedThreads();

        state.increaseCountOfConsumersInRecoveryState();

        state.increaseCountOfProducersInBusyPollingOfConnections();
        state.increaseCountOfProducersInBusyPollingOfConnections();

        assertEquals(6, state.summarizeStats().size(), "Must be 6 warnings in stats: one by one to kind of metrics");

        // после тайм-аута должно стать на один меньше, т.к. предупреждение
        // о производителях в состоянии ожидания сбрасывается через тайм-аут
        Thread.sleep(Duration.ofMillis(1_200));

        assertEquals(5, state.summarizeStats().size(), "Must be 5 warnings because busy waiting threads does not update metrics again");

        state.decreaseCountConsumersInRecoveryState();
        assertEquals(4, state.summarizeStats().size(), "Must be 4 warnings because we decrease count of consumers in recovery state to 0");

        state.decreaseCountProducersInRecoveryState();
        assertEquals(4, state.summarizeStats().size(), "Count of warnings must be the same because count of producers in recovery state must be equal 1 after decreasing");

        state.decreaseCountProducersInRecoveryState();
        assertEquals(3, state.summarizeStats().size(), "Must be 4 warnings because we decrease count of producers in recovery state to 0");
    }
}
