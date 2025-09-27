# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

AMCP v1.5 (Agent Mesh Communication Protocol) is an open-source agent-based communication framework for building distributed systems. It provides a CloudEvents 1.0 compliant event-driven architecture where autonomous agents communicate through topic-based pub/sub messaging.

**Technology Stack**: Java 8+, Maven, SLF4J, Jackson JSON, Apache HTTP Client

## Essential Commands

### Build & Test
```bash
# Build entire project (all modules: core, connectors, examples)
./scripts/build-all.sh

# Run all tests (unit, integration, examples)
./scripts/run-tests.sh

# Build only core module
mvn compile -pl core

# Run tests for specific module
mvn test -pl core
mvn test -pl examples

# Package all modules
mvn package

# Clean build artifacts
mvn clean
```

### Development Commands
```bash
# Run example applications
./scripts/run-greeting-agent.sh        # Basic agent demo
./scripts/run-travel-planner.sh        # Multi-agent system demo
./scripts/run-weather-system.sh        # Real-time data collection demo

# Run AMCP v1.5 comprehensive demo
mvn compile -pl core
javac -cp "core/target/classes" examples/src/main/java/io/amcp/examples/AMCP15Demo*.java -d examples/target/classes
java -cp "core/target/classes:examples/target/classes" io.amcp.examples.AMCP15DemoLauncher

# Deploy (if deployment.yaml is configured)
./scripts/deploy.sh
```

### Testing Individual Components
```bash
# Test specific test classes
mvn test -Dtest="*Test" -pl core
mvn test -Dtest="*IT" -pl core          # Integration tests
mvn test -Dtest="*ExampleTest" -pl examples

# Run with specific Maven profile
mvn compile -Pdev                       # Development profile
mvn package -Pprod                      # Production profile (skips tests)
mvn package -Prelease                   # Release profile (includes sources/javadocs)
```

## Architecture Overview

### Core Framework Structure
The system follows a layered architecture with three main modules:

**Core Module (`io.amcp.core`)**:
- `Agent` - Base interface for all computational entities
- `AgentContext` - Runtime environment providing lifecycle management and event routing  
- `Event` - CloudEvents 1.0 compliant message format with hierarchical topics
- `EventBroker` - Pluggable pub/sub messaging abstraction
- Implementation classes in `io.amcp.core.impl` and `io.amcp.messaging.impl`

**Event-Driven Communication Pattern**:
```java
// Agents publish events to hierarchical topics
publishEvent("weather.data.update", weatherData);

// Other agents subscribe to topic patterns
subscribe("weather.*");        // All weather events  
subscribe("weather.data.**");  // All weather.data subtopics
```

**Agent Lifecycle States**: 
`INACTIVE` → `ACTIVE` → `MIGRATING` → `DESTROYED`

### Key Design Patterns

**Agent Pattern**: All computational units implement `Agent` interface with `handleEvent()` for processing incoming messages and lifecycle callbacks (`onActivate()`, `onDeactivate()`, etc.).

**Request-Response Pattern**: Uses correlation IDs and `replyTo` metadata for async request/response workflows between agents.

**Migration Pattern**: Agents can be serialized and moved between contexts with state preservation using `dispatchAgent()` and `receiveAgent()`.

**CloudEvents Compliance**: Events follow CloudEvents 1.0 specification mapping:
- `messageId` → CloudEvents `id`
- `topic` → CloudEvents `type`  
- `sender` → CloudEvents `source`
- `payload` → CloudEvents `data`

## Development Guidelines

### Creating New Agents
```java
public class MyAgent implements Agent {
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            // Process event by topic
            switch (event.getTopic()) {
                case "my.topic":
                    handleMyTopic(event);
                    break;
            }
        });
    }
    
    @Override
    public void onActivate() {
        subscribe("my.topic.*");
        // Setup periodic tasks, initialize resources
    }
    
    @Override
    public void cleanup() {
        // Resource cleanup and shutdown
    }
}
```

### Event Publishing Best Practices
```java
// v1.5 convenience methods (preferred)
publishJsonEvent("user.created", userData);
sendMessage(targetAgentId, "notification", message);
broadcastEvent("system.maintenance", "Starting in 5 minutes");

// CloudEvents compliant publishing
Event event = Event.builder()
    .topic("user.login")
    .payload(loginData)
    .dataContentType("application/json")
    .dataSchema("https://schemas.company.io/user/v1")
    .traceId(correlationId)
    .build();
```

### Topic Naming Conventions
Use hierarchical dot notation:
- `{domain}.{action}.{entity}` (e.g., `user.created.profile`)
- `{service}.{event_type}` (e.g., `weather.data.update`)
- System events: `system.{event}` (e.g., `system.shutdown`)

### Testing Agents
```java
@Test
public void testAgentEventHandling() {
    // Use InMemoryEventBroker for testing
    AgentContext context = new SimpleAgentContext("test-context");
    Agent agent = new MyAgent();
    
    context.createAgent(agent.getClass());
    context.publishEvent(Event.create("test.topic", testData));
    
    // Verify agent behavior
}
```

## Module Dependencies

**Build Order**: core → connectors → examples

The root `pom.xml` manages dependency versions centrally. Core module has minimal dependencies (SLF4J, Jackson, HTTP Client). Examples module depends on core for agent implementations.

## Environment Configuration

### API Keys for Enhanced Examples
```bash
# Travel Planner example
export MAPS_API_KEY=your_google_maps_api_key
export WEATHER_API_KEY=your_weather_api_key

# Optional preferences
export TRAVEL_DEFAULT_MODE=driving
export TRAVEL_OPTIMIZE_FOR=time
```

### Development Profiles
- `dev` - Full testing enabled
- `prod` - Tests skipped, optimized build
- `release` - Includes source and javadoc artifacts

## CloudEvents Integration

AMCP v1.5 is fully CloudEvents 1.0 compliant. Events can be validated and exported:

```java
if (event.isCloudEventsCompliant()) {
    Map<String, Object> cloudEventMap = event.toCloudEventsMap();
    // Send to external CloudEvents-compliant systems
}
```

This enables integration with external event-driven systems and future multi-language SDK development.

<citations>
<document>
    <document_type>WARP_DOCUMENTATION</document_type>
    <document_id>getting-started/quickstart-guide/coding-in-warp</document_id>
</document>
</citations>