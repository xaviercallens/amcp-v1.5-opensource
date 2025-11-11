package io.amcp.llm.providers;

import io.amcp.llm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Azure OpenAI Connector for AMCP v1.6
 * Supports Azure OpenAI Service deployments
 */
public class AzureOpenAIConnector implements LLMConnector {
    
    private static final Logger logger = LoggerFactory.getLogger(AzureOpenAIConnector.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    
    private final String endpoint;
    private final String apiKey;
    private final String deploymentName;
    private final String apiVersion;
    private final int timeout;
    private final HttpClient httpClient;
    
    public AzureOpenAIConnector(LLMConfig.AzureOpenAIConfig config, int timeout) {
        this.endpoint = config.endpoint().orElse(System.getenv("AZURE_OPENAI_ENDPOINT"));
        this.apiKey = config.apiKey().orElse(System.getenv("AZURE_OPENAI_API_KEY"));
        this.deploymentName = config.deploymentName().orElse(System.getenv("AZURE_OPENAI_DEPLOYMENT"));
        this.apiVersion = config.apiVersion();
        this.timeout = timeout;
        
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeout))
                .build();
        
        if (endpoint == null || endpoint.isEmpty()) {
            logger.warn("Azure OpenAI endpoint not configured. Set AZURE_OPENAI_ENDPOINT environment variable");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("Azure OpenAI API key not configured. Set AZURE_OPENAI_API_KEY environment variable");
        }
        if (deploymentName == null || deploymentName.isEmpty()) {
            logger.warn("Azure OpenAI deployment name not configured. Set AZURE_OPENAI_DEPLOYMENT environment variable");
        }
        
        logger.info("Azure OpenAI connector initialized - Endpoint: {}, Deployment: {}, API Version: {}", 
                    endpoint, deploymentName, apiVersion);
    }
    
    @Override
    public CompletableFuture<LLMResponse> generate(LLMRequest request) {
        long startTime = System.currentTimeMillis();
        
        if (!isConfigured()) {
            return CompletableFuture.completedFuture(createErrorResponse("Azure OpenAI not fully configured"));
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Build Azure OpenAI API request
                Map<String, Object> requestBody = buildRequestBody(request);
                String jsonBody = mapper.writeValueAsString(requestBody);
                
                // Build Azure-specific URL
                String url = String.format("%s/openai/deployments/%s/chat/completions?api-version=%s",
                                          endpoint, deploymentName, apiVersion);
                
                // Create HTTP request with Azure-specific header
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("api-key", apiKey)  // Azure uses 'api-key' header
                        .timeout(Duration.ofSeconds(timeout))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
                
                // Send request
                HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                
                // Parse response
                if (httpResponse.statusCode() == 200) {
                    return parseSuccessResponse(httpResponse.body(), startTime, deploymentName);
                } else {
                    logger.error("Azure OpenAI API error: Status {}, Body: {}", httpResponse.statusCode(), httpResponse.body());
                    return createErrorResponse("Azure OpenAI API error: " + httpResponse.statusCode());
                }
                
            } catch (Exception e) {
                logger.error("Error calling Azure OpenAI API: {}", e.getMessage(), e);
                return createErrorResponse("Azure OpenAI API call failed: " + e.getMessage());
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> isAvailable() {
        if (!isConfigured()) {
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Test with a simple completion request
                String url = String.format("%s/openai/deployments/%s/chat/completions?api-version=%s",
                                          endpoint, deploymentName, apiVersion);
                
                Map<String, Object> testBody = new HashMap<>();
                testBody.put("messages", java.util.List.of(
                    Map.of("role", "user", "content", "test")
                ));
                testBody.put("max_tokens", 1);
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("api-key", apiKey)
                        .timeout(Duration.ofSeconds(5))
                        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(testBody)))
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return response.statusCode() == 200;
                
            } catch (Exception e) {
                logger.warn("Azure OpenAI availability check failed: {}", e.getMessage());
                return false;
            }
        });
    }
    
    @Override
    public LLMProvider getProvider() {
        return LLMProvider.AZURE_OPENAI;
    }
    
    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("provider", "azure-openai");
        metadata.put("endpoint", endpoint);
        metadata.put("deploymentName", deploymentName);
        metadata.put("apiVersion", apiVersion);
        metadata.put("timeout", timeout);
        metadata.put("configured", isConfigured());
        return metadata;
    }
    
    private boolean isConfigured() {
        return endpoint != null && !endpoint.isEmpty() &&
               apiKey != null && !apiKey.isEmpty() &&
               deploymentName != null && !deploymentName.isEmpty();
    }
    
    private Map<String, Object> buildRequestBody(LLMRequest request) {
        Map<String, Object> body = new HashMap<>();
        
        // Messages (Azure OpenAI requires chat format)
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            body.put("messages", request.getMessages());
        } else if (request.getPrompt() != null) {
            var messages = new java.util.ArrayList<Map<String, String>>();
            if (request.getSystemPrompt() != null) {
                messages.add(Map.of("role", "system", "content", request.getSystemPrompt()));
            }
            messages.add(Map.of("role", "user", "content", request.getPrompt()));
            body.put("messages", messages);
        }
        
        // Optional parameters
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }
        if (request.getTopP() != null) {
            body.put("top_p", request.getTopP());
        }
        
        body.put("stream", request.isStream());
        
        return body;
    }
    
    private LLMResponse parseSuccessResponse(String responseBody, long startTime, String deployment) {
        try {
            JsonNode root = mapper.readTree(responseBody);
            
            LLMResponse response = new LLMResponse();
            response.setProvider(LLMProvider.AZURE_OPENAI);
            response.setModel(deployment);
            response.setResponseTimeMs(System.currentTimeMillis() - startTime);
            
            // Extract content
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                JsonNode message = firstChoice.get("message");
                if (message != null) {
                    String content = message.get("content").asText();
                    response.setContent(content);
                }
            }
            
            // Extract token usage
            JsonNode usage = root.get("usage");
            if (usage != null) {
                int totalTokens = usage.get("total_tokens").asInt();
                response.setTokenCount(totalTokens);
                response.addMetadata("prompt_tokens", usage.get("prompt_tokens").asInt());
                response.addMetadata("completion_tokens", usage.get("completion_tokens").asInt());
            }
            
            // Additional metadata
            if (root.has("id")) {
                response.addMetadata("id", root.get("id").asText());
            }
            if (root.has("created")) {
                response.addMetadata("created", root.get("created").asLong());
            }
            response.addMetadata("deployment", deployment);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error parsing Azure OpenAI response: {}", e.getMessage(), e);
            return createErrorResponse("Failed to parse Azure OpenAI response: " + e.getMessage());
        }
    }
    
    private LLMResponse createErrorResponse(String errorMessage) {
        LLMResponse response = new LLMResponse();
        response.setProvider(LLMProvider.AZURE_OPENAI);
        response.setError(errorMessage);
        return response;
    }
}
