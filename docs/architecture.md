# AMCP v1.5 Architecture Overview

## Introduction

The Agent Mesh Communication Protocol (AMCP) v1.5 is a modern, scalable framework for building distributed agent-based systems. It provides a robust foundation for creating complex applications where autonomous agents communicate through event-driven messaging patterns.

## Core Principles

### 1. Agent-Centric Design
- **Everything is an Agent**: All computational entities implement the `io.amcp.core.Agent` interface
- **Autonomous Operation**: Agents make decisions independently based on received events
- **Lifecycle Management**: Complete control over agent creation, activation, deactivation, and destruction
- **State Isolation**: Each agent maintains its own state and context

### 2. Event-Driven Communication
- **Asynchronous Messaging**: All communication is non-blocking using CompletableFuture patterns
- **Pub/Sub Architecture**: Agents publish and subscribe to hierarchical topics
- **Loose Coupling**: Agents interact only through events, enabling system flexibility
- **Topic Hierarchy**: Structured topic naming supports selective event filtering

### 3. Scalable Architecture
- **Multi-Broker Support**: Pluggable message brokers (in-memory, Kafka, Solace, NATS)
- **Horizontal Scaling**: Agents can be distributed across multiple nodes
- **Load Balancing**: Event distribution across agent instances
- **Fault Tolerance**: Built-in error handling and recovery mechanisms

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    AMCP v1.5 Framework                     │
├─────────────────────────────────────────────────────────────┤
│  Application Layer                                          │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────┐ │
│  │   Travel    │ │   Weather   │ │      Custom             │ │
│  │   Planner   │ │   Monitor   │ │   Applications          │ │
│  └─────────────┘ └─────────────┘ └─────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│  Agent Framework Layer                                      │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────┐ │
│  │    Agent    │ │    Event    │ │      Messaging          │ │
│  │  Lifecycle  │ │  System     │ │     Infrastructure      │ │
│  └─────────────┘ └─────────────┘ └─────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│  Core Infrastructure                                        │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────┐ │
│  │   Context   │ │   Broker    │ │      Connectors         │ │
│  │ Management  │ │  Abstraction│ │    (Kafka, etc.)        │ │
│  └─────────────┘ └─────────────┘ └─────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Component Details

### Agent Interface (`io.amcp.core.Agent`)

The foundation of AMCP systems. All agents implement this interface:

```java
public interface Agent extends Serializable {
    CompletableFuture<Void> handleEvent(Event event);
    void setAgentContext(AgentContext context);
    AgentContext getAgentContext();
    AgentID getAgentId();
    void onActivate();
    void onDeactivate();
    void cleanup();
}
```

**Key Methods:**
- `handleEvent()`: Primary event processing method
- `onActivate()`: Called when agent becomes active
- `onDeactivate()`: Called before agent deactivation
- `cleanup()`: Resource cleanup and shutdown

### Event System (`io.amcp.core.Event`)

Events are the primary communication mechanism:

```java
Event event = Event.builder()
    .topic("weather.data.update")
    .payload(weatherData)
    .sender(getAgentId())
    .deliveryOptions(DeliveryOptions.RELIABLE)
    .metadata("region", "north-america")
    .build();
```

**Features:**
- **Hierarchical Topics**: Support for wildcard subscriptions (`weather.*`, `weather.**`)
- **Flexible Payloads**: Any serializable object as payload
- **Delivery Options**: Reliable, best-effort, ordered delivery modes
- **Rich Metadata**: Custom key-value metadata support

### Agent Context (`io.amcp.core.AgentContext`)

Provides runtime environment and services:

```java
public interface AgentContext {
    void publishEvent(Event event);
    void subscribe(String topicPattern);
    void unsubscribe(String topicPattern);
    AgentID createAgent(Class<? extends Agent> agentClass);
    void migrateAgent(AgentID agentId, String targetNode);
    ScheduledExecutorService getScheduler();
    EventBroker getEventBroker();
}
```

### Messaging Infrastructure

#### Event Broker Abstraction
- **Pluggable Architecture**: Support for multiple broker implementations
- **Topic Management**: Hierarchical topic routing and subscription management
- **Quality of Service**: Configurable delivery guarantees
- **Monitoring**: Built-in metrics and health monitoring

#### Supported Brokers
1. **InMemoryEventBroker**: Testing and development
2. **KafkaEventBroker**: Production scalability (planned)
3. **SolaceEventBroker**: Enterprise messaging (planned)
4. **NATSEventBroker**: Cloud-native deployments (planned)

## Design Patterns

### 1. Agent Lifecycle Pattern

```java
public class MyAgent implements Agent {
    private AgentLifecycle state = AgentLifecycle.INACTIVE;
    private ScheduledExecutorService scheduler;
    
    @Override
    public void onActivate() {
        state = AgentLifecycle.ACTIVE;
        scheduler = Executors.newScheduledThreadPool(2);
        subscribe("my.topic.*");
        
        // Start periodic tasks
        scheduler.scheduleAtFixedRate(this::performPeriodicTask, 
                                    0, 30, TimeUnit.SECONDS);
    }
    
    @Override
    public void cleanup() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
```

### 2. Event Handling Pattern

```java
@Override
public CompletableFuture<Void> handleEvent(Event event) {
    return CompletableFuture.runAsync(() -> {
        try {
            switch (event.getTopic()) {
                case "data.request":
                    handleDataRequest(event);
                    break;
                case "system.shutdown":
                    handleShutdown(event);
                    break;
                default:
                    logger.debug("Unhandled event: {}", event.getTopic());
            }
        } catch (Exception e) {
            logger.error("Error processing event: {}", event.getTopic(), e);
            // Publish error event for monitoring
            publishErrorEvent(e, event);
        }
    });
}
```

### 3. Request-Response Pattern

```java
// Request
public CompletableFuture<String> requestData(String query) {
    String correlationId = UUID.randomUUID().toString();
    CompletableFuture<String> future = new CompletableFuture<>();
    
    // Store future for response correlation
    pendingRequests.put(correlationId, future);
    
    Event request = Event.builder()
        .topic("data.request")
        .payload(query)
        .metadata("correlationId", correlationId)
        .metadata("replyTo", "data.response." + getAgentId())
        .build();
    
    publishEvent(request);
    return future;
}

// Response handling
private void handleDataResponse(Event event) {
    String correlationId = event.getMetadata("correlationId");
    CompletableFuture<String> future = pendingRequests.remove(correlationId);
    
    if (future != null) {
        future.complete((String) event.getPayload());
    }
}
```

## Scalability Considerations

### Horizontal Scaling
- **Agent Distribution**: Agents can run on different nodes
- **Load Balancing**: Topic-based routing distributes load
- **State Partitioning**: Stateful agents can be partitioned by key

### Performance Optimization
- **Async Processing**: Non-blocking event handling
- **Batch Processing**: Group related events for efficiency
- **Connection Pooling**: Efficient resource utilization
- **Caching**: Agent-level caching for frequently accessed data

### Fault Tolerance
- **Circuit Breakers**: Prevent cascade failures
- **Retry Mechanisms**: Configurable retry policies
- **Graceful Degradation**: Fallback strategies for service failures
- **Health Monitoring**: Continuous system health checks

## Security Model

### Authentication & Authorization
- **Agent Identity**: Unique agent IDs with verification
- **Topic Permissions**: Fine-grained access control
- **Secure Communication**: Encrypted message transport
- **Audit Logging**: Complete event audit trail

### Data Protection
- **Payload Encryption**: Sensitive data protection
- **Secure Serialization**: Safe object serialization
- **Input Validation**: Event payload validation
- **Privacy Controls**: PII handling and protection

## Monitoring & Observability

### Metrics Collection
- **Agent Metrics**: CPU, memory, event processing rates
- **System Metrics**: Broker performance, network utilization
- **Business Metrics**: Application-specific KPIs
- **Custom Metrics**: User-defined monitoring points

### Distributed Tracing
- **Event Correlation**: Track events across agent boundaries
- **Performance Analysis**: Identify bottlenecks and hotspots
- **Error Tracking**: Complete error context and stack traces
- **Dependency Mapping**: Visualize agent interactions

### Logging Strategy
- **Structured Logging**: JSON-formatted log entries
- **Context Preservation**: Correlation IDs across calls
- **Log Levels**: Configurable verbosity levels
- **Log Aggregation**: Centralized log collection and analysis

## Deployment Patterns

### Single-Node Deployment
- Development and testing environments
- Simplified configuration and debugging
- InMemoryEventBroker for basic setups

### Multi-Node Cluster
- Production scalability requirements
- External message broker (Kafka, Solace)
- Load balancing and failover capabilities

### Cloud-Native Deployment
- Kubernetes orchestration
- Container-based agent deployment
- Auto-scaling based on event load
- Service mesh integration

### Microservices Integration
- Agent-per-service architecture
- API gateway integration
- External service connectors
- Event-driven microservices communication

## Best Practices

### Agent Design
- **Single Responsibility**: One primary function per agent
- **Stateless When Possible**: Easier scaling and recovery
- **Idempotent Operations**: Safe retry mechanisms
- **Resource Management**: Proper cleanup and shutdown

### Event Design
- **Clear Topic Naming**: Hierarchical and descriptive topics
- **Payload Versioning**: Backward-compatible data structures
- **Appropriate Granularity**: Neither too fine nor too coarse
- **Error Handling**: Comprehensive error event patterns

### Performance Guidelines
- **Async Operations**: Avoid blocking calls in event handlers
- **Batch Processing**: Group related operations
- **Connection Reuse**: Minimize connection overhead
- **Memory Management**: Monitor and control memory usage

### Testing Strategies
- **Unit Testing**: Individual agent behavior
- **Integration Testing**: Multi-agent scenarios
- **Load Testing**: Performance under stress
- **Chaos Testing**: Fault injection and recovery

## Migration from v1.4

### Key Changes
- **Package Structure**: `com.amcp.*` → `io.amcp.*`
- **Enhanced Security**: New authentication and authorization
- **Improved Monitoring**: Built-in observability features
- **Better Error Handling**: Comprehensive error management
- **Performance Improvements**: Optimized event processing

### Migration Steps
1. Update package imports
2. Review agent lifecycle methods
3. Update event handling patterns
4. Configure new security features
5. Set up monitoring and logging

## Future Roadmap

### Planned Features
- **Advanced Connectors**: Kafka, Solace, NATS brokers
- **AI Integration**: Machine learning agent capabilities
- **Graph Analytics**: Agent relationship analysis
- **Real-time Dashboards**: Visual system monitoring
- **Auto-scaling**: Dynamic resource allocation

### Research Areas
- **Quantum Computing**: Quantum agent algorithms
- **Edge Computing**: IoT and edge deployment
- **Blockchain Integration**: Decentralized agent networks
- **Advanced AI**: Autonomous learning agents

---

This architecture overview provides the foundation for understanding AMCP v1.5's design principles and implementation patterns. For specific implementation details, refer to the API documentation and code examples.