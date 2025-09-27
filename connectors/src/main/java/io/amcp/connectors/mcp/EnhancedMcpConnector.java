package io.amcp.connectors.mcp;

import io.amcp.tools.ToolConnector;
import io.amcp.tools.ToolRequest;
import io.amcp.tools.ToolResponse;
import io.amcp.security.AuthenticationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * AMCP v1.5 Enhanced MCP Connector using HTTP/JSON instead of gRPC.
 * Provides enterprise-grade integration with Model Context Protocol servers.
 */
public class EnhancedMcpConnector implements ToolConnector {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedMcpConnector.class);
    
    private final String mcpServerUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Duration timeout;
    private volatile boolean initialized = false;
    
    public EnhancedMcpConnector(String mcpServerUrl) {
        this(mcpServerUrl, Duration.ofSeconds(30));
    }
    
    public EnhancedMcpConnector(String mcpServerUrl, Duration timeout) {
        this.mcpServerUrl = mcpServerUrl;
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(timeout)
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String getToolId() {
        return "enhanced-mcp-connector";
    }
    
    @Override
    public String getToolName() {
        return "Enhanced MCP Connector";
    }
    
    @Override
    public String getVersion() {
        return "1.5.0";
    }
    
    @Override
    public CompletableFuture<Boolean> isHealthy() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(mcpServerUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString("{\"jsonrpc\":\"2.0\",\"method\":\"ping\",\"id\":\"health-check\"}"))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return response.statusCode() < 500;
            } catch (Exception e) {
                logger.debug("Health check failed: {}", e.getMessage());
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> initialize(Map<String, Object> config) {
        return CompletableFuture.runAsync(() -> {
            logger.info("Initializing MCP connector for server: {}", mcpServerUrl);
            initialized = true;
        });
    }
    
    @Override
    public String[] getSupportedOperations() {
        return new String[]{"call", "invoke", "execute", "query"};
    }
    
    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            logger.info("Shutting down MCP connector");
            initialized = false;
        });
    }
    
    @Override
    public CompletableFuture<ToolResponse> invoke(ToolRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // Create MCP request payload
                Map<String, Object> mcpRequest = createMcpRequest(request);
                String requestBody = objectMapper.writeValueAsString(mcpRequest);
                
                // Build HTTP request with authentication
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(mcpServerUrl))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "AMCP-v1.5-MCP-Connector")
                    .timeout(timeout)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));
                
                // Add authentication headers if available
                if (request.hasAuthentication()) {
                    AuthenticationContext authContext = request.getAuthContext();
                    addAuthenticationHeaders(requestBuilder, authContext);
                }
                
                HttpRequest httpRequest = requestBuilder.build();
                
                // Execute request with retry logic
                HttpResponse<String> response = executeWithRetry(httpRequest, 3);
                
                // Process response
                long executionTime = System.currentTimeMillis() - startTime;
                return processResponse(response, request.getRequestId(), executionTime);
                
            } catch (Exception e) {
                long executionTime = System.currentTimeMillis() - startTime;
                logger.error("MCP connector error for request {}: {}", request.getRequestId(), e.getMessage(), e);
                return ToolResponse.error("MCP connector error: " + e.getMessage(), 
                                        request.getRequestId(), executionTime);
            }
        });
    }
    
    @Override
    public Map<String, Object> getRequestSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
            "operation", Map.of("type", "string", "description", "MCP operation to execute"),
            "parameters", Map.of("type", "object", "description", "Operation parameters"),
            "timeout", Map.of("type", "integer", "description", "Request timeout in milliseconds")
        ));
        schema.put("required", new String[]{"operation"});
        return schema;
    }
    
    private Map<String, Object> createMcpRequest(ToolRequest request) {
        Map<String, Object> mcpRequest = new HashMap<>();
        mcpRequest.put("jsonrpc", "2.0");
        mcpRequest.put("id", request.getRequestId());
        mcpRequest.put("method", request.getOperation());
        mcpRequest.put("params", request.getParameters());
        return mcpRequest;
    }
    
    private void addAuthenticationHeaders(HttpRequest.Builder requestBuilder, AuthenticationContext authContext) {
        if (authContext.hasToken()) {
            requestBuilder.header("Authorization", "Bearer " + authContext.getToken());
        }
        
        // Add custom headers
        authContext.getHeaders().forEach(requestBuilder::header);
    }
    
    private HttpResponse<String> executeWithRetry(HttpRequest request, int maxRetries) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() < 500) {
                    return response; // Success or client error (don't retry client errors)
                }
                
                logger.warn("MCP server error (attempt {}/{}): HTTP {}", 
                           attempt, maxRetries, response.statusCode());
                
            } catch (Exception e) {
                lastException = e;
                logger.warn("MCP request failed (attempt {}/{}): {}", 
                           attempt, maxRetries, e.getMessage());
            }
            
            if (attempt < maxRetries) {
                // Exponential backoff
                Thread.sleep(Math.min(1000 * (long) Math.pow(2, attempt - 1), 10000));
            }
        }
        
        throw new IOException("MCP request failed after " + maxRetries + " attempts", lastException);
    }
    
    private ToolResponse processResponse(HttpResponse<String> response, String requestId, long executionTime) {
        try {
            if (response.statusCode() == 200) {
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                
                if (jsonResponse.has("error")) {
                    JsonNode error = jsonResponse.get("error");
                    String errorMessage = error.has("message") ? error.get("message").asText() : "Unknown MCP error";
                    return ToolResponse.error("MCP server error: " + errorMessage, requestId, executionTime);
                }
                
                Object result = objectMapper.convertValue(jsonResponse.get("result"), Object.class);
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("mcpVersion", "1.0");
                metadata.put("httpStatus", response.statusCode());
                
                return ToolResponse.success(result, requestId, executionTime, metadata);
                
            } else {
                return ToolResponse.error("HTTP " + response.statusCode() + ": " + response.body(), 
                                        requestId, executionTime);
            }
            
        } catch (Exception e) {
            logger.error("Failed to process MCP response: {}", e.getMessage(), e);
            return ToolResponse.error("Failed to process MCP response: " + e.getMessage(), 
                                    requestId, executionTime);
        }
    }
}