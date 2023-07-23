package net.cache.bus.core.impl;

import net.cache.bus.core.state.ComponentState;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * Immutable implementation of the state of a single bus component.
 *
 * @param componentId the identifier of the component, cannot be {@code null}.
 * @param status      the functioning status of the component, cannot be {@code null}.
 * @param severities  the set of registered issues in the operation of the component, cannot be {@code null}
 * @author Alik
 * @see ComponentState
 * @see net.cache.bus.core.state.ComponentState.Status
 */
public record ImmutableComponentState(
        @Nonnull String componentId,
        @Nonnull Status status,
        @Nonnull List<SeverityInfo> severities) implements ComponentState {

    public ImmutableComponentState(@Nonnull String componentId, @Nonnull Status status) {
        this(componentId, status, Collections.emptyList());
    }
}
