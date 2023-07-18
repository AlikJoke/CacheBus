package net.cache.bus.core;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Абстрагированное от конкретной реализации провайдера кэширования
 * представление кэша. Поддерживает базовые операции для работы с кэшем:
 * <ul>
 * <li>Получение объекта из кэша по ключу</li>
 * <li>Удаление объекта из кэша по ключу</li>
 * <li>Добавление элемента в кэш</li>
 * <li>Очищение содержимого кэша</li>
 * </ul>
 * <br>
 * Конкретной реализацией может быть как локальный кэш, так и инвалидационной или реплицируемый. <br>
 *
 * @param <K> тип ключа элементов кэша, должен быть сериализуемым
 * @param <V> тип значения элементов кэша, должен быть сериализуемым
 * @author Alik
 */
public interface Cache<K extends Serializable, V extends Serializable> {

    /**
     * Возвращает имя кэша.
     *
     * @return не может быть {@code null}.
     */
    String getName();

    /**
     * Получает из кэша значение по ключу, если оно имеется в кэше.
     * Для более строгой типизации используется паттерн "токен типа", если тип значения не известен,
     * то можно использовать {@link Object}.
     *
     * @param key ключ элемента в кэше, не может быть {@code null}.
     * @return значение в кэше по указанному ключу, обернутое в {@link Optional}. Значение может отсутствовать.
     */
    @Nonnull
    @CheckReturnValue
    Optional<V> get(@Nonnull K key);

    /**
     * Удаляет элемент из кэша по ключу. Если элемента с таким ключом
     * не существует, то ничего не происходит. <br>
     *
     * @param key ключ элемента в кэше, не может быть {@code null}.
     */
    void evict(@Nonnull K key);

    /**
     * Удаляет элемент из кэша по ключу. Возвращает значение, ранее ассоциированное с ключом.
     * Если элемента с таким ключом не существует, возвращает {@code null}.
     *
     * @param key ключ элемента в кэше, не может быть {@code null}.
     * @return значение, ранее ассоциированное с данным ключем, обернутое в {@link Optional}. Значение может отсутствовать.
     */
    @Nonnull
    @CheckReturnValue
    Optional<V> remove(@Nonnull K key);

    /**
     * Добавляет элемент в кэш. Производит замену элемента на новое значение, если элемент уже есть в кэше.
     *
     * @param key   ключ элемента, не может быть {@code null}.
     * @param value значение элемента, может быть {@code null}.
     */
    void put(@Nonnull K key, @Nullable V value);

    /**
     * Добавляет элемент в кэш, если еще нет ассоциированного с данным ключом значения.
     *
     * @param key   ключ элемента, не может быть {@code null}.
     * @param value значение элемента, может быть {@code null}.
     */
    void putIfAbsent(@Nonnull K key, @Nullable V value);

    /**
     * Выполняет очистку данного кэша.
     */
    void clear();

    /**
     * Выполняет слияние уже существующего элемента в кэше с данным ключом с
     * новым значением в соответствии с переданной функцией слияния. Позволяет
     * избежать конфликтов при одновременном изменении одного элемента
     * (например, в случае изменения объекта, содержащего в себе коллекцию или
     * ассоциативный массив) несколькими потоками.<br>
     * В случае, если элемента в кэше не существует, будет выполнено его
     * добавление в кэш без операции слияния. <br>
     * Функция слияния должна возвращать {@code null}, если значение для этого ключа
     * должно быть удалено из кэша. <br>
     * Реализация операции должна быть безопасной с точки зрения многопоточного доступа
     * к кэшу и должна учитывать возможность изменения элемента в кэше параллельным потоком.
     *
     * @param key           ключ элемента в кэше; не может быть {@code null}.
     * @param value         новое значение элемента для добавления или модификации; не может быть {@code null}.
     * @param mergeFunction функция слияния нового значения с уже существующим в кэше, не может быть {@code null}.
     */
    void merge(@Nonnull K key, @Nonnull V value, @Nonnull BiFunction<? super V, ? super V, ? extends V> mergeFunction);

    /**
     * Выполняет вычисление элемента в кэше, если его не существовало до этого.
     * Если элемент с таким ключом уже существует, будет выдан он и вычисление
     * не произойдет. Семантика операции аналогична семантике метода
     * {@linkplain ConcurrentMap#computeIfAbsent(Object, Function)}.
     *
     * @param key           ключ элемента в кэше; не может быть {@code null}.
     * @param valueFunction функция вычисления значения элемента при его отсутствии в кэше; не может быть {@code null}.
     * @return значение элемента из кэша (уже существующее на момент вызова или добавленное в результате него), обернутое в {@link Optional}.
     */
    @CheckReturnValue
    @Nonnull
    Optional<V> computeIfAbsent(@Nonnull K key, @Nonnull Function<? super K, ? extends V> valueFunction);

    /**
     * Регистрирует слушатель событий изменения элементов кэша.
     *
     * @param listener слушатель событий, не может быть {@code null}.
     * @see CacheEventListener
     */
    void registerEventListener(@Nonnull CacheEventListener<K, V> listener);

    /**
     * Снимает с кэша заданный слушатель событий, если он был ранее зарегистрирован.
     *
     * @param listener слушатель событий, который был зарегистрирован для кэша ранее, не может быть {@code null}.
     * @see CacheEventListener
     */
    void unregisterEventListener(@Nonnull CacheEventListener<K, V> listener);
}