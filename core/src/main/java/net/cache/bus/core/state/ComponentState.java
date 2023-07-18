package net.cache.bus.core.state;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Возвращает информацию о состоянии одного компонента шины.
 *
 * @author Alik
 */
public interface ComponentState {

    /**
     * Возвращает идентификатор компонента шина.
     *
     * @return не может быть {@code null}.
     */
    @Nonnull
    String componentId();

    /**
     * Возвращает общий статус компонента шины.
     *
     * @return не может быть {@code null}.
     * @see Status
     */
    @Nonnull
    Status status();

    /**
     * Возвращает признак наличия проблем в работе компонента.
     *
     * @return признак наличия проблем в работе компонента
     * @see ComponentState#severities()
     */
    default boolean hasSeverities() {
        return !severities().isEmpty();
    }

    /**
     * Возвращает список зарегистрированных проблем в работе компонента.
     *
     * @return не может быть {@code null}.
     */
    @Nonnull
    List<SeverityInfo> severities();

    /**
     * Информация об одной зарегистрированной проблеме в работе компонента.
     *
     * @author Alik
     */
    interface SeverityInfo {

        /**
         * Возвращает строковое представление проблемы в работе компонента.
         *
         * @return не может быть {@code null}.
         */
        @Nonnull
        String asString();
    }

    /**
     * Статус функционирования компонента.
     *
     * @author Alik
     */
    enum Status {

        /**
         * Запущен / активен
         */
        UP_OK,

        /**
         * Запущен, но находится в невосстановимом состоянии
         */
        UP_FATAL_BROKEN,

        /**
         * В процессе запуска / активируется
         */
        UP_NOT_READY,

        /**
         * Остановлен / неактивен
         */
        DOWN
    }
}
