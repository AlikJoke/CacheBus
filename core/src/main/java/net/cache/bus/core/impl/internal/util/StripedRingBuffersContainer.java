package net.cache.bus.core.impl.internal.util;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

/**
 * Контейнер, содержащий кольцевые буферы. Обращения к буферам производится по индексу.
 * Индекс является "полосой", т.к. контейнер используется для разбиения данных на "полосы",
 * которые обрабатываются в разных потоках.
 *
 * @param <E> тип данных, хранимых в буферах
 * @author Alik
 * @see RingBuffer
 */
public final class StripedRingBuffersContainer<E> {

    private static final int RING_BUFFER_DEFAULT_CAPACITY = 100;

    private final RingBuffer<E>[] buffers;

    /**
     * Конструктор контейнера, создающий набор буферов с размером по-умолчанию {@linkplain StripedRingBuffersContainer#RING_BUFFER_DEFAULT_CAPACITY}.
     *
     * @param stripes количество "полос" (количество буферов)
     */
    public StripedRingBuffersContainer(final int stripes) {
        this(stripes, RING_BUFFER_DEFAULT_CAPACITY);
    }

    /**
     * Конструктор контейнера, создающий набор буферов заданного размера.
     *
     * @param stripes количество "полос" (количество буферов); доводится до ближайшего четного (в меньшую сторону), если число нечетное.
     * @param bufferCapacity вместимость (размер) каждого буфера
     */
    public StripedRingBuffersContainer(final int stripes, final int bufferCapacity) {
        final int evenStripes = stripes % 2 == 1 ? stripes - 1 : stripes;
        @SuppressWarnings("unchecked")
        final RingBuffer<E>[] buffers = new RingBuffer[evenStripes];
        this.buffers = buffers;

        for (int i = 0; i < evenStripes; i++) {
            this.buffers[i] = new RingBuffer<>(bufferCapacity);
        }
    }

    /**
     * Возвращает кольцевой буфер по его индексу (полосе).
     *
     * @param index индекс буфера
     * @return кольцевой буфер с заданным индексом, не может быть {@code null}.
     */
    @Nonnull
    public RingBuffer<E> get(final int index) {
        return this.buffers[index];
    }

    /**
     * Возвращает размер контейнера (количество полос).
     * @return количество полос, не может быть {@code null}.
     */
    @Nonnegative
    public int size() {
        return this.buffers.length;
    }
}
