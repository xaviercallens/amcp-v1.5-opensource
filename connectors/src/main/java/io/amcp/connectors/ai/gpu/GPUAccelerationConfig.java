package io.amcp.connectors.ai.gpu;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

/**
 * GPU acceleration configuration for LLM inference.
 * Detects available GPU resources and configures Ollama for optimal performance.
 * 
 * Supports:
 * - NVIDIA CUDA (via nvidia-smi)
 * - AMD ROCm (via rocm-smi)
 * - Apple Metal (via system_profiler)
 * - CPU fallback
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class GPUAccelerationConfig {
    
    public enum GPUType {
        NVIDIA_CUDA,
        AMD_ROCM,
        APPLE_METAL,
        CPU_ONLY,
        UNKNOWN
    }
    
    public enum AccelerationLevel {
        FULL_GPU,      // Full model on GPU
        PARTIAL_GPU,   // Partial offloading
        CPU_ONLY       // No GPU acceleration
    }
    
    private final GPUType detectedGPU;
    private final AccelerationLevel accelerationLevel;
    private final int gpuLayers;
    private final long vramAvailableMB;
    private final Map<String, String> ollamaEnvVars;
    
    public GPUAccelerationConfig() {
        this.detectedGPU = detectGPU();
        this.vramAvailableMB = detectVRAM();
        this.gpuLayers = calculateOptimalGPULayers();
        this.accelerationLevel = determineAccelerationLevel();
        this.ollamaEnvVars = buildOllamaEnvironment();
    }
    
    /**
     * Detect available GPU type
     */
    private GPUType detectGPU() {
        // Check for NVIDIA GPU
        if (isNvidiaGPUAvailable()) {
            return GPUType.NVIDIA_CUDA;
        }
        
        // Check for AMD GPU
        if (isAMDGPUAvailable()) {
            return GPUType.AMD_ROCM;
        }
        
        // Check for Apple Silicon
        if (isAppleMetalAvailable()) {
            return GPUType.APPLE_METAL;
        }
        
        // Check environment variable override
        String gpuType = System.getenv("AMCP_GPU_TYPE");
        if (gpuType != null) {
            try {
                return GPUType.valueOf(gpuType.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid value, continue with detection
            }
        }
        
        return GPUType.CPU_ONLY;
    }
    
    /**
     * Check if NVIDIA GPU is available
     */
    private boolean isNvidiaGPUAvailable() {
        try {
            Process process = Runtime.getRuntime().exec("nvidia-smi");
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if AMD GPU is available
     */
    private boolean isAMDGPUAvailable() {
        try {
            Process process = Runtime.getRuntime().exec("rocm-smi");
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if Apple Metal is available
     */
    private boolean isAppleMetalAvailable() {
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("mac")) {
            return false;
        }
        
        try {
            Process process = Runtime.getRuntime().exec(new String[]{
                "system_profiler", "SPDisplaysDataType"
            });
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Metal")) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Assume Metal is available on macOS
            return true;
        }
        
        return false;
    }
    
    /**
     * Detect available VRAM in MB
     */
    private long detectVRAM() {
        // Check environment variable override
        String vramEnv = System.getenv("AMCP_GPU_VRAM_MB");
        if (vramEnv != null) {
            try {
                return Long.parseLong(vramEnv);
            } catch (NumberFormatException e) {
                // Invalid value, continue with detection
            }
        }
        
        switch (detectedGPU) {
            case NVIDIA_CUDA:
                return detectNvidiaVRAM();
            case AMD_ROCM:
                return detectAMDVRAM();
            case APPLE_METAL:
                return detectAppleVRAM();
            default:
                return 0;
        }
    }
    
    /**
     * Detect NVIDIA VRAM
     */
    private long detectNvidiaVRAM() {
        try {
            Process process = Runtime.getRuntime().exec(
                "nvidia-smi --query-gpu=memory.total --format=csv,noheader,nounits"
            );
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            String line = reader.readLine();
            if (line != null) {
                return Long.parseLong(line.trim());
            }
        } catch (Exception e) {
            System.err.println("Failed to detect NVIDIA VRAM: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * Detect AMD VRAM
     */
    private long detectAMDVRAM() {
        try {
            Process process = Runtime.getRuntime().exec("rocm-smi --showmeminfo vram");
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Total")) {
                    String[] parts = line.split(":");
                    if (parts.length > 1) {
                        String vramStr = parts[1].trim().replaceAll("[^0-9]", "");
                        return Long.parseLong(vramStr);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to detect AMD VRAM: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * Detect Apple Metal VRAM (unified memory)
     */
    private long detectAppleVRAM() {
        try {
            Process process = Runtime.getRuntime().exec("sysctl hw.memsize");
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            String line = reader.readLine();
            if (line != null && line.contains(":")) {
                String[] parts = line.split(":");
                long bytes = Long.parseLong(parts[1].trim());
                // Assume 50% of unified memory available for GPU
                return (bytes / 1024 / 1024) / 2;
            }
        } catch (Exception e) {
            System.err.println("Failed to detect Apple VRAM: " + e.getMessage());
        }
        return 8192; // Default 8GB for Apple Silicon
    }
    
    /**
     * Calculate optimal GPU layers based on VRAM
     */
    private int calculateOptimalGPULayers() {
        if (detectedGPU == GPUType.CPU_ONLY) {
            return 0;
        }
        
        // Check environment variable override
        String layersEnv = System.getenv("AMCP_GPU_LAYERS");
        if (layersEnv != null) {
            try {
                return Integer.parseInt(layersEnv);
            } catch (NumberFormatException e) {
                // Invalid value, continue with calculation
            }
        }
        
        // Estimate layers based on VRAM
        // Rule of thumb: ~100MB per layer for 7B models
        if (vramAvailableMB >= 8000) {
            return -1; // Full GPU offloading
        } else if (vramAvailableMB >= 6000) {
            return 40; // Most layers on GPU
        } else if (vramAvailableMB >= 4000) {
            return 30; // Partial offloading
        } else if (vramAvailableMB >= 2000) {
            return 20; // Minimal offloading
        } else {
            return 0; // CPU only
        }
    }
    
    /**
     * Determine acceleration level
     */
    private AccelerationLevel determineAccelerationLevel() {
        if (gpuLayers == -1 || gpuLayers >= 40) {
            return AccelerationLevel.FULL_GPU;
        } else if (gpuLayers > 0) {
            return AccelerationLevel.PARTIAL_GPU;
        } else {
            return AccelerationLevel.CPU_ONLY;
        }
    }
    
    /**
     * Build Ollama environment variables
     */
    private Map<String, String> buildOllamaEnvironment() {
        Map<String, String> env = new HashMap<>();
        
        // Set GPU layers
        if (gpuLayers != 0) {
            env.put("OLLAMA_NUM_GPU", String.valueOf(gpuLayers));
        }
        
        // Set GPU type specific variables
        switch (detectedGPU) {
            case NVIDIA_CUDA:
                env.put("OLLAMA_GPU_DRIVER", "cuda");
                break;
            case AMD_ROCM:
                env.put("OLLAMA_GPU_DRIVER", "rocm");
                break;
            case APPLE_METAL:
                env.put("OLLAMA_GPU_DRIVER", "metal");
                break;
            default:
                env.put("OLLAMA_GPU_DRIVER", "cpu");
        }
        
        // Set thread count for CPU inference
        int cpuThreads = Runtime.getRuntime().availableProcessors();
        env.put("OLLAMA_NUM_THREAD", String.valueOf(cpuThreads));
        
        return env;
    }
    
    /**
     * Get configuration summary
     */
    public String getConfigSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("GPU Acceleration Configuration:\n");
        summary.append("  GPU Type: ").append(detectedGPU).append("\n");
        summary.append("  VRAM Available: ").append(vramAvailableMB).append(" MB\n");
        summary.append("  GPU Layers: ").append(gpuLayers == -1 ? "ALL" : gpuLayers).append("\n");
        summary.append("  Acceleration Level: ").append(accelerationLevel).append("\n");
        summary.append("  CPU Threads: ").append(Runtime.getRuntime().availableProcessors()).append("\n");
        return summary.toString();
    }
    
    /**
     * Get recommended model based on GPU capabilities
     * PRIORITIZES Qwen2.5:0.5b, Gemma 2B and Qwen2 for optimal performance
     */
    public String getRecommendedModel() {
        if (accelerationLevel == AccelerationLevel.FULL_GPU && vramAvailableMB >= 8000) {
            return "qwen2:7b"; // Large context, excellent multilingual support
        } else if (accelerationLevel == AccelerationLevel.PARTIAL_GPU || vramAvailableMB >= 4000) {
            return "gemma:2b"; // RECOMMENDED: Fast and efficient
        } else if (vramAvailableMB >= 1500) {
            return "gemma:2b"; // Excellent speed/quality ratio
        } else if (vramAvailableMB >= 800) {
            return "qwen2:1.5b"; // Ultra-lightweight for minimal resources
        } else {
            return "qwen2.5:0.5b"; // NEW: Ultra-minimal for very constrained systems
        }
    }
    
    // Getters
    public GPUType getDetectedGPU() { return detectedGPU; }
    public AccelerationLevel getAccelerationLevel() { return accelerationLevel; }
    public int getGpuLayers() { return gpuLayers; }
    public long getVramAvailableMB() { return vramAvailableMB; }
    public Map<String, String> getOllamaEnvVars() { return new HashMap<>(ollamaEnvVars); }
}
