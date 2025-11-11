package io.amcp.llm;

import java.util.concurrent.CompletableFuture;

/**
 * LLM Connector interface for AMCP v1.6
 * Unified interface for all LLM providers
 */
public interface LLMConnector {
    
    /**
     * Send a prompt to the LLM and get response
     * 
     * @param request LLM request
     * @return CompletableFuture with LLM response
     */
    CompletableFuture<LLMResponse> generate(LLMRequest request);
    
    /**
     * Send a simple prompt (convenience method)
     * 
     * @param prompt The prompt text
     * @return CompletableFuture with LLM response
     */
    default CompletableFuture<LLMResponse> generate(String prompt) {
        return generate(new LLMRequest(prompt));
    }
    
    /**
     * Check if the provider is available
     * 
     * @return true if provider is reachable
     */
    CompletableFuture<Boolean> isAvailable();
    
    /**
     * Get the provider type
     * 
     * @return LLM provider enum
     */
    LLMProvider getProvider();
    
    /**
     * Get provider-specific metadata
     * 
     * @return metadata map
     */
    java.util.Map<String, Object> getMetadata();
}
