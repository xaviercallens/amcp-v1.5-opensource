package io.amcp.tools;

import java.util.concurrent.CompletableFuture;
import java.util.Map;

/**
 * AMCP v1.4 Tool Connector Interface.
 * Defines the contract for integrating external tools and MCP (Model Context Protocol) servers
 * into the AMCP agent ecosystem.
 */
public interface ToolConnector {
    
    /**
     * Unique identifier for this tool connector
     */
    String getToolId();
    
    /**
     * Human-readable name for the tool
     */
    String getToolName();
    
    /**
     * Version of the tool connector implementation
     */
    String getVersion();
    
    /**
     * Check if the tool is currently healthy and available
     */
    CompletableFuture<Boolean> isHealthy();
    
    /**
     * Initialize the connector with configuration
     */
    CompletableFuture<Void> initialize(Map<String, Object> config);
    
    /**
     * Execute a tool operation with the given request
     */
    CompletableFuture<ToolResponse> invoke(ToolRequest request);
    
    /**
     * Get the schema definition for requests this tool accepts
     */
    Map<String, Object> getRequestSchema();
    
    /**
     * Get supported operations for this tool
     */
    String[] getSupportedOperations();
    
    /**
     * Clean up resources and connections
     */
    CompletableFuture<Void> shutdown();
}