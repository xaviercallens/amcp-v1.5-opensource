# 🎯 AMCP v1.5 OrchestratorAgent

## Overview

The **OrchestratorAgent** is the intelligent heart of AMCP v1.5, providing sophisticated agent orchestration powered by TinyLlama via OLLAMA. It acts as a smart middleware that analyzes user intent, discovers available agents, and routes requests to the most appropriate specialized agents in the system.

## Key Features

### 🧠 Intelligent Intent Analysis
- **TinyLlama Integration**: Uses TinyLlama via OLLAMA for natural language understanding
- **Dynamic Parameter Extraction**: Automatically extracts locations, stock symbols, and other parameters
- **Confidence-Based Routing**: Makes intelligent routing decisions based on confidence scores
- **Fallback Mechanisms**: Robust error handling with multiple fallback strategies

### 🔍 Dynamic Agent Discovery
- **Real-Time Discovery**: Automatically discovers available agents in the AMCP mesh
- **Capability Matching**: Routes requests based on agent capabilities and specializations
- **Registry Integration**: Seamless integration with the AgentRegistry system
- **Load Balancing**: Intelligent distribution of requests across available agents

### 🚀 Advanced Orchestration
- **Session Tracking**: Maintains conversation context across multiple interactions
- **Response Synthesis**: Formats and enhances responses using TinyLlama
- **Correlation Management**: Tracks requests through the entire processing pipeline
- **Timeout Handling**: Graceful handling of slow or unresponsive agents

### 🔧 Enterprise-Grade Architecture
- **Asynchronous Processing**: Non-blocking operations using CompletableFuture
- **Thread-Safe Operations**: Concurrent access with proper synchronization
- **Metrics and Observability**: Built-in statistics and monitoring capabilities
- **Configurable Timeouts**: Adjustable timeouts for different operation types

## Architecture

```
User Request
     ↓
EnhancedChatAgent
     ↓
OrchestratorAgent ←→ TinyLlama (via OLLAMA)
     ↓
AgentRegistry (Discovery)
     ↓
Intent Analysis & Routing
     ↓
Specialized Agent (Weather/Stock/Travel/etc.)
     ↓
Response Synthesis ←→ TinyLlama (formatting)
     ↓
Formatted Response to User
```

## Integration Examples

### Basic Integration

```java
// Initialize OrchestratorAgent
OrchestratorAgent orchestrator = new OrchestratorAgent();
orchestrator.setContext(agentContext);
orchestrator.onActivate();

// Process user request
CompletableFuture<String> response = orchestrator.orchestrateRequest(
    "What's the weather in Paris?", 
    "conversation-123"
);

response.thenAccept(result -> {
    System.out.println("Orchestrated Response: " + result);
});
```

### With EnhancedChatAgent

```java
// EnhancedChatAgent automatically integrates with OrchestratorAgent
EnhancedChatAgent chatAgent = new EnhancedChatAgent();
chatAgent.setContext(agentContext);
chatAgent.onActivate(); // Automatically creates and activates OrchestratorAgent

// Chat requests are automatically routed through orchestration
```

## Supported Agent Types

### WeatherAgent
- **Intent Keywords**: weather, temperature, forecast, rain, sun, climate
- **Parameters**: location (city, country, coordinates)
- **Examples**: 
  - "What's the weather in London?"
  - "Temperature in Tokyo today"
  - "Will it rain in Paris tomorrow?"

### StockPriceAgent  
- **Intent Keywords**: stock, price, shares, market, financial
- **Parameters**: symbol, company name
- **Examples**:
  - "Apple stock price"
  - "Tesla shares today"
  - "MSFT current price"

### TravelPlannerAgent
- **Intent Keywords**: travel, trip, flight, hotel, vacation
- **Parameters**: destination, dates, preferences
- **Examples**:
  - "Plan a trip to Tokyo"
  - "Find flights to New York"
  - "Hotel recommendations for London"

### General Chat
- **Fallback**: Any queries not matching specialized agents
- **Handled by**: TinyLlama direct processing
- **Examples**: General conversation, questions, help requests

## Configuration

### OLLAMA Setup

```bash
# Install and start OLLAMA
ollama serve

# Pull TinyLlama model
ollama pull tinyllama

# Verify installation
curl http://localhost:11434/api/version
```

### Agent Registry Configuration

```java
// Agents are automatically discovered and registered
// No manual configuration required for standard agents

// Custom agent registration
AgentInfo customAgent = new AgentInfo(
    "CustomAgent",
    "CustomAgentType", 
    "Description of custom agent",
    Set.of("capability1", "capability2"),
    "custom.request.topic"
);
```

### Timeout Configuration

```java
// Default timeouts (configurable)
- Intent Analysis: 15 seconds
- Agent Response: 30 seconds  
- Response Formatting: 15 seconds
- Overall Orchestration: 60 seconds
```

## Monitoring and Statistics

### Orchestration Statistics

```java
Map<String, Object> stats = orchestratorAgent.getOrchestrationStats();

// Available metrics:
// - activeSessions: Number of active orchestration sessions
// - pendingResponses: Number of pending agent responses  
// - agentId: Orchestrator agent identifier
// - lifecycleState: Current agent state
```

### Session Tracking

```java
// Each request creates an orchestration session
// Sessions track:
// - Original query
// - Detected intent
// - Target agent
// - Processing timeline
// - Final response
// - Metadata and parameters
```

## Error Handling

### Robust Fallback System

1. **Primary**: TinyLlama intent analysis and routing
2. **Secondary**: Keyword-based fallback routing
3. **Tertiary**: Default chat agent handling
4. **Final**: Static error response

### Timeout Management

- **Progressive Timeouts**: Different timeouts for different operations
- **Graceful Degradation**: System continues operating with reduced functionality
- **Error Propagation**: Clear error messages to users and logs

## Performance Considerations

### Optimization Features

- **Connection Pooling**: Reuses HTTP connections to OLLAMA
- **Response Caching**: Caches formatted responses (optional)
- **Session Cleanup**: Automatic cleanup of expired sessions
- **Resource Management**: Proper resource cleanup and memory management

### Scalability

- **Concurrent Processing**: Handles multiple requests simultaneously
- **Agent Discovery Caching**: Caches agent registry for performance
- **Asynchronous Operations**: Non-blocking throughout the pipeline
- **Configurable Thread Pools**: Adjustable concurrency levels

## Deployment

### Development Environment

```bash
# Run the orchestrator demo
cd examples
./run-orchestrator-demo.sh

# Or run specific demo
java -cp "../core/target/classes:../connectors/target/classes:target/classes" \
  io.amcp.examples.orchestrator.OrchestratorDemo
```

### Production Deployment

```bash
# Ensure OLLAMA is running in production
ollama serve --host 0.0.0.0:11434

# Deploy with proper JVM settings
java -Xmx2G -Xms1G \
  -Damcp.orchestrator.timeout=30 \
  -Damcp.ollama.url=http://ollama-server:11434 \
  -jar amcp-orchestrator.jar
```

### Docker Deployment

```dockerfile
FROM openjdk:21-jdk-slim

# Install OLLAMA
RUN curl -fsSL https://ollama.ai/install.sh | sh

# Copy AMCP files
COPY amcp-core.jar /app/
COPY amcp-connectors.jar /app/

# Start services
CMD ["sh", "-c", "ollama serve & java -jar /app/amcp-connectors.jar"]
```

## Troubleshooting

### Common Issues

**OLLAMA Connection Failed**
```bash
# Check OLLAMA status
curl http://localhost:11434/api/version

# Start OLLAMA if not running
ollama serve
```

**TinyLlama Model Not Found**
```bash
# Install TinyLlama model
ollama pull tinyllama

# List installed models
ollama list
```

**Agent Discovery Issues**
```java
// Check agent registry
Map<String, Object> stats = orchestrator.getOrchestrationStats();
System.out.println("Active Sessions: " + stats.get("activeSessions"));
```

### Debugging

**Enable Debug Logging**
```java
// Add to JVM arguments
-Damcp.orchestrator.debug=true
-Damcp.logging.level=DEBUG
```

**Monitor Agent Events**
```java
// Subscribe to orchestrator events for debugging
context.subscribe(debugAgent, "orchestrator.**");
context.subscribe(debugAgent, "agent.response.**");
```

## Best Practices

### Intent Design
- **Clear Keywords**: Use distinct keywords for different agent types
- **Parameter Extraction**: Design prompts for reliable parameter extraction  
- **Confidence Thresholds**: Set appropriate confidence levels for routing decisions

### Agent Integration
- **Response Format**: Ensure agents return well-structured responses
- **Timeout Handling**: Implement proper timeout handling in specialized agents
- **Error Messages**: Provide clear error messages for debugging

### Performance
- **Model Selection**: TinyLlama provides good balance of speed and accuracy
- **Batch Processing**: Group similar requests when possible
- **Resource Monitoring**: Monitor memory and CPU usage in production

## Future Enhancements

### Planned Features
- **Multi-Model Support**: Support for different LLM models
- **Advanced Caching**: Intelligent response caching strategies
- **Agent Learning**: Learn from user interactions to improve routing
- **Custom Routing Rules**: User-defined routing rules and preferences

### Experimental Features
- **Voice Integration**: Voice-to-text and text-to-voice capabilities
- **Context Persistence**: Long-term conversation context storage
- **Agent Recommendations**: Suggest agents based on user patterns
- **Real-time Analytics**: Advanced analytics and insights dashboard

---

**OrchestratorAgent v1.5** - Intelligent Multi-Agent Orchestration for AMCP Enterprise Edition

For support and documentation: [AMCP Enterprise Documentation](https://github.com/xaviercallens/amcp-enterpriseedition)