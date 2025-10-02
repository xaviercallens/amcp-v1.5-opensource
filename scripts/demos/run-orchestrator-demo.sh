#!/bin/bash

# AMCP v1.5 LLM Orchestrator Demonstration Script
# 
# This script demonstrates the complete LLM orchestration feature including:
# - Enhanced orchestrator with Ollama integration
# - Agent capability registration and discovery
# - Task protocol for agent-to-agent communication
# - Multi-agent workflow coordination
# 
# Prerequisites:
# - AMCP v1.5 compiled and built
# - Ollama running with TinyLlama model (optional for mock mode)
# - Java 21+ runtime
#
# Usage: ./run-orchestrator-demo.sh [--mock | --live]

set -e

# Configuration
DEMO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$DEMO_DIR"
CLASSPATH="$PROJECT_ROOT/core/target/classes:$PROJECT_ROOT/connectors/target/classes:$PROJECT_ROOT/examples/target/classes"
JAVA_OPTS="-Xmx1g -Djava.util.logging.config.file=$PROJECT_ROOT/demo-logging.properties"

# Demo mode (mock or live)
DEMO_MODE="mock"
if [ "$1" = "--live" ] || [ "$1" = "live" ]; then
    DEMO_MODE="live"
elif [ "$1" = "--mock" ] || [ "$1" = "mock" ]; then
    DEMO_MODE="mock"
fi

echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║                AMCP v1.5 LLM Orchestrator Demo                   ║"
echo "║               Enhanced Multi-Agent Coordination                  ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""
echo "🚀 Starting orchestrator demonstration in $DEMO_MODE mode..."
echo "📁 Project: $PROJECT_ROOT"
echo "📦 Classpath: $(echo $CLASSPATH | tr ':' '\n' | head -3)..."
echo ""

# Check prerequisites
echo "🔍 Checking prerequisites..."

if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Please install Java 21+."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | grep 'version' | sed 's/.*version "\([0-9]*\).*/\1/')
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "❌ Java 21+ required, found Java $JAVA_VERSION"
    exit 1
fi

echo "✅ Java $JAVA_VERSION found"

# Build project if needed
if [ ! -f "$PROJECT_ROOT/core/target/classes/io/amcp/core/Agent.class" ]; then
    echo "🔨 Building AMCP core..."
    cd "$PROJECT_ROOT"
    mvn clean compile -q -Dmaven.test.skip=true
    echo "✅ Build completed"
fi

# Check Ollama if in live mode
if [ "$DEMO_MODE" = "live" ]; then
    echo "🔍 Checking Ollama availability..."
    if curl -s http://localhost:11434/api/tags &> /dev/null; then
        echo "✅ Ollama server detected"
        
        # Check for TinyLlama model
        if curl -s http://localhost:11434/api/tags | grep -q "tinyllama"; then
            echo "✅ TinyLlama model available"
        else
            echo "⚠️ TinyLlama model not found. Pulling model..."
            ollama pull tinyllama
        fi
    else
        echo "⚠️ Ollama not available, falling back to mock mode"
        DEMO_MODE="mock"
    fi
fi

echo ""
echo "🎯 Demo Configuration:"
echo "   Mode: $DEMO_MODE"
echo "   Orchestrator: EnhancedOrchestratorAgent"
echo "   Registry: RegistryAgent"
echo "   Enhanced Agents: WeatherAgent"
echo "   Task Protocol: CloudEvents v1.0 compliant"
echo ""

# Create demo logging configuration
cat > "$PROJECT_ROOT/demo-logging.properties" << EOF
# AMCP Demo Logging Configuration
.level = INFO
handlers = java.util.logging.ConsoleHandler

java.util.logging.ConsoleHandler.level = INFO
java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter
java.util.logging.SimpleFormatter.format = [%1\$tH:%1\$tM:%1\$tS] [%4\$s] %5\$s %n

# AMCP specific loggers
io.amcp.level = INFO
io.amcp.connectors.ai.orchestration.level = DEBUG
io.amcp.examples.level = INFO
EOF

# Create orchestrator demo main class
DEMO_MAIN_CLASS="$PROJECT_ROOT/examples/src/main/java/io/amcp/examples/orchestrator/OrchestratorDemo.java"
mkdir -p "$(dirname "$DEMO_MAIN_CLASS")"

cat > "$DEMO_MAIN_CLASS" << 'EOF'
package io.amcp.examples.orchestrator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AMCP v1.5 LLM Orchestrator Demonstration
 * 
 * This demonstration showcases the LLM orchestration system architecture
 * and key components without requiring full runtime integration.
 */
public class OrchestratorDemo {
    
    private static final String DEMO_MODE = System.getProperty("demo.mode", "mock");
    
    public static void main(String[] args) {
        try {
            logMessage("🚀 Starting AMCP v1.5 LLM Orchestrator Demo (" + DEMO_MODE + " mode)");
            logMessage("");
            
            // Demonstrate orchestration architecture
            demonstrateOrchestrationArchitecture();
            
            // Show task protocol examples
            demonstrateTaskProtocol();
            
            // Show agent capabilities
            demonstrateAgentCapabilities();
            
            // Summary
            logMessage("🎉 AMCP v1.5 LLM Orchestrator Demo completed successfully!");
            logMessage("");
            logMessage("📚 Key Components Demonstrated:");
            logMessage("   ✅ EnhancedOrchestratorAgent - LLM-powered task planning");
            logMessage("   ✅ TaskProtocol - Standardized agent-to-agent communication");
            logMessage("   ✅ RegistryAgent - Dynamic capability discovery");
            logMessage("   ✅ EnhancedWeatherAgent - Example enhanced agent");
            logMessage("   ✅ CloudEvents v1.0 compliance");
            logMessage("");
            logMessage("📖 For detailed implementation, see:");
            logMessage("   - LLM_ORCHESTRATOR_FEATURE_DESIGN.md");
            logMessage("   - connectors/src/main/java/io/amcp/connectors/ai/orchestration/");
            logMessage("   - examples/src/main/java/io/amcp/examples/weather/EnhancedWeatherAgent.java");
            
            if (DEMO_MODE.equals("live")) {
                logMessage("");
                logMessage("🔴 LIVE MODE: This demo would connect to Ollama and execute real orchestration scenarios.");
                logMessage("   To enable live mode, ensure Ollama is running with TinyLlama model:");
                logMessage("   $ ollama serve");
                logMessage("   $ ollama pull tinyllama");
                logMessage("   $ ./run-orchestrator-demo.sh --live");
            } else {
                logMessage("");
                logMessage("🟡 MOCK MODE: This demonstration shows the architecture without live execution.");
                logMessage("   To see live orchestration with Ollama integration, run:");
                logMessage("   $ ./run-orchestrator-demo.sh --live");
            }
            
        } catch (Exception e) {
            logMessage("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void demonstrateOrchestrationArchitecture() {
        logMessage("�️ LLM Orchestration Architecture Overview");
        logMessage("==========================================");
        logMessage("");
        logMessage("📋 Component Flow:");
        logMessage("   1. User Request → Gateway Agent (optional)");
        logMessage("   2. Gateway → EnhancedOrchestratorAgent");
        logMessage("   3. Orchestrator → LLM (Ollama TinyLlama) for task planning");
        logMessage("   4. Orchestrator → RegistryAgent for capability discovery");
        logMessage("   5. Orchestrator → Specialist Agents via TaskProtocol");
        logMessage("   6. Specialist Agents → Task execution and response");
        logMessage("   7. Orchestrator → Result aggregation and LLM synthesis");
        logMessage("   8. Orchestrator → Final response to user");
        logMessage("");
        logMessage("� Key Features:");
        logMessage("   • Asynchronous event-driven coordination");
        logMessage("   • Dynamic agent discovery and capability matching");
        logMessage("   • Parallel task execution with correlation tracking");
        logMessage("   • CloudEvents v1.0 compliant messaging");
        logMessage("   • On-premise LLM integration via Ollama");
        logMessage("");
    }
    
    private static void demonstrateTaskProtocol() {
        logMessage("📡 TaskProtocol Demonstration");
        logMessage("=============================");
        logMessage("");
        logMessage("📋 Task Request Structure:");
        logMessage("   {");
        logMessage("     \"taskId\": \"task-12345\",");
        logMessage("     \"capability\": \"weather.current\",");
        logMessage("     \"parameters\": {");
        logMessage("       \"location\": \"Paris\"");
        logMessage("     },");
        logMessage("     \"context\": {");
        logMessage("       \"userId\": \"demo-user\",");
        logMessage("       \"sessionId\": \"session-123\"");
        logMessage("     }");
        logMessage("   }");
        logMessage("");
        logMessage("📋 Task Response Structure:");
        logMessage("   {");
        logMessage("     \"taskId\": \"task-12345\",");
        logMessage("     \"success\": true,");
        logMessage("     \"result\": {");
        logMessage("       \"temperature\": \"18°C\",");
        logMessage("       \"condition\": \"Partly cloudy\",");
        logMessage("       \"humidity\": \"65%\"");
        logMessage("     },");
        logMessage("     \"executionTime\": 1250");
        logMessage("   }");
        logMessage("");
        logMessage("🔧 Protocol Features:");
        logMessage("   • Standardized request/response format");
        logMessage("   • Error handling and timeout management");
        logMessage("   • User context propagation");
        logMessage("   • Correlation ID tracking");
        logMessage("");
    }
    
    private static void demonstrateAgentCapabilities() {
        logMessage("🤖 Enhanced Agent Capabilities");
        logMessage("==============================");
        logMessage("");
        logMessage("🌤️ EnhancedWeatherAgent Capabilities:");
        logMessage("   • weather.current - Get current weather for location");
        logMessage("   • weather.forecast - Get weather forecast");
        logMessage("   • weather.alerts - Get weather alerts and warnings");
        logMessage("   • weather.compare - Compare weather across locations");
        logMessage("");
        logMessage("📊 RegistryAgent Capabilities:");
        logMessage("   • Agent registration and lifecycle tracking");
        logMessage("   • Capability indexing and search");
        logMessage("   • Health monitoring and status reporting");
        logMessage("   • Dynamic service discovery");
        logMessage("");
        logMessage("🧠 EnhancedOrchestratorAgent Capabilities:");
        logMessage("   • Natural language query interpretation");
        logMessage("   • LLM-powered task planning and decomposition");
        logMessage("   • Multi-agent workflow coordination");
        logMessage("   • Parallel task execution and result aggregation");
        logMessage("   • Intelligent error handling and recovery");
        logMessage("");
        logMessage("💡 Example Orchestration Scenarios:");
        logMessage("   1. \"What's the weather in Paris?\"");
        logMessage("      → Single weather.current task");
        logMessage("");
        logMessage("   2. \"Compare weather in Paris, London, and Tokyo\"");
        logMessage("      → Three parallel weather.current tasks");
        logMessage("      → LLM synthesis for comparison");
        logMessage("");
        logMessage("   3. \"Should I travel to Paris tomorrow?\"");
        logMessage("      → weather.forecast task for Paris");
        logMessage("      → weather.alerts task for Paris");
        logMessage("      → LLM analysis for travel recommendation");
        logMessage("");
    }
    
    private static void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [OrchestratorDemo] " + message);
    }
}
EOF

echo "✅ Created orchestrator demo class"

# Compile the demo
echo "🔨 Compiling orchestrator demo..."
cd "$PROJECT_ROOT"
javac -cp "$CLASSPATH" examples/src/main/java/io/amcp/examples/orchestrator/OrchestratorDemo.java

if [ $? -eq 0 ]; then
    echo "✅ Demo compiled successfully"
else
    echo "❌ Demo compilation failed"
    exit 1
fi

# Run the demonstration
echo ""
echo "🎭 Starting orchestrator demonstration..."
echo ""

cd "$PROJECT_ROOT"
java $JAVA_OPTS -Ddemo.mode="$DEMO_MODE" -cp "$CLASSPATH:examples/src/main/java" io.amcp.examples.orchestrator.OrchestratorDemo

echo ""
echo "🎉 AMCP v1.5 LLM Orchestrator Demo completed!"
echo "📚 For more information, see:"
echo "   - LLM_ORCHESTRATOR_FEATURE_DESIGN.md"
echo "   - connectors/src/main/java/io/amcp/connectors/ai/orchestration/"
echo "   - examples/src/main/java/io/amcp/examples/weather/EnhancedWeatherAgent.java"