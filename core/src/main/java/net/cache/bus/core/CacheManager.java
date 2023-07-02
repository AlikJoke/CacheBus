package net.cache.bus.core;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.Set;

/**
 * Менеджер кэшей. Предоставляет доступ к кэшам разного рода, а также
 * различного вида общим настройкам о кэшировании.
 *
 * @see Cache
 *
 * @author Alik
 *
 */
public interface CacheManager {

    /**
     * Возвращает "нижестоящий" менеджер кэшей, на основе которого реализуется
     * данный менеджер. Нижестоящий менеджер может не существовать, если
     * реализация менеджера не основана на уже существующей реализации какого-то
     * провайдера кэширования - в таком случае реализация должна генерировать
     * исключение {@linkplain UnsupportedOperationException}.
     *
     * @return нижестоящий менеджер кэшей некоторого провайдера кэширования, если таковой имеется; не может быть {@code null}.
     * @throws UnsupportedOperationException если нижестоящий менеджер кэшей не существует
     */
    @Nonnull
    <T> T getUnderlyingCacheManager();

    /**
     * Возвращает список названий кэшей, управляемых данным менеджером.
     *
     * @see #getCache(String)
     *
     * @return список названий кэшей, не может быть {@code null}.
     */
    @Nonnull
    Set<String> getCacheNames();

    /**
     * Возвращает кэш по его имени.
     *
     * @see Cache
     *
     * @param cacheName имя кэша, не может быть {@code null}.
     * @return объект кэша или {@linkplain Optional#empty()}, если кэша с таким именем не существует.
     */
    @Nonnull
    <K> Optional<Cache<K>> getCache(@Nonnull String cacheName);
}