package io.amcp.llm;

/**
 * LLM Provider enumeration for AMCP v1.6
 * Supports multiple LLM backends
 */
public enum LLMProvider {
    /**
     * Ollama - Local LLM inference
     * Supports: Llama, Mistral, Gemma, Qwen, etc.
     */
    OLLAMA("ollama", "http://localhost:11434"),
    
    /**
     * OpenAI - Official OpenAI API
     * Models: GPT-4, GPT-3.5-turbo, GPT-4-turbo
     */
    OPENAI("openai", "https://api.openai.com/v1"),
    
    /**
     * Azure OpenAI - Microsoft Azure OpenAI Service
     * Requires: Azure subscription and deployment
     */
    AZURE_OPENAI("azure-openai", "https://{resource}.openai.azure.com"),
    
    /**
     * LightLLM - High-performance LLM inference server
     * https://github.com/ModelTC/LightLLM
     * Optimized for throughput and low latency
     */
    LIGHTLLM("lightllm", "http://localhost:8080");
    
    private final String name;
    private final String defaultEndpoint;
    
    LLMProvider(String name, String defaultEndpoint) {
        this.name = name;
        this.defaultEndpoint = defaultEndpoint;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDefaultEndpoint() {
        return defaultEndpoint;
    }
    
    public static LLMProvider fromString(String provider) {
        if (provider == null) {
            return OLLAMA; // Default
        }
        
        for (LLMProvider p : values()) {
            if (p.name.equalsIgnoreCase(provider)) {
                return p;
            }
        }
        
        throw new IllegalArgumentException("Unknown LLM provider: " + provider);
    }
}
