package net.cache.bus.jdk.serialization;

import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.CacheEntryEventType;
import net.cache.bus.core.impl.ImmutableCacheEntryEvent;
import net.cache.bus.core.transport.CacheEntryEventConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.*;

/**
 * Реализация конвертера на основе стандартной JDK-сериализации.
 * Не рекомендуется для использования в случае, если используется реплицируемый кэш
 * со сложной структурой значений или если ключи кэширования имеют сложную структуру.
 * Под "сложной" структурой тут имеется в виду структура, которая может часто меняться
 * или содержит не сериализуемые поля.
 * <br/>
 * Транспортный формат обладает всеми недостатками обычного формата JDK-сериализации.
 *
 * @author Alik
 * @see CacheEntryEventConverter
 */
@ThreadSafe
@Immutable
public final class JdkCacheEntryEventConverter implements CacheEntryEventConverter {

    private static final Logger logger = LoggerFactory.getLogger(JdkCacheEntryEventConverter.class);

    private static final int BUF_SIZE = 512;

    @Nonnull
    @Override
    public <K extends Serializable, V extends Serializable> byte[] toBinary(@Nonnull CacheEntryEvent<K, V> event, boolean serializeValueFields) {

        try (final var bos = new ByteArrayOutputStream(BUF_SIZE);
             final var oos = new ObjectOutputStream(bos)) {
            writeTo(oos, event, serializeValueFields);

            return bos.toByteArray();
        } catch (IOException ex) {
            logger.error("Unable to serialize event: " + event, ex);
            throw new RuntimeException(ex);
        }
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <K extends Serializable, V extends Serializable> CacheEntryEvent<K, V> fromBinary(@Nonnull byte[] data) {

        try (final var bis = new ByteArrayInputStream(data);
             final var ois = new ObjectInputStream(bis)) {
            final byte keyType = ois.readByte();
            final K key = (K) (keyType == 1 ? ois.readUTF() : ois.readObject());
            final CacheEntryEventType eventType = CacheEntryEventType.valueOf(ois.readByte());
            if (eventType == null) {
                throw new NullPointerException();
            }

            final String cacheName = ois.readUTF();

            final V oldValue = (V) ois.readObject();
            final V newValue = (V) ois.readObject();

            return new ImmutableCacheEntryEvent<>(key, oldValue, newValue, eventType, cacheName);
        } catch (IOException | ClassNotFoundException e) {
            logger.error("Unable to deserialize from binary event", e);
            throw new RuntimeException(e);
        }
    }

    private void writeTo(
            final ObjectOutput output,
            final CacheEntryEvent<?, ?> event,
            final boolean serializeValueFields) throws IOException {
        // Небольшая оптимизация для строк: подавляющее большинство ключей в кэшах - строки. readUTF экономнее, чем readObject для строк.
        final int keyType = getKeyType(event.key());
        output.writeByte(keyType);

        if (keyType == 1) {
            output.writeUTF(event.key().toString());
        } else {
            output.writeObject(event.key());
        }

        output.writeByte(event.eventType().getId());
        output.writeUTF(event.cacheName());

        output.writeObject(serializeValueFields ? event.oldValue() : null);
        output.writeObject(serializeValueFields ? event.newValue() : null);
    }

    private int getKeyType(final Object key) {
        return key.getClass() == String.class ? 1 : 2;
    }
}
