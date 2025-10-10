package io.amcp.connectors.ai.gpu;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

/**
 * Unit tests for GPUAccelerationConfig
 */
public class GPUAccelerationConfigTest {
    
    @Test
    public void testGPUDetection() {
        GPUAccelerationConfig config = new GPUAccelerationConfig();
        
        assertNotNull(config.getDetectedGPU());
        assertNotNull(config.getAccelerationLevel());
        
        // Should detect at least CPU
        assertTrue(config.getDetectedGPU() != GPUAccelerationConfig.GPUType.UNKNOWN);
    }
    
    @Test
    public void testOllamaEnvironmentVariables() {
        GPUAccelerationConfig config = new GPUAccelerationConfig();
        
        Map<String, String> envVars = config.getOllamaEnvVars();
        assertNotNull(envVars);
        assertTrue(envVars.containsKey("OLLAMA_GPU_DRIVER"));
        assertTrue(envVars.containsKey("OLLAMA_NUM_THREAD"));
    }
    
    @Test
    public void testConfigSummary() {
        GPUAccelerationConfig config = new GPUAccelerationConfig();
        
        String summary = config.getConfigSummary();
        assertNotNull(summary);
        assertTrue(summary.contains("GPU Type"));
        assertTrue(summary.contains("VRAM Available"));
        assertTrue(summary.contains("GPU Layers"));
    }
    
    @Test
    public void testRecommendedModel() {
        GPUAccelerationConfig config = new GPUAccelerationConfig();
        
        String model = config.getRecommendedModel();
        assertNotNull(model);
        assertFalse(model.isEmpty());
        
        // Should recommend one of the supported models
        assertTrue(
            model.equals("qwen2:1.5b") || 
            model.equals("gemma:2b") || 
            model.equals("gemma:7b") || 
            model.equals("llama3.1:8b") ||
            model.equals("qwen2.5:0.5b"), // Include new ultra-minimal model
            "Unexpected model recommendation: " + model
        );
    }
    
    @Test
    public void testGPULayersCalculation() {
        GPUAccelerationConfig config = new GPUAccelerationConfig();
        
        int layers = config.getGpuLayers();
        
        // Should be -1 (full GPU), positive number, or 0 (CPU only)
        assertTrue(layers >= -1);
    }
}
