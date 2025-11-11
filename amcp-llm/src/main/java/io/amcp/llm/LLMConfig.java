package io.amcp.llm;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.util.Optional;

/**
 * LLM Configuration for AMCP v1.6
 * Supports multiple LLM providers with unified configuration
 */
@ConfigMapping(prefix = "amcp.llm")
public interface LLMConfig {
    
    /**
     * LLM provider (ollama, openai, azure-openai, lightllm)
     */
    @WithName("provider")
    @WithDefault("ollama")
    String provider();
    
    /**
     * Default model to use
     */
    @WithName("model")
    @WithDefault("gemma2:2b")
    String model();
    
    /**
     * Request timeout in seconds
     */
    @WithName("timeout")
    @WithDefault("120")
    int timeout();
    
    /**
     * Enable caching
     */
    @WithName("cache.enabled")
    @WithDefault("true")
    boolean cacheEnabled();
    
    /**
     * Cache TTL in hours
     */
    @WithName("cache.ttl-hours")
    @WithDefault("24")
    int cacheTtlHours();
    
    /**
     * Maximum concurrent requests
     */
    @WithName("max-concurrent-requests")
    @WithDefault("10")
    int maxConcurrentRequests();
    
    /**
     * Ollama configuration
     */
    OllamaConfig ollama();
    
    /**
     * OpenAI configuration
     */
    OpenAIConfig openai();
    
    /**
     * Azure OpenAI configuration
     */
    AzureOpenAIConfig azureOpenai();
    
    /**
     * LightLLM configuration
     */
    LightLLMConfig lightllm();
    
    /**
     * Ollama-specific configuration
     */
    interface OllamaConfig {
        @WithName("endpoint")
        @WithDefault("http://localhost:11434")
        String endpoint();
        
        @WithName("model")
        @WithDefault("gemma2:2b")
        String model();
    }
    
    /**
     * OpenAI-specific configuration
     */
    interface OpenAIConfig {
        @WithName("endpoint")
        @WithDefault("https://api.openai.com/v1")
        String endpoint();
        
        @WithName("api-key")
        Optional<String> apiKey();
        
        @WithName("model")
        @WithDefault("gpt-3.5-turbo")
        String model();
        
        @WithName("temperature")
        @WithDefault("0.7")
        double temperature();
        
        @WithName("max-tokens")
        @WithDefault("1000")
        int maxTokens();
    }
    
    /**
     * Azure OpenAI-specific configuration
     */
    interface AzureOpenAIConfig {
        @WithName("endpoint")
        Optional<String> endpoint();
        
        @WithName("api-key")
        Optional<String> apiKey();
        
        @WithName("deployment-name")
        Optional<String> deploymentName();
        
        @WithName("api-version")
        @WithDefault("2024-02-01")
        String apiVersion();
        
        @WithName("temperature")
        @WithDefault("0.7")
        double temperature();
        
        @WithName("max-tokens")
        @WithDefault("1000")
        int maxTokens();
    }
    
    /**
     * LightLLM-specific configuration
     */
    interface LightLLMConfig {
        @WithName("endpoint")
        @WithDefault("http://localhost:8080")
        String endpoint();
        
        @WithName("model")
        Optional<String> model();
        
        @WithName("temperature")
        @WithDefault("0.7")
        double temperature();
        
        @WithName("max-new-tokens")
        @WithDefault("1000")
        int maxNewTokens();
        
        @WithName("top-p")
        @WithDefault("0.9")
        double topP();
        
        @WithName("top-k")
        @WithDefault("50")
        int topK();
    }
}
