package net.cache.bus.one_nio.serialization;

import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.CacheEntryEventType;
import net.cache.bus.core.impl.ImmutableCacheEntryEvent;
import net.cache.bus.core.transport.CacheEntryEventConverter;
import one.nio.serial.CalcSizeStream;
import one.nio.serial.DeserializeStream;
import one.nio.serial.SerializeStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.Serializable;

/**
 * Реализация конвертера на основе библиотеки OneNio.
 * Является рекомендуемой по-умолчанию реализацией за счет своей скорости и компактности формата.
 *
 * @author Alik
 * @see CacheEntryEventConverter
 */
@ThreadSafe
@Immutable
public final class OneNioCacheEntryEventConverter implements CacheEntryEventConverter {

    private static final Logger logger = LoggerFactory.getLogger(OneNioCacheEntryEventConverter.class);

    @Nonnull
    @Override
    public <K extends Serializable, V extends Serializable> byte[] toBinary(@Nonnull CacheEntryEvent<K, V> event, boolean serializeValueFields) {

        try (final CalcSizeStream css = new CalcSizeStream()) {
            writeTo(css, event, serializeValueFields);

            final byte[] buf = new byte[css.count()];
            try (final SerializeStream out = new SerializeStream(buf, css.capacity())) {
                writeTo(out, event, serializeValueFields);
            }

            return buf;
        } catch (IOException ex) {
            logger.error("Unable to serialize event: " + event, ex);
            throw new RuntimeException(ex);
        }
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <K extends Serializable, V extends Serializable> CacheEntryEvent<K, V> fromBinary(@Nonnull byte[] data) {

        try (final DeserializeStream in = new DeserializeStream(data)) {
            final byte keyType = in.readByte();
            final K key = (K) (keyType == 1 ? in.readUTF() : in.readObject());
            final long eventTime = in.readLong();

            final CacheEntryEventType eventType = CacheEntryEventType.valueOf(in.readByte());
            if (eventType == null) {
                throw new NullPointerException();
            }

            final String cacheName = in.readUTF();

            final V oldValue = (V) in.readObject();
            final V newValue = (V) in.readObject();

            return new ImmutableCacheEntryEvent<>(key, oldValue, newValue, eventTime, eventType, cacheName);
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

        output.writeLong(event.eventTime());

        output.writeByte(event.eventType().getId());
        output.writeUTF(event.cacheName());

        output.writeObject(serializeValueFields ? event.oldValue() : null);
        output.writeObject(serializeValueFields ? event.newValue() : null);
    }

    private int getKeyType(final Object key) {
        return key.getClass() == String.class ? 1 : 2;
    }
}
