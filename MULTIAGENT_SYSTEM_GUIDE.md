# AMCP v1.5 Multi-Agent Communication System

## Overview

The AMCP v1.5 Multi-Agent Communication System demonstrates advanced agent orchestration with the Enhanced Chat Agent coordinating conversations between specialized Weather, Travel, and Stock agents via the Agent Mesh Communication Protocol (AMCP).

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    AMCP v1.5 Multi-Agent System                │
└─────────────────────────────────────────────────────────────────┘
                                   │
                          ┌────────▼────────┐
                          │ Enhanced Chat   │
                          │     Agent       │ ◄─── User Requests
                          │ (Orchestrator)  │
                          └─────────────────┘
                                   │
                        ┌──────────┼──────────┐
                        │          │          │
                        ▼          ▼          ▼
                ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
                │ Weather      │ │ Travel       │ │ Stock Price  │
                │ Agent        │ │ Planner      │ │ Agent        │
                │              │ │ Agent        │ │              │
                └──────────────┘ └──────────────┘ └──────────────┘
                        │          │          │
                        └──────────┼──────────┘
                                   │
                          ┌────────▼────────┐
                          │ AMCP Event Bus  │
                          │ (Message Mesh)  │
                          └─────────────────┘
```

## Core Components

### 1. Enhanced Chat Agent (`EnhancedChatAgent`)
- **Role**: Central orchestrator and conversation manager
- **Capabilities**:
  - Natural language processing with OLLAMA integration
  - Intelligent request routing via PromptManager
  - Agent discovery through AgentRegistry
  - Multi-agent response synthesis
  - A2A communication coordination

### 2. Agent Registry (`AgentRegistry`)
- **Role**: Agent discovery and capability matching system
- **Features**:
  - Dynamic agent registration and heartbeat monitoring
  - Capability-based agent lookup
  - Service mesh topology management
  - Agent health and status tracking

### 3. Prompt Manager (`PromptManager`)
- **Role**: Intelligent routing and intent analysis
- **Capabilities**:
  - Intent pattern matching for weather/stock/travel requests
  - Parameter extraction and validation
  - Confidence scoring and agent selection
  - Request optimization and prompt engineering

### 4. Specialized Agents

#### Weather Agent
- **Capabilities**: `weather.current`, `weather.forecast`, `weather.alerts`
- **Examples**:
  - "What's the weather in Nice?"
  - "Weather forecast for London this week"
  - "Is it raining in Paris?"

#### Travel Planner Agent
- **Capabilities**: `travel.planning`, `travel.booking`, `travel.itinerary`
- **Examples**:
  - "Plan a 3-day trip to Rome"
  - "Find flights to Barcelona"
  - "Create an itinerary for Tokyo"

#### Stock Price Agent
- **Capabilities**: `stock.price`, `market.data`, `portfolio.tracking`
- **Examples**:
  - "Stock price of Amadeus"
  - "LVMH current price"
  - "Market summary"

## Communication Flow

### Request Processing Flow
```
1. User Input → Enhanced Chat Agent
2. Enhanced Chat Agent → PromptManager (Intent Analysis)
3. PromptManager → AgentRegistry (Agent Discovery)
4. Enhanced Chat Agent → Target Agent (AMCP Event)
5. Target Agent → Response Processing
6. Response → Enhanced Chat Agent (Response Synthesis)
7. Final Response → User
```

### AMCP Event Topics

#### Chat Events
- `chat.request.message` - User chat requests
- `chat.response.final` - Final synthesized responses

#### Agent-Specific Events
- `weather.request` / `weather.response` - Weather queries
- `travel.request` / `travel.response` - Travel planning
- `stock.request` / `stock.response` - Stock information

#### Discovery Events
- `agent.discovery.request` - Agent capability queries
- `agent.heartbeat` - Agent health monitoring

## Quick Start

### 1. Run Predefined Demo
```bash
./run-multiagent-demo.sh
```

This runs 5 predefined conversations demonstrating:
- Weather queries: "Hello chat provide me the weather in Nice"
- Stock queries: "What is the stock price of Amadeus"
- Travel planning: "Help me to organize and plan my trip to Rome for 3 days"
- Multi-agent coordination: Complex requests involving multiple agents

### 2. Interactive Mode
```bash
./run-multiagent-demo.sh interactive
```

Interactive mode allows you to:
- Type natural language questions
- See real-time agent coordination
- Test various request patterns
- Exit with 'exit' command

## Example Conversations

### Single Agent Requests

**Weather Query:**
```
👤 User: What's the weather in Nice?
🤖 EnhancedChatAgent: [Routes to WeatherAgent]
🌤️  WeatherAgent: Current weather in Nice: 22°C, partly cloudy, 15 km/h winds
```

**Stock Query:**
```
👤 User: What is the stock price of Amadeus?
🤖 EnhancedChatAgent: [Routes to StockPriceAgent]
📈 StockPriceAgent: Amadeus IT Group SA (AMADEUS): 68.50 EUR 🟢 +1.25 EUR (+1.86%)
```

**Travel Planning:**
```
👤 User: Plan a 3-day trip to Rome
🤖 EnhancedChatAgent: [Routes to TravelPlannerAgent]
✈️  TravelPlannerAgent: Rome 3-Day Itinerary:
Day 1: Colosseum, Roman Forum
Day 2: Vatican City, Sistine Chapel
Day 3: Trevi Fountain, Spanish Steps
```

### Multi-Agent Coordination

**Complex Request:**
```
👤 User: What's the weather in Paris and current stock prices for LVMH?
🤖 EnhancedChatAgent: [Coordinates WeatherAgent + StockPriceAgent]
🌤️  WeatherAgent: Paris weather: 18°C, light rain, 10 km/h winds
📈 StockPriceAgent: LVMH: 725.80 EUR 🔴 -12.40 EUR (-1.68%)
🤖 EnhancedChatAgent: [Synthesizes responses]
```

## Configuration

### Agent Registry Configuration
```java
// Automatic registration of core agents
agentRegistry.registerAgent("WeatherAgent", Arrays.asList(
    "weather.current", "weather.forecast", "weather.alerts"));
agentRegistry.registerAgent("TravelPlannerAgent", Arrays.asList(
    "travel.planning", "travel.booking", "travel.itinerary"));
agentRegistry.registerAgent("StockPriceAgent", Arrays.asList(
    "stock.price", "market.data", "portfolio.tracking"));
```

### Prompt Manager Intent Patterns
```java
// Weather patterns
"weather|temperature|forecast|rain|sunny|cloudy"

// Stock patterns  
"stock|price|market|trading|shares|equity"

// Travel patterns
"travel|trip|vacation|flight|hotel|itinerary"
```

## Development

### Adding New Agents

1. **Create Agent Class** implementing the `Agent` interface
2. **Register Capabilities** in AgentRegistry
3. **Add Intent Patterns** in PromptManager
4. **Define AMCP Topics** for communication
5. **Update Demo** with example conversations

### Custom Intent Patterns

```java
// Add new intent pattern to PromptManager
promptPatterns.put("CUSTOM_INTENT", Arrays.asList(
    "custom|special|unique",
    "my custom patterns"
));

// Register corresponding agent
agentRegistry.registerAgent("CustomAgent", Arrays.asList(
    "custom.capability.1",
    "custom.capability.2"
));
```

## Monitoring and Debugging

### Event Flow Logging
All AMCP events are logged with:
- Timestamp
- Agent source/target
- Topic and correlation ID
- Payload summary

### Agent Health Monitoring
- Heartbeat events every 30 seconds
- Agent status tracking (ACTIVE/INACTIVE)
- Capability availability monitoring

### Performance Metrics
- Request routing time
- Agent response latencies
- Event throughput statistics
- Conversation success rates

## Troubleshooting

### Common Issues

**Agent Not Responding:**
- Check agent activation status
- Verify topic subscriptions
- Review event routing configuration

**Intent Not Recognized:**
- Update PromptManager patterns
- Check parameter extraction logic
- Verify agent capability registration

**OLLAMA Connection Issues:**
- Ensure OLLAMA is running on localhost:11434
- Verify TinyLlama model is available
- Check connector configuration

### Debug Mode
Enable verbose logging by setting:
```bash
export AMCP_DEBUG=true
./run-multiagent-demo.sh
```

## Advanced Features

### Multi-Tenant Support
- Context-based agent isolation
- Tenant-specific event routing
- Security boundary enforcement

### Load Balancing
- Multiple agent instances
- Request distribution strategies
- Failover and redundancy

### State Persistence
- Conversation history storage
- Agent state migration
- Event replay capabilities

## API Reference

### Enhanced Chat Agent API
```java
// Direct chat interaction
CompletableFuture<String> chat(String message, String conversationId)

// Get conversation history
List<ChatMessage> getConversationHistory(String conversationId)

// Get available agents
CompletableFuture<Map<String, String>> getAvailableAgents()
```

### Agent Registry API
```java
// Register agent capabilities
void registerAgent(String agentId, List<String> capabilities)

// Find agents by capability
CompletableFuture<List<String>> findAgentsByCapability(String capability)

// Get agent status
AgentStatus getAgentStatus(String agentId)
```

## Integration Examples

### External API Integration
```java
// Weather API integration
WeatherConnector weatherAPI = new WeatherConnector();
String forecast = weatherAPI.getForecast(location);

// Stock API integration  
StockConnector stockAPI = new StockConnector();
StockPrice price = stockAPI.getPrice(symbol);
```

### Custom Event Handlers
```java
@Override
public CompletableFuture<Void> handleEvent(Event event) {
    return CompletableFuture.runAsync(() -> {
        // Custom event processing logic
        processCustomEvent(event);
    });
}
```

## Performance Benchmarks

### Typical Response Times
- Intent Analysis: < 50ms
- Agent Discovery: < 20ms  
- Weather Requests: < 500ms
- Stock Requests: < 300ms
- Travel Planning: < 1000ms

### Throughput Capacity
- Concurrent conversations: 100+
- Events per second: 1000+
- Agent-to-agent latency: < 100ms

## Future Enhancements

### Version 1.6 Roadmap
- Streaming responses for long conversations
- Voice interface integration
- Mobile agent migration examples
- Enhanced security with authentication tokens

### Planned Integrations
- LangChain adapter for Python agents
- Semantic Kernel integration for .NET
- Kubernetes-native deployment
- OpenTelemetry observability

---

For more information, see the [AMCP v1.5 specification](../docs/AMCP-v1.5-spec.md) and [enterprise documentation](../docs/enterprise-features.md).