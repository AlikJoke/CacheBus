module net.cache.bus.ehcache2_integration {

    requires jsr305;
    requires ehcache;
    requires transitive net.cache.bus.core;

    exports net.cache.bus.ehcache2.configuration;
    exports net.cache.bus.ehcache2.listeners to net.cache.bus.core;
    exports net.cache.bus.ehcache2.adapters to net.cache.bus.core;
}