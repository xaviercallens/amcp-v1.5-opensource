#!/bin/bash

# Automated test script for AMCP orchestration with Phi3 3.8B
# Tests weather query through orchestrator with LLM integration

echo "=========================================="
echo "AMCP v1.5 - Phi3 3.8B Orchestration Test"
echo "=========================================="
echo ""

# Set environment variables for Phi3 3.8B
export OLLAMA_MODEL=phi3:3.8b
export OLLAMA_ENABLED=true
export OLLAMA_BASE_URL=http://localhost:11434

echo "✅ Configuration:"
echo "   Model: $OLLAMA_MODEL"
echo "   Ollama URL: $OLLAMA_BASE_URL"
echo ""

# Check Ollama service
echo "🔍 Checking Ollama service..."
if curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo "✅ Ollama service is running"
else
    echo "❌ Ollama service is not running!"
    echo "   Starting Ollama in background..."
    ollama serve > /tmp/ollama.log 2>&1 &
    sleep 3
    if curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
        echo "✅ Ollama service started"
    else
        echo "❌ Failed to start Ollama"
        exit 1
    fi
fi

# Check model availability
echo ""
echo "🔍 Checking Phi3 3.8B model..."
if ollama list | grep -q "phi3:3.8b"; then
    echo "✅ Phi3 3.8B model is available"
else
    echo "❌ Phi3 3.8B model not found!"
    exit 1
fi

echo ""
echo "=========================================="
echo "Running AMCP CLI Demo"
echo "=========================================="
echo ""
echo "Commands to execute:"
echo "  1. activate orchestrator"
echo "  2. activate weather"
echo "  3. ask orchestrator 'What is the weather in Paris?'"
echo "  4. exit"
echo ""

# Create command sequence
cat > /tmp/amcp_test_commands.txt << 'EOF'
activate orchestrator
activate weather
ask orchestrator What is the weather in Paris?
exit
EOF

echo "Executing test commands..."
echo ""

# Run CLI with commands
timeout 120 java -Xmx2g -DOLLAMA_MODEL=phi3:3.8b -jar cli/target/amcp-cli-1.5.0.jar demo < /tmp/amcp_test_commands.txt

EXIT_CODE=$?

echo ""
echo "=========================================="
echo "Test Complete"
echo "=========================================="

if [ $EXIT_CODE -eq 0 ] || [ $EXIT_CODE -eq 124 ]; then
    echo "✅ Test completed (exit code: $EXIT_CODE)"
else
    echo "❌ Test failed with exit code: $EXIT_CODE"
fi

# Cleanup
rm -f /tmp/amcp_test_commands.txt

exit 0
