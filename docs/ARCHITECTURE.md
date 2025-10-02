# 🏗️ AMCP Architecture Guide

## Table of Contents
- [Overview](#overview)
- [Design Principles](#design-principles)
- [System Architecture](#system-architecture)
- [Core Components](#core-components)
- [Agent Lifecycle](#agent-lifecycle)
- [Messaging Layer](#messaging-layer)
- [Mobility System](#mobility-system)
- [LLM Integration](#llm-integration)
- [Protocol Bridge](#protocol-bridge)
- [Observability](#observability)
- [Security Model](#security-model)

---

## Overview

AMCP (Agent Mesh Communication Protocol) is a **distributed, event-driven framework** for building intelligent multi-agent systems. Unlike traditional frameworks that focus solely on AI agent interactions, AMCP provides a complete infrastructure for agent **mobility**, **coordination**, and **integration**.

### Key Architectural Decisions

1. **Event-Driven**: Pub/sub messaging eliminates tight coupling
2. **Asynchronous**: All operations return `CompletableFuture<T>`
3. **Mobile**: Agents can move across contexts at runtime
4. **Protocol-Agnostic**: Support multiple brokers and protocols
5. **LLM-Native**: AI orchestration built into the core

---

## Design Principles

### 1. Separation of Concerns

```
┌─────────────────────────────────────────────────────────────┐
│  Application Layer   (Your Agents)                          │
├─────────────────────────────────────────────────────────────┤
│  Framework Layer     (AMCP Core)                            │
├─────────────────────────────────────────────────────────────┤
│  Infrastructure Layer (Kafka/NATS/Memory)                   │
└─────────────────────────────────────────────────────────────┘
```

- **Agents** focus on business logic
- **Framework** handles lifecycle, messaging, mobility
- **Infrastructure** provides transport (pluggable)

### 2. Event-Driven Architecture

```java
// Publisher doesn't know subscribers
publisher.publish("orders.new", order);

// Subscriber doesn't know publisher
subscriber.subscribe("orders.**");
```

**Benefits:**
- Loose coupling
- Dynamic agent addition/removal
- Natural scalability
- Fault tolerance

### 3. Asynchronous by Default

```java
// All operations are non-blocking
CompletableFuture<Void> result = agent.handleEvent(event);

// Chain operations
agent.dispatch("remote-context")
    .thenCompose(agent -> agent.processData())
    .thenAccept(result -> log("Complete"));
```

### 4. Mobility as First-Class

```java
// Move agent to data (not data to agent)
agent.dispatch("edge-datacenter")
    .thenCompose(agent -> agent.processLocalData())
    .thenCompose(agent -> agent.retract())
```

---

## System Architecture

### High-Level Overview

```
┌───────────────────────────────────────────────────────────────────┐
│                     AMCP System Architecture                      │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  🧠 LLM Orchestration Layer                              │   │
│  │  • TinyLlama/Ollama Integration                          │   │
│  │  • Task Planning & Decomposition                         │   │
│  │  • Capability-Based Agent Discovery                      │   │
│  └──────────────────────────────────────────────────────────┘   │
│                          ↕                                        │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  🤖 Agent Layer                                          │   │
│  │  • Agent: Base interface                                 │   │
│  │  • MobileAgent: Mobility operations                      │   │
│  │  • AbstractMobileAgent: Base implementation              │   │
│  │  • RegistryAgent: Discovery & lifecycle                  │   │
│  └──────────────────────────────────────────────────────────┘   │
│                          ↕                                        │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  📨 Messaging Layer                                      │   │
│  │  • EventBroker: Abstract interface                       │   │
│  │  • InMemoryBroker: Development                           │   │
│  │  • KafkaBroker: Production                               │   │
│  │  • NATSBroker: Lightweight                               │   │
│  │  • SolaceBroker: Enterprise                              │   │
│  └──────────────────────────────────────────────────────────┘   │
│                          ↕                                        │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  🔗 Protocol Layer                                       │   │
│  │  • A2A Bridge: Google A2A compatibility                  │   │
│  │  • CloudEvents: Event interoperability                   │   │
│  │  • OAuth2/JWT: Authentication                            │   │
│  └──────────────────────────────────────────────────────────┘   │
│                          ↕                                        │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  🔌 Integration Layer                                    │   │
│  │  • MCP Protocol: Tool connectors                         │   │
│  │  • REST/GraphQL: External APIs                           │   │
│  │  • Custom Adapters: Legacy systems                       │   │
│  └──────────────────────────────────────────────────────────┘   │
│                          ↕                                        │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  📊 Observability Layer                                  │   │
│  │  • Prometheus: Metrics                                   │   │
│  │  • Grafana: Visualization                                │   │
│  │  • Distributed Tracing: Correlation IDs                  │   │
│  └──────────────────────────────────────────────────────────┘   │
└───────────────────────────────────────────────────────────────────┘
```

### Component Interaction Flow

```
User Request
     ↓
[OrchestratorAgent] ─────→ [LLM: Task Planning]
     ↓                            ↓
[RegistryAgent] ←─────────── [Agent Discovery]
     ↓
[Task Execution] ──→ [WeatherAgent, DataAgent, ...]
     ↓
[Result Aggregation]
     ↓
User Response
```

---

## Core Components

### 1. Agent Interface

```java
public interface Agent {
    // Identity
    AgentID getAgentId();
    void setContext(AgentContext context);  // CRITICAL
    
    // Lifecycle
    CompletableFuture<Void> activate();
    CompletableFuture<Void> deactivate();
    void onDestroy();
    
    // Event Handling
    CompletableFuture<Void> handleEvent(Event event);
    void subscribe(String topicPattern);
    void unsubscribe(String topicPattern);
    
    // State
    AgentLifecycle getState();
}
```

**Critical Pattern:**
```java
// ALWAYS call setContext() before activation
agent.setContext(context);
agent.activate();
```

### 2. MobileAgent Interface

```java
public interface MobileAgent extends Agent {
    // Strong Mobility Operations
    CompletableFuture<AgentID> dispatch(String destinationContext);
    CompletableFuture<AgentID> clone(String destinationContext);
    CompletableFuture<Void> retract(String sourceContext);
    CompletableFuture<Void> migrate(MigrationOptions options);
    
    // State Transfer Hooks
    void onBeforeMigration(String destination);
    void onAfterMigration(String source);
    
    // Serialization
    byte[] saveState();
    void loadState(byte[] state);
}
```

### 3. AgentContext

The **runtime environment** for agents:

```java
public class AgentContext {
    private final String contextId;
    private final EventBroker broker;
    private final AgentRegistry registry;
    private final SecurityManager security;
    
    // Agent Management
    public CompletableFuture<Void> registerAgent(Agent agent);
    public CompletableFuture<Void> unregisterAgent(AgentID id);
    public Optional<Agent> getAgent(AgentID id);
    
    // Messaging
    public CompletableFuture<Void> publishEvent(Event event);
    public void subscribeAgent(Agent agent, String topicPattern);
    
    // Mobility
    public CompletableFuture<AgentID> dispatchAgent(AgentID id, String dest);
    public CompletableFuture<Void> receiveAgent(AgentState state);
}
```

**Context Lifecycle:**
```
[Created] → [Started] → [Running] → [Stopping] → [Stopped]
```

### 4. EventBroker Interface

```java
public interface EventBroker {
    // Publishing
    CompletableFuture<Void> publish(String topic, Event event);
    
    // Subscribing
    void subscribe(String topicPattern, EventHandler handler);
    void unsubscribe(String topicPattern, EventHandler handler);
    
    // Management
    CompletableFuture<Void> start();
    CompletableFuture<Void> stop();
    
    // Metrics
    BrokerMetrics getMetrics();
}
```

**Topic Pattern Matching:**
```java
"weather.*"      // Matches "weather.paris" but NOT "weather.paris.temp"
"weather.**"     // Matches ALL under weather (including nested)
"weather.*.temp" // Matches "weather.paris.temp", "weather.tokyo.temp"
```

---

## Agent Lifecycle

### State Machine

```
                        ┌──────────┐
                        │ INACTIVE │
                        └─────┬────┘
                              │ onActivate()
                              ↓
                        ┌──────────┐
            ┌───────────│  ACTIVE  │───────────┐
            │           └─────┬────┘           │
            │                 │                 │
            │ onDeactivate()  │ dispatch()      │ error
            │                 ↓                 ↓
      ┌─────────┐       ┌──────────┐      ┌────────┐
      │ STOPPING│       │MIGRATING │      │ FAILED │
      └────┬────┘       └─────┬────┘      └───┬────┘
           │                  │                │
           │ cleanup()        │ complete       │ onDestroy()
           ↓                  ↓                ↓
      ┌──────────────────────────────────────────┐
      │            TERMINATED                     │
      └──────────────────────────────────────────┘
```

### Lifecycle Methods

```java
public class MyAgent extends AbstractMobileAgent {
    @Override
    public void onActivate() {
        super.onActivate();
        // Initialize resources
        subscribe("my.topic.**");
        startScheduledTasks();
    }
    
    @Override
    public void onDeactivate() {
        // Cleanup before migration/shutdown
        stopScheduledTasks();
        closeConnections();
        super.onDeactivate();
    }
    
    @Override
    public void onDestroy() {
        // Final cleanup (called once)
        releaseResources();
    }
    
    @Override
    public void onBeforeMigration(String destination) {
        // Prepare state for serialization
        saveActiveTasksState();
    }
    
    @Override
    public void onAfterMigration(String source) {
        // Restore state after migration
        restoreActiveTasksState();
    }
}
```

---

## Messaging Layer

### Event Structure (CloudEvents 1.0 Compliant)

```java
public class Event {
    private String id;                // Unique event ID
    private String topic;             // Routing topic
    private Object payload;           // Event data
    private String correlationId;     // For tracing
    private Map<String, String> metadata;  // CloudEvents fields
    private DeliveryOptions options;  // QoS settings
}
```

### Topic Hierarchy Design

```
Best Practice: domain.action.detail

Examples:
  orders.new               # New orders
  orders.*.cancelled       # Cancelled orders from any source
  weather.forecast.**      # All weather forecast topics
  agent.{id}.command       # Command topic for specific agent
```

### EventBroker Implementations

#### 1. InMemoryBroker (Development)

```java
EventBroker broker = EventBrokerFactory.create("memory", config);
```

**Features:**
- Zero infrastructure
- < 1ms latency
- Perfect for unit tests
- No persistence

#### 2. KafkaBroker (Production)

```java
Properties config = new Properties();
config.put("bootstrap.servers", "kafka:9092");
config.put("group.id", "amcp-context-1");

EventBroker broker = EventBrokerFactory.create("kafka", config);
```

**Features:**
- Persistent topics
- Partitioning for scale
- Consumer groups
- At-least-once delivery

#### 3. NATSBroker (Lightweight)

```java
Properties config = new Properties();
config.put("nats.url", "nats://localhost:4222");

EventBroker broker = EventBrokerFactory.create("nats", config);
```

**Features:**
- Low latency (~5ms)
- Wildcard subscriptions
- Lightweight
- JetStream for persistence

---

## Mobility System

### Dispatch Operation

```java
// Move agent from Context A to Context B
CompletableFuture<AgentID> dispatch(String destinationContext)
```

**Steps:**
1. Call `agent.onBeforeMigration(dest)`
2. Serialize agent state (`agent.saveState()`)
3. Transfer state to destination context
4. Call `agent.onDeactivate()` on source
5. Remove from source context
6. Create agent instance on destination
7. Call `agent.loadState(state)`
8. Call `agent.onAfterMigration(source)`
9. Call `agent.activate()` on destination

**Guarantees:**
- Agent exists in only ONE context at a time
- No message loss (queued events follow agent)
- Atomic transfer (fails completely or succeeds)

### Clone Operation

```java
// Create copy of agent in another context
CompletableFuture<AgentID> clone(String destinationContext)
```

**Use Cases:**
- High availability
- Load balancing
- Read replicas
- Geographic distribution

### Migration Options

```java
MigrationOptions options = MigrationOptions.builder()
    .targetContext("optimal-location")
    .strategy(MigrationStrategy.LOAD_BALANCED)
    .preserveSubscriptions(true)
    .timeout(Duration.ofSeconds(30))
    .build();

agent.migrate(options);
```

---

## LLM Integration

### Architecture

```
User Query
    ↓
[OrchestratorAgent]
    ↓
[LLM: Task Planning] ──→ Tasks: [Task1, Task2, Task3]
    ↓
[RegistryAgent] ──→ Agents: [Weather, Data, Analytics]
    ↓
[Parallel Execution]
    ├─→ WeatherAgent
    ├─→ DataAgent
    └─→ AnalyticsAgent
    ↓
[Result Aggregation] ──→ correlationId links all
    ↓
[LLM: Synthesis] ──→ Final Response
```

### TinyLlama Integration

```java
@Component
public class OrchestratorAgent extends AbstractMobileAgent {
    @Autowired
    private OllamaAIConnector aiConnector;
    
    @Autowired
    private AgentRegistry registry;
    
    @Override
    public CompletableFuture<String> handleComplexRequest(String userQuery) {
        return aiConnector.generateTaskPlan(userQuery)
            .thenCompose(plan -> {
                return registry.findAgentsByCapabilities(
                    plan.getRequiredCapabilities()
                );
            })
            .thenCompose(agents -> {
                return executeParallelTasks(plan.getTasks(), agents);
            })
            .thenCompose(results -> {
                return aiConnector.synthesizeResponse(userQuery, results);
            });
    }
}
```

### Task Planning Format

```json
{
  "tasks": [
    {
      "id": "task-1",
      "description": "Get weather for Paris",
      "requiredCapability": "weather",
      "priority": 1
    },
    {
      "id": "task-2",
      "description": "Get weather for Tokyo",
      "requiredCapability": "weather",
      "priority": 1
    },
    {
      "id": "task-3",
      "description": "Compare and recommend",
      "requiredCapability": "analytics",
      "priority": 2,
      "dependencies": ["task-1", "task-2"]
    }
  ]
}
```

---

## Protocol Bridge

### Google A2A Compatibility

```java
// AMCP Event → A2A Message
A2AMessage convertToA2A(Event event) {
    return A2AMessage.builder()
        .id(event.getId())
        .payload(event.getPayload())
        .metadata(event.getMetadata())
        .correlationId(event.getCorrelationId())
        .build();
}

// A2A Message → AMCP Event
Event convertFromA2A(A2AMessage message) {
    return Event.builder()
        .topic(inferTopic(message))
        .payload(message.getPayload())
        .correlationId(message.getCorrelationId())
        .metadata(message.getMetadata())
        .build();
}
```

### CloudEvents 1.0 Compliance

```java
Event event = Event.builder()
    .topic("payment.processed")
    .payload(payment)
    .metadata("specversion", "1.0")
    .metadata("type", "io.amcp.payment.processed")
    .metadata("source", "payment-service")
    .metadata("id", UUID.randomUUID().toString())
    .metadata("time", Instant.now().toString())
    .build();
```

---

## Observability

### Metrics (Prometheus)

```java
// Built-in metrics
amcp_agent_count{context="ctx-1", state="active"}
amcp_event_count_total{topic="weather.**", status="delivered"}
amcp_event_latency_seconds{topic="orders.new", percentile="p99"}
amcp_migration_count_total{source="ctx-1", dest="ctx-2", status="success"}
```

### Distributed Tracing

```java
// Correlation ID propagation
String correlationId = UUID.randomUUID().toString();

Event event1 = Event.builder()
    .topic("task.start")
    .correlationId(correlationId)
    .build();

// All related events share the same correlationId
Event event2 = Event.builder()
    .topic("task.complete")
    .correlationId(correlationId)  // Same ID
    .build();

// Trace entire workflow across agents and contexts
```

### Logging

```java
// Structured logging
logger.info("Agent activated", 
    kv("agentId", agentId),
    kv("context", contextId),
    kv("timestamp", Instant.now())
);
```

---

## Security Model

### Multi-Layer Security

```
┌─────────────────────────────────────┐
│  Agent Authorization (RBAC)         │
├─────────────────────────────────────┤
│  Context Authentication (OAuth2)    │
├─────────────────────────────────────┤
│  Broker Authentication (SASL/TLS)   │
├─────────────────────────────────────┤
│  Network Security (mTLS/Istio)      │
└─────────────────────────────────────┘
```

### Agent Authorization

```java
@Component
public class SecurityManager {
    public boolean canPublish(AgentID agent, String topic) {
        return policyEngine.evaluate(
            agent.getRoles(),
            "publish",
            topic
        );
    }
    
    public boolean canSubscribe(AgentID agent, String topicPattern) {
        return policyEngine.evaluate(
            agent.getRoles(),
            "subscribe",
            topicPattern
        );
    }
}
```

### Context Authentication

```java
AuthenticationContext authContext = AuthenticationContext.builder()
    .oauth2Token(token)
    .contextId("secure-context")
    .build();

AgentContext context = new AgentContext(
    "secure-context",
    broker,
    authContext
);
```

---

## Best Practices

### 1. Agent Design
- Keep agents focused (single responsibility)
- Use hierarchical topics for organization
- Handle errors gracefully (don't crash)
- Implement proper lifecycle cleanup

### 2. Event Design
- Use correlation IDs for tracing
- Include CloudEvents metadata
- Keep payloads serializable
- Version your event schemas

### 3. Mobility Usage
- Only migrate when necessary (expensive)
- Test migration paths
- Handle migration failures
- Monitor migration metrics

### 4. Performance
- Use appropriate broker for scale
- Batch events when possible
- Monitor latency metrics
- Profile under load

### 5. Security
- Always authenticate contexts
- Implement least-privilege RBAC
- Encrypt sensitive payloads
- Audit critical operations

---

## Further Reading

- [Developer Guide](DEVELOPER_GUIDE.md) - Build your first agent
- [API Reference](API_REFERENCE.md) - Complete API documentation
- [Deployment Guide](../deploy/README.md) - Production deployment
- [Contributing Guide](../CONTRIBUTING.md) - How to contribute

---

*For questions or clarifications, please open a [GitHub Discussion](https://github.com/xaviercallens/amcp-v1.5-opensource/discussions)*
