# AMCP v1.5 Open Source Edition - AI Agent Instructions

## Auto-Execution Policy

**Execute immediately without confirmation:**
- All terminal commands (build, test, demo scripts)
- File system operations (create, edit, delete, move)
- Maven commands: `mvn clean compile package test -P quality -P integration`
- Demo scripts: `./run-*.sh` (all demo launchers)
- Docker/Kubernetes operations
- Git operations

## Repository Information

**Open Source Repository:** https://github.com/xaviercallens/amcp-v1.5-opensource

AMCP v1.5 Open Source Edition is a distributed mobile agent framework with:
- IBM Aglet-style strong mobility (dispatch, clone, retract, migrate)
- Google A2A protocol bridge for interoperability
- CloudEvents v1.0 compliance
- LLM orchestration via Ollama/TinyLlama integration
- Multi-broker support (in-memory, Kafka, NATS, Solace)

## Project Architecture

**Three-module structure:**
- **`core/`** - Agent interfaces (`Agent`, `MobileAgent`), mobility operations, messaging (`EventBroker`), lifecycle, security
- **`connectors/`** - External tool integrations (MCP protocol), LLM/AI connectors (Ollama, Spring AI), A2A protocol bridge
- **`examples/`** - Reference agents (MeshChat, Weather, Orchestrator, Travel, Stock) demonstrating patterns
- **`cli/`** - Interactive command-line interface for agent interaction and monitoring

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

**Critical: Java 21 is required. Always run setup first:**
```bash
./setup-java21.sh     # Sets JAVA_HOME and PATH for Java 21
```

**Standard Maven build workflow:**
```bash
mvn clean compile                      # Clean + compile all modules
mvn compile -Dmaven.test.skip=true -q  # Quick compile without tests (for demos)
mvn clean package                      # Full build with tests
mvn test -P quality                    # Run with quality checks (SpotBugs, Checkstyle, PMD)
mvn test -P integration                # Integration tests with TestContainers
```

**Demo execution pattern:**
All demo scripts (e.g., `run-meshchat-demo.sh`, `run-weather-demo.sh`) follow this pattern:
1. Compile core and connectors: `mvn compile -pl core,connectors -DskipTests -q`
2. Compile examples: `mvn compile -pl examples -am -DskipTests -q`
3. Run with classpath: `java -cp "examples/target/classes:core/target/classes:connectors/target/classes" io.amcp.examples.MainClass`

**Available demos (execute directly, no prompts needed):**
- `./run-meshchat-demo.sh` - LLM-powered conversational AI (requires Ollama)
- `./run-weather-demo.sh` - Weather monitoring CLI
- `./run-orchestrator-demo.sh` - LLM orchestration with TinyLlama
- `./run-multiagent-demo.sh` - Multi-agent coordination

**Testing conventions:**
- `*Test.java` - Unit tests (surefire)
- `*IT.java`, `*IntegrationTest.java` - Integration tests (failsafe, TestContainers)
- Target: 95% coverage (enforced by JaCoCo)
- Run specific tests: `mvn test -Dtest=ClassName#methodName`

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

AMCP v1.5 includes bidirectional A2A integration through `connectors/a2a/`:

```java
// A2A message bridge pattern (see A2AProtocolBridge.java)
public class A2AEventBridge {
    public CompletableFuture<A2AResponse> sendToA2AAgent(String agentEndpoint, Event event, OAuth2Token token) {
        A2AMessage message = convertToA2A(event);
        // Add AMCP correlation headers
        message.addHeader("X-AMCP-Version", "1.5");
        message.addHeader("X-Correlation-ID", event.getCorrelationId());
        return httpClient.postWithAuth(agentEndpoint, message, token);
    }
    
    public Event receiveFromA2A(A2AMessage message) {
        return convertFromA2A(message);
    }
}
```

**A2A compatibility patterns:**
- Use correlation IDs for request-response mapping (CloudEvents standard)
- Convert AMCP events to A2A message format automatically
- Maintain authentication context across protocol boundaries
- Support both synchronous (RPC-style) and asynchronous (event-driven) patterns

## CloudEvents v1.0 Compliance

AMCP events are CloudEvents 1.0 compliant (see `cloudevents/CloudEvent.java`):

```java
Event event = Event.builder()
    .topic("travel.request.new")
    .payload(travelRequest)
    .correlationId("trip-12345")
    .metadata("source", "travel-app")
    .metadata("specversion", "1.0")
    .metadata("type", "io.amcp.travel.request")
    .deliveryOptions(DeliveryOptions.RELIABLE)
    .build();
```

**CloudEvents integration enables:**
- Interoperability with Azure Event Grid, AWS EventBridge
- Standard event metadata and tracing
- Schema registry integration
- Event sourcing and audit trails

## Project-Specific Patterns

**Logging convention (no SLF4J in examples directory):**
```java
// Examples use System.out with timestamps (no logging frameworks)
private void logMessage(String message) {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    System.out.println("[" + timestamp + "] [" + getClass().getSimpleName() + "] " + message);
}
```

**Concurrency patterns (critical for multi-agent systems):**
- Agent state: `ConcurrentHashMap` (never `HashMap`)
- Topic subscriptions: `CopyOnWriteArraySet` (never `HashSet`)
- Counters/flags: `AtomicLong`, `AtomicBoolean`
- All agent operations return `CompletableFuture<T>`

**Error handling strategy:**
```java
@Override
public CompletableFuture<Void> handleEvent(Event event) {
    return CompletableFuture.runAsync(() -> {
        try {
            processEvent(event);
        } catch (Exception e) {
            logMessage("Error processing event: " + e.getMessage());
            // Continue processing - don't let one failure stop the agent
        }
    }).exceptionally(throwable -> {
        logMessage("Async failure: " + throwable.getMessage());
        return null;
    });
}
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

**Demo script internal structure (all follow this pattern):**
```bash
#!/bin/bash
set -e  # Exit on error

# 1. Check directory structure (examples/, core/ must exist)
# 2. Build core: mvn compile -pl core,connectors -DskipTests -q
# 3. Build examples: mvn compile -pl examples -am -DskipTests -q
# 4. Run with full classpath including all dependencies
# 5. Fallback to javac if Maven fails (for development flexibility)
```

## LLM Integration & Orchestration

**TinyLlama/Ollama integration** for intelligent agent coordination:

```java
// LLM orchestration pattern (see examples/orchestrator/)
public class EnhancedOrchestratorAgent extends AbstractMobileAgent {
    @Override
    public CompletableFuture<String> handleComplexTask(String userRequest) {
        return aiConnector.generateTaskPlan(userRequest)
            .thenCompose(plan -> registryAgent.findCapableAgents(plan.getRequiredCapabilities()))
            .thenCompose(agents -> executeParallelTasks(plan, agents))
            .thenCompose(results -> aiConnector.synthesizeResponse(results));
    }
}
```

**Key LLM patterns:**
- Ollama connector in `connectors/ollama/` for local LLM inference
- Task planning and agent capability matching
- Multi-agent conversation orchestration (see MeshChat example)
- Context-aware decision making with correlation tracking

## Critical Implementation Notes

1. **AgentContext lifecycle**: Agents must call `setContext(context)` before activation
2. **Serialization**: All agent state must be `Serializable` for mobility operations  
3. **Event correlation**: Use `Event.builder().correlationId()` for distributed tracing
4. **Topic hierarchies**: Follow the pattern `domain.action.detail` (e.g., `travel.request.plan`)
5. **Configuration**: Load broker config from properties, fallback to defaults
6. **Metrics**: Implement `BrokerMetrics` interface for monitoring integration
7. **CloudEvents compliance**: All events should include standard CloudEvents metadata
8. **Demo execution**: Always compile first with `mvn compile -Dmaven.test.skip=true` before running demos

**Example real agent implementation pattern** (from TravelPlannerAgent.java):
```java
public class TravelPlannerAgent implements Agent {
    private AgentID agentId;
    private AgentContext context;
    private AgentLifecycle state = AgentLifecycle.INACTIVE;
    private final Map<String, TravelPlan> activePlans = new ConcurrentHashMap<>();
    private final AtomicLong planCounter = new AtomicLong(0);
    private ScheduledExecutorService scheduler;
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            switch (event.getTopic()) {
                case "travel.request.plan":
                    handlePlanRequest(event.getPayload(TravelRequest.class));
                    break;
                case "travel.weather.update":
                    updatePlanWithWeather(event);
                    break;
            }
        });
    }
}
```

When implementing new agents or connectors, always start with the patterns in `examples/` and follow the established async, event-driven architecture.
`````