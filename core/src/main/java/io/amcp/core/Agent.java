package io.amcp.core;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

/**
 * Core agent interface defining the fundamental behavior and lifecycle operations
 * for all agents within the AMCP v1.4 framework.
 * 
 * <p>This interface provides the basic contract for agent implementations, including
 * event handling, lifecycle management, and identification. All agents must implement
 * this interface to participate in the AMCP mesh communication protocol.</p>
 * 
 * <p>Key design principles:
 * <ul>
 *   <li>Asynchronous operations - All methods return CompletableFuture for non-blocking execution</li>
 *   <li>Event-driven architecture - Agents react to and publish events through the messaging system</li>
 *   <li>Serializable agents - Support for mobility requires agent state to be transferable</li>
 *   <li>Lifecycle awareness - Agents are managed through well-defined lifecycle states</li>
 * </ul>
 * </p>
 * 
 * @author AMCP Development Team
 * @version 1.4.0
 * @since 1.0.0
 */
public interface Agent extends Serializable {

    /**
     * Unique identifier for this agent instance.
     * 
     * @return the agent's unique identifier
     */
    AgentID getAgentId();

    /**
     * Current execution context for this agent.
     * Provides access to messaging, lifecycle management, and platform services.
     * 
     * @return the agent's current execution context
     */
    AgentContext getContext();

    /**
     * Current lifecycle state of this agent.
     * 
     * @return the current agent lifecycle state
     */
    AgentLifecycle getLifecycleState();

    /**
     * Handles an incoming event asynchronously.
     * 
     * <p>This is the primary entry point for event-driven agent behavior. Agents
     * receive events through this method and should process them based on the
     * event topic, payload, and metadata.</p>
     * 
     * <p>Example implementation:
     * <pre>{@code
     * @Override
     * public CompletableFuture<Void> handleEvent(Event event) {
     *     return CompletableFuture.runAsync(() -> {
     *         switch (event.getTopic()) {
     *             case "travel.request.plan":
     *                 handleTravelRequest(event);
     *                 break;
     *             case "weather.forecast.update":
     *                 handleWeatherUpdate(event);
     *                 break;
     *             default:
     *                 log.debug("Unhandled event topic: {}", event.getTopic());
     *         }
     *     });
     * }
     * }</pre>
     * 
     * @param event the event to process
     * @return CompletableFuture that completes when event processing is finished
     */
    CompletableFuture<Void> handleEvent(Event event);

    /**
     * Publishes an event to the messaging system.
     * 
     * <p>Convenience method for publishing simple events with topic and payload.
     * For more control over event properties, use {@link #publishEvent(Event)}.</p>
     * 
     * @param topic the event topic
     * @param payload the event payload
     * @return CompletableFuture that completes when the event is published
     */
    default CompletableFuture<Void> publishEvent(String topic, Object payload) {
        Event event = Event.builder()
            .topic(topic)
            .payload(payload)
            .sender(getAgentId())
            .build();
        return publishEvent(event);
    }

    /**
     * Publishes an event to the messaging system.
     * 
     * @param event the complete event to publish
     * @return CompletableFuture that completes when the event is published
     */
    CompletableFuture<Void> publishEvent(Event event);

    /**
     * Subscribes to events matching the given topic pattern.
     * 
     * <p>Topic patterns support hierarchical matching:
     * <ul>
     *   <li>"travel.*" - matches "travel.request", "travel.response", etc.</li>
     *   <li>"travel.**" - matches "travel.request.new", "travel.response.success.details", etc.</li>
     *   <li>"travel.request.plan" - exact match only</li>
     * </ul>
     * </p>
     * 
     * @param topicPattern the topic pattern to subscribe to
     * @return CompletableFuture that completes when subscription is established
     */
    CompletableFuture<Void> subscribe(String topicPattern);

    /**
     * Unsubscribes from events matching the given topic pattern.
     * 
     * @param topicPattern the topic pattern to unsubscribe from
     * @return CompletableFuture that completes when unsubscription is finished
     */
    CompletableFuture<Void> unsubscribe(String topicPattern);

    /**
     * Lifecycle callback invoked when the agent is being activated.
     * 
     * <p>This method is called by the AgentContext when the agent transitions
     * from INACTIVE to ACTIVE state. Agents should use this method to:
     * <ul>
     *   <li>Initialize resources</li>
     *   <li>Subscribe to relevant event topics</li>
     *   <li>Register with external services</li>
     *   <li>Start background tasks</li>
     * </ul>
     * </p>
     */
    void onActivate();

    /**
     * Lifecycle callback invoked when the agent is being deactivated.
     * 
     * <p>This method is called by the AgentContext when the agent transitions
     * from ACTIVE to INACTIVE state. Agents should use this method to:
     * <ul>
     *   <li>Clean up resources</li>
     *   <li>Unsubscribe from event topics</li>
     *   <li>Stop background tasks</li>
     *   <li>Save persistent state</li>
     * </ul>
     * </p>
     */
    void onDeactivate();

    /**
     * Lifecycle callback invoked when the agent is being destroyed.
     * 
     * <p>This method is called by the AgentContext when the agent is being
     * permanently removed from the system. This is the final cleanup opportunity.</p>
     */
    void onDestroy();

    /**
     * Lifecycle callback invoked before the agent is migrated to another context.
     * 
     * <p>Agents can use this method to:
     * <ul>
     *   <li>Prepare state for serialization</li>
     *   <li>Clean up context-specific resources</li>
     *   <li>Notify other agents of pending migration</li>
     * </ul>
     * </p>
     * 
     * @param destinationContext the target context for migration
     */
    void onBeforeMigration(String destinationContext);

    /**
     * Lifecycle callback invoked after the agent has been migrated from another context.
     * 
     * <p>Agents can use this method to:
     * <ul>
     *   <li>Restore context-specific resources</li>
     *   <li>Re-establish connections</li>
     *   <li>Resume normal operation</li>
     * </ul>
     * </p>
     * 
     * @param sourceContext the original context before migration
     */
    void onAfterMigration(String sourceContext);

}