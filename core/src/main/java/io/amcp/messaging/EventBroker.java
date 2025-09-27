package io.amcp.messaging;

import io.amcp.core.Event;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Abstract interface for event broker implementations in AMCP.
 * Provides a unified API for different messaging systems (Solace, Kafka, NATS).
 * 
 * @since AMCP 1.0
 */
public interface EventBroker {
    
    /**
     * Configuration information for the broker connection.
     */
    interface BrokerConfig {
        String getBrokerUrl();
        String getUsername();
        String getPassword();
        String getClientId();
        java.util.Map<String, Object> getProperties();
    }
    
    /**
     * Acknowledgment interface for message delivery confirmation.
     */
    interface Acknowledgment {
        /**
         * Acknowledges successful processing of the message.
         */
        void acknowledge();
        
        /**
         * Negatively acknowledges the message (redelivery may occur).
         */
        void reject();
        
        /**
         * Negatively acknowledges with requeue option.
         */
        void reject(boolean requeue);
    }
    
    /**
     * Subscription handle for managing topic subscriptions.
     */
    interface Subscription {
        /**
         * Gets the topic pattern for this subscription.
         */
        String getTopicPattern();
        
        /**
         * Gets the subscriber ID.
         */
        String getSubscriberId();
        
        /**
         * Checks if the subscription is active.
         */
        boolean isActive();
        
        /**
         * Unsubscribes and closes this subscription.
         */
        CompletableFuture<Void> close();
    }
    
    /**
     * Publisher interface for sending events.
     */
    interface Publisher {
        /**
         * Publishes an event to the specified topic.
         * 
         * @param event the event to publish
         * @return CompletableFuture that completes when the event is sent
         */
        CompletableFuture<Void> publish(Event event);
        
        /**
         * Publishes an event and waits for broker acknowledgment.
         * 
         * @param event the event to publish
         * @return CompletableFuture that completes when the event is acknowledged
         */
        CompletableFuture<Void> publishWithAck(Event event);
        
        /**
         * Closes the publisher and releases resources.
         */
        CompletableFuture<Void> close();
    }
    
    /**
     * Subscriber interface for receiving events.
     */
    interface Subscriber {
        /**
         * Subscribes to a topic pattern with a message handler.
         * 
         * @param topicPattern the topic pattern to subscribe to
         * @param handler the event handler
         * @return Subscription handle for managing the subscription
         */
        CompletableFuture<Subscription> subscribe(String topicPattern, 
                                                Consumer<Event> handler);
        
        /**
         * Subscribes to a topic pattern with a message handler and acknowledgment support.
         * 
         * @param topicPattern the topic pattern to subscribe to
         * @param handler the event handler with acknowledgment
         * @return Subscription handle for managing the subscription
         */
        CompletableFuture<Subscription> subscribe(String topicPattern,
                                                java.util.function.BiConsumer<Event, Acknowledgment> handler);
        
        /**
         * Closes the subscriber and all its subscriptions.
         */
        CompletableFuture<Void> close();
    }
    
    // Main EventBroker interface methods
    
    /**
     * Connects to the event broker using the provided configuration.
     * 
     * @param config broker connection configuration
     * @return CompletableFuture that completes when connected
     */
    CompletableFuture<Void> connect(BrokerConfig config);
    
    /**
     * Disconnects from the event broker.
     * 
     * @return CompletableFuture that completes when disconnected
     */
    CompletableFuture<Void> disconnect();
    
    /**
     * Checks if the broker connection is active.
     * 
     * @return true if connected, false otherwise
     */
    boolean isConnected();
    
    /**
     * Creates a publisher for sending events.
     * 
     * @param publisherId unique identifier for the publisher
     * @return Publisher instance
     */
    Publisher createPublisher(String publisherId);
    
    /**
     * Creates a subscriber for receiving events.
     * 
     * @param subscriberId unique identifier for the subscriber
     * @return Subscriber instance
     */
    Subscriber createSubscriber(String subscriberId);
    
    /**
     * Gets broker-specific information and capabilities.
     * 
     * @return metadata about the broker implementation
     */
    BrokerInfo getBrokerInfo();
    
    /**
     * Information about the broker implementation and capabilities.
     */
    interface BrokerInfo {
        /**
         * Gets the broker type (e.g., "solace", "kafka", "nats").
         */
        String getBrokerType();
        
        /**
         * Gets the broker version.
         */
        String getVersion();
        
        /**
         * Checks if the broker supports wildcard subscriptions.
         */
        boolean supportsWildcards();
        
        /**
         * Checks if the broker supports message acknowledgments.
         */
        boolean supportsAcknowledgments();
        
        /**
         * Checks if the broker supports persistent delivery.
         */
        boolean supportsPersistentDelivery();
        
        /**
         * Gets additional broker-specific capabilities.
         */
        java.util.Set<String> getCapabilities();
    }
}