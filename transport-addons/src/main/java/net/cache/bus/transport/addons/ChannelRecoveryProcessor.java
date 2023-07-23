package net.cache.bus.transport.addons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Random;

/**
 * Обработчик для выполнения переподключения к каналу сообщений шины кэшей.
 *
 * @author Alik
 */
public final class ChannelRecoveryProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ChannelRecoveryProcessor.class);

    private final Runnable cleaningCallback;
    private final Runnable recoveryCallback;
    private final long maxRecoveryAttemptTimeMillis;

    public ChannelRecoveryProcessor(
            @Nonnull final Runnable cleaningCallback,
            @Nonnull final Runnable recoveryCallback,
            @Nonnegative final long maxRecoveryAttemptTimeMillis) {

        this.recoveryCallback = Objects.requireNonNull(recoveryCallback, "recoveryCallback");
        this.cleaningCallback = Objects.requireNonNull(cleaningCallback, "cleaningCallback");
        this.maxRecoveryAttemptTimeMillis = maxRecoveryAttemptTimeMillis;
    }

    public void recover(@Nonnull Exception exception) {

        logger.debug("Trying to recover connection, exception detected", exception);

        this.clean();

        if (!this.recover()) {
            logger.error("Fatal disconnect, cannot recover connection in {} ms", this.maxRecoveryAttemptTimeMillis);
            throw new NotRecoverableException(exception);
        }
    }

    private void clean() {

        try {
            this.cleaningCallback.run();
        } catch (Exception e) {
            logger.warn("Exception while execution of cleaning callback", e);
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

        long nextAttemptTimeout = 30;
        boolean isRecovered;

        do {
            isRecovered = this.recoverTry();
            nextAttemptTimeout = Math.min(nextAttemptTimeout * 3, this.maxRecoveryAttemptTimeMillis);

        } while (!isRecovered && this.waitNextAttempt(nextAttemptTimeout + rand.nextLong(nextAttemptTimeout / 5)));

        return isRecovered;
    }

    private boolean waitNextAttempt(final long waitTimeInMillis) {

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

            logger.debug("Successful connection recovery");
            return true;
        } catch (Exception ex) {
            logger.debug("Recovery attempt unsuccessful", ex);
        }

        return false;
    }
}
