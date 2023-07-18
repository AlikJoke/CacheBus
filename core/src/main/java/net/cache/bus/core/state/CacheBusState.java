package net.cache.bus.core.state;

import javax.annotation.Nonnull;

/**
 * Состояние шины кэшей. Содержит информацию о состоянии шины (в целом) и всех ее компонент в отдельности.
 *
 * @author Alik
 * @see ComponentState
 */
public interface CacheBusState extends ComponentState {

    /**
     * Возвращает информацию о состоянии канала входящих / исходящих сообщений.
     *
     * @return не может быть {@code null}.
     */
    @Nonnull
    ComponentState channelState();

    /**
     * Возвращает информацию о состоянии очереди обработки поступивших с других
     * серверов сообщений об изменении элементов кэша.
     *
     * @return не может быть {@code null}.
     */
    @Nonnull
    ComponentState processingQueueState();

    /**
     * Возвращает информацию о состоянии очереди отправки исходящих сообщений об изменении элементов кэша.
     *
     * @return не может быть {@code null}.
     */
    @Nonnull
    ComponentState sendingQueueState();

    /**
     * Возвращает информацию о состоянии менеджера кэшей.
     * @return не может быть {@code null}.
     */
    @Nonnull
    ComponentState cacheManagerState();
}
