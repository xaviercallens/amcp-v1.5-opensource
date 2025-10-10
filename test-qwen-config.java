import io.amcp.connectors.ai.ModelConfiguration;
import io.amcp.connectors.ai.async.AsyncLLMConnector;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class TestQwenConfig {
    public static void main(String[] args) {
        System.out.println("🚀 AMCP Qwen2.5:0.5b Configuration Test");
        System.out.println("=====================================");
        
        // Test 1: Model Configuration
        System.out.println("\n📋 Test 1: Model Configuration");
        var qwen25 = ModelConfiguration.LightweightModel.QWEN2_5_0_5B;
        var config = ModelConfiguration.getModelConfig(qwen25);
        
        System.out.println("✅ Model: " + qwen25.getModelName());
        System.out.println("✅ RAM Required: " + qwen25.getRamRequirementGB() + "GB");
        System.out.println("✅ Description: " + qwen25.getDescription());
        System.out.println("✅ Temperature: " + config.getTemperature());
        System.out.println("✅ Max Tokens: " + config.getMaxTokens());
        System.out.println("✅ Context Window: " + config.getContextWindow());
        
        // Test 2: System Recommendation
        System.out.println("\n📋 Test 2: System Recommendations");
        System.out.println("For 0.5GB RAM: " + ModelConfiguration.getRecommendedModel(0.5).getModelName());
        System.out.println("For 1.0GB RAM: " + ModelConfiguration.getRecommendedModel(1.0).getModelName());
        System.out.println("For 2.0GB RAM: " + ModelConfiguration.getRecommendedModel(2.0).getModelName());
        
        // Test 3: Async Connector Test
        System.out.println("\n📋 Test 3: Async Connector Test");
        AsyncLLMConnector connector = new AsyncLLMConnector();
        
        try {
            String prompt = "What is 2+2?";
            String model = "qwen2.5:0.5b";
            Map<String, Object> parameters = Map.of("temperature", 0.5);
            
            System.out.println("Sending request to " + model + "...");
            CompletableFuture<String> future = connector.generateAsync(prompt, model, parameters);
            
            try {
                String response = future.get(10, TimeUnit.SECONDS);
                System.out.println("✅ Response received: " + response.substring(0, Math.min(100, response.length())) + "...");
            } catch (Exception e) {
                if (e.getMessage().contains("404")) {
                    System.out.println("⚠️  Model not found - this is expected if Ollama isn't running");
                } else {
                    System.out.println("⚠️  Connection issue: " + e.getMessage());
                }
            }
            
        } finally {
            connector.shutdown();
        }
        
        // Test 4: Available Models
        System.out.println("\n📋 Test 4: Available Models");
        var availableModels = ModelConfiguration.listAvailableModels();
        availableModels.stream()
            .filter(model -> model.contains("qwen2.5") || model.contains("gemma:2b"))
            .forEach(model -> System.out.println("✅ " + model));
        
        System.out.println("\n🎉 Configuration test completed!");
        System.out.println("\n💡 Next steps:");
        System.out.println("   1. Test with: echo 'Hello' | ollama run qwen2.5:0.5b");
        System.out.println("   2. Interactive: ollama run qwen2.5:0.5b");
        System.out.println("   3. AMCP Tester: mvn exec:java -Dexec.mainClass=\"io.amcp.connectors.ai.terminal.LocalModelTester\"");
    }
}
