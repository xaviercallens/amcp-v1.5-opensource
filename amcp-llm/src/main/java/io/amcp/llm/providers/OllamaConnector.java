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
 * Ollama Connector for AMCP v1.6
 * Local LLM inference with Ollama
 * Supports: Llama, Mistral, Gemma, Qwen, and many more models
 */
public class OllamaConnector implements LLMConnector {
    
    private static final Logger logger = LoggerFactory.getLogger(OllamaConnector.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    
    private final String endpoint;
    private final String defaultModel;
    private final int timeout;
    private final HttpClient httpClient;
    
    public OllamaConnector(LLMConfig.OllamaConfig config, int timeout) {
        this.endpoint = config.endpoint();
        this.defaultModel = config.model();
        this.timeout = timeout;
        
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeout))
                .build();
        
        logger.info("Ollama connector initialized - Endpoint: {}, Model: {}", endpoint, defaultModel);
    }
    
    @Override
    public CompletableFuture<LLMResponse> generate(LLMRequest request) {
        long startTime = System.currentTimeMillis();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Build Ollama API request
                Map<String, Object> requestBody = buildRequestBody(request);
                String jsonBody = mapper.writeValueAsString(requestBody);
                
                // Create HTTP request
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint + "/api/generate"))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(timeout))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
                
                // Send request
                HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                
                // Parse response
                if (httpResponse.statusCode() == 200) {
                    return parseSuccessResponse(httpResponse.body(), startTime, request.getModel() != null ? request.getModel() : defaultModel);
                } else {
                    logger.error("Ollama API error: Status {}, Body: {}", httpResponse.statusCode(), httpResponse.body());
                    return createErrorResponse("Ollama API error: " + httpResponse.statusCode());
                }
                
            } catch (Exception e) {
                logger.error("Error calling Ollama API: {}", e.getMessage(), e);
                return createErrorResponse("Ollama API call failed: " + e.getMessage());
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> isAvailable() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint + "/api/tags"))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return response.statusCode() == 200;
                
            } catch (Exception e) {
                logger.warn("Ollama availability check failed: {}", e.getMessage());
                return false;
            }
        });
    }
    
    @Override
    public LLMProvider getProvider() {
        return LLMProvider.OLLAMA;
    }
    
    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("provider", "ollama");
        metadata.put("endpoint", endpoint);
        metadata.put("model", defaultModel);
        metadata.put("timeout", timeout);
        return metadata;
    }
    
    private Map<String, Object> buildRequestBody(LLMRequest request) {
        Map<String, Object> body = new HashMap<>();
        
        // Model
        body.put("model", request.getModel() != null ? request.getModel() : defaultModel);
        
        // Prompt
        if (request.getPrompt() != null) {
            body.put("prompt", request.getPrompt());
        } else if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            // Ollama can use messages format for chat models
            // For now, convert to prompt
            StringBuilder prompt = new StringBuilder();
            if (request.getSystemPrompt() != null) {
                prompt.append("System: ").append(request.getSystemPrompt()).append("\n\n");
            }
            for (LLMRequest.Message msg : request.getMessages()) {
                prompt.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
            }
            body.put("prompt", prompt.toString());
        }
        
        // Options
        Map<String, Object> options = new HashMap<>();
        if (request.getTemperature() != null) {
            options.put("temperature", request.getTemperature());
        }
        if (request.getTopP() != null) {
            options.put("top_p", request.getTopP());
        }
        if (request.getTopK() != null) {
            options.put("top_k", request.getTopK());
        }
        if (request.getMaxTokens() != null) {
            options.put("num_predict", request.getMaxTokens());
        }
        
        if (!options.isEmpty()) {
            body.put("options", options);
        }
        
        // Disable streaming for simplicity
        body.put("stream", false);
        
        return body;
    }
    
    private LLMResponse parseSuccessResponse(String responseBody, long startTime, String model) {
        try {
            JsonNode root = mapper.readTree(responseBody);
            
            LLMResponse response = new LLMResponse();
            response.setProvider(LLMProvider.OLLAMA);
            response.setModel(model);
            response.setResponseTimeMs(System.currentTimeMillis() - startTime);
            
            // Extract generated text
            if (root.has("response")) {
                response.setContent(root.get("response").asText());
            }
            
            // Extract context and metadata
            if (root.has("context")) {
                JsonNode context = root.get("context");
                if (context.isArray()) {
                    response.setTokenCount(context.size());
                }
            }
            
            // Additional Ollama metadata
            if (root.has("total_duration")) {
                response.addMetadata("total_duration_ns", root.get("total_duration").asLong());
            }
            if (root.has("load_duration")) {
                response.addMetadata("load_duration_ns", root.get("load_duration").asLong());
            }
            if (root.has("prompt_eval_count")) {
                response.addMetadata("prompt_tokens", root.get("prompt_eval_count").asInt());
            }
            if (root.has("eval_count")) {
                response.addMetadata("completion_tokens", root.get("eval_count").asInt());
                response.setTokenCount(root.get("eval_count").asInt());
            }
            if (root.has("eval_duration")) {
                response.addMetadata("eval_duration_ns", root.get("eval_duration").asLong());
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error parsing Ollama response: {}", e.getMessage(), e);
            return createErrorResponse("Failed to parse Ollama response: " + e.getMessage());
        }
    }
    
    private LLMResponse createErrorResponse(String errorMessage) {
        LLMResponse response = new LLMResponse();
        response.setProvider(LLMProvider.OLLAMA);
        response.setError(errorMessage);
        return response;
    }
}
