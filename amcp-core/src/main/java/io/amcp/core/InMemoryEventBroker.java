package io.amcp.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * In-memory implementation of EventBroker for development and testing.
 * Events are routed directly within the JVM without external message broker.
 */
public class InMemoryEventBroker implements EventBroker {
    private static final Logger logger = LoggerFactory.getLogger(InMemoryEventBroker.class);
    
    private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private volatile boolean running = false;

    private static class Subscription {
        final String topicPattern;
        final Consumer<Event> handler;

        Subscription(String topicPattern, Consumer<Event> handler) {
            this.topicPattern = topicPattern;
            this.handler = handler;
        }
    }

    @Override
    public CompletableFuture<Void> publish(Event event) {
        if (!running) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Broker not running"));
        }

        return CompletableFuture.runAsync(() -> {
            logger.debug("Publishing event: {}", event.getTopic());
            
            // Find matching subscriptions
            for (Subscription sub : subscriptions.values()) {
                if (event.matchesTopic(sub.topicPattern)) {
                    try {
                        executor.submit(() -> sub.handler.accept(event));
                    } catch (Exception e) {
                        logger.error("Error delivering event to subscriber: {}", 
                                e.getMessage(), e);
                    }
                }
            }
        }, executor);
    }

    @Override
    public String subscribe(String topicPattern, Consumer<Event> handler) {
        String subscriptionId = UUID.randomUUID().toString();
        subscriptions.put(subscriptionId, new Subscription(topicPattern, handler));
        logger.debug("Created subscription {} for pattern {}", subscriptionId, topicPattern);
        return subscriptionId;
    }

    @Override
    public void unsubscribe(String subscriptionId) {
        subscriptions.remove(subscriptionId);
        logger.debug("Removed subscription {}", subscriptionId);
    }

    @Override
    public CompletableFuture<Void> start() {
        if (running) {
            return CompletableFuture.completedFuture(null);
        }
        
        running = true;
        logger.info("InMemoryEventBroker started");
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> stop() {
        if (!running) {
            return CompletableFuture.completedFuture(null);
        }
        
        running = false;
        subscriptions.clear();
        executor.shutdown();
        logger.info("InMemoryEventBroker stopped");
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public String getBrokerType() {
        return "memory";
    }
}
