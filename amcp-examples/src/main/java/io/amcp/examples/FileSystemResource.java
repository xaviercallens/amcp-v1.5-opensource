package io.amcp.examples;

import io.amcp.core.AgentContext;
import io.amcp.core.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * REST API for FileSystemAgent operations.
 * 
 * Endpoints:
 * - GET  /fs/list?path=/path/to/dir - List files in directory
 * - GET  /fs/info?path=/path/to/file - Get file information
 * - POST /fs/search - Search for files
 */
@Path("/fs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FileSystemResource {

    @Inject
    AgentContext agentContext;

    /**
     * Lists files in a directory.
     * 
     * Example: GET /fs/list?path=/tmp&includeHidden=false
     */
    @GET
    @Path("/list")
    public Map<String, Object> listFiles(
            @QueryParam("path") @DefaultValue(".") String path,
            @QueryParam("includeHidden") @DefaultValue("false") boolean includeHidden) {
        
        try {
            // Prepare request
            Map<String, Object> request = new HashMap<>();
            request.put("path", path);
            request.put("includeHidden", includeHidden);
            
            // Subscribe to response
            CompletableFuture<Map<String, Object>> responseFuture = new CompletableFuture<>();
            
            String subId = agentContext.getEventBroker().subscribe("fs.list.response", event -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> response = event.getPayload(Map.class);
                responseFuture.complete(response);
            });
            
            // Also subscribe to error
            String errorSubId = agentContext.getEventBroker().subscribe("fs.list.error", event -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> error = event.getPayload(Map.class);
                responseFuture.complete(error);
            });
            
            try {
                // Publish request
                Event requestEvent = Event.create("fs.list.request", 
                        "https://amcp.dev/fs-api", request);
                agentContext.getEventBroker().publish(requestEvent).join();
                
                // Wait for response
                Map<String, Object> result = responseFuture.get(5, TimeUnit.SECONDS);
                result.put("success", !result.containsKey("error"));
                return result;
                
            } finally {
                agentContext.getEventBroker().unsubscribe(subId);
                agentContext.getEventBroker().unsubscribe(errorSubId);
            }
            
        } catch (Exception e) {
            return createErrorResponse("Failed to list files: " + e.getMessage());
        }
    }

    /**
     * Gets information about a specific file.
     * 
     * Example: GET /fs/info?path=/tmp/test.txt
     */
    @GET
    @Path("/info")
    public Map<String, Object> getFileInfo(@QueryParam("path") String path) {
        
        if (path == null || path.isEmpty()) {
            return createErrorResponse("Path parameter is required");
        }
        
        try {
            // Prepare request
            Map<String, Object> request = new HashMap<>();
            request.put("path", path);
            
            // Subscribe to response
            CompletableFuture<Map<String, Object>> responseFuture = new CompletableFuture<>();
            
            String subId = agentContext.getEventBroker().subscribe("fs.info.response", event -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> response = event.getPayload(Map.class);
                responseFuture.complete(response);
            });
            
            String errorSubId = agentContext.getEventBroker().subscribe("fs.info.error", event -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> error = event.getPayload(Map.class);
                responseFuture.complete(error);
            });
            
            try {
                // Publish request
                Event requestEvent = Event.create("fs.info.request",
                        "https://amcp.dev/fs-api", request);
                agentContext.getEventBroker().publish(requestEvent).join();
                
                // Wait for response
                Map<String, Object> result = responseFuture.get(5, TimeUnit.SECONDS);
                result.put("success", !result.containsKey("error"));
                return result;
                
            } finally {
                agentContext.getEventBroker().unsubscribe(subId);
                agentContext.getEventBroker().unsubscribe(errorSubId);
            }
            
        } catch (Exception e) {
            return createErrorResponse("Failed to get file info: " + e.getMessage());
        }
    }

    /**
     * Searches for files matching a pattern.
     * 
     * Example: POST /fs/search
     * Body: { "path": "/tmp", "pattern": "*.txt", "maxDepth": 2 }
     */
    @POST
    @Path("/search")
    public Map<String, Object> searchFiles(Map<String, Object> searchRequest) {
        
        if (searchRequest == null || !searchRequest.containsKey("path")) {
            return createErrorResponse("Search request must include 'path'");
        }
        
        try {
            // Subscribe to response
            CompletableFuture<Map<String, Object>> responseFuture = new CompletableFuture<>();
            
            String subId = agentContext.getEventBroker().subscribe("fs.search.response", event -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> response = event.getPayload(Map.class);
                responseFuture.complete(response);
            });
            
            String errorSubId = agentContext.getEventBroker().subscribe("fs.search.error", event -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> error = event.getPayload(Map.class);
                responseFuture.complete(error);
            });
            
            try {
                // Publish request
                Event requestEvent = Event.create("fs.search.request",
                        "https://amcp.dev/fs-api", searchRequest);
                agentContext.getEventBroker().publish(requestEvent).join();
                
                // Wait for response
                Map<String, Object> result = responseFuture.get(10, TimeUnit.SECONDS);
                result.put("success", !result.containsKey("error"));
                return result;
                
            } finally {
                agentContext.getEventBroker().unsubscribe(subId);
                agentContext.getEventBroker().unsubscribe(errorSubId);
            }
            
        } catch (Exception e) {
            return createErrorResponse("Failed to search files: " + e.getMessage());
        }
    }

    /**
     * Gets the status of all file system agents.
     */
    @GET
    @Path("/agents")
    public Map<String, Object> getAgentStatus() {
        Map<String, Object> status = new HashMap<>();
        
        long fsAgentCount = agentContext.getAgents().values().stream()
                .filter(agent -> agent.getClass().getSimpleName().equals("FileSystemAgent"))
                .count();
        
        status.put("fileSystemAgents", fsAgentCount);
        status.put("totalAgents", agentContext.getAgents().size());
        status.put("contextRunning", agentContext.isStarted());
        
        return status;
    }

    /**
     * Creates an error response map.
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        return error;
    }
}
