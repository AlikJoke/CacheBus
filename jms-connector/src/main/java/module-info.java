module net.cache.bus.jms.connector {

    requires javax.jms.api;
    requires jsr305;

    requires transitive net.cache.bus.core;
    requires net.cache.bus.transport.addons;

    exports net.cache.bus.jms.configuration;
    exports net.cache.bus.jms.channel;
}