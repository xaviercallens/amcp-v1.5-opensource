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
 * LightLLM Connector for AMCP v1.6
 * High-performance LLM inference server
 * https://github.com/ModelTC/LightLLM
 * 
 * Features:
 * - Continuous batching
 * - Token attention caching
 * - Multi-GPU support
 * - High throughput optimization
 */
public class LightLLMConnector implements LLMConnector {
    
    private static final Logger logger = LoggerFactory.getLogger(LightLLMConnector.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    
    private final String endpoint;
    private final String model;
    private final double temperature;
    private final int maxNewTokens;
    private final double topP;
    private final int topK;
    private final int timeout;
    private final HttpClient httpClient;
    
    public LightLLMConnector(LLMConfig.LightLLMConfig config, int timeout) {
        this.endpoint = config.endpoint();
        this.model = config.model().orElse(null);
        this.temperature = config.temperature();
        this.maxNewTokens = config.maxNewTokens();
        this.topP = config.topP();
        this.topK = config.topK();
        this.timeout = timeout;
        
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeout))
                .build();
        
        logger.info("LightLLM connector initialized - Endpoint: {}, Model: {}", endpoint, model);
    }
    
    @Override
    public CompletableFuture<LLMResponse> generate(LLMRequest request) {
        long startTime = System.currentTimeMillis();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Build LightLLM API request
                Map<String, Object> requestBody = buildRequestBody(request);
                String jsonBody = mapper.writeValueAsString(requestBody);
                
                // Create HTTP request
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint + "/generate"))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(timeout))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
                
                // Send request
                HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                
                // Parse response
                if (httpResponse.statusCode() == 200) {
                    return parseSuccessResponse(httpResponse.body(), startTime, model);
                } else {
                    logger.error("LightLLM API error: Status {}, Body: {}", httpResponse.statusCode(), httpResponse.body());
                    return createErrorResponse("LightLLM API error: " + httpResponse.statusCode());
                }
                
            } catch (Exception e) {
                logger.error("Error calling LightLLM API: {}", e.getMessage(), e);
                return createErrorResponse("LightLLM API call failed: " + e.getMessage());
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> isAvailable() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint + "/health"))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return response.statusCode() == 200;
                
            } catch (Exception e) {
                logger.warn("LightLLM availability check failed: {}", e.getMessage());
                return false;
            }
        });
    }
    
    @Override
    public LLMProvider getProvider() {
        return LLMProvider.LIGHTLLM;
    }
    
    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("provider", "lightllm");
        metadata.put("endpoint", endpoint);
        metadata.put("model", model);
        metadata.put("temperature", temperature);
        metadata.put("maxNewTokens", maxNewTokens);
        metadata.put("topP", topP);
        metadata.put("topK", topK);
        metadata.put("timeout", timeout);
        return metadata;
    }
    
    private Map<String, Object> buildRequestBody(LLMRequest request) {
        Map<String, Object> body = new HashMap<>();
        
        // Prompt (LightLLM expects text prompt)
        if (request.getPrompt() != null) {
            body.put("prompt", request.getPrompt());
        } else if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            // Convert messages to prompt
            StringBuilder prompt = new StringBuilder();
            if (request.getSystemPrompt() != null) {
                prompt.append("System: ").append(request.getSystemPrompt()).append("\n\n");
            }
            for (LLMRequest.Message msg : request.getMessages()) {
                prompt.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
            }
            body.put("prompt", prompt.toString());
        }
        
        // Model (optional for LightLLM if server has single model)
        if (model != null) {
            body.put("model", model);
        }
        
        // Generation parameters
        body.put("temperature", request.getTemperature() != null ? request.getTemperature() : temperature);
        body.put("max_new_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : maxNewTokens);
        body.put("top_p", request.getTopP() != null ? request.getTopP() : topP);
        body.put("top_k", request.getTopK() != null ? request.getTopK() : topK);
        
        // LightLLM specific parameters
        body.put("do_sample", true);
        body.put("repetition_penalty", 1.0);
        
        return body;
    }
    
    private LLMResponse parseSuccessResponse(String responseBody, long startTime, String modelName) {
        try {
            JsonNode root = mapper.readTree(responseBody);
            
            LLMResponse response = new LLMResponse();
            response.setProvider(LLMProvider.LIGHTLLM);
            response.setModel(modelName);
            response.setResponseTimeMs(System.currentTimeMillis() - startTime);
            
            // Extract generated text
            if (root.has("generated_text")) {
                response.setContent(root.get("generated_text").asText());
            } else if (root.has("text")) {
                response.setContent(root.get("text").asText());
            } else if (root.has("output")) {
                response.setContent(root.get("output").asText());
            }
            
            // Extract token count if available
            if (root.has("token_count")) {
                response.setTokenCount(root.get("token_count").asInt());
            } else if (root.has("tokens_used")) {
                response.setTokenCount(root.get("tokens_used").asInt());
            }
            
            // Additional metadata
            if (root.has("finish_reason")) {
                response.addMetadata("finish_reason", root.get("finish_reason").asText());
            }
            if (root.has("generation_time")) {
                response.addMetadata("generation_time_ms", root.get("generation_time").asDouble());
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error parsing LightLLM response: {}", e.getMessage(), e);
            return createErrorResponse("Failed to parse LightLLM response: " + e.getMessage());
        }
    }
    
    private LLMResponse createErrorResponse(String errorMessage) {
        LLMResponse response = new LLMResponse();
        response.setProvider(LLMProvider.LIGHTLLM);
        response.setError(errorMessage);
        return response;
    }
}
