#!/bin/bash

# OLLAMA TinyLlama Demo using Maven exec plugin
echo "🚀 AMCP OLLAMA TinyLlama 1.1B Demo (Maven)"
echo "=========================================="

# Set JAVA_HOME
export JAVA_HOME="/opt/homebrew/Cellar/openjdk@21/21.0.8/libexec/openjdk.jdk/Contents/Home"

# Check OLLAMA service
echo "🔍 Checking OLLAMA service..."
if ! curl -s http://localhost:11434/api/tags > /dev/null; then
    echo "❌ OLLAMA not running. Starting it now..."
    echo "Run: brew services start ollama"
    echo "Or: ollama serve &"
    exit 1
fi

# Check TinyLlama model
echo "🔍 Checking TinyLlama model..."
if ! ollama list | grep -q tinyllama; then
    echo "📥 TinyLlama not found. Downloading..."
    ollama pull tinyllama
    echo "✅ TinyLlama downloaded successfully!"
fi

echo "✅ All prerequisites ready!"
echo ""
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
echo ""

# Run using Maven exec plugin to handle dependencies properly
mvn exec:java \
    -Dexec.mainClass="io.amcp.connectors.ollama.OllamaIntegrationDemo" \
    -Dexec.args="" \
    -pl connectors \
    -q