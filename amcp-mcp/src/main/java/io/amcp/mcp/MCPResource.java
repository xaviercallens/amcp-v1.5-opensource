package io.amcp.mcp;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST endpoint for MCP (Model Context Protocol) tool calls.
 * 
 * Exposes HTTP endpoints for LLMs and external systems to:
 * - Discover available tools
 * - Get tool schemas
 * - Call tools
 * - Check tool status
 * 
 * Spec Reference: Quarkus AMCP Extension.md §2.4 (Line 153)
 * "REST service that translates MCP requests to AMCP events"
 */
@Path("/mcp")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MCPResource {
    
    private static final Logger logger = LoggerFactory.getLogger(MCPResource.class);
    
    private MCPAdapter mcpAdapter;
    private MCPToolRegistry toolRegistry;

    public MCPResource() {
    }

    public MCPResource(MCPAdapter mcpAdapter, MCPToolRegistry toolRegistry) {
        this.mcpAdapter = mcpAdapter;
        this.toolRegistry = toolRegistry;
    }

    /**
     * Lists all available MCP tools.
     * 
     * @return List of tool definitions
     */
    @GET
    @Path("/tools")
    public Response listTools() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("tools", toolRegistry.getAllTools());
            response.put("count", toolRegistry.getToolCount());
            response.put("categories", toolRegistry.getCategories());
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Error listing tools: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }

    /**
     * Gets a specific tool by name.
     * 
     * @param toolName Name of the tool
     * @return Tool definition
     */
    @GET
    @Path("/tools/{toolName}")
    public Response getTool(@PathParam("toolName") String toolName) {
        try {
            MCPTool tool = toolRegistry.getTool(toolName);
            if (tool == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Tool not found: " + toolName))
                    .build();
            }
            
            return Response.ok(tool).build();
            
        } catch (Exception e) {
            logger.error("Error getting tool: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }

    /**
     * Gets tools by category.
     * 
     * @param category Category name
     * @return List of tools in category
     */
    @GET
    @Path("/tools/category/{category}")
    public Response getToolsByCategory(@PathParam("category") String category) {
        try {
            return Response.ok(Map.of(
                "category", category,
                "tools", toolRegistry.getToolsByCategory(category)
            )).build();
            
        } catch (Exception e) {
            logger.error("Error getting tools by category: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }

    /**
     * Calls an MCP tool.
     * 
     * Request body:
     * {
     *   "tool": "weather",
     *   "parameters": {
     *     "city": "Paris",
     *     "units": "celsius"
     *   }
     * }
     * 
     * @param request Tool call request
     * @return Tool execution result
     */
    @POST
    @Path("/call")
    public Response callTool(Map<String, Object> request) {
        try {
            String toolName = (String) request.get("tool");
            if (toolName == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Missing required field: tool"))
                    .build();
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = (Map<String, Object>) request.get("parameters");
            if (parameters == null) {
                parameters = new HashMap<>();
            }
            
            logger.info("🔧 MCP tool call: {} with parameters: {}", toolName, parameters);
            
            // Validate tool exists
            if (!toolRegistry.hasTool(toolName)) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Tool not found: " + toolName))
                    .build();
            }
            
            // Call tool via adapter
            CompletableFuture<Object> resultFuture = mcpAdapter.callTool(toolName, parameters);
            
            // Wait for result (synchronous for REST API)
            Object result = resultFuture.get();
            
            return Response.ok(Map.of(
                "tool", toolName,
                "result", result,
                "timestamp", System.currentTimeMillis()
            )).build();
            
        } catch (Exception e) {
            logger.error("Error calling tool: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }

    /**
     * Gets the OpenAPI schema for all tools.
     * Useful for LLMs to understand available tools.
     * 
     * @return OpenAPI-compatible schema
     */
    @GET
    @Path("/schema")
    public Response getSchema() {
        try {
            Map<String, Object> schema = new HashMap<>();
            schema.put("openapi", "3.0.0");
            schema.put("info", Map.of(
                "title", "AMCP MCP Tools",
                "version", "1.6.0",
                "description", "Model Context Protocol tools exposed by AMCP agents"
            ));
            
            // Build paths for each tool
            Map<String, Object> paths = new HashMap<>();
            for (MCPTool tool : toolRegistry.getAllTools()) {
                paths.put("/mcp/call/" + tool.getName(), Map.of(
                    "post", Map.of(
                        "summary", tool.getDescription(),
                        "parameters", tool.getParameters(),
                        "responses", Map.of(
                            "200", Map.of("description", "Success", "schema", tool.getReturns())
                        )
                    )
                ));
            }
            schema.put("paths", paths);
            
            return Response.ok(schema).build();
            
        } catch (Exception e) {
            logger.error("Error generating schema: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }

    /**
     * Gets MCP service status.
     * 
     * @return Status information
     */
    @GET
    @Path("/status")
    public Response getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "MCP Tool Service");
        status.put("version", "1.6.0");
        status.put("status", mcpAdapter != null ? "active" : "unavailable");
        status.put("toolCount", toolRegistry.getToolCount());
        status.put("timestamp", System.currentTimeMillis());
        
        if (mcpAdapter != null) {
            status.put("statistics", mcpAdapter.getStatistics());
        }
        
        return Response.ok(status).build();
    }

    // Setters for CDI injection
    
    public void setMcpAdapter(MCPAdapter mcpAdapter) {
        this.mcpAdapter = mcpAdapter;
    }

    public void setToolRegistry(MCPToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }
}
