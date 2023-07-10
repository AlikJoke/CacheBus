package net.cache.bus.core.impl.internal.util;

import javax.annotation.Nonnull;

/**
 * Реализация структуры данных кольцевого буфера.<br>
 * Особенность данной реализации в том, что она блокируется в случае, если производится добавление в буфер
 * и буфер полон, а также если производится чтение из буфера и буфер пуст. Методы возвращают управление в
 * этих случаях только тогда, когда в буфере появится место (при добавлении) или в буфере появятся
 * данные (при чтении).
 *
 * @param <E> тип хранимых в буфере данных
 * @author Alik
 */
public final class RingBuffer<E> {

    private final E[] elements;
    private final int capacity;

    public RingBuffer(int capacity) {

        if (capacity <= 0) {
            throw new IllegalArgumentException("Buffer capacity must be positive: " + capacity);
        }

        this.capacity = capacity;
        @SuppressWarnings("unchecked")
        final E[] elements = (E[]) new Object[capacity];
        this.elements = elements;
    }

    /**
     * Добавляет элемент в буфер. Метод блокируется, если буфер полон.
     *
     * @param elem элемент для добавления в буфер, не может быть {@code null}.
     */
    public void offer(@Nonnull final E elem) {
        // TODO
    }

    /**
     * Возвращает элемент из буфера. Метод блокируется, если данных в буфере нет.
     *
     * @return элемент данных из буфера, не может быть {@code null}.
     */
    @Nonnull
    public E poll() {
        // TODO
        return null;
    }
}
