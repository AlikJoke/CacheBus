package net.cache.bus.transport;

import java.util.concurrent.TimeUnit;

/**
 * Общие константы для транспортных каналов.
 *
 * @author Alik
 */
public abstract class ChannelConstants {

    /**
     * Тип сообщения события об изменении кэша.
     */
    public static final String MESSAGE_TYPE = "CacheEvent";

    /**
     * Тайм-аут получения соединения с каналом.
     */
    public static final int POLL_CHANNEL_TIMEOUT = 5;

    /**
     * Единицы изменения тайм-аута получения соединения с каналом.
     */
    public static final TimeUnit POLL_CHANNEL_TIMEOUT_UNITS = TimeUnit.SECONDS;

    /**
     * Тайм-аут переподключения к каналу для одного соединения.
     */
    public static final int RECONNECT_RETRY_TIMEOUT = 5;

    /**
     * Единицы измерения тайм-аута переподключения к каналу для одного соединения.
     */
    public static final TimeUnit RECONNECT_RETRY_TIMEOUT_UNITS = TimeUnit.MINUTES;

    private ChannelConstants() {
    }
}
