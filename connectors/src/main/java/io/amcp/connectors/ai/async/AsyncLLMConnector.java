package io.amcp.connectors.ai.async;

import io.amcp.connectors.ai.cache.LLMResponseCache;
import io.amcp.connectors.ai.fallback.LLMFallbackSystem;
import io.amcp.connectors.ai.gpu.GPUAccelerationConfig;
import io.amcp.tools.ToolRequest;
import io.amcp.tools.ToolResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * Async LLM connector with caching, GPU acceleration, and extended timeouts.
 * 
 * Features:
 * - Asynchronous LLM calls with CompletableFuture
 * - Response caching (memory + disk)
 * - Intelligent fallback system for timeouts
 * - GPU acceleration configuration
 * - Configurable timeouts (default 60s)
 * - Request queuing and rate limiting
 * - Automatic retry with exponential backoff
 * - Rule-based fallback responses
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class AsyncLLMConnector {
    
    private static final int DEFAULT_TIMEOUT_SECONDS = 120; // Extended timeout for complex queries
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final int DEFAULT_MAX_CONCURRENT_REQUESTS = 10;
    private static final int GEMMA_QWEN_TIMEOUT_SECONDS = 90; // Optimized for fast models
    
    private final String ollamaBaseUrl;
    private final HttpClient httpClient;
    private final LLMResponseCache responseCache;
    private final LLMFallbackSystem fallbackSystem;
    private final GPUAccelerationConfig gpuConfig;
    private final ExecutorService executorService;
    private final Semaphore rateLimiter;
    
    private final int timeoutSeconds;
    private final int maxRetries;
    private final boolean cachingEnabled;
    
    // Statistics
    private long totalRequests = 0;
    private long cachedResponses = 0;
    private long failedRequests = 0;
    private long fallbackResponses = 0;
    private long totalLatencyMs = 0;
    
    public AsyncLLMConnector() {
        this(
            System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434"),
            getOptimizedDefaultTimeout(),
            DEFAULT_MAX_RETRIES,
            true
        );
    }
    
    /**
     * Get optimized default timeout based on performance configuration
     */
    private static int getOptimizedDefaultTimeout() {
        // Check if performance mode is enabled
        String performanceMode = System.getenv("AMCP_PERFORMANCE_MODE");
        if ("optimized".equals(performanceMode)) {
            return 90; // Faster timeout for optimized systems
        }
        return DEFAULT_TIMEOUT_SECONDS;
    }
    
    public AsyncLLMConnector(String ollamaBaseUrl, int timeoutSeconds, 
                            int maxRetries, boolean cachingEnabled) {
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.timeoutSeconds = timeoutSeconds;
        this.maxRetries = maxRetries;
        this.cachingEnabled = cachingEnabled;
        
        // Initialize HTTP client with extended timeout
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        
        // Initialize caching
        this.responseCache = cachingEnabled ? new LLMResponseCache() : null;
        
        // Initialize fallback system
        this.fallbackSystem = cachingEnabled ? new LLMFallbackSystem(responseCache) : null;
        
        // Initialize GPU configuration
        this.gpuConfig = new GPUAccelerationConfig();
        
        // Initialize executor service
        this.executorService = Executors.newFixedThreadPool(
            DEFAULT_MAX_CONCURRENT_REQUESTS,
            r -> {
                Thread t = new Thread(r);
                t.setName("AsyncLLM-" + System.nanoTime());
                t.setDaemon(true);
                return t;
            }
        );
        
        // Initialize rate limiter
        this.rateLimiter = new Semaphore(DEFAULT_MAX_CONCURRENT_REQUESTS);
        
        logMessage("AsyncLLMConnector initialized");
        logMessage(gpuConfig.getConfigSummary());
    }
    
    /**
     * Generate LLM response asynchronously
     */
    public CompletableFuture<String> generateAsync(String prompt, String model, 
                                                   Map<String, Object> parameters) {
        totalRequests++;
        
        // Check cache first
        if (cachingEnabled) {
            Optional<String> cachedResponse = responseCache.get(prompt, model, parameters);
            if (cachedResponse.isPresent()) {
                cachedResponses++;
                logMessage("Cache hit for prompt: " + truncate(prompt, 50));
                return CompletableFuture.completedFuture(cachedResponse.get());
            }
        }
        
        // Execute async request with fallback support
        return CompletableFuture.supplyAsync(() -> {
            try {
                rateLimiter.acquire();
                return executeWithRetryAndFallback(prompt, model, parameters);
            } catch (Exception e) {
                failedRequests++;
                throw new CompletionException(e);
            } finally {
                rateLimiter.release();
            }
        }, executorService);
    }
    
    /**
     * Execute request with retry logic and fallback support
     */
    private String executeWithRetryAndFallback(String prompt, String model, Map<String, Object> parameters) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                long startTime = System.currentTimeMillis();
                String response = executeLLMRequest(prompt, model, parameters);
                long latency = System.currentTimeMillis() - startTime;
                
                totalLatencyMs += latency;
                
                // Cache successful response
                if (cachingEnabled && response != null) {
                    responseCache.put(prompt, response, model, parameters);
                    
                    // Learn from successful interaction for fallback system
                    if (fallbackSystem != null) {
                        fallbackSystem.learnFromResponse(prompt, response, model, parameters);
                    }
                }
                
                logMessage(String.format("LLM request completed in %dms (attempt %d/%d)", 
                    latency, attempt, maxRetries));
                
                return response;
                
            } catch (HttpTimeoutException | java.util.concurrent.TimeoutException e) {
                lastException = e;
                logMessage(String.format("LLM request timed out (attempt %d/%d): %s", 
                    attempt, maxRetries, e.getMessage()));
                
                // Try fallback on timeout
                if (fallbackSystem != null && attempt == maxRetries) {
                    Optional<String> fallbackResponse = fallbackSystem.attemptFallback(prompt, model, parameters);
                    if (fallbackResponse.isPresent()) {
                        fallbackResponses++;
                        logMessage("Using fallback response due to timeout");
                        return fallbackResponse.get();
                    }
                }
                
                if (attempt < maxRetries) {
                    // Exponential backoff
                    try {
                        long backoffMs = (long) (Math.pow(2, attempt) * 1000);
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
            } catch (Exception e) {
                lastException = e;
                logMessage(String.format("LLM request failed (attempt %d/%d): %s", 
                    attempt, maxRetries, e.getMessage()));
                
                if (attempt < maxRetries) {
                    // Exponential backoff
                    try {
                        long backoffMs = (long) (Math.pow(2, attempt) * 1000);
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // Final fallback attempt if all retries failed
        if (fallbackSystem != null) {
            Optional<String> fallbackResponse = fallbackSystem.attemptFallback(prompt, model, parameters);
            if (fallbackResponse.isPresent()) {
                fallbackResponses++;
                logMessage("Using fallback response after all retries failed");
                return fallbackResponse.get();
            }
        }
        
        throw new RuntimeException("LLM request failed after " + maxRetries + " attempts and fallback", lastException);
    }
    
    /**
     * Execute request with retry logic (legacy method for backward compatibility)
     * @deprecated Use executeWithRetryAndFallback instead
     */
    @Deprecated
    @SuppressWarnings("unused")
    private String executeWithRetry(String prompt, String model, Map<String, Object> parameters) {
        // This method is kept for backward compatibility but delegates to the new method
        return executeWithRetryAndFallback(prompt, model, parameters);
    }
    
    /**
     * Get optimized timeout for specific model with performance mode awareness
     */
    private int getOptimizedTimeout(String model) {
        // Check for performance mode optimizations
        String performanceMode = System.getenv("AMCP_PERFORMANCE_MODE");
        boolean isOptimized = "optimized".equals(performanceMode);
        
        // Get model-specific environment timeouts if available
        if (model.contains("qwen2.5:0.5b")) {
            String envTimeout = System.getenv("AMCP_QWEN25_TIMEOUT");
            if (envTimeout != null) {
                try {
                    return Integer.parseInt(envTimeout);
                } catch (NumberFormatException e) {
                    // Fall back to default
                }
            }
            return isOptimized ? 45 : 60; // Even faster in optimized mode
        } else if (model.contains("gemma:2b")) {
            String envTimeout = System.getenv("AMCP_GEMMA_TIMEOUT");
            if (envTimeout != null) {
                try {
                    return Integer.parseInt(envTimeout);
                } catch (NumberFormatException e) {
                    // Fall back to default
                }
            }
            return isOptimized ? 70 : GEMMA_QWEN_TIMEOUT_SECONDS;
        } else if (model.contains("qwen2:1.5b")) {
            return isOptimized ? 70 : GEMMA_QWEN_TIMEOUT_SECONDS;
        } else if (model.contains("qwen2")) {
            String envTimeout = System.getenv("AMCP_QWEN2_TIMEOUT");
            if (envTimeout != null) {
                try {
                    return Integer.parseInt(envTimeout);
                } catch (NumberFormatException e) {
                    // Fall back to default
                }
            }
            return isOptimized ? 90 : (GEMMA_QWEN_TIMEOUT_SECONDS + 30);
        }
        
        // Default timeout with performance optimization
        return isOptimized ? (timeoutSeconds - 30) : timeoutSeconds;
    }
    
    /**
     * Execute LLM request
     */
    private String executeLLMRequest(String prompt, String model, Map<String, Object> parameters) 
            throws Exception {
        
        // Build request payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("prompt", prompt);
        payload.put("stream", false);
        
        // Add parameters
        if (parameters.containsKey("temperature")) {
            payload.put("temperature", parameters.get("temperature"));
        }
        if (parameters.containsKey("max_tokens")) {
            payload.put("max_tokens", parameters.get("max_tokens"));
        }
        if (parameters.containsKey("top_p")) {
            payload.put("top_p", parameters.get("top_p"));
        }
        
        // Add GPU configuration
        Map<String, Object> options = new HashMap<>();
        if (gpuConfig.getGpuLayers() != 0) {
            options.put("num_gpu", gpuConfig.getGpuLayers());
        }
        options.put("num_thread", Runtime.getRuntime().availableProcessors());
        payload.put("options", options);
        
        // Build HTTP request with optimized timeout
        String jsonPayload = toJson(payload);
        int optimizedTimeout = getOptimizedTimeout(model);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ollamaBaseUrl + "/api/generate"))
            .timeout(Duration.ofSeconds(optimizedTimeout))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build();
        
        // Send request
        HttpResponse<String> response = httpClient.send(
            request, 
            HttpResponse.BodyHandlers.ofString()
        );
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("LLM request failed with status: " + response.statusCode());
        }
        
        // Parse response
        return parseResponse(response.body());
    }
    
    /**
     * Invoke tool request (compatibility with existing ToolRequest interface)
     */
    public CompletableFuture<ToolResponse> invoke(ToolRequest toolRequest) {
        Map<String, Object> params = toolRequest.getParameters();
        String prompt = (String) params.get("prompt");
        String model = (String) params.getOrDefault("model", gpuConfig.getRecommendedModel());
        long startTime = System.currentTimeMillis();
        
        return generateAsync(prompt, model, params)
            .thenApply(response -> {
                long executionTime = System.currentTimeMillis() - startTime;
                Map<String, Object> result = new HashMap<>();
                result.put("response", response);
                result.put("model", model);
                result.put("cached", false); // TODO: track if from cache
                return ToolResponse.success(result, toolRequest.getRequestId(), executionTime);
            })
            .exceptionally(ex -> {
                long executionTime = System.currentTimeMillis() - startTime;
                return ToolResponse.error(
                    ex.getMessage(),
                    toolRequest.getRequestId(),
                    executionTime
                );
            });
    }
    
    /**
     * Batch generate multiple prompts
     */
    public CompletableFuture<List<String>> generateBatch(List<String> prompts, String model,
                                                         Map<String, Object> parameters) {
        List<CompletableFuture<String>> futures = prompts.stream()
            .map(prompt -> generateAsync(prompt, model, parameters))
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .toList());
    }
    
    /**
     * Simple JSON serialization
     */
    private String toJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) json.append(",");
            first = false;
            
            json.append("\"").append(entry.getKey()).append("\":");
            
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mapValue = (Map<String, Object>) value;
                json.append(toJson(mapValue));
            } else {
                json.append(value);
            }
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Escape JSON string
     */
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    /**
     * Parse LLM response
     */
    private String parseResponse(String jsonResponse) {
        // Simple JSON parsing for "response" field
        int responseStart = jsonResponse.indexOf("\"response\":");
        if (responseStart == -1) {
            throw new RuntimeException("Invalid response format");
        }
        
        int valueStart = jsonResponse.indexOf("\"", responseStart + 11) + 1;
        int valueEnd = jsonResponse.indexOf("\"", valueStart);
        
        while (valueEnd > 0 && jsonResponse.charAt(valueEnd - 1) == '\\') {
            valueEnd = jsonResponse.indexOf("\"", valueEnd + 1);
        }
        
        if (valueEnd == -1) {
            throw new RuntimeException("Invalid response format");
        }
        
        return jsonResponse.substring(valueStart, valueEnd)
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\");
    }
    
    /**
     * Get connector statistics
     */
    public ConnectorStats getStats() {
        double avgLatencyMs = totalRequests > 0 ? 
            (double) totalLatencyMs / totalRequests : 0.0;
        double cacheHitRate = totalRequests > 0 ? 
            (double) cachedResponses / totalRequests : 0.0;
        
        LLMResponseCache.CacheStats cacheStats = cachingEnabled ? 
            responseCache.getStats() : null;
        
        LLMFallbackSystem.FallbackStats fallbackStats = fallbackSystem != null ? 
            fallbackSystem.getStats() : null;
        
        return new ConnectorStats(
            totalRequests,
            cachedResponses,
            failedRequests,
            fallbackResponses,
            avgLatencyMs,
            cacheHitRate,
            cacheStats,
            fallbackStats
        );
    }
    
    /**
     * Cleanup expired cache entries and unused fallback rules
     */
    public void cleanupCache() {
        if (cachingEnabled) {
            responseCache.cleanupExpired();
        }
        if (fallbackSystem != null) {
            fallbackSystem.cleanupRules();
        }
    }
    
    /**
     * Shutdown connector
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logMessage("AsyncLLMConnector shutdown completed");
    }
    
    /**
     * Truncate string for logging
     */
    private String truncate(String str, int maxLength) {
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }
    
    /**
     * Log message
     */
    private void logMessage(String message) {
        String timestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [AsyncLLMConnector] " + message);
    }
    
    /**
     * Connector statistics
     */
    public static class ConnectorStats {
        private final long totalRequests;
        private final long cachedResponses;
        private final long failedRequests;
        private final long fallbackResponses;
        private final double avgLatencyMs;
        private final double cacheHitRate;
        private final LLMResponseCache.CacheStats cacheStats;
        private final LLMFallbackSystem.FallbackStats fallbackStats;
        
        public ConnectorStats(long totalRequests, long cachedResponses, long failedRequests,
                             long fallbackResponses, double avgLatencyMs, double cacheHitRate,
                             LLMResponseCache.CacheStats cacheStats, LLMFallbackSystem.FallbackStats fallbackStats) {
            this.totalRequests = totalRequests;
            this.cachedResponses = cachedResponses;
            this.failedRequests = failedRequests;
            this.fallbackResponses = fallbackResponses;
            this.avgLatencyMs = avgLatencyMs;
            this.cacheHitRate = cacheHitRate;
            this.cacheStats = cacheStats;
            this.fallbackStats = fallbackStats;
        }
        
        public long getTotalRequests() { return totalRequests; }
        public long getCachedResponses() { return cachedResponses; }
        public long getFailedRequests() { return failedRequests; }
        public long getFallbackResponses() { return fallbackResponses; }
        public double getAvgLatencyMs() { return avgLatencyMs; }
        public double getCacheHitRate() { return cacheHitRate; }
        public LLMResponseCache.CacheStats getCacheStats() { return cacheStats; }
        public LLMFallbackSystem.FallbackStats getFallbackStats() { return fallbackStats; }
        
        @Override
        public String toString() {
            return String.format(
                "ConnectorStats{requests=%d, cached=%d, failed=%d, fallback=%d, avgLatency=%.2fms, cacheHitRate=%.2f%%}",
                totalRequests, cachedResponses, failedRequests, fallbackResponses, avgLatencyMs, cacheHitRate * 100
            );
        }
    }
}
