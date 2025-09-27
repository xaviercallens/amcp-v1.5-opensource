# AMCP v1.5 Core API Reference

## Package: io.amcp.core

### Agent Interface

The foundation interface that all AMCP agents must implement.

```java
package io.amcp.core;

public interface Agent extends Serializable {
    /**
     * Handles incoming events asynchronously.
     * This is the primary method for agent communication and business logic.
     * 
     * @param event The incoming event to process
     * @return CompletableFuture that completes when event processing is done
     */
    CompletableFuture<Void> handleEvent(Event event);
    
    /**
     * Sets the agent's runtime context.
     * Called during agent initialization.
     * 
     * @param context The agent context providing runtime services
     */
    void setAgentContext(AgentContext context);
    
    /**
     * Gets the agent's runtime context.
     * 
     * @return The current agent context
     */
    AgentContext getAgentContext();
    
    /**
     * Gets the unique identifier for this agent.
     * 
     * @return The agent's unique ID
     */
    AgentID getAgentId();
    
    /**
     * Called when the agent transitions to ACTIVE state.
     * Override to perform initialization, subscribe to topics, etc.
     */
    default void onActivate() {}
    
    /**
     * Called when the agent transitions from ACTIVE state.
     * Override to perform cleanup before deactivation.
     */
    default void onDeactivate() {}
    
    /**
     * Called during agent shutdown for resource cleanup.
     * Override to close connections, shutdown executors, etc.
     */
    default void cleanup() {}
}
```

### Event Class

Represents messages exchanged between agents.

```java
package io.amcp.core;

public class Event implements Serializable {
    /**
     * Creates a new event builder.
     * 
     * @return A new Event.Builder instance
     */
    public static Builder builder() { return new Builder(); }
    
    /**
     * Gets the event topic (routing key).
     * 
     * @return The topic string
     */
    public String getTopic() { /* ... */ }
    
    /**
     * Gets the event payload.
     * 
     * @return The payload object
     */
    public Object getPayload() { /* ... */ }
    
    /**
     * Gets the sender agent ID.
     * 
     * @return The sender's AgentID
     */
    public AgentID getSender() { /* ... */ }
    
    /**
     * Gets event metadata value.
     * 
     * @param key The metadata key
     * @return The metadata value, or null if not found
     */
    public String getMetadata(String key) { /* ... */ }
    
    /**
     * Gets all event metadata.
     * 
     * @return Map of all metadata key-value pairs
     */
    public Map<String, String> getAllMetadata() { /* ... */ }
    
    /**
     * Gets the event timestamp.
     * 
     * @return When the event was created
     */
    public Instant getTimestamp() { /* ... */ }
    
    /**
     * Gets the delivery options for this event.
     * 
     * @return The delivery options
     */
    public DeliveryOptions getDeliveryOptions() { /* ... */ }
    
    /**
     * Builder class for creating Event instances.
     */
    public static class Builder {
        /**
         * Sets the event topic.
         * 
         * @param topic The topic string
         * @return This builder
         */
        public Builder topic(String topic) { /* ... */ }
        
        /**
         * Sets the event payload.
         * 
         * @param payload The payload object
         * @return This builder
         */
        public Builder payload(Object payload) { /* ... */ }
        
        /**
         * Sets the sender agent ID.
         * 
         * @param sender The sender's AgentID
         * @return This builder
         */
        public Builder sender(AgentID sender) { /* ... */ }
        
        /**
         * Adds metadata key-value pair.
         * 
         * @param key The metadata key
         * @param value The metadata value
         * @return This builder
         */
        public Builder metadata(String key, String value) { /* ... */ }
        
        /**
         * Sets delivery options.
         * 
         * @param options The delivery options
         * @return This builder
         */
        public Builder deliveryOptions(DeliveryOptions options) { /* ... */ }
        
        /**
         * Builds the Event instance.
         * 
         * @return The constructed Event
         */
        public Event build() { /* ... */ }
    }
}
```

### AgentContext Interface

Provides runtime services to agents.

```java
package io.amcp.core;

public interface AgentContext {
    /**
     * Publishes an event to the messaging system.
     * 
     * @param event The event to publish
     * @return CompletableFuture that completes when event is published
     */
    CompletableFuture<Void> publishEvent(Event event);
    
    /**
     * Publishes an event with simple topic and payload.
     * 
     * @param topic The topic to publish to
     * @param payload The event payload
     * @return CompletableFuture that completes when event is published
     */
    default CompletableFuture<Void> publishEvent(String topic, Object payload) {
        Event event = Event.builder()
            .topic(topic)
            .payload(payload)
            .sender(getOwnerAgent().getAgentId())
            .build();
        return publishEvent(event);
    }
    
    /**
     * Subscribes to events matching the topic pattern.
     * Supports wildcards: * (single level), ** (multiple levels)
     * 
     * @param topicPattern The topic pattern to subscribe to
     */
    void subscribe(String topicPattern);
    
    /**
     * Unsubscribes from events matching the topic pattern.
     * 
     * @param topicPattern The topic pattern to unsubscribe from
     */
    void unsubscribe(String topicPattern);
    
    /**
     * Creates a new agent instance.
     * 
     * @param agentClass The agent class to instantiate
     * @return The new agent's ID
     * @throws AgentCreationException If agent creation fails
     */
    AgentID createAgent(Class<? extends Agent> agentClass) throws AgentCreationException;
    
    /**
     * Activates an agent.
     * 
     * @param agentId The agent to activate
     * @return CompletableFuture that completes when activation is done
     */
    CompletableFuture<Void> activateAgent(AgentID agentId);
    
    /**
     * Deactivates an agent.
     * 
     * @param agentId The agent to deactivate
     * @return CompletableFuture that completes when deactivation is done
     */
    CompletableFuture<Void> deactivateAgent(AgentID agentId);
    
    /**
     * Destroys an agent, removing it from the system.
     * 
     * @param agentId The agent to destroy
     * @return CompletableFuture that completes when destruction is done
     */
    CompletableFuture<Void> destroyAgent(AgentID agentId);
    
    /**
     * Migrates an agent to another node/context.
     * 
     * @param agentId The agent to migrate
     * @param targetNode The target node identifier
     * @return CompletableFuture that completes when migration is done
     */
    CompletableFuture<Void> migrateAgent(AgentID agentId, String targetNode);
    
    /**
     * Gets the shared scheduled executor service.
     * 
     * @return ScheduledExecutorService for periodic tasks
     */
    ScheduledExecutorService getScheduler();
    
    /**
     * Gets the event broker instance.
     * 
     * @return The underlying EventBroker
     */
    EventBroker getEventBroker();
    
    /**
     * Gets the agent that owns this context.
     * 
     * @return The owner agent
     */
    Agent getOwnerAgent();
    
    /**
     * Gets system-wide configuration properties.
     * 
     * @param key The configuration key
     * @return The configuration value, or null if not found
     */
    String getProperty(String key);
    
    /**
     * Gets system-wide configuration properties with default value.
     * 
     * @param key The configuration key
     * @param defaultValue The default value if key not found
     * @return The configuration value or default
     */
    String getProperty(String key, String defaultValue);
}
```

### AgentID Class

Unique identifier for agents in the system.

```java
package io.amcp.core;

public class AgentID implements Serializable, Comparable<AgentID> {
    /**
     * Creates a new random AgentID.
     * 
     * @return A new unique AgentID
     */
    public static AgentID create() { /* ... */ }
    
    /**
     * Creates an AgentID from a string representation.
     * 
     * @param id The string representation
     * @return The corresponding AgentID
     */
    public static AgentID fromString(String id) { /* ... */ }
    
    /**
     * Gets the string representation of this AgentID.
     * 
     * @return The ID as a string
     */
    public String getId() { /* ... */ }
    
    /**
     * Gets the node where this agent is located.
     * 
     * @return The node identifier
     */
    public String getNode() { /* ... */ }
    
    /**
     * Gets the timestamp when this AgentID was created.
     * 
     * @return The creation timestamp
     */
    public Instant getCreatedAt() { /* ... */ }
    
    @Override
    public String toString() { /* ... */ }
    
    @Override
    public boolean equals(Object obj) { /* ... */ }
    
    @Override
    public int hashCode() { /* ... */ }
    
    @Override
    public int compareTo(AgentID other) { /* ... */ }
}
```

### AgentLifecycle Enum

Represents the current state of an agent.

```java
package io.amcp.core;

public enum AgentLifecycle {
    /**
     * Agent is created but not yet active.
     */
    INACTIVE,
    
    /**
     * Agent is active and processing events.
     */
    ACTIVE,
    
    /**
     * Agent is being migrated to another node.
     */
    MIGRATING,
    
    /**
     * Agent is suspended and not processing events.
     */
    SUSPENDED,
    
    /**
     * Agent has been destroyed and removed from the system.
     */
    DESTROYED;
    
    /**
     * Checks if the agent is in an active processing state.
     * 
     * @return true if agent can process events
     */
    public boolean isActive() {
        return this == ACTIVE;
    }
    
    /**
     * Checks if the agent is in a transitional state.
     * 
     * @return true if agent is changing states
     */
    public boolean isTransitioning() {
        return this == MIGRATING;
    }
}
```

### DeliveryOptions Class

Configuration for event delivery guarantees.

```java
package io.amcp.core;

public class DeliveryOptions implements Serializable {
    /**
     * Best-effort delivery (default).
     * Fast but no guarantee of delivery.
     */
    public static final DeliveryOptions BEST_EFFORT = /* ... */;
    
    /**
     * Reliable delivery with acknowledgment.
     * Slower but guaranteed delivery.
     */
    public static final DeliveryOptions RELIABLE = /* ... */;
    
    /**
     * Ordered delivery preserving message sequence.
     * Messages delivered in the order they were sent.
     */
    public static final DeliveryOptions ORDERED = /* ... */;
    
    /**
     * Creates a new delivery options builder.
     * 
     * @return A new DeliveryOptions.Builder
     */
    public static Builder builder() { /* ... */ }
    
    /**
     * Gets the delivery mode.
     * 
     * @return The delivery mode
     */
    public DeliveryMode getMode() { /* ... */ }
    
    /**
     * Gets the retry count for failed deliveries.
     * 
     * @return The maximum retry attempts
     */
    public int getRetryCount() { /* ... */ }
    
    /**
     * Gets the timeout for delivery acknowledgment.
     * 
     * @return The timeout duration
     */
    public Duration getTimeout() { /* ... */ }
    
    /**
     * Delivery mode enumeration.
     */
    public enum DeliveryMode {
        BEST_EFFORT,
        RELIABLE,
        ORDERED
    }
    
    /**
     * Builder for DeliveryOptions.
     */
    public static class Builder {
        public Builder mode(DeliveryMode mode) { /* ... */ }
        public Builder retryCount(int count) { /* ... */ }
        public Builder timeout(Duration timeout) { /* ... */ }
        public DeliveryOptions build() { /* ... */ }
    }
}
```

## Package: io.amcp.messaging

### EventBroker Interface

Abstract interface for message broker implementations.

```java
package io.amcp.messaging;

public interface EventBroker {
    /**
     * Starts the event broker.
     * 
     * @return CompletableFuture that completes when broker is started
     */
    CompletableFuture<Void> start();
    
    /**
     * Stops the event broker.
     * 
     * @return CompletableFuture that completes when broker is stopped
     */
    CompletableFuture<Void> stop();
    
    /**
     * Publishes an event to the broker.
     * 
     * @param event The event to publish
     * @return CompletableFuture that completes when event is published
     */
    CompletableFuture<Void> publish(Event event);
    
    /**
     * Subscribes to events matching the topic pattern.
     * 
     * @param topicPattern The topic pattern
     * @param handler The event handler
     */
    void subscribe(String topicPattern, EventHandler handler);
    
    /**
     * Unsubscribes from events matching the topic pattern.
     * 
     * @param topicPattern The topic pattern
     * @param handler The event handler to remove
     */
    void unsubscribe(String topicPattern, EventHandler handler);
    
    /**
     * Checks if the broker is currently running.
     * 
     * @return true if broker is active
     */
    boolean isRunning();
    
    /**
     * Gets broker health information.
     * 
     * @return Health status and metrics
     */
    BrokerHealth getHealth();
    
    /**
     * Gets broker configuration.
     * 
     * @return Current configuration
     */
    BrokerConfig getConfig();
}
```

### EventHandler Interface

Callback interface for event processing.

```java
package io.amcp.messaging;

@FunctionalInterface
public interface EventHandler {
    /**
     * Handles an incoming event.
     * 
     * @param event The event to handle
     * @return CompletableFuture that completes when handling is done
     */
    CompletableFuture<Void> handle(Event event);
}
```

## Usage Examples

### Basic Agent Implementation

```java
import io.amcp.core.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

public class WeatherAgent implements Agent {
    private static final Logger logger = LoggerFactory.getLogger(WeatherAgent.class);
    
    private AgentID agentId;
    private AgentContext context;
    private AgentLifecycle state = AgentLifecycle.INACTIVE;
    private final AtomicLong eventCount = new AtomicLong(0);
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        eventCount.incrementAndGet();
        
        return CompletableFuture.runAsync(() -> {
            switch (event.getTopic()) {
                case "weather.data.request":
                    handleWeatherRequest(event);
                    break;
                case "weather.alert.severe":
                    handleSevereWeatherAlert(event);
                    break;
                default:
                    logger.debug("Unhandled event: {}", event.getTopic());
            }
        });
    }
    
    @Override
    public void onActivate() {
        state = AgentLifecycle.ACTIVE;
        context.subscribe("weather.*");
        context.subscribe("system.shutdown");
        
        logger.info("WeatherAgent activated with ID: {}", agentId);
    }
    
    @Override
    public void onDeactivate() {
        state = AgentLifecycle.INACTIVE;
        context.unsubscribe("weather.*");
        context.unsubscribe("system.shutdown");
        
        logger.info("WeatherAgent deactivated. Processed {} events.", eventCount.get());
    }
    
    private void handleWeatherRequest(Event event) {
        String location = (String) event.getPayload();
        
        // Simulate weather data retrieval
        WeatherData data = getWeatherData(location);
        
        // Publish response
        Event response = Event.builder()
            .topic("weather.data.response")
            .payload(data)
            .sender(agentId)
            .metadata("correlationId", event.getMetadata("correlationId"))
            .build();
        
        context.publishEvent(response);
    }
    
    private WeatherData getWeatherData(String location) {
        // Implementation details...
        return new WeatherData(location, 22.5, "Sunny");
    }
    
    // ... other methods
}
```

### Event Publishing and Subscription

```java
// Simple event publishing
context.publishEvent("user.login", userInfo);

// Complex event with metadata
Event complexEvent = Event.builder()
    .topic("order.payment.processed")
    .payload(paymentData)
    .sender(getAgentId())
    .deliveryOptions(DeliveryOptions.RELIABLE)
    .metadata("userId", user.getId())
    .metadata("orderId", order.getId())
    .metadata("correlationId", UUID.randomUUID().toString())
    .build();

context.publishEvent(complexEvent);

// Topic subscription patterns
context.subscribe("user.*");          // All user events
context.subscribe("order.**");        // All order events (any depth)
context.subscribe("payment.success"); // Specific event only
```

### Request-Response Pattern

```java
public class DataRequestAgent implements Agent {
    private final Map<String, CompletableFuture<Object>> pendingRequests = 
        new ConcurrentHashMap<>();
    
    public CompletableFuture<Object> requestData(String query) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<Object> responseFuture = new CompletableFuture<>();
        
        // Store the future for response correlation
        pendingRequests.put(correlationId, responseFuture);
        
        // Send request
        Event request = Event.builder()
            .topic("data.request")
            .payload(query)
            .sender(getAgentId())
            .metadata("correlationId", correlationId)
            .metadata("replyTo", "data.response." + getAgentId().getId())
            .build();
        
        context.publishEvent(request);
        
        // Set up timeout
        context.getScheduler().schedule(() -> {
            CompletableFuture<Object> future = pendingRequests.remove(correlationId);
            if (future != null && !future.isDone()) {
                future.completeExceptionally(new TimeoutException("Request timeout"));
            }
        }, 30, TimeUnit.SECONDS);
        
        return responseFuture;
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            if (event.getTopic().startsWith("data.response.")) {
                handleDataResponse(event);
            }
        });
    }
    
    private void handleDataResponse(Event event) {
        String correlationId = event.getMetadata("correlationId");
        if (correlationId != null) {
            CompletableFuture<Object> future = pendingRequests.remove(correlationId);
            if (future != null) {
                future.complete(event.getPayload());
            }
        }
    }
    
    @Override
    public void onActivate() {
        context.subscribe("data.response." + getAgentId().getId());
    }
}
```

## Error Handling

### Exception Types

```java
package io.amcp.core;

/**
 * Base exception for AMCP framework errors.
 */
public class AMCPException extends Exception {
    public AMCPException(String message) { super(message); }
    public AMCPException(String message, Throwable cause) { super(message, cause); }
}

/**
 * Thrown when agent creation fails.
 */
public class AgentCreationException extends AMCPException {
    public AgentCreationException(String message) { super(message); }
    public AgentCreationException(String message, Throwable cause) { super(message, cause); }
}

/**
 * Thrown when event delivery fails.
 */
public class EventDeliveryException extends AMCPException {
    public EventDeliveryException(String message) { super(message); }
    public EventDeliveryException(String message, Throwable cause) { super(message, cause); }
}
```

### Error Handling Best Practices

```java
@Override
public CompletableFuture<Void> handleEvent(Event event) {
    return CompletableFuture.runAsync(() -> {
        try {
            processEvent(event);
        } catch (Exception e) {
            logger.error("Error processing event: {}", event.getTopic(), e);
            
            // Publish error event for monitoring
            Event errorEvent = Event.builder()
                .topic("system.error")
                .payload(createErrorDetails(e, event))
                .sender(getAgentId())
                .metadata("originalTopic", event.getTopic())
                .metadata("errorType", e.getClass().getSimpleName())
                .build();
            
            try {
                context.publishEvent(errorEvent);
            } catch (Exception publishError) {
                logger.error("Failed to publish error event", publishError);
            }
        }
    });
}
```

This API reference provides comprehensive coverage of the core AMCP v1.5 interfaces and classes. For more detailed information about specific implementations and advanced usage patterns, refer to the example applications and architecture documentation.