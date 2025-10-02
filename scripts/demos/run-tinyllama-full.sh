#!/bin/bash

# Full OLLAMA TinyLlama Demo Runner with Build for AMCP v1.5
echo "🚀 AMCP OLLAMA TinyLlama 1.1B Demo (Full Build)"
echo "=============================================="

# Set JAVA_HOME
export JAVA_HOME="/opt/homebrew/Cellar/openjdk@21/21.0.8/libexec/openjdk.jdk/Contents/Home"

# Check OLLAMA service
echo "🔍 Checking OLLAMA service..."
if ! curl -s http://localhost:11434/api/tags > /dev/null; then
    echo "❌ OLLAMA not running. Starting OLLAMA service..."
    ollama serve &
    sleep 3
    echo "✅ OLLAMA service started"
fi

# Check TinyLlama model
echo "🔍 Checking TinyLlama model..."
if ! ollama list | grep -q tinyllama; then
    echo "📥 Downloading TinyLlama model..."
    ollama pull tinyllama
fi

echo "✅ All prerequisites ready!"
echo ""

# Build project if needed
if [ ! -d "connectors/target/classes" ] || [ ! -d "core/target/classes" ]; then
    echo "🔨 Building project..."
    mvn clean compile -q
    if [ $? -ne 0 ]; then
        echo "❌ Build failed"
        exit 1
    fi
    echo "✅ Build completed"
fi

echo "💡 TinyLlama 1.1B Specifications:"
echo "   • Parameters: 1.1 billion"
echo "   • Download Size: ~637MB"
echo "   • RAM Usage: ~2-3GB"
echo "   • Speed: Very fast (small model size)"
echo "   • Trained on: 1+ trillion tokens"
echo "   • Perfect for: Basic conversation, lightweight AI tasks"
echo ""
echo "🎯 Available Commands in Demo:"
echo "   • Type any message to chat with TinyLlama"
echo "   • /help - Show available commands"
echo "   • /model - Display model information"
echo "   • /exit - Exit the demo"
echo ""
echo "🚀 Starting OLLAMA Integration Demo..."
echo "====================================="

# Run with Maven to ensure all dependencies are available
cd connectors && mvn exec:java -Dexec.mainClass=io.amcp.connectors.ollama.OllamaIntegrationDemo -q