package com.amcp.messaging.impl;

import io.amcp.core.Event;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * In-memory implementation of EventBroker for testing and development.
 * Not suitable for production use across multiple processes.
 * 
 * @since AMCP 1.0
 */
public class InMemoryEventBroker extends AbstractEventBroker {
    
    private final ConcurrentMap<String, List<SubscriptionHandler>> topicSubscriptions = new ConcurrentHashMap<>();
    
    private static class SubscriptionHandler {
        @SuppressWarnings("unused") // Used for debugging and logging
        final String subscriberId;
        @SuppressWarnings("unused") // Used for debugging and logging  
        final String topicPattern;
        final Pattern compiledPattern;
        final Consumer<Event> handler;
        final java.util.function.BiConsumer<Event, Acknowledgment> ackHandler;
        volatile boolean active = true;
        
        SubscriptionHandler(String subscriberId, String topicPattern, Consumer<Event> handler) {
            this.subscriberId = subscriberId;
            this.topicPattern = topicPattern;
            this.compiledPattern = compileTopicPattern(topicPattern);
            this.handler = handler;
            this.ackHandler = null;
        }
        
        SubscriptionHandler(String subscriberId, String topicPattern, 
                          java.util.function.BiConsumer<Event, Acknowledgment> ackHandler) {
            this.subscriberId = subscriberId;
            this.topicPattern = topicPattern;
            this.compiledPattern = compileTopicPattern(topicPattern);
            this.handler = null;
            this.ackHandler = ackHandler;
        }
        
        boolean matches(String topic) {
            return compiledPattern.matcher(topic).matches();
        }
        
        void handleEvent(Event event) {
            if (!active) return;
            
            try {
                if (handler != null) {
                    handler.accept(event);
                } else if (ackHandler != null) {
                    ackHandler.accept(event, new InMemoryAcknowledgment());
                }
            } catch (Exception e) {
                System.err.println("Error handling event in subscription: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        private static Pattern compileTopicPattern(String pattern) {
            // Convert topic pattern to regex
            // * matches any characters except dots
            // ** matches any characters including dots
            // . is literal dot
            String regex = pattern
                .replace(".", "\\.")  // Escape dots first
                .replace("**", "DOUBLE_STAR_PLACEHOLDER")  // Temporary placeholder
                .replace("*", "[^.]*")  // Single star
                .replace("DOUBLE_STAR_PLACEHOLDER", ".*");  // Double star
            
            return Pattern.compile("^" + regex + "$");
        }
    }
    
    private static class InMemoryAcknowledgment implements Acknowledgment {
        @SuppressWarnings("unused") // Reserved for future tracking
        private volatile boolean acknowledged = false;
        
        @Override
        public void acknowledge() {
            acknowledged = true;
        }
        
        @Override
        public void reject() {
            reject(false);
        }
        
        @Override
        public void reject(boolean requeue) {
            acknowledged = false;
            // In-memory implementation doesn't support requeue
            if (requeue) {
                System.out.println("Requeue requested but not supported in in-memory broker");
            }
        }
    }
    
    @Override
    protected CompletableFuture<Void> doConnect(BrokerConfig config) {
        // In-memory broker is always "connected"
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    protected CompletableFuture<Void> doDisconnect() {
        // Clear all subscriptions
        topicSubscriptions.clear();
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    protected boolean isActuallyConnected() {
        return true; // In-memory broker is always connected
    }
    
    @Override
    protected Publisher doCreatePublisher(String publisherId) {
        return new InMemoryPublisher(publisherId);
    }
    
    @Override
    protected Subscriber doCreateSubscriber(String subscriberId) {
        return new InMemorySubscriber(subscriberId);
    }
    
    @Override
    public BrokerInfo getBrokerInfo() {
        return new InMemoryBrokerInfo();
    }
    
    private class InMemoryPublisher extends AbstractPublisher {
        
        InMemoryPublisher(String publisherId) {
            super(publisherId);
        }
        
        @Override
        protected CompletableFuture<Void> doPublish(Event event) {
            return deliverEvent(event);
        }
        
        @Override
        protected CompletableFuture<Void> doPublishWithAck(Event event) {
            // In-memory implementation doesn't distinguish between ack and non-ack
            return deliverEvent(event);
        }
        
        @Override
        protected CompletableFuture<Void> doClose() {
            return CompletableFuture.completedFuture(null);
        }
        
        private CompletableFuture<Void> deliverEvent(Event event) {
            String topic = event.getTopic();
            
            // Find all matching subscriptions
            List<SubscriptionHandler> matchingHandlers = new ArrayList<>();
            
            for (List<SubscriptionHandler> handlers : topicSubscriptions.values()) {
                for (SubscriptionHandler handler : handlers) {
                    if (handler.active && handler.matches(topic)) {
                        matchingHandlers.add(handler);
                    }
                }
            }
            
            // Deliver to all matching subscribers asynchronously
            return CompletableFuture.runAsync(() -> {
                for (SubscriptionHandler handler : matchingHandlers) {
                    handler.handleEvent(event);
                }
            });
        }
    }
    
    private class InMemorySubscriber extends AbstractSubscriber {
        
        InMemorySubscriber(String subscriberId) {
            super(subscriberId);
        }
        
        @Override
        public CompletableFuture<Subscription> subscribe(String topicPattern, Consumer<Event> handler) {
            if (closed.get()) {
                CompletableFuture<Subscription> future = new CompletableFuture<>();
                future.completeExceptionally(new IllegalStateException("Subscriber is closed"));
                return future;
            }
            
            SubscriptionHandler subscriptionHandler = new SubscriptionHandler(subscriberId, topicPattern, handler);
            
            topicSubscriptions.computeIfAbsent(topicPattern, k -> new CopyOnWriteArrayList<>())
                             .add(subscriptionHandler);
            
            InMemorySubscription subscription = new InMemorySubscription(topicPattern, subscriberId, subscriptionHandler);
            addSubscription(subscription);
            
            return CompletableFuture.completedFuture(subscription);
        }
        
        @Override
        public CompletableFuture<Subscription> subscribe(String topicPattern,
                                                       java.util.function.BiConsumer<Event, Acknowledgment> handler) {
            if (closed.get()) {
                CompletableFuture<Subscription> future = new CompletableFuture<>();
                future.completeExceptionally(new IllegalStateException("Subscriber is closed"));
                return future;
            }
            
            SubscriptionHandler subscriptionHandler = new SubscriptionHandler(subscriberId, topicPattern, handler);
            
            topicSubscriptions.computeIfAbsent(topicPattern, k -> new CopyOnWriteArrayList<>())
                             .add(subscriptionHandler);
            
            InMemorySubscription subscription = new InMemorySubscription(topicPattern, subscriberId, subscriptionHandler);
            addSubscription(subscription);
            
            return CompletableFuture.completedFuture(subscription);
        }
        
        @Override
        protected CompletableFuture<Void> doClose() {
            return CompletableFuture.completedFuture(null);
        }
    }
    
    private class InMemorySubscription extends AbstractSubscription {
        private final SubscriptionHandler handler;
        
        InMemorySubscription(String topicPattern, String subscriberId, SubscriptionHandler handler) {
            super(topicPattern, subscriberId);
            this.handler = handler;
        }
        
        @Override
        protected boolean isActuallyActive() {
            return handler.active;
        }
        
        @Override
        protected CompletableFuture<Void> doClose() {
            handler.active = false;
            
            // Remove from topic subscriptions
            List<SubscriptionHandler> handlers = topicSubscriptions.get(topicPattern);
            if (handlers != null) {
                handlers.remove(handler);
                if (handlers.isEmpty()) {
                    topicSubscriptions.remove(topicPattern);
                }
            }
            
            return CompletableFuture.completedFuture(null);
        }
    }
    
    private static class InMemoryBrokerInfo implements BrokerInfo {
        @Override
        public String getBrokerType() {
            return "in-memory";
        }
        
        @Override
        public String getVersion() {
            return "1.0.0";
        }
        
        @Override
        public boolean supportsWildcards() {
            return true;
        }
        
        @Override
        public boolean supportsAcknowledgments() {
            return true; // Basic support
        }
        
        @Override
        public boolean supportsPersistentDelivery() {
            return false; // In-memory only
        }
        
        @Override
        public Set<String> getCapabilities() {
            return new HashSet<>(Arrays.asList(
                "wildcards",
                "acknowledgments",
                "synchronous-delivery"
            ));
        }
    }
}