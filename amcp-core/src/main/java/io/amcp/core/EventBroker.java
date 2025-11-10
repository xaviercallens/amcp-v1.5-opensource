package io.amcp.core;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Interface for event brokers that handle message routing between agents.
 * Implementations can use various message brokers (Kafka, NATS, in-memory, etc.).
 */
public interface EventBroker extends AutoCloseable {

    /**
     * Publishes an event to the broker.
     *
     * @param event the event to publish
     * @return a CompletableFuture that completes when the event is published
     */
    CompletableFuture<Void> publish(Event event);

    /**
     * Subscribes to events matching the specified topic pattern.
     *
     * @param topicPattern the topic pattern (supports wildcards like "hello.*")
     * @param handler      the event handler
     * @return a subscription ID that can be used to unsubscribe
     */
    String subscribe(String topicPattern, Consumer<Event> handler);

    /**
     * Unsubscribes from a previously created subscription.
     *
     * @param subscriptionId the subscription ID
     */
    void unsubscribe(String subscriptionId);

    /**
     * Starts the broker (connects to backend if needed).
     *
     * @return a CompletableFuture that completes when the broker is started
     */
    CompletableFuture<Void> start();

    /**
     * Stops the broker and releases resources.
     *
     * @return a CompletableFuture that completes when the broker is stopped
     */
    CompletableFuture<Void> stop();

    /**
     * Checks if the broker is running.
     *
     * @return true if the broker is running
     */
    boolean isRunning();

    /**
     * Gets the broker type identifier.
     *
     * @return the broker type (e.g., "memory", "kafka", "nats")
     */
    String getBrokerType();

    @Override
    default void close() throws Exception {
        stop().get();
    }
}
