package net.cache.bus.transport.addons;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Обработчик для выполнения переподключения к каналу сообщений шины кэшей.
 *
 * @author Alik
 */
public final class ChannelRecoveryProcessor {

    private static final Logger logger = Logger.getLogger(ChannelRecoveryProcessor.class.getCanonicalName());

    private final Runnable cleaningCallback;
    private final Runnable recoveryCallback;
    private final int maxRecoveryAttemptTimeMillis;

    public ChannelRecoveryProcessor(
            @Nonnull final Runnable recoveryCallback,
            @Nonnull final Runnable cleaningCallback,
            @Nonnegative final int maxRecoveryAttemptTimeMillis) {

        this.recoveryCallback = Objects.requireNonNull(recoveryCallback, "recoveryCallback");
        this.cleaningCallback = Objects.requireNonNull(cleaningCallback, "cleaningCallback");
        this.maxRecoveryAttemptTimeMillis = maxRecoveryAttemptTimeMillis;
    }

    public void recover(@Nonnull Exception exception) {

        logger.log(Level.ALL, "Trying to recover connection, exception detected", exception);

        this.clean();

        if (!this.recover()) {
            logger.log(Level.ALL, "Fatal disconnect, can not recover connection in {} ms", this.maxRecoveryAttemptTimeMillis);
            throw new NotRecoverableException(exception);
        }
    }

    private void clean() {

        try {
            this.cleaningCallback.run();
        } catch (Exception e) {
            logger.log(Level.FINE, "Exception while execution of cleaning callback", e);
        }
    }

    /**
     * Производит попытки восстановления соединения в течение заданного времени.
     *
     * @return {@code true} - если соединение удалось восстановить,
     * {@code false} - иначе.
     */
    private boolean recover() {

        final Random rand = new Random();

        int nextAttemptTimeout = 30;
        boolean isRecovered;

        do {
            isRecovered = this.recoverTry();
            nextAttemptTimeout = Math.min(nextAttemptTimeout * 3, this.maxRecoveryAttemptTimeMillis);

        } while (!isRecovered && this.waitNextAttempt(nextAttemptTimeout + rand.nextInt(nextAttemptTimeout / 5)));

        return isRecovered;
    }

    private boolean waitNextAttempt(final int waitTimeInMillis) {

        try {
            Thread.sleep(waitTimeInMillis);
        } catch (InterruptedException ex) {
            logger.info("Waiting of next attempt is interrupted");

            Thread.currentThread().interrupt();
        }

        return !Thread.currentThread().isInterrupted();
    }

    private boolean recoverTry() {

        try {
            this.recoveryCallback.run();

            logger.fine("Successful connection recovery");
            return true;
        } catch (Exception ex) {
            logger.log(Level.FINE, "Recovery attempt unsuccessful", ex);
        }

        return false;
    }
}
