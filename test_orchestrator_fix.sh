#!/bin/bash

echo "🧪 Testing Orchestrator Agent Fix - Simulation Mode"
echo "================================================="

# Load environment variables
source .env

echo "✅ Environment loaded:"
echo "   - OLLAMA_ENABLED: $OLLAMA_ENABLED"
echo "   - USE_SIMULATION_MODE: $USE_SIMULATION_MODE"
echo "   - CHAT_AGENT_ENABLED: $CHAT_AGENT_ENABLED"

echo ""
echo "🚀 Starting AMCP CLI test..."

# Test with a simple weather query
java -jar cli/target/amcp-cli-1.5.0.jar << 'EOF'
agents
activate orchestrator
status
What is the weather in Paris?
quit
EOF

echo ""
echo "✅ Test completed!"
echo ""
echo "Expected behavior:"
echo "- OrchestratorAgent should show 'Running in simulation mode'"
echo "- No Ollama connection attempts"
echo "- Weather query should route to WeatherAgent"
echo "- Response should be formatted without AI delays"
