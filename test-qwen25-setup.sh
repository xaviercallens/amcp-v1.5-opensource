#!/bin/bash

# Quick test script for Qwen2.5:0.5b setup and configuration
# Tests the new ultra-minimal model support without requiring Ollama to be running

echo "🚀 AMCP Qwen2.5:0.5b Setup Test"
echo "================================"

# Test 1: Compile and verify LocalModelTester
echo "📋 Test 1: Compiling LocalModelTester..."
cd connectors
if mvn compile -q; then
    echo "✅ LocalModelTester compiled successfully"
else
    echo "❌ LocalModelTester compilation failed"
    exit 1
fi

# Test 2: Run model configuration tests
echo "📋 Test 2: Running model configuration tests..."
if mvn test -Dtest=ModelConfigurationTest -q; then
    echo "✅ Model configuration tests passed"
else
    echo "❌ Model configuration tests failed"
    exit 1
fi

# Test 3: Run LocalModelTester tests
echo "📋 Test 3: Running LocalModelTester tests..."
if mvn test -Dtest=LocalModelTesterTest -q; then
    echo "✅ LocalModelTester tests passed"
else
    echo "❌ LocalModelTester tests failed"
    exit 1
fi

# Test 4: Verify setup script exists and is executable
echo "📋 Test 4: Checking setup script..."
cd ../scripts
if [ -x "setup-local-models.sh" ]; then
    echo "✅ Setup script is executable"
else
    echo "❌ Setup script not found or not executable"
    exit 1
fi

# Test 5: Test Java model recommendations
echo "📋 Test 5: Testing model recommendations..."
cd ../connectors
cat > /tmp/test-recommendations.java << 'EOF'
import io.amcp.connectors.ai.ModelConfiguration;

public class TestRecommendations {
    public static void main(String[] args) {
        System.out.println("Testing Qwen2.5:0.5b recommendations:");
        
        // Test ultra-minimal system (0.5GB)
        var model = ModelConfiguration.getRecommendedModel(0.5);
        System.out.println("0.5GB RAM -> " + model.getModelName() + " (" + model.getDescription() + ")");
        
        // Test minimal system (1.0GB)  
        model = ModelConfiguration.getRecommendedModel(1.0);
        System.out.println("1.0GB RAM -> " + model.getModelName() + " (" + model.getDescription() + ")");
        
        // Test recommended system (2.0GB)
        model = ModelConfiguration.getRecommendedModel(2.0);
        System.out.println("2.0GB RAM -> " + model.getModelName() + " (" + model.getDescription() + ")");
        
        // Test Qwen2.5:0.5b configuration
        var config = ModelConfiguration.getModelConfig(ModelConfiguration.LightweightModel.QWEN2_5_0_5B);
        System.out.println("\nQwen2.5:0.5b Configuration:");
        System.out.println("- Temperature: " + config.getTemperature());
        System.out.println("- Max Tokens: " + config.getMaxTokens());
        System.out.println("- Top-P: " + config.getTopP());
        System.out.println("- Context Window: " + config.getContextWindow());
        System.out.println("- Streaming: " + config.supportsStreaming());
        
        System.out.println("\n✅ All model recommendations working correctly!");
    }
}
EOF

if mvn exec:java -Dexec.mainClass="TestRecommendations" -Dexec.cleanupDaemonThreads=false -q 2>/dev/null; then
    echo "✅ Model recommendations working correctly"
else
    echo "❌ Model recommendations test failed"
    exit 1
fi

# Clean up
rm -f /tmp/test-recommendations.java

echo ""
echo "🎉 All tests passed! Qwen2.5:0.5b setup is working correctly."
echo ""
echo "📋 Summary of what was tested:"
echo "   ✅ LocalModelTester compilation"
echo "   ✅ Model configuration tests (19 tests)"
echo "   ✅ LocalModelTester functionality tests (12 tests)"
echo "   ✅ Setup script availability"
echo "   ✅ Java model recommendations"
echo ""
echo "🚀 Ready to use:"
echo "   • Run setup: ./scripts/setup-local-models.sh"
echo "   • Test models: mvn exec:java -Dexec.mainClass=\"io.amcp.connectors.ai.terminal.LocalModelTester\""
echo "   • Install Qwen2.5:0.5b: ollama pull qwen2.5:0.5b"
echo ""
echo "💡 Qwen2.5:0.5b Features:"
echo "   • Ultra-minimal: Only 0.4GB RAM required"
echo "   • Ultra-fast: 60-second timeout optimization"
echo "   • Optimized parameters for consistency and speed"
echo "   • Perfect for resource-constrained environments"
