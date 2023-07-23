package net.cache.bus.core;

import javax.annotation.Nonnull;

/**
 * An exception indicating that an exceptional situation occurred due an invalid
 * state of the object in this stage of its lifecycle.
 *
 * @author Alik
 */
public final class LifecycleException extends IllegalStateException {

    public LifecycleException(@Nonnull String message) {
        super(message);
    }
}
