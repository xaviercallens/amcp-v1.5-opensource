#!/bin/bash

# AMCP v1.5 Build Script for macOS
# This script builds all modules in the correct order

set -e

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "🚀 AMCP v1.5 Build Script Starting..."
echo "Project Directory: $PROJECT_DIR"
echo "=================================="

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

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | grep -oP 'version "([0-9]+)' | grep -oP '([0-9]+)')
if [ "$JAVA_VERSION" -lt 8 ]; then
    echo "❌ Java 8 or later required. Found: Java $JAVA_VERSION"
    exit 1
fi

echo "✅ Java $(java -version 2>&1 | head -1 | cut -d'"' -f2) found"
echo "✅ Maven $(mvn --version | head -1 | cut -d' ' -f3) found"

# Change to project directory
cd "$PROJECT_DIR"

# Clean previous builds
echo ""
echo "🧹 Cleaning Previous Builds..."
mvn clean -q

# Build all modules
echo ""
echo "🔨 Building All Modules..."
echo "Building in order: core -> connectors -> examples"

# Build with Maven
if mvn compile -q; then
    echo "✅ Build successful!"
else
    echo "❌ Build failed!"
    exit 1
fi

# Run tests
echo ""
echo "🧪 Running Tests..."
if mvn test -q; then
    echo "✅ All tests passed!"
else
    echo "⚠️  Some tests may have failed - check output above"
fi

# Package
echo ""
echo "📦 Packaging..."
if mvn package -q; then
    echo "✅ Packaging successful!"
else
    echo "❌ Packaging failed!"
    exit 1
fi

# Display build results
echo ""
echo "📊 Build Summary"
echo "================"
find . -name "*.jar" -type f | while read jar; do
    echo "📦 $(basename "$jar") -> $(realpath --relative-to="$PROJECT_DIR" "$jar")"
done

echo ""
echo "🎉 AMCP v1.5 Build Completed Successfully!"
echo ""
echo "Next Steps:"
echo "  • Run examples: ./scripts/run-greeting-agent.sh"
echo "  • Run travel planner: ./scripts/run-travel-planner.sh" 
echo "  • View documentation: cat README.md"
echo ""