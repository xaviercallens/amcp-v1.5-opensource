package io.amcp.connectors.ai.ollama;

import io.amcp.core.auth.AuthenticationContext;
import io.amcp.core.connector.AbstractToolConnector;
import io.amcp.core.connector.ToolRequest;
import io.amcp.core.connector.ToolResponse;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AMCP v1.5 Connector for integrating OLLAMA models via Spring AI.
 * This connector allows agents to send prompts to locally running OLLAMA models
 * and receive generated responses asynchronously.
 */
@Component
public class OllamaSpringAIConnector extends AbstractToolConnector {

    private final ChatClient chatClient;
    private final OllamaConnectorConfig config;
    private final String defaultModel;
    
    @Autowired
    public OllamaSpringAIConnector(OllamaConnectorConfig config) {
        this.config = config;
        this.defaultModel = config.getDefaultModel();
        
        // Initialize Spring AI client with OLLAMA API
        OllamaApi ollamaApi = new OllamaApi(config.getBaseUrl());
        this.chatClient = new OllamaChatClient(ollamaApi);
        
        String timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [OllamaSpringAIConnector] Initialized with base URL: " 
            + config.getBaseUrl() + " and default model: " + defaultModel);
    }

    @Override
    public CompletableFuture<ToolResponse> invoke(ToolRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Extract parameters
                String prompt = (String) request.getParams().get("prompt");
                String model = request.getParams().containsKey("model") ? 
                    (String) request.getParams().get("model") : defaultModel;
                
                // Configure additional parameters
                Map<String, Object> options = new HashMap<>();
                if (request.getParams().containsKey("temperature")) {
                    options.put("temperature", request.getParams().get("temperature"));
                }
                if (request.getParams().containsKey("maxTokens")) {
                    options.put("max_tokens", request.getParams().get("maxTokens"));
                }
                
                logMessage("Sending prompt to OLLAMA model: " + model);
                
                // Create prompt and send to OLLAMA via Spring AI
                Prompt aiPrompt = new Prompt(prompt, options);
                ChatResponse response = chatClient.call(aiPrompt);
                
                // Process response
                Generation generation = response.getResult().getOutput();
                String generatedText = generation.getContent().toString();
                
                // Construct tool response
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("text", generatedText);
                resultMap.put("model", model);
                
                logMessage("Received response from OLLAMA model: " + model);
                
                return ToolResponse.builder()
                    .success(true)
                    .data(resultMap)
                    .build();
                
            } catch (Exception e) {
                logMessage("Error calling OLLAMA model: " + e.getMessage());
                return ToolResponse.builder()
                    .success(false)
                    .error("Error calling OLLAMA: " + e.getMessage())
                    .build();
            }
        });
    }
    
    @Override
    public Map<String, Object> getRequestSchema() {
        Map<String, Object> schema = new HashMap<>();
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> prompt = new HashMap<>();
        prompt.put("type", "string");
        prompt.put("description", "The prompt text to send to the OLLAMA model");
        properties.put("prompt", prompt);
        
        Map<String, Object> model = new HashMap<>();
        model.put("type", "string");
        model.put("description", "The OLLAMA model to use (e.g., llama2, mistral, etc.)");
        properties.put("model", model);
        
        Map<String, Object> temperature = new HashMap<>();
        temperature.put("type", "number");
        temperature.put("description", "Temperature for generation (0.0-1.0)");
        properties.put("temperature", temperature);
        
        Map<String, Object> maxTokens = new HashMap<>();
        maxTokens.put("type", "integer");
        maxTokens.put("description", "Maximum number of tokens to generate");
        properties.put("maxTokens", maxTokens);
        
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", java.util.Arrays.asList("prompt"));
        
        return schema;
    }
    
    private void logMessage(String message) {
        String timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [OllamaSpringAIConnector] " + message);
    }
}