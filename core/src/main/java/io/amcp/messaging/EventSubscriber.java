package io.amcp.messaging;

import io.amcp.core.Event;
import java.util.function.Consumer;

/**
 * Interface for event subscription and handling in the AMCP messaging system.
 * 
 * EventSubscriber provides the contract for components that wish to receive
 * and process events from the event broker.
 */
public interface EventSubscriber {
    
    /**
     * Subscribe to events on a specific topic.
     * 
     * @param topic The topic pattern to subscribe to (supports wildcards)
     * @param handler The event handler function
     * @return A subscription identifier for unsubscribing
     */
    String subscribe(String topic, Consumer<Event> handler);
    
    /**
     * Subscribe to events on a specific topic with a filter.
     * 
     * @param topic The topic pattern to subscribe to
     * @param filter The filter function to apply to events
     * @param handler The event handler function
     * @return A subscription identifier for unsubscribing
     */
    String subscribe(String topic, java.util.function.Predicate<Event> filter, Consumer<Event> handler);
    
    /**
     * Unsubscribe from a topic using the subscription identifier.
     * 
     * @param subscriptionId The subscription identifier returned by subscribe()
     * @return true if successfully unsubscribed, false otherwise
     */
    boolean unsubscribe(String subscriptionId);
    
    /**
     * Unsubscribe from all subscriptions for this subscriber.
     * 
     * @return The number of subscriptions that were removed
     */
    int unsubscribeAll();
    
    /**
     * Get the subscriber identifier.
     * 
     * @return The unique identifier for this subscriber
     */
    String getSubscriberId();
    
    /**
     * Check if this subscriber is active and receiving events.
     * 
     * @return true if active, false otherwise
     */
    boolean isActive();
    
    /**
     * Get the number of active subscriptions for this subscriber.
     * 
     * @return The count of active subscriptions
     */
    int getSubscriptionCount();
}