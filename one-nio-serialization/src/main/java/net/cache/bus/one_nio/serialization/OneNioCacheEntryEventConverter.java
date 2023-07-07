package net.cache.bus.one_nio.serialization;

import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.CacheEntryEventType;
import net.cache.bus.core.impl.ImmutableCacheEntryEvent;
import net.cache.bus.core.transport.CacheEntryEventConverter;
import one.nio.serial.CalcSizeStream;
import one.nio.serial.DeserializeStream;
import one.nio.serial.SerializeStream;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class OneNioCacheEntryEventConverter implements CacheEntryEventConverter {

    private static final Logger logger = Logger.getLogger(OneNioCacheEntryEventConverter.class.getCanonicalName());

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
            logger.log(Level.ALL, "Unable to serialize event: " + event, ex);
            throw new RuntimeException(ex);
        }
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <K extends Serializable, V extends Serializable> CacheEntryEvent<K, V> fromBinary(@Nonnull byte[] data) {

        try (final DeserializeStream in = new DeserializeStream(data)) {
            final K key = (K) in.readObject();
            final CacheEntryEventType eventType = CacheEntryEventType.valueOf(in.readInt());
            if (eventType == null) {
                throw new NullPointerException();
            }

            final String cacheName = in.readUTF();
            final Instant eventTime = (Instant) in.readObject();
            final V oldValue = (V) in.readObject();
            final V newValue = (V) in.readObject();

            return new ImmutableCacheEntryEvent<>(key, oldValue, newValue, eventTime, eventType, cacheName);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeTo(
            final ObjectOutput output,
            final CacheEntryEvent<?, ?> event,
            final boolean serializeValueFields) throws IOException {
        output.writeObject(event.key());
        output.writeInt(event.eventType().getId());
        output.writeUTF(event.cacheName());
        output.writeObject(event.eventTime());
        output.writeObject(serializeValueFields ? event.oldValue() : null);
        output.writeObject(serializeValueFields ? event.newValue() : null);
    }
}
