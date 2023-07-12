package net.cache.bus.core.configuration;

import net.cache.bus.core.transport.CacheBusMessageChannel;
import net.cache.bus.core.transport.CacheEntryEventConverter;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.concurrent.ExecutorService;

/**
 * Конфигурация транспорта для данной шины кэшей.
 *
 * @author Alik
 * @see CacheEntryEventConverter
 * @see CacheBusConfiguration
 * @see CacheBusMessageChannel
 */
public interface CacheBusTransportConfiguration {

    /**
     * Возвращает конвертер для формирования "транспортного" представления события
     * об изменении элемента кэша и обратного преобразования из бинарного представления
     * в объект типа {@link net.cache.bus.core.CacheEntryEvent}.
     *
     * @return не может быть {@code null}.
     * @see CacheEntryEventConverter
     */
    @Nonnull
    CacheEntryEventConverter converter();

    /**
     * Возвращает канал сообщений, используемый для взаимодействия с другими серверами.
     *
     * @return канал сообщений, не может быть {@code null}.
     * @see CacheBusMessageChannel
     */
    @Nonnull
    CacheBusMessageChannel<CacheBusMessageChannelConfiguration> messageChannel();

    /**
     * Возвращает конфигурацию канала сообщений шины кэшей.
     *
     * @return не может быть {@code null}.
     * @see CacheBusMessageChannelConfiguration
     */
    @Nonnull
    CacheBusMessageChannelConfiguration messageChannelConfiguration();

    /**
     * Возвращает пул потоков, на котором производится обработка сообщений
     * (десериализация и применение к локальным кэшам) с других серверов.
     *
     * @return не может быть {@code null}.
     */
    @Nonnull
    ExecutorService processingPool();

    /**
     * Возвращает максимальное количество потоков, которое может заниматься обработкой
     * полученных с других серверов сообщений.
     * @return максимальное количество потоков обработки, не может быть отрицательным;
     * если значение {@code 0}, то получение из канала сообщений и их обработка производятся в одном потоке.
     */
    @Nonnegative
    int maxConcurrentProcessingThreads();

    /**
     * Возвращает максимальный размер буфера одного потока обработки сообщений.
     * Каждый поток имеет свой буфер данного размера.
     * Размер буфера влияет на пропускную способность обработки сообщений из канала, чем больше буфер,
     * тем выше пропускная способность (ниже вероятность того, что произойдет блокировка потока записи
     * (он же поток чтения из канала) из-за того, что потоки-обработчики сообщений не успевают обработать
     * сообщения). С другой стороны, чем больше размер буфера, тем больше потребление памяти.<br>
     * Если {@code maxConcurrentProcessingThreads() == 0}, то буферы не используются и значение будет проигнорировано.
     *
     * @return максимальный размер буфера одного потока обработки сообщений, не может быть отрицательным;
     * при значении {@code 0} будет использоваться значение по-умолчанию ({@code 256}).
     */
    @Nonnegative
    int maxProcessingThreadBufferCapacity();

    /**
     * Возвращает признак, надо ли использовать синхронную обработку полученных из канала сообщений.
     *
     * @return {@code true}, если надо использовать синхронную обработку сообщений, иначе {@code false}.
     */
    default boolean useSynchronousProcessing() {
        return maxConcurrentProcessingThreads() == 0;
    }
}
