#!/bin/bash

# Simple OLLAMA TinyLlama Demo Runner for AMCP v1.5
echo "🚀 AMCP OLLAMA TinyLlama 1.1B Demo"
echo "=================================="

# Set JAVA_HOME
export JAVA_HOME="/opt/homebrew/Cellar/openjdk@21/21.0.8/libexec/openjdk.jdk/Contents/Home"

# Check OLLAMA service
echo "🔍 Checking OLLAMA..."
if ! curl -s http://localhost:11434/api/tags > /dev/null; then
    echo "❌ OLLAMA not running. Start with: brew services start ollama"
    exit 1
fi

# Check TinyLlama model
if ! ollama list | grep -q tinyllama; then
    echo "📥 Downloading TinyLlama..."
    ollama pull tinyllama
fi

echo "✅ All prerequisites ready!"
echo ""
echo "💡 TinyLlama 1.1B Info:"
echo "   • Size: ~637MB download, ~2-3GB RAM"
echo "   • Speed: Very fast due to small parameter count"
echo "   • Use case: Basic conversation, lightweight AI"
echo ""
echo "🎯 Demo Commands:"
echo "   • Type messages to chat with TinyLlama"
echo "   • /help - Show commands"
echo "   • /model - Model information"
echo "   • /exit - Quit demo"
echo ""

# Build classpath
CLASSPATH="connectors/target/classes:core/target/classes"
if [ -d "lib" ]; then
    CLASSPATH="$CLASSPATH:lib/*"
fi

echo "🚀 Starting demo..."
echo ""

java -cp "$CLASSPATH" io.amcp.connectors.ollama.OllamaIntegrationDemo