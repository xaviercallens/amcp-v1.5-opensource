#!/bin/bash

# AMCP CLI Runner Script
# Runs the AMCP Interactive CLI with all dependencies included

echo "🚀 Starting AMCP Interactive CLI v1.5.0"
echo "========================================"
echo ""

# Check if JAR exists
CLI_JAR="cli/target/amcp-cli-1.5.0.jar"

if [ ! -f "$CLI_JAR" ]; then
    echo "❌ CLI JAR not found. Building..."
    mvn clean package -DskipTests -q
    
    if [ $? -ne 0 ]; then
        echo "❌ Build failed. Please check the error messages above."
        exit 1
    fi
    echo "✅ Build successful!"
fi

# Run the CLI with the shaded JAR (includes all dependencies)
echo "🎮 Launching AMCP CLI..."
echo ""

java -jar "$CLI_JAR" "$@"

exit $?
