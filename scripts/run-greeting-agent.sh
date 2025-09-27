#!/bin/bash

# AMCP v1.5 - Greeting Agent Runner Script
# Demonstrates basic AMCP agent lifecycle and messaging

set -e

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "👋 AMCP v1.5 - Greeting Agent Demo"
echo "=================================="

# Change to project directory
cd "$PROJECT_DIR"

# Check if project is built
if [ ! -f "target/classes/io/amcp/examples/GreetingAgent.class" ]; then
    echo "📦 Project not built. Building now..."
    ./scripts/build-all.sh
fi

# Set up classpath
CLASSPATH="target/classes:core/target/classes:examples/target/classes"

# Add Maven dependencies to classpath
if [ -d "target/dependency" ]; then
    CLASSPATH="$CLASSPATH:target/dependency/*"
fi

# Run the Greeting Agent
echo ""
echo "🚀 Starting Greeting Agent..."
echo "Press Ctrl+C to stop"
echo ""

java -cp "$CLASSPATH" io.amcp.examples.GreetingAgent

echo ""
echo "👋 Greeting Agent Demo Complete!"