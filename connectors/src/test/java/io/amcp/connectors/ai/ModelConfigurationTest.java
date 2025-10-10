package io.amcp.connectors.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ModelConfiguration functionality.
 * Tests model recommendations, configurations, and Gemma 2B/Qwen2 prioritization.
 * 
 * @author AMCP Development Team
 * @version 1.5.1
 */
class ModelConfigurationTest {
    
    @Test
    @DisplayName("Test Gemma 2B prioritization for optimal RAM range")
    void testGemma2BPrioritization() {
        // Test that Gemma 2B is recommended for the optimal range (1.5-2.5GB)
        ModelConfiguration.LightweightModel model = ModelConfiguration.getRecommendedModel(2.0);
        assertEquals(ModelConfiguration.LightweightModel.GEMMA_2B, model);
        
        // Test edge cases
        model = ModelConfiguration.getRecommendedModel(1.6);
        assertEquals(ModelConfiguration.LightweightModel.GEMMA_2B, model);
        
        model = ModelConfiguration.getRecommendedModel(2.4);
        assertEquals(ModelConfiguration.LightweightModel.GEMMA_2B, model);
    }
    
    @Test
    @DisplayName("Test Qwen2.5:0.5b prioritization for ultra-minimal resources")
    void testQwen25UltraMinimalPrioritization() {
        // Test that Qwen2.5:0.5b is recommended for ultra-minimal resources (<0.8GB)
        ModelConfiguration.LightweightModel model = ModelConfiguration.getRecommendedModel(0.5);
        assertEquals(ModelConfiguration.LightweightModel.QWEN2_5_0_5B, model);
        
        model = ModelConfiguration.getRecommendedModel(0.7);
        assertEquals(ModelConfiguration.LightweightModel.QWEN2_5_0_5B, model);
        
        model = ModelConfiguration.getRecommendedModel(0.3);
        assertEquals(ModelConfiguration.LightweightModel.QWEN2_5_0_5B, model);
    }
    
    @Test
    @DisplayName("Test Qwen2 1.5B prioritization for minimal resources")
    void testQwen2MinimalResourcesPrioritization() {
        // Test that Qwen2 1.5B is recommended for minimal resources (0.8-1.5GB)
        ModelConfiguration.LightweightModel model = ModelConfiguration.getRecommendedModel(1.0);
        assertEquals(ModelConfiguration.LightweightModel.QWEN2_1_5B, model);
        
        model = ModelConfiguration.getRecommendedModel(1.4);
        assertEquals(ModelConfiguration.LightweightModel.QWEN2_1_5B, model);
        
        model = ModelConfiguration.getRecommendedModel(0.9);
        assertEquals(ModelConfiguration.LightweightModel.QWEN2_1_5B, model);
    }
    
    @Test
    @DisplayName("Test Qwen2 7B prioritization for high-end systems")
    void testQwen2HighEndPrioritization() {
        // Test that Qwen2 7B is recommended for 6-8GB range
        ModelConfiguration.LightweightModel model = ModelConfiguration.getRecommendedModel(7.0);
        assertEquals(ModelConfiguration.LightweightModel.QWEN2_7B, model);
        
        model = ModelConfiguration.getRecommendedModel(6.5);
        assertEquals(ModelConfiguration.LightweightModel.QWEN2_7B, model);
        
        model = ModelConfiguration.getRecommendedModel(7.9);
        assertEquals(ModelConfiguration.LightweightModel.QWEN2_7B, model);
    }
    
    @Test
    @DisplayName("Test complete RAM-based model recommendation hierarchy")
    void testCompleteRecommendationHierarchy() {
        // Test the complete hierarchy with NEW Qwen2.5:0.5b
        assertEquals(ModelConfiguration.LightweightModel.QWEN2_5_0_5B, 
                    ModelConfiguration.getRecommendedModel(0.5));
        
        assertEquals(ModelConfiguration.LightweightModel.QWEN2_1_5B, 
                    ModelConfiguration.getRecommendedModel(1.0));
        
        assertEquals(ModelConfiguration.LightweightModel.GEMMA_2B, 
                    ModelConfiguration.getRecommendedModel(2.0));
        
        assertEquals(ModelConfiguration.LightweightModel.PHI3_MINI, 
                    ModelConfiguration.getRecommendedModel(3.5));
        
        assertEquals(ModelConfiguration.LightweightModel.LLAMA3_8B, 
                    ModelConfiguration.getRecommendedModel(5.0));
        
        assertEquals(ModelConfiguration.LightweightModel.QWEN2_7B, 
                    ModelConfiguration.getRecommendedModel(7.0));
        
        assertEquals(ModelConfiguration.LightweightModel.LLAMA3_1_8B, 
                    ModelConfiguration.getRecommendedModel(10.0));
    }
    
    @Test
    @DisplayName("Test Gemma 2B optimized configuration")
    void testGemma2BOptimizedConfig() {
        ModelConfiguration.ModelConfig config = ModelConfiguration.getModelConfig(
            ModelConfiguration.LightweightModel.GEMMA_2B
        );
        
        assertNotNull(config);
        assertEquals("gemma:2b", config.getModelName());
        assertEquals(0.6, config.getTemperature(), 0.001); // Lower temperature for focused responses
        assertEquals(4096, config.getMaxTokens()); // Increased max tokens
        assertEquals(0.85, config.getTopP(), 0.001); // Optimized top_p
        assertEquals(8192, config.getContextWindow()); // Large context window
        assertTrue(config.supportsStreaming());
    }
    
    @Test
    @DisplayName("Test Qwen2.5:0.5b optimized configuration")
    void testQwen25_0_5BOptimizedConfig() {
        ModelConfiguration.ModelConfig config = ModelConfiguration.getModelConfig(
            ModelConfiguration.LightweightModel.QWEN2_5_0_5B
        );
        
        assertNotNull(config);
        assertEquals("qwen2.5:0.5b", config.getModelName());
        assertEquals(0.5, config.getTemperature(), 0.001); // Very low temperature for consistency
        assertEquals(2048, config.getMaxTokens()); // Reasonable token limit for 0.5B model
        assertEquals(0.8, config.getTopP(), 0.001); // Lower top_p for focused responses
        assertEquals(8192, config.getContextWindow()); // Good context window
        assertTrue(config.supportsStreaming());
    }
    
    @Test
    @DisplayName("Test Qwen2 1.5B optimized configuration")
    void testQwen2_1_5BOptimizedConfig() {
        ModelConfiguration.ModelConfig config = ModelConfiguration.getModelConfig(
            ModelConfiguration.LightweightModel.QWEN2_1_5B
        );
        
        assertNotNull(config);
        assertEquals("qwen2:1.5b", config.getModelName());
        assertEquals(0.6, config.getTemperature(), 0.001); // Lower temperature for consistency
        assertEquals(3072, config.getMaxTokens()); // Optimized token count
        assertEquals(0.9, config.getTopP(), 0.001); // Good diversity
        assertEquals(32768, config.getContextWindow()); // Qwen2's large context advantage
        assertTrue(config.supportsStreaming());
    }
    
    @Test
    @DisplayName("Test Qwen2 7B optimized configuration")
    void testQwen2_7BOptimizedConfig() {
        ModelConfiguration.ModelConfig config = ModelConfiguration.getModelConfig(
            ModelConfiguration.LightweightModel.QWEN2_7B
        );
        
        assertNotNull(config);
        assertEquals("qwen2:7b", config.getModelName());
        assertEquals(0.7, config.getTemperature(), 0.001);
        assertEquals(4096, config.getMaxTokens()); // Higher token limit for complex tasks
        assertEquals(0.9, config.getTopP(), 0.001);
        assertEquals(32768, config.getContextWindow()); // Large context
        assertTrue(config.supportsStreaming());
    }
    
    @Test
    @DisplayName("Test model configuration by name lookup")
    void testModelConfigByName() {
        // Test Gemma 2B lookup
        ModelConfiguration.ModelConfig config = ModelConfiguration.getModelConfigByName("gemma:2b");
        assertNotNull(config);
        assertEquals("gemma:2b", config.getModelName());
        assertEquals(0.6, config.getTemperature(), 0.001);
        
        // Test Qwen2 1.5B lookup
        config = ModelConfiguration.getModelConfigByName("qwen2:1.5b");
        assertNotNull(config);
        assertEquals("qwen2:1.5b", config.getModelName());
        assertEquals(32768, config.getContextWindow());
        
        // Test unknown model (should return default)
        config = ModelConfiguration.getModelConfigByName("unknown:model");
        assertNotNull(config);
        assertEquals("unknown:model", config.getModelName());
        assertEquals(0.7, config.getTemperature(), 0.001); // Default temperature
    }
    
    @Test
    @DisplayName("Test model RAM requirements")
    void testModelRAMRequirements() {
        // Test that prioritized models have correct RAM requirements
        assertEquals(1.4, ModelConfiguration.LightweightModel.GEMMA_2B.getRamRequirementGB(), 0.001);
        assertEquals(0.9, ModelConfiguration.LightweightModel.QWEN2_1_5B.getRamRequirementGB(), 0.001);
        assertEquals(4.4, ModelConfiguration.LightweightModel.QWEN2_7B.getRamRequirementGB(), 0.001);
        
        // Test that Gemma 2B is indeed lightweight
        assertTrue(ModelConfiguration.LightweightModel.GEMMA_2B.getRamRequirementGB() < 2.0);
        
        // Test that Qwen2 1.5B is ultra-lightweight
        assertTrue(ModelConfiguration.LightweightModel.QWEN2_1_5B.getRamRequirementGB() < 1.0);
    }
    
    @Test
    @DisplayName("Test model descriptions contain performance indicators")
    void testModelDescriptions() {
        String gemma2BDesc = ModelConfiguration.LightweightModel.GEMMA_2B.getDescription();
        assertTrue(gemma2BDesc.contains("FAST"));
        assertTrue(gemma2BDesc.contains("efficient"));
        assertTrue(gemma2BDesc.contains("RECOMMENDED"));
        
        String qwen2Desc = ModelConfiguration.LightweightModel.QWEN2_1_5B.getDescription();
        assertTrue(qwen2Desc.contains("Ultra-lightweight"));
        assertTrue(qwen2Desc.contains("FAST"));
    }
    
    @Test
    @DisplayName("Test available models list includes prioritized models")
    void testAvailableModelsList() {
        List<String> availableModels = ModelConfiguration.listAvailableModels();
        
        assertNotNull(availableModels);
        assertFalse(availableModels.isEmpty());
        
        // Check that prioritized models are included
        boolean hasGemma2B = availableModels.stream()
            .anyMatch(model -> model.contains("gemma:2b") && model.contains("RECOMMENDED"));
        assertTrue(hasGemma2B, "Gemma 2B should be in available models list with RECOMMENDED tag");
        
        boolean hasQwen2 = availableModels.stream()
            .anyMatch(model -> model.contains("qwen2:1.5b") && model.contains("Ultra-lightweight"));
        assertTrue(hasQwen2, "Qwen2 1.5B should be in available models list");
    }
    
    @Test
    @DisplayName("Test deployment instructions for prioritized models")
    void testDeploymentInstructions() {
        // Test Gemma 2B deployment instructions
        String gemmaInstructions = ModelConfiguration.getDeploymentInstructions(
            ModelConfiguration.LightweightModel.GEMMA_2B
        );
        
        assertNotNull(gemmaInstructions);
        assertTrue(gemmaInstructions.contains("gemma:2b"));
        assertTrue(gemmaInstructions.contains("ollama pull gemma:2b"));
        assertTrue(gemmaInstructions.contains("1.4GB"));
        
        // Test Qwen2 deployment instructions
        String qwenInstructions = ModelConfiguration.getDeploymentInstructions(
            ModelConfiguration.LightweightModel.QWEN2_1_5B
        );
        
        assertNotNull(qwenInstructions);
        assertTrue(qwenInstructions.contains("qwen2:1.5b"));
        assertTrue(qwenInstructions.contains("ollama pull qwen2:1.5b"));
        assertTrue(qwenInstructions.contains("0.9GB"));
    }
    
    @Test
    @DisplayName("Test system resource compatibility")
    void testSystemResourceCompatibility() {
        // Test that models can be validated against system resources
        assertTrue(ModelConfiguration.canRunModel(ModelConfiguration.LightweightModel.QWEN2_1_5B));
        
        // Test system info generation
        String systemInfo = ModelConfiguration.getSystemInfo();
        assertNotNull(systemInfo);
        assertTrue(systemInfo.contains("System Resources"));
        assertTrue(systemInfo.contains("Recommended Model"));
    }
    
    @Test
    @DisplayName("Test context window advantages")
    void testContextWindowAdvantages() {
        // Test that Qwen2 models have larger context windows
        ModelConfiguration.ModelConfig qwen2Config = ModelConfiguration.getModelConfig(
            ModelConfiguration.LightweightModel.QWEN2_1_5B
        );
        ModelConfiguration.ModelConfig gemmaConfig = ModelConfiguration.getModelConfig(
            ModelConfiguration.LightweightModel.GEMMA_2B
        );
        
        // Qwen2 should have larger context window
        assertTrue(qwen2Config.getContextWindow() > gemmaConfig.getContextWindow());
        assertEquals(32768, qwen2Config.getContextWindow());
        assertEquals(8192, gemmaConfig.getContextWindow());
    }
    
    @Test
    @DisplayName("Test performance-optimized parameters")
    void testPerformanceOptimizedParameters() {
        // Test that prioritized models have performance-optimized parameters
        ModelConfiguration.ModelConfig gemmaConfig = ModelConfiguration.getModelConfig(
            ModelConfiguration.LightweightModel.GEMMA_2B
        );
        
        // Lower temperature for more focused responses
        assertTrue(gemmaConfig.getTemperature() < 0.7);
        
        // Higher max tokens for better completions
        assertTrue(gemmaConfig.getMaxTokens() > 2048);
        
        ModelConfiguration.ModelConfig qwenConfig = ModelConfiguration.getModelConfig(
            ModelConfiguration.LightweightModel.QWEN2_1_5B
        );
        
        // Optimized token count
        assertEquals(3072, qwenConfig.getMaxTokens());
        
        // Lower temperature for consistency
        assertTrue(qwenConfig.getTemperature() < 0.7);
    }
    
    @Test
    @DisplayName("Test model enum values and ordering")
    void testModelEnumValues() {
        ModelConfiguration.LightweightModel[] models = ModelConfiguration.LightweightModel.values();
        
        // Check that prioritized models are at the top (NEW Qwen2.5:0.5b is now first)
        assertEquals(ModelConfiguration.LightweightModel.QWEN2_5_0_5B, models[0]);
        assertEquals(ModelConfiguration.LightweightModel.GEMMA_2B, models[1]);
        assertEquals(ModelConfiguration.LightweightModel.QWEN2_1_5B, models[2]);
        
        // Verify all expected models are present
        boolean hasQwen25_0_5B = false, hasGemma2B = false, hasQwen2_1_5B = false, hasQwen2_7B = false;
        
        for (ModelConfiguration.LightweightModel model : models) {
            if (model == ModelConfiguration.LightweightModel.QWEN2_5_0_5B) hasQwen25_0_5B = true;
            if (model == ModelConfiguration.LightweightModel.GEMMA_2B) hasGemma2B = true;
            if (model == ModelConfiguration.LightweightModel.QWEN2_1_5B) hasQwen2_1_5B = true;
            if (model == ModelConfiguration.LightweightModel.QWEN2_7B) hasQwen2_7B = true;
        }
        
        assertTrue(hasQwen25_0_5B, "Qwen2.5:0.5b should be in enum");
        assertTrue(hasGemma2B, "Gemma 2B should be in enum");
        assertTrue(hasQwen2_1_5B, "Qwen2 1.5B should be in enum");
        assertTrue(hasQwen2_7B, "Qwen2 7B should be in enum");
    }
}
