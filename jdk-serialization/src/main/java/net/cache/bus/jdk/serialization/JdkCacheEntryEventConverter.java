package net.cache.bus.jdk.serialization;

import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.CacheEntryEventType;
import net.cache.bus.core.impl.ImmutableCacheEntryEvent;
import net.cache.bus.core.transport.CacheEntryEventConverter;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.*;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

@ThreadSafe
@Immutable
public final class JdkCacheEntryEventConverter implements CacheEntryEventConverter {

    private static final Logger logger = Logger.getLogger(JdkCacheEntryEventConverter.class.getCanonicalName());

    private static final int BUF_SIZE = 512;

    @Nonnull
    @Override
    public <K extends Serializable, V extends Serializable> byte[] toBinary(@Nonnull CacheEntryEvent<K, V> event, boolean serializeValueFields) {

        try (final var bos = new ByteArrayOutputStream(BUF_SIZE);
             final var oos = new ObjectOutputStream(bos)) {
            writeTo(oos, event, serializeValueFields);

            return bos.toByteArray();
        } catch (IOException ex) {
            logger.log(Level.ALL, "Unable to serialize event: " + event, ex);
            throw new RuntimeException(ex);
        }
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <K extends Serializable, V extends Serializable> CacheEntryEvent<K, V> fromBinary(@Nonnull byte[] data) {

        try (final var bis = new ByteArrayInputStream(data);
             final var ois = new ObjectInputStream(bis)) {
            final K key = (K) ois.readObject();
            final CacheEntryEventType eventType = CacheEntryEventType.valueOf(ois.readInt());
            if (eventType == null) {
                throw new NullPointerException();
            }

            final String cacheName = ois.readUTF();
            final Instant eventTime = (Instant) ois.readObject();
            final V oldValue = (V) ois.readObject();
            final V newValue = (V) ois.readObject();

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
