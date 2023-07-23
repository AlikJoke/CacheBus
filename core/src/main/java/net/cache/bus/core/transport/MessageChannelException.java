package net.cache.bus.core.transport;

import javax.annotation.Nonnull;

/**
 * An exception indicating that an exceptional situation occurred while working with the message channel.
 *
 * @author Alik
 */
public final class MessageChannelException extends RuntimeException {

    public MessageChannelException(@Nonnull String message) {
        super(message);
    }

    public MessageChannelException(@Nonnull Exception exception) {
        super(exception);
    }

    public MessageChannelException(@Nonnull String message, @Nonnull Exception exception) {
        super(message, exception);
    }
}
