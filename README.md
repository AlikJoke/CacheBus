# CacheBus

## Cache Clustering Library

Clustering in this case refers to the ability to distribute cache changes across different servers interested in the changes in the cache.

It supports working with caches configured as either replicated (each server contains a full replica of the cache) or invalidation-based (when an item in the cache is modified on one server, the modified value is removed from all servers) cache.

The need for this library may arise when the cache "out of the box" does not support distributed mode of operation (such as ```EhCache```, which requires connecting ```EhCache Terracotta```), or existing mechanisms for ensuring cache distribution are unsatisfactory (e.g., using JGroups for ```Infinispan```, which can cause issues when operating in a network where broadcast/multicast is prohibited and only unicast is allowed, leading to frequent errors and overall system performance degradation due to frequent network interactions with timeouts).

Additionally, the library will be useful in cases where a message broker (such as ```ArtemisMQ```, ```Apache Kafka```, or ```RabbitMQ```) is already used for other purposes since it allows leveraging a familiar and reliable method of distributing data among cache change subscribers.

The library integrates with caching providers such as ```Infinispan```, ```EhCache``` (v2 and v3), and any other caching providers compatible with the ```JSR-107``` (```JCache```) standard: ```Redisson```, ```Caffeine```, ```Hazelcast```, ```Coherence```, etc. (these JSR-107 implementations should be wrapped in the necessary JCache interfaces beforehand). The architecture of the library is designed to facilitate easy integration of new caching providers (following a plugin architecture, where a new caching provider acts as a plugin whose abstractions are wrapped in CacheBus abstractions).

The library has an adapter for working in the Spring Framework context. When configuring the bus within the application where the library is being integrated, the appropriate transport implementation (communication channel with other servers in the logical cluster) is chosen, along with the serialization/deserialization mechanism for messages sent over the channel, and a specific caching provider. If it is necessary to record cache bus metrics information, an implementation of the metrics registry library can also be connected (there is a module adapter for Micrometer Metrics).

Depending on the application's requirements for cache data consistency, additional functionality can be used that is based on checking timestamps of cache element changes. This eliminates the possibility of applying events from the channel multiple times (in case of broker failure, for example), or applying changes that are no longer relevant (for example, a cache element was locally updated after a remote cache had already changed the same element).

If the data change stream in the caches is large, message send/processing buffers can be configured to increase bus throughput.

## Core API
The central component of the library is the abstraction ```ru.joke.cache.bus.core.CacheBus``` and its default implementation ```ru.joke.cache.bus.core.impl.DefaultCacheBus``` (in case of usage Spring Framework you can use another implementation ```ru.joke.cache.bus.spring.SpringCacheBusBean``` from module ```ru.joke.cache-bus:spring-adapter```).
The ```ru.joke.cache.bus.core.ExtendedCacheBus``` is used to manage the lifecycle of the bus, supporting bus management methods.
The bus must be passed a configuration described by ```ru.joke.cache.bus.core.configuration.CacheBusConfiguration``` (the builder pattern using ```ru.joke.cache.bus.core.impl.configuration.ImmutableCacheBusConfiguration#builder()``` can be used to build it).
When configuring the configuration and components of the bus (specified through the configuration), carefully read the Java-docs for the corresponding interfaces and their implementations, which will be selected when forming the configuration according to the requirements of the application.

## Cache Bus Configuration

Cache bus configuration includes the following main components:

1. Transport configuration: includes the following settings, depending on transport type:
   1. Topic/channel name where cache change messages are sent and retrieved for processing by subscribers
   2. Settings of pools used for subscription (usually only a single thread from the pool is used), so a pool of one thread is sufficient 
   3. Settings of timeouts for reconnecting to the channel (broker) in case of disconnection 
   4. Number of connections the channel can use to interact with the broker 
   5. Server host identifier (used to filter messages to avoid receiving messages sent from the current server)
   6. Settings for asynchronous send and asynchronous send buffers (by default, send is synchronous in the cache element change thread)
   7. Settings for multithreaded processing of messages received from other servers and buffers for these messages (by default, processing is synchronous in the channel read thread)
2. Message converter when sending/receiving from the channel to binary format: depending on application needs, either a custom implementation or one of the existing ones can be used. Of the existing ones, the ```One-Nio``` library is recommended, which is highly efficient in terms of memory usage and performance. To avoid extra dependencies, a converter using the native JDK serialization can be used (however, this approach has all the drawbacks inherent to JDK serialization). 
3. Caching provider configuration: as mentioned above, a set of adapters is supported for ```Infinispan```, ```EhCache``` v2 and v3, and for ```JCache``` (and therefore any cache compatible with the ```JSR-107``` specification). Provider configuration usually involves passing a reference to the cache manager of the caching provider. 
4. Cache configuration source for the bus: a list of caches to be clustered by the bus; each cache specifies the type (invalidation or replicated) as well as additional settings such as using timestamps when comparing cache element changes, etc. For configuring the cache configuration source, both Java API and XML settings can be used (the corresponding XSD schema is located in the ```ru.joke.cache-bus:core``` module in the resource folder: ```./configuration/configuration.xsd```). 
5. Metrics registry implementation for recording the most important bus metrics: by default, a No-Op implementation is used that does not record metrics. If needed, a module using the ```Micrometer``` Metrics library can be connected.

Application may contain several buses for different cache providers and message channels.

### Modules
The main module of the library is ```ru.joke.cache-bus:core```. Additional modules can be connected as needed:

1. When using a ```JMS```-compatible message channel: ```ru.joke.cache-bus:jms-connector```.
2. When using an ```Apache Kafka```-based message channel: ```ru.joke.cache-bus:kafka-connector```.
3. When using a ```RabbitMQ```-based message channel: ```ru.joke.cache-bus:rabbit-connector```.
4. When using the ```Infinispan``` caching provider: ```ru.joke.cache-bus:infinispan-integration```.
5. When using the ```EhCache``` v2 caching provider: ```ru.joke.cache-bus:ehcache2-integration```.
6. When using the ```EhCache``` v3 caching provider: ```ru.joke.cache-bus:ehcache3-integration```.
7. When using another caching provider compatible with ```JSR-107``` (```JCache```): ```ru.joke.cache-bus:jcache-integration```.
8. When using the ```One-Nio library``` for serialization/deserialization: ```ru.joke.cache-bus:one-nio-serialization```.
9. When using the standard JDK serialization mechanism: ```ru.joke.cache-bus:jdk-serialization```.
10. When using the ```Jackson JSON``` library for serialization/deserialization: ```ru.joke.cache-bus:jackson-serialization```.
11. When using the ```Micrometer``` library as the metrics registry: ```ru.joke.cache-bus:micrometer-metrics-provider```.

If the application integrating the bus uses Spring Framework, the ```ru.joke.cache-bus:spring-adapter``` module can be used (in which case the ```ru.joke.cache-bus:core``` module does not need to be included directly, the dependency will be transitive).

At a minimum, one of the transport modules, one of the caching provider integration modules, and one of the serialization modules are required, plus either ```ru.joke.cache-bus:core``` or ```ru.joke.cache-bus:spring-adapter```.
Any of the additional modules can be replaced with their own implementation of the message channel, serialization, metrics registry and/or cache provider adapters. Furthermore, if there is a need to load the configuration from sources other than XML or Java API, a custom cache configuration source implementation can be created and connected to the bus.

Examples of bus configuration and integration can be found in the integration tests in the ```ru.joke.cache-bus:integration-tests``` module (in progress).

## Monitoring the CacheBus Status
In addition to the aforementioned metrics, the bus provides a public Java API for retrieving the operational status of the bus and its components (monitoring based on a pull model). The operational status provides both brief information about the most critical issues affecting the operation of the bus, and the overall operational status of the bus.

The main lifecycle stages of the bus and its components are logged, along with information critical for operation (exceptions and warnings/recommendations for changing bus settings). A logging library compatible with ```slf4j-api``` should be used as the logging provider: ```Logback```, ```Log4j```, ```Log4j2``` or ```Jakarta Commons Logging```.