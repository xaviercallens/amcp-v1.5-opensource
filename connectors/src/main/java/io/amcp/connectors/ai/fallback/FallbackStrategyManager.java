package io.amcp.connectors.ai.fallback;

import io.amcp.connectors.ai.planning.TaskPlanningEngine.AgentInfo;
import io.amcp.connectors.ai.planning.TaskDefinition;
import io.amcp.tools.ToolRequest;
import io.amcp.tools.ToolResponse;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive Fallback Strategy Manager for AMCP v1.5 Enhanced Orchestrator
 * 
 * Provides robust fallback strategies including:
 * - LLM re-prompting with stricter instructions
 * - Alternate agent selection and routing
 * - Partial response composition with error notices
 * - Graceful degradation strategies
 * - Circuit breaker patterns for failing services
 * - Emergency response generation
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 * @since 1.5.0
 */
public class FallbackStrategyManager {
    
    // Fallback configuration
    private static final int MAX_REPROMPT_ATTEMPTS = 3;
    private static final int MAX_AGENT_RETRIES = 2;
    private static final int CIRCUIT_BREAKER_THRESHOLD = 5;
    private static final long CIRCUIT_BREAKER_TIMEOUT_MS = 30000; // 30 seconds
    
    // Strategy tracking
    private final Map<String, FallbackContext> activeStrategies = new HashMap<>();
    private final Map<String, CircuitBreakerState> agentCircuitBreakers = new HashMap<>();
    private final Map<String, AtomicInteger> agentFailureCounts = new HashMap<>();
    
    // Emergency response templates
    private final Map<String, String> emergencyResponses = new HashMap<>();
    
    public FallbackStrategyManager() {
        initializeEmergencyResponses();
    }
    
    /**
     * Handle orchestration failure with comprehensive fallback strategies
     */
    public OrchestrationResult handleOrchestrationFailure(OrchestrationSession session, Throwable error) {
        String correlationId = session.getCorrelationId();
        FallbackContext context = activeStrategies.computeIfAbsent(correlationId, 
            k -> new FallbackContext(correlationId, session.getOriginalQuery()));
        
        logMessage("Handling orchestration failure for: " + correlationId + " - " + error.getMessage());
        
        // Try different fallback strategies in order
        OrchestrationResult result = tryFallbackStrategies(session, context, error);
        
        // Clean up context
        activeStrategies.remove(correlationId);
        
        return result;
    }
    
    /**
     * Handle malformed LLM output with re-prompting strategies
     */
    public String handleMalformedLLMOutput(String originalPrompt, String malformedOutput, 
                                          String correlationId, Object llmConnector) {
        FallbackContext context = activeStrategies.computeIfAbsent(correlationId,
            k -> new FallbackContext(correlationId, originalPrompt));
        
        if (context.getRepromptAttempts() >= MAX_REPROMPT_ATTEMPTS) {
            logMessage("Max reprompt attempts reached for: " + correlationId);
            return generateEmergencyResponse("llm_failure", originalPrompt);
        }
        
        context.incrementRepromptAttempts();
        
        // Create stricter prompt
        String stricterPrompt = createStricterPrompt(originalPrompt, malformedOutput, context.getRepromptAttempts());
        
        try {
            // Re-prompt with stricter instructions
            String improvedResponse = repromptLLM(stricterPrompt, correlationId, llmConnector);
            
            if (isValidResponse(improvedResponse)) {
                logMessage("Successful reprompt for: " + correlationId + " on attempt " + context.getRepromptAttempts());
                return improvedResponse;
            } else {
                // Recursive fallback with next attempt
                return handleMalformedLLMOutput(stricterPrompt, improvedResponse, correlationId, llmConnector);
            }
            
        } catch (Exception e) {
            logMessage("Reprompt failed for: " + correlationId + " - " + e.getMessage());
            return generateEmergencyResponse("llm_failure", originalPrompt);
        }
    }
    
    /**
     * Handle agent failure with alternate routing and circuit breaker logic
     */
    public CompletableFuture<TaskResult> handleAgentFailure(TaskExecution taskExecution, 
                                                           Throwable error, 
                                                           Set<AgentInfo> availableAgents) {
        String agentId = taskExecution.getDefinition().getAgentType();
        String correlationId = taskExecution.getCorrelationId();
        
        // Update circuit breaker state
        updateCircuitBreaker(agentId, false);
        
        // Check if circuit breaker is open
        if (isCircuitBreakerOpen(agentId)) {
            logMessage("Circuit breaker open for agent: " + agentId + ", finding alternate");
            return routeToAlternateAgent(taskExecution, availableAgents);
        }
        
        // Try retry with same agent if under retry limit
        FallbackContext context = activeStrategies.computeIfAbsent(correlationId,
            k -> new FallbackContext(correlationId, taskExecution.getDefinition().getName()));
        
        if (context.getAgentRetryCount(agentId) < MAX_AGENT_RETRIES) {
            context.incrementAgentRetry(agentId);
            logMessage("Retrying agent: " + agentId + " attempt: " + context.getAgentRetryCount(agentId));
            return retryTaskWithAgent(taskExecution);
        }
        
        // Find alternate agent
        return routeToAlternateAgent(taskExecution, availableAgents);
    }
    
    /**
     * Compose partial response when some tasks fail
     */
    public OrchestrationResult composePartialResponse(List<TaskResult> completedTasks, 
                                                     List<TaskExecution> failedTasks,
                                                     OrchestrationSession session) {
        StringBuilder response = new StringBuilder();
        List<String> errors = new ArrayList<>();
        
        // Add successful results
        if (!completedTasks.isEmpty()) {
            response.append("I was able to gather some information for you:\n\n");
            
            for (TaskResult result : completedTasks) {
                if (result.isSuccess()) {
                    response.append("• ").append(result.getFormattedResponse()).append("\n");
                }
            }
        }
        
        // Add failure notices
        if (!failedTasks.isEmpty()) {
            response.append("\nHowever, I encountered some issues:\n\n");
            
            for (TaskExecution failedTask : failedTasks) {
                String capability = failedTask.getDefinition().getName();
                String errorNotice = generateErrorNotice(capability, failedTask.getResult());
                response.append("• ").append(errorNotice).append("\n");
                errors.add(errorNotice);
            }
            
            response.append("\nPlease try again later or rephrase your request.");
        }
        
        return new OrchestrationResult(
            session.getCorrelationId(),
            response.toString(),
            completedTasks,
            errors,
            true // isPartial
        );
    }
    
    /**
     * Generate emergency response when all strategies fail
     */
    public String generateEmergencyResponse(String failureType, String originalQuery) {
        String template = emergencyResponses.get(failureType);
        if (template == null) {
            template = emergencyResponses.get("general");
        }
        
        return template.replace("{query}", originalQuery)
                      .replace("{timestamp}", LocalDateTime.now().toString());
    }
    
    // Private helper methods
    
    private OrchestrationResult tryFallbackStrategies(OrchestrationSession session, 
                                                     FallbackContext context, 
                                                     Throwable error) {
        // Strategy 1: Simplified single-agent routing
        try {
            OrchestrationResult simplified = trySimplifiedRouting(session);
            if (simplified != null) {
                logMessage("Simplified routing succeeded for: " + session.getCorrelationId());
                return simplified;
            }
        } catch (Exception e) {
            logMessage("Simplified routing failed: " + e.getMessage());
        }
        
        // Strategy 2: LLM direct response
        try {
            OrchestrationResult llmDirect = tryDirectLLMResponse(session);
            if (llmDirect != null) {
                logMessage("Direct LLM response succeeded for: " + session.getCorrelationId());
                return llmDirect;
            }
        } catch (Exception e) {
            logMessage("Direct LLM response failed: " + e.getMessage());
        }
        
        // Strategy 3: Emergency response
        String emergencyResponse = generateEmergencyResponse("orchestration_failure", session.getOriginalQuery());
        return new OrchestrationResult(
            session.getCorrelationId(),
            emergencyResponse,
            new ArrayList<>(),
            Arrays.asList("Orchestration failed: " + error.getMessage()),
            true
        );
    }
    
    private OrchestrationResult trySimplifiedRouting(OrchestrationSession session) {
        // Route to single best agent based on query keywords
        String query = session.getNormalizedQuery();
        String targetAgent = determineBestAgent(query);
        
        if (targetAgent != null) {
            // Create simple task and execute
            TaskDefinition simpleTask = new TaskDefinition(
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
            
            // Execute task synchronously
            TaskResult result = executeSimpleTask(simpleTask, session.getCorrelationId());
            
            if (result != null && result.isSuccess()) {
                return new OrchestrationResult(
                    session.getCorrelationId(),
                    result.getFormattedResponse(),
                    Arrays.asList(result),
                    new ArrayList<>(),
                    false
                );
            }
        }
        
        return null;
    }
    
    private OrchestrationResult tryDirectLLMResponse(OrchestrationSession session) {
        try {
            String prompt = "Answer this question directly and helpfully: " + session.getOriginalQuery();
            
            // Mock LLM call - would integrate with actual connector
            String response = "I apologize, but I'm unable to process your request at the moment. " +
                            "Please try again later or contact support if the issue persists.";
            
            return new OrchestrationResult(
                session.getCorrelationId(),
                response,
                new ArrayList<>(),
                Arrays.asList("Used direct LLM fallback"),
                true
            );
            
        } catch (Exception e) {
            return null;
        }
    }
    
    private String createStricterPrompt(String originalPrompt, String malformedOutput, int attempt) {
        StringBuilder stricterPrompt = new StringBuilder();
        
        stricterPrompt.append("CRITICAL: You MUST respond with ONLY valid JSON. No explanations, no markdown, no additional text.\n\n");
        stricterPrompt.append("Previous attempt ").append(attempt).append(" failed with invalid output.\n");
        stricterPrompt.append("MALFORMED OUTPUT WAS: ").append(malformedOutput.substring(0, Math.min(100, malformedOutput.length()))).append("\n\n");
        stricterPrompt.append("RULES:\n");
        stricterPrompt.append("1. Response must start with { or [\n");
        stricterPrompt.append("2. Response must end with } or ]\n");
        stricterPrompt.append("3. All strings must be in double quotes\n");
        stricterPrompt.append("4. No trailing commas\n");
        stricterPrompt.append("5. No comments or explanations\n\n");
        stricterPrompt.append("ORIGINAL REQUEST:\n");
        stricterPrompt.append(originalPrompt);
        stricterPrompt.append("\n\nJSON RESPONSE:");
        
        return stricterPrompt.toString();
    }
    
    private String repromptLLM(String prompt, String correlationId, Object llmConnector) {
        // Mock implementation - would integrate with actual LLM connector
        return "{\"intent\": \"general\", \"targetAgent\": \"ChatAgent\", \"confidence\": 0.5}";
    }
    
    private boolean isValidResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = response.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
               (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }
    
    private void updateCircuitBreaker(String agentId, boolean success) {
        if (success) {
            // Reset failure count on success
            agentFailureCounts.put(agentId, new AtomicInteger(0));
            agentCircuitBreakers.remove(agentId);
        } else {
            // Increment failure count
            int failures = agentFailureCounts.computeIfAbsent(agentId, k -> new AtomicInteger(0))
                                           .incrementAndGet();
            
            if (failures >= CIRCUIT_BREAKER_THRESHOLD) {
                agentCircuitBreakers.put(agentId, new CircuitBreakerState(System.currentTimeMillis()));
                logMessage("Circuit breaker opened for agent: " + agentId);
            }
        }
    }
    
    private boolean isCircuitBreakerOpen(String agentId) {
        CircuitBreakerState state = agentCircuitBreakers.get(agentId);
        if (state == null) {
            return false;
        }
        
        if (System.currentTimeMillis() - state.getOpenedAt() > CIRCUIT_BREAKER_TIMEOUT_MS) {
            agentCircuitBreakers.remove(agentId);
            agentFailureCounts.put(agentId, new AtomicInteger(0));
            logMessage("Circuit breaker reset for agent: " + agentId);
            return false;
        }
        
        return true;
    }
    
    private CompletableFuture<TaskResult> routeToAlternateAgent(TaskExecution originalTask, 
                                                               Set<AgentInfo> availableAgents) {
        String capability = originalTask.getDefinition().getName();
        String originalAgent = originalTask.getDefinition().getAgentType();
        
        // Find alternate agent for same capability
        Optional<AgentInfo> alternateAgent = availableAgents.stream()
            .filter(agent -> !agent.getAgentId().equals(originalAgent))
            .filter(agent -> agent.getCapabilities().contains(capability) || 
                           agent.getCapabilities().contains("general.query"))
            .findFirst();
        
        if (alternateAgent.isPresent()) {
            TaskDefinition alternateTask = new TaskDefinition(
                UUID.randomUUID().toString(), // taskId
                capability, // name
                "Alternate task for: " + capability, // description
                alternateAgent.get().getAgentId(), // agentType
                originalTask.getDefinition().getParameters(), // parameters
                originalTask.getDefinition().getDependencies(), // dependencies
                originalTask.getDefinition().getPriority(), // priority
                originalTask.getDefinition().getTimeoutMs(), // timeoutMs
                originalTask.getDefinition().isOptional() // optional
            );
            
            logMessage("Routing to alternate agent: " + alternateAgent.get().getAgentId());
            return CompletableFuture.supplyAsync(() -> executeSimpleTask(alternateTask, originalTask.getCorrelationId()));
        }
        
        // No alternate found, return failure
        return CompletableFuture.completedFuture(
            new TaskResult(originalTask.getTaskId(), false, 
                         "No alternate agent available for capability: " + capability, null)
        );
    }
    
    private CompletableFuture<TaskResult> retryTaskWithAgent(TaskExecution taskExecution) {
        // Add delay before retry
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000); // 1 second delay
                return executeSimpleTask(taskExecution.getDefinition(), taskExecution.getCorrelationId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new TaskResult(taskExecution.getTaskId(), false, "Retry interrupted", null);
            }
        });
    }
    
    private TaskResult executeSimpleTask(TaskDefinition task, String correlationId) {
        // Mock implementation - would integrate with actual agent execution
        return new TaskResult(
            UUID.randomUUID().toString(),
            true,
            "Mock response for " + task.getName(),
            Map.of("agent", task.getAgentType(), "capability", task.getName())
        );
    }
    
    private String determineBestAgent(String query) {
        String lowercaseQuery = query.toLowerCase();
        
        if (lowercaseQuery.contains("weather")) return "WeatherAgent";
        if (lowercaseQuery.contains("stock")) return "StockPriceAgent";
        if (lowercaseQuery.contains("travel")) return "TravelPlannerAgent";
        
        return "ChatAgent"; // Default fallback
    }
    
    private String generateErrorNotice(String capability, TaskResult result) {
        switch (capability) {
            case "weather.get":
                return "Weather information is temporarily unavailable";
            case "stock.price":
                return "Stock price data is temporarily unavailable";
            case "travel.plan":
                return "Travel planning service is temporarily unavailable";
            default:
                return "Service temporarily unavailable: " + capability;
        }
    }
    
    private void initializeEmergencyResponses() {
        emergencyResponses.put("llm_failure", 
            "I'm having difficulty processing your request: '{query}'. " +
            "Please try rephrasing your question or contact support.");
        
        emergencyResponses.put("orchestration_failure",
            "I encountered an error while processing your request: '{query}'. " +
            "Please try again in a few moments.");
        
        emergencyResponses.put("agent_failure",
            "The requested service is temporarily unavailable. " +
            "Please try again later or contact support if the issue persists.");
        
        emergencyResponses.put("general",
            "I'm unable to process your request at the moment. " +
            "Please try again later or contact support for assistance.");
    }
    
    /**
     * Record failure for circuit breaker tracking
     */
    public void recordFailure(String agentId) {
        agentFailureCounts.computeIfAbsent(agentId, k -> new AtomicInteger(0)).incrementAndGet();
        
        int failures = agentFailureCounts.get(agentId).get();
        if (failures >= CIRCUIT_BREAKER_THRESHOLD) {
            agentCircuitBreakers.put(agentId, new CircuitBreakerState(System.currentTimeMillis()));
            logMessage("Circuit breaker OPEN for agent: " + agentId + " (failures: " + failures + ")");
        }
    }
    
    /**
     * Check if circuit breaker is open for an agent
     */
    public boolean isCircuitOpen(String agentId) {
        CircuitBreakerState state = agentCircuitBreakers.get(agentId);
        if (state == null) {
            return false;
        }
        
        // Check if timeout has passed
        long timeoutExpiry = state.getOpenedAt() + CIRCUIT_BREAKER_TIMEOUT_MS;
        if (System.currentTimeMillis() > timeoutExpiry) {
            // Reset circuit breaker
            agentCircuitBreakers.remove(agentId);
            agentFailureCounts.remove(agentId);
            logMessage("Circuit breaker RESET for agent: " + agentId);
            return false;
        }
        
        return true;
    }
    
    private void logMessage(String message) {
        System.out.println("[FallbackStrategyManager] " + message);
    }
    
    /**
     * Handle task failure with fallback strategies
     */
    public Map<String, Object> handleTaskFailure(TaskDefinition task, Exception error, Map<String, Object> context) {
        logMessage("Handling task failure for task: " + task.getTaskId() + " - " + error.getMessage());
        
        Map<String, Object> fallbackResult = new HashMap<>();
        fallbackResult.put("taskId", task.getTaskId());
        fallbackResult.put("status", "fallback");
        fallbackResult.put("errorMessage", error.getMessage());
        fallbackResult.put("timestamp", System.currentTimeMillis());
        
        return fallbackResult;
    }
    
    /**
     * Set prompt optimizer for enhanced fallback strategies
     */
    public void setPromptOptimizer(Object promptOptimizer) {
        logMessage("Setting prompt optimizer for enhanced fallback strategies");
        // In a real implementation, this would store the optimizer reference
        // For now, we just log the operation
    }
    
    // Helper classes
    
    private static class FallbackContext {
        private final String correlationId;
        private final String originalQuery;
        private final long createdAt;
        private int repromptAttempts = 0;
        private final Map<String, Integer> agentRetryCounts = new HashMap<>();
        
        public FallbackContext(String correlationId, String originalQuery) {
            this.correlationId = correlationId;
            this.originalQuery = originalQuery;
            this.createdAt = System.currentTimeMillis();
        }
        
        public String getCorrelationId() { return correlationId; }
        public String getOriginalQuery() { return originalQuery; }
        public long getCreatedAt() { return createdAt; }
        
        public int getRepromptAttempts() { return repromptAttempts; }
        public void incrementRepromptAttempts() { repromptAttempts++; }
        
        public int getAgentRetryCount(String agentId) {
            return agentRetryCounts.getOrDefault(agentId, 0);
        }
        
        public void incrementAgentRetry(String agentId) {
            agentRetryCounts.put(agentId, getAgentRetryCount(agentId) + 1);
        }
    }
    
    private static class CircuitBreakerState {
        private final long openedAt;
        
        public CircuitBreakerState(long openedAt) {
            this.openedAt = openedAt;
        }
        
        public long getOpenedAt() { return openedAt; }
    }
}

// Supporting classes that would be defined elsewhere

class OrchestrationSession {
    private final String correlationId;
    private final String originalQuery;
    private final String normalizedQuery;
    
    public OrchestrationSession(String correlationId, String originalQuery, String normalizedQuery) {
        this.correlationId = correlationId;
        this.originalQuery = originalQuery;
        this.normalizedQuery = normalizedQuery;
    }
    
    public String getCorrelationId() { return correlationId; }
    public String getOriginalQuery() { return originalQuery; }
    public String getNormalizedQuery() { return normalizedQuery; }
}

class OrchestrationResult {
    private final String correlationId;
    private final String response;
    private final List<TaskResult> completedTasks;
    private final List<String> errors;
    private final boolean isPartial;
    
    public OrchestrationResult(String correlationId, String response, List<TaskResult> completedTasks, 
                             List<String> errors, boolean isPartial) {
        this.correlationId = correlationId;
        this.response = response;
        this.completedTasks = new ArrayList<>(completedTasks);
        this.errors = new ArrayList<>(errors);
        this.isPartial = isPartial;
    }
    
    public String getCorrelationId() { return correlationId; }
    public String getResponse() { return response; }
    public List<TaskResult> getCompletedTasks() { return new ArrayList<>(completedTasks); }
    public List<String> getErrors() { return new ArrayList<>(errors); }
    public boolean isPartial() { return isPartial; }
    public boolean isSuccess() { return !response.contains("error") && !response.contains("unable"); }
}

class TaskExecution {
    private final TaskDefinition definition;
    private final String taskId;
    private final String correlationId;
    private TaskResult result;
    
    public TaskExecution(TaskDefinition definition, String correlationId) {
        this.definition = definition;
        this.taskId = UUID.randomUUID().toString();
        this.correlationId = correlationId;
    }
    
    public TaskDefinition getDefinition() { return definition; }
    public String getTaskId() { return taskId; }
    public String getCorrelationId() { return correlationId; }
    public TaskResult getResult() { return result; }
    public void setResult(TaskResult result) { this.result = result; }
}

class TaskResult {
    private final String taskId;
    private final boolean success;
    private final String response;
    private final Map<String, Object> data;
    
    public TaskResult(String taskId, boolean success, String response, Map<String, Object> data) {
        this.taskId = taskId;
        this.success = success;
        this.response = response;
        this.data = data != null ? new HashMap<>(data) : new HashMap<>();
    }
    
    public String getTaskId() { return taskId; }
    public boolean isSuccess() { return success; }
    public String getResponse() { return response; }
    public String getFormattedResponse() { return response; }
    public Map<String, Object> getData() { return new HashMap<>(data); }
}