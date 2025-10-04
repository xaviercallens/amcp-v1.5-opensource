# Ollama + TinyLlama Setup for AMCP ChatMesh Agent

## Overview
This guide provides complete setup instructions for integrating Ollama with TinyLlama to enable AI-powered chat functionality in the AMCP ChatMesh agent.

## Installation Status ✅

### Ollama Installation
- **Version**: 0.12.3
- **Status**: ✅ Installed and running
- **Service**: ✅ Enabled and started
- **Location**: System-wide installation

### TinyLlama Model
- **Model**: tinyllama:latest
- **ID**: 2644915ede35
- **Size**: 637 MB
- **Status**: ✅ Downloaded and ready

## Configuration

### 1. Ollama Service Configuration

#### Check Service Status
```bash
sudo systemctl status ollama
```

#### Start/Stop/Restart Service
```bash
sudo systemctl start ollama
sudo systemctl stop ollama
sudo systemctl restart ollama
```

#### Enable Auto-start on Boot
```bash
sudo systemctl enable ollama
```

### 2. Ollama API Configuration

#### Default Settings
- **Host**: `localhost`
- **Port**: `11434`
- **API Endpoint**: `http://localhost:11434`
- **Model API**: `http://localhost:11434/api/generate`
- **Chat API**: `http://localhost:11434/api/chat`

#### Test API Connection
```bash
curl http://localhost:11434/api/tags
```

Expected response:
```json
{
  "models": [
    {
      "name": "tinyllama:latest",
      "model": "tinyllama:latest",
      "modified_at": "2025-10-04T13:43:01.123456789Z",
      "size": 637000000,
      "digest": "2644915ede35..."
    }
  ]
}
```

### 3. TinyLlama Model Configuration

#### Model Information
- **Name**: `tinyllama`
- **Full Name**: `tinyllama:latest`
- **Parameters**: ~1.1B parameters
- **Context Length**: 2048 tokens
- **Use Case**: Lightweight conversational AI

#### Test Model Interaction
```bash
# Interactive chat
ollama run tinyllama

# Single prompt test
ollama run tinyllama "Hello, how are you?"

# API test
curl -X POST http://localhost:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "model": "tinyllama",
    "prompt": "Hello, how are you?",
    "stream": false
  }'
```

## AMCP ChatMesh Agent Integration

### 1. Environment Configuration

Create or update your environment configuration:

```bash
# Create .env file in project root
cat > /home/kalxav/CascadeProjects/amcp-v1.5-opensource/.env << 'EOF'
# Ollama Configuration
OLLAMA_HOST=localhost
OLLAMA_PORT=11434
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=tinyllama
OLLAMA_TIMEOUT=30000

# Chat Agent Configuration
CHAT_AGENT_ENABLED=true
CHAT_AGENT_MODEL=tinyllama
CHAT_AGENT_MAX_TOKENS=512
CHAT_AGENT_TEMPERATURE=0.7
CHAT_AGENT_CONTEXT_LENGTH=2048

# API Keys (optional for enhanced features)
# OPENAI_API_KEY=your_openai_key_here
# ANTHROPIC_API_KEY=your_anthropic_key_here
EOF
```

### 2. Java System Properties

For running the AMCP CLI with Ollama integration:

```bash
java -jar cli/target/amcp-cli-1.5.0.jar \
  -Dollama.host=localhost \
  -Dollama.port=11434 \
  -Dollama.model=tinyllama \
  -Dchat.agent.enabled=true
```

### 3. Application Properties

Create `application.properties` in `cli/src/main/resources/`:

```properties
# Ollama Configuration
ollama.host=localhost
ollama.port=11434
ollama.base-url=http://localhost:11434
ollama.model=tinyllama
ollama.timeout=30000
ollama.max-retries=3

# Chat Agent Settings
chat.agent.enabled=true
chat.agent.model=tinyllama
chat.agent.max-tokens=512
chat.agent.temperature=0.7
chat.agent.context-length=2048
chat.agent.system-prompt=You are a helpful AI assistant integrated with the AMCP agent mesh system.

# Logging
logging.level.io.amcp.examples.meshchat=DEBUG
logging.level.io.amcp.connectors.ollama=DEBUG
```

## Testing the Integration

### 1. Start Ollama Service
```bash
sudo systemctl start ollama
```

### 2. Verify Model Availability
```bash
ollama list
# Should show: tinyllama:latest
```

### 3. Test Basic Model Response
```bash
curl -X POST http://localhost:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "model": "tinyllama",
    "prompt": "Explain what an AI agent is in simple terms.",
    "stream": false,
    "options": {
      "temperature": 0.7,
      "max_tokens": 200
    }
  }'
```

### 4. Start AMCP CLI and Test Chat Agent
```bash
# Navigate to project directory
cd /home/kalxav/CascadeProjects/amcp-v1.5-opensource

# Start AMCP CLI
java -jar cli/target/amcp-cli-1.5.0.jar

# In the CLI, run these commands:
agents                    # List available agents
activate chat            # Activate the chat agent
status                   # Check agent status
chat "Hello, can you help me?"  # Test chat functionality
```

## Troubleshooting

### Common Issues and Solutions

#### 1. Ollama Service Not Running
```bash
# Check status
sudo systemctl status ollama

# If not running, start it
sudo systemctl start ollama

# Check logs
sudo journalctl -u ollama -f
```

#### 2. Model Not Found
```bash
# List available models
ollama list

# If tinyllama not listed, pull it again
ollama pull tinyllama
```

#### 3. Connection Refused
```bash
# Check if Ollama is listening on correct port
netstat -tlnp | grep 11434

# Test connection
curl http://localhost:11434/api/tags
```

#### 4. Chat Agent Activation Fails
```bash
# Check AMCP logs for specific error messages
# Common issues:
# - Ollama service not running
# - Model not available
# - Network connectivity issues
# - Configuration mismatch
```

### Performance Optimization

#### 1. Memory Settings
```bash
# For systems with limited RAM, configure Ollama memory usage
export OLLAMA_MAX_LOADED_MODELS=1
export OLLAMA_NUM_PARALLEL=1
```

#### 2. Model Parameters
```json
{
  "model": "tinyllama",
  "options": {
    "temperature": 0.7,
    "top_p": 0.9,
    "top_k": 40,
    "repeat_penalty": 1.1,
    "num_ctx": 2048
  }
}
```

## Advanced Configuration

### 1. Custom Model Parameters
Create a custom Modelfile for fine-tuned behavior:

```bash
# Create custom Modelfile
cat > Modelfile << 'EOF'
FROM tinyllama

PARAMETER temperature 0.7
PARAMETER top_p 0.9
PARAMETER top_k 40
PARAMETER repeat_penalty 1.1

SYSTEM """
You are an AI assistant integrated with the AMCP (Agent Mesh Communication Protocol) system. 
You help users interact with multiple specialized agents including travel planning, stock analysis, 
and weather information. Be concise, helpful, and professional in your responses.
"""
EOF

# Create custom model
ollama create amcp-chat -f Modelfile
```

### 2. Multiple Model Support
```bash
# Pull additional models for different use cases
ollama pull llama2:7b-chat     # Larger model for complex conversations
ollama pull codellama:7b       # Code-focused model
ollama pull mistral:7b         # Alternative conversational model
```

### 3. API Rate Limiting and Monitoring
```bash
# Monitor Ollama performance
ollama ps                      # Show running models
htop                          # Monitor system resources
```

## Integration Testing Commands

### Complete Test Sequence
```bash
# 1. Verify Ollama installation
ollama --version

# 2. Check service status
sudo systemctl status ollama

# 3. List available models
ollama list

# 4. Test model directly
ollama run tinyllama "Test message"

# 5. Test API endpoint
curl -X POST http://localhost:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{"model": "tinyllama", "prompt": "Hello", "stream": false}'

# 6. Start AMCP CLI
java -jar cli/target/amcp-cli-1.5.0.jar

# 7. In CLI: Test agent activation
# agents
# activate chat
# status
# chat "Hello, I'm testing the integration!"
```

## Expected Results

When properly configured, you should see:

1. **Ollama Service**: Running on port 11434
2. **TinyLlama Model**: Available and responsive
3. **AMCP Chat Agent**: Successfully activated
4. **Chat Functionality**: Working with AI responses
5. **Integration**: Seamless communication between AMCP and Ollama

## Support and Resources

- **Ollama Documentation**: https://ollama.com/docs
- **TinyLlama Model**: https://ollama.com/library/tinyllama
- **AMCP Project**: Local documentation in project directory
- **Troubleshooting**: Check logs in `/var/log/ollama/` and AMCP CLI output

---

**Setup Complete!** 🎉

Your Ollama + TinyLlama integration is now ready for testing with the AMCP ChatMesh agent.
