package net.cache.bus.transport;

import java.util.concurrent.TimeUnit;

/**
 * Common constants for transport channels.
 *
 * @author Alik
 */
public abstract class ChannelConstants {

    /**
     * Type of message for cache change event.
     */
    public static final String MESSAGE_TYPE = "CacheEvent";

    /**
     * Timeout for obtaining a connection to the channel.
     */
    public static final int POLL_CHANNEL_TIMEOUT = 5;

    /**
     * Units of timeout for obtaining a connection to the channel.
     */
    public static final TimeUnit POLL_CHANNEL_TIMEOUT_UNITS = TimeUnit.SECONDS;

    /**
     * Timeout for reconnecting to the channel for a single connection.
     */
    public static final int RECONNECT_RETRY_TIMEOUT = 5;

    /**
     * Units of measurement for timeout for reconnecting to the channel for a single connection.
     */
    public static final TimeUnit RECONNECT_RETRY_TIMEOUT_UNITS = TimeUnit.MINUTES;

    private ChannelConstants() {
    }
}
