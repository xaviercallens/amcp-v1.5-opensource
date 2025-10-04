#!/bin/bash

echo "🧪 Testing AMCP in Simulation Mode (Ollama Performance Workaround)"
echo "=================================================================="

# Load environment variables
echo "1. Loading environment configuration..."
source .env

echo "2. Environment Status:"
echo "   - CHAT_AGENT_ENABLED: $CHAT_AGENT_ENABLED"
echo "   - USE_SIMULATION_MODE: $USE_SIMULATION_MODE"
echo "   - AI_FALLBACK_MODE: $AI_FALLBACK_MODE"
echo "   - OLLAMA_ENABLED: $OLLAMA_ENABLED"

# Check if AMCP CLI exists
if [ ! -f "cli/target/amcp-cli-1.5.0.jar" ]; then
    echo "❌ AMCP CLI JAR not found. Building..."
    mvn clean package -DskipTests
fi

echo ""
echo "3. Starting AMCP CLI in simulation mode..."
echo "   (This should work without Ollama performance issues)"
echo ""
echo "🎯 Expected Behavior:"
echo "   - Weather agent: Should work with real OpenWeatherMap API"
echo "   - Stock agent: Should work with simulated data (no Polygon API needed)"
echo "   - Chat agent: Should use simulation mode (no AI delays)"
echo "   - Orchestrator: Should coordinate agents without AI bottlenecks"
echo ""
echo "📝 Test Commands to Try:"
echo "   agents              # List available agents"
echo "   activate weather    # Activate weather agent"
echo "   activate orchestrator # Activate orchestrator"
echo "   status             # Check agent status"
echo "   weather Paris      # Test weather functionality"
echo "   quit               # Exit CLI"
echo ""
echo "🚀 Starting AMCP CLI now..."
echo "   Press Ctrl+C to exit when done testing"
echo ""

# Start AMCP CLI
java -jar cli/target/amcp-cli-1.5.0.jar
