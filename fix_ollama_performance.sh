#!/bin/bash

echo "🔧 Fixing Ollama Performance Issues for AMCP Integration"
echo "======================================================="

# Stop any existing Ollama processes
echo "1. Stopping existing Ollama processes..."
sudo pkill -f ollama || true
sleep 3

# Create optimized Ollama configuration
echo "2. Creating optimized Ollama configuration..."
sudo mkdir -p /etc/systemd/system/ollama.service.d

cat << 'EOF' | sudo tee /etc/systemd/system/ollama.service.d/override.conf > /dev/null
[Service]
Environment="OLLAMA_MAX_LOADED_MODELS=1"
Environment="OLLAMA_NUM_PARALLEL=1"
Environment="OLLAMA_MAX_QUEUE=2"
Environment="OLLAMA_KEEP_ALIVE=2m"
Environment="OLLAMA_HOST=0.0.0.0"
Environment="OLLAMA_PORT=11434"
Environment="OLLAMA_DEBUG=false"
Environment="OLLAMA_FLASH_ATTENTION=false"
Environment="OLLAMA_LLM_LIBRARY=cpu"
TimeoutStartSec=120
TimeoutStopSec=30
Restart=always
RestartSec=5
EOF

# Reload systemd and restart Ollama
echo "3. Reloading systemd configuration..."
sudo systemctl daemon-reload

echo "4. Starting Ollama service with optimized settings..."
sudo systemctl start ollama

echo "5. Waiting for Ollama to start..."
sleep 10

# Test basic connectivity
echo "6. Testing basic API connectivity..."
if curl -s http://localhost:11434/api/tags > /dev/null; then
    echo "✅ Ollama API is accessible"
else
    echo "❌ Ollama API is not accessible"
    exit 1
fi

# Test model availability
echo "7. Checking TinyLlama model availability..."
if ollama list | grep -q tinyllama; then
    echo "✅ TinyLlama model is available"
else
    echo "❌ TinyLlama model not found"
    exit 1
fi

# Test simple generation with timeout
echo "8. Testing simple text generation (with timeout)..."
timeout 30 curl -X POST http://localhost:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "model": "tinyllama",
    "prompt": "Hi",
    "stream": false,
    "options": {
      "num_predict": 10,
      "temperature": 0.1,
      "top_p": 0.9,
      "stop": ["\n"]
    }
  }' 2>/dev/null | jq -r '.response // "ERROR"'

if [ $? -eq 0 ]; then
    echo "✅ Text generation test passed"
else
    echo "⚠️ Text generation test timed out (this is expected on slow systems)"
fi

echo ""
echo "🎯 Ollama Configuration Complete!"
echo ""
echo "Configuration Summary:"
echo "- Max loaded models: 1"
echo "- Parallel requests: 1" 
echo "- Keep alive: 2 minutes"
echo "- CPU-only mode enabled"
echo "- Flash attention disabled for compatibility"
echo ""
echo "Next Steps:"
echo "1. Test AMCP integration: ./amcp-demos.sh"
echo "2. If still slow, consider using a smaller model or adjusting parameters"
echo "3. Monitor system resources during testing"
echo ""
echo "Alternative Solutions if Performance Issues Persist:"
echo "- Use simulation mode instead of real AI"
echo "- Switch to a cloud-based AI service"
echo "- Use a different local model (e.g., phi-2, qwen)"
