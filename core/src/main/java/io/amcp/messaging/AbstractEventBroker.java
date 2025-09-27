package com.amcp.messaging.impl;

import io.amcp.core.Event;
import io.amcp.messaging.EventBroker;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Abstract base implementation for EventBroker implementations.
 * Provides common functionality and lifecycle management.
 * 
 * @since AMCP 1.0
 */
public abstract class AbstractEventBroker implements EventBroker {
    protected final Logger logger = Logger.getLogger(getClass().getName());
    
    protected final AtomicBoolean connected = new AtomicBoolean(false);
    protected final ConcurrentMap<String, Publisher> publishers = new ConcurrentHashMap<>();
    protected final ConcurrentMap<String, Subscriber> subscribers = new ConcurrentHashMap<>();
    
    protected BrokerConfig currentConfig;
    
    @Override
    public final CompletableFuture<Void> connect(BrokerConfig config) {
        if (config == null) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("BrokerConfig cannot be null"));
            return future;
        }
        
        if (connected.get()) {
            logger.warning("Broker is already connected");
            return CompletableFuture.completedFuture(null);
        }
        
        this.currentConfig = config;
        
        return doConnect(config)
                .thenRun(() -> {
                    connected.set(true);
                    logger.info("Connected to broker: " + config.getBrokerUrl());
                })
                .exceptionally(throwable -> {
                    logger.severe("Failed to connect to broker: " + throwable.getMessage());
                    connected.set(false);
                    throw new RuntimeException("Connection failed", throwable);
                });
    }
    
    @Override
    public final CompletableFuture<Void> disconnect() {
        if (!connected.get()) {
            logger.warning("Broker is not connected");
            return CompletableFuture.completedFuture(null);
        }
        
        return doDisconnect()
                .thenRun(() -> {
                    connected.set(false);
                    // Close all publishers and subscribers
                    publishers.values().forEach(pub -> pub.close());
                    subscribers.values().forEach(sub -> sub.close());
                    publishers.clear();
                    subscribers.clear();
                    logger.info("Disconnected from broker");
                })
                .exceptionally(throwable -> {
                    logger.severe("Error during disconnect: " + throwable.getMessage());
                    // Still mark as disconnected even if cleanup failed
                    connected.set(false);
                    return null;
                });
    }
    
    @Override
    public final boolean isConnected() {
        return connected.get() && isActuallyConnected();
    }
    
    @Override
    public final Publisher createPublisher(String publisherId) {
        if (!isConnected()) {
            throw new IllegalStateException("Broker is not connected");
        }
        
        return publishers.computeIfAbsent(publisherId, id -> {
            logger.info("Creating publisher: " + id);
            return doCreatePublisher(id);
        });
    }
    
    @Override
    public final Subscriber createSubscriber(String subscriberId) {
        if (!isConnected()) {
            throw new IllegalStateException("Broker is not connected");
        }
        
        return subscribers.computeIfAbsent(subscriberId, id -> {
            logger.info("Creating subscriber: " + id);
            return doCreateSubscriber(id);
        });
    }
    
    // Abstract methods that subclasses must implement
    
    /**
     * Performs the actual connection to the broker.
     */
    protected abstract CompletableFuture<Void> doConnect(BrokerConfig config);
    
    /**
     * Performs the actual disconnection from the broker.
     */
    protected abstract CompletableFuture<Void> doDisconnect();
    
    /**
     * Checks if the connection is actually active (broker-specific).
     */
    protected abstract boolean isActuallyConnected();
    
    /**
     * Creates a broker-specific publisher implementation.
     */
    protected abstract Publisher doCreatePublisher(String publisherId);
    
    /**
     * Creates a broker-specific subscriber implementation.
     */
    protected abstract Subscriber doCreateSubscriber(String subscriberId);
    
    // Helper classes for common functionality
    
    /**
     * Base publisher implementation with common functionality.
     */
    protected abstract static class AbstractPublisher implements Publisher {
        protected final Logger logger = Logger.getLogger(getClass().getName());
        protected final String publisherId;
        protected final AtomicBoolean closed = new AtomicBoolean(false);
        
        protected AbstractPublisher(String publisherId) {
            this.publisherId = publisherId;
        }
        
        @Override
        public final CompletableFuture<Void> publish(Event event) {
            if (closed.get()) {
                CompletableFuture<Void> future = new CompletableFuture<>();
                future.completeExceptionally(new IllegalStateException("Publisher is closed"));
                return future;
            }
            
            if (event == null) {
                CompletableFuture<Void> future = new CompletableFuture<>();
                future.completeExceptionally(new IllegalArgumentException("Event cannot be null"));
                return future;
            }
            
            return doPublish(event);
        }
        
        @Override
        public final CompletableFuture<Void> publishWithAck(Event event) {
            if (closed.get()) {
                CompletableFuture<Void> future = new CompletableFuture<>();
                future.completeExceptionally(new IllegalStateException("Publisher is closed"));
                return future;
            }
            
            if (event == null) {
                CompletableFuture<Void> future = new CompletableFuture<>();
                future.completeExceptionally(new IllegalArgumentException("Event cannot be null"));
                return future;
            }
            
            return doPublishWithAck(event);
        }
        
        @Override
        public final CompletableFuture<Void> close() {
            if (closed.compareAndSet(false, true)) {
                logger.info("Closing publisher: " + publisherId);
                return doClose();
            }
            return CompletableFuture.completedFuture(null);
        }
        
        protected abstract CompletableFuture<Void> doPublish(Event event);
        protected abstract CompletableFuture<Void> doPublishWithAck(Event event);
        protected abstract CompletableFuture<Void> doClose();
    }
    
    /**
     * Base subscriber implementation with common functionality.
     */
    protected abstract static class AbstractSubscriber implements Subscriber {
        protected final Logger logger = Logger.getLogger(getClass().getName());
        protected final String subscriberId;
        protected final AtomicBoolean closed = new AtomicBoolean(false);
        protected final ConcurrentMap<String, Subscription> subscriptions = new ConcurrentHashMap<>();
        
        protected AbstractSubscriber(String subscriberId) {
            this.subscriberId = subscriberId;
        }
        
        @Override
        public final CompletableFuture<Void> close() {
            if (closed.compareAndSet(false, true)) {
                logger.info("Closing subscriber: " + subscriberId);
                
                // Close all subscriptions first
                java.util.List<CompletableFuture<Void>> closeFutures = new java.util.ArrayList<>();
                for (Subscription subscription : subscriptions.values()) {
                    closeFutures.add(subscription.close());
                }
                
                return CompletableFuture.allOf(closeFutures.toArray(new CompletableFuture[0]))
                        .thenCompose(v -> doClose());
            }
            return CompletableFuture.completedFuture(null);
        }
        
        protected void addSubscription(Subscription subscription) {
            subscriptions.put(subscription.getTopicPattern(), subscription);
        }
        
        protected void removeSubscription(String topicPattern) {
            subscriptions.remove(topicPattern);
        }
        
        protected abstract CompletableFuture<Void> doClose();
    }
    
    /**
     * Base subscription implementation with common functionality.
     */
    protected abstract static class AbstractSubscription implements Subscription {
        protected final Logger logger = Logger.getLogger(getClass().getName());
        protected final String topicPattern;
        protected final String subscriberId;
        protected final AtomicBoolean active = new AtomicBoolean(true);
        
        protected AbstractSubscription(String topicPattern, String subscriberId) {
            this.topicPattern = topicPattern;
            this.subscriberId = subscriberId;
        }
        
        @Override
        public final String getTopicPattern() {
            return topicPattern;
        }
        
        @Override
        public final String getSubscriberId() {
            return subscriberId;
        }
        
        @Override
        public final boolean isActive() {
            return active.get() && isActuallyActive();
        }
        
        @Override
        public final CompletableFuture<Void> close() {
            if (active.compareAndSet(true, false)) {
                logger.info("Closing subscription: " + topicPattern + " for subscriber: " + subscriberId);
                return doClose();
            }
            return CompletableFuture.completedFuture(null);
        }
        
        protected abstract boolean isActuallyActive();
        protected abstract CompletableFuture<Void> doClose();
    }
}