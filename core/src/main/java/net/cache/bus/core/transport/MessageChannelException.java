package net.cache.bus.core.transport;

import javax.annotation.Nonnull;

/**
 * Исключение, сообщающее о том, что при работе с каналом сообщений произошла исключительная ситуация.
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
