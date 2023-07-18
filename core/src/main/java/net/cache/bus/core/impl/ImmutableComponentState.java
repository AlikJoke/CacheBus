package net.cache.bus.core.impl;

import net.cache.bus.core.state.ComponentState;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * Неизменяемая реализация состояния одного компонента шины.
 *
 * @param componentId идентификатор компонента, не может быть {@code null}.
 * @param status      статус функционирования компонента, не может быть {@code null}.
 * @param severities  набор зарегистрированных проблем в работе компонента, не может быть {@code null}.
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
