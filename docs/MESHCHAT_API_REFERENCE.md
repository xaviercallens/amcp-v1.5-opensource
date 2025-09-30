# MeshChat API Reference

## Overview

This guide provides detailed API documentation for developers working with the MeshChat agent system. All agents follow AMCP v1.5 Enterprise Edition patterns with event-driven architecture and CompletableFuture-based async operations.

---

## Core Architecture Patterns

### Agent Base Class Pattern
```java
public class MyAgent extends AbstractMobileAgent {
    @Override
    public void onActivate() {
        super.onActivate();
        subscribe("my.topic.**");
        // Initialize resources
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            // Process event asynchronously
        });
    }
    
    @Override
    public void setContext(AgentContext context) {
        this.context = context; // CRITICAL: Always call before activation
    }
}
```

### Event Publishing Pattern
```java
// Simple event publishing
publishEvent("topic.name", payloadObject);

// Complex event with metadata
publishEvent(Event.builder()
    .topic("meshchat.user.message")
    .payload(messageData)
    .correlationId(UUID.randomUUID().toString())
    .metadata("sessionId", sessionId)
    .deliveryOptions(DeliveryOptions.RELIABLE)
    .build());
```

### Event Subscription Pattern
```java
@Override
public void onActivate() {
    super.onActivate();
    
    // Subscribe to specific topics
    subscribe("meshchat.user.message");
    subscribe("orchestrator.task.assign");
    
    // Subscribe to topic patterns
    subscribe("travel.**");        // All travel events
    subscribe("stock.request.*");  // All stock requests
    subscribe("*.response");       // All response events
}
```

---

## MeshChatAgent API

### Class: `io.amcp.examples.meshchat.MeshChatAgent`

Main gateway agent providing human-to-AI conversational interface.

#### Constructor
```java
public MeshChatAgent()
```

#### Core Methods

##### Session Management
```java
/**
 * Creates a new chat session for a user
 * @param userId The unique identifier for the user
 * @return The session ID for the new chat session
 */
public String createChatSession(String userId)

/**
 * Retrieves an existing chat session
 * @param sessionId The session identifier
 * @return The ChatSession object or null if not found
 */
public ChatSession getSession(String sessionId)

/**
 * Adds a message to an existing session
 * @param sessionId The session identifier
 * @param message The message content
 * @param sender The sender identifier ("user" or "assistant")
 */
public void addMessageToSession(String sessionId, String message, String sender)
```

##### Message Processing
```java
/**
 * Processes an incoming user message asynchronously
 * @param message The user's message content
 * @param sessionId The session identifier
 * @return CompletableFuture containing the response
 */
public CompletableFuture<String> processMessage(String message, String sessionId)

/**
 * Validates if a message is acceptable for processing
 * @param message The message to validate
 * @return true if valid, false otherwise
 */
public boolean isValidMessage(String message)

/**
 * Classifies a message to determine routing
 * @param message The message to classify
 * @return Classification string ("travel", "stock", "general")
 */
public String classifyMessageTopic(String message)
```

##### Response Generation
```java
/**
 * Generates a response based on topic classification
 * @param topic The classified topic
 * @param message The original message
 * @param sessionId The session identifier
 * @return Generated response string
 */
public String generateResponse(String topic, String message, String sessionId)

/**
 * Generates a fallback response for unhandled cases
 * @param message The original message
 * @return Default response string
 */
public String generateFallbackResponse(String message)
```

##### Utility Methods
```java
/**
 * Formats current timestamp for logging
 * @return Formatted timestamp string (HH:mm:ss)
 */
public String formatTimestamp()

/**
 * Logs a message with timestamp and class name
 * @param message The message to log
 */
public void logMessage(String message)
```

#### Data Classes

##### ChatSession
```java
public class ChatSession {
    private String sessionId;
    private String userId;
    private long createdTime;
    private List<ChatMessage> messages;
    
    // Constructors
    public ChatSession(String sessionId, String userId)
    
    // Getters and setters
    public String getSessionId()
    public void setSessionId(String sessionId)
    public String getUserId()
    public void setUserId(String userId)
    public long getCreatedTime()
    public void setCreatedTime(long createdTime)
    public List<ChatMessage> getMessages()
    public void setMessages(List<ChatMessage> messages)
    
    // Utility methods
    public void addMessage(ChatMessage message)
    public ChatMessage getLastMessage()
    public int getMessageCount()
}
```

##### ChatMessage
```java
public class ChatMessage {
    private String content;
    private String sender;
    private long timestamp;
    
    // Constructors
    public ChatMessage(String content, String sender)
    public ChatMessage(String content, String sender, long timestamp)
    
    // Getters and setters
    public String getContent()
    public void setContent(String content)
    public String getSender()
    public void setSender(String sender)
    public long getTimestamp()
    public void setTimestamp(long timestamp)
}
```

#### Event Topics

##### Published Events
```java
// User message received
"meshchat.user.message" -> {
    sessionId: String,
    message: String,
    userId: String,
    timestamp: long
}

// Session started
"meshchat.session.start" -> {
    sessionId: String,
    userId: String,
    timestamp: long
}

// Session ended
"meshchat.session.end" -> {
    sessionId: String,
    userId: String,
    duration: long
}
```

##### Subscribed Events
```java
// Orchestrator responses
"orchestrator.response"
"orchestrator.task.complete"

// Agent responses
"travel.agent.response"
"stock.agent.response"
"*.agent.response"

// System events
"system.agent.registered"
"system.agent.deregistered"
```

---

## TravelPlannerAgent API

### Class: `io.amcp.examples.travel.TravelPlannerAgent`

Specialized agent for travel planning and destination services.

#### Core Methods

##### Destination Management
```java
/**
 * Retrieves destination information by name
 * @param name The destination name (case-insensitive)
 * @return TravelDestination object or null if not found
 */
public TravelDestination getDestination(String name)

/**
 * Gets all available destinations
 * @return List of all TravelDestination objects
 */
public List<TravelDestination> getAllDestinations()

/**
 * Searches destinations based on criteria
 * @param criteria Search terms (location, activities, etc.)
 * @return List of matching destinations
 */
public List<TravelDestination> searchDestinations(String criteria)
```

##### Trip Planning
```java
/**
 * Plans a trip to a single destination
 * @param destination The destination name
 * @param days Number of days for the trip
 * @param budget Total budget in USD
 * @return Detailed trip plan as formatted string
 */
public String planTrip(String destination, int days, double budget)

/**
 * Plans a multi-city trip
 * @param cities List of destination cities
 * @param totalDays Total days for the entire trip
 * @param budget Total budget in USD
 * @return Multi-city itinerary as formatted string
 */
public String planMultiCityTrip(List<String> cities, int totalDays, double budget)

/**
 * Estimates budget for a trip
 * @param destination The destination name
 * @param days Number of days
 * @param travelStyle Style of travel ("budget", "mid-range", "luxury")
 * @return Estimated budget in USD
 */
public double estimateBudget(String destination, int days, String travelStyle)
```

##### Recommendations
```java
/**
 * Gets destination recommendations based on interests
 * @param interests User interests (e.g., "culture", "adventure", "food")
 * @return Formatted recommendation string
 */
public String getDestinationRecommendations(String interests)

/**
 * Provides best time to visit information
 * @param destination The destination name
 * @return Best time to visit description
 */
public String getBestTimeToVisit(String destination)

/**
 * Provides cultural tips for a destination
 * @param destination The destination name
 * @return Cultural tips and etiquette advice
 */
public String getCulturalTips(String destination)
```

#### Data Classes

##### TravelDestination
```java
public class TravelDestination {
    private String name;
    private String country;
    private String description;
    private List<String> highlights;
    private List<String> activities;
    private String bestTimeToVisit;
    private double averageDailyBudget;
    private String culturalTips;
    
    // Constructor
    public TravelDestination(String name, String country, String description)
    
    // Getters and setters for all fields
    // Utility methods
    public boolean matchesSearchCriteria(String criteria)
    public String getFormattedInfo()
    public double getEstimatedCost(int days, String travelStyle)
}
```

#### Available Destinations
```java
// 11 major destinations with comprehensive data:
- Tokyo, Japan
- Paris, France  
- New York, USA
- London, UK
- Sydney, Australia
- Dubai, UAE
- Bangkok, Thailand
- Rome, Italy
- Barcelona, Spain
- Amsterdam, Netherlands
- Reykjavik, Iceland
```

#### Event Topics

##### Published Events
```java
// Trip plan completed
"travel.plan.completed" -> {
    destination: String,
    days: int,
    budget: double,
    plan: String
}

// Destination info requested
"travel.destination.info" -> {
    destination: String,
    info: TravelDestination
}
```

##### Subscribed Events
```java
// Travel requests
"travel.request.plan"
"travel.request.destination"
"travel.request.budget"
"meshchat.travel.query"
```

---

## StockAgent API

### Class: `io.amcp.examples.stock.StockAgent`

Specialized agent for financial services and stock market analysis.

#### Core Methods

##### Stock Information
```java
/**
 * Gets current stock quote and analysis
 * @param symbol Stock symbol (e.g., "AAPL", "GOOGL")
 * @return StockData object with current information
 */
public StockData getStockQuote(String symbol)

/**
 * Gets overview of major market indices
 * @return List of current market index data
 */
public List<StockData> getMarketOverview()

/**
 * Gets specific market index information
 * @param indexName Index name ("SP500", "NASDAQ", "DOW")
 * @return MarketIndex object with current data
 */
public MarketIndex getMarketIndex(String indexName)
```

##### Analysis Services
```java
/**
 * Provides detailed stock analysis
 * @param symbol Stock symbol to analyze
 * @return Comprehensive analysis string
 */
public String analyzeStock(String symbol)

/**
 * Provides investment recommendation
 * @param symbol Stock symbol
 * @param riskTolerance Risk level ("conservative", "moderate", "aggressive")
 * @return Investment recommendation string
 */
public String getInvestmentRecommendation(String symbol, String riskTolerance)

/**
 * Analyzes cryptocurrency
 * @param cryptoSymbol Crypto symbol (e.g., "BTC", "ETH")
 * @return Cryptocurrency analysis string
 */
public String analyzeCryptocurrency(String cryptoSymbol)
```

##### Portfolio Services
```java
/**
 * Provides portfolio diversification advice
 * @param currentHoldings List of current stock symbols
 * @return Diversification recommendations
 */
public String getDiversificationAdvice(List<String> currentHoldings)

/**
 * Analyzes specific market sector
 * @param sectorName Sector name (e.g., "Technology", "Healthcare")
 * @return Sector analysis and trends
 */
public String analyzeSector(String sectorName)

/**
 * Gets current market sentiment
 * @return Market sentiment analysis
 */
public String getMarketSentiment()
```

#### Data Classes

##### StockData
```java
public class StockData {
    private String symbol;
    private String companyName;
    private double currentPrice;
    private double changePercent;
    private long volume;
    private double marketCap;
    private String analysis;
    private String recommendation;
    
    // Constructor
    public StockData(String symbol, String companyName, double currentPrice)
    
    // Getters and setters for all fields
    // Utility methods
    public boolean isPositivePerformance()
    public String getFormattedPrice()
    public String getMarketCapFormatted()
}
```

##### MarketIndex
```java
public class MarketIndex {
    private String name;
    private double value;
    private double changePercent;
    private String trend;
    
    // Constructor and getters/setters
    public boolean isBullish()
    public String getFormattedValue()
}
```

#### Sample Stocks Available
```java
// Major stocks with simulated real-time data:
- AAPL (Apple Inc.)
- GOOGL (Alphabet Inc.)
- MSFT (Microsoft Corporation)
- TSLA (Tesla Inc.)
- AMZN (Amazon.com Inc.)
- META (Meta Platforms Inc.)
- NVDA (NVIDIA Corporation)
- JPM (JPMorgan Chase & Co.)
- JNJ (Johnson & Johnson)
- V (Visa Inc.)
```

#### Event Topics

##### Published Events
```java
// Stock analysis completed
"stock.analysis.completed" -> {
    symbol: String,
    analysis: String,
    recommendation: String
}

// Market update
"stock.market.update" -> {
    indices: List<MarketIndex>,
    sentiment: String
}
```

##### Subscribed Events
```java
// Stock requests
"stock.request.quote"
"stock.request.analysis"
"stock.request.recommendation"
"meshchat.stock.query"
```

---

## EnhancedOrchestratorAgent API

### Class: `io.amcp.examples.orchestration.EnhancedOrchestratorAgent`

Intelligent orchestrator with LLM-powered task routing and planning.

#### Core Methods

##### Task Orchestration
```java
/**
 * Plans and orchestrates a complex task
 * @param taskDescription Natural language task description
 * @param context Additional context information
 * @return CompletableFuture containing execution plan
 */
public CompletableFuture<String> planTask(String taskDescription, Map<String, Object> context)

/**
 * Routes a task to appropriate specialist agent
 * @param task Task description
 * @param availableAgents List of available agent capabilities
 * @return Routing decision and agent assignment
 */
public String routeTask(String task, List<String> availableAgents)

/**
 * Coordinates multiple agents for complex tasks
 * @param tasks List of subtasks
 * @param sessionId Session identifier for tracking
 * @return Coordination plan and execution strategy
 */
public CompletableFuture<String> coordinateMultiAgentTask(List<String> tasks, String sessionId)
```

##### Enhanced Prompting
```java
/**
 * Builds context-aware planning prompt for LLM
 * @param task Task description
 * @param availableAgents Available agent capabilities
 * @param sessionContext Previous conversation context
 * @return Enhanced prompt for better LLM planning
 */
public String buildPlanningPrompt(String task, List<String> availableAgents, String sessionContext)

/**
 * Classifies task complexity and requirements
 * @param task Task description
 * @return Task classification and complexity assessment
 */
public TaskClassification classifyTask(String task)
```

#### Enhanced Features

##### Keyword-Based Routing
```java
// Built-in intelligent routing keywords:

// Travel-related keywords:
private static final String[] TRAVEL_KEYWORDS = {
    "trip", "travel", "hotel", "flight", "destination", "vacation",
    "journey", "tour", "visit", "booking", "itinerary", "sightseeing"
};

// Financial keywords:
private static final String[] STOCK_KEYWORDS = {
    "stock", "price", "market", "investment", "portfolio", "trading",
    "financial", "money", "fund", "dividend", "crypto", "bitcoin"
};

// Multi-agent coordination keywords:
private static final String[] COORDINATION_KEYWORDS = {
    "plan and research", "coordinate", "multiple", "both", "also",
    "additionally", "meanwhile", "combine", "integrate"
};
```

##### Context Enhancement
```java
/**
 * Extracts relevant context from conversation history
 * @param sessionId Session identifier
 * @param messageCount Number of recent messages to consider
 * @return Extracted context summary
 */
public String extractSessionContext(String sessionId, int messageCount)

/**
 * Enhances task understanding with domain knowledge
 * @param task Original task
 * @param domain Identified domain (travel, finance, etc.)
 * @return Enhanced task description with domain context
 */
public String enhanceTaskWithDomainKnowledge(String task, String domain)
```

#### Event Topics

##### Published Events
```java
// Task orchestration started
"orchestrator.task.started" -> {
    taskId: String,
    description: String,
    assignedAgents: List<String>
}

// Task completed
"orchestrator.task.completed" -> {
    taskId: String,
    result: String,
    duration: long
}

// Agent coordination
"orchestrator.coordination.request" -> {
    agents: List<String>,
    task: String,
    priority: int
}
```

##### Subscribed Events
```java
// Task requests
"meshchat.task.request"
"orchestrator.plan.request"
"*.task.assign"

// Agent responses
"travel.agent.response"
"stock.agent.response"
"*.agent.complete"
```

---

## ConversationMemorySystem API

### Class: `io.amcp.examples.memory.ConversationMemorySystem`

Persistent conversation memory with context management.

#### Core Methods

##### Session Management
```java
/**
 * Creates a new conversation session
 * @param userId User identifier
 * @return New ConversationSession object
 */
public ConversationSession createSession(String userId)

/**
 * Retrieves existing session
 * @param sessionId Session identifier
 * @return ConversationSession or null if not found
 */
public ConversationSession getSession(String sessionId)

/**
 * Gets all sessions for a user
 * @param userId User identifier
 * @return List of user's conversation sessions
 */
public List<ConversationSession> getUserSessions(String userId)

/**
 * Ends and archives a session
 * @param sessionId Session identifier
 */
public void endSession(String sessionId)
```

##### Message Management
```java
/**
 * Adds message to session
 * @param sessionId Session identifier
 * @param content Message content
 * @param sender Sender identifier
 */
public void addMessage(String sessionId, String content, String sender)

/**
 * Gets session message history
 * @param sessionId Session identifier
 * @return List of conversation messages
 */
public List<ConversationMessage> getSessionMessages(String sessionId)

/**
 * Searches messages within a session
 * @param sessionId Session identifier
 * @param query Search query
 * @return List of matching messages
 */
public List<ConversationMessage> searchMessages(String sessionId, String query)
```

##### Context Operations
```java
/**
 * Extracts relevant context from recent messages
 * @param sessionId Session identifier
 * @param messageCount Number of recent messages to extract
 * @return Context summary string
 */
public String extractContext(String sessionId, int messageCount)

/**
 * Summarizes long conversation for efficiency
 * @param sessionId Session identifier
 * @return Conversation summary
 */
public String summarizeConversation(String sessionId)

/**
 * Cleans up old sessions to manage memory
 * @param maxAgeMillis Maximum age in milliseconds
 */
public void cleanupOldSessions(long maxAgeMillis)
```

#### Data Classes

##### ConversationSession
```java
public class ConversationSession {
    private String sessionId;
    private String userId;
    private long startTime;
    private long lastActivity;
    private List<ConversationMessage> messages;
    private Map<String, Object> metadata;
    private boolean isActive;
    
    // Constructors and getters/setters
    // Utility methods
    public void addMessage(ConversationMessage message)
    public ConversationMessage getLastMessage()
    public long getDuration()
    public int getMessageCount()
    public boolean isExpired(long maxAge)
}
```

##### ConversationMessage
```java
public class ConversationMessage {
    private String messageId;
    private String content;
    private String sender;
    private long timestamp;
    private Map<String, Object> metadata;
    
    // Constructors and getters/setters
    public boolean matches(String query)
    public long getAge()
    public String getFormattedTimestamp()
}
```

---

## AgentRegistry & DiscoveryService API

### Class: `io.amcp.examples.discovery.AgentRegistry`

Dynamic agent registration and capability tracking.

#### Core Methods

##### Agent Registration
```java
/**
 * Registers an agent with capabilities
 * @param agentId Agent identifier
 * @param capabilities List of agent capabilities
 * @param metadata Additional agent metadata
 */
public void registerAgent(String agentId, List<String> capabilities, Map<String, Object> metadata)

/**
 * Deregisters an agent
 * @param agentId Agent identifier
 */
public void deregisterAgent(String agentId)

/**
 * Updates agent capabilities
 * @param agentId Agent identifier
 * @param capabilities Updated capability list
 */
public void updateCapabilities(String agentId, List<String> capabilities)
```

##### Discovery Operations
```java
/**
 * Finds agents by capability
 * @param capability Required capability
 * @return List of matching agent IDs
 */
public List<String> findAgentsByCapability(String capability)

/**
 * Gets all active agents
 * @return List of all registered agent IDs
 */
public List<String> getActiveAgents()

/**
 * Checks if agent is available
 * @param agentId Agent identifier
 * @return true if agent is active and available
 */
public boolean isAgentAvailable(String agentId)
```

### Class: `io.amcp.examples.discovery.DiscoveryService`

Intelligent capability matching and routing recommendations.

#### Core Methods

##### Capability Matching
```java
/**
 * Finds best agent for a task
 * @param taskDescription Task description
 * @param requiredCapabilities Required capabilities
 * @return Recommended agent ID
 */
public String findBestAgent(String taskDescription, List<String> requiredCapabilities)

/**
 * Gets agents ranked by suitability
 * @param task Task description
 * @return List of agents ranked by match score
 */
public List<AgentMatch> rankAgentsByTask(String task)

/**
 * Suggests multi-agent coordination
 * @param complexTask Complex task requiring multiple agents
 * @return Agent coordination recommendation
 */
public CoordinationPlan suggestCoordination(String complexTask)
```

---

## Error Handling Patterns

### Standard Exception Handling
```java
@Override
public CompletableFuture<Void> handleEvent(Event event) {
    return CompletableFuture.runAsync(() -> {
        try {
            processEvent(event);
        } catch (Exception e) {
            logger.error("Error processing event: {}", event.getTopic(), e);
            publishErrorEvent(event, e);
        }
    });
}

private void publishErrorEvent(Event originalEvent, Exception error) {
    publishEvent("error.processing", Map.of(
        "originalTopic", originalEvent.getTopic(),
        "errorMessage", error.getMessage(),
        "timestamp", System.currentTimeMillis()
    ));
}
```

### Graceful Degradation
```java
public String generateResponse(String topic, String message, String sessionId) {
    try {
        return switch (topic) {
            case "travel" -> handleTravelQuery(message, sessionId);
            case "stock" -> handleStockQuery(message, sessionId);
            default -> handleGeneralQuery(message, sessionId);
        };
    } catch (Exception e) {
        logger.warn("Error generating response for topic: {}", topic, e);
        return generateFallbackResponse(message);
    }
}
```

---

## Performance Considerations

### Async Processing
```java
// Use CompletableFuture for all long-running operations
public CompletableFuture<String> processComplexQuery(String query) {
    return CompletableFuture.supplyAsync(() -> {
        // Long-running processing
        return result;
    }, executorService);
}
```

### Caching Strategies
```java
// Cache frequently accessed data
private final LoadingCache<String, TravelDestination> destinationCache = 
    Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build(this::loadDestination);
```

### Memory Management
```java
// Regular cleanup of conversation history
@Scheduled(fixedDelay = 3600000) // Every hour
public void cleanupExpiredSessions() {
    long maxAge = TimeUnit.DAYS.toMillis(7); // 7 days
    memorySystem.cleanupOldSessions(maxAge);
}
```

---

## Integration Examples

### Adding a New Agent Type

1. **Extend AbstractMobileAgent**:
```java
public class WeatherAgent extends AbstractMobileAgent {
    @Override
    public void onActivate() {
        super.onActivate();
        subscribe("weather.request.**");
        subscribe("meshchat.weather.query");
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            if (event.getTopic().startsWith("weather.request")) {
                handleWeatherRequest(event);
            }
        });
    }
}
```

2. **Register with Discovery Service**:
```java
agentRegistry.registerAgent(
    getAgentId().getId(),
    List.of("weather", "forecast", "climate"),
    Map.of("location", "global", "accuracy", "high")
);
```

3. **Update Orchestrator Routing**:
```java
// Add weather keywords to EnhancedOrchestratorAgent
private static final String[] WEATHER_KEYWORDS = {
    "weather", "forecast", "temperature", "rain", "sunny", "climate"
};
```

### Custom Event Handling
```java
public class CustomEventHandler {
    @EventHandler("custom.event.type")
    public CompletableFuture<Void> handleCustomEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            // Custom processing logic
            Map<String, Object> payload = event.getPayload();
            // Process and respond
        });
    }
}
```

---

This API reference provides comprehensive documentation for integrating with and extending the MeshChat agent system. All methods follow AMCP v1.5 Enterprise Edition patterns for consistency and reliability.