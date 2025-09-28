#!/bin/bash
# Quick TinyLlama Test Command
# Usage: ./test-tinyllama.sh

echo "🚀 AMCP TinyLlama Quick Test"
echo "============================"

# Check if OLLAMA is running
if ! curl -s localhost:11434/api/tags >/dev/null; then
    echo "❌ OLLAMA service not running. Starting..."
    ollama serve &
    sleep 3
fi

# Quick API test
echo "🔍 Testing TinyLlama model..."
response=$(curl -s -X POST localhost:11434/api/generate \
    -H "Content-Type: application/json" \
    -d '{"model":"tinyllama","prompt":"Hello! Can you respond in one sentence?","stream":false}')

if echo "$response" | grep -q "response"; then
    echo "✅ TinyLlama is responding correctly!"
    echo "📋 Sample response:"
    echo "$response" | jq -r '.response' | head -c 100
    echo "..."
    echo ""
    echo "🎯 To run full interactive demo:"
    echo "   cd connectors && mvn exec:java -Dexec.mainClass=io.amcp.connectors.demo.OllamaIntegrationDemo"
    echo ""
    echo "Or use the convenience script:"
    echo "   ./run-maven-demo.sh"
else
    echo "❌ TinyLlama test failed"
    echo "Response: $response"
fi