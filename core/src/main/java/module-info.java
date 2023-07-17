module net.cache.bus.core {

    requires jsr305;
    requires transitive org.slf4j;
    requires transitive java.xml;

    exports net.cache.bus.core;
    exports net.cache.bus.core.configuration;
    exports net.cache.bus.core.transport;
    exports net.cache.bus.core.impl;
    exports net.cache.bus.core.impl.resolvers;
    exports net.cache.bus.core.impl.configuration;
}