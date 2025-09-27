package io.amcp.messaging.impl;

import io.amcp.core.Event;
import io.amcp.messaging.EventBroker;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

/**
 * Simple in-memory implementation of EventBroker for AMCP v1.5.
 * Provides basic pub/sub functionality with topic pattern matching.
 * 
 * @since AMCP 1.5.0
 */
public class InMemoryEventBroker implements EventBroker {
    
    private final ConcurrentMap<String, List<Consumer<Event>>> subscribers;
    private final ConcurrentMap<String, Pattern> topicPatterns;
    private volatile boolean connected = false;
    
    public InMemoryEventBroker() {
        this.subscribers = new ConcurrentHashMap<>();
        this.topicPatterns = new ConcurrentHashMap<>();
        this.connected = true; // In-memory broker is always "connected"
    }
    
    @Override
    public CompletableFuture<Void> connect(BrokerConfig config) {
        return CompletableFuture.runAsync(() -> {
            this.connected = true;
        });
    }
    
    @Override
    public CompletableFuture<Void> disconnect() {
        return CompletableFuture.runAsync(() -> {
            this.connected = false;
            subscribers.clear();
            topicPatterns.clear();
        });
    }
    
    @Override
    public boolean isConnected() {
        return connected;
    }
    
    @Override
    public Publisher createPublisher(String publisherId) {
        return new InMemoryPublisher(publisherId);
    }
    
    @Override
    public Subscriber createSubscriber(String subscriberId) {
        return new InMemorySubscriber(subscriberId);
    }
    
    @Override
    public BrokerInfo getBrokerInfo() {
        return new InMemoryBrokerInfo();
    }
    
    // Internal publisher implementation
    private class InMemoryPublisher implements Publisher {
        private final String publisherId;
        private volatile boolean closed = false;
        
        public InMemoryPublisher(String publisherId) {
            this.publisherId = publisherId;
        }
        
        @Override
        public CompletableFuture<Void> publish(Event event) {
            if (closed) {
                CompletableFuture<Void> future = new CompletableFuture<>();
                future.completeExceptionally(new IllegalStateException("Publisher is closed"));
                return future;
            }
            
            return CompletableFuture.runAsync(() -> {
                String topic = event.getTopic();
                
                // Find all matching subscribers
                List<CompletableFuture<Void>> deliveryFutures = new CopyOnWriteArrayList<>();
                
                for (String patternStr : subscribers.keySet()) {
                    if (topicMatches(topic, patternStr)) {
                        List<Consumer<Event>> handlers = subscribers.get(patternStr);
                        if (handlers != null) {
                            for (Consumer<Event> handler : handlers) {
                                try {
                                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> handler.accept(event));
                                    deliveryFutures.add(future);
                                } catch (Exception e) {
                                    System.err.println("Error delivering event to subscriber: " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            });
        }
        
        @Override
        public CompletableFuture<Void> publishWithAck(Event event) {
            // For in-memory broker, this is the same as regular publish
            return publish(event);
        }
        
        @Override
        public CompletableFuture<Void> close() {
            return CompletableFuture.runAsync(() -> {
                closed = true;
            });
        }
    }
    
    // Internal subscriber implementation
    private class InMemorySubscriber implements Subscriber {
        private final String subscriberId;
        private volatile boolean closed = false;
        
        public InMemorySubscriber(String subscriberId) {
            this.subscriberId = subscriberId;
        }
        
        @Override
        public CompletableFuture<Subscription> subscribe(String topicPattern, Consumer<Event> handler) {
            if (closed) {
                CompletableFuture<Subscription> future = new CompletableFuture<>();
                future.completeExceptionally(new IllegalStateException("Subscriber is closed"));
                return future;
            }
            
            return CompletableFuture.supplyAsync(() -> {
                subscribers.computeIfAbsent(topicPattern, k -> new CopyOnWriteArrayList<>()).add(handler);
                topicPatterns.put(topicPattern, compileTopicPattern(topicPattern));
                return new InMemorySubscription(topicPattern, subscriberId);
            });
        }
        
        @Override
        public CompletableFuture<Subscription> subscribe(String topicPattern, 
                                                       BiConsumer<Event, Acknowledgment> handler) {
            // For in-memory broker, we don't need acknowledgments, so convert to simple handler
            Consumer<Event> simpleHandler = event -> handler.accept(event, new InMemoryAcknowledgment());
            return subscribe(topicPattern, simpleHandler);
        }
        
        @Override
        public CompletableFuture<Void> close() {
            return CompletableFuture.runAsync(() -> {
                closed = true;
            });
        }
    }
    
    // Internal subscription implementation
    private class InMemorySubscription implements Subscription {
        private final String topicPattern;
        private final String subscriberId;
        private volatile boolean active = true;
        
        public InMemorySubscription(String topicPattern, String subscriberId) {
            this.topicPattern = topicPattern;
            this.subscriberId = subscriberId;
        }
        
        @Override
        public String getTopicPattern() {
            return topicPattern;
        }
        
        @Override
        public String getSubscriberId() {
            return subscriberId;
        }
        
        @Override
        public boolean isActive() {
            return active;
        }
        
        @Override
        public CompletableFuture<Void> close() {
            return CompletableFuture.runAsync(() -> {
                active = false;
                subscribers.remove(topicPattern);
                topicPatterns.remove(topicPattern);
            });
        }
    }
    
    // Internal acknowledgment implementation
    private class InMemoryAcknowledgment implements Acknowledgment {
        @Override
        public void acknowledge() {
            // No-op for in-memory broker
        }
        
        @Override
        public void reject() {
            // No-op for in-memory broker
        }
        
        @Override
        public void reject(boolean requeue) {
            // No-op for in-memory broker
        }
    }
    
    // Internal broker info implementation
    private class InMemoryBrokerInfo implements BrokerInfo {
        @Override
        public String getBrokerType() {
            return "in-memory";
        }
        
        @Override
        public String getVersion() {
            return "1.5.0";
        }
        
        @Override
        public boolean supportsWildcards() {
            return true;
        }
        
        @Override
        public boolean supportsAcknowledgments() {
            return false; // Simple in-memory broker doesn't need acks
        }
        
        @Override
        public boolean supportsPersistentDelivery() {
            return false; // In-memory is not persistent
        }
        
        @Override
        public Set<String> getCapabilities() {
            Set<String> capabilities = new HashSet<>();
            capabilities.add("wildcard-subscriptions");
            capabilities.add("topic-patterns");
            return capabilities;
        }
    }
    
    private boolean topicMatches(String topic, String pattern) {
        // Simple pattern matching
        if (pattern.equals("*") || pattern.equals("**")) {
            return true;
        }
        
        if (pattern.contains("*")) {
            Pattern compiledPattern = topicPatterns.get(pattern);
            if (compiledPattern != null) {
                return compiledPattern.matcher(topic).matches();
            }
            return false;
        }
        
        return topic.equals(pattern);
    }
    
    private Pattern compileTopicPattern(String topicPattern) {
        // Convert AMCP topic patterns to regex
        // * matches a single segment
        // ** matches multiple segments
        String regex = topicPattern
                .replace(".", "\\.")  // Escape dots
                .replace("**", ".*")  // ** becomes .*
                .replace("*", "[^.]*"); // * becomes [^.]*
        
        return Pattern.compile("^" + regex + "$");
    }
}