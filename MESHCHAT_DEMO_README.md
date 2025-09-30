# AMCP v1.5 MeshChat - Comprehensive Demo Guide

## Overview

This demo showcases the **Agent Mesh Communication Protocol (AMCP) v1.5 Enterprise Edition** MeshChat system - a comprehensive human-to-AI conversation platform with intelligent multi-agent orchestration.

## Architecture

The MeshChat system implements a sophisticated agent ecosystem:

```
Human User
    ↓ (Natural Language)
MeshChatAgent (Gateway)
    ↓ (Event-Driven Messaging)  
EnhancedOrchestratorAgent (LLM-Powered Router)
    ↓ (Intelligent Delegation)
┌─────────────────┬─────────────────┬─────────────────┐
│ TravelPlanner   │ StockAgent      │ Future Agents   │
│ Agent           │                 │ (Weather, News) │
└─────────────────┴─────────────────┴─────────────────┘
```

## Key Features

✅ **Human-to-AI Gateway**: Natural language conversation interface  
✅ **LLM Integration**: TinyLlama via Ollama for intelligent task routing  
✅ **Multi-Agent Orchestration**: Automatic delegation to specialist agents  
✅ **Conversation Memory**: Persistent session management with context  
✅ **Agent Discovery**: Dynamic agent registration and capability matching  
✅ **Event-Driven Architecture**: Asynchronous, scalable communication  
✅ **Enterprise Ready**: Security, monitoring, Kubernetes deployment  

## Demo Modes

### 1. Interactive Mode (Default)
```bash
./run-meshchat-full-demo.sh
# or
./run-meshchat-full-demo.sh interactive
```

Start an interactive CLI session where you can:
- Have natural conversations with AI agents
- Ask complex questions spanning multiple domains
- View conversation history and session information
- See real-time agent coordination

**Sample Commands:**
- `"Plan a 2-week trip to Japan in spring with $3000 budget"`
- `"Analyze Apple, Google, and Microsoft stocks for investment"`
- `"I need help with a business trip to Tokyo and want to invest in Japanese tech"`

### 2. Automated Scenarios
```bash
./run-meshchat-full-demo.sh scenarios
```

Run pre-configured demonstration scenarios:

**Travel Scenario**: Travel planning with destination recommendations
**Stock Scenario**: Financial analysis with investment advice  
**Multi-Agent Scenario**: Complex orchestration across multiple agents

### 3. Architecture Overview
```bash
./run-meshchat-full-demo.sh architecture
```

Display the system architecture diagram and component relationships.

### 4. Technical Highlights
```bash
./run-meshchat-full-demo.sh technical
```

Show detailed technical features and implementation highlights.

## Prerequisites

- **Java 17+**: Required for AMCP framework
- **Maven 3.8+**: For building the project
- **Ollama (Optional)**: For real LLM integration
  ```bash
  # Install Ollama
  curl -fsSL https://ollama.ai/install.sh | sh
  
  # Pull TinyLlama model
  ollama pull tinyllama
  ```

## Quick Start

1. **Clone and Build**:
   ```bash
   git clone <repository-url>
   cd amcp-v1.5-enterprise-edition
   mvn clean package -DskipTests=true
   ```

2. **Run Demo**:
   ```bash
   ./run-meshchat-full-demo.sh
   ```

3. **Start Chatting**:
   ```
   💬 MeshChat > Plan a trip to Barcelona for 5 days
   
   🤖 I'll help you plan your Barcelona trip! Let me coordinate with our travel specialist...
   
   ✈️ Barcelona 5-Day Itinerary:
   Day 1: Sagrada Familia & Gothic Quarter
   Day 2: Park Güell & Gràcia neighborhood
   ...
   ```

## Advanced Usage

### Custom Scenarios
```bash
java -cp "core/target/classes:examples/target/classes:connectors/target/classes" \
     io.amcp.examples.meshchat.MeshChatCLI scenario travel "Your custom query"
```

### Configuration
Modify `demo-session.properties` for custom settings:
```properties
amcp.event.broker.type=memory
amcp.orchestrator.llm.provider=ollama
amcp.orchestrator.llm.model=tinyllama
amcp.memory.enabled=true
```

## Agent Capabilities

### MeshChatAgent
- Human conversation interface
- Session management  
- Message routing coordination
- Conversation memory integration

### TravelPlannerAgent  
- 11 global destinations database
- Itinerary planning algorithms
- Budget optimization
- Booking guidance and recommendations

### StockAgent
- Real-time market simulation
- Investment analysis and advice
- Portfolio optimization
- Cryptocurrency insights

### EnhancedOrchestratorAgent
- LLM-powered task analysis
- Intelligent agent routing
- Multi-agent coordination
- Fallback planning

## Enterprise Features

- **Security**: Role-based access control, authentication context
- **Scalability**: Event-driven architecture with pluggable brokers
- **Monitoring**: Structured logging, metrics, health checks
- **Deployment**: Kubernetes manifests, Docker containers
- **Interoperability**: CloudEvents compliance, A2A bridge

## Troubleshooting

**Build Issues**:
```bash
mvn clean compile  # Verify compilation
mvn dependency:resolve  # Check dependencies
```

**Runtime Issues**:
- Check Java version: `java -version`
- Verify classpath in error messages
- Review logs in `demo-session.properties`

**Ollama Connection**:
- Check service: `ollama serve`
- Test model: `ollama run tinyllama "Hello"`
- Fallback: Demo works with simulated responses

## Next Steps

1. **Extend Agents**: Add WeatherAgent, NewsAgent, CalendarAgent
2. **Custom Integration**: Integrate with your LLM or tools
3. **Production Deploy**: Use Kubernetes manifests in `deploy/k8s/`
4. **Monitoring**: Enable Prometheus/Grafana stack

## Support

- **Documentation**: `/docs` folder for detailed guides
- **Examples**: `/examples` for agent implementation patterns  
- **Tests**: `/core/src/test` for testing examples

---

🚀 **Ready to experience the future of multi-agent AI conversation?**

Run `./run-meshchat-full-demo.sh` and start chatting!