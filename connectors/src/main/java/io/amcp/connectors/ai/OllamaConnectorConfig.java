package io.amcp.connectors.ai;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

/**
 * Configuration class for OLLAMA integration with AMCP v1.5.
 * This configuration provides connection settings and model management for local OLLAMA deployments.
 * 
 * Features:
 * - Configurable base URL for OLLAMA server
 * - Multiple model support with automatic fallback
 * - Connection pooling and timeout management
 * - Health monitoring and auto-recovery
 * 
 * @author Xavier Callens
 * @version 1.5.0
 * @since 2024-12-19
 */
public class OllamaConnectorConfig {

    private final String ollamaBaseUrl;
    private final String defaultModel;
    private final int timeoutSeconds;
    private final int maxRetries;
    private final int healthCheckIntervalSeconds;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final HttpClient httpClient;
    private volatile boolean isHealthy = true;

    /**
     * Creates OLLAMA connector config with default settings.
     */
    public OllamaConnectorConfig() {
        this("http://localhost:11434", "llama3.2", 30, 3, 60);
    }

    /**
     * Creates OLLAMA connector config with custom settings.
     * 
     * @param ollamaBaseUrl base URL for OLLAMA server
     * @param defaultModel default model name
     * @param timeoutSeconds request timeout
     * @param maxRetries maximum retry attempts
     * @param healthCheckIntervalSeconds health check interval
     */
    public OllamaConnectorConfig(String ollamaBaseUrl, String defaultModel, 
                               int timeoutSeconds, int maxRetries, int healthCheckIntervalSeconds) {
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.defaultModel = defaultModel;
        this.timeoutSeconds = timeoutSeconds;
        this.maxRetries = maxRetries;
        this.healthCheckIntervalSeconds = healthCheckIntervalSeconds;
        
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
            
        // Start health monitoring
        startHealthMonitoring();
    }

    /**
     * Gets the configured OLLAMA base URL.
     * 
     * @return base URL string
     */
    public String getOllamaBaseUrl() {
        return ollamaBaseUrl;
    }

    /**
     * Gets the default model name.
     * 
     * @return model name string
     */
    public String getDefaultModel() {
        return defaultModel;
    }

    /**
     * Gets the configured timeout in seconds.
     * 
     * @return timeout value
     */
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * Gets the maximum retry attempts.
     * 
     * @return max retries value
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Gets the HTTP client instance.
     * 
     * @return HttpClient instance
     */
    public HttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Checks if the OLLAMA service is currently healthy.
     * 
     * @return true if healthy, false otherwise
     */
    public boolean isHealthy() {
        return isHealthy;
    }

    /**
     * Performs an asynchronous health check of the OLLAMA service.
     * 
     * @return CompletableFuture with health status
     */
    public CompletableFuture<Boolean> checkHealthAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaBaseUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
                    
                HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
                return response.statusCode() == 200;
            } catch (Exception e) {
                logMessage("OLLAMA health check failed: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Starts periodic health monitoring of the OLLAMA service.
     */
    private void startHealthMonitoring() {
        scheduler.scheduleWithFixedDelay(() -> {
            checkHealthAsync().thenAccept(healthy -> {
                if (healthy != isHealthy) {
                    isHealthy = healthy;
                    logMessage("OLLAMA health status changed: " + (healthy ? "HEALTHY" : "UNHEALTHY"));
                }
            });
        }, 0, healthCheckIntervalSeconds, java.util.concurrent.TimeUnit.SECONDS);
    }

    /**
     * Logs a message with timestamp (following AMCP logging pattern).
     * 
     * @param message the message to log
     */
    private void logMessage(String message) {
        String timestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [OllamaConnectorConfig] " + message);
    }

    /**
     * Shutdown hook to clean up resources.
     */
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            logMessage("OLLAMA connector configuration shutdown completed");
        }
    }
}