# Ollama Performance Issues - Troubleshooting & Solutions

## Problem Diagnosed ❌

The Ollama + TinyLlama integration is experiencing severe performance issues:

- **API Response Time**: 30+ seconds (should be 1-5 seconds)
- **Token Generation Rate**: 0.21 tokens/s (extremely slow)
- **Timeout Errors**: Consistent failures in AMCP integration
- **System Impact**: High CPU usage, long model loading times

## Root Cause Analysis

### Performance Metrics Observed:
```
total duration:       8m5.327952079s
load duration:        40.483129ms
prompt eval count:    35 token(s)
prompt eval duration: 4.453771493s
prompt eval rate:     7.86 tokens/s
eval count:           102 token(s)
eval duration:        8m0.832484512s
eval rate:            0.21 tokens/s  ← EXTREMELY SLOW
```

### Likely Causes:
1. **CPU-Only Inference**: No GPU acceleration available
2. **System Resources**: Limited compute resources for AI inference
3. **Model Size**: TinyLlama (1B parameters) still too large for optimal performance
4. **Memory Constraints**: Insufficient RAM allocation for model inference
5. **Container Environment**: Running in LXC container may limit performance

## Solutions Implemented ✅

### 1. Ollama Service Optimization
```bash
# Applied optimized configuration:
OLLAMA_MAX_LOADED_MODELS=1
OLLAMA_NUM_PARALLEL=1
OLLAMA_MAX_QUEUE=2
OLLAMA_KEEP_ALIVE=2m
OLLAMA_FLASH_ATTENTION=false
OLLAMA_LLM_LIBRARY=cpu
```

### 2. System Service Configuration
- Increased timeout values
- Optimized restart policies
- Reduced parallel processing

### 3. Model Parameters Tuning
```json
{
  "num_predict": 5,
  "temperature": 0.1,
  "top_p": 0.9,
  "stop": ["\n", ".", "!"]
}
```

## Alternative Solutions 🔄

Since the performance issues persist, here are alternative approaches:

### Option 1: Use Simulation Mode (Recommended)
```bash
# Update .env to disable real AI and use simulation
export CHAT_AGENT_ENABLED="false"
export USE_SIMULATION_MODE="true"
export AI_FALLBACK_MODE="simulation"
```

### Option 2: Switch to Smaller Model
```bash
# Try an even smaller model (if available)
ollama pull phi:2.7b-mini
# or
ollama pull qwen:0.5b
```

### Option 3: Cloud-Based AI Service
```bash
# Use OpenAI API instead (requires API key)
export OPENAI_API_KEY="your_api_key_here"
export CHAT_AGENT_PROVIDER="openai"
export CHAT_AGENT_MODEL="gpt-3.5-turbo"
```

### Option 4: Mock AI Service
Create a simple mock AI service for testing:

```bash
# Start simple mock AI server on port 8080
python3 -c "
import http.server
import json
from urllib.parse import urlparse, parse_qs

class MockAIHandler(http.server.BaseHTTPRequestHandler):
    def do_POST(self):
        if self.path == '/api/generate':
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length)
            request_data = json.loads(post_data.decode('utf-8'))
            
            # Simple mock response
            response = {
                'response': f'Mock AI response to: {request_data.get(\"prompt\", \"unknown\")}',
                'done': True
            }
            
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps(response).encode())
        else:
            self.send_response(404)
            self.end_headers()

if __name__ == '__main__':
    server = http.server.HTTPServer(('localhost', 8080), MockAIHandler)
    print('Mock AI server running on http://localhost:8080')
    server.serve_forever()
" &
```

## Recommended Configuration for AMCP Testing

Given the performance constraints, here's the recommended configuration:

### Update `.env` file:
```bash
# Disable real AI integration temporarily
export CHAT_AGENT_ENABLED="false"
export USE_SIMULATION_MODE="true"
export AI_FALLBACK_MODE="simulation"

# Keep other services active
export POLYGON_API_KEY="ZGgVNySPtrCA7u1knnya3wdefCLGpJwd"
export OPENWEATHER_API_KEY="3bd965f39881ba0f116ee0810fdfd058"

# Ollama settings (for future use when performance improves)
export OLLAMA_HOST="localhost"
export OLLAMA_PORT="11434"
export OLLAMA_BASE_URL="http://localhost:11434"
export OLLAMA_MODEL="tinyllama"
export OLLAMA_TIMEOUT="60000"
export OLLAMA_ENABLED="false"  # Disabled due to performance
```

### Test AMCP with Simulation Mode:
```bash
# Load updated environment
source .env

# Start AMCP CLI
java -jar cli/target/amcp-cli-1.5.0.jar

# Test commands (should work without AI delays):
# agents
# activate weather
# activate orchestrator
# status
# weather "Paris"
# travel "Tokyo"
```

## Performance Benchmarking

### Current System Performance:
- **Model Loading**: ~40ms ✅
- **Prompt Processing**: ~4.4s ⚠️ (should be <1s)
- **Token Generation**: 0.21 tokens/s ❌ (should be >10 tokens/s)
- **Total Response Time**: 8+ minutes ❌ (should be <10s)

### Target Performance for Production:
- **Token Generation**: >10 tokens/s
- **Total Response Time**: <5 seconds
- **Model Loading**: <1 second
- **Memory Usage**: <2GB

## Hardware Recommendations

For optimal Ollama performance:

### Minimum Requirements:
- **CPU**: 8+ cores, 3.0+ GHz
- **RAM**: 8GB+ available
- **Storage**: SSD with >10GB free space

### Recommended Requirements:
- **GPU**: NVIDIA GPU with 8GB+ VRAM
- **CPU**: 16+ cores, 3.5+ GHz  
- **RAM**: 16GB+ available
- **Storage**: NVMe SSD

### Current System Assessment:
- **RAM**: 29GB total ✅ (sufficient)
- **CPU**: Unknown specs ⚠️
- **GPU**: None detected ❌
- **Environment**: LXC container ⚠️ (may limit performance)

## Next Steps

### Immediate Actions:
1. ✅ **Use Simulation Mode**: Test AMCP functionality without AI delays
2. ⏳ **Monitor System Resources**: Check CPU usage during Ollama operations
3. ⏳ **Consider Alternatives**: Evaluate cloud AI services or smaller models

### Future Improvements:
1. **Hardware Upgrade**: Add GPU acceleration if possible
2. **Model Optimization**: Try quantized or distilled models
3. **Infrastructure**: Move to bare metal or optimized container
4. **Alternative AI**: Integrate with cloud-based AI services

## Testing Commands

### Test Simulation Mode:
```bash
# Should work fast without AI delays
./amcp-demos.sh
```

### Test Ollama Performance:
```bash
# Benchmark Ollama directly
time ollama run tinyllama "Hello" --verbose
```

### Monitor System Resources:
```bash
# Monitor during AI operations
htop
# or
top -p $(pgrep ollama)
```

---

**Conclusion**: While Ollama + TinyLlama is installed correctly, performance constraints make it unsuitable for real-time AMCP integration. Simulation mode is recommended for testing and demonstration purposes.
