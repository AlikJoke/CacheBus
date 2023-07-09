package net.cache.bus.core.testing.transport;

import net.cache.bus.core.CacheEntryEvent;
import net.cache.bus.core.CacheEntryEventType;
import net.cache.bus.core.impl.ImmutableCacheEntryEvent;
import net.cache.bus.core.transport.CacheEntryEventConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@Execution(ExecutionMode.CONCURRENT)
public abstract class BaseCacheEntryEventConverterTest {

    private final CacheEntryEventConverter converter = createConverter();

    @Test
    public void testWhenEventWithValuesAndSimpleKeyThenConversionSuccess() {
        final CacheEntryEvent<String, Value> event = composeEvent(
                "test1",
                new Value("v1", 2, 13.5, new HashSet<>(Set.of(new Date(1002))), true, null),
                null,
                CacheEntryEventType.EVICTED
        );
        final byte[] eventSerializedWithValues = converter.toBinary(event, true);
        final byte[] eventSerializedWithoutValues = converter.toBinary(event, false);

        assertTrue(eventSerializedWithValues.length > eventSerializedWithoutValues.length,
                "Binary representation of event with values must be large than representation without values");

        final CacheEntryEvent<String, Value> deserializedEventWithValues = converter.fromBinary(eventSerializedWithValues);
        final CacheEntryEvent<String, Value> deserializedEventWithoutValues = converter.fromBinary(eventSerializedWithoutValues);

        assertNotNull(deserializedEventWithValues, "Event after deserialization must be not null");
        assertNotNull(deserializedEventWithoutValues, "Event after deserialization must be not null");

        assertEquals(event, deserializedEventWithValues, "Source and deserialized event must be equal");

        assertEquals(event.cacheName(), deserializedEventWithoutValues.cacheName(), "Cache name must be equal");
        assertEquals(event.eventType(), deserializedEventWithoutValues.eventType(), "Event type must be equal");
        assertEquals(event.key(), deserializedEventWithoutValues.key(), "Cache key must be equal");
        assertNull(deserializedEventWithoutValues.oldValue(), "Old cache value must be null");
        assertNull(deserializedEventWithoutValues.newValue(), "New cache value must be null");
    }

    @Test
    public void testWhenEventWithoutValuesAndSimpleKeyThenConversionSuccess() {
        final CacheEntryEvent<String, Value>  event = composeEvent(
                "test1",
                null,
                null,
                CacheEntryEventType.UPDATED
        );
        final byte[] eventSerialized1 = converter.toBinary(event, true);
        final byte[] eventSerialized2 = converter.toBinary(event, false);

        final CacheEntryEvent<String, Value> deserializedEvent1 = converter.fromBinary(eventSerialized1);
        final CacheEntryEvent<String, Value> deserializedEvent2 = converter.fromBinary(eventSerialized2);

        assertNotNull(deserializedEvent1, "Event after deserialization must be not null");
        assertNotNull(deserializedEvent2, "Event after deserialization must be not null");

        assertEquals(event, deserializedEvent1, "Source and deserialized event must be equal");
        assertEquals(event, deserializedEvent2, "Source and deserialized event must be equal");
    }

    @Test
    public void testWhenEventHasComplexKeyThenConversionSuccess() {
        final CacheEntryEvent<Key, Value>  event = composeEvent(
                new Key("123", 23, new Key("-", 65, null)),
                new Value("v1", 2, 13.5, new HashSet<>(Set.of(new Date(1002))), true, null),
                new Value("v2", 4, 413.5, new HashSet<>(Set.of(new Date(2001))), false, new Value("3", 123, 54.9, null, true, null)),
                CacheEntryEventType.UPDATED
        );
        final byte[] eventSerialized = converter.toBinary(event, true);
        final CacheEntryEvent<Key, Value> deserializedEvent = converter.fromBinary(eventSerialized);

        assertEquals(event, deserializedEvent, "Source and deserialized event must be equal");
    }

    @Nonnull
    protected abstract CacheEntryEventConverter createConverter();

    private <K extends Serializable, V extends Serializable> CacheEntryEvent<K, V> composeEvent(
            final K key,
            final V oldValue,
            final V newValue,
            final CacheEntryEventType eventType) {
        return new ImmutableCacheEntryEvent<>(key, oldValue, newValue, eventType, UUID.randomUUID().toString());
    }

    protected record Key(
            String keyStr,
            long keyLong,
            Key keyComplex) implements Serializable {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (keyLong != key.keyLong) return false;
            if (!Objects.equals(keyStr, key.keyStr)) return false;
            return Objects.equals(keyComplex, key.keyComplex);
        }

        @Override
        public int hashCode() {
            int result = keyStr != null ? keyStr.hashCode() : 0;
            result = 31 * result + (int) (keyLong ^ (keyLong >>> 32));
            result = 31 * result + (keyComplex != null ? keyComplex.hashCode() : 0);
            return result;
        }
    }

    protected record Value(
            String fieldStr,
            int fieldInt,
            Double fieldDouble,
            Set<Date> fieldDateSet,
            boolean fieldBool,
            Value fieldValue) implements Serializable {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Value value = (Value) o;

            if (fieldInt != value.fieldInt) return false;
            if (fieldBool != value.fieldBool) return false;
            if (!Objects.equals(fieldStr, value.fieldStr)) return false;
            if (!Objects.equals(fieldDouble, value.fieldDouble)) return false;
            if (!Objects.equals(fieldDateSet, value.fieldDateSet))
                return false;
            return Objects.equals(fieldValue, value.fieldValue);
        }
    }
}
