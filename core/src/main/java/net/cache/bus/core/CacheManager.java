package net.cache.bus.core;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Менеджер кэшей. Предоставляет доступ к кэшам разного рода, а также
 * различного вида общим настройкам о кэшировании.
 *
 * @author Alik
 * @see Cache
 */
public interface CacheManager {

    /**
     * Возвращает "нижестоящий" менеджер кэшей, на основе которого реализуется
     * данный менеджер. Нижестоящий менеджер может не существовать, если
     * реализация менеджера не основана на уже существующей реализации какого-то
     * провайдера кэширования - в таком случае реализация должна генерировать
     * исключение {@linkplain UnsupportedOperationException}.
     *
     * @param managerType тип исходного менеджера кэшей, не может быть {@code null}.
     * @return нижестоящий менеджер кэшей некоторого провайдера кэширования, если таковой имеется; не может быть {@code null}.
     * @throws UnsupportedOperationException если нижестоящий менеджер кэшей не существует
     */
    @Nonnull
    <T> T getUnderlyingCacheManager(@Nonnull Class<T> managerType);

    /**
     * Возвращает кэш по его имени.
     *
     * @param cacheName имя кэша, не может быть {@code null}.
     * @return объект кэша или {@linkplain Optional#empty()}, если кэша с таким именем не существует.
     * @see Cache
     */
    @Nonnull
    <K, V> Optional<Cache<K, V>> getCache(@Nonnull String cacheName);
}