package io.amcp.messaging;

import io.amcp.core.Event;
import io.amcp.mobility.BrokerMetrics;
import java.util.concurrent.CompletableFuture;

/**
 * Event broker interface for pluggable messaging systems in AMCP v1.4.
 * 
 * <p>The EventBroker provides an abstraction layer over different messaging
 * technologies (Kafka, NATS, Solace, in-memory) while maintaining consistent
 * event routing and delivery semantics.</p>
 * 
 * <p>Implementations must support:
 * <ul>
 *   <li>Hierarchical topic routing (travel.*, travel.**)</li>
 *   <li>CloudEvents specification compliance</li>
 *   <li>Multiple delivery guarantee levels</li>
 *   <li>Correlation ID propagation for distributed tracing</li>
 *   <li>Subscription management and filtering</li>
 * </ul>
 * </p>
 * 
 * @author AMCP Development Team
 * @version 1.4.0
 * @since 1.0.0
 */
public interface EventBroker {

    /**
     * Publishes an event to the messaging system.
     * 
     * <p>The event is delivered to all subscribers whose topic patterns match
     * the event's topic. Delivery semantics depend on the event's DeliveryOptions.</p>
     * 
     * @param event the event to publish
     * @return CompletableFuture that completes when publishing is finished
     */
    CompletableFuture<Void> publish(Event event);

    /**
     * Subscribes to events matching the given topic pattern.
     * 
     * <p>The subscriber will receive all future events whose topics match
     * the specified pattern. The subscriber's handleEvent method will be
     * called for each matching event.</p>
     * 
     * @param subscriber the event subscriber
     * @param topicPattern the topic pattern to subscribe to
     * @return CompletableFuture that completes when subscription is established
     */
    CompletableFuture<Void> subscribe(EventSubscriber subscriber, String topicPattern);

    /**
     * Unsubscribes from events matching the given topic pattern.
     * 
     * @param subscriber the event subscriber to unsubscribe
     * @param topicPattern the topic pattern to unsubscribe from
     * @return CompletableFuture that completes when unsubscription is finished
     */
    CompletableFuture<Void> unsubscribe(EventSubscriber subscriber, String topicPattern);

    /**
     * Starts the event broker.
     * 
     * <p>Initializes connections to underlying messaging infrastructure
     * and begins processing events.</p>
     * 
     * @return CompletableFuture that completes when broker is ready
     */
    CompletableFuture<Void> start();

    /**
     * Stops the event broker.
     * 
     * <p>Cleanly shuts down connections and ensures all pending events
     * are processed or properly handled.</p>
     * 
     * @return CompletableFuture that completes when shutdown is finished
     */
    CompletableFuture<Void> stop();

    /**
     * Gets the current status of the broker.
     * 
     * @return true if the broker is running and ready to process events
     */
    boolean isRunning();

    /**
     * Gets broker metrics and statistics.
     * 
     * @return broker metrics including message rates, error counts, etc.
     */
    BrokerMetrics getMetrics();

    /**
     * Interface for event subscribers.
     */
    interface EventSubscriber {
        /**
         * Handles a received event.
         * 
         * @param event the received event
         * @return CompletableFuture that completes when event processing is finished
         */
        CompletableFuture<Void> handleEvent(Event event);

        /**
         * Gets the subscriber identifier.
         * 
         * @return unique subscriber identifier
         */
        String getSubscriberId();
    }

}