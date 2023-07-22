package net.cache.bus.core.configuration;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashSet;
import java.util.Set;

/**
 * Источник конфигурации кэшей, подключенных к шине.
 *
 * @author Alik
 * @see CacheConfiguration
 * @see CacheBusConfiguration
 * @see CacheSetConfiguration
 * @see SimpleCacheConfigurationSource
 */
public interface CacheConfigurationSource {

    /**
     * Возвращает конфигурацию кэшей из источника.
     *
     * @return не может быть {@code null}.
     */
    @Nonnull
    CacheSetConfiguration pull();

    /**
     * Возвращает источник конфигурации кэшей по-умолчанию, конфигурируемый вручную.
     *
     * @return не может быть {@code null}.
     * @see SimpleCacheConfigurationSource
     */
    @Nonnull
    static SimpleCacheConfigurationSource createDefault() {
        return new SimpleCacheConfigurationSource();
    }

    /**
     * Источник конфигурации кэшей, настраиваемый вручную.<br>
     *
     * @author Alik
     * @implSpec Реализация не является потокобезопасной, поэтому ссылку на источник передавать
     * можно только в объект конфигурации шины. При публикации ссылки извне результат не определен,
     * если источник будет модифицироваться в других потоках.
     * @see CacheConfigurationSource
     */
    @NotThreadSafe
    final class SimpleCacheConfigurationSource implements CacheConfigurationSource {

        private final Set<CacheConfiguration> configurations = new HashSet<>();
        private boolean useAsyncCleaning;

        @Nonnull
        @Override
        public CacheSetConfiguration pull() {
            final Set<CacheConfiguration> cacheConfigurations = Set.copyOf(this.configurations);
            return new CacheSetConfiguration() {
                @Nonnull
                @Override
                public Set<CacheConfiguration> cacheConfigurations() {
                    return cacheConfigurations;
                }

                @Override
                public boolean useAsyncCleaning() {
                    return useAsyncCleaning;
                }
            };
        }

        /**
         * Добавляет конфигурацию одного кэша в источник.
         *
         * @param configuration конфигурация кэша, не может быть {@code null}.
         * @return источник для дальнейшего построения, не может быть {@code null}.
         */
        @Nonnull
        public SimpleCacheConfigurationSource add(@Nonnull CacheConfiguration configuration) {
            this.configurations.add(configuration);
            return this;
        }

        /**
         * Добавляет конфигурации кэшей в источник.
         *
         * @param configurations конфигурации кэшей, не может быть {@code null}.
         * @return источник для дальнейшего построения, не может быть {@code null}.
         */
        @Nonnull
        public SimpleCacheConfigurationSource addAll(@Nonnull Set<CacheConfiguration> configurations) {
            this.configurations.addAll(configurations);
            return this;
        }

        /**
         * Очищает источник кэшей.
         *
         * @return источник для дальнейшего построения, не может быть {@code null}.
         */
        @Nonnull
        public SimpleCacheConfigurationSource clear() {
            this.configurations.clear();
            return this;
        }

        /**
         * Устанавливает признак использования асинхронного пула для очистки устаревших временных
         * меток изменения элементов кэша.
         *
         * @param useAsyncCleaning признак использования асинхронного пула для очистки устаревших временных меток
         * @return не может быть {@code null}.
         */
        @Nonnull
        public SimpleCacheConfigurationSource useAsyncCleaning(boolean useAsyncCleaning) {
            this.useAsyncCleaning = useAsyncCleaning;
            return this;
        }

        @Override
        public String toString() {
            return "SimpleCacheSetConfigurationSource{" +
                    "configurations=" + configurations +
                    ", useAsyncCleaning=" + useAsyncCleaning +
                    '}';
        }
    }
}
