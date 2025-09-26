package io.amcp.messaging.impl;

import io.amcp.core.Event;
import io.amcp.messaging.EventBroker;
import io.amcp.mobility.BrokerMetrics;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory EventBroker implementation for testing and development.
 * 
 * <p>This implementation provides a lightweight, in-process event broker
 * suitable for development, testing, and single-node deployments. For
 * production distributed deployments, use KafkaEventBroker or similar.</p>
 * 
 * <p>Features:
 * <ul>
 *   <li>Topic pattern matching with hierarchical routing</li>
 *   <li>Asynchronous event delivery</li>
 *   <li>Thread-safe subscription management</li>
 *   <li>Basic metrics collection</li>
 * </ul>
 * </p>
 * 
 * @author AMCP Development Team
 * @version 1.4.0
 * @since 1.0.0
 */
public class InMemoryEventBroker implements EventBroker {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Map<String, Set<EventSubscriber>> subscriptions = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "InMemoryEventBroker-" + Thread.currentThread().getName());
        t.setDaemon(true);
        return t;
    });

    // Metrics
    private final AtomicLong publishedEvents = new AtomicLong(0);
    private final AtomicLong deliveredEvents = new AtomicLong(0);
    private final AtomicLong failedDeliveries = new AtomicLong(0);

    @Override
    public CompletableFuture<Void> publish(Event event) {
        if (!running.get()) {
            CompletableFuture<Void> result = new CompletableFuture<>();
            result.completeExceptionally(new IllegalStateException("EventBroker is not running"));
            return result;
        }

        if (event == null || event.getTopic() == null) {
            CompletableFuture<Void> result = new CompletableFuture<>();
            result.completeExceptionally(new IllegalArgumentException("Event and topic cannot be null"));
            return result;
        }

        return CompletableFuture.runAsync(() -> {
            try {
                publishedEvents.incrementAndGet();
                
                logMessage("Publishing event to topic: " + event.getTopic() + " with ID: " + event.getId());

                // Find all matching subscribers
                Set<EventSubscriber> matchingSubscribers = findMatchingSubscribers(event.getTopic());
                
                if (matchingSubscribers.isEmpty()) {
                    logMessage("No subscribers found for topic: " + event.getTopic());
                    return;
                }

                logMessage("Delivering event " + event.getId() + " to " + matchingSubscribers.size() + " subscribers");

                // Deliver to all matching subscribers
                for (EventSubscriber subscriber : matchingSubscribers) {
                    deliverToSubscriber(subscriber, event);
                }

            } catch (Exception e) {
                logMessage("Error publishing event: " + event.getId() + " - " + e.getMessage());
                throw new RuntimeException("Failed to publish event", e);
            }
        }, executorService);
    }

    @Override
    public CompletableFuture<Void> subscribe(EventSubscriber subscriber, String topicPattern) {
        if (!running.get()) {
            CompletableFuture<Void> result = new CompletableFuture<>();
            result.completeExceptionally(new IllegalStateException("EventBroker is not running"));
            return result;
        }

        if (subscriber == null || topicPattern == null) {
            CompletableFuture<Void> result = new CompletableFuture<>();
            result.completeExceptionally(new IllegalArgumentException("Subscriber and topic pattern cannot be null"));
            return result;
        }

        return CompletableFuture.runAsync(() -> {
            subscriptions.computeIfAbsent(topicPattern, k -> new CopyOnWriteArraySet<>()).add(subscriber);
            logMessage("Subscriber " + subscriber.getSubscriberId() + " subscribed to topic pattern: " + topicPattern);
        }, executorService);
    }

    @Override
    public CompletableFuture<Void> unsubscribe(EventSubscriber subscriber, String topicPattern) {
        if (subscriber == null || topicPattern == null) {
            CompletableFuture<Void> result = new CompletableFuture<>();
            result.completeExceptionally(new IllegalArgumentException("Subscriber and topic pattern cannot be null"));
            return result;
        }

        return CompletableFuture.runAsync(() -> {
            Set<EventSubscriber> subscribers = subscriptions.get(topicPattern);
            if (subscribers != null) {
                subscribers.remove(subscriber);
                if (subscribers.isEmpty()) {
                    subscriptions.remove(topicPattern);
                }
                logMessage("Subscriber " + subscriber.getSubscriberId() + 
                    " unsubscribed from topic pattern: " + topicPattern);
            }
        }, executorService);
    }

    @Override
    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(() -> {
            if (running.compareAndSet(false, true)) {
                logMessage("Starting InMemoryEventBroker");
                // Initialize any required resources
            } else {
                logMessage("EventBroker is already running");
            }
        });
    }

    @Override
    public CompletableFuture<Void> stop() {
        return CompletableFuture.runAsync(() -> {
            if (running.compareAndSet(true, false)) {
                logMessage("Stopping InMemoryEventBroker");
                
                // Clear subscriptions
                subscriptions.clear();
                
                // Shutdown executor service
                executorService.shutdown();
                try {
                    if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        executorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executorService.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                
                logMessage("InMemoryEventBroker stopped");
            } else {
                logMessage("EventBroker is already stopped");
            }
        });
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public BrokerMetrics getMetrics() {
        return new BrokerMetrics() {
            @Override
            public long getTotalEventsPublished() {
                return publishedEvents.get();
            }

            @Override
            public long getTotalEventsDelivered() {
                return deliveredEvents.get();
            }

            @Override
            public int getActiveSubscriptions() {
                return subscriptions.size();
            }

            @Override
            public int getConnectedAgents() {
                return getTotalSubscriberCount();
            }

            @Override
            public double getAverageProcessingTime() {
                return 0.0; // Simple implementation
            }

            @Override
            public long getFailedDeliveries() {
                return failedDeliveries.get();
            }
        };
    }

    /**
     * Finds all subscribers whose topic patterns match the given topic.
     * 
     * @param topic the event topic
     * @return set of matching subscribers
     */
    private Set<EventSubscriber> findMatchingSubscribers(String topic) {
        Set<EventSubscriber> matchingSubscribers = new CopyOnWriteArraySet<>();
        
        for (Map.Entry<String, Set<EventSubscriber>> entry : subscriptions.entrySet()) {
            String pattern = entry.getKey();
            if (topicMatches(topic, pattern)) {
                matchingSubscribers.addAll(entry.getValue());
            }
        }
        
        return matchingSubscribers;
    }

    /**
     * Checks if a topic matches a pattern.
     * 
     * <p>Supports hierarchical pattern matching:
     * <ul>
     *   <li>"travel.*" matches "travel.request" but not "travel.request.new"</li>
     *   <li>"travel.**" matches "travel.request.new" and all nested topics</li>
     *   <li>"travel.request" matches exactly "travel.request"</li>
     * </ul>
     * </p>
     * 
     * @param topic the topic to test
     * @param pattern the pattern to match against
     * @return true if the topic matches the pattern
     */
    private boolean topicMatches(String topic, String pattern) {
        if (pattern.equals(topic)) {
            return true;
        }

        if (pattern.endsWith(".**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return topic.startsWith(prefix + ".");
        }

        if (pattern.endsWith(".*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return topic.startsWith(prefix + ".") && 
                   topic.indexOf('.', prefix.length() + 1) == -1;
        }

        return false;
    }

    /**
     * Delivers an event to a specific subscriber.
     * 
     * @param subscriber the target subscriber
     * @param event the event to deliver
     */
    private void deliverToSubscriber(EventSubscriber subscriber, Event event) {
        CompletableFuture.runAsync(() -> {
            try {
                subscriber.handleEvent(event).get();
                deliveredEvents.incrementAndGet();
                logMessage("Successfully delivered event " + event.getId() + 
                    " to subscriber " + subscriber.getSubscriberId());
            } catch (Exception e) {
                failedDeliveries.incrementAndGet();
                logMessage("Failed to deliver event " + event.getId() + 
                    " to subscriber " + subscriber.getSubscriberId() + ": " + e.getMessage());
            }
        }, executorService);
    }

    /**
     * Gets the number of published events.
     * 
     * @return published event count
     */
    public long getPublishedEventCount() {
        return publishedEvents.get();
    }

    /**
     * Gets the number of delivered events.
     * 
     * @return delivered event count
     */
    public long getDeliveredEventCount() {
        return deliveredEvents.get();
    }

    /**
     * Gets the number of failed deliveries.
     * 
     * @return failed delivery count
     */
    public long getFailedDeliveryCount() {
        return failedDeliveries.get();
    }

    /**
     * Gets the current number of subscription patterns.
     * 
     * @return subscription count
     */
    public int getSubscriptionCount() {
        return subscriptions.size();
    }

    /**
     * Gets the total number of subscribers across all patterns.
     * 
     * @return total subscriber count
     */
    public int getTotalSubscriberCount() {
        return subscriptions.values().stream()
            .mapToInt(Set::size)
            .sum();
    }

    /**
     * Shutdown the event broker (for backward compatibility).
     */
    public void shutdown() {
        stop().join();
    }

    /**
     * Simple logging method to avoid SLF4J dependency.
     */
    private void logMessage(String message) {
        System.out.println("[InMemoryEventBroker] " + message);
    }

}