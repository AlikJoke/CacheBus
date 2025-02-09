package ru.joke.cache.bus.jdk.serialization;

import ru.joke.cache.bus.core.CacheEntryEvent;
import ru.joke.cache.bus.core.CacheEntryEventType;
import ru.joke.cache.bus.core.impl.ImmutableCacheEntryEvent;
import ru.joke.cache.bus.core.transport.CacheEntryEventConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.*;

/**
 * Implementation of a converter based on the standard JDK serialization.
 * Not recommended for use in case a replicable cache with complex value structure is used or if caching keys have a complex structure.
 * By "complex" structure, we mean a structure that can change frequently or contains non-serializable fields.<br>
 * The transport format has all the disadvantages of the regular JDK serialization format.
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
            final long eventTime = ois.readLong();
            final CacheEntryEventType eventType = CacheEntryEventType.valueOf(ois.readByte());
            if (eventType == null) {
                throw new NullPointerException();
            }

            final String cacheName = ois.readUTF();

            final V oldValue = (V) ois.readObject();
            final V newValue = (V) ois.readObject();

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
        // A small optimization for strings: the vast majority of cache keys are strings. readUTF is more efficient than readObject for strings.
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
