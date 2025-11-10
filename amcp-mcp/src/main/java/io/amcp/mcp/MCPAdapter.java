package io.amcp.mcp;

import io.amcp.core.AgentContext;
import io.amcp.core.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * MCP Adapter - Translates between MCP tool calls and AMCP events.
 * 
 * This adapter enables AMCP agents to be exposed as MCP tools that can be
 * called by external LLMs or agent frameworks.
 * 
 * Pattern:
 * 1. LLM calls MCP tool → MCPAdapter receives request
 * 2. MCPAdapter publishes AMCP event → Agent handles event
 * 3. Agent publishes response event → MCPAdapter receives response
 * 4. MCPAdapter returns result → LLM receives response
 * 
 * Spec Reference: Quarkus AMCP Extension.md §2.4 (Lines 149-154)
 */
public class MCPAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(MCPAdapter.class);
    
    private static final String MCP_REQUEST_TOPIC = "mcp.request";
    private static final String MCP_RESPONSE_TOPIC = "mcp.response";
    private static final long DEFAULT_TIMEOUT_MS = 30000; // 30 seconds
    
    private final AgentContext agentContext;
    private final MCPToolRegistry toolRegistry;
    private final Map<String, CompletableFuture<Object>> pendingCalls = new ConcurrentHashMap<>();

    public MCPAdapter(AgentContext agentContext, MCPToolRegistry toolRegistry) {
        this.agentContext = agentContext;
        this.toolRegistry = toolRegistry;
        
        // Subscribe to MCP response topic
        subscribeToResponses();
    }

    /**
     * Calls an MCP tool by publishing an AMCP event and waiting for response.
     * 
     * @param toolName Tool name to call
     * @param parameters Tool parameters
     * @return Tool execution result
     */
    public CompletableFuture<Object> callTool(String toolName, Map<String, Object> parameters) {
        String callId = UUID.randomUUID().toString();
        
        logger.info("🔧 Calling MCP tool: {} (callId: {})", toolName, callId);
        
        try {
            // Validate tool exists
            MCPTool tool = toolRegistry.getTool(toolName);
            if (tool == null) {
                return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Tool not found: " + toolName));
            }
            
            // Create response future
            CompletableFuture<Object> responseFuture = new CompletableFuture<>();
            pendingCalls.put(callId, responseFuture);
            
            // Build AMCP event for tool call
            Map<String, Object> payload = new HashMap<>();
            payload.put("tool", toolName);
            payload.put("parameters", parameters);
            payload.put("callId", callId);
            
            Event requestEvent = Event.create(MCP_REQUEST_TOPIC + "." + toolName, payload);
            requestEvent.getMetadata().put("mcp.callId", callId);
            requestEvent.getMetadata().put("mcp.tool", toolName);
            
            // Publish event to AMCP mesh
            agentContext.getBroker().publish(requestEvent);
            
            logger.debug("Published MCP request event for tool: {}", toolName);
            
            // Set timeout
            responseFuture.orTimeout(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> {
                    pendingCalls.remove(callId);
                    logger.error("MCP tool call timeout: {}", toolName);
                    return Map.of("error", "Tool call timeout");
                });
            
            return responseFuture;
            
        } catch (Exception e) {
            logger.error("Error calling MCP tool {}: {}", toolName, e.getMessage(), e);
            pendingCalls.remove(callId);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Subscribes to MCP response events from agents.
     */
    private void subscribeToResponses() {
        agentContext.getBroker().subscribe(MCP_RESPONSE_TOPIC + ".**", this::handleResponse);
        logger.info("Subscribed to MCP response topic");
    }

    /**
     * Handles MCP response events from agents.
     */
    private void handleResponse(Event event) {
        try {
            String callId = (String) event.getMetadata().get("mcp.callId");
            if (callId == null) {
                logger.warn("MCP response missing callId");
                return;
            }
            
            CompletableFuture<Object> future = pendingCalls.remove(callId);
            if (future == null) {
                logger.warn("No pending call found for callId: {}", callId);
                return;
            }
            
            // Extract result from event
            Object result = event.getData();
            
            logger.debug("Received MCP response for callId: {}", callId);
            future.complete(result);
            
        } catch (Exception e) {
            logger.error("Error handling MCP response: {}", e.getMessage(), e);
        }
    }

    /**
     * Translates MCP tool call to AMCP event topic.
     * 
     * Pattern: mcp.request.{toolName}
     * Example: mcp.request.weather → Event published to weather agent
     */
    public String getTopicForTool(String toolName) {
        return MCP_REQUEST_TOPIC + "." + toolName;
    }

    /**
     * Gets statistics about pending MCP calls.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pendingCalls", pendingCalls.size());
        stats.put("registeredTools", toolRegistry.getAllTools().size());
        return stats;
    }
}
