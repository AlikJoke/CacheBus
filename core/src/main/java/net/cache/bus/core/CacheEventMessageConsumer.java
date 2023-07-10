package net.cache.bus.core;

import javax.annotation.Nonnull;
import java.io.Closeable;

/**
 * Потребитель сообщений об изменении элементов кэша с других серверов,
 * выполняющий применение изменений к локальному кэшу.
 *
 * @author Alik
 * @see CacheBus
 */
public interface CacheEventMessageConsumer extends Closeable {

    /**
     * Потребляет хэш-ключ сообщения и тело в бинарном формате и выполняет
     * применение изменения к локальному кэшу.
     *
     * @param messageHash хэш-ключ сообщения
     * @param messageBody тело сообщения в бинарном формате, не может быть {@code null}.
     */
    void accept(int messageHash, @Nonnull byte[] messageBody);

    /**
     * Производит закрытие потребителя сообщений и связанных с ним ресурсов, если это необходимо.
     */
    @Override
    default void close() {
    }
}
