package net.cache.bus.core.impl.test;

import net.cache.bus.core.CacheEventMessageConsumer;
import net.cache.bus.core.configuration.CacheBusMessageChannelConfiguration;
import net.cache.bus.core.transport.CacheBusMessageChannel;
import net.cache.bus.core.transport.CacheEntryOutputMessage;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class FakeCacheBusMessageChannel implements CacheBusMessageChannel<CacheBusMessageChannelConfiguration> {

    private CacheBusMessageChannelConfiguration configuration;
    private final List<CacheEntryOutputMessage> messages = new ArrayList<>();
    private CacheEventMessageConsumer consumer;
    private boolean unsubscribeCalled;

    @Override
    public void activate(@Nonnull CacheBusMessageChannelConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void send(@Nonnull CacheEntryOutputMessage eventOutputMessage) {
        this.messages.add(eventOutputMessage);
    }

    @Override
    public void subscribe(@Nonnull CacheEventMessageConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public void close() {
        this.unsubscribeCalled = true;
    }

    public CacheBusMessageChannelConfiguration getConfiguration() {
        return configuration;
    }

    public List<CacheEntryOutputMessage> getMessages() {
        return messages;
    }

    public CacheEventMessageConsumer getConsumer() {
        return consumer;
    }

    public boolean isUnsubscribeCalled() {
        return unsubscribeCalled;
    }
}
