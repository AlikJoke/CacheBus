package net.cache.bus.core.impl.internal.util;

import net.cache.bus.core.impl.internal.util.StripedRingBuffersContainer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class StripedRingBuffersContainerTest {

    @Test
    public void testSize() {
        final StripedRingBuffersContainer<Integer> container = new StripedRingBuffersContainer<>(10);
        assertEquals(10, container.size(), "Size of container must be equal");
    }

    @Test
    public void testCreatedBuffers() {
        final StripedRingBuffersContainer<Integer> container = new StripedRingBuffersContainer<>(10, 2);
        for (int i = 0; i < container.size(); i++) {
            assertNotNull(container.get(i), "Buffer must be not null");
        }
    }

}
