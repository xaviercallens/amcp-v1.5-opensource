package io.amcp.connectors.ai;

import io.amcp.tools.ToolConnector;
import io.amcp.tools.ToolRequest;
import io.amcp.tools.ToolResponse;
import io.amcp.core.Event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * OLLAMA AI connector for AMCP v1.5 Enterprise Edition.
 * This connector enables AMCP agents to interact with local OLLAMA models for AI-powered conversations.
 * 
 * Features:
 * - Asynchronous prompt processing
 * - Configurable model selection
 * - Error handling and retry logic
 * - Integration with AMCP event system
 * - Support for conversation context
 * 
 * Example usage in an agent:
 * <pre>
 * {@code
 * ToolResponse response = callMCPTool("ollama-chat", Map.of(
 *     "prompt", "Explain quantum computing in simple terms",
 *     "model", "llama3.2",
 *     "conversationId", "user-123"
 * ), authContext);
 * }
 * </pre>
 * 
 * @author Xavier Callens
 * @version 1.5.0
 * @since 2024-12-19
 */
public class OllamaSpringAIConnector implements ToolConnector {

    private static final String TOOL_ID = "ollama-ai-chat";
    private static final String TOOL_NAME = "ollama-chat";
    private static final String VERSION = "1.5.0";

    private final OllamaConnectorConfig config;
    private final ObjectMapper objectMapper;

    /**
     * Constructs an OLLAMA connector with default configuration.
     */
    public OllamaSpringAIConnector() {
        this(new OllamaConnectorConfig());
    }

    /**
     * Constructs an OLLAMA connector with custom configuration.
     * 
     * @param config the OLLAMA configuration
     */
    public OllamaSpringAIConnector(OllamaConnectorConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Legacy constructor for backwards compatibility.
     * 
     * @param baseUrl the OLLAMA server base URL
     * @param defaultModel the default model to use
     * @param timeoutSeconds the request timeout in seconds
     */
    public OllamaSpringAIConnector(String baseUrl, String defaultModel, int timeoutSeconds) {
        this(new OllamaConnectorConfig(baseUrl, defaultModel, timeoutSeconds, 3, 60));
    }

    @Override
    public String getToolId() {
        return TOOL_ID;
    }

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public CompletableFuture<Boolean> isHealthy() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getOllamaBaseUrl() + "/api/tags"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
                    
                HttpResponse<String> response = config.getHttpClient().send(request, 
                    HttpResponse.BodyHandlers.ofString());
                return response.statusCode() == 200;
            } catch (Exception e) {
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Void> initialize(Map<String, Object> configParams) {
        return CompletableFuture.runAsync(() -> {
            logMessage("OLLAMA connector initialized with base URL: " + this.config.getOllamaBaseUrl());
        });
    }

    @Override
    public CompletableFuture<ToolResponse> invoke(ToolRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            Map<String, Object> params = request.getParameters();
            
            // Validate required parameters
            String prompt = getRequiredParam(params, "prompt");
            String model = (String) params.getOrDefault("model", config.getDefaultModel());
            String conversationId = (String) params.getOrDefault("conversationId", "default");
            boolean stream = Boolean.parseBoolean((String) params.getOrDefault("stream", "false"));

            logMessage("Processing OLLAMA request for model: " + model + ", conversation: " + conversationId);

            return executeOllamaRequest(prompt, model, conversationId, stream)
                .handle((result, throwable) -> {
                    long executionTime = System.currentTimeMillis() - startTime;
                    if (throwable != null) {
                        logMessage("OLLAMA request failed: " + throwable.getMessage());
                        return ToolResponse.error("OLLAMA_ERROR: " + throwable.getMessage(), 
                            request.getRequestId(), executionTime);
                    }
                    return ToolResponse.success(result, request.getRequestId(), executionTime);
                });

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logMessage("Invalid OLLAMA request parameters: " + e.getMessage());
            return CompletableFuture.completedFuture(
                ToolResponse.error("INVALID_PARAMS: " + e.getMessage(), 
                    request.getRequestId(), executionTime)
            );
        }
    }

    @Override
    public Map<String, Object> getRequestSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "prompt", Map.of(
                    "type", "string",
                    "description", "The prompt/question to send to the AI model",
                    "minLength", 1,
                    "maxLength", 4000
                ),
                "model", Map.of(
                    "type", "string",
                    "description", "The OLLAMA model to use (e.g., 'llama3.2', 'mistral', 'codellama')",
                    "default", config.getDefaultModel()
                ),
                "conversationId", Map.of(
                    "type", "string",
                    "description", "Unique identifier for conversation context",
                    "default", "default"
                ),
                "stream", Map.of(
                    "type", "boolean",
                    "description", "Whether to stream the response (not yet implemented)",
                    "default", false
                )
            ),
            "required", new String[]{"prompt"}
        );
    }

    @Override
    public String[] getSupportedOperations() {
        return new String[]{"chat", "generate", "complete"};
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            config.shutdown();
            logMessage("OLLAMA connector shutdown completed");
        });
    }

    /**
     * Executes the OLLAMA API request asynchronously.
     * 
     * @param prompt the user prompt
     * @param model the model to use
     * @param conversationId the conversation identifier
     * @param stream whether to stream the response
     * @return CompletableFuture with the AI response
     */
    private CompletableFuture<Map<String, Object>> executeOllamaRequest(
            String prompt, String model, String conversationId, boolean stream) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Build the request payload
                Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "prompt", prompt,
                    "stream", stream,
                    "options", Map.of(
                        "temperature", 0.7,
                        "top_k", 40,
                        "top_p", 0.9,
                        "repeat_penalty", 1.1
                    )
                );

                String jsonBody = objectMapper.writeValueAsString(requestBody);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getOllamaBaseUrl() + "/api/generate"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

                HttpResponse<String> response = config.getHttpClient().send(request, 
                    HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new RuntimeException("OLLAMA API returned status: " + response.statusCode() + 
                        ", body: " + response.body());
                }

                // Parse the response
                JsonNode responseJson = objectMapper.readTree(response.body());
                String aiResponse = responseJson.get("response").asText();
                
                logMessage("OLLAMA response received (" + aiResponse.length() + " chars) for conversation: " + conversationId);

                return Map.of(
                    "response", aiResponse,
                    "model", model,
                    "conversationId", conversationId,
                    "timestamp", System.currentTimeMillis(),
                    "done", responseJson.get("done").asBoolean()
                );

            } catch (Exception e) {
                throw new RuntimeException("Failed to execute OLLAMA request", e);
            }
        });
    }

    /**
     * Gets a required parameter from the params map.
     * 
     * @param params the parameters map
     * @param key the parameter key
     * @return the parameter value
     * @throws IllegalArgumentException if the parameter is missing or empty
     */
    private String getRequiredParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null || value.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("Required parameter '" + key + "' is missing or empty");
        }
        return value.toString().trim();
    }

    /**
     * Logs a message with timestamp (following AMCP logging pattern).
     * 
     * @param message the message to log
     */
    private void logMessage(String message) {
        String timestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [OllamaSpringAIConnector] " + message);
    }

    /**
     * Publishes an event when AI processing is complete.
     * This allows other agents to react to AI responses.
     * 
     * @param conversationId the conversation identifier  
     * @param response the AI response
     * @param agentContext the agent context for publishing events
     */
    public void publishAIResponse(String conversationId, String response, io.amcp.core.AgentContext agentContext) {
        if (agentContext != null) {
            Event aiEvent = Event.builder()
                .topic("ai.ollama.response")
                .payload(Map.of(
                    "conversationId", conversationId,
                    "response", response,
                    "source", "ollama-connector",
                    "timestamp", System.currentTimeMillis()
                ))
                .correlationId("ollama-" + conversationId)
                .build();

            agentContext.publishEvent(aiEvent);
            logMessage("Published AI response event for conversation: " + conversationId);
        }
    }
}