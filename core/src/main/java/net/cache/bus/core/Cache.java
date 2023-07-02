package net.cache.bus.core;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
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
 * @author Alik
 */
public interface Cache<K> {

    /**
     * Получает из кэша значение по ключу, если оно имеется в кэше.
     * Для более строгой типизации используется паттерн "токен типа", если тип значения не известен,
     * то можно использовать {@link Object}.
     *
     * @param key       ключ элемента в кэше, не может быть {@code null}.
     * @param valueType токен типа значения кэша, не может быть {@code null}.
     * @return значение в кэше по указанному ключу, обернутое в {@link Optional}. Значение может отсутствовать.
     */
    @Nonnull
    @CheckReturnValue
    <T> Optional<T> get(@Nonnull K key, @Nonnull Class<T> valueType);

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
     * @param key       ключ элемента в кэше, не может быть {@code null}.
     * @param valueType токен типа значения кэша, не может быть {@code null}.
     * @return значение, ранее ассоциированное с данным ключем, обернутое в {@link Optional}. Значение может отсутствовать.
     */
    @Nonnull
    @CheckReturnValue
    <T> Optional<T> remove(@Nonnull K key, @Nonnull Class<T> valueType);

    /**
     * Добавляет элемент в кэш. Производит замену элемента на новое значение, если элемент уже есть в кэше.
     *
     * @param key   ключ элемента, не может быть {@code null}.
     * @param value значение элемента, не может быть {@code null}.
     * @return предыдущее значение элемета с данным ключом в кэше, если такое имелось, обернутое в {@link Optional}. Значение может отсутствовать.
     */
    @Nonnull
    @CheckReturnValue
    <T> Optional<T> put(@Nonnull K key, @Nonnull T value);

    /**
     * Добавляет элемент в кэш, если еще нет ассоциированного с данным ключом значения.
     *
     * @param key   ключ элемента, не может быть {@code null}.
     * @param value значение элемента, не может быть {@code null}.
     * @return предыдущее значение элемета с данным ключом в кэше, если такое имелось, обернутое в {@link Optional}. Значение может отсутствовать.
     */
    @Nonnull
    @CheckReturnValue
    <T> Optional<T> putIfAbsent(@Nonnull K key, @Nonnull T value);

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
     *
     * @param key           ключ элемента в кэше; не может быть {@code null}.
     * @param value         новое значение элемента для добавления или модификации; не может быть {@code null}.
     * @param mergeFunction функция слияния нового значения с уже существующим в кэше, не может быть {@code null}.
     * @return предыдущее значение элемента в кэше, обернутое в {@link Optional}.
     */
    @Nonnull
    @CheckReturnValue
    <T> Optional<T> merge(@Nonnull K key, @Nonnull T value, @Nonnull BiFunction<? super T, ? super T, ? extends T> mergeFunction);

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
    <T> Optional<T> computeIfAbsent(@Nonnull K key, @Nonnull Function<? super K, ? extends T> valueFunction);
}