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
 * @see SimpleCacheConfigurationSource
 */
public interface CacheConfigurationSource {

    /**
     * Возвращает конфигурации кэшей из источника.
     *
     * @return не может быть {@code null}.
     */
    @Nonnull
    Set<CacheConfiguration> pull();

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

        @Nonnull
        @Override
        public Set<CacheConfiguration> pull() {
            return new HashSet<>(this.configurations);
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
    }
}
