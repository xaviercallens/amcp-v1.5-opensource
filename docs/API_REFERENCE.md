# AMCP v1.5 - API Reference

Complete API reference for the Agent Mesh Communication Protocol (AMCP) v1.5 Open Source Edition.

---

## Table of Contents

- [Core Interfaces](#core-interfaces)
  - [Agent](#agent)
  - [MobileAgent](#mobileagent)
  - [AgentContext](#agentcontext)
  - [EventBroker](#eventbroker)
- [Core Classes](#core-classes)
  - [Event](#event)
  - [AgentID](#agentid)
  - [AgentLifecycle](#agentlifecycle)
- [Messaging](#messaging)
  - [Topic Patterns](#topic-patterns)
  - [Event Publishing](#event-publishing)
  - [Event Subscription](#event-subscription)
- [Mobility](#mobility)
  - [Mobility Operations](#mobility-operations)
  - [MigrationOptions](#migrationoptions)
  - [MobilityStrategy](#mobilitystrategy)
- [Security](#security)
  - [SecurityContext](#securitycontext)
  - [AuthenticationContext](#authenticationcontext)
- [Tool Integration](#tool-integration)
  - [ToolConnector](#toolconnector)
  - [LLM Integration](#llm-integration)
- [Best Practices](#best-practices)

---

## Core Interfaces

### Agent

The fundamental interface that all agents must implement.

#### Methods

##### `AgentID getAgentId()`
Returns the unique identifier for this agent instance.

**Returns:** `AgentID` - The agent's unique identifier

**Example:**
```java
AgentID id = agent.getAgentId();
System.out.println("Agent ID: " + id.toString());
```

---

##### `AgentContext getContext()`
Returns the current execution context for this agent.

**Returns:** `AgentContext` - The agent's execution context

**Example:**
```java
AgentContext context = agent.getContext();
EventBroker broker = context.getEventBroker();
```

---

##### `AgentLifecycle getLifecycleState()`
Returns the current lifecycle state of this agent.

**Returns:** `AgentLifecycle` - Current state (INACTIVE, ACTIVE, MIGRATING, etc.)

**Example:**
```java
if (agent.getLifecycleState() == AgentLifecycle.ACTIVE) {
    // Agent is ready to process events
}
```

---

##### `CompletableFuture<Void> handleEvent(Event event)`
Handles an incoming event asynchronously.

**Parameters:**
- `event` - The event to process

**Returns:** `CompletableFuture<Void>` - Completes when processing is done

**Example:**
```java
@Override
public CompletableFuture<Void> handleEvent(Event event) {
    return CompletableFuture.runAsync(() -> {
        switch (event.getTopic()) {
            case "request.process":
                processRequest(event.getPayload());
                break;
            case "status.check":
                respondWithStatus(event);
                break;
            default:
                log.debug("Unknown event: {}", event.getTopic());
        }
    });
}
```

---

##### `CompletableFuture<Void> publishEvent(String topic, Object payload)`
Publishes a simple event with topic and payload.

**Parameters:**
- `topic` - The event topic (e.g., "weather.forecast.request")
- `payload` - The event payload (must be serializable)

**Returns:** `CompletableFuture<Void>` - Completes when event is published

**Example:**
```java
publishEvent("weather.forecast.response", forecastData)
    .thenRun(() -> log.info("Forecast published"))
    .exceptionally(throwable -> {
        log.error("Failed to publish: {}", throwable.getMessage());
        return null;
    });
```

---

##### `CompletableFuture<Void> publishEvent(Event event)`
Publishes a complete event with full metadata control.

**Parameters:**
- `event` - The complete event to publish

**Returns:** `CompletableFuture<Void>` - Completes when event is published

**Example:**
```java
Event event = Event.builder()
    .topic("travel.plan.response")
    .payload(travelPlan)
    .correlationId(requestId)
    .metadata("priority", "high")
    .deliveryOptions(DeliveryOptions.RELIABLE)
    .build();
    
publishEvent(event)
    .thenRun(() -> log.info("Travel plan published"));
```

---

##### `CompletableFuture<Void> subscribe(String topicPattern)`
Subscribes to events matching the topic pattern.

**Parameters:**
- `topicPattern` - Topic pattern with wildcard support

**Returns:** `CompletableFuture<Void>` - Completes when subscription is established

**Topic Pattern Syntax:**
- `"weather.forecast"` - Exact match only
- `"weather.*"` - Matches "weather.forecast", "weather.alert" (one level)
- `"weather.**"` - Matches all sub-topics recursively

**Example:**
```java
subscribe("weather.**")
    .thenRun(() -> log.info("Subscribed to all weather events"));

subscribe("travel.request.*")
    .thenRun(() -> log.info("Subscribed to travel requests"));
```

---

##### `CompletableFuture<Void> unsubscribe(String topicPattern)`
Unsubscribes from events matching the topic pattern.

**Parameters:**
- `topicPattern` - Topic pattern to unsubscribe from

**Returns:** `CompletableFuture<Void>` - Completes when unsubscription is done

**Example:**
```java
unsubscribe("weather.**")
    .thenRun(() -> log.info("Unsubscribed from weather events"));
```

---

#### Lifecycle Callbacks

##### `void onActivate()`
Called when the agent transitions from INACTIVE to ACTIVE state.

**Use this to:**
- Initialize resources
- Subscribe to event topics
- Start background tasks
- Register with external services

**Example:**
```java
@Override
public void onActivate() {
    super.onActivate();
    log.info("Activating weather agent");
    
    // Subscribe to relevant topics
    subscribe("weather.forecast.request");
    subscribe("weather.alert.new");
    
    // Initialize HTTP client for API calls
    this.httpClient = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build();
    
    // Start periodic weather check
    scheduler = Executors.newScheduledThreadPool(1);
    scheduler.scheduleAtFixedRate(
        this::checkWeatherUpdates,
        0, 5, TimeUnit.MINUTES
    );
}
```

---

##### `void onDeactivate()`
Called when the agent transitions from ACTIVE to INACTIVE state.

**Use this to:**
- Clean up resources
- Unsubscribe from event topics
- Stop background tasks
- Save persistent state

**Example:**
```java
@Override
public void onDeactivate() {
    log.info("Deactivating weather agent");
    
    // Stop scheduled tasks
    if (scheduler != null) {
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
    
    // Close HTTP client
    if (httpClient != null) {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }
    
    // Save state to persistent storage
    saveState();
    
    super.onDeactivate();
}
```

---

##### `void onDestroy()`
Called when the agent is being permanently removed from the system.

**Use this for final cleanup.**

**Example:**
```java
@Override
public void onDestroy() {
    log.info("Destroying weather agent");
    
    // Final cleanup
    activeRequests.clear();
    cachedForecasts.clear();
    
    super.onDestroy();
}
```

---

##### `void onBeforeMigration(String destinationContext)`
Called before the agent is migrated to another context.

**Parameters:**
- `destinationContext` - The target context ID

**Use this to:**
- Prepare state for serialization
- Clean up context-specific resources
- Notify other agents of pending migration

**Example:**
```java
@Override
public void onBeforeMigration(String destinationContext) {
    log.info("Preparing to migrate to: {}", destinationContext);
    
    // Clean up non-serializable resources
    if (httpClient != null) {
        httpClient.dispatcher().executorService().shutdown();
    }
    
    // Notify peer agents
    publishEvent("agent.migration.started", 
        Map.of("destination", destinationContext));
    
    super.onBeforeMigration(destinationContext);
}
```

---

##### `void onAfterMigration(String sourceContext)`
Called after the agent has been migrated from another context.

**Parameters:**
- `sourceContext` - The original context ID

**Use this to:**
- Restore context-specific resources
- Re-establish connections
- Resume normal operation

**Example:**
```java
@Override
public void onAfterMigration(String sourceContext) {
    log.info("Completed migration from: {}", sourceContext);
    
    // Re-initialize HTTP client
    this.httpClient = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build();
    
    // Re-subscribe to topics (if needed)
    subscribe("weather.**");
    
    // Notify peer agents
    publishEvent("agent.migration.completed",
        Map.of("source", sourceContext));
    
    super.onAfterMigration(sourceContext);
}
```

---

### MobileAgent

Extended interface for agents supporting strong mobility operations (IBM Aglet-style).

#### Additional Methods

##### `CompletableFuture<Void> dispatch(String destinationContext)`
Moves this agent to a remote execution context.

**Parameters:**
- `destinationContext` - The target context identifier

**Returns:** `CompletableFuture<Void>` - Completes when dispatch is successful

**Guarantees:**
- Atomic operation (either succeeds completely or fails completely)
- State is preserved during migration
- Original agent instance is destroyed after successful migration

**Example:**
```java
// Move to edge device for local processing
dispatch("edge-device-paris")
    .thenRun(() -> log.info("Successfully moved to edge device"))
    .exceptionally(throwable -> {
        log.error("Failed to dispatch: {}", throwable.getMessage());
        return null;
    });
```

---

##### `CompletableFuture<AgentID> clone(String destinationContext)`
Creates a clone of this agent in a remote context.

**Parameters:**
- `destinationContext` - The target context for the clone

**Returns:** `CompletableFuture<AgentID>` - The AgentID of the created clone

**Note:** Both original and cloned agents continue to execute independently.

**Example:**
```java
// Create a backup clone in EU datacenter
clone("eu-datacenter-1")
    .thenAccept(cloneId -> {
        log.info("Clone created: {}", cloneId);
        clones.add(cloneId);
    });
```

---

##### `CompletableFuture<Void> retract(String sourceContext)`
Recalls an agent from a remote context back to the local context.

**Parameters:**
- `sourceContext` - The context from which to retract the agent

**Returns:** `CompletableFuture<Void>` - Completes when retraction is successful

**Example:**
```java
// Bring back agent from edge device
retract("edge-device-paris")
    .thenRun(() -> log.info("Agent recalled successfully"));
```

---

##### `CompletableFuture<Void> migrate(MigrationOptions options)`
Performs intelligent migration based on specified options.

**Parameters:**
- `options` - Migration preferences and constraints

**Returns:** `CompletableFuture<Void>` - Completes when migration is successful

**Example:**
```java
MigrationOptions options = MigrationOptions.builder()
    .strategy(MigrationStrategy.LOAD_BALANCED)
    .preferredRegions(List.of("us-east", "us-west"))
    .maxLatency(Duration.ofMillis(100))
    .minCpu(2)
    .minMemory(4096) // MB
    .build();

migrate(options)
    .thenRun(() -> log.info("Migrated to optimal context"));
```

---

##### `CompletableFuture<List<AgentID>> replicate(String... contexts)`
Creates replicas across multiple contexts for high availability.

**Parameters:**
- `contexts` - The target contexts for replication

**Returns:** `CompletableFuture<List<AgentID>>` - The AgentIDs of all replicas

**Use Cases:**
- Fault tolerance
- Load distribution
- Geographic distribution

**Example:**
```java
// Create replicas in multiple regions
replicate("us-east-1", "us-west-2", "eu-central-1")
    .thenAccept(replicaIds -> {
        log.info("Created {} replicas", replicaIds.size());
        replicaIds.forEach(id -> 
            log.info("  Replica: {}", id));
    });
```

---

##### `CompletableFuture<Void> federateWith(List<AgentID> agents, String federationId)`
Forms a federation with other agents for collaborative processing.

**Parameters:**
- `agents` - The list of agents to federate with
- `federationId` - Unique identifier for this federation

**Returns:** `CompletableFuture<Void>` - Completes when federation is established

**Example:**
```java
// Create federation of planning agents
List<AgentID> planners = List.of(
    findAgent("travel-planner"),
    findAgent("activity-planner"),
    findAgent("budget-planner")
);

federateWith(planners, "trip-planning-federation")
    .thenRun(() -> log.info("Federation established"));
```

---

##### `MobilityState getMobilityState()`
Returns the current mobility state of this agent.

**Returns:** `MobilityState` - Current state (STATIONARY, MIGRATING, REPLICATED, etc.)

**Example:**
```java
if (agent.getMobilityState() == MobilityState.MIGRATING) {
    log.warn("Agent is currently migrating, operation deferred");
}
```

---

##### `List<MigrationEvent> getMigrationHistory()`
Returns the migration history for this agent.

**Returns:** `List<MigrationEvent>` - Migration events in chronological order

**Example:**
```java
List<MigrationEvent> history = agent.getMigrationHistory();
history.forEach(event -> 
    log.info("Migrated from {} to {} at {}",
        event.getSourceContext(),
        event.getDestinationContext(),
        event.getTimestamp()));
```

---

##### `CompletableFuture<MobilityAssessment> assessMobility(String targetContext)`
Checks if this agent can migrate to the specified context.

**Parameters:**
- `targetContext` - The context to check migration feasibility

**Returns:** `CompletableFuture<MobilityAssessment>` - Assessment result

**Checks:**
- Context availability and compatibility
- Authentication and authorization
- Resource requirements
- Network connectivity

**Example:**
```java
assessMobility("edge-device-tokyo")
    .thenAccept(assessment -> {
        if (assessment.isFeasible()) {
            log.info("Migration is feasible");
            dispatch("edge-device-tokyo");
        } else {
            log.warn("Cannot migrate: {}", assessment.getReason());
        }
    });
```

---

### AgentContext

Agent execution context providing access to platform services.

#### Methods

##### `String getContextId()`
Returns the unique identifier for this context.

**Example:**
```java
String contextId = context.getContextId();
log.info("Running in context: {}", contextId);
```

---

##### `EventBroker getEventBroker()`
Returns the EventBroker instance for this context.

**Example:**
```java
EventBroker broker = context.getEventBroker();
broker.publish(event);
```

---

##### `MobilityManager getMobilityManager()`
Returns the MobilityManager for agent migration operations.

**Example:**
```java
MobilityManager manager = context.getMobilityManager();
manager.migrateAgent(agentId, "target-context");
```

---

##### `SecurityContext getSecurityContext()`
Returns the security context for this agent context.

**Example:**
```java
SecurityContext security = context.getSecurityContext();
if (security.hasPermission("weather.api.access")) {
    callWeatherAPI();
}
```

---

##### `CompletableFuture<Void> registerAgent(Agent agent)`
Registers an agent with this context.

**Parameters:**
- `agent` - The agent to register

**Example:**
```java
WeatherAgent agent = new WeatherAgent();
context.registerAgent(agent)
    .thenRun(() -> log.info("Agent registered"));
```

---

##### `CompletableFuture<Void> activateAgent(AgentID agentId)`
Activates an agent, transitioning it to ACTIVE state.

**Parameters:**
- `agentId` - The ID of the agent to activate

**Example:**
```java
context.activateAgent(agentId)
    .thenRun(() -> log.info("Agent activated"));
```

---

### EventBroker

Message broker interface for event-driven communication.

#### Methods

##### `CompletableFuture<Void> publish(Event event)`
Publishes an event to all subscribers.

**Parameters:**
- `event` - The event to publish

**Example:**
```java
Event event = Event.builder()
    .topic("notification.alert")
    .payload(alertData)
    .build();
    
broker.publish(event);
```

---

##### `CompletableFuture<Void> subscribe(String topicPattern, EventHandler handler)`
Subscribes to events matching a topic pattern.

**Parameters:**
- `topicPattern` - Topic pattern with wildcard support
- `handler` - Event handler callback

**Example:**
```java
broker.subscribe("weather.**", event -> {
    log.info("Received weather event: {}", event.getTopic());
    return CompletableFuture.completedFuture(null);
});
```

---

## Core Classes

### Event

Represents an event in the AMCP messaging system.

#### Builder Pattern

```java
Event event = Event.builder()
    .topic("travel.request.plan")
    .payload(travelRequest)
    .correlationId("trip-12345")
    .sender(agentId)
    .metadata("priority", "high")
    .metadata("source", "mobile-app")
    .deliveryOptions(DeliveryOptions.RELIABLE)
    .build();
```

#### Methods

- `String getTopic()` - Get the event topic
- `<T> T getPayload(Class<T> type)` - Get typed payload
- `String getCorrelationId()` - Get correlation ID
- `AgentID getSender()` - Get sender agent ID
- `Map<String, Object> getMetadata()` - Get all metadata
- `Object getMetadata(String key)` - Get specific metadata value
- `long getTimestamp()` - Get event timestamp
- `DeliveryOptions getDeliveryOptions()` - Get delivery preferences

---

### AgentID

Unique identifier for an agent.

#### Methods

- `String getId()` - Get the string representation
- `String getType()` - Get the agent type
- `equals(Object o)` - Compare agent IDs
- `hashCode()` - Hash code for collections

#### Example

```java
AgentID id = AgentID.of("weather-agent-001");
System.out.println(id.getId()); // "weather-agent-001"
```

---

### AgentLifecycle

Enum representing agent lifecycle states.

#### Values

- `INACTIVE` - Agent is registered but not processing events
- `ACTIVE` - Agent is actively processing events
- `MIGRATING` - Agent is being moved to another context
- `SUSPENDED` - Agent is temporarily suspended
- `DESTROYED` - Agent has been permanently removed

#### State Transitions

```
INACTIVE --> ACTIVE --> SUSPENDED --> ACTIVE
    |           |                       |
    |           +----> MIGRATING -------+
    |                                   |
    +-----------------> DESTROYED <-----+
```

---

## Messaging

### Topic Patterns

AMCP uses hierarchical topic patterns for flexible event routing.

#### Syntax

- **Exact match:** `"weather.forecast.request"`
  - Matches only this exact topic

- **Single-level wildcard (*):** `"weather.*"`
  - Matches: `"weather.forecast"`, `"weather.alert"`
  - Does NOT match: `"weather.forecast.hourly"`

- **Multi-level wildcard (**):** `"weather.**"`
  - Matches: `"weather.forecast"`, `"weather.forecast.hourly"`, `"weather.alert.severe"`
  - Matches all sub-topics recursively

#### Best Practices

```java
// Domain-based hierarchy
"travel.request.plan"
"travel.request.book"
"travel.response.plan"

// Feature-based hierarchy
"weather.forecast.hourly"
"weather.forecast.daily"
"weather.alert.severe"

// Entity-based hierarchy
"agent.weather-001.status"
"agent.weather-001.error"
```

---

### Event Publishing

#### Simple Publishing

```java
// Quick publish
publishEvent("status.update", statusData);

// With error handling
publishEvent("data.process", data)
    .exceptionally(throwable -> {
        log.error("Failed to publish: {}", throwable.getMessage());
        return null;
    });
```

#### Advanced Publishing

```java
Event event = Event.builder()
    .topic("payment.transaction.completed")
    .payload(transaction)
    .correlationId(orderId)
    .sender(getAgentId())
    .metadata("amount", transaction.getAmount())
    .metadata("currency", "USD")
    .deliveryOptions(DeliveryOptions.builder()
        .persistent(true)
        .priority(Priority.HIGH)
        .ttl(Duration.ofMinutes(5))
        .build())
    .build();

publishEvent(event);
```

---

### Event Subscription

#### Basic Subscription

```java
@Override
public void onActivate() {
    super.onActivate();
    
    // Subscribe to specific topics
    subscribe("order.new");
    subscribe("order.cancelled");
    
    // Subscribe to topic pattern
    subscribe("order.**");
}
```

#### Handling Events

```java
@Override
public CompletableFuture<Void> handleEvent(Event event) {
    return CompletableFuture.runAsync(() -> {
        String topic = event.getTopic();
        
        if (topic.startsWith("order.")) {
            handleOrderEvent(event);
        } else if (topic.startsWith("payment.")) {
            handlePaymentEvent(event);
        }
    });
}

private void handleOrderEvent(Event event) {
    switch (event.getTopic()) {
        case "order.new":
            Order order = event.getPayload(Order.class);
            processNewOrder(order);
            break;
        case "order.cancelled":
            String orderId = event.getPayload(String.class);
            cancelOrder(orderId);
            break;
    }
}
```

---

## Mobility

### Mobility Operations

#### Dispatch (Move Agent)

```java
// Simple dispatch
dispatch("edge-device-tokyo");

// With callback
dispatch("edge-device-tokyo")
    .thenRun(() -> {
        log.info("Now running on edge device");
        startLocalProcessing();
    });
```

#### Clone (Create Copy)

```java
// Clone to backup location
clone("backup-datacenter")
    .thenAccept(cloneId -> {
        log.info("Backup clone created: {}", cloneId);
        synchronizeWith(cloneId);
    });
```

#### Migrate (Intelligent Movement)

```java
MigrationOptions options = MigrationOptions.builder()
    .strategy(MigrationStrategy.COST_OPTIMIZED)
    .preferredRegions(List.of("us-west", "us-east"))
    .maxCost(0.10) // USD per hour
    .build();

migrate(options);
```

---

### MigrationOptions

Configuration for intelligent migration.

#### Builder

```java
MigrationOptions options = MigrationOptions.builder()
    .strategy(MigrationStrategy.LOAD_BALANCED)
    .preferredRegions(List.of("us-east", "eu-west"))
    .maxLatency(Duration.ofMillis(100))
    .minCpu(2)
    .minMemory(4096) // MB
    .minDiskSpace(10240) // MB
    .maxCost(0.20) // USD per hour
    .requireGpu(false)
    .build();
```

---

### MobilityStrategy

Strategy for selecting migration destination.

#### Values

- `LOAD_BALANCED` - Distribute load evenly across contexts
- `COST_OPTIMIZED` - Minimize operational cost
- `LATENCY_OPTIMIZED` - Minimize network latency
- `PROXIMITY_BASED` - Move closer to data/users
- `RESOURCE_CONSTRAINED` - Respect resource limits

---

## Security

### SecurityContext

Security context for authentication and authorization.

#### Methods

```java
SecurityContext security = context.getSecurityContext();

// Check permissions
if (security.hasPermission("api.weather.access")) {
    callWeatherAPI();
}

// Get authentication info
String userId = security.getUserId();
List<String> roles = security.getRoles();
```

---

### AuthenticationContext

Authentication context for tool calls and external services.

#### Usage

```java
AuthenticationContext auth = AuthenticationContext.builder()
    .userId("user-123")
    .token("Bearer eyJhbGc...")
    .roles(List.of("agent.admin", "api.access"))
    .build();

// Pass to tool connector
ToolResponse response = toolConnector.invoke(request, auth);
```

---

## Tool Integration

### ToolConnector

Interface for integrating external tools via MCP protocol.

#### Implementation Example

```java
public class WeatherAPIConnector extends AbstractToolConnector {
    
    @Override
    public CompletableFuture<ToolResponse> invoke(
            ToolRequest request,
            AuthenticationContext auth) {
        
        // Validate request
        validateRequest(request);
        
        // Call external API
        return callExternalAPI(request, auth)
            .thenApply(this::transformResponse);
    }
    
    @Override
    public Map<String, Object> getRequestSchema() {
        return Map.of(
            "location", Map.of("type", "string", "required", true),
            "units", Map.of("type", "string", "default", "metric")
        );
    }
}
```

---

### LLM Integration

Integration with Ollama/TinyLlama for AI-powered agents.

#### Example

```java
public class ChatAgent extends AbstractMobileAgent {
    
    private OllamaConnector llm;
    
    @Override
    public void onActivate() {
        super.onActivate();
        
        // Initialize LLM connector
        llm = new OllamaConnector("http://localhost:11434", "tinyllama");
        
        subscribe("chat.message");
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        if (event.getTopic().equals("chat.message")) {
            String userMessage = event.getPayload(String.class);
            
            return llm.generate(userMessage)
                .thenAccept(response -> {
                    publishEvent("chat.response", response);
                });
        }
        return CompletableFuture.completedFuture(null);
    }
}
```

---

## Best Practices

### Agent Design

1. **Keep agents focused** - One responsibility per agent
2. **Use async operations** - All I/O should be non-blocking
3. **Handle errors gracefully** - Never crash the agent
4. **Clean up resources** - Implement proper lifecycle callbacks

### Messaging

1. **Use hierarchical topics** - `domain.action.detail`
2. **Include correlation IDs** - For request-response patterns
3. **Add metadata** - For context and routing
4. **Set delivery options** - Based on importance

### Mobility

1. **Test migration** - Ensure state is serializable
2. **Handle migration failures** - Implement retry logic
3. **Clean up resources** - In `onBeforeMigration()`
4. **Restore resources** - In `onAfterMigration()`

### Performance

1. **Batch events** - When possible
2. **Use thread pools** - For parallel processing
3. **Cache frequently accessed data** - Reduce remote calls
4. **Monitor metrics** - Track throughput and latency

### Security

1. **Validate inputs** - Always validate event payloads
2. **Use authentication** - For all external calls
3. **Check permissions** - Before sensitive operations
4. **Encrypt sensitive data** - Both in transit and at rest

---

## Quick Reference

### Common Patterns

#### Request-Response

```java
// Publisher
String correlationId = UUID.randomUUID().toString();
Event request = Event.builder()
    .topic("service.request")
    .payload(requestData)
    .correlationId(correlationId)
    .build();
publishEvent(request);

// Responder
@Override
public CompletableFuture<Void> handleEvent(Event event) {
    if (event.getTopic().equals("service.request")) {
        Object response = processRequest(event.getPayload());
        
        return publishEvent(Event.builder()
            .topic("service.response")
            .payload(response)
            .correlationId(event.getCorrelationId())
            .build());
    }
    return CompletableFuture.completedFuture(null);
}
```

#### Pub/Sub Broadcast

```java
// Publisher
publishEvent("notification.broadcast", notificationData);

// Multiple subscribers
subscribe("notification.**");
```

#### Agent Migration

```java
// Before migration
@Override
public void onBeforeMigration(String dest) {
    saveState();
    closeConnections();
}

// After migration
@Override
public void onAfterMigration(String source) {
    restoreState();
    reconnect();
}
```

---

*For more examples, see `docs/DEVELOPER_GUIDE.md`*

*For architecture details, see `docs/ARCHITECTURE.md`*
