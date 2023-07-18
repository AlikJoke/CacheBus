package net.cache.bus.core.impl.internal;

import net.cache.bus.core.state.ComponentState;
import org.junit.jupiter.api.Test;

import static net.cache.bus.core.impl.internal.AsyncMessageProcessingState.THREADS_WAITING_ON_OFFER_LABEL;
import static org.junit.jupiter.api.Assertions.*;

public class AsyncMessageProcessingStateTest {

    private static final String TEST_ID = "1";
    private static final String TEST_INTERRUPTED_THREADS_LABEL = "IT: %d";

    @Test
    public void testInitialState() {
        final AsyncMessageProcessingState state = new AsyncMessageProcessingState(TEST_ID, TEST_INTERRUPTED_THREADS_LABEL, 2);

        assertEquals(TEST_ID, state.componentId(), "ComponentId must be equal");
        assertEquals(ComponentState.Status.UP_OK, state.status(), "Initial status must be UP_OK");
        assertFalse(state.hasSeverities(), "Severities must be empty at initial state");
    }

    @Test
    public void testTransitionToStoppedState() {
        final AsyncMessageProcessingState state = new AsyncMessageProcessingState(TEST_ID, TEST_INTERRUPTED_THREADS_LABEL, 2);
        state.toStoppedState();

        assertEquals(ComponentState.Status.DOWN, state.status(), "Status must be DOWN after stop");
    }

    @Test
    public void testIncreasingOfInterruptedThreads() {
        final AsyncMessageProcessingState state = new AsyncMessageProcessingState(TEST_ID, TEST_INTERRUPTED_THREADS_LABEL, 2);
        state.increaseCountOfInterruptedThreads();

        assertEquals(ComponentState.Status.UP_OK, state.status(), "Status must be UP_OK until max interrupted threads limit is reached");
        assertTrue(state.hasSeverities(), "After thread interruption must exist one severity");
        assertEquals(TEST_INTERRUPTED_THREADS_LABEL.formatted(1), state.severities().get(0).asString(), "After thread interruption must exist one severity and equal to template");

        state.increaseCountOfInterruptedThreads();

        assertEquals(ComponentState.Status.UP_FATAL_BROKEN, state.status(), "Status must be UP_FATAL_BROKEN when max interrupted threads limit is reached");
        assertTrue(state.hasSeverities(), "After thread interruption must exist one severity");
        assertEquals(TEST_INTERRUPTED_THREADS_LABEL.formatted(2), state.severities().get(0).asString(), "After thread interruption must exist one severity and equal to template");
    }

    @Test
    public void testBusyWaitingInfo() {
        final AsyncMessageProcessingState state = new AsyncMessageProcessingState(TEST_ID, TEST_INTERRUPTED_THREADS_LABEL, 2);
        state.onBufferFull();
        state.onBufferFull();

        assertEquals(ComponentState.Status.UP_OK, state.status(), "Status must be UP_OK");
        assertTrue(state.hasSeverities(), "After buffer full events must exist one severity");
        assertEquals(state.severities().get(0).asString(), THREADS_WAITING_ON_OFFER_LABEL);
    }
}
