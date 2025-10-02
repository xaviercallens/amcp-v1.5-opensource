# 👨‍💻 AMCP Developer Guide

**Learn to build multi-agent systems with AMCP in 1 hour.**

## Table of Contents
- [Prerequisites](#prerequisites)
- [Tutorial 1: Your First Agent (15 min)](#tutorial-1-your-first-agent)
- [Tutorial 2: External API Integration (20 min)](#tutorial-2-external-api-integration)
- [Tutorial 3: Mobile Agent (15 min)](#tutorial-3-mobile-agent)
- [Tutorial 4: LLM-Powered Agent (20 min)](#tutorial-4-llm-powered-agent)
- [Common Patterns](#common-patterns)
- [Testing Your Agents](#testing-your-agents)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

**Before starting:**
```bash
# 1. Java 21+ installed
java -version  # Should show 21 or higher

# 2. Maven 3.8+
mvn -version

# 3. AMCP cloned and built
git clone https://github.com/xaviercallens/amcp-v1.5-opensource.git
cd amcp-v1.5-opensource
./setup-java21.sh
mvn clean install -DskipTests
```

**Recommended IDE:**
- IntelliJ IDEA (Community or Ultimate)
- VS Code with Java Extension Pack
- Eclipse with Java 21 support

---

## Tutorial 1: Your First Agent

**Goal:** Create an agent that responds to greetings.

### Step 1: Create the Agent Class

```java
package io.amcp.examples.tutorial;

import io.amcp.core.*;
import io.amcp.core.impl.AbstractMobileAgent;
import java.util.concurrent.CompletableFuture;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * GreetingAgent - Responds to greeting events
 */
public class GreetingAgent extends AbstractMobileAgent {
    
    private static final DateTimeFormatter TIME_FORMAT = 
        DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private int greetingCount = 0;
    
    public GreetingAgent(String agentId) {
        super(new AgentID(agentId));
    }
    
    @Override
    public void onActivate() {
        super.onActivate();
        
        // Subscribe to greeting topics
        subscribe("greeting.hello");
        subscribe("greeting.goodbye");
        
        logMessage("GreetingAgent activated and ready!");
        logMessage("Subscribed to: greeting.hello, greeting.goodbye");
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                switch (event.getTopic()) {
                    case "greeting.hello":
                        handleHello(event);
                        break;
                    case "greeting.goodbye":
                        handleGoodbye(event);
                        break;
                    default:
                        logMessage("Unknown topic: " + event.getTopic());
                }
            } catch (Exception e) {
                logMessage("Error handling event: " + e.getMessage());
            }
        });
    }
    
    private void handleHello(Event event) {
        String name = event.getPayload(String.class);
        greetingCount++;
        
        String response = String.format(
            "Hello, %s! 👋 You're my %dth visitor today!",
            name, greetingCount
        );
        
        logMessage(response);
        
        // Publish response event
        Event responseEvent = Event.builder()
            .topic("greeting.response")
            .payload(response)
            .correlationId(event.getCorrelationId())
            .build();
            
        context.publishEvent(responseEvent);
    }
    
    private void handleGoodbye(Event event) {
        String name = event.getPayload(String.class);
        
        String response = String.format(
            "Goodbye, %s! 👋 Thanks for stopping by!",
            name
        );
        
        logMessage(response);
    }
    
    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        System.out.println("[" + timestamp + "] [GreetingAgent] " + message);
    }
    
    @Override
    public void onDeactivate() {
        logMessage("GreetingAgent deactivating... handled " + greetingCount + " greetings");
        super.onDeactivate();
    }
}
```

### Step 2: Create a Test Runner

```java
package io.amcp.examples.tutorial;

import io.amcp.core.*;
import io.amcp.core.broker.InMemoryEventBroker;
import java.util.Scanner;
import java.util.UUID;

public class GreetingDemo {
    
    public static void main(String[] args) throws Exception {
        System.out.println("🚀 AMCP Tutorial 1: Your First Agent\n");
        
        // 1. Create an EventBroker (in-memory for simplicity)
        EventBroker broker = new InMemoryEventBroker();
        broker.start().get();
        
        // 2. Create an AgentContext
        AgentContext context = new AgentContext("demo-context", broker);
        
        // 3. Create and register the agent
        GreetingAgent agent = new GreetingAgent("greeting-agent-1");
        agent.setContext(context);  // CRITICAL!
        agent.activate().get();
        
        // 4. Subscribe to responses
        broker.subscribe("greeting.response", event -> {
            String response = event.getPayload(String.class);
            System.out.println("\n✉️  Response: " + response + "\n");
        });
        
        // 5. Interactive loop
        Scanner scanner = new Scanner(System.in);
        System.out.println("Commands:");
        System.out.println("  hello <name>   - Send a greeting");
        System.out.println("  goodbye <name> - Say goodbye");
        System.out.println("  quit           - Exit\n");
        
        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            
            if (input.equals("quit")) {
                break;
            }
            
            String[] parts = input.split(" ", 2);
            if (parts.length != 2) {
                System.out.println("Invalid command. Use: hello <name> or goodbye <name>");
                continue;
            }
            
            String command = parts[0];
            String name = parts[1];
            
            Event event = Event.builder()
                .topic("greeting." + command)
                .payload(name)
                .correlationId(UUID.randomUUID().toString())
                .build();
            
            broker.publish("greeting." + command, event);
        }
        
        // Cleanup
        agent.deactivate().get();
        broker.stop().get();
        System.out.println("\n👋 Goodbye!");
    }
}
```

### Step 3: Run It!

```bash
# From project root
cd examples/
mkdir -p src/main/java/io/amcp/examples/tutorial

# Save the above classes in the tutorial package
# Then compile and run:
mvn compile
mvn exec:java -Dexec.mainClass="io.amcp.examples.tutorial.GreetingDemo"
```

**Try it:**
```
> hello Alice
[12:34:56] [GreetingAgent] Hello, Alice! 👋 You're my 1th visitor today!
✉️  Response: Hello, Alice! 👋 You're my 1th visitor today!

> hello Bob
[12:34:58] [GreetingAgent] Hello, Bob! 👋 You're my 2th visitor today!
✉️  Response: Hello, Bob! 👋 You're my 2th visitor today!

> goodbye Alice
[12:35:00] [GreetingAgent] Goodbye, Alice! 👋 Thanks for stopping by!
```

### What You Learned

✅ **Creating an agent** - Extend `AbstractMobileAgent`  
✅ **Subscribing to topics** - `subscribe("greeting.hello")`  
✅ **Handling events** - `handleEvent(Event event)`  
✅ **Publishing responses** - `context.publishEvent(event)`  
✅ **Agent lifecycle** - `onActivate()`, `onDeactivate()`  

---

## Tutorial 2: External API Integration

**Goal:** Build an agent that fetches real data from an external API.

### Step 1: Add HTTP Client

```xml
<!-- Add to examples/pom.xml -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```

### Step 2: Create the Quote Agent

```java
package io.amcp.examples.tutorial;

import io.amcp.core.*;
import io.amcp.core.impl.AbstractMobileAgent;
import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.CompletableFuture;

/**
 * QuoteAgent - Fetches inspirational quotes from an external API
 */
public class QuoteAgent extends AbstractMobileAgent {
    
    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public QuoteAgent(String agentId) {
        super(new AgentID(agentId));
    }
    
    @Override
    public void onActivate() {
        super.onActivate();
        subscribe("quote.request");
        logMessage("QuoteAgent activated - Ready to fetch quotes!");
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            if ("quote.request".equals(event.getTopic())) {
                handleQuoteRequest(event);
            }
        });
    }
    
    private void handleQuoteRequest(Event event) {
        logMessage("Fetching quote from API...");
        
        try {
            // Call external API (example: quotable.io)
            Request request = new Request.Builder()
                .url("https://api.quotable.io/random")
                .build();
            
            Response response = httpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new Exception("API call failed: " + response.code());
            }
            
            String jsonData = response.body().string();
            JsonNode json = objectMapper.readTree(jsonData);
            
            String quote = json.get("content").asText();
            String author = json.get("author").asText();
            
            String result = String.format("💭 \"%s\" — %s", quote, author);
            logMessage("Retrieved quote: " + result);
            
            // Publish response
            Event responseEvent = Event.builder()
                .topic("quote.response")
                .payload(result)
                .correlationId(event.getCorrelationId())
                .build();
            
            context.publishEvent(responseEvent);
            
        } catch (Exception e) {
            logMessage("Error fetching quote: " + e.getMessage());
            
            Event errorEvent = Event.builder()
                .topic("quote.error")
                .payload("Failed to fetch quote: " + e.getMessage())
                .correlationId(event.getCorrelationId())
                .build();
            
            context.publishEvent(errorEvent);
        }
    }
    
    private void logMessage(String message) {
        System.out.println("[QuoteAgent] " + message);
    }
    
    @Override
    public void onDeactivate() {
        httpClient.dispatcher().executorService().shutdown();
        super.onDeactivate();
    }
}
```

### Step 3: Test the Quote Agent

```java
public class QuoteDemo {
    public static void main(String[] args) throws Exception {
        System.out.println("🚀 AMCP Tutorial 2: External API Integration\n");
        
        EventBroker broker = new InMemoryEventBroker();
        broker.start().get();
        
        AgentContext context = new AgentContext("demo-context", broker);
        
        QuoteAgent agent = new QuoteAgent("quote-agent-1");
        agent.setContext(context);
        agent.activate().get();
        
        // Subscribe to responses
        broker.subscribe("quote.response", event -> {
            System.out.println("\n" + event.getPayload(String.class) + "\n");
        });
        
        // Request quotes
        Scanner scanner = new Scanner(System.in);
        System.out.println("Press Enter to get a quote, or type 'quit' to exit\n");
        
        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine();
            
            if ("quit".equals(input)) break;
            
            Event request = Event.builder()
                .topic("quote.request")
                .correlationId(UUID.randomUUID().toString())
                .build();
            
            broker.publish("quote.request", request);
        }
        
        agent.deactivate().get();
        broker.stop().get();
    }
}
```

### What You Learned

✅ **External API calls** - Using OkHttpClient  
✅ **JSON parsing** - Jackson ObjectMapper  
✅ **Error handling** - Publishing error events  
✅ **Resource cleanup** - Proper shutdown in `onDeactivate()`  

---

## Tutorial 3: Mobile Agent

**Goal:** Create an agent that migrates between contexts.

### Step 1: Create a Counter Agent

```java
package io.amcp.examples.tutorial;

import io.amcp.core.*;
import io.amcp.core.impl.AbstractMobileAgent;
import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CounterAgent - A mobile agent that counts events and can migrate
 */
public class CounterAgent extends AbstractMobileAgent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private AtomicInteger eventCount = new AtomicInteger(0);
    private String currentContext;
    
    public CounterAgent(String agentId) {
        super(new AgentID(agentId));
    }
    
    @Override
    public void onActivate() {
        super.onActivate();
        currentContext = context.getContextId();
        subscribe("counter.increment");
        subscribe("counter.status");
        subscribe("counter.migrate");
        
        logMessage("Activated in context: " + currentContext);
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            switch (event.getTopic()) {
                case "counter.increment":
                    handleIncrement(event);
                    break;
                case "counter.status":
                    handleStatus(event);
                    break;
                case "counter.migrate":
                    handleMigrate(event);
                    break;
            }
        });
    }
    
    private void handleIncrement(Event event) {
        int newCount = eventCount.incrementAndGet();
        logMessage(String.format("Incremented to %d (context: %s)", 
            newCount, currentContext));
    }
    
    private void handleStatus(Event event) {
        String status = String.format(
            "Counter: %d | Context: %s",
            eventCount.get(),
            currentContext
        );
        logMessage(status);
        
        Event response = Event.builder()
            .topic("counter.status.response")
            .payload(status)
            .correlationId(event.getCorrelationId())
            .build();
        
        context.publishEvent(response);
    }
    
    private void handleMigrate(Event event) {
        String targetContext = event.getPayload(String.class);
        logMessage("Migration requested to: " + targetContext);
        
        try {
            dispatch(targetContext).get();
            logMessage("Migration complete!");
        } catch (Exception e) {
            logMessage("Migration failed: " + e.getMessage());
        }
    }
    
    @Override
    public void onBeforeMigration(String destination) {
        logMessage(String.format(
            "Preparing to migrate from %s to %s (count: %d)",
            currentContext, destination, eventCount.get()
        ));
    }
    
    @Override
    public void onAfterMigration(String source) {
        currentContext = context.getContextId();
        logMessage(String.format(
            "Successfully migrated from %s to %s (count preserved: %d)",
            source, currentContext, eventCount.get()
        ));
    }
    
    @Override
    public byte[] saveState() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeInt(eventCount.get());
            oos.writeObject(currentContext);
            oos.close();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save state", e);
        }
    }
    
    @Override
    public void loadState(byte[] state) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(state);
            ObjectInputStream ois = new ObjectInputStream(bis);
            int count = ois.readInt();
            String context = (String) ois.readObject();
            ois.close();
            
            eventCount.set(count);
            currentContext = context;
            
            logMessage("State loaded: count=" + count + ", context=" + context);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load state", e);
        }
    }
    
    private void logMessage(String message) {
        System.out.println("[CounterAgent] " + message);
    }
}
```

### Step 2: Test Migration

```java
public class MigrationDemo {
    public static void main(String[] args) throws Exception {
        System.out.println("🚀 AMCP Tutorial 3: Agent Mobility\n");
        
        // Create two contexts
        EventBroker broker = new InMemoryEventBroker();
        broker.start().get();
        
        AgentContext contextA = new AgentContext("context-A", broker);
        AgentContext contextB = new AgentContext("context-B", broker);
        
        // Create agent in context A
        CounterAgent agent = new CounterAgent("counter-1");
        agent.setContext(contextA);
        agent.activate().get();
        
        // Increment a few times
        for (int i = 0; i < 5; i++) {
            Event event = Event.builder()
                .topic("counter.increment")
                .build();
            broker.publish("counter.increment", event);
            Thread.sleep(100);
        }
        
        // Check status
        Event statusReq = Event.builder()
            .topic("counter.status")
            .build();
        broker.publish("counter.status", statusReq);
        Thread.sleep(500);
        
        // Migrate to context B
        System.out.println("\n🚀 Migrating agent to context-B...\n");
        Event migrateEvent = Event.builder()
            .topic("counter.migrate")
            .payload("context-B")
            .build();
        broker.publish("counter.migrate", migrateEvent);
        Thread.sleep(1000);
        
        // Increment again
        for (int i = 0; i < 3; i++) {
            Event event = Event.builder()
                .topic("counter.increment")
                .build();
            broker.publish("counter.increment", event);
            Thread.sleep(100);
        }
        
        // Final status
        broker.publish("counter.status", statusReq);
        Thread.sleep(500);
        
        // Cleanup
        broker.stop().get();
    }
}
```

### What You Learned

✅ **Agent mobility** - `dispatch(destinationContext)`  
✅ **State serialization** - `saveState()` and `loadState()`  
✅ **Migration hooks** - `onBeforeMigration()`, `onAfterMigration()`  
✅ **Context switching** - Agent works in multiple contexts  

---

## Tutorial 4: LLM-Powered Agent

**Goal:** Create an agent that uses LLM for intelligent responses.

*(Full code example with Ollama/TinyLlama integration - see `examples/orchestrator/` for complete implementation)*

**Key Concepts:**
```java
@Component
public class SmartAgent extends AbstractMobileAgent {
    
    @Autowired
    private OllamaAIConnector llm;
    
    public CompletableFuture<String> processRequest(String userQuery) {
        return llm.generateResponse(userQuery)
            .thenApply(response -> enhanceResponse(response));
    }
}
```

---

## Common Patterns

### Pattern 1: Request-Response

```java
// Publisher
String correlationId = UUID.randomUUID().toString();
Event request = Event.builder()
    .topic("service.request")
    .payload(data)
    .correlationId(correlationId)
    .build();

CompletableFuture<Event> response = awaitResponse(correlationId);
broker.publish("service.request", request);
```

### Pattern 2: Pub/Sub Broadcast

```java
// Multiple subscribers to same topic
agent1.subscribe("notifications.**");
agent2.subscribe("notifications.**");
agent3.subscribe("notifications.**");

// One publish, all receive
broker.publish("notifications.alert", event);
```

### Pattern 3: Topic Hierarchy

```java
// Organize topics hierarchically
"orders.new"           // New orders
"orders.fulfilled"     // Completed orders
"orders.cancelled"     // Cancelled orders

// Subscribe to all
subscribe("orders.**");
```

---

## Testing Your Agents

### Unit Testing

```java
@Test
public void testGreetingAgent() throws Exception {
    // Arrange
    EventBroker broker = new InMemoryEventBroker();
    broker.start().get();
    
    AgentContext context = new AgentContext("test-context", broker);
    GreetingAgent agent = new GreetingAgent("test-agent");
    agent.setContext(context);
    agent.activate().get();
    
    CompletableFuture<Event> responseFuture = new CompletableFuture<>();
    broker.subscribe("greeting.response", responseFuture::complete);
    
    // Act
    Event request = Event.builder()
        .topic("greeting.hello")
        .payload("TestUser")
        .build();
    broker.publish("greeting.hello", request);
    
    // Assert
    Event response = responseFuture.get(5, TimeUnit.SECONDS);
    String payload = response.getPayload(String.class);
    assertTrue(payload.contains("Hello, TestUser"));
    
    // Cleanup
    agent.deactivate().get();
    broker.stop().get();
}
```

---

## Troubleshooting

### Issue: "Agent not receiving events"

**Check:**
1. Did you call `agent.setContext(context)` before `activate()`?
2. Is the agent subscribed to the correct topic?
3. Is the broker started?
4. Are you publishing to the right topic?

### Issue: "NullPointerException in handleEvent"

**Solution:** Always null-check payloads:
```java
String data = event.getPayload(String.class);
if (data == null) {
    logMessage("Received null payload");
    return;
}
```

### Issue: "Agent migration fails"

**Check:**
1. Is agent state `Serializable`?
2. Are all fields either serializable or `transient`?
3. Is destination context running?

---

## Next Steps

1. **Explore Examples** - Check out `examples/` directory
2. **Read Architecture** - Understand the design in [ARCHITECTURE.md](ARCHITECTURE.md)
3. **Join Community** - Ask questions in [GitHub Discussions](https://github.com/xaviercallens/amcp-v1.5-opensource/discussions)
4. **Contribute** - See [CONTRIBUTING.md](../CONTRIBUTING.md)

---

**Happy Coding! 🚀**

*Questions? Open an issue or discussion on GitHub!*
