# MeshChat Architecture Guide

## System Overview

The MeshChat Agent System represents a sophisticated implementation of the Agent Mesh Communication Protocol (AMCP) v1.5 Enterprise Edition, designed to provide intelligent conversational AI with multi-agent orchestration capabilities.

```
┌─────────────────────────────────────────────────────────────────┐
│                     MeshChat Ecosystem                         │
│                                                                 │
│  ┌─────────────────┐    ┌──────────────────┐                  │
│  │   Human User    │◄──►│   MeshChatCLI    │                  │
│  └─────────────────┘    └──────────────────┘                  │
│                                   │                            │
│                                   ▼                            │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              MeshChatAgent                              │   │
│  │         (Conversation Gateway)                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                   │                            │
│                                   ▼                            │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                AMCP Event Mesh                         │   │
│  │                                                         │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐   │   │
│  │  │Enhanced     │ │Travel       │ │  StockAgent     │   │   │
│  │  │Orchestrator │ │PlannerAgent │ │                 │   │   │
│  │  └─────────────┘ └─────────────┘ └─────────────────┘   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                   │                            │
│                                   ▼                            │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │             Support Systems                             │   │
│  │                                                         │   │
│  │ ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐    │   │
│  │ │AgentRegistry│ │Conversation │ │  Discovery      │    │   │
│  │ │             │ │MemorySystem │ │  Service        │    │   │
│  │ └─────────────┘ └─────────────┘ └─────────────────┘    │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Architectural Principles

### 1. Event-Driven Architecture

The entire system is built on AMCP's event-driven foundation:

```java
// Core event flow pattern
User Input → MeshChatAgent → Event Publication → Agent Processing → Response Synthesis
```

**Key Benefits:**
- **Loose Coupling**: Agents communicate through events, not direct calls
- **Scalability**: Easy to add new agents without modifying existing ones
- **Reliability**: Built-in retry and error handling through event system
- **Observability**: All interactions are traceable through event logs

### 2. Agent Specialization

Each agent is designed for specific domain expertise:

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  MeshChatAgent  │    │TravelPlanner    │    │   StockAgent    │
│                 │    │     Agent       │    │                 │
│  • Gateway      │    │ • Destinations  │    │ • Stock Data    │
│  • Orchestration│    │ • Trip Planning │    │ • Market Analysis│
│  • Context Mgmt │    │ • Budget Est.   │    │ • Investment    │
│  • Response Syn │    │ • Cultural Tips │    │ • Crypto Info   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### 3. Intelligent Orchestration

The EnhancedOrchestratorAgent provides LLM-powered task routing:

```java
// Orchestration flow
Task Analysis → Intent Classification → Agent Selection → Coordination → Response
```

### 4. Persistent Memory

ConversationMemorySystem maintains context across sessions:

```java
// Memory architecture
Session Creation → Message Storage → Context Extraction → Response Enhancement
```

---

## Core Components Deep Dive

### MeshChatAgent - The Conversation Gateway

#### Responsibilities
1. **Human Interface**: Receives and validates user input
2. **Intent Analysis**: Classifies messages for appropriate routing
3. **Session Management**: Maintains conversation state and history
4. **Response Synthesis**: Combines specialist responses into coherent answers
5. **Error Handling**: Provides fallback responses for edge cases

#### Architecture Pattern
```java
public class MeshChatAgent extends AbstractMobileAgent {
    // Core chat functionality
    private final Map<String, ChatSession> activeSessions = new ConcurrentHashMap<>();
    
    // Integration points
    private ConversationMemorySystem memorySystem;
    private AgentRegistry agentRegistry;
    
    // Message processing pipeline
    processMessage() → classifyTopic() → routeToSpecialist() → synthesizeResponse()
}
```

#### Event Flow
```
Incoming Message Event:
  "meshchat.user.message" {
    sessionId: "session-123",
    message: "Plan a trip to Tokyo",
    userId: "user-456"
  }

Outgoing Orchestration Event:
  "orchestrator.task.request" {
    task: "Plan a trip to Tokyo",
    context: { sessionId: "session-123", domain: "travel" },
    priority: "normal"
  }
```

### EnhancedOrchestratorAgent - The Intelligent Router

#### Key Enhancements Over Basic Orchestrator
1. **Agent-Aware Prompting**: Uses knowledge of available agents in LLM prompts
2. **Keyword-Based Routing**: Implements fast keyword detection before LLM analysis
3. **Context Integration**: Incorporates conversation history into task planning
4. **Multi-Agent Coordination**: Handles complex tasks requiring multiple specialists

#### Enhanced Prompt Engineering
```java
public String buildPlanningPrompt(String task, List<String> availableAgents, String context) {
    return """
        You are an intelligent task orchestrator with access to these specialist agents:
        
        🧳 TravelPlannerAgent: Expert in trip planning, destinations, budgets, cultural advice
           - Keywords: trip, travel, hotel, flight, destination, vacation, itinerary
           - Capabilities: 11 major destinations, budget estimation, cultural tips
        
        📈 StockAgent: Expert in financial markets, investment analysis, stock data
           - Keywords: stock, price, market, investment, portfolio, crypto, financial
           - Capabilities: Real-time quotes, analysis, recommendations, crypto insights
        
        🎯 Multi-Agent Coordination: For complex tasks requiring multiple specialists
           - Keywords: "plan and research", "coordinate", "both", "also research"
           - Example: "Plan Tokyo trip and research Japanese stocks"
        
        Previous Context: %s
        
        Current Task: "%s"
        
        INSTRUCTIONS:
        1. Analyze the task and determine the best approach
        2. Route to appropriate specialist(s) based on keywords and context
        3. For multi-agent tasks, specify coordination strategy
        4. Provide clear, actionable response
        
        Choose the most appropriate routing:
        """.formatted(context, task);
}
```

#### Routing Decision Tree
```
Task Input
    │
    ├── Contains Travel Keywords? → Route to TravelPlannerAgent
    │   └── Generate travel-specific response
    │
    ├── Contains Stock Keywords? → Route to StockAgent  
    │   └── Generate financial analysis response
    │
    ├── Contains Multi-Agent Keywords? → Coordinate Multiple Agents
    │   └── Plan multi-agent workflow
    │
    └── General Query → Use LLM for conversational response
        └── Generate context-aware general response
```

### TravelPlannerAgent - The Travel Specialist

#### Domain Model
```java
public class TravelDestination {
    // Core destination data
    private String name;
    private String country;
    private String description;
    
    // Rich metadata
    private List<String> highlights;           // Top attractions
    private List<String> activities;           // Things to do
    private String bestTimeToVisit;           // Seasonal advice
    private double averageDailyBudget;        // Cost estimates
    private String culturalTips;              // Local customs
    
    // Advanced features
    public String planItinerary(int days, double budget) { ... }
    public double estimateCosts(String travelStyle) { ... }
    public boolean matchesInterests(List<String> interests) { ... }
}
```

#### Destination Database (11 Major Cities)
```java
// Sample destination configuration
tokyo = new TravelDestination("Tokyo", "Japan",
    "A fascinating blend of ultra-modern technology and traditional culture...");
tokyo.setHighlights(List.of(
    "Senso-ji Temple and Asakusa district",
    "Tokyo Skytree and modern architecture", 
    "Shibuya crossing and youth culture",
    "Traditional gardens and imperial palace"
));
tokyo.setActivities(List.of(
    "Sushi making class",
    "Traditional tea ceremony",
    "Robot restaurant show",
    "Cherry blossom viewing (seasonal)"
));
tokyo.setAverageDailyBudget(120.0); // Mid-range estimate
tokyo.setCulturalTips("Always bow when greeting, remove shoes when entering homes...");
```

#### Trip Planning Algorithm
```java
public String planTrip(String destination, int days, double budget) {
    TravelDestination dest = getDestination(destination);
    if (dest == null) return "Destination not found";
    
    // Budget allocation strategy
    double dailyBudget = budget / days;
    String travelStyle = determineTravelStyle(dailyBudget, dest.getAverageDailyBudget());
    
    // Day-by-day itinerary generation
    List<String> itinerary = generateItinerary(dest, days, travelStyle);
    
    // Cost breakdown
    CostBreakdown costs = calculateCosts(dest, days, travelStyle, budget);
    
    return formatTripPlan(dest, days, budget, itinerary, costs);
}
```

### StockAgent - The Financial Specialist

#### Market Simulation Architecture
```java
public class StockData {
    // Real-time simulation
    private String symbol;
    private String companyName;
    private double currentPrice;
    private double changePercent;      // Daily change
    private long volume;
    private double marketCap;
    
    // Analysis components
    private String technicalAnalysis;  // Price trends, moving averages
    private String fundamentalAnalysis; // P/E ratio, growth prospects
    private String recommendation;     // Buy/Hold/Sell with reasoning
    private String riskAssessment;     // Volatility and risk factors
}
```

#### Market Data Generation
```java
// Realistic market simulation
private void updateMarketData() {
    for (StockData stock : stockDatabase.values()) {
        // Simulate realistic price movements
        double volatility = getVolatilityForStock(stock.getSymbol());
        double marketTrend = getCurrentMarketTrend();
        double sectorTrend = getSectorTrend(stock.getSector());
        
        // Apply compound factors
        double priceChange = calculatePriceChange(volatility, marketTrend, sectorTrend);
        stock.updatePrice(priceChange);
        
        // Generate analysis based on new data
        stock.setAnalysis(generateAnalysis(stock));
        stock.setRecommendation(generateRecommendation(stock));
    }
}
```

#### Investment Analysis Engine
```java
public String getInvestmentRecommendation(String symbol, String riskTolerance) {
    StockData stock = getStockQuote(symbol);
    
    // Multi-factor analysis
    TechnicalAnalysis technical = analyzeTechnicalIndicators(stock);
    FundamentalAnalysis fundamental = analyzeFundamentals(stock);
    RiskAnalysis risk = assessRisk(stock, riskTolerance);
    
    // Generate comprehensive recommendation
    return synthesizeRecommendation(technical, fundamental, risk, riskTolerance);
}
```

---

## System Integration Patterns

### Event-Driven Communication

#### Event Schema Standards
```java
// All events follow CloudEvents 1.0 specification
public class AMCPEvent {
    private String id;           // Unique event identifier
    private String source;       // Publishing agent
    private String type;         // Event type (topic)
    private String subject;      // Optional event subject
    private Object data;         // Event payload
    private Map<String, Object> extensions; // Custom metadata
    
    // AMCP-specific extensions
    private String correlationId;  // For request-response tracking
    private String sessionId;     // For conversation context
    private long timestamp;       // Event creation time
    private DeliveryOptions deliveryOptions; // Reliability settings
}
```

#### Topic Hierarchy Design
```
meshchat.                    // MeshChat namespace
├── user.message            // User input events
├── session.start           // Session lifecycle
├── session.end
└── response.generated      // Response events

orchestrator.               // Orchestration namespace  
├── task.request           // Task orchestration
├── task.assigned
├── task.completed
└── coordination.required   // Multi-agent coordination

travel.                    // Travel domain
├── request.plan          // Trip planning requests
├── request.destination   // Destination info
├── response.plan         // Trip plan responses
└── agent.response        // General travel responses

stock.                     // Financial domain
├── request.quote         // Stock quote requests
├── request.analysis      // Analysis requests
├── response.quote        // Quote responses
└── market.update         // Market data updates

system.                    // System events
├── agent.registered      // Agent lifecycle
├── agent.deregistered
├── health.check
└── metrics.update        // Performance metrics
```

### Multi-Agent Coordination Patterns

#### Sequential Coordination
```java
// For dependent tasks (e.g., plan trip → book accommodation)
public CompletableFuture<String> coordinateSequentialTasks(List<Task> tasks) {
    return tasks.stream()
        .reduce(CompletableFuture.completedFuture(""),
                (future, task) -> future.thenCompose(result -> 
                    executeTask(task, result)),
                (f1, f2) -> f1.thenCombine(f2, String::concat));
}
```

#### Parallel Coordination
```java
// For independent tasks (e.g., research stocks + plan trip)
public CompletableFuture<String> coordinateParallelTasks(List<Task> tasks) {
    List<CompletableFuture<String>> futures = tasks.stream()
        .map(this::executeTaskAsync)
        .toList();
        
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(v -> futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.joining("\n\n")));
}
```

#### Hierarchical Coordination
```java
// For complex tasks with subtasks
public CompletableFuture<String> coordinateHierarchicalTask(ComplexTask task) {
    // Break down into subtasks
    List<SubTask> subTasks = decomposeTask(task);
    
    // Execute subtasks with dependencies
    return executeWithDependencies(subTasks)
        .thenApply(results -> synthesizeResults(results, task.getContext()));
}
```

---

## Memory and State Management

### Conversation Memory Architecture

#### Session Lifecycle Management
```java
public class ConversationSession {
    // Session metadata
    private String sessionId;
    private String userId;
    private long startTime;
    private long lastActivity;
    private boolean isActive;
    
    // Message storage
    private List<ConversationMessage> messages;
    private String contextSummary;
    
    // State management
    private Map<String, Object> sessionState;
    private Set<String> mentionedTopics;
    private List<String> previousQueries;
    
    // Memory optimization
    public void summarizeAndTruncate(int keepRecentCount) {
        if (messages.size() > keepRecentCount * 2) {
            List<ConversationMessage> recent = messages.subList(
                messages.size() - keepRecentCount, messages.size());
            List<ConversationMessage> toSummarize = messages.subList(
                0, messages.size() - keepRecentCount);
            
            this.contextSummary = generateSummary(toSummarize);
            this.messages = new ArrayList<>(recent);
        }
    }
}
```

#### Context Extraction Strategy
```java
public String extractContext(String sessionId, int messageCount) {
    ConversationSession session = getSession(sessionId);
    if (session == null) return "";
    
    List<ConversationMessage> recentMessages = session.getMessages()
        .stream()
        .skip(Math.max(0, session.getMessages().size() - messageCount))
        .toList();
    
    // Extract key information
    Set<String> topics = extractTopics(recentMessages);
    Set<String> entities = extractEntities(recentMessages);
    String sentiment = analyzeSentiment(recentMessages);
    
    return buildContextSummary(topics, entities, sentiment, session.getContextSummary());
}
```

### Agent State Management

#### Agent Registry Design
```java
public class AgentRegistry {
    // Agent metadata storage
    private final Map<String, AgentMetadata> registeredAgents = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> capabilityIndex = new ConcurrentHashMap<>();
    private final Map<String, HealthStatus> agentHealth = new ConcurrentHashMap<>();
    
    // Performance tracking
    private final Map<String, AgentPerformanceMetrics> performanceMetrics = new ConcurrentHashMap<>();
    
    public void registerAgent(String agentId, List<String> capabilities, Map<String, Object> metadata) {
        AgentMetadata agentMeta = new AgentMetadata(agentId, capabilities, metadata);
        registeredAgents.put(agentId, agentMeta);
        
        // Update capability index
        capabilities.forEach(cap -> 
            capabilityIndex.computeIfAbsent(cap, k -> ConcurrentHashMap.newKeySet())
                .add(agentId));
        
        // Initialize health monitoring
        agentHealth.put(agentId, new HealthStatus(true, System.currentTimeMillis()));
        
        publishEvent("system.agent.registered", agentMeta);
    }
}
```

#### Health Monitoring
```java
@Scheduled(fixedDelay = 30000) // Every 30 seconds
public void performHealthChecks() {
    registeredAgents.keySet().parallelStream()
        .forEach(agentId -> {
            HealthStatus status = checkAgentHealth(agentId);
            agentHealth.put(agentId, status);
            
            if (!status.isHealthy()) {
                publishEvent("system.agent.unhealthy", Map.of(
                    "agentId", agentId,
                    "lastSeen", status.getLastHealthy(),
                    "status", status
                ));
            }
        });
}
```

---

## Security and Reliability

### Security Architecture

#### Authentication and Authorization
```java
public class AgentSecurityManager {
    // Agent authentication
    public boolean authenticateAgent(String agentId, String token) {
        return tokenValidator.validate(token) && 
               authorizedAgents.contains(agentId);
    }
    
    // Topic-level authorization
    public boolean canSubscribeToTopic(String agentId, String topic) {
        Set<String> allowedTopics = agentPermissions.get(agentId);
        return allowedTopics != null && 
               allowedTopics.stream().anyMatch(pattern -> matchesPattern(topic, pattern));
    }
    
    // Message filtering
    public boolean canAccessMessage(String agentId, Event event) {
        return hasTopicAccess(agentId, event.getTopic()) &&
               hasDataAccess(agentId, event.getData());
    }
}
```

#### Secure Communication
```java
// End-to-end encryption for sensitive data
public class SecureEventPublisher {
    public void publishSecureEvent(String topic, Object data, String... authorizedAgents) {
        // Encrypt payload
        byte[] encryptedData = encryptionService.encrypt(
            serializer.serialize(data), 
            Arrays.asList(authorizedAgents)
        );
        
        // Publish with encryption metadata
        publishEvent(Event.builder()
            .topic(topic)
            .payload(encryptedData)
            .metadata("encrypted", true)
            .metadata("authorizedAgents", Arrays.asList(authorizedAgents))
            .build());
    }
}
```

### Reliability Patterns

#### Circuit Breaker for Agent Communication
```java
public class AgentCircuitBreaker {
    private final Map<String, CircuitBreakerState> agentStates = new ConcurrentHashMap<>();
    
    public CompletableFuture<String> callAgent(String agentId, String request) {
        CircuitBreakerState state = agentStates.computeIfAbsent(agentId, 
            k -> new CircuitBreakerState());
        
        if (state.isOpen()) {
            return CompletableFuture.completedFuture(
                "Agent temporarily unavailable: " + agentId);
        }
        
        return doCallAgent(agentId, request)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    state.recordFailure();
                } else {
                    state.recordSuccess();
                }
            });
    }
}
```

#### Retry with Exponential Backoff
```java
public CompletableFuture<String> callWithRetry(String agentId, String request, int maxRetries) {
    return CompletableFuture.supplyAsync(() -> {
        Exception lastException = null;
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return callAgentSync(agentId, request);
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries - 1) {
                    try {
                        Thread.sleep(Math.min(1000 * (1L << attempt), 30000)); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry", ie);
                    }
                }
            }
        }
        
        throw new RuntimeException("All retry attempts failed", lastException);
    });
}
```

---

## Performance Optimization

### Caching Strategies

#### Multi-Level Caching
```java
public class MeshChatCacheManager {
    // L1: In-memory cache for frequently accessed data
    private final Cache<String, String> responseCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(15, TimeUnit.MINUTES)
        .build();
    
    // L2: Conversation context cache
    private final Cache<String, ConversationContext> contextCache = Caffeine.newBuilder()
        .maximumSize(500)
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build();
    
    // L3: Agent capability cache
    private final Cache<String, List<String>> capabilityCache = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build();
    
    public String getCachedResponse(String query, String context) {
        String cacheKey = generateCacheKey(query, context);
        return responseCache.getIfPresent(cacheKey);
    }
}
```

#### Intelligent Cache Invalidation
```java
@EventHandler("system.agent.updated")
public void handleAgentUpdate(Event event) {
    String agentId = event.getPayload(String.class);
    
    // Invalidate related caches
    capabilityCache.invalidate(agentId);
    responseCache.invalidateAll(); // Conservative approach
    
    // Warm up cache with new data
    CompletableFuture.runAsync(() -> {
        preloadAgentCapabilities(agentId);
    });
}
```

### Asynchronous Processing

#### Event Processing Pipeline
```java
public class EventProcessingPipeline {
    private final ExecutorService eventProcessor = ForkJoinPool.commonPool();
    private final CompletionService<Void> completionService = 
        new ExecutorCompletionService<>(eventProcessor);
    
    public void processEventAsync(Event event) {
        completionService.submit(() -> {
            try {
                // Stage 1: Validation
                validateEvent(event);
                
                // Stage 2: Routing
                List<String> targetAgents = routeEvent(event);
                
                // Stage 3: Parallel processing
                List<CompletableFuture<Void>> futures = targetAgents.stream()
                    .map(agentId -> deliverToAgent(agentId, event))
                    .toList();
                
                // Stage 4: Coordination
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .join();
                
                return null;
            } catch (Exception e) {
                handleProcessingError(event, e);
                return null;
            }
        });
    }
}
```

### Memory Optimization

#### Efficient Data Structures
```java
public class OptimizedConversationStorage {
    // Use memory-efficient collections
    private final TLongObjectMap<ConversationSession> sessionsByTime = 
        new TLongObjectHashMap<>();
    private final TObjectIntMap<String> topicFrequency = 
        new TObjectIntHashMap<>();
    
    // Compression for long-term storage
    public void archiveOldSessions(long cutoffTime) {
        sessionsByTime.forEachEntry((time, session) -> {
            if (time < cutoffTime) {
                byte[] compressed = compressionService.compress(session);
                archiveStorage.store(session.getSessionId(), compressed);
                sessionsByTime.remove(time);
            }
            return true;
        });
    }
}
```

---

## Monitoring and Observability

### Metrics Collection

#### Key Performance Indicators
```java
@Component
public class MeshChatMetrics {
    // Response time metrics
    private final Timer responseTimer = Timer.builder("meshchat.response.time")
        .description("Response time for MeshChat queries")
        .register(meterRegistry);
    
    // Throughput metrics
    private final Counter messageCounter = Counter.builder("meshchat.messages.total")
        .description("Total messages processed")
        .register(meterRegistry);
    
    // Error metrics
    private final Counter errorCounter = Counter.builder("meshchat.errors.total")
        .description("Total errors encountered")
        .tag("type", "unknown")
        .register(meterRegistry);
    
    // Agent utilization
    private final Gauge agentUtilization = Gauge.builder("meshchat.agents.utilization")
        .description("Agent utilization percentage")
        .register(meterRegistry, this, MeshChatMetrics::calculateUtilization);
    
    // Queue depth monitoring
    private final Gauge queueDepth = Gauge.builder("meshchat.queue.depth")
        .description("Event queue depth")
        .register(meterRegistry, eventQueue, Queue::size);
}
```

#### Distributed Tracing
```java
public class TracingIntegration {
    @Autowired
    private Tracer tracer;
    
    public CompletableFuture<String> processWithTracing(String message, String sessionId) {
        Span span = tracer.nextSpan()
            .name("meshchat.process.message")
            .tag("session.id", sessionId)
            .tag("message.length", String.valueOf(message.length()))
            .start();
        
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            return processMessage(message, sessionId)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        span.tag("error", throwable.getMessage());
                    }
                    span.tag("response.length", String.valueOf(result.length()));
                    span.end();
                });
        }
    }
}
```

### Health Checks

#### Comprehensive Health Assessment
```java
@Component
public class MeshChatHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        Health.Builder status = Health.up();
        
        // Check agent availability
        int activeAgents = agentRegistry.getActiveAgents().size();
        status.withDetail("active.agents", activeAgents);
        if (activeAgents < 3) {
            status.down().withDetail("reason", "Insufficient active agents");
        }
        
        // Check event broker
        if (!eventBroker.isConnected()) {
            status.down().withDetail("reason", "Event broker disconnected");
        }
        
        // Check memory system
        if (!memorySystem.isOperational()) {
            status.down().withDetail("reason", "Memory system failure");
        }
        
        // Performance metrics
        double avgResponseTime = getAverageResponseTime();
        status.withDetail("avg.response.time.ms", avgResponseTime);
        if (avgResponseTime > 5000) {
            status.down().withDetail("reason", "High response times");
        }
        
        return status.build();
    }
}
```

---

## Deployment Architecture

### Container Architecture

#### Multi-Container Deployment
```yaml
# docker-compose.yml for MeshChat system
version: '3.8'
services:
  meshchat-core:
    image: meshchat-system:latest
    environment:
      - AMCP_EVENT_BROKER_TYPE=kafka
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - CONVERSATION_MEMORY_ENABLED=true
    depends_on:
      - kafka
      - redis
    ports:
      - "8080:8080"
    
  meshchat-agents:
    image: meshchat-agents:latest
    environment:
      - AMCP_EVENT_BROKER_TYPE=kafka
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - AGENT_REGISTRY_ENABLED=true
    depends_on:
      - kafka
      - meshchat-core
    scale: 3
    
  kafka:
    image: confluentinc/cp-kafka:latest
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    
  redis:
    image: redis:alpine
    ports:
      - "6379:6379"
```

### Kubernetes Deployment

#### Production-Ready Manifests
```yaml
# meshchat-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: meshchat-system
  labels:
    app: meshchat
    tier: application
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: meshchat
  template:
    metadata:
      labels:
        app: meshchat
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      containers:
      - name: meshchat
        image: meshchat-system:1.5.0
        ports:
        - containerPort: 8080
          name: http
        env:
        - name: AMCP_EVENT_BROKER_TYPE
          value: "kafka"
        - name: KAFKA_BOOTSTRAP_SERVERS
          value: "kafka-cluster:9092"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
```

---

## Future Architecture Considerations

### Scalability Enhancements

#### Horizontal Scaling Strategy
```java
// Agent load balancing
public class LoadBalancedAgentRouter {
    public String selectOptimalAgent(String capability, String task) {
        List<String> capableAgents = agentRegistry.findAgentsByCapability(capability);
        
        // Load balancing factors
        return capableAgents.stream()
            .min(Comparator.comparing(this::getAgentLoad)
                .thenComparing(this::getAgentLatency)
                .thenComparing(this::getAgentCapacityScore))
            .orElse(null);
    }
}
```

#### Event Partitioning
```java
// Partition events by session for better scaling
public class SessionPartitionedEventBroker {
    public void publishEvent(Event event) {
        String partitionKey = extractSessionId(event)
            .orElse(event.getCorrelationId())
            .orElse(generateRandomKey());
            
        broker.publish(event.getTopic(), event, partitionKey);
    }
}
```

### Advanced AI Integration

#### Multi-LLM Support
```java
// Support for multiple LLM providers
public interface LLMProvider {
    CompletableFuture<String> generateResponse(String prompt, LLMConfig config);
    boolean isAvailable();
    double getCurrentLoad();
}

public class LLMRouter {
    private final List<LLMProvider> providers;
    
    public CompletableFuture<String> route(String prompt, String preferredProvider) {
        return providers.stream()
            .filter(p -> p.isAvailable())
            .min(Comparator.comparing(LLMProvider::getCurrentLoad))
            .map(provider -> provider.generateResponse(prompt, getConfig()))
            .orElse(CompletableFuture.completedFuture("No LLM providers available"));
    }
}
```

---

This architecture guide provides a comprehensive view of the MeshChat system's design, implementation patterns, and operational considerations. The system demonstrates how sophisticated multi-agent AI can be built using AMCP's enterprise-grade foundation while maintaining scalability, reliability, and extensibility.