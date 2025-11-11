package io.amcp.llm;

import io.amcp.llm.providers.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified LLM Service for AMCP v1.6
 * Manages multiple LLM providers with caching and fallback
 */
@ApplicationScoped
public class LLMService {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMService.class);
    
    @Inject
    LLMConfig config;
    
    private LLMConnector primaryConnector;
    private final Map<LLMProvider, LLMConnector> connectors = new HashMap<>();
    private final LLMCache cache;
    private LLMProvider activeProvider;
    
    public LLMService() {
        this.cache = new LLMCache();
    }
    
    @Inject
    public void initialize(LLMConfig config) {
        this.config = config;
        
        // Initialize connectors based on configuration
        activeProvider = LLMProvider.fromString(config.provider());
        logger.info("Initializing LLM Service with provider: {}", activeProvider);
        
        try {
            // Initialize primary provider
            switch (activeProvider) {
                case OLLAMA:
                    primaryConnector = new OllamaConnector(config.ollama(), config.timeout());
                    connectors.put(LLMProvider.OLLAMA, primaryConnector);
                    break;
                    
                case OPENAI:
                    primaryConnector = new OpenAIConnector(config.openai(), config.timeout());
                    connectors.put(LLMProvider.OPENAI, primaryConnector);
                    break;
                    
                case AZURE_OPENAI:
                    primaryConnector = new AzureOpenAIConnector(config.azureOpenai(), config.timeout());
                    connectors.put(LLMProvider.AZURE_OPENAI, primaryConnector);
                    break;
                    
                case LIGHTLLM:
                    primaryConnector = new LightLLMConnector(config.lightllm(), config.timeout());
                    connectors.put(LLMProvider.LIGHTLLM, primaryConnector);
                    break;
            }
            
            logger.info("LLM Service initialized successfully with {}", activeProvider);
            
        } catch (Exception e) {
            logger.error("Failed to initialize LLM Service: {}", e.getMessage(), e);
            throw new RuntimeException("LLM Service initialization failed", e);
        }
    }
    
    /**
     * Generate response from LLM with caching
     *
     * @param request LLM request
     * @return CompletableFuture with LLM response
     */
    public CompletableFuture<LLMResponse> generate(LLMRequest request) {
        // Check cache if enabled
        if (config.cacheEnabled()) {
            String cacheKey = cache.generateKey(request);
            LLMResponse cached = cache.get(cacheKey);
            if (cached != null) {
                logger.debug("Cache hit for request");
                cached.setCached(true);
                return CompletableFuture.completedFuture(cached);
            }
        }
        
        // Generate from primary provider
        return primaryConnector.generate(request)
                .thenApply(response -> {
                    // Cache successful response
                    if (config.cacheEnabled() && !response.hasError()) {
                        String cacheKey = cache.generateKey(request);
                        cache.put(cacheKey, response, config.cacheTtlHours());
                    }
                    return response;
                })
                .exceptionally(throwable -> {
                    logger.error("LLM generation failed: {}", throwable.getMessage());
                    LLMResponse errorResponse = new LLMResponse();
                    errorResponse.setProvider(activeProvider);
                    errorResponse.setError("Generation failed: " + throwable.getMessage());
                    return errorResponse;
                });
    }
    
    /**
     * Generate response with simple prompt (convenience method)
     *
     * @param prompt The prompt text
     * @return CompletableFuture with LLM response
     */
    public CompletableFuture<LLMResponse> generate(String prompt) {
        return generate(new LLMRequest(prompt));
    }
    
    /**
     * Check if primary provider is available
     *
     * @return CompletableFuture<Boolean>
     */
    public CompletableFuture<Boolean> isAvailable() {
        return primaryConnector.isAvailable();
    }
    
    /**
     * Get current active provider
     *
     * @return LLMProvider enum
     */
    public LLMProvider getActiveProvider() {
        return activeProvider;
    }
    
    /**
     * Get metadata about the LLM service
     *
     * @return metadata map
     */
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("activeProvider", activeProvider.getName());
        metadata.put("cacheEnabled", config.cacheEnabled());
        metadata.put("cacheTtlHours", config.cacheTtlHours());
        metadata.put("maxConcurrentRequests", config.maxConcurrentRequests());
        metadata.put("timeout", config.timeout());
        
        if (primaryConnector != null) {
            metadata.put("providerMetadata", primaryConnector.getMetadata());
        }
        
        // Cache statistics
        metadata.put("cacheStats", cache.getStatistics());
        
        return metadata;
    }
    
    /**
     * Clear the cache
     */
    public void clearCache() {
        cache.clear();
        logger.info("LLM cache cleared");
    }
    
    /**
     * Simple in-memory cache implementation
     */
    private static class LLMCache {
        private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
        private long hits = 0;
        private long misses = 0;
        
        public String generateKey(LLMRequest request) {
            // Simple hash-based key generation
            StringBuilder key = new StringBuilder();
            if (request.getPrompt() != null) {
                key.append(request.getPrompt());
            }
            if (request.getModel() != null) {
                key.append(":").append(request.getModel());
            }
            if (request.getTemperature() != null) {
                key.append(":").append(request.getTemperature());
            }
            return Integer.toString(key.toString().hashCode());
        }
        
        public LLMResponse get(String key) {
            CacheEntry entry = cache.get(key);
            if (entry != null && !entry.isExpired()) {
                hits++;
                return entry.response;
            }
            misses++;
            cache.remove(key);  // Remove expired entry
            return null;
        }
        
        public void put(String key, LLMResponse response, int ttlHours) {
            long expiryTime = System.currentTimeMillis() + (ttlHours * 3600 * 1000L);
            cache.put(key, new CacheEntry(response, expiryTime));
        }
        
        public void clear() {
            cache.clear();
            hits = 0;
            misses = 0;
        }
        
        public Map<String, Object> getStatistics() {
            Map<String, Object> stats = new HashMap<>();
            stats.put("entries", cache.size());
            stats.put("hits", hits);
            stats.put("misses", misses);
            stats.put("hitRate", hits + misses > 0 ? (double) hits / (hits + misses) : 0.0);
            return stats;
        }
        
        private static class CacheEntry {
            final LLMResponse response;
            final long expiryTime;
            
            CacheEntry(LLMResponse response, long expiryTime) {
                this.response = response;
                this.expiryTime = expiryTime;
            }
            
            boolean isExpired() {
                return System.currentTimeMillis() > expiryTime;
            }
        }
    }
}
