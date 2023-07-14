package net.cache.bus.transport.addons;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Упрощенная структура очереди, основанная на потокобезопасной очереди {@link ConcurrentLinkedQueue}.
 * Дополняет {@link ConcurrentLinkedQueue} методами с блокированием при получении данных из очереди,
 * когда очередь пуста.
 *
 * @param <E> тип хранимых в очереди элементов данных
 * @author Alik
 * @see ConcurrentLinkedQueue
 */
@ThreadSafe
public final class ConcurrentLinkedBlockingQueue<E> {

    private final Semaphore emptySemaphore = new Semaphore(1);
    private final Queue<E> sourceQueue = new ConcurrentLinkedQueue<>();

    /**
     * Возвращает элемент и удаляет его из головы очереди. Если очередь пуста, то блокируется и ждет появления данных в очереди
     * на протяжении заданного тайм-аута. Если после ожидания по тайм-ауту очередь все так же пуста, то вернется {@code null}.
     *
     * @param timeout время тайм-аута ожидания появления в пустой очереди новых элементов, первый из которых можно будет вернуть; не может быть отрицательным.
     * @param unit    единицы времени ожидания появления в пустой очереди новых элементов, не может быть {@code null}.
     * @return элемент данных из головы очереди или {@code null}, если очередь пуста (после того, как пройдет время тайм-аута).
     */
    @Nullable
    public E poll(@Nonnegative long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        E result;
        while ((result = this.sourceQueue.poll()) == null
                && this.emptySemaphore.tryAcquire(timeout, unit)) ;

        return result;
    }

    /**
     * Удаляет и возвращает элемент из головы очереди. Если очередь пуста, то блокируется до момента появления нового элемента в очереди, который можно вернуть.
     *
     * @return элемент из головы очереди, не может быть {@code null}.
     * @throws InterruptedException если поток был прерван извне
     */
    @Nonnull
    public E take() throws InterruptedException {

        while (true) {
            final E result = this.sourceQueue.poll();
            if (result != null) {
                return result;
            }

            this.emptySemaphore.acquire();
        }
    }

    /**
     * Добавляет в хвост очереди элемент данных.
     *
     * @param elem элемент данных, не может быть {@code null}.
     */
    public void offer(@Nonnull E elem) {
        this.sourceQueue.offer(elem);
        this.emptySemaphore.release();
    }

    /**
     * Выполняет переданное действие над каждым элементом данных очереди в цикле.
     *
     * @param action действие для выполнения над элементом данных, не может быть {@code null}.
     */
    public void forEach(@Nonnull Consumer<? super E> action) {
        this.sourceQueue.forEach(action);
    }

    /**
     * Очищает очередь.
     */
    public void clear() {
        this.sourceQueue.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ConcurrentLinkedBlockingQueue<?> that = (ConcurrentLinkedBlockingQueue<?>) o;
        return sourceQueue.equals(that.sourceQueue);
    }

    @Override
    public int hashCode() {
        return sourceQueue.hashCode();
    }
}
