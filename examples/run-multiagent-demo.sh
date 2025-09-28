#!/bin/bash

# AMCP v1.5 Multi-Agent Communication Demo
# This script demonstrates the complete multi-agent system with intelligent orchestration

set -e

echo "🚀 AMCP v1.5 Multi-Agent Communication Demo"
echo "============================================="
echo

# Build the project
echo "🔨 Building AMCP modules..."
cd "$(dirname "$0")/.."

# Build core and connectors modules
mvn clean compile -pl core,connectors -DskipTests -q

if [ $? -eq 0 ]; then
    echo "✅ Build successful"
else
    echo "❌ Build failed"
    exit 1
fi

# Generate dependency classpath
mvn dependency:build-classpath -pl connectors -Dmdep.outputFile=connectors/classpath.txt -q

echo
echo "🤖 Starting Multi-Agent Demo..."
echo

# Change to examples directory and run the demo
cd examples

# Set up classpath with all dependencies
CLASSPATH="target/classes:../connectors/target/classes:$(cat ../connectors/classpath.txt)"

# Run the demo
java -cp "$CLASSPATH" io.amcp.examples.multiagent.MultiAgentDemo