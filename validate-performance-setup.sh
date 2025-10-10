#!/bin/bash

# AMCP Performance Setup Validation Script

echo "🔍 AMCP Performance Setup Validation"
echo "===================================="

# Check if optimizations are loaded
if [ -f ~/.amcp-env ]; then
    source ~/.amcp-env
    echo "✅ AMCP environment loaded"
    
    if [ "$AMCP_PERFORMANCE_MODE" = "optimized" ]; then
        echo "✅ Performance mode: OPTIMIZED"
    else
        echo "⚠️  Performance mode: Standard"
    fi
else
    echo "❌ AMCP environment not found"
    exit 1
fi

# Check Ollama status
if pgrep -x "ollama" > /dev/null; then
    echo "✅ Ollama: Running"
    
    # Check if models are loaded
    if ollama ps 2>/dev/null | grep -q "qwen2.5:0.5b"; then
        echo "✅ Qwen2.5:0.5b: Loaded and ready"
    else
        echo "⚠️  Qwen2.5:0.5b: Available but not loaded"
    fi
else
    echo "❌ Ollama: Not running"
    echo "💡 Start with: ~/.amcp/start-ollama-optimized.sh"
fi

# Check performance tools
echo ""
echo "🛠️  Performance Tools Status:"

if [ -x ~/.amcp/start-ollama-optimized.sh ]; then
    echo "✅ Optimized startup script"
else
    echo "❌ Optimized startup script missing"
fi

if [ -x ~/.amcp/monitor-performance.sh ]; then
    echo "✅ Performance monitoring script"
else
    echo "❌ Performance monitoring script missing"
fi

if [ -x ~/.amcp/test-performance.sh ]; then
    echo "✅ Performance testing script"
else
    echo "❌ Performance testing script missing"
fi

if [ -f ~/.amcp/model-profiles.json ]; then
    echo "✅ Model optimization profiles"
else
    echo "❌ Model optimization profiles missing"
fi

# Check system resources
echo ""
echo "💻 System Resources:"
TOTAL_RAM=$(free -h | grep 'Mem:' | awk '{print $2}')
USED_RAM=$(free -h | grep 'Mem:' | awk '{print $3}')
echo "   RAM: $USED_RAM / $TOTAL_RAM"

if pgrep -x "ollama" > /dev/null; then
    OLLAMA_PID=$(pgrep -x "ollama")
    OLLAMA_MEM=$(ps -p $OLLAMA_PID -o rss= 2>/dev/null | awk '{print $1/1024}' 2>/dev/null || echo "N/A")
    echo "   Ollama: ${OLLAMA_MEM}MB"
fi

# Test basic functionality
echo ""
echo "🧪 Basic Functionality Test:"
echo "Testing Qwen2.5:0.5b response time..."

start_time=$(date +%s.%N)
if timeout 10 bash -c "echo 'Hi' | ollama run qwen2.5:0.5b >/dev/null 2>&1"; then
    end_time=$(date +%s.%N)
    duration=$(echo "$end_time - $start_time" | bc 2>/dev/null || echo "N/A")
    echo "✅ Response time: ${duration}s"
else
    echo "⚠️  Response test timed out or failed"
fi

# Performance configuration summary
echo ""
echo "⚙️  Performance Configuration:"
echo "   Max Models: ${AMCP_MAX_LOADED_MODELS:-2}"
echo "   Parallel Requests: ${AMCP_PARALLEL_REQUESTS:-2}"
echo "   Context Size: ${AMCP_CONTEXT_SIZE:-32768}"
echo "   Keep Alive: ${AMCP_KEEP_ALIVE:--1}"
echo "   GPU Enabled: ${AMCP_GPU_ENABLED:-false}"

# Quick commands
echo ""
echo "🚀 Quick Commands:"
echo "   Monitor: ~/.amcp/monitor-performance.sh"
echo "   Test: ~/.amcp/test-performance.sh"
echo "   Chat: ollama run qwen2.5:0.5b"
echo "   AMCP Tester: cd connectors && mvn exec:java -Dexec.mainClass=\"io.amcp.connectors.ai.terminal.LocalModelTester\""

echo ""
if [ "$AMCP_PERFORMANCE_MODE" = "optimized" ] && pgrep -x "ollama" > /dev/null; then
    echo "🎉 Performance setup is READY and OPTIMIZED!"
else
    echo "⚠️  Performance setup needs attention - check the issues above"
fi
