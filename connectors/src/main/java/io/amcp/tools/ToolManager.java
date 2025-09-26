package io.amcp.tools;

import io.amcp.tools.mcp.DuckDuckGoMCPConnector;
import io.amcp.tools.mcp.WeatherAPIMCPConnector;
import io.amcp.tools.bridge.A2ABridgeConnector;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AMCP v1.4 Tool Manager.
 * Centralized management system for all MCP tool connectors in the AMCP ecosystem.
 * Provides registration, lifecycle management, and orchestration capabilities.
 */
public class ToolManager {
    
    private static ToolManager instance;
    private static final Object lock = new Object();
    
    private final Map<String, ToolConnector> connectors = new ConcurrentHashMap<>();
    private final Map<String, ToolConfig> configurations = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong(0);
    
    private volatile boolean initialized = false;
    
    private ToolManager() {
        // Private constructor for singleton
    }
    
    public static ToolManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ToolManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initialize the Tool Manager with default connectors
     */
    public CompletableFuture<Void> initialize() {
        return initialize(new HashMap<>());
    }
    
    /**
     * Initialize the Tool Manager with custom configuration
     */
    public CompletableFuture<Void> initialize(Map<String, Map<String, Object>> toolConfigs) {
        return CompletableFuture.runAsync(() -> {
            if (initialized) {
                return;
            }
            
            try {
                // Register default connectors
                registerDefaultConnectors();
                
                // Initialize connectors with provided configurations
                initializeConnectors(toolConfigs);
                
                this.initialized = true;
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize ToolManager", e);
            }
        });
    }
    
    /**
     * Register a custom tool connector
     */
    public CompletableFuture<Void> registerConnector(ToolConnector connector, Map<String, Object> config) {
        return CompletableFuture.runAsync(() -> {
            String toolId = connector.getToolId();
            
            if (connectors.containsKey(toolId)) {
                throw new IllegalArgumentException("Tool connector already registered: " + toolId);
            }
            
            connectors.put(toolId, connector);
            configurations.put(toolId, new ToolConfig(toolId, config));
            
            // Initialize the connector if the manager is already initialized
            if (initialized) {
                try {
                    connector.initialize(config).get();
                } catch (Exception e) {
                    connectors.remove(toolId);
                    configurations.remove(toolId);
                    throw new RuntimeException("Failed to initialize connector: " + toolId, e);
                }
            }
        });
    }
    
    /**
     * Execute a tool operation
     */
    public CompletableFuture<ToolResponse> executeTool(String toolId, ToolRequest request) {
        if (!initialized) {
            return CompletableFuture.completedFuture(
                ToolResponse.error("ToolManager not initialized", request.getRequestId(), 0)
            );
        }
        
        ToolConnector connector = connectors.get(toolId);
        if (connector == null) {
            return CompletableFuture.completedFuture(
                ToolResponse.error("Tool connector not found: " + toolId, request.getRequestId(), 0)
            );
        }
        
        return connector.invoke(request)
                .thenApply(response -> {
                    // Add tool manager metadata
                    Map<String, Object> metadata = new HashMap<>(response.getMetadata());
                    metadata.put("tool_id", toolId);
                    metadata.put("tool_name", connector.getToolName());
                    metadata.put("tool_version", connector.getVersion());
                    metadata.put("request_number", requestCounter.incrementAndGet());
                    
                    return response.isSuccess() 
                        ? ToolResponse.success(response.getData(), response.getRequestId(), 
                                             response.getExecutionTimeMs(), metadata)
                        : ToolResponse.error(response.getErrorMessage(), response.getRequestId(), 
                                           response.getExecutionTimeMs(), metadata);
                });
    }
    
    /**
     * Execute tool operation with automatic request ID generation
     */
    public CompletableFuture<ToolResponse> executeTool(String toolId, String operation, 
                                                      Map<String, Object> parameters) {
        ToolRequest request = new ToolRequest(operation, parameters);
        return executeTool(toolId, request);
    }
    
    /**
     * Get health status of all connectors
     */
    public CompletableFuture<Map<String, Boolean>> getHealthStatus() {
        Map<String, CompletableFuture<Boolean>> healthChecks = new HashMap<>();
        
        for (Map.Entry<String, ToolConnector> entry : connectors.entrySet()) {
            String toolId = entry.getKey();
            ToolConnector connector = entry.getValue();
            healthChecks.put(toolId, connector.isHealthy());
        }
        
        return CompletableFuture.allOf(
            healthChecks.values().toArray(new CompletableFuture[0])
        ).thenApply(v -> {
            Map<String, Boolean> results = new HashMap<>();
            for (Map.Entry<String, CompletableFuture<Boolean>> entry : healthChecks.entrySet()) {
                try {
                    results.put(entry.getKey(), entry.getValue().get());
                } catch (Exception e) {
                    results.put(entry.getKey(), false);
                }
            }
            return results;
        });
    }
    
    /**
     * Get detailed information about all registered connectors
     */
    public Map<String, ToolInfo> getToolInfo() {
        Map<String, ToolInfo> toolInfo = new HashMap<>();
        
        for (Map.Entry<String, ToolConnector> entry : connectors.entrySet()) {
            String toolId = entry.getKey();
            ToolConnector connector = entry.getValue();
            ToolConfig config = configurations.get(toolId);
            
            ToolInfo info = new ToolInfo(
                toolId,
                connector.getToolName(),
                connector.getVersion(),
                connector.getSupportedOperations(),
                connector.getRequestSchema(),
                config != null ? config.getConfiguration() : new HashMap<>()
            );
            
            toolInfo.put(toolId, info);
        }
        
        return toolInfo;
    }
    
    /**
     * Search for web content using DuckDuckGo
     */
    public CompletableFuture<ToolResponse> searchWeb(String query) {
        Map<String, Object> params = new HashMap<>();
        params.put("query", query);
        return executeTool("duckduckgo-mcp", "search", params);
    }
    
    /**
     * Get current weather for a location
     */
    public CompletableFuture<ToolResponse> getCurrentWeather(String location) {
        Map<String, Object> params = new HashMap<>();
        params.put("location", location);
        return executeTool("weather-api-mcp", "current_weather", params);
    }
    
    /**
     * Get weather forecast for a location
     */
    public CompletableFuture<ToolResponse> getWeatherForecast(String location, int days) {
        Map<String, Object> params = new HashMap<>();
        params.put("location", location);
        params.put("days", days);
        return executeTool("weather-api-mcp", "forecast", params);
    }
    
    /**
     * Establish agent-to-agent bridge connection
     */
    public CompletableFuture<ToolResponse> establishA2AConnection(String bridgeUrl, String connectionId) {
        Map<String, Object> params = new HashMap<>();
        params.put("bridgeUrl", bridgeUrl);
        if (connectionId != null) {
            params.put("connectionId", connectionId);
        }
        return executeTool("a2a-bridge", "establish_connection", params);
    }
    
    /**
     * Send message via A2A bridge
     */
    public CompletableFuture<ToolResponse> sendA2AMessage(String connectionId, String targetAgentId, 
                                                         Map<String, Object> messageData) {
        Map<String, Object> params = new HashMap<>(messageData);
        params.put("connectionId", connectionId);
        params.put("targetAgentId", targetAgentId);
        return executeTool("a2a-bridge", "send_message", params);
    }
    
    /**
     * Broadcast event to all A2A connections
     */
    public CompletableFuture<ToolResponse> broadcastEvent(String topic, Map<String, Object> eventData) {
        Map<String, Object> params = new HashMap<>(eventData);
        params.put("topic", topic);
        return executeTool("a2a-bridge", "broadcast_event", params);
    }
    
    /**
     * Shutdown all connectors
     */
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            this.initialized = false;
            
            List<CompletableFuture<Void>> shutdownTasks = new ArrayList<>();
            
            for (ToolConnector connector : connectors.values()) {
                shutdownTasks.add(connector.shutdown());
            }
            
            try {
                CompletableFuture.allOf(shutdownTasks.toArray(new CompletableFuture[0])).get();
            } catch (Exception e) {
                System.err.println("Error during tool manager shutdown: " + e.getMessage());
            }
            
            connectors.clear();
            configurations.clear();
        });
    }
    
    private void registerDefaultConnectors() throws Exception {
        // Register DuckDuckGo MCP Connector
        DuckDuckGoMCPConnector duckduckgoConnector = new DuckDuckGoMCPConnector();
        connectors.put(duckduckgoConnector.getToolId(), duckduckgoConnector);
        
        // Register Weather API MCP Connector
        WeatherAPIMCPConnector weatherConnector = new WeatherAPIMCPConnector();
        connectors.put(weatherConnector.getToolId(), weatherConnector);
        
        // Register A2A Bridge Connector
        A2ABridgeConnector bridgeConnector = new A2ABridgeConnector();
        connectors.put(bridgeConnector.getToolId(), bridgeConnector);
    }
    
    private void initializeConnectors(Map<String, Map<String, Object>> toolConfigs) throws Exception {
        List<CompletableFuture<Void>> initTasks = new ArrayList<>();
        
        for (Map.Entry<String, ToolConnector> entry : connectors.entrySet()) {
            String toolId = entry.getKey();
            ToolConnector connector = entry.getValue();
            
            Map<String, Object> config = toolConfigs.getOrDefault(toolId, new HashMap<>());
            configurations.put(toolId, new ToolConfig(toolId, config));
            
            initTasks.add(connector.initialize(config));
        }
        
        try {
            CompletableFuture.allOf(initTasks.toArray(new CompletableFuture[0])).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize one or more connectors", e);
        }
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    // Helper classes
    
    public static class ToolInfo {
        private final String toolId;
        private final String toolName;
        private final String version;
        private final String[] supportedOperations;
        private final Map<String, Object> requestSchema;
        private final Map<String, Object> configuration;
        
        public ToolInfo(String toolId, String toolName, String version, 
                       String[] supportedOperations, Map<String, Object> requestSchema,
                       Map<String, Object> configuration) {
            this.toolId = toolId;
            this.toolName = toolName;
            this.version = version;
            this.supportedOperations = supportedOperations;
            this.requestSchema = requestSchema;
            this.configuration = configuration;
        }
        
        public String getToolId() { return toolId; }
        public String getToolName() { return toolName; }
        public String getVersion() { return version; }
        public String[] getSupportedOperations() { return supportedOperations; }
        public Map<String, Object> getRequestSchema() { return requestSchema; }
        public Map<String, Object> getConfiguration() { return configuration; }
    }
    
    private static class ToolConfig {
        private final String toolId;
        private final Map<String, Object> configuration;
        
        public ToolConfig(String toolId, Map<String, Object> configuration) {
            this.toolId = toolId;
            this.configuration = configuration != null ? new HashMap<>(configuration) : new HashMap<>();
        }
        
        public String getToolId() { return toolId; }
        public Map<String, Object> getConfiguration() { return new HashMap<>(configuration); }
    }
}