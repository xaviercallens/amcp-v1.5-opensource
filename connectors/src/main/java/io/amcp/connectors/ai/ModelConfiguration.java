package io.amcp.connectors.ai;

import java.util.*;

/**
 * Configuration for lightweight LLM models suitable for local deployment.
 * Optimized for resource-constrained environments (2-8GB RAM).
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class ModelConfiguration {
    
    /**
     * Recommended lightweight models for local deployment
     */
    public enum LightweightModel {
        // Ultra-light models (< 2GB RAM)
        PHI3_MINI("phi3:3.8b", 2.3, "Microsoft Phi-3 Mini - Excellent quality, 3.8B parameters"),
        GEMMA_2B("gemma:2b", 1.4, "Google Gemma 2B - Fast and efficient"),
        QWEN2_1_5B("qwen2:1.5b", 0.9, "Alibaba Qwen2 1.5B - Very lightweight"),
        
        // Light models (2-4GB RAM)
        PHI3_MEDIUM("phi3:medium", 7.9, "Microsoft Phi-3 Medium - Better reasoning, 14B parameters"),
        LLAMA3_8B("llama3:8b", 4.7, "Meta Llama 3 8B - Balanced performance"),
        MISTRAL_7B("mistral:7b", 4.1, "Mistral 7B - Strong general purpose"),
        GEMMA_7B("gemma:7b", 4.8, "Google Gemma 7B - Good quality"),
        
        // Medium models (4-8GB RAM)
        LLAMA3_1_8B("llama3.1:8b", 4.7, "Meta Llama 3.1 8B - Latest version"),
        QWEN2_7B("qwen2:7b", 4.4, "Alibaba Qwen2 7B - Multilingual"),
        
        // Fallback (legacy)
        TINYLLAMA("tinyllama", 0.6, "TinyLlama 1.1B - Minimal resource usage");
        
        private final String modelName;
        private final double ramRequirementGB;
        private final String description;
        
        LightweightModel(String modelName, double ramRequirementGB, String description) {
            this.modelName = modelName;
            this.ramRequirementGB = ramRequirementGB;
            this.description = description;
        }
        
        public String getModelName() { return modelName; }
        public double getRamRequirementGB() { return ramRequirementGB; }
        public String getDescription() { return description; }
    }
    
    /**
     * Model-specific configuration parameters
     */
    public static class ModelConfig {
        private final String modelName;
        private final double temperature;
        private final int maxTokens;
        private final double topP;
        private final int contextWindow;
        private final boolean supportsStreaming;
        
        public ModelConfig(String modelName, double temperature, int maxTokens, 
                          double topP, int contextWindow, boolean supportsStreaming) {
            this.modelName = modelName;
            this.temperature = temperature;
            this.maxTokens = maxTokens;
            this.topP = topP;
            this.contextWindow = contextWindow;
            this.supportsStreaming = supportsStreaming;
        }
        
        public String getModelName() { return modelName; }
        public double getTemperature() { return temperature; }
        public int getMaxTokens() { return maxTokens; }
        public double getTopP() { return topP; }
        public int getContextWindow() { return contextWindow; }
        public boolean supportsStreaming() { return supportsStreaming; }
    }
    
    /**
     * Get recommended model configuration based on available RAM
     */
    public static LightweightModel getRecommendedModel(double availableRamGB) {
        if (availableRamGB < 2.0) {
            return LightweightModel.QWEN2_1_5B;
        } else if (availableRamGB < 3.0) {
            return LightweightModel.PHI3_MINI;
        } else if (availableRamGB < 5.0) {
            return LightweightModel.LLAMA3_8B;
        } else if (availableRamGB < 8.0) {
            return LightweightModel.LLAMA3_1_8B;
        } else {
            return LightweightModel.PHI3_MEDIUM;
        }
    }
    
    /**
     * Get optimized configuration for a specific model
     */
    public static ModelConfig getModelConfig(LightweightModel model) {
        switch (model) {
            case PHI3_MINI:
                return new ModelConfig(
                    model.getModelName(),
                    0.7,    // temperature
                    2048,   // max tokens
                    0.9,    // top_p
                    4096,   // context window
                    true    // streaming support
                );
                
            case PHI3_MEDIUM:
                return new ModelConfig(
                    model.getModelName(),
                    0.7,
                    4096,
                    0.9,
                    8192,
                    true
                );
                
            case LLAMA3_8B:
            case LLAMA3_1_8B:
                return new ModelConfig(
                    model.getModelName(),
                    0.7,
                    2048,
                    0.9,
                    8192,
                    true
                );
                
            case MISTRAL_7B:
                return new ModelConfig(
                    model.getModelName(),
                    0.7,
                    2048,
                    0.9,
                    8192,
                    true
                );
                
            case GEMMA_2B:
            case GEMMA_7B:
                return new ModelConfig(
                    model.getModelName(),
                    0.7,
                    2048,
                    0.9,
                    8192,
                    true
                );
                
            case QWEN2_1_5B:
            case QWEN2_7B:
                return new ModelConfig(
                    model.getModelName(),
                    0.7,
                    2048,
                    0.9,
                    32768,  // Qwen2 has large context
                    true
                );
                
            case TINYLLAMA:
            default:
                return new ModelConfig(
                    model.getModelName(),
                    0.7,
                    512,
                    0.9,
                    2048,
                    true
                );
        }
    }
    
    /**
     * Get model configuration by name
     */
    public static ModelConfig getModelConfigByName(String modelName) {
        for (LightweightModel model : LightweightModel.values()) {
            if (model.getModelName().equals(modelName)) {
                return getModelConfig(model);
            }
        }
        // Default configuration for unknown models
        return new ModelConfig(modelName, 0.7, 2048, 0.9, 4096, true);
    }
    
    /**
     * List all available lightweight models
     */
    public static List<String> listAvailableModels() {
        List<String> models = new ArrayList<>();
        for (LightweightModel model : LightweightModel.values()) {
            models.add(String.format("%s (%.1fGB RAM) - %s", 
                model.getModelName(), 
                model.getRamRequirementGB(), 
                model.getDescription()));
        }
        return models;
    }
    
    /**
     * Get deployment instructions for a model
     */
    public static String getDeploymentInstructions(LightweightModel model) {
        StringBuilder instructions = new StringBuilder();
        instructions.append("Deployment Instructions for ").append(model.getModelName()).append(":\n\n");
        instructions.append("1. Install Ollama:\n");
        instructions.append("   curl -fsSL https://ollama.com/install.sh | sh\n\n");
        instructions.append("2. Pull the model:\n");
        instructions.append("   ollama pull ").append(model.getModelName()).append("\n\n");
        instructions.append("3. Test the model:\n");
        instructions.append("   ollama run ").append(model.getModelName()).append("\n\n");
        instructions.append("4. Set environment variable:\n");
        instructions.append("   export OLLAMA_MODEL=").append(model.getModelName()).append("\n\n");
        instructions.append("Requirements:\n");
        instructions.append("- RAM: ").append(model.getRamRequirementGB()).append("GB minimum\n");
        instructions.append("- Disk: ~").append((int)(model.getRamRequirementGB() * 1.5)).append("GB\n");
        instructions.append("- CPU: Modern multi-core processor recommended\n");
        
        return instructions.toString();
    }
    
    /**
     * Validate if system has enough resources for a model
     */
    public static boolean canRunModel(LightweightModel model) {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        double availableGB = maxMemory / (1024.0 * 1024.0 * 1024.0);
        
        return availableGB >= model.getRamRequirementGB();
    }
    
    /**
     * Get system resource information
     */
    public static String getSystemInfo() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        
        double maxGB = maxMemory / (1024.0 * 1024.0 * 1024.0);
        double totalGB = totalMemory / (1024.0 * 1024.0 * 1024.0);
        double freeGB = freeMemory / (1024.0 * 1024.0 * 1024.0);
        
        StringBuilder info = new StringBuilder();
        info.append("System Resources:\n");
        info.append(String.format("- Max Memory: %.2f GB\n", maxGB));
        info.append(String.format("- Total Memory: %.2f GB\n", totalGB));
        info.append(String.format("- Free Memory: %.2f GB\n", freeGB));
        info.append(String.format("- Processors: %d\n", runtime.availableProcessors()));
        
        LightweightModel recommended = getRecommendedModel(maxGB);
        info.append(String.format("\nRecommended Model: %s (%.1fGB RAM)\n", 
            recommended.getModelName(), recommended.getRamRequirementGB()));
        
        return info.toString();
    }
}
