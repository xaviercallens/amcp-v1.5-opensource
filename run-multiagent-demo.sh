#!/bin/bash

# AMCP v1.5 Multi-Agent Communication Demo Runner
# Demonstrates Enhanced Chat Agent orchestrating Weather, Travel, and Stock agents

echo "🚀 Starting AMCP v1.5 Multi-Agent Communication Demo..."
echo "========================================================="

# Set up environment
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"

# Change to project directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
cd "$SCRIPT_DIR"

# Check if Maven build is up to date
echo "🔨 Building all modules..."
if ! mvn clean install -DskipTests -q; then
    echo "❌ Build failed! Please fix compile errors first."
    exit 1
fi

echo "✅ Build completed successfully!"
echo ""

# Check for interactive mode
if [ "$1" = "interactive" ]; then
    echo "🎮 Starting Interactive Multi-Agent Demo..."
    echo "You can ask questions like:"
    echo "  • 'What's the weather in Nice?'"
    echo "  • 'What is the stock price of Amadeus?'"
    echo "  • 'Plan a trip to Rome for 3 days'"
    echo "  • Type 'exit' to quit"
    echo ""
    
    cd examples
    mvn exec:java \
        -Dexec.mainClass="io.amcp.examples.multiagent.MultiAgentDemo" \
        -Dexec.args="interactive" \
        -q
else
    echo "🤖 Starting Predefined Multi-Agent Conversations..."
    echo "Demonstrating:"
    echo "  ✓ Agent architecture and capabilities"
    echo "  ✓ Intelligent request analysis and routing"
    echo "  ✓ Multi-agent coordination patterns"
    echo "  ✓ Weather, Stock, and Travel agent integration"
    echo "  ✓ AMCP v1.5 design principles"
    echo ""
    
    cd examples
    mvn exec:java \
        -Dexec.mainClass="io.amcp.examples.multiagent.MultiAgentDemo" \
        -q
fi

echo ""
echo "🏁 Multi-Agent Demo completed!"
echo ""
echo "📋 For complete implementation details, see:"
echo "   • MULTIAGENT_SYSTEM_GUIDE.md - Comprehensive system guide"
echo "   • connectors/src/main/java/io/amcp/connectors/ai/ - Agent implementations"
echo "   • examples/src/main/java/io/amcp/examples/ - Example agents"
echo ""
echo "To run again:"
echo "  • Standard demo: ./run-multiagent-demo.sh"
echo "  • Interactive mode: ./run-multiagent-demo.sh interactive"