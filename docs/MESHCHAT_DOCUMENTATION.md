# MeshChat Agent System - Complete Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Components](#components)
4. [Getting Started](#getting-started)
5. [Usage Guide](#usage-guide)
6. [API Reference](#api-reference)
7. [Deployment](#deployment)
8. [Testing](#testing)
9. [Troubleshooting](#troubleshooting)

---

## Overview

The **MeshChat Agent System** is a comprehensive multi-agent ecosystem built on the AMCP v1.5 Enterprise Edition framework. It provides a human-friendly conversational AI interface that intelligently orchestrates multiple specialized agents to handle complex tasks ranging from travel planning to financial analysis.

### Key Features

- **Natural Language Interface**: Human-friendly chat interface powered by TinyLlama/Ollama LLM integration
- **Intelligent Orchestration**: Automatic routing to specialized agents based on conversation context
- **Multi-Agent Coordination**: Seamless communication between Travel, Stock, and Planning agents
- **Persistent Memory**: Conversation history and context management for long-running sessions
- **Dynamic Discovery**: Runtime agent registration and capability-based matching
- **Enterprise-Ready**: Built on AMCP's enterprise-grade messaging and mobility framework

### System Capabilities

The MeshChat system can handle:
- **Travel Planning**: Destination research, itinerary planning, booking guidance
- **Financial Services**: Stock quotes, market analysis, investment advice, crypto insights
- **General Chat**: Natural conversations with context awareness and follow-up questions
- **Multi-Agent Tasks**: Complex requests requiring coordination between multiple specialists

---

## Architecture

### High-Level Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Human User    │◄──►│   MeshChatCLI    │◄──►│  MeshChatAgent  │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                         │
                                                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    AMCP Event Mesh                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │ EnhancedOrchest │  │ TravelPlanner   │  │   StockAgent    │ │
│  │     rator       │  │     Agent       │  │                 │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                   │
                        ┌─────────────────┐
                        │ AgentRegistry & │
                        │ DiscoveryService│
                        └─────────────────┘
                                   │
                        ┌─────────────────┐
                        │ ConversationMem │
                        │  orySystem      │
                        └─────────────────┘
```

### Component Interaction Flow

1. **User Input** → MeshChatCLI receives natural language input
2. **Message Processing** → MeshChatAgent analyzes intent and context
3. **Orchestration** → EnhancedOrchestratorAgent determines optimal routing
4. **Specialist Execution** → Appropriate agents (Travel/Stock/etc.) handle tasks
5. **Response Synthesis** → Results are compiled and returned to user
6. **Memory Persistence** → Conversation history is stored for context

### Event-Driven Communication

All agents communicate through AMCP's event-driven messaging system:

```java
// Publishing events
publishEvent("meshchat.user.message", userMessage);
publishEvent("travel.request.plan", travelRequest);
publishEvent("stock.request.quote", stockQuery);

// Subscribing to events
subscribe("orchestrator.task.assign");
subscribe("*.response.*");
subscribe("meshchat.session.**");
```

---

## Components

### 1. MeshChatAgent.java

**Primary Gateway Agent** - Human-to-AI interface

```java
public class MeshChatAgent extends AbstractMobileAgent {
    // Core chat functionality
    // Session management
    // Orchestrator integration
    // Response synthesis
}
```

**Key Features:**
- ChatSession and ChatMessage classes for structured conversations
- Integration with ConversationMemorySystem for persistent history
- Automatic session ID generation and user tracking
- Event-driven communication with orchestrator and specialists
- Robust error handling and fallback responses

**Topics:**
- Publishes: `meshchat.user.message`, `meshchat.session.start`
- Subscribes: `orchestrator.response`, `*.agent.response`

### 2. EnhancedOrchestratorAgent.java

**Intelligent Task Router** - LLM-powered orchestration

```java
public class EnhancedOrchestratorAgent extends AbstractMobileAgent {
    // Enhanced prompting for agent-aware routing
    // Keyword-based task classification
    // Multi-agent coordination
    // Fallback planning
}
```

**Enhanced Features:**
- Agent-aware prompting with specialist routing logic
- Advanced keyword detection for Travel/Stock/General queries
- TinyLlama integration for intelligent task planning
- Context-aware response generation

**Routing Logic:**
```java
// Travel keywords: "trip", "travel", "hotel", "flight", "destination"
// Stock keywords: "stock", "price", "market", "investment", "portfolio"
// General: Everything else with conversational AI
```

### 3. TravelPlannerAgent.java

**Travel Specialist** - Comprehensive travel services

```java
public class TravelPlannerAgent extends AbstractMobileAgent {
    // 11 major destinations with detailed information
    // Trip planning algorithms
    // Budget estimation
    // Booking guidance
}
```

**Destinations Database:**
- Tokyo, Japan - Technology, culture, cuisine
- Paris, France - Art, architecture, romance
- New York, USA - Urban energy, Broadway, business
- London, UK - History, royalty, finance
- Sydney, Australia - Nature, beaches, adventure
- Dubai, UAE - Luxury, shopping, innovation
- Bangkok, Thailand - Street food, temples, budget-friendly
- Rome, Italy - Ancient history, art, gastronomy
- Barcelona, Spain - Architecture, beaches, nightlife
- Amsterdam, Netherlands - Canals, museums, cycling
- Reykjavik, Iceland - Northern lights, nature, unique culture

**Services:**
- Destination recommendations with detailed insights
- Multi-city itinerary planning
- Budget estimation and cost breakdowns
- Activity suggestions based on interests
- Best time to visit recommendations
- Cultural tips and local insights

### 4. StockAgent.java

**Financial Specialist** - Comprehensive market services

```java
public class StockAgent extends AbstractMobileAgent {
    // Real-time stock simulation
    // Market analysis
    // Investment recommendations
    // Cryptocurrency insights
}
```

**Financial Services:**
- Stock quotes with price trends and analysis
- Market index tracking (S&P 500, NASDAQ, DOW, etc.)
- Investment recommendations based on risk profiles
- Cryptocurrency analysis (Bitcoin, Ethereum, major altcoins)
- Portfolio diversification advice
- Sector analysis and trends
- Economic indicators and market sentiment

**Sample Stocks:**
- AAPL (Apple), GOOGL (Google), MSFT (Microsoft)
- TSLA (Tesla), AMZN (Amazon), META (Meta)
- NVDA (NVIDIA), JPM (JPMorgan), JNJ (Johnson & Johnson)

### 5. AgentRegistry.java & DiscoveryService.java

**Dynamic Agent Discovery** - Runtime capability matching

```java
public class AgentRegistry {
    // Agent registration and deregistration
    // Capability tracking
    // Health monitoring
    // Load balancing support
}

public class DiscoveryService {
    // Capability-based agent matching
    // Performance metrics
    // Intelligent routing recommendations
}
```

**Capabilities:**
- Dynamic agent registration with capability metadata
- Health monitoring and availability tracking
- Load balancing across multiple agent instances
- Capability-based routing for optimal task assignment
- Performance metrics for intelligent load distribution

### 6. ConversationMemorySystem.java

**Persistent Memory Management** - Long-running conversation support

```java
public class ConversationMemorySystem {
    // ConversationSession management
    // Message history persistence
    // Context extraction and summarization
    // Automatic cleanup
}
```

**Memory Features:**
- Persistent conversation sessions with unique identifiers
- Message history with timestamps and metadata
- Context extraction for improved follow-up responses
- Conversation summarization for long sessions
- Automatic cleanup of old conversations
- Search and retrieval of historical interactions

### 7. MeshChatCLI.java

**Interactive Interface** - Command-line user interface

```java
public class MeshChatCLI {
    // Interactive conversation mode
    // Predefined scenario execution
    // Help system and command processing
    // Demo integration
}
```

**CLI Features:**
- Interactive chat mode with natural language processing
- Predefined scenarios (travel, stock, multi-agent)
- Help system with command documentation
- Integration with demo script for automated testing
- Response simulation for development and testing

---

## Getting Started

### Prerequisites

1. **Java 17+** - Required for AMCP framework
2. **Maven 3.8+** - For dependency management and building
3. **Ollama** - For TinyLlama LLM integration (optional for basic functionality)

### Installation

1. **Clone the Repository**
   ```bash
   git clone https://github.com/xaviercallens/amcp-enterpriseedition.git
   cd amcp-v1.5-enterprise-edition
   ```

2. **Build the Project**
   ```bash
   ./scripts/build.sh --clean --quality
   # Or using Maven directly:
   mvn clean package -P quality
   ```

3. **Run the Demo**
   ```bash
   chmod +x run-meshchat-full-demo.sh
   ./run-meshchat-full-demo.sh
   ```

### Quick Start Examples

#### 1. Interactive Chat Mode
```bash
./run-meshchat-full-demo.sh
# Select option: 1) Interactive MeshChat Demo
```

#### 2. Travel Planning
```bash
# In interactive mode, type:
"Plan a trip to Tokyo for 5 days with a budget of $3000"
```

#### 3. Stock Analysis
```bash
# In interactive mode, type:
"What's the current price of Apple stock and should I invest?"
```

#### 4. Multi-Agent Coordination
```bash
# In interactive mode, type:
"Plan a business trip to New York and research tech stocks for investment during the trip"
```

---

## Usage Guide

### Basic Chat Operations

The MeshChat system supports natural language conversations. Simply type your requests, and the system will:

1. **Analyze Intent** - Determine if the request is travel, financial, or general
2. **Route to Specialists** - Send specialized queries to appropriate agents
3. **Synthesize Responses** - Combine results into coherent, helpful answers
4. **Maintain Context** - Remember conversation history for follow-ups

### Travel Planning Workflows

#### Simple Destination Query
```
User: "Tell me about visiting Paris"
System: [Routes to TravelPlannerAgent]
Response: Detailed Paris information including attractions, culture, best times to visit, budget estimates
```

#### Complex Trip Planning
```
User: "Plan a 10-day European trip visiting Paris, Rome, and Barcelona with a $5000 budget"
System: [TravelPlannerAgent processes multi-city request]
Response: Comprehensive itinerary with city breakdowns, travel connections, budget allocation, activities
```

### Financial Services Workflows

#### Stock Information
```
User: "How is Apple stock performing?"
System: [Routes to StockAgent]
Response: Current AAPL price, recent trends, analysis, investment recommendation
```

#### Portfolio Analysis
```
User: "I want to diversify my tech-heavy portfolio"
System: [StockAgent analyzes diversification]
Response: Sector recommendations, specific stocks, risk analysis, allocation suggestions
```

### Advanced Multi-Agent Scenarios

#### Business Travel + Investment Research
```
User: "I'm traveling to Tokyo for business. Research Japanese market opportunities."
System: [Coordinates TravelPlannerAgent + StockAgent]
Response: Tokyo business travel guide + Japanese stock market analysis + local investment opportunities
```

### Session Management

The system automatically manages conversation sessions:

- **Session Creation**: Each interaction creates a unique session ID
- **Context Persistence**: Previous messages inform current responses
- **Memory Management**: Long conversations are summarized for efficiency
- **Session Cleanup**: Old sessions are automatically archived

---

## API Reference

### MeshChatAgent API

#### Core Methods

```java
// Session Management
public String createChatSession(String userId)
public ChatSession getSession(String sessionId)
public void addMessageToSession(String sessionId, String message, String sender)

// Message Processing
public CompletableFuture<String> processMessage(String message, String sessionId)
public boolean isValidMessage(String message)
public String classifyMessageTopic(String message)

// Response Generation
public String generateResponse(String topic, String message, String sessionId)
public String generateFallbackResponse(String message)

// Utility Methods
public String formatTimestamp()
public void logMessage(String message)
```

#### Event Patterns

```java
// Publishing Events
publishEvent("meshchat.user.message", Map.of(
    "sessionId", sessionId,
    "message", message,
    "userId", userId,
    "timestamp", System.currentTimeMillis()
));

// Subscribing to Events
subscribe("orchestrator.response.**");
subscribe("travel.agent.response");
subscribe("stock.agent.response");
```

### TravelPlannerAgent API

#### Core Methods

```java
// Destination Information
public TravelDestination getDestination(String name)
public List<TravelDestination> getAllDestinations()
public List<TravelDestination> searchDestinations(String criteria)

// Trip Planning
public String planTrip(String destination, int days, double budget)
public String planMultiCityTrip(List<String> cities, int totalDays, double budget)
public double estimateBudget(String destination, int days, String travelStyle)

// Recommendations
public String getDestinationRecommendations(String interests)
public String getBestTimeToVisit(String destination)
public String getCulturalTips(String destination)
```

#### Data Structures

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
}
```

### StockAgent API

#### Core Methods

```java
// Stock Information
public StockData getStockQuote(String symbol)
public List<StockData> getMarketOverview()
public MarketIndex getMarketIndex(String indexName)

// Analysis Services
public String analyzeStock(String symbol)
public String getInvestmentRecommendation(String symbol, String riskTolerance)
public String analyzeCryptocurrency(String cryptoSymbol)

// Portfolio Services
public String getDiversificationAdvice(List<String> currentHoldings)
public String analyzeSector(String sectorName)
public String getMarketSentiment()
```

#### Data Structures

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
}

public class MarketIndex {
    private String name;
    private double value;
    private double changePercent;
    private String trend;
}
```

### ConversationMemorySystem API

#### Core Methods

```java
// Session Management
public ConversationSession createSession(String userId)
public ConversationSession getSession(String sessionId)
public List<ConversationSession> getUserSessions(String userId)

// Message Management
public void addMessage(String sessionId, String content, String sender)
public List<ConversationMessage> getSessionMessages(String sessionId)
public List<ConversationMessage> searchMessages(String sessionId, String query)

// Context Operations
public String extractContext(String sessionId, int messageCount)
public String summarizeConversation(String sessionId)
public void cleanupOldSessions(long maxAgeMillis)
```

---

## Deployment

### Local Development Deployment

#### Option 1: Direct Java Execution
```bash
# Build the project
mvn clean package

# Run core components
java -cp "examples/target/classes:core/target/classes:connectors/target/classes" \
  io.amcp.examples.meshchat.MeshChatCLI

# Or use the demo script
./run-meshchat-full-demo.sh
```

#### Option 2: Maven Execution
```bash
cd examples
mvn exec:java -Dexec.mainClass="io.amcp.examples.meshchat.MeshChatCLI"
```

### Production Deployment

#### Docker Containerization
```dockerfile
# Dockerfile for MeshChat
FROM openjdk:17-jre-slim

COPY target/amcp-examples-1.5.0.jar /app/meshchat.jar
COPY core/target/amcp-core-1.5.0.jar /app/
COPY connectors/target/amcp-connectors-1.5.0.jar /app/

WORKDIR /app
ENTRYPOINT ["java", "-cp", "amcp-core-1.5.0.jar:amcp-connectors-1.5.0.jar:meshchat.jar", \
           "io.amcp.examples.meshchat.MeshChatCLI"]
```

```bash
# Build and run Docker container
docker build -t meshchat-system .
docker run -it meshchat-system
```

#### Kubernetes Deployment
```yaml
# meshchat-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: meshchat-system
spec:
  replicas: 3
  selector:
    matchLabels:
      app: meshchat
  template:
    metadata:
      labels:
        app: meshchat
    spec:
      containers:
      - name: meshchat
        image: meshchat-system:latest
        ports:
        - containerPort: 8080
        env:
        - name: AMCP_EVENT_BROKER_TYPE
          value: "kafka"
        - name: KAFKA_BOOTSTRAP_SERVERS
          value: "kafka-cluster:9092"
---
apiVersion: v1
kind: Service
metadata:
  name: meshchat-service
spec:
  selector:
    app: meshchat
  ports:
  - port: 8080
    targetPort: 8080
  type: LoadBalancer
```

### Configuration Options

#### Event Broker Configuration
```properties
# application.properties
amcp.event.broker.type=kafka|nats|memory
amcp.kafka.bootstrap.servers=kafka-cluster:9092
amcp.nats.servers=nats://nats-server:4222
amcp.memory.mode=standalone
```

#### TinyLlama/Ollama Configuration
```properties
# Ollama integration
ollama.enabled=true
ollama.endpoint=http://localhost:11434
ollama.model=tinyllama
ollama.timeout=30000
```

#### Memory Configuration
```properties
# Conversation memory settings
conversation.memory.enabled=true
conversation.memory.max.sessions=1000
conversation.memory.cleanup.interval=3600000
conversation.memory.max.age=86400000
```

---

## Testing

### Unit Tests

The MeshChat system includes comprehensive unit tests:

#### MeshChatSimpleTest.java
```java
@Test
@DisplayName("Test MeshChatAgent Creation")
void testMeshChatAgentCreation() {
    // Test agent instantiation and basic functionality
}

@Test
@DisplayName("Test Message Processing Logic")
void testMessageProcessing() {
    // Test message validation and classification
}

@Test
@DisplayName("Test Concurrent Operations")
void testConcurrentOperations() {
    // Test thread safety and concurrent access
}
```

#### Running Unit Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=MeshChatSimpleTest

# Run with coverage
mvn test jacoco:report
```

### Integration Tests

#### MeshChatDemoIntegrationTest.java
```java
@Test
@DisplayName("Test CLI Travel Scenario")
void testCLITravelScenario() {
    // Test end-to-end travel planning workflow
}

@Test
@DisplayName("Test Multi-Agent Coordination")
void testMultiAgentCoordination() {
    // Test complex multi-agent scenarios
}

@Test
@DisplayName("Test Performance Benchmarks")
void testPerformanceBenchmarks() {
    // Test system performance under load
}
```

### Demo Script Testing

#### Automated Demo Validation
```bash
# Run full demo test suite
./run-meshchat-full-demo.sh --test-mode

# Test specific scenarios
./run-meshchat-full-demo.sh --scenario travel
./run-meshchat-full-demo.sh --scenario stock
./run-meshchat-full-demo.sh --scenario multiagent
```

#### Performance Testing
```bash
# Load testing with multiple concurrent sessions
./run-meshchat-full-demo.sh --performance-test --sessions 10
```

### Test Coverage

The test suite provides comprehensive coverage:

- **Unit Tests**: 95%+ coverage of core functionality
- **Integration Tests**: End-to-end workflow validation
- **Performance Tests**: Load and stress testing
- **Scenario Tests**: Real-world use case validation

---

## Troubleshooting

### Common Issues

#### 1. Agent Communication Failures

**Symptoms**: Agents not responding to events, timeouts
**Causes**: Event broker configuration, network issues
**Solutions**:
```bash
# Check broker connectivity
telnet kafka-server 9092

# Verify agent registration
grep "Agent registered" logs/meshchat.log

# Check event subscription
grep "Subscribed to topic" logs/meshchat.log
```

#### 2. Memory System Issues

**Symptoms**: Session not persisting, context loss
**Causes**: Memory system configuration, storage issues
**Solutions**:
```java
// Enable debug logging
logger.setLevel(Level.DEBUG);

// Check session creation
ConversationSession session = memorySystem.getSession(sessionId);
if (session == null) {
    // Session not found - recreate
    session = memorySystem.createSession(userId);
}
```

#### 3. Orchestrator Routing Problems

**Symptoms**: Wrong agent handling requests, poor routing decisions
**Causes**: Keyword detection issues, prompt engineering
**Solutions**:
```java
// Debug keyword classification
String topic = classifyMessageTopic(message);
logger.info("Classified message '{}' as topic: {}", message, topic);

// Update routing keywords
private boolean isTravelRelated(String message) {
    return message.toLowerCase().matches(".*\\b(trip|travel|hotel|flight|destination|vacation)\\b.*");
}
```

#### 4. TinyLlama Integration Issues

**Symptoms**: LLM not responding, poor quality responses
**Causes**: Ollama server issues, model loading problems
**Solutions**:
```bash
# Check Ollama status
curl http://localhost:11434/api/tags

# Test model directly
curl -X POST http://localhost:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{"model": "tinyllama", "prompt": "Hello", "stream": false}'

# Restart Ollama if needed
ollama serve
```

### Debug Configuration

#### Enable Debug Logging
```properties
# logback.xml configuration
<logger name="io.amcp.examples.meshchat" level="DEBUG"/>
<logger name="io.amcp.core" level="INFO"/>
<logger name="io.amcp.connectors" level="INFO"/>
```

#### Monitor System Metrics
```java
// Add metrics collection
@Component
public class MeshChatMetrics {
    private final Counter messageCounter = Counter.build()
        .name("meshchat_messages_total")
        .help("Total messages processed")
        .register();
    
    private final Histogram responseTime = Histogram.build()
        .name("meshchat_response_time_seconds")
        .help("Response time distribution")
        .register();
}
```

### Performance Optimization

#### 1. Memory Management
```java
// Optimize conversation cleanup
memorySystem.cleanupOldSessions(TimeUnit.HOURS.toMillis(24));

// Limit session history
if (session.getMessages().size() > 100) {
    session.summarizeAndTruncate(50);
}
```

#### 2. Event Processing Optimization
```java
// Use async processing
@Override
public CompletableFuture<Void> handleEvent(Event event) {
    return CompletableFuture.runAsync(() -> {
        processEventAsync(event);
    }, executorService);
}
```

#### 3. Response Caching
```java
// Cache frequent responses
private final LoadingCache<String, String> responseCache = Caffeine.newBuilder()
    .maximumSize(1000)
    .expireAfterWrite(1, TimeUnit.HOURS)
    .build(this::generateResponse);
```

### Monitoring and Alerts

#### System Health Checks
```java
@Component
public class MeshChatHealthCheck {
    public boolean isHealthy() {
        return agentRegistry.getActiveAgents().size() >= 3 &&
               eventBroker.isConnected() &&
               memorySystem.isOperational();
    }
}
```

#### Alert Configuration
```yaml
# Prometheus alerts
groups:
- name: meshchat
  rules:
  - alert: MeshChatAgentDown
    expr: meshchat_active_agents < 3
    for: 5m
    annotations:
      summary: "MeshChat agent down"
      description: "Less than 3 agents active for 5 minutes"
```

---

## Support and Resources

### Documentation Links
- [AMCP v1.5 Technical Specification](Agent Mesh Communication Protocol - Technical Specification v1.pdf)
- [Enhanced Orchestrator Documentation](docs/ORCHESTRATOR_IMPLEMENTATION_COMPLETE.md)
- [Ollama Integration Guide](OLLAMA_INTEGRATION_GUIDE.md)
- [Multi-Agent System Guide](MULTIAGENT_SYSTEM_GUIDE.md)

### Community Resources
- **GitHub Repository**: https://github.com/xaviercallens/amcp-enterpriseedition
- **Issue Tracking**: GitHub Issues for bug reports and feature requests
- **Discussions**: GitHub Discussions for community questions and ideas

### Professional Support
For enterprise support and consulting services, contact the AMCP development team through the GitHub repository.

---

**Last Updated**: January 2025  
**Version**: 1.5.0 Enterprise Edition  
**Authors**: AMCP Development Team