#!/bin/bash

# Test script for AMCP demo with Phi3 3.8B
# Tests weather query orchestration with LLM integration

echo "=========================================="
echo "AMCP v1.5 - Phi3 3.8B Weather Test"
echo "=========================================="
echo ""

# Set environment variables
export OLLAMA_MODEL=phi3:3.8b
export OLLAMA_ENABLED=true
export OLLAMA_BASE_URL=http://localhost:11434

echo "Configuration:"
echo "  Model: $OLLAMA_MODEL"
echo "  Ollama URL: $OLLAMA_BASE_URL"
echo ""

# Check if Ollama is running
echo "Checking Ollama service..."
if curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo "✅ Ollama service is running"
else
    echo "❌ Ollama service is not running!"
    echo "   Please start it with: ollama serve"
    exit 1
fi

# Check if Phi3 3.8B is available
echo ""
echo "Checking Phi3 3.8B model..."
if ollama list | grep -q "phi3:3.8b"; then
    echo "✅ Phi3 3.8B model is available"
else
    echo "❌ Phi3 3.8B model not found!"
    echo "   Please pull it with: ollama pull phi3:3.8b"
    exit 1
fi

echo ""
echo "=========================================="
echo "Starting AMCP Demo with Phi3 3.8B"
echo "=========================================="
echo ""
echo "Test Query: What is the weather in Paris?"
echo ""
echo "Expected Flow:"
echo "  1. User query → OrchestratorAgent"
echo "  2. OrchestratorAgent → Phi3 3.8B (intent analysis)"
echo "  3. OrchestratorAgent → WeatherAgent (weather.request)"
echo "  4. WeatherAgent → OpenWeatherMap API"
echo "  5. WeatherAgent → OrchestratorAgent (structured response)"
echo "  6. OrchestratorAgent → User (final answer)"
echo ""
echo "Press Ctrl+C to exit when done"
echo ""
echo "=========================================="
echo ""

# Run the demo
java -jar cli/target/amcp-cli-1.5.0.jar demo
