package ru.joke.cache.bus.core.impl.test;

import ru.joke.cache.bus.core.CacheEventMessageConsumer;
import ru.joke.cache.bus.core.configuration.CacheBusMessageChannelConfiguration;
import ru.joke.cache.bus.core.impl.ImmutableComponentState;
import ru.joke.cache.bus.core.state.ComponentState;
import ru.joke.cache.bus.core.transport.CacheBusMessageChannel;
import ru.joke.cache.bus.core.transport.CacheEntryOutputMessage;

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

    @Nonnull
    @Override
    public ComponentState state() {
        return new ImmutableComponentState("fake-channel", unsubscribeCalled ? ComponentState.Status.DOWN : ComponentState.Status.UP_OK);
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
