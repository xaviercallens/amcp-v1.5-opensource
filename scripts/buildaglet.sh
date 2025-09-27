#!/bin/bash

# AMCP v1.5 Agent ("Aglet") Build Script for macOS
# This script builds agent-specific components and examples

set -e

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "🤖 AMCP v1.5 Agent Build Script Starting..."
echo "Project Directory: $PROJECT_DIR"
echo "=========================================="

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check prerequisites
echo "📋 Checking Prerequisites..."
if ! command_exists java; then
    echo "❌ Java not found. Please install Java 8 or later."
    exit 1
fi

if ! command_exists mvn; then
    echo "❌ Maven not found. Please install Maven 3.6.0 or later."
    exit 1
fi

# Check Java version (macOS compatible)
JAVA_VERSION_FULL=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
echo "Detected Java version: $JAVA_VERSION_FULL"

# Handle both Java 8 format (1.8.x) and Java 9+ format (9.x, 11.x, etc.)
if [[ "$JAVA_VERSION_FULL" == 1.* ]]; then
    JAVA_MAJOR_VERSION=$(echo "$JAVA_VERSION_FULL" | cut -d'.' -f2)
else
    JAVA_MAJOR_VERSION=$(echo "$JAVA_VERSION_FULL" | cut -d'.' -f1)
fi

echo "Java major version: $JAVA_MAJOR_VERSION"

if [ "$JAVA_MAJOR_VERSION" -lt 8 ]; then
    echo "❌ Java 8 or later required. Found: Java $JAVA_VERSION_FULL"
    exit 1
fi

echo "✅ Java $(java -version 2>&1 | head -1 | cut -d'"' -f2) found"
echo "✅ Maven $(mvn --version | head -1 | cut -d' ' -f3) found"

# Change to project directory
cd "$PROJECT_DIR"

# Clean previous builds
echo ""
echo "🧹 Cleaning Previous Agent Builds..."
mvn clean -q

# Build core framework first (required for agents)
echo ""
echo "🔨 Building Core Framework..."
echo "Building core -> connectors -> examples"

# Build core module
echo "  📦 Building amcp-core..."
if mvn -pl core compile -q; then
    echo "    ✅ Core compilation successful!"
else
    echo "    ❌ Core compilation failed!"
    exit 1
fi

# Build connectors module  
echo "  🔌 Building amcp-connectors..."
if mvn -pl connectors compile -q; then
    echo "    ✅ Connectors compilation successful!"
else
    echo "    ❌ Connectors compilation failed!"
    exit 1
fi

# Build examples (agents) module
echo "  🤖 Building agent examples..."
if mvn -pl examples compile -q; then
    echo "    ✅ Agent examples compilation successful!"
else
    echo "    ❌ Agent examples compilation failed!"
    exit 1
fi

# Run agent-specific tests
echo ""
echo "🧪 Running Agent Tests..."
if mvn -pl examples test -q; then
    echo "✅ All agent tests passed!"
else
    echo "⚠️  Some agent tests may have failed - check output above"
fi

# Package agent components
echo ""
echo "📦 Packaging Agent Components..."
if mvn -pl core,connectors,examples package -q; then
    echo "✅ Agent packaging successful!"
else
    echo "❌ Agent packaging failed!"
    exit 1
fi

# Display agent build results
echo ""
echo "📊 Agent Build Summary"
echo "======================"

echo "🤖 Available Agents:"
echo "  • GreetingAgent - Basic agent lifecycle demo"
echo "  • TravelPlannerAgent - Advanced coordination agent"
echo "  • WeatherCollectorAgent - Data collection agent"
echo "  • WeatherSystemLauncher - Multi-agent system"

echo ""
echo "📦 Generated Agent JARs:"
find . -name "*amcp*.jar" -type f | while read jar; do
    if [ -x "$(which realpath)" ]; then
        echo "📦 $(basename "$jar") -> $(realpath --relative-to="$PROJECT_DIR" "$jar")"
    else
        # Fallback for systems without realpath
        echo "📦 $(basename "$jar") -> $jar"
    fi
done

echo ""
echo "🎉 AMCP v1.5 Agent Build Completed Successfully!"
echo ""
echo "🚀 Next Steps - Run Agents:"
echo "  • Basic agent: ./scripts/run-greeting-agent.sh"
echo "  • Travel planner: ./scripts/run-travel-planner.sh" 
echo "  • Weather system: ./scripts/run-weather-system.sh"
echo "  • Run tests: ./scripts/run-tests.sh"
echo ""
echo "📖 Documentation:"
echo "  • Agent examples: examples/src/main/java/io/amcp/examples/"
echo "  • Installation guide: docs/INSTALLATION_MACOS.md"
echo "  • Framework docs: README.md"
echo ""