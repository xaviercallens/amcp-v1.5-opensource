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
 * OpenAI Connector for AMCP v1.6
 * Supports GPT-4, GPT-3.5-turbo, GPT-4-turbo
 */
public class OpenAIConnector implements LLMConnector {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAIConnector.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    
    private final String endpoint;
    private final String apiKey;
    private final String defaultModel;
    private final int timeout;
    private final HttpClient httpClient;
    
    public OpenAIConnector(LLMConfig.OpenAIConfig config, int timeout) {
        this.endpoint = config.endpoint();
        this.apiKey = config.apiKey().orElse(System.getenv("OPENAI_API_KEY"));
        this.defaultModel = config.model();
        this.timeout = timeout;
        
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeout))
                .build();
        
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("OpenAI API key not configured. Set OPENAI_API_KEY environment variable or amcp.llm.openai.api-key property");
        }
        
        logger.info("OpenAI connector initialized - Endpoint: {}, Model: {}", endpoint, defaultModel);
    }
    
    @Override
    public CompletableFuture<LLMResponse> generate(LLMRequest request) {
        long startTime = System.currentTimeMillis();
        
        if (apiKey == null || apiKey.isEmpty()) {
            return CompletableFuture.completedFuture(createErrorResponse("OpenAI API key not configured"));
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Build OpenAI API request
                Map<String, Object> requestBody = buildRequestBody(request);
                String jsonBody = mapper.writeValueAsString(requestBody);
                
                // Create HTTP request
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint + "/chat/completions"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .timeout(Duration.ofSeconds(timeout))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
                
                // Send request
                HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                
                // Parse response
                if (httpResponse.statusCode() == 200) {
                    return parseSuccessResponse(httpResponse.body(), startTime, request.getModel() != null ? request.getModel() : defaultModel);
                } else {
                    logger.error("OpenAI API error: Status {}, Body: {}", httpResponse.statusCode(), httpResponse.body());
                    return createErrorResponse("OpenAI API error: " + httpResponse.statusCode());
                }
                
            } catch (Exception e) {
                logger.error("Error calling OpenAI API: {}", e.getMessage(), e);
                return createErrorResponse("OpenAI API call failed: " + e.getMessage());
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> isAvailable() {
        if (apiKey == null || apiKey.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint + "/models"))
                        .header("Authorization", "Bearer " + apiKey)
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return response.statusCode() == 200;
                
            } catch (Exception e) {
                logger.warn("OpenAI availability check failed: {}", e.getMessage());
                return false;
            }
        });
    }
    
    @Override
    public LLMProvider getProvider() {
        return LLMProvider.OPENAI;
    }
    
    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("provider", "openai");
        metadata.put("endpoint", endpoint);
        metadata.put("model", defaultModel);
        metadata.put("timeout", timeout);
        metadata.put("configured", apiKey != null && !apiKey.isEmpty());
        return metadata;
    }
    
    private Map<String, Object> buildRequestBody(LLMRequest request) {
        Map<String, Object> body = new HashMap<>();
        
        // Model
        body.put("model", request.getModel() != null ? request.getModel() : defaultModel);
        
        // Messages
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            body.put("messages", request.getMessages());
        } else if (request.getPrompt() != null) {
            // Convert prompt to messages format
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
    
    private LLMResponse parseSuccessResponse(String responseBody, long startTime, String model) {
        try {
            JsonNode root = mapper.readTree(responseBody);
            
            LLMResponse response = new LLMResponse();
            response.setProvider(LLMProvider.OPENAI);
            response.setModel(model);
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
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error parsing OpenAI response: {}", e.getMessage(), e);
            return createErrorResponse("Failed to parse OpenAI response: " + e.getMessage());
        }
    }
    
    private LLMResponse createErrorResponse(String errorMessage) {
        LLMResponse response = new LLMResponse();
        response.setProvider(LLMProvider.OPENAI);
        response.setError(errorMessage);
        return response;
    }
}
