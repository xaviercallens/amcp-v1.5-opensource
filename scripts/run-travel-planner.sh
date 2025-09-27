#!/bin/bash

# AMCP v1.5 - Travel Planner Agent Runner Script
# Advanced example with external service integration and route optimization

set -e

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "✈️  AMCP v1.5 - Travel Planner Agent"
echo "===================================="

# Change to project directory
cd "$PROJECT_DIR"

# Check if project is built
if [ ! -f "examples/target/classes/io/amcp/examples/TravelPlannerAgent.class" ]; then
    echo "📦 Project not built. Building now..."
    ./scripts/build-all.sh
fi

# Set up classpath
CLASSPATH="target/classes:core/target/classes:examples/target/classes"

# Add Maven dependencies to classpath
if [ -d "target/dependency" ]; then
    CLASSPATH="$CLASSPATH:target/dependency/*"
fi

# Configuration
echo ""
echo "🔧 Configuration Options:"
echo "   • Set MAPS_API_KEY environment variable for enhanced features"
echo "   • Set WEATHER_API_KEY for weather integration"
echo "   • Default demo mode uses mock data"
echo ""

# Display current configuration
if [ -n "$MAPS_API_KEY" ]; then
    echo "✅ Maps API Key: configured"
else
    echo "🔧 Maps API Key: using mock data"
fi

if [ -n "$WEATHER_API_KEY" ]; then
    echo "✅ Weather API Key: configured"
else
    echo "🔧 Weather API Key: using mock data"
fi

# Run the Travel Planner Agent
echo ""
echo "🚀 Starting Travel Planner Agent..."
echo "Use the interactive commands to plan trips"
echo "Type 'help' for available commands"
echo "Press Ctrl+C to stop"
echo ""

java -cp "$CLASSPATH" io.amcp.examples.travel.TravelPlannerAgent

echo ""
echo "✈️  Travel Planner Demo Complete!"