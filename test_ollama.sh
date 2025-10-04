#!/bin/bash

echo "🔍 Testing Ollama + TinyLlama Integration for AMCP"
echo "=================================================="

# Test 1: Check Ollama version
echo "1. Checking Ollama version..."
ollama --version

# Test 2: Check if service is running
echo -e "\n2. Checking Ollama service status..."
if pgrep -x "ollama" > /dev/null; then
    echo "✅ Ollama service is running"
else
    echo "❌ Ollama service is not running"
    echo "Starting Ollama service..."
    sudo systemctl start ollama
    sleep 2
fi

# Test 3: List available models
echo -e "\n3. Listing available models..."
ollama list

# Test 4: Test basic connectivity
echo -e "\n4. Testing API connectivity..."
if curl -s http://localhost:11434/api/tags > /dev/null; then
    echo "✅ Ollama API is accessible"
else
    echo "❌ Ollama API is not accessible"
    echo "Waiting for service to start..."
    sleep 5
fi

# Test 5: Simple model test
echo -e "\n5. Testing TinyLlama model..."
echo "Sending test prompt to TinyLlama..."
ollama run tinyllama "Say hello in one sentence." --verbose

echo -e "\n🎉 Ollama setup test completed!"
echo "You can now test the AMCP ChatMesh agent integration."
echo ""
echo "Next steps:"
echo "1. Start AMCP CLI: java -jar cli/target/amcp-cli-1.5.0.jar"
echo "2. Run: agents"
echo "3. Run: activate chat"
echo "4. Run: chat \"Hello, test message\""
