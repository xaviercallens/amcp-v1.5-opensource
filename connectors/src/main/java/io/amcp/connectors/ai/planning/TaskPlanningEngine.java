package io.amcp.connectors.ai.planning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.amcp.tools.ToolRequest;
import io.amcp.tools.ToolResponse;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Advanced Task Planning Engine for AMCP v1.5 Enhanced Orchestrator
 * 
 * Provides sophisticated task planning capabilities including:
 * - Structured JSON task plan generation with validation
 * - Few-shot learning prompts for reliable LLM responses
 * - Dependency management and parallel execution planning
 * - Data normalization and input validation
 * - Fallback planning strategies
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 * @since 1.5.0
 */
public class TaskPlanningEngine {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Generate a comprehensive task plan for the given query
     */
    public TaskPlan generateTaskPlan(String normalizedQuery, Set<AgentInfo> agents, String correlationId) {
        try {
            // Build enhanced planning prompt with few-shot examples
            String planningPrompt = buildAdvancedPlanningPrompt(normalizedQuery, agents);
            
            // Create LLM parameters with JSON enforcement
            Map<String, Object> params = createLLMParameters(planningPrompt, "json");
            
            // Note: This would integrate with actual LLM connector
            String mockPlanJson = generateMockTaskPlan(normalizedQuery, agents);
            
            return parseAndValidateTaskPlan(mockPlanJson, correlationId, normalizedQuery);
            
        } catch (Exception e) {
            logMessage("Task planning failed: " + e.getMessage());
            return createFallbackTaskPlan(normalizedQuery, agents, correlationId);
        }
    }
    
    /**
     * Build advanced planning prompt with few-shot examples
     */
    private String buildAdvancedPlanningPrompt(String query, Set<AgentInfo> agents) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an expert task planner for AMCP v1.5. Analyze the query and create a structured task plan.\n\n");
        
        // Available agents and capabilities
        prompt.append("AVAILABLE AGENTS:\n");
        for (AgentInfo agent : agents) {
            prompt.append("• ").append(agent.getAgentId()).append(": ");
            prompt.append(String.join(", ", agent.getCapabilities())).append("\n");
        }
        
        // Few-shot examples
        prompt.append("\nEXAMPLES:\n");
        prompt.append("Query: \"What's the weather in Paris and stock price of Apple?\"\n");
        prompt.append("Response: [\n");
        prompt.append("  {\"capability\": \"weather.get\", \"params\": {\"location\": \"Paris,FR\", \"date\": \"2025-09-30\"}, \"agent\": \"WeatherAgent\", \"priority\": 1},\n");
        prompt.append("  {\"capability\": \"stock.price\", \"params\": {\"symbol\": \"AAPL\"}, \"agent\": \"StockPriceAgent\", \"priority\": 1}\n");
        prompt.append("]\n\n");
        
        prompt.append("Query: \"Plan a trip to Rome with weather forecast\"\n");
        prompt.append("Response: [\n");
        prompt.append("  {\"capability\": \"travel.plan\", \"params\": {\"destination\": \"Rome,IT\", \"duration\": \"3days\"}, \"agent\": \"TravelPlannerAgent\", \"priority\": 1, \"dependencies\": []},\n");
        prompt.append("  {\"capability\": \"weather.forecast\", \"params\": {\"location\": \"Rome,IT\", \"days\": 7}, \"agent\": \"WeatherAgent\", \"priority\": 2, \"dependencies\": [\"travel.plan\"]}\n");
        prompt.append("]\n\n");
        
        prompt.append("RULES:\n");
        prompt.append("- Respond ONLY with valid JSON array\n");
        prompt.append("- Each task must have: capability, params, agent, priority\n");
        prompt.append("- Optional: dependencies (array of capability names)\n");
        prompt.append("- Priority 1 = execute immediately, 2 = wait for dependencies\n");
        prompt.append("- Normalize locations to \"City,Country\" format\n");
        prompt.append("- Use ISO date formats\n\n");
        
        prompt.append("Query: \"").append(query).append("\"\n");
        prompt.append("Response:");
        
        return prompt.toString();
    }
    
    /**
     * Parse and validate task plan JSON
     */
    private TaskPlan parseAndValidateTaskPlan(String planJson, String correlationId, String userQuery) {
        try {
            JsonNode planNode = objectMapper.readTree(planJson);
            
            if (!planNode.isArray()) {
                throw new IllegalArgumentException("Task plan must be JSON array");
            }
            
            List<TaskDefinition> tasks = new ArrayList<>();
            for (JsonNode taskNode : planNode) {
                TaskDefinition task = parseTaskDefinition(taskNode);
                validateTaskDefinition(task);
                tasks.add(task);
            }
            
            return new TaskPlan(UUID.randomUUID().toString(), correlationId, userQuery, tasks);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse task plan: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parse individual task definition from JSON
     */
    private TaskDefinition parseTaskDefinition(JsonNode taskNode) {
        String capability = taskNode.get("capability").asText();
        String agent = taskNode.get("agent").asText();
        int priority = taskNode.get("priority").asInt();
        
        Map<String, Object> params = new HashMap<>();
        JsonNode paramsNode = taskNode.get("params");
        if (paramsNode != null) {
            paramsNode.fields().forEachRemaining(entry -> {
                params.put(entry.getKey(), entry.getValue().asText());
            });
        }
        
        List<String> dependencies = new ArrayList<>();
        JsonNode depsNode = taskNode.get("dependencies");
        if (depsNode != null && depsNode.isArray()) {
            depsNode.forEach(dep -> dependencies.add(dep.asText()));
        }
        
        return new TaskDefinition(
            UUID.randomUUID().toString(), // taskId
            capability, // name  
            "Generated task: " + capability, // description
            agent, // agentType
            params, // parameters
            new HashSet<>(dependencies), // dependencies as Set<String>
            priority, // priority
            30000L, // timeoutMs (30 seconds)
            false // optional
        );
    }
    
    /**
     * Validate task definition
     */
    private void validateTaskDefinition(TaskDefinition task) {
        if (task.getName() == null || task.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Task name cannot be empty");
        }
        if (task.getAgentType() == null || task.getAgentType().trim().isEmpty()) {
            throw new IllegalArgumentException("Task agent type cannot be empty");
        }
        if (task.getPriority() < 1) {
            throw new IllegalArgumentException("Task priority must be >= 1");
        }
    }
    
    /**
     * Create fallback task plan for error scenarios
     */
    private TaskPlan createFallbackTaskPlan(String query, Set<AgentInfo> agents, String correlationId) {
        // Simple fallback: route to most appropriate single agent
        String targetAgent = determineBestAgent(query, agents);
        
        TaskDefinition fallbackTask = new TaskDefinition(
            UUID.randomUUID().toString(), // taskId
            "general.query", // name
            "Fallback task for: " + query, // description
            targetAgent, // agentType
            Map.of("query", query), // parameters
            new HashSet<>(), // dependencies
            1, // priority
            30000L, // timeoutMs
            false // optional
        );
        
        return new TaskPlan(UUID.randomUUID().toString(), correlationId, query, Arrays.asList(fallbackTask));
    }
    
    /**
     * Determine best agent based on query keywords
     */
    private String determineBestAgent(String query, Set<AgentInfo> agents) {
        String lowercaseQuery = query.toLowerCase();
        
        if (lowercaseQuery.contains("weather") || lowercaseQuery.contains("temperature") || lowercaseQuery.contains("forecast")) {
            return "WeatherAgent";
        } else if (lowercaseQuery.contains("stock") || lowercaseQuery.contains("price") || lowercaseQuery.contains("market")) {
            return "StockPriceAgent";
        } else if (lowercaseQuery.contains("travel") || lowercaseQuery.contains("trip") || lowercaseQuery.contains("vacation")) {
            return "TravelPlannerAgent";
        } else {
            return "ChatAgent"; // Default fallback
        }
    }
    
    /**
     * Create LLM parameters for structured response
     */
    private Map<String, Object> createLLMParameters(String prompt, String format) {
        Map<String, Object> params = new HashMap<>();
        params.put("prompt", prompt);
        params.put("model", "tinyllama");
        params.put("temperature", 0.2); // Lower temperature for structured responses
        params.put("max_tokens", 500);
        params.put("format", format);
        return params;
    }
    
    /**
     * Generate mock task plan for testing (would be replaced with actual LLM call)
     */
    private String generateMockTaskPlan(String query, Set<AgentInfo> agents) {
        String lowercaseQuery = query.toLowerCase();
        
        if (lowercaseQuery.contains("weather") && lowercaseQuery.contains("paris")) {
            return "[{\"capability\": \"weather.get\", \"params\": {\"location\": \"Paris,FR\", \"date\": \"2025-09-30\"}, \"agent\": \"WeatherAgent\", \"priority\": 1}]";
        } else if (lowercaseQuery.contains("stock") && lowercaseQuery.contains("apple")) {
            return "[{\"capability\": \"stock.price\", \"params\": {\"symbol\": \"AAPL\"}, \"agent\": \"StockPriceAgent\", \"priority\": 1}]";
        } else if (lowercaseQuery.contains("travel") && lowercaseQuery.contains("rome")) {
            return "[{\"capability\": \"travel.plan\", \"params\": {\"destination\": \"Rome,IT\", \"duration\": \"3days\"}, \"agent\": \"TravelPlannerAgent\", \"priority\": 1}]";
        } else {
            return "[{\"capability\": \"general.query\", \"params\": {\"query\": \"" + query + "\"}, \"agent\": \"ChatAgent\", \"priority\": 1}]";
        }
    }
    
    private void logMessage(String message) {
        System.out.println("[TaskPlanningEngine] " + message);
    }
    
    /**
     * Update capabilities for an agent
     */
    public void updateAgentCapabilities(String agentId, Set<String> capabilities) {
        logMessage("Updating capabilities for agent " + agentId + ": " + capabilities);
        // In a real implementation, this would update the agent registry
        // For now, we just log the operation
    }
    
    /**
     * Remove an agent and its capabilities
     */
    public void removeAgentCapabilities(String agentId) {
        logMessage("Removing capabilities for agent " + agentId);
        // In a real implementation, this would remove the agent from the registry
        // For now, we just log the operation
    }
    
    // Helper classes for type safety
    public static class AgentInfo {
        private final String agentId;
        private final List<String> capabilities;
        
        public AgentInfo(String agentId, List<String> capabilities) {
            this.agentId = agentId;
            this.capabilities = new ArrayList<>(capabilities);
        }
        
        public String getAgentId() { return agentId; }
        public List<String> getCapabilities() { return new ArrayList<>(capabilities); }
    }
}