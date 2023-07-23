package ru.joke.cache.bus.transport.addons;

import javax.annotation.Nonnull;

final class NotRecoverableException extends RuntimeException {

    public NotRecoverableException(@Nonnull final Exception exception) {
        super(exception);
    }
}
