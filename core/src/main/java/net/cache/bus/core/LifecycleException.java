package net.cache.bus.core;

import javax.annotation.Nonnull;

/**
 * Исключение, сообщающее о том, что произошла исключительная ситуация, связанная с недопустимым
 * состоянием объекта в данной стадии жизненного цикла.
 *
 * @author Alik
 */
public final class LifecycleException extends IllegalStateException {

    public LifecycleException(@Nonnull String message) {
        super(message);
    }
}
