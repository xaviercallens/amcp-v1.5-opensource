# Quick Start: Testing ChatMesh Agent with Ollama + TinyLlama

## Prerequisites ✅
- ✅ Ollama installed (version 0.12.3)
- ✅ TinyLlama model downloaded (637 MB)
- ✅ AMCP project compiled successfully
- ✅ Environment variables configured

## Quick Test Steps

### 1. Load Environment Variables
```bash
cd /home/kalxav/CascadeProjects/amcp-v1.5-opensource
source .env
```

### 2. Test Ollama Setup (Optional)
```bash
./test_ollama.sh
```

### 3. Start AMCP CLI
```bash
java -jar cli/target/amcp-cli-1.5.0.jar
```

### 4. Test ChatMesh Agent in CLI

Once the CLI starts, run these commands:

```bash
# List all available agents
agents

# Check current status
status

# Activate the chat agent
activate chat

# Verify chat agent is active
status

# Test basic chat functionality
chat "Hello, can you introduce yourself?"

# Test agent coordination
chat "I want to plan a trip to Tokyo and check the weather there"

# Test multi-agent workflow
chat "Help me plan a business trip to New York, check weather, and analyze tech stocks"
```

## Expected Output

### 1. Agent List
```
🤖 Registered Agents:
orchestrator    ⚪ INACTIVE - Master orchestrator for complex multi-agent workflows
chat            ⚪ INACTIVE - Enhanced conversational agent with AI capabilities  
weather         ⚪ INACTIVE - Weather information with OpenWeatherMap API integration
```

### 2. Chat Agent Activation
```
🤖[1] 🟢[eventbroker] 🔴[openweather] 🔴[polygon.io] amcp> activate chat
[15:43:01] [EnhancedChatAgent] 🎯 Activating Enhanced Chat Agent...
[15:43:01] [EnhancedChatAgent] 🔗 Connected to Ollama at http://localhost:11434
[15:43:01] [EnhancedChatAgent] 🤖 Using model: tinyllama
✅ Agent 'chat' activated successfully
```

### 3. Chat Interaction
```
🤖[1] 🟢[eventbroker] 🔴[openweather] 🔴[polygon.io] amcp> chat "Hello!"
[15:43:05] [EnhancedChatAgent] 💬 Processing user message: Hello!
[15:43:06] [EnhancedChatAgent] 🤖 TinyLlama response: Hello! I'm an AI assistant integrated with the AMCP system. I can help you with various tasks including travel planning, weather information, and stock analysis. How can I assist you today?
```

## Troubleshooting

### Issue: Chat Agent Won't Activate
**Symptoms**: `❌ Failed to activate agent 'chat': java.lang.NoClassDefFoundError`

**Solutions**:
1. Check if Ollama is running:
   ```bash
   sudo systemctl status ollama
   sudo systemctl start ollama
   ```

2. Verify TinyLlama model:
   ```bash
   ollama list
   ```

3. Test Ollama API:
   ```bash
   curl http://localhost:11434/api/tags
   ```

### Issue: No Response from Chat Agent
**Symptoms**: Chat command hangs or returns empty response

**Solutions**:
1. Check Ollama logs:
   ```bash
   sudo journalctl -u ollama -f
   ```

2. Test model directly:
   ```bash
   ollama run tinyllama "Test message"
   ```

3. Restart Ollama service:
   ```bash
   sudo systemctl restart ollama
   ```

### Issue: Connection Refused
**Symptoms**: `Connection refused` or `Unable to connect to Ollama`

**Solutions**:
1. Check if port 11434 is open:
   ```bash
   netstat -tlnp | grep 11434
   ```

2. Verify environment variables:
   ```bash
   echo $OLLAMA_BASE_URL
   echo $OLLAMA_MODEL
   ```

## Advanced Testing

### 1. Multi-Agent Workflows
```bash
# Activate multiple agents
activate weather
activate chat
activate orchestrator

# Test complex queries
chat "Plan a 3-day trip to Paris, check the weather forecast, and suggest some tech stocks to invest in"
```

### 2. Performance Testing
```bash
# Test response times
chat "Give me a quick summary of artificial intelligence"
chat "What are the benefits of agent-based systems?"
chat "Explain machine learning in simple terms"
```

### 3. Context Testing
```bash
# Test conversation context
chat "My name is John"
chat "What's my name?"
chat "I'm planning a trip to Japan"
chat "What did I just tell you about my travel plans?"
```

## Configuration Tuning

### Model Parameters
Edit `.env` file to adjust TinyLlama behavior:

```bash
# More creative responses
export CHAT_AGENT_TEMPERATURE="0.9"

# More conservative responses  
export CHAT_AGENT_TEMPERATURE="0.3"

# Longer responses
export CHAT_AGENT_MAX_TOKENS="1024"

# Shorter responses
export CHAT_AGENT_MAX_TOKENS="256"
```

### Alternative Models
If you want to try different models:

```bash
# Download larger model (requires more RAM)
ollama pull llama2:7b-chat

# Update environment
export OLLAMA_MODEL="llama2:7b-chat"
export CHAT_AGENT_MODEL="llama2:7b-chat"
```

## Success Indicators

✅ **Ollama Service**: Running on port 11434  
✅ **TinyLlama Model**: Loaded and responsive  
✅ **Chat Agent**: Activates without errors  
✅ **AI Responses**: Coherent and contextual  
✅ **Multi-Agent**: Coordinates with other agents  
✅ **Performance**: Responses within 5-10 seconds  

## Next Steps

Once basic chat is working:

1. **Test Multi-Agent Coordination**: Try complex queries that require multiple agents
2. **Performance Optimization**: Tune model parameters for your use case
3. **Custom Prompts**: Modify system prompts for specific domains
4. **Integration Testing**: Test with external APIs (weather, stocks)
5. **Production Setup**: Configure for production deployment

---

**Happy Testing!** 🚀

Your ChatMesh agent is now ready to demonstrate AI-powered multi-agent conversations!
