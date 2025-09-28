#!/bin/bash

# OLLAMA Chat Agent Test Script for AMCP v1.5 Enterprise Edition
# Tests the OLLAMA integration with TinyLlama 1.1B model

echo "🚀 AMCP OLLAMA Integration Test with TinyLlama 1.1B"
echo "================================================="

# Set JAVA_HOME for compatibility
export JAVA_HOME="/opt/homebrew/Cellar/openjdk@21/21.0.8/libexec/openjdk.jdk/Contents/Home"

# Check if OLLAMA is running
echo "🔍 Checking OLLAMA service status..."
if ! curl -s http://localhost:11434/api/tags > /dev/null; then
    echo "❌ OLLAMA service is not running. Please start it with:"
    echo "   brew services start ollama"
    echo "   or"
    echo "   ollama serve"
    exit 1
fi
echo "✅ OLLAMA service is running"

# Check if TinyLlama model is available
echo "🔍 Checking TinyLlama model availability..."
if ! ollama list | grep -q tinyllama; then
    echo "❌ TinyLlama model not found. Downloading it now..."
    ollama pull tinyllama
    if [ $? -ne 0 ]; then
        echo "❌ Failed to download TinyLlama model"
        exit 1
    fi
fi
echo "✅ TinyLlama model is available"

# Test OLLAMA connectivity with TinyLlama
echo "🔍 Testing TinyLlama connectivity..."
RESPONSE=$(curl -s -X POST http://localhost:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "model": "tinyllama",
    "prompt": "Hello! Respond with: TinyLlama working",
    "stream": false
  }' | grep -o '"response":"[^"]*"' | cut -d'"' -f4)

if [ -z "$RESPONSE" ]; then
    echo "❌ TinyLlama connectivity test failed"
    exit 1
fi
echo "✅ TinyLlama connectivity test passed: $RESPONSE"

# Build the project
echo "🔨 Building AMCP project..."
mvn clean compile -q > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "❌ Build failed. Running with verbose output:"
    mvn clean compile
    exit 1
fi
echo "✅ Project built successfully"

# Create temporary classpath with all required dependencies
echo "🔧 Preparing classpath with dependencies..."
CLASSPATH="connectors/target/classes:core/target/classes"

# Add Maven dependencies to classpath
if [ -d "$HOME/.m2/repository" ]; then
    # Add Jackson dependencies
    JACKSON_VERSION="2.17.2"
    JACKSON_CORE="$HOME/.m2/repository/com/fasterxml/jackson/core/jackson-core/$JACKSON_VERSION/jackson-core-$JACKSON_VERSION.jar"
    JACKSON_DATABIND="$HOME/.m2/repository/com/fasterxml/jackson/core/jackson-databind/$JACKSON_VERSION/jackson-databind-$JACKSON_VERSION.jar"
    JACKSON_ANNOTATIONS="$HOME/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/$JACKSON_VERSION/jackson-annotations-$JACKSON_VERSION.jar"
    
    if [ -f "$JACKSON_CORE" ] && [ -f "$JACKSON_DATABIND" ] && [ -f "$JACKSON_ANNOTATIONS" ]; then
        CLASSPATH="$CLASSPATH:$JACKSON_CORE:$JACKSON_DATABIND:$JACKSON_ANNOTATIONS"
        echo "✅ Jackson dependencies found and added to classpath"
    else
        echo "❌ Jackson dependencies not found in local Maven repository"
        echo "Please run: mvn dependency:copy-dependencies first"
        exit 1
    fi
fi

echo "🚀 Starting OLLAMA Chat Agent Demo with TinyLlama 1.1B..."
echo "================================================="
echo ""
echo "💡 Tips for testing:"
echo "   - Type normal messages to chat with TinyLlama"
echo "   - Use /help to see available commands"
echo "   - Use /model to see model information"
echo "   - Use /exit to quit the demo"
echo ""
echo "⚡ TinyLlama 1.1B Features:"
echo "   - Size: 1.1B parameters (~637MB download)"
echo "   - RAM Usage: ~2-3GB"
echo "   - Perfect for: Basic conversation, ultra-low-end devices"
echo "   - Speed: Very fast responses due to small size"
echo ""

# Run the demo
java -cp "$CLASSPATH" io.amcp.connectors.ollama.OllamaIntegrationDemo

echo ""
echo "👋 OLLAMA Chat Agent Demo completed!"