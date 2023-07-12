package net.cache.bus.core.impl.internal.util;

import net.cache.bus.core.configuration.ConfigurationException;

import javax.annotation.Nonnull;
import java.util.concurrent.Semaphore;

/**
 * Реализация структуры данных кольцевого буфера для обработки взаимодействия single producer vs single consumer.<br>
 * Особенность данной реализации в том, что она блокируется в случае, если производится добавление в буфер
 * и буфер полон, а также если производится чтение из буфера и буфер пуст. Методы возвращают управление в
 * этих случаях только тогда, когда в буфере появится место (при добавлении) или в буфере появятся
 * данные (при чтении).
 *
 * @param <E> тип хранимых в буфере данных
 * @author Alik
 */
public final class RingBuffer<E> {

    private final Semaphore readSemaphore;
    private final Semaphore writeSemaphore;
    private final E[] elements;
    private final int capacity;

    private volatile int writeCounter;
    private volatile int readCounter;

    public RingBuffer(int capacity) {

        if (capacity <= 0) {
            throw new ConfigurationException("Buffer capacity must be positive: " + capacity);
        }

        this.capacity = capacity;
        @SuppressWarnings("unchecked")
        final E[] elements = (E[]) new Object[capacity];
        this.elements = elements;
        this.writeCounter = -1;
        this.readCounter = 0;
        this.readSemaphore = new Semaphore(1);
        this.writeSemaphore = new Semaphore(1);
    }

    /**
     * Добавляет элемент в буфер. Метод блокируется, если буфер полон.
     *
     * @param elem элемент для добавления в буфер, не может быть {@code null}.
     */
    public void offer(@Nonnull final E elem) throws InterruptedException {

        int currentWriteValue;
        while ((currentWriteValue = this.writeCounter) - this.readCounter == this.capacity - 1) {
            this.writeSemaphore.acquire();
        }

        final int nextCounter = currentWriteValue + 1;
        this.elements[nextCounter % this.capacity] = elem;
        this.writeCounter = nextCounter;
        this.readSemaphore.release();
    }

    /**
     * Возвращает элемент из буфера. Метод блокируется, если данных в буфере нет.
     *
     * @return элемент данных из буфера, не может быть {@code null}.
     */
    @Nonnull
    public E poll() throws InterruptedException {
        int currentReadPosition;
        while (this.writeCounter < (currentReadPosition = this.readCounter)) {
            this.readSemaphore.acquire();
        }

        final E elem = this.elements[currentReadPosition % this.capacity];
        this.readCounter = currentReadPosition + 1;
        this.writeSemaphore.release();
        return elem;
    }
}
