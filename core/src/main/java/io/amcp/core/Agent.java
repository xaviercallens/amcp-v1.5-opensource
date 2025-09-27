package io.amcp.core;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

/**
 * Base interface for all agents in the AMCP system.
 * Agents are autonomous computational entities that can process events,
 * maintain state, and participate in the agent mesh communication.
 * 
 * AMCP v1.5 enhances the Agent interface with:
 * - Simplified convenience methods for common operations
 * - Enhanced lifecycle callbacks with error handling
 * - Built-in retry and resilience patterns  
 * - CloudEvents-aware event publishing
 * - Multi-language compatibility preparations
 * 
 * @since AMCP 1.0
 * @version 1.5.0 - Enhanced with simplified API and v1.5 features
 */
public interface Agent extends Serializable {
    
    /**
     * Gets the unique identifier for this agent.
     * The ID persists across migrations and context changes.
     */
    AgentID getAgentId();
    
    /**
     * Gets the agent type, which determines the agent's behavior and capabilities.
     */
    String getAgentType();
    
    /**
     * Gets the current lifecycle state of this agent.
     */
    AgentLifecycle getLifecycleState();
    
    /**
     * Gets the current agent context hosting this agent.
     */
    AgentContext getAgentContext();
    
    // Lifecycle callbacks - called by the runtime
    
    /**
     * Called when the agent is first created.
     * Override to perform initialization logic.
     * 
     * @param initData optional initialization data
     */
    default void onCreate(Object initData) {
        // Default implementation - subclasses can override
    }
    
    /**
     * Called when the agent is activated (becomes active).
     * This happens after creation or when resuming from inactive state.
     */
    default void onActivate() {
        // Default implementation - subclasses can override
    }
    
    /**
     * Called when the agent is deactivated (becomes inactive).
     * The agent should prepare for potential serialization/storage.
     */
    default void onDeactivate() {
        // Default implementation - subclasses can override
    }
    
    /**
     * Called before the agent is cloned.
     * The clone will have the same state but a different AgentID.
     * 
     * @param cloneId the ID of the new clone
     */
    default void onBeforeClone(AgentID cloneId) {
        // Default implementation - subclasses can override
    }
    
    /**
     * Called after this agent has been cloned.
     * This is called on the original agent, not the clone.
     * 
     * @param clone the newly created clone
     */
    default void onAfterClone(Agent clone) {
        // Default implementation - subclasses can override
    }
    
    /**
     * Called before the agent is dispatched (migrated) to another context.
     * The agent should prepare its state for serialization.
     * 
     * @param destinationContext the target context for migration
     */
    default void onBeforeDispatch(String destinationContext) {
        // Default implementation - subclasses can override
    }
    
    /**
     * Called after the agent has arrived at a new context (post-migration).
     * The agent can perform any necessary reinitialization.
     * 
     * @param previousContext the context the agent came from
     */
    default void onArrival(String previousContext) {
        // Default implementation - subclasses can override
    }
    
    /**
     * Called when the agent is about to be destroyed.
     * Perform any necessary cleanup here.
     */
    default void onDestroy() {
        // Default implementation - subclasses can override
    }
    
    // Event handling
    
    /**
     * Called when this agent receives an event.
     * Override to implement custom event processing logic.
     * 
     * @param event the received event
     * @return CompletableFuture that completes when event processing is done
     */
    CompletableFuture<Void> handleEvent(Event event);
    
    /**
     * Called when this agent receives a control event.
     * Control events are system-level events for managing agent lifecycle.
     * 
     * @param controlEvent the control event
     * @return CompletableFuture that completes when control event processing is done
     */
    default CompletableFuture<Void> handleControlEvent(ControlEvent controlEvent) {
        // Default implementation handles basic control events
        return CompletableFuture.runAsync(() -> {
            switch (controlEvent.getCommand()) {
                case PING:
                    // Respond to ping with pong
                    getAgentContext().publishEvent(
                        Event.create("agent." + getAgentId() + ".pong", "pong", getAgentId())
                    );
                    break;
                case SHUTDOWN:
                    // Graceful shutdown - context will handle the actual destruction
                    onDestroy();
                    break;
                // Other control events handled by default implementations
            }
        });
    }
    
    // State management
    
    /**
     * Gets the current state of the agent for serialization.
     * Override if the agent has complex state that needs custom serialization.
     * 
     * @return the agent's serializable state
     */
    default Object captureState() {
        return this; // By default, the agent itself is its state
    }
    
    /**
     * Restores the agent's state after deserialization.
     * Override if the agent needs custom state restoration logic.
     * 
     * @param state the previously captured state
     */
    default void restoreState(Object state) {
        // Default implementation assumes the agent is its own state
        // Subclasses with complex state should override this
    }
    
    // Utility methods for event publishing (delegated to context)
    
    /**
     * Publishes an event to the specified topic.
     */
    default CompletableFuture<Void> publishEvent(String topic, Object payload) {
        return getAgentContext().publishEvent(Event.create(topic, payload, getAgentId()));
    }
    
    /**
     * Publishes an event with delivery options.
     */
    default CompletableFuture<Void> publishEvent(String topic, Object payload, DeliveryOptions options) {
        Event event = Event.builder()
                .topic(topic)
                .payload(payload)
                .sender(getAgentId())
                .deliveryOptions(options)
                .build();
        return getAgentContext().publishEvent(event);
    }
    
    /**
     * Publishes a pre-built event.
     */
    default CompletableFuture<Void> publishEvent(Event event) {
        return getAgentContext().publishEvent(event);
    }
    
    /**
     * Subscribes to a topic pattern.
     */
    default void subscribe(String topicPattern) {
        getAgentContext().subscribe(getAgentId(), topicPattern);
    }
    
    /**
     * Unsubscribes from a topic pattern.
     */
    default void unsubscribe(String topicPattern) {
        getAgentContext().unsubscribe(getAgentId(), topicPattern);
    }
    
    // AMCP v1.5 Enhanced convenience methods
    
    /**
     * Publishes an event and waits for acknowledgment (if delivery options require it).
     * 
     * @param topic the event topic
     * @param payload the event payload
     * @return CompletableFuture that completes when the event is published and acknowledged
     */
    default CompletableFuture<Void> publishEventAndWait(String topic, Object payload) {
        DeliveryOptions options = new DeliveryOptions.Builder()
                .requireAcknowledgment(true)
                .build();
        return publishEvent(topic, payload, options);
    }
    
    /**
     * Broadcasts an event to all agents (uses wildcard topic).
     * 
     * @param eventType the type of broadcast event
     * @param payload the event payload
     * @return CompletableFuture that completes when the broadcast is sent
     */
    default CompletableFuture<Void> broadcastEvent(String eventType, Object payload) {
        String broadcastTopic = "broadcast." + eventType;
        return publishEvent(broadcastTopic, payload);
    }
    
    /**
     * Sends a direct message to another agent by ID.
     * 
     * @param targetAgentId the recipient agent ID
     * @param messageType the type of message
     * @param payload the message payload
     * @return CompletableFuture that completes when the message is sent
     */
    default CompletableFuture<Void> sendMessage(AgentID targetAgentId, String messageType, Object payload) {
        String directTopic = "agent." + targetAgentId + ".message." + messageType;
        return publishEvent(directTopic, payload);
    }
    
    /**
     * Publishes a CloudEvents-compliant event with content type specification.
     * 
     * @param topic the event topic (CloudEvents 'type')
     * @param payload the event payload (CloudEvents 'data')
     * @param contentType the data content type (e.g., "application/json")
     * @return CompletableFuture that completes when the event is published
     */
    default CompletableFuture<Void> publishCloudEvent(String topic, Object payload, String contentType) {
        Event event = Event.builder()
                .topic(topic)
                .payload(payload)
                .sender(getAgentId())
                .dataContentType(contentType)
                .build();
        return publishEvent(event);
    }
    
    /**
     * Publishes a JSON event (convenience method for JSON content).
     */
    default CompletableFuture<Void> publishJsonEvent(String topic, Object payload) {
        return publishCloudEvent(topic, payload, "application/json");
    }
    
    /**
     * Publishes a text event (convenience method for plain text content).
     */
    default CompletableFuture<Void> publishTextEvent(String topic, String textPayload) {
        return publishCloudEvent(topic, textPayload, "text/plain");
    }
    
    /**
     * Subscribes to events from a specific agent.
     * 
     * @param sourceAgentId the agent to listen to
     * @param eventPattern optional event type pattern (null for all events)
     */
    default void subscribeToAgent(AgentID sourceAgentId, String eventPattern) {
        String topicPattern = eventPattern != null 
            ? "agent." + sourceAgentId + "." + eventPattern
            : "agent." + sourceAgentId + ".*";
        subscribe(topicPattern);
    }
    
    /**
     * Subscribes to broadcast events of a specific type.
     * 
     * @param eventType the broadcast event type to listen for
     */
    default void subscribeToBroadcasts(String eventType) {
        subscribe("broadcast." + eventType);
    }
    
    /**
     * Subscribes to all broadcast events.
     */
    default void subscribeToAllBroadcasts() {
        subscribe("broadcast.*");
    }
    
    // Enhanced lifecycle callbacks with error handling
    
    /**
     * Called when an error occurs during event processing.
     * Override to implement custom error handling logic.
     * 
     * @param event the event that caused the error
     * @param error the exception that occurred
     * @return CompletableFuture that completes when error handling is done
     */
    default CompletableFuture<Void> onError(Event event, Throwable error) {
        return CompletableFuture.runAsync(() -> {
            // Default error handling: log and continue
            System.err.println("[" + getAgentId() + "] Error processing event " + 
                             event.getMessageId() + ": " + error.getMessage());
            error.printStackTrace();
        });
    }
    
    /**
     * Called when the agent successfully processes an event.
     * Override to implement custom success handling (metrics, logging, etc.).
     * 
     * @param event the event that was processed
     */
    default void onEventProcessed(Event event) {
        // Default implementation - subclasses can override for metrics/logging
    }
    
    /**
     * Called when the agent's state changes.
     * Override to implement custom state change reactions.
     * 
     * @param previousState the previous lifecycle state
     * @param newState the new lifecycle state
     */
    default void onStateChange(AgentLifecycle previousState, AgentLifecycle newState) {
        // Default implementation - subclasses can override
        System.out.println("[" + getAgentId() + "] State changed: " + previousState + " -> " + newState);
    }
    
    /**
     * Called periodically (if the agent context supports it) for health checks.
     * Override to implement custom health reporting.
     * 
     * @return true if the agent is healthy, false otherwise
     */
    default boolean isHealthy() {
        return getLifecycleState() == AgentLifecycle.ACTIVE;
    }
    
    // Utility methods for common operations
    
    /**
     * Creates a structured log message with agent context.
     */
    default void log(String level, String message) {
        System.out.println("[" + level.toUpperCase() + "] [" + getAgentId() + "] " + message);
    }
    
    /**
     * Logs an info message.
     */
    default void logInfo(String message) {
        log("INFO", message);
    }
    
    /**
     * Logs a warning message.
     */
    default void logWarn(String message) {
        log("WARN", message);
    }
    
    /**
     * Logs an error message.
     */
    default void logError(String message) {
        log("ERROR", message);
    }
    
    /**
     * Logs an error with exception.
     */
    default void logError(String message, Throwable error) {
        log("ERROR", message + ": " + error.getMessage());
        error.printStackTrace();
    }
}