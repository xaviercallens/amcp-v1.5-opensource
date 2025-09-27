#!/bin/bash

# AMCP v1.5 - Weather System Demo Script
# Real-time weather monitoring with agent-based architecture

set -e

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "🌤️  AMCP v1.5 - Weather System Demo"
echo "==================================="

# Change to project directory
cd "$PROJECT_DIR"

# Check if project is built
if [ ! -f "target/classes/io/amcp/examples/weather/WeatherSystemLauncher.class" ]; then
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
echo "🔧 Weather System Configuration:"

if [ -n "$OPENWEATHER_API_KEY" ]; then
    echo "✅ OpenWeather API Key: configured"
    echo "   • Real weather data will be used"
else
    echo "🔧 OpenWeather API Key: not set"
    echo "   • Demo mode with simulated data"
    echo "   • Set OPENWEATHER_API_KEY for live data"
fi

# Display default locations
echo ""
echo "📍 Default Monitoring Locations:"
echo "   • New York, NY"
echo "   • London, UK"  
echo "   • Tokyo, Japan"
echo "   • Sydney, Australia"
echo ""

# Run the Weather System
echo "🚀 Starting Weather Collection System..."
echo "Interactive CLI will start - type 'help' for commands"
echo "Press Ctrl+C to stop"
echo ""

java -cp "$CLASSPATH" io.amcp.examples.weather.WeatherSystemLauncher

echo ""
echo "🌤️  Weather System Demo Complete!"