package io.amcp.connectors.ai.terminal;

import io.amcp.connectors.ai.ModelConfiguration;
import io.amcp.connectors.ai.async.AsyncLLMConnector;
import org.junit.jupiter.api.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LocalModelTester functionality.
 * Tests the terminal-based model testing utility and configuration tools.
 * 
 * @author AMCP Development Team
 * @version 1.5.1
 */
class LocalModelTesterTest {
    
    private LocalModelTester tester;
    
    @BeforeEach
    void setUp() {
        tester = new LocalModelTester();
    }
    
    @AfterEach
    void tearDown() {
        if (tester != null) {
            tester.shutdown();
        }
    }
    
    @Test
    @DisplayName("Test LocalModelTester initialization")
    void testInitialization() {
        assertNotNull(tester);
        // Test should not throw any exceptions during initialization
        assertDoesNotThrow(() -> new LocalModelTester());
    }
    
    @Test
    @DisplayName("Test Qwen2.5:0.5b model configuration")
    void testQwen25ModelConfiguration() {
        // Test that Qwen2.5:0.5b is properly configured
        ModelConfiguration.LightweightModel qwen25 = ModelConfiguration.LightweightModel.QWEN2_5_0_5B;
        
        assertEquals("qwen2.5:0.5b", qwen25.getModelName());
        assertEquals(0.4, qwen25.getRamRequirementGB(), 0.001);
        assertTrue(qwen25.getDescription().contains("ULTRA-FAST"));
        assertTrue(qwen25.getDescription().contains("NEW"));
        
        // Test configuration
        ModelConfiguration.ModelConfig config = ModelConfiguration.getModelConfig(qwen25);
        assertEquals(0.5, config.getTemperature(), 0.001); // Very low for consistency
        assertEquals(2048, config.getMaxTokens());
        assertEquals(0.8, config.getTopP(), 0.001);
        assertEquals(8192, config.getContextWindow());
        assertTrue(config.supportsStreaming());
    }
    
    @Test
    @DisplayName("Test model recommendation for ultra-minimal systems")
    void testUltraMinimalRecommendation() {
        // Test that Qwen2.5:0.5b is recommended for very low RAM systems
        ModelConfiguration.LightweightModel recommended;
        
        recommended = ModelConfiguration.getRecommendedModel(0.3);
        assertEquals(ModelConfiguration.LightweightModel.QWEN2_5_0_5B, recommended);
        
        recommended = ModelConfiguration.getRecommendedModel(0.5);
        assertEquals(ModelConfiguration.LightweightModel.QWEN2_5_0_5B, recommended);
        
        recommended = ModelConfiguration.getRecommendedModel(0.7);
        assertEquals(ModelConfiguration.LightweightModel.QWEN2_5_0_5B, recommended);
        
        // At 0.8GB, should switch to Qwen2 1.5B
        recommended = ModelConfiguration.getRecommendedModel(0.9);
        assertEquals(ModelConfiguration.LightweightModel.QWEN2_1_5B, recommended);
    }
    
    @Test
    @DisplayName("Test async connector with Qwen2.5:0.5b")
    void testAsyncConnectorWithQwen25() {
        AsyncLLMConnector connector = new AsyncLLMConnector();
        
        try {
            String prompt = "Test prompt for Qwen2.5:0.5b";
            String model = "qwen2.5:0.5b";
            Map<String, Object> parameters = Map.of("temperature", 0.5);
            
            CompletableFuture<String> future = connector.generateAsync(prompt, model, parameters);
            
            assertNotNull(future);
            assertFalse(future.isDone()); // Should be async
            
            // Test timeout behavior (should complete within 60s for Qwen2.5:0.5b or fail gracefully)
            assertDoesNotThrow(() -> {
                try {
                    String result = future.get(65, TimeUnit.SECONDS); // Slightly more than optimized timeout
                    // If Ollama is running with Qwen2.5:0.5b, we should get a response
                    assertNotNull(result);
                } catch (Exception e) {
                    // Expected if Ollama is not running or model not available
                    String message = e.getMessage();
                    assertTrue((message != null && (message.contains("Connection refused") || 
                              message.contains("failed") ||
                              message.contains("404"))) ||
                              e.getCause() instanceof java.net.ConnectException);
                }
            });
            
        } finally {
            connector.shutdown();
        }
    }
    
    @Test
    @DisplayName("Test model deployment instructions")
    void testModelDeploymentInstructions() {
        ModelConfiguration.LightweightModel qwen25 = ModelConfiguration.LightweightModel.QWEN2_5_0_5B;
        
        String instructions = ModelConfiguration.getDeploymentInstructions(qwen25);
        
        assertNotNull(instructions);
        assertTrue(instructions.contains("qwen2.5:0.5b"));
        assertTrue(instructions.contains("ollama pull qwen2.5:0.5b"));
        assertTrue(instructions.contains("0.4GB"));
        assertTrue(instructions.contains("Deployment Instructions"));
    }
    
    @Test
    @DisplayName("Test system resource compatibility")
    void testSystemResourceCompatibility() {
        // Qwen2.5:0.5b should be runnable on almost any system
        assertTrue(ModelConfiguration.canRunModel(ModelConfiguration.LightweightModel.QWEN2_5_0_5B));
        
        // Test system info generation includes new model
        String systemInfo = ModelConfiguration.getSystemInfo();
        assertNotNull(systemInfo);
        assertTrue(systemInfo.contains("System Resources"));
        assertTrue(systemInfo.contains("Recommended Model"));
    }
    
    @Test
    @DisplayName("Test available models list includes Qwen2.5:0.5b")
    void testAvailableModelsList() {
        var availableModels = ModelConfiguration.listAvailableModels();
        
        assertNotNull(availableModels);
        assertFalse(availableModels.isEmpty());
        
        // Check that Qwen2.5:0.5b is included with proper description
        boolean hasQwen25 = availableModels.stream()
            .anyMatch(model -> model.contains("qwen2.5:0.5b") && 
                              model.contains("ULTRA-FAST") && 
                              model.contains("NEW"));
        assertTrue(hasQwen25, "Qwen2.5:0.5b should be in available models list with proper tags");
    }
    
    @Test
    @DisplayName("Test model configuration by name lookup for Qwen2.5:0.5b")
    void testModelConfigByNameLookup() {
        ModelConfiguration.ModelConfig config = ModelConfiguration.getModelConfigByName("qwen2.5:0.5b");
        
        assertNotNull(config);
        assertEquals("qwen2.5:0.5b", config.getModelName());
        assertEquals(0.5, config.getTemperature(), 0.001);
        assertEquals(2048, config.getMaxTokens());
        assertEquals(0.8, config.getTopP(), 0.001);
        assertEquals(8192, config.getContextWindow());
        assertTrue(config.supportsStreaming());
    }
    
    @Test
    @DisplayName("Test GPU acceleration recommendations include Qwen2.5:0.5b")
    void testGPURecommendations() {
        // For very low VRAM systems, should recommend Qwen2.5:0.5b
        io.amcp.connectors.ai.gpu.GPUAccelerationConfig gpuConfig = 
            new io.amcp.connectors.ai.gpu.GPUAccelerationConfig();
        
        String recommended = gpuConfig.getRecommendedModel();
        assertNotNull(recommended);
        
        // The actual recommendation depends on detected GPU, but the method should work
        assertTrue(recommended.contains(":") && recommended.contains("b")); // Should be a valid model name
    }
    
    @Test
    @DisplayName("Test LocalModelTester shutdown")
    void testShutdown() {
        LocalModelTester testTester = new LocalModelTester();
        
        // Should shutdown gracefully without exceptions
        assertDoesNotThrow(() -> testTester.shutdown());
    }
    
    @Test
    @DisplayName("Test model performance characteristics")
    void testModelPerformanceCharacteristics() {
        // Test that Qwen2.5:0.5b has the expected performance characteristics
        ModelConfiguration.LightweightModel qwen25 = ModelConfiguration.LightweightModel.QWEN2_5_0_5B;
        ModelConfiguration.ModelConfig config = ModelConfiguration.getModelConfig(qwen25);
        
        // Should have very conservative settings for stability
        assertTrue(config.getTemperature() <= 0.5, "Temperature should be very low for consistency");
        assertTrue(config.getTopP() <= 0.8, "Top-P should be conservative for focused responses");
        assertTrue(config.getMaxTokens() <= 2048, "Token limit should be reasonable for 0.5B model");
        
        // Should still have good context window
        assertTrue(config.getContextWindow() >= 8192, "Should have decent context window");
        
        // Should support streaming
        assertTrue(config.supportsStreaming(), "Should support streaming responses");
    }
    
    @Test
    @DisplayName("Test model hierarchy consistency")
    void testModelHierarchyConsistency() {
        // Test that the model hierarchy makes sense
        ModelConfiguration.LightweightModel[] models = ModelConfiguration.LightweightModel.values();
        
        // Qwen2.5:0.5b should be first (smallest RAM requirement)
        assertEquals(ModelConfiguration.LightweightModel.QWEN2_5_0_5B, models[0]);
        
        // RAM requirements should generally increase (with some exceptions for different model families)
        double qwen25Ram = ModelConfiguration.LightweightModel.QWEN2_5_0_5B.getRamRequirementGB();
        double qwen2Ram = ModelConfiguration.LightweightModel.QWEN2_1_5B.getRamRequirementGB();
        double gemmaRam = ModelConfiguration.LightweightModel.GEMMA_2B.getRamRequirementGB();
        
        assertTrue(qwen25Ram < qwen2Ram, "Qwen2.5:0.5b should require less RAM than Qwen2 1.5B");
        assertTrue(qwen25Ram < gemmaRam, "Qwen2.5:0.5b should require less RAM than Gemma 2B");
        assertTrue(qwen25Ram == 0.4, "Qwen2.5:0.5b should require exactly 0.4GB RAM");
    }
}
