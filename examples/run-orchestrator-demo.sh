#!/bin/bash

# AMCP v1.5 OrchestratorAgent Demo Runner
# This script demonstrates the new OrchestratorAgent with TinyLlama integration

echo "🎯 AMCP v1.5 OrchestratorAgent Demo"
echo "=================================="

# Set Java environment
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Build classpath
CLASSPATH="../core/target/classes:../connectors/target/classes:target/classes"

# Check if OLLAMA is running
echo "🔍 Checking OLLAMA availability..."
if curl -s http://localhost:11434/api/version > /dev/null 2>&1; then
    echo "✅ OLLAMA is running and accessible"
else
    echo "⚠️  Warning: OLLAMA is not running. Please start OLLAMA with:"
    echo "   ollama serve"
    echo "   ollama pull tinyllama"
    echo ""
fi

# Check for required model
echo "🤖 Checking TinyLlama model..."
if curl -s http://localhost:11434/api/tags | grep -q tinyllama; then
    echo "✅ TinyLlama model is available"
else
    echo "⚠️  Warning: TinyLlama model not found. Please install with:"
    echo "   ollama pull tinyllama"
    echo ""
fi

# Display available options
echo ""
echo "📋 Demo Options:"
echo "  1. Run OrchestratorDemo (requires functional agents)"
echo "  2. Test OrchestratorAgent standalone"
echo "  3. Show agent configuration"
echo ""

# Prepare for demo
echo "🚀 Preparing AMCP v1.5 environment..."

# Compile demo if needed
if [ ! -f "target/classes/io/amcp/examples/orchestrator/OrchestratorDemo.class" ]; then
    echo "📦 Compiling OrchestratorDemo..."
    javac -cp "$CLASSPATH" -d target/classes src/main/java/io/amcp/examples/orchestrator/OrchestratorDemo.java
    if [ $? -eq 0 ]; then
        echo "✅ OrchestratorDemo compiled successfully"
    else
        echo "❌ Failed to compile OrchestratorDemo"
        exit 1
    fi
fi

# Show system info
echo ""
echo "🔧 System Configuration:"
echo "  Java Version: $(java -version 2>&1 | head -n 1)"
echo "  AMCP Version: v1.5 Enterprise Edition"
echo "  OrchestratorAgent: Active"
echo "  TinyLlama Integration: Available"
echo ""

# Instructions
echo "📖 How to use the OrchestratorAgent:"
echo ""
echo "The OrchestratorAgent acts as an intelligent middleware that:"
echo "• Analyzes user intent using TinyLlama via OLLAMA"
echo "• Dynamically discovers available agents in the system"  
echo "• Routes requests to the most appropriate agent"
echo "• Synthesizes and formats responses for optimal user experience"
echo ""
echo "Example queries to try:"
echo "  'What's the weather in Paris?' → Routes to WeatherAgent"
echo "  'Apple stock price' → Routes to StockPriceAgent"
echo "  'Plan a trip to Tokyo' → Routes to TravelPlannerAgent"
echo "  'Hello, how are you?' → Handles as general chat"
echo ""

# Ready to run
echo "🎬 Ready to run! Use one of these commands:"
echo ""
echo "# Run the full demo:"
echo "java -cp \"$CLASSPATH\" io.amcp.examples.orchestrator.OrchestratorDemo"
echo ""
echo "# Or run the enhanced chat demo:"
echo "cd ../examples && ./run-chat-demo.sh"
echo ""
echo "💡 Note: Make sure OLLAMA is running (ollama serve) and TinyLlama is installed (ollama pull tinyllama)"
echo "🚀 The OrchestratorAgent is now ready for intelligent multi-agent orchestration!"