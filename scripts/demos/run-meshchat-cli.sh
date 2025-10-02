#!/bin/bash

# AMCP MeshChat CLI Launch Script
# Compiles and runs the MeshChat interactive command-line interface

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}"
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║                AMCP MeshChat CLI Launcher v1.5              ║"
echo "║           Enterprise Agent Communication System              ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# Function to log messages with timestamp
log() {
    echo -e "${GREEN}[$(date '+%H:%M:%S')] $1${NC}"
}

error() {
    echo -e "${RED}[$(date '+%H:%M:%S')] ERROR: $1${NC}"
}

warn() {
    echo -e "${YELLOW}[$(date '+%H:%M:%S')] WARNING: $1${NC}"
}

# Check if we're in the correct directory
if [[ ! -f "pom.xml" ]] || [[ ! -d "examples" ]]; then
    error "Please run this script from the AMCP project root directory"
    error "Expected structure: amcp-v1.5-opensource-edition/run-meshchat-cli.sh"
    exit 1
fi

# Build the project if needed
log "🔨 Building AMCP project..."
if ! mvn clean compile -q; then
    error "Failed to compile the project"
    exit 1
fi

log "✅ Project compiled successfully"

# Set up classpath
CORE_CLASSES="core/target/classes"
EXAMPLES_CLASSES="examples/target/classes" 
CONNECTORS_CLASSES="connectors/target/classes"
CORE_LIBS="core/lib/*"
EXAMPLES_LIBS="examples/lib/*"
CONNECTORS_LIBS="connectors/lib/*"

# Compile examples if not already done
if [[ ! -d "$EXAMPLES_CLASSES" ]]; then
    log "🔨 Compiling examples module..."
    cd examples
    if ! mvn compile -q; then
        error "Failed to compile examples module"
        exit 1
    fi
    cd ..
fi

# Check if MeshChat CLI class exists
MESHCHAT_CLI_CLASS="$EXAMPLES_CLASSES/io/amcp/examples/meshchat/MeshChatCLI.class"
if [[ ! -f "$MESHCHAT_CLI_CLASS" ]]; then
    log "🔨 Compiling MeshChat CLI..."
    cd examples
    if ! mvn compile -q; then
        error "Failed to compile MeshChat CLI"
        exit 1
    fi
    cd ..
fi

# Show agent status
log "🤖 AMCP Agent Ecosystem Status:"
echo "   🌍 Travel Planner Agent - Ready for trip planning and destination advice"
echo "   📈 Stock Agent - Ready for financial analysis and market data"
echo "   🌤️ Weather Agent - Ready for weather forecasts and climate info"
echo "   💬 MeshChat Agent - Ready for human-to-AI conversation coordination"
echo "   🎯 Enhanced Orchestrator - Ready for intelligent task routing"

# Set Java options for better performance
JAVA_OPTS="-Xmx512m -Xms256m"
JAVA_OPTS="$JAVA_OPTS -Dfile.encoding=UTF-8"
JAVA_OPTS="$JAVA_OPTS -Djava.awt.headless=true"

# Add AMCP configuration
JAVA_OPTS="$JAVA_OPTS -Damcp.event.broker.type=memory"
JAVA_OPTS="$JAVA_OPTS -Damcp.context.id=meshchat-cli"
JAVA_OPTS="$JAVA_OPTS -Damcp.logging.level=INFO"

log "🚀 Starting MeshChat CLI..."
log "   Classpath: $CORE_CLASSES:$EXAMPLES_CLASSES:$CONNECTORS_CLASSES:$CORE_LIBS:$EXAMPLES_LIBS:$CONNECTORS_LIBS"
log "   Main class: io.amcp.examples.meshchat.MeshChatCLI"

echo ""
log "💡 Tips:"
echo "   • Type your questions naturally (e.g., 'Plan a trip to Paris')"
echo "   • Use /help for available commands"
echo "   • Use /quit to exit gracefully"
echo ""

# Run the MeshChat CLI
java $JAVA_OPTS -cp "$CORE_CLASSES:$EXAMPLES_CLASSES:$CONNECTORS_CLASSES:$CORE_LIBS:$EXAMPLES_LIBS:$CONNECTORS_LIBS" \
    io.amcp.examples.meshchat.MeshChatCLI

# Check exit status
EXIT_CODE=$?
if [[ $EXIT_CODE -eq 0 ]]; then
    log "👋 MeshChat CLI exited successfully"
else
    error "MeshChat CLI exited with error code: $EXIT_CODE"
fi

exit $EXIT_CODE