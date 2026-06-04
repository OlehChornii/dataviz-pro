package com.dataviz.common.event;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class EventBus {

    private static final Logger LOG = Logger.getLogger(EventBus.class.getName());

    private static final class Holder {
        static final EventBus INSTANCE = new EventBus();
    }
    public static EventBus getInstance() { return Holder.INSTANCE; }
    private EventBus() {}

    private final Map<Class<?>, List<Consumer<Object>>> subscribers =
            new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> void subscribe(Class<T> eventType, Consumer<T> handler) {
        subscribers
                .computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add((Consumer<Object>) handler);
        LOG.fine(() -> "Subscribed to " + eventType.getSimpleName());
    }

    @SuppressWarnings("unchecked")
    public <T> void unsubscribe(Class<T> eventType, Consumer<T> handler) {
        List<Consumer<Object>> list = subscribers.get(eventType);
        if (list != null) {
            list.remove((Consumer<Object>) handler);
        }
    }

    public <T> void publish(T event) {
        Objects.requireNonNull(event);
        List<Consumer<Object>> list = subscribers.get(event.getClass());
        if (list != null) {
            LOG.fine(() -> "Publishing " + event.getClass().getSimpleName());
            for (Consumer<Object> h : list) {
                try { h.accept(event); }
                catch (Exception ex) {
                    LOG.severe(() -> "EventBus handler error: " + ex.getMessage());
                }
            }
        }
    }
}