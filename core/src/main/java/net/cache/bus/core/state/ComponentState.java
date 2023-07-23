package net.cache.bus.core.state;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Returns information about the state of a single component of the bus.
 *
 * @author Alik
 */
public interface ComponentState {

    /**
     * Returns the identifier of the bus component.
     *
     * @return cannot be {@code null}.
     */
    @Nonnull
    String componentId();

    /**
     * Returns the overall status of the bus component.
     *
     * @return cannot be {@code null}.
     * @see Status
     */
    @Nonnull
    Status status();

    /**
     * Returns whether the component has any severities indicating problems in its operation.
     *
     * @return true if the component has severities, false otherwise.
     * @see ComponentState#severities()
     */
    default boolean hasSeverities() {
        return !severities().isEmpty();
    }

    /**
     * Returns a list of registered severities indicating problems in the component's operation.
     *
     * @return cannot be {@code null}.
     */
    @Nonnull
    List<SeverityInfo> severities();

    /**
     * Information about a registered severity indicating a problem in the component's operation.
     *
     * @author Alik
     */
    interface SeverityInfo {

        /**
         * Returns the string representation of the problem in the component's operation.
         *
         * @return cannot be {@code null}.
         */
        @Nonnull
        String asString();
    }

    /**
     * Status of the component's operation.
     *
     * @author Alik
     */
    enum Status {

        /**
         * Running / active.
         */
        UP_OK,

        /**
         * Running but in an unrecoverable state.
         */
        UP_FATAL_BROKEN,

        /**
         * Starting up / being activated.
         */
        UP_NOT_READY,

        /**
         * Stopped / inactive.
         */
        DOWN
    }
}
