package ehcache2.listeners;

import net.cache.bus.core.Cache;
import net.cache.bus.core.CacheBus;
import net.cache.bus.core.CacheEventListener;
import net.cache.bus.core.CacheEventListenerFactory;

import javax.annotation.Nonnull;
import java.util.Objects;

public final class EhCache2CacheEventListenerFactory implements CacheEventListenerFactory {

    private final CacheBus cacheBus;

    public EhCache2CacheEventListenerFactory(@Nonnull CacheBus cacheBus) {
        this.cacheBus = Objects.requireNonNull(cacheBus, "cacheBus");
    }

    @Nonnull
    @Override
    public <K, V> CacheEventListener<K, V> create(@Nonnull Cache<K, V> cache) {
        return new EhCache2CacheEntryEventListener<>(this.cacheBus);
    }
}
