# OLLAMA Integration for AMCP v1.5 Enterprise Edition

## Overview

This integration provides local OLLAMA model support for the Agent Mesh Communication Protocol (AMCP) v1.5 Enterprise Edition. It enables AMCP agents to leverage local Large Language Models (LLMs) running through OLLAMA for AI-powered capabilities without requiring cloud services.

## Architecture Components

### OllamaConnectorConfig
Configuration management for OLLAMA service connection:
- HTTP client configuration with connection pooling
- Health monitoring and timeout settings
- Configurable request/response handling
- Resource cleanup and lifecycle management

### OllamaSpringAIConnector
AMCP ToolConnector implementation for OLLAMA integration:
- Implements `ToolConnector` interface for seamless integration
- HTTP-based communication with OLLAMA REST API
- Request/response transformation and error handling
- Asynchronous operation support with `CompletableFuture`

### AIChatAgent
Intelligent chat agent with multi-agent orchestration capabilities:
- Agent interface implementation with lifecycle management
- Conversation context and history management
- Intent detection and agent orchestration
- Integration with existing AMCP agents (Travel, Weather, Stock)

### OllamaIntegrationDemo
Interactive demonstration application showcasing:
- Basic OLLAMA connectivity testing
- Simple chat interface with AI responses
- Command-based interaction (/help, /model, /exit)
- Error handling and user feedback

## Prerequisites

### 1. OLLAMA Installation
Install OLLAMA on your local machine:

**macOS:**
```bash
brew install ollama
```

**Linux:**
```bash
curl -fsSL https://ollama.ai/install.sh | sh
```

**Windows:**
Download and install from https://ollama.ai/

### 2. Start OLLAMA Service
```bash
ollama serve
```
This starts OLLAMA on `http://localhost:11434` by default.

### 3. Download a Model
```bash
# Download Llama 2 (recommended for development)
ollama pull llama2

# Alternative models
ollama pull codellama        # For code generation
ollama pull mistral          # For general chat
ollama pull llama2:13b       # Larger model for better quality
```

### 4. Verify Installation
```bash
# List installed models
ollama list

# Test with a simple prompt
ollama run llama2 "Hello, how are you?"
```

## Configuration

### Default Configuration
The OLLAMA connector uses the following default settings:
- **Endpoint**: `http://localhost:11434`
- **Connection Timeout**: 30 seconds
- **Read Timeout**: 120 seconds
- **Max Retries**: 3
- **Health Check Interval**: 60 seconds

### Custom Configuration
Create a `config/ollama.properties` file to customize settings:
```properties
ollama.endpoint=http://localhost:11434
ollama.connection.timeout=30000
ollama.read.timeout=120000
ollama.max.retries=3
ollama.health.check.interval=60000
ollama.default.model=llama2
```

## Usage Examples

### 1. Running the Demo
```bash
cd /path/to/amcp-v1.5-enterprise-edition

# Compile the project
mvn clean compile

# Run the OLLAMA integration demo
java -cp "connectors/target/classes:core/target/classes" \
  io.amcp.connectors.ollama.OllamaIntegrationDemo
```

### 2. Interactive Chat
```
=== Interactive OLLAMA Chat Demo ===
You: Hello! What is AMCP?
AI: AMCP (Agent Mesh Communication Protocol) is a distributed agent framework that enables intelligent agents to communicate and collaborate in a mesh network...

You: /help
Available commands:
  /help - Show this help message
  /model - Show information about the current OLLAMA model
  /exit - Exit the demo

You: /exit
Goodbye!
```

### 3. Integrating in Custom Agents
```java
public class MyCustomAgent extends AbstractAgent {
    private final OllamaSpringAIConnector ollamaConnector;
    
    public MyCustomAgent() {
        OllamaConnectorConfig config = new OllamaConnectorConfig();
        this.ollamaConnector = new OllamaSpringAIConnector(config);
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        if (event.getTopic().startsWith("ai.request")) {
            return processAIRequest(event);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    private CompletableFuture<Void> processAIRequest(Event event) {
        String prompt = event.getPayload(String.class);
        
        ToolRequest request = new ToolRequest("chat", Map.of(
            "model", "llama2",
            "prompt", prompt,
            "temperature", 0.7
        ));
        
        return ollamaConnector.invoke(request)
            .thenAccept(response -> {
                if (response.isSuccess()) {
                    publishEvent("ai.response", response.getData());
                } else {
                    publishEvent("ai.error", response.getErrorMessage());
                }
            });
    }
}
```

## API Reference

### OllamaSpringAIConnector Methods

#### `invoke(ToolRequest request)`
Executes an OLLAMA model request.

**Parameters:**
- `request`: ToolRequest with operation "chat" and parameters:
  - `model`: Model name (e.g., "llama2")
  - `prompt`: Input text for the model
  - `temperature`: Randomness control (0.0-1.0)
  - `max_tokens`: Maximum response length

**Returns:** `CompletableFuture<ToolResponse>` containing the model response

### Supported Operations

#### Chat Completion
```java
ToolRequest chatRequest = new ToolRequest("chat", Map.of(
    "model", "llama2",
    "prompt", "Explain quantum computing",
    "temperature", 0.7,
    "max_tokens", 500
));
```

## Advanced Features

### 1. Multi-Agent Orchestration
The AIChatAgent can orchestrate multiple specialized agents:

```java
// Intent detection routes to appropriate agents
if (intent.contains("travel")) {
    return delegateToTravelAgent(userMessage);
} else if (intent.contains("weather")) {
    return delegateToWeatherAgent(userMessage);
} else if (intent.contains("stock")) {
    return delegateToStockAgent(userMessage);
}
```

### 2. Conversation Context Management
Maintains conversation history for coherent multi-turn dialogs:

```java
private final Map<String, ConversationContext> conversations = new ConcurrentHashMap<>();

public void addToConversation(String sessionId, String role, String content) {
    conversations.computeIfAbsent(sessionId, k -> new ConversationContext())
        .addMessage(role, content);
}
```

### 3. Health Monitoring
Automatic health checks ensure OLLAMA service availability:

```java
// Periodic health checks
CompletableFuture<Boolean> healthCheck = config.checkHealthAsync();
healthCheck.thenAccept(healthy -> {
    if (!healthy) {
        logger.warn("OLLAMA service health check failed");
    }
});
```

## Performance Considerations

### Model Selection
- **llama2**: Good balance of speed and quality for general use
- **llama2:7b**: Faster responses, lower memory usage
- **llama2:13b**: Better quality responses, higher resource requirements
- **codellama**: Optimized for code generation tasks

### Optimization Tips
1. **Use appropriate temperature values:**
   - 0.1-0.3: Focused, deterministic responses
   - 0.5-0.7: Balanced creativity and coherence
   - 0.8-1.0: More creative but potentially less coherent

2. **Set reasonable token limits:**
   - Short responses: 50-200 tokens
   - Medium responses: 200-500 tokens
   - Long responses: 500-1000+ tokens

3. **Connection pooling:**
   - The connector automatically manages HTTP connections
   - Reuse connector instances when possible

## Troubleshooting

### Common Issues

#### 1. "Connection refused" error
```
Caused by: java.net.ConnectException: Connection refused
```
**Solution:** Ensure OLLAMA service is running: `ollama serve`

#### 2. "Model not found" error
```
{"error":"model 'llama2' not found"}
```
**Solution:** Download the model: `ollama pull llama2`

#### 3. Timeout errors
```
java.util.concurrent.TimeoutException
```
**Solution:** Increase timeout in configuration or use a smaller model

#### 4. Out of memory errors
**Solution:** 
- Use smaller models (7b instead of 13b)
- Reduce max_tokens parameter
- Ensure sufficient system RAM

### Debug Logging
Enable debug logging for detailed troubleshooting:

```java
// Add to your logging configuration
Logger.getLogger("io.amcp.connectors.ai").setLevel(Level.DEBUG);
```

### Health Check Commands
```bash
# Test OLLAMA service
curl http://localhost:11434/api/tags

# Test specific model
curl -X POST http://localhost:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{"model": "llama2", "prompt": "Hello", "stream": false}'
```

## Security Considerations

### Network Security
- OLLAMA runs locally by default (no external API calls)
- Configure firewall rules if exposing OLLAMA service
- Use HTTPS for remote OLLAMA deployments

### Data Privacy
- All model interactions happen locally
- No data sent to external services
- Consider data encryption for sensitive prompts

### Authentication
```java
// For secured OLLAMA deployments
OllamaConnectorConfig config = new OllamaConnectorConfig();
config.setAuthToken("your-api-token");
config.setUseHttps(true);
```

## Integration with AMCP Ecosystem

### Event-Driven Architecture
The OLLAMA integration follows AMCP's event-driven patterns:

```java
// Subscribe to AI-related events
agent.subscribe("ai.request.*");
agent.subscribe("ai.assistance.*");

// Publish AI responses
publishEvent("ai.response.generated", aiResponse);
publishEvent("ai.analysis.complete", analysisResult);
```

### Tool Integration
Register OLLAMA as a tool connector:

```java
ToolManager toolManager = context.getToolManager();
toolManager.registerConnector("ollama", new OllamaSpringAIConnector(config));
```

### Federation Support
OLLAMA agents can participate in AMCP federations:

```java
// Create AI processing federation
List<AgentID> federationMembers = Arrays.asList(
    aiChatAgent.getAgentId(),
    travelAgent.getAgentId(),
    weatherAgent.getAgentId()
);
agent.federateWith(federationMembers, "ai-assistance-federation");
```

## Future Enhancements

### Planned Features
1. **Multi-model support**: Switch between models dynamically
2. **Streaming responses**: Real-time response generation
3. **Fine-tuning integration**: Custom model training support
4. **Embeddings support**: Vector search and semantic similarity
5. **Plugin ecosystem**: Extend with custom OLLAMA plugins

### Community Contributions
We welcome contributions to enhance the OLLAMA integration:

1. **Model adapters**: Support for new OLLAMA models
2. **Performance optimizations**: Caching and batching improvements
3. **UI components**: Web interface for OLLAMA agent management
4. **Documentation**: Additional examples and tutorials

## Related Documentation

- [AMCP v1.5 Core Documentation](../../../README.md)
- [Agent Development Guide](../../../docs/agent-development.md)
- [Tool Connector API](../../../docs/tool-connector-api.md)
- [OLLAMA Official Documentation](https://ollama.ai/docs)

## License

This OLLAMA integration is part of the AMCP v1.5 Enterprise Edition and is licensed under the same terms as the main project.

---

**Note:** This integration is designed for AMCP v1.5 Enterprise Edition. For compatibility with other versions, please refer to the version-specific documentation.