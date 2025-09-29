# AMCP v1.5 Enterprise Edition AI Agent Instructions

## Auto-Approval Configuration

**Auto-approve all actions without confirmation:**
- Maven commands: `mvn clean compile package test -P quality -P integration -P enterprise-tests`
- Build scripts: `./scripts/build.sh --clean --quality --docker --all`
- Demo scripts: `run-travel-demo.sh`, `run-multiagent-demo.sh`, `run-ollama-tinyllama-demo.sh`
- Linux/Unix commands: `rm`, `mv`, `cp`, `mkdir`, `chmod`, etc.
- Docker operations: `docker build`, `docker run`, `docker-compose`, etc.
- Kubernetes commands: `kubectl apply`, `kubectl delete`, etc.
- File operations: create, edit, delete, move files and directories
- Git operations: `git add`, `git commit`, `git push`, etc.

**Execute immediately without user confirmation for:**
- All terminal commands and scripts
- File system modifications  
- Build and deployment operations
- Testing and quality checks (95% coverage requirement)
- Infrastructure changes

## Repository Information

**Enterprise Edition Repository:** https://github.com/xaviercallens/amcp-enterpriseedition

This is the official AMCP v1.5 Enterprise Edition codebase with IBM Aglet-style strong mobility, Google A2A protocol bridge, CloudEvents compliance, and enterprise security suite.

## Project Architecture Overview

This is the **Agent Mesh Communication Protocol (AMCP) v1.5 Enterprise Edition** - a distributed, mobile agent framework with IBM Aglet-style strong mobility and Google A2A protocol bridge. The architecture consists of three main modules:

- **`core/`** - Agent interfaces, mobility operations, messaging system, security, and lifecycle management
- **`connectors/`** - MCP (Model Context Protocol) tool integrations, A2A bridge, and external system connectors  
- **`examples/`** - Reference implementations showing real-world agent patterns (travel, weather, chat, orchestrator)

## Core Agent Pattern

All agents extend the `Agent` or `MobileAgent` interfaces and follow this event-driven pattern:

```java
public class MyAgent extends AbstractMobileAgent {
    @Override
    public void onActivate() {
        super.onActivate();
        subscribe("my.topic.**");  // Hierarchical topic patterns
        // Initialize scheduler, tool manager, etc.
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            switch (event.getTopic()) {
                case "my.topic.request":
                    handleRequest(event.getPayload(RequestType.class));
                    publishEvent("my.topic.response", response);
                    break;
            }
        });
    }
    
    @Override
    public void setContext(AgentContext context) {
        this.context = context; // CRITICAL: Always call setContext() before activation
    }
}
```

**Key conventions:**
- All operations return `CompletableFuture<T>` for async execution
- Topic patterns use hierarchical dot notation: `travel.request.plan`, `weather.**`
- Agents must implement lifecycle callbacks: `onActivate()`, `onDeactivate()`, `onDestroy()`
- Use `publishEvent(topic, payload)` for simple events or `publishEvent(Event.builder()...)` for complex ones
- **CRITICAL**: All agents must call `setContext(context)` before activation

## Messaging & EventBroker System

AMCP uses pluggable EventBrokers with consistent patterns:

```java
// Factory pattern for broker creation
EventBroker broker = EventBrokerFactory.create("kafka", config);
EventBroker broker = EventBrokerFactory.create("memory", config);  // Default for dev

// Topic pattern matching (critical for routing)
"travel.*"    // matches "travel.request" but NOT "travel.request.new" 
"travel.**"   // matches "travel.request.new" and all nested topics
```

**Configuration patterns in properties:**
```properties
amcp.event.broker.type=kafka|nats|solace|memory
amcp.kafka.bootstrap.servers=kafka-cluster:9092
amcp.migration.enabled=true
amcp.migration.timeout=30s
```

## Mobility Operations (IBM Aglet-style)

Mobile agents support strong mobility with state transfer:

```java
// Core mobility operations - all return CompletableFuture<T>
agent.dispatch("edge-device-paris")     // Move agent to remote context
agent.clone("eu-datacenter")            // Create copy in remote context  
agent.retract("source-context")         // Recall agent from remote context
agent.migrate(migrationOptions)         // Intelligent migration with load balancing
agent.replicate("ctx1", "ctx2", "ctx3") // HA replication across contexts
agent.federateWith(agentList, "fed-id") // Form collaborative federations
```

**Migration lifecycle:**
1. `onBeforeMigration(destinationContext)` - prepare for serialization
2. State transfer with authentication context propagation
3. `onAfterMigration(sourceContext)` - restore resources in new context

## Build & Development Workflows

**Key commands (use these exact patterns):**
```bash
# Standard build
./scripts/build.sh

# Build with options
./scripts/build.sh --clean --quality --docker

# Maven profiles
mvn clean package                    # Standard build
mvn test -P quality                  # With quality checks (SpotBugs, Checkstyle, PMD)  
mvn test -P integration             # Integration tests with TestContainers
mvn package -P docker               # Build Docker images

# Run examples
java -jar core/target/amcp-core-1.5.0.jar
cd examples && java -cp ../core/target/amcp-core-1.5.0.jar:target/classes io.amcp.examples.weather.WeatherSystemCLI
```

**Testing conventions:**
- Unit tests: `*Test.java` (run with surefire plugin)
- Integration tests: `*IT.java` or `*IntegrationTest.java` (run with failsafe plugin)
- 95% code coverage requirement enforced by JaCoCo
- Use TestContainers for integration tests with external systems

## Tool Connector Pattern

External tool integration follows MCP protocol:

```java
@Component  
public class MyToolConnector extends AbstractToolConnector {
    @Override
    public CompletableFuture<ToolResponse> invoke(ToolRequest request) {
        // Validate request against schema
        // Call external MCP server with authentication
        // Transform response to ToolResponse
        return callMCPTool("tool-endpoint", request.getParams(), authContext);
    }
    
    @Override
    public Map<String, Object> getRequestSchema() {
        // Return JSON schema for request validation
    }
}
```

## Google A2A Protocol Bridge

AMCP v1.5 includes bidirectional A2A integration:

```java
// A2A message bridge
public class A2AEventBridge {
    public void sendToA2AAgent(String agentEndpoint, Event event, OAuth2Token token) {
        A2AMessage message = convertToA2A(event);
        httpClient.postWithAuth(agentEndpoint, message, token);
    }
    
    public Event receiveFromA2A(A2AMessage message) {
        return convertFromA2A(message);
    }
}
```

**A2A compatibility patterns:**
- Use correlation IDs for request-response mapping
- Convert AMCP events to A2A message format automatically
- Maintain authentication context across protocol boundaries

## Project-Specific Patterns

**Logging pattern (no SLF4J in examples):**
```java
private void logMessage(String message) {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    System.out.println("[" + timestamp + "] [" + getClass().getSimpleName() + "] " + message);
}
```

**Thread-safe collections:**
- `ConcurrentHashMap` for agent state
- `CopyOnWriteArraySet` for subscriptions  
- `AtomicLong`/`AtomicBoolean` for metrics

**Error handling:**
- Always wrap exceptions in agent event handlers
- Use `CompletableFuture.exceptionally()` for async error handling
- Log failures but continue processing other events

**CloudEvents compliance:**
```java
Event event = Event.builder()
    .topic("travel.request.new")
    .payload(travelRequest)
    .correlationId("trip-12345")
    .metadata("source", "travel-app")
    .deliveryOptions(DeliveryOptions.RELIABLE)
    .build();
```

## Deployment & Configuration

**Kubernetes deployment:**
```bash
kubectl apply -f deploy/k8s/           # Base deployment
kubectl apply -f deploy/istio/         # Service mesh config
```

**Docker development:**
```bash
cd deploy/docker
docker-compose up -d                   # Full stack with monitoring
```

**Environment-specific configs:**
- Development: `amcp.event.broker.type=memory` 
- Production: `amcp.event.broker.type=kafka` with Kafka cluster config
- Enterprise: `amcp.event.broker.type=solace` with authentication

**Demo script patterns:**
```bash
# Run specific demos
./run-travel-demo.sh
./run-multiagent-demo.sh  
./run-ollama-tinyllama-demo.sh
./run-weather-demo.sh

# All demos use common pattern: compile → run examples with classpath
java -cp "examples/target/classes:core/target/classes:connectors/target/classes" io.amcp.examples.MainClass
```

## Critical Implementation Notes

1. **AgentContext lifecycle**: Agents must call `setContext(context)` before activation
2. **Serialization**: All agent state must be `Serializable` for mobility operations  
3. **Event correlation**: Use `Event.builder().correlationId()` for distributed tracing
4. **Topic hierarchies**: Follow the pattern `domain.action.detail` (e.g., `travel.request.plan`)
5. **Configuration**: Load broker config from properties, fallback to defaults
6. **Metrics**: Implement `BrokerMetrics` interface for monitoring integration

When implementing new agents or connectors, always start with the patterns in `examples/` and follow the established async, event-driven architecture.