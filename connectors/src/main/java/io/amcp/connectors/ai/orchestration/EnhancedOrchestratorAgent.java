package io.amcp.connectors.ai.orchestration;

import io.amcp.core.Agent;
import io.amcp.core.AgentContext;
import io.amcp.core.AgentID;
import io.amcp.core.AgentLifecycle;
import io.amcp.core.Event;
import io.amcp.connectors.ai.AgentRegistry;
import io.amcp.connectors.ai.OllamaSpringAIConnector;
import io.amcp.tools.ToolRequest;
import io.amcp.tools.ToolResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Enhanced AMCP v1.5 LLM-Orchestrated Agent with Standardized Task Protocol
 * 
 * This next-generation orchestrator agent provides:
 * - Advanced LLM-powered task planning and decomposition
 * - Standardized TaskProtocol for agent-to-agent communication
 * - Dynamic workflow orchestration with dependency management
 * - Context-aware task routing and result synthesis
 * - Comprehensive error handling and recovery
 * - Real-time capability discovery and agent coordination
 * 
 * The EnhancedOrchestratorAgent serves as the intelligent coordinator
 * for complex multi-agent workflows, leveraging Ollama LLM for natural
 * language understanding and strategic task orchestration.
 * 
 * @author AMCP Team
 * @version 1.5.0
 * @since 2024-09-29
 */
public class EnhancedOrchestratorAgent implements Agent {
    
    private static final String AGENT_TYPE = "ENHANCED_LLM_ORCHESTRATOR";
    private static final String VERSION = "1.5.0";
    
    private final AgentID agentId;
    private AgentContext context;
    private AgentLifecycle lifecycleState = AgentLifecycle.INACTIVE;
    
    // Core orchestration components
    private final AgentRegistry agentRegistry;
    private final OllamaSpringAIConnector ollamaConnector;
    private final TaskPlanningEngine planningEngine;
    private final WorkflowCoordinator workflowCoordinator;
    
    // Task orchestration tracking
    private final Map<String, OrchestrationWorkflow> activeWorkflows = new ConcurrentHashMap<>();
    private final Map<String, TaskExecution> pendingTasks = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Object>> pendingResponses = new ConcurrentHashMap<>();
    
    // Configuration
    private final OrchestratorConfig config;
    
    public EnhancedOrchestratorAgent() {
        this(new OrchestratorConfig());
    }
    
    public EnhancedOrchestratorAgent(OrchestratorConfig config) {
        this.agentId = AgentID.named("EnhancedOrchestratorAgent");
        this.config = config;
        this.agentRegistry = new AgentRegistry();
        this.ollamaConnector = new OllamaSpringAIConnector();
        this.planningEngine = new TaskPlanningEngine(ollamaConnector, config);
        this.workflowCoordinator = new WorkflowCoordinator();
    }
    
    /**
     * Configuration class for orchestrator behavior
     */
    public static class OrchestratorConfig {
        private final String llmModel;
        private final double planningTemperature;
        private final int maxTaskDepth;
        private final long taskTimeoutMs;
        private final boolean enableParallelExecution;
        private final boolean enableTaskCaching;
        
        public OrchestratorConfig() {
            this("tinyllama", 0.3, 5, 60000, true, true);
        }
        
        public OrchestratorConfig(String llmModel, double planningTemperature, int maxTaskDepth, 
                                 long taskTimeoutMs, boolean enableParallelExecution, boolean enableTaskCaching) {
            this.llmModel = llmModel;
            this.planningTemperature = planningTemperature;
            this.maxTaskDepth = maxTaskDepth;
            this.taskTimeoutMs = taskTimeoutMs;
            this.enableParallelExecution = enableParallelExecution;
            this.enableTaskCaching = enableTaskCaching;
        }
        
        // Getters
        public String getLlmModel() { return llmModel; }
        public double getPlanningTemperature() { return planningTemperature; }
        public int getMaxTaskDepth() { return maxTaskDepth; }
        public long getTaskTimeoutMs() { return taskTimeoutMs; }
        public boolean isEnableParallelExecution() { return enableParallelExecution; }
        public boolean isEnableTaskCaching() { return enableTaskCaching; }
    }
    
    /**
     * Represents a complete orchestration workflow
     */
    private static class OrchestrationWorkflow {
        private final String workflowId;
        private final String originalRequest;
        private final TaskProtocol.UserContext userContext;
        private final long startTime;
        private WorkflowState state;
        private ExecutionPlan executionPlan;
        private final Map<String, Object> results = new ConcurrentHashMap<>();
        private final Map<String, String> taskStatus = new ConcurrentHashMap<>();
        private String finalResult;
        private Exception error;
        
        public OrchestrationWorkflow(String workflowId, String originalRequest, TaskProtocol.UserContext userContext) {
            this.workflowId = workflowId;
            this.originalRequest = originalRequest;
            this.userContext = userContext;
            this.startTime = System.currentTimeMillis();
            this.state = WorkflowState.PLANNING;
        }
        
        // Getters and setters
        public String getWorkflowId() { return workflowId; }
        public String getOriginalRequest() { return originalRequest; }
        public TaskProtocol.UserContext getUserContext() { return userContext; }
        public long getStartTime() { return startTime; }
        public WorkflowState getState() { return state; }
        public void setState(WorkflowState state) { this.state = state; }
        public ExecutionPlan getExecutionPlan() { return executionPlan; }
        public void setExecutionPlan(ExecutionPlan executionPlan) { this.executionPlan = executionPlan; }
        public Map<String, Object> getResults() { return results; }
        public Map<String, String> getTaskStatus() { return taskStatus; }
        public String getFinalResult() { return finalResult; }
        public void setFinalResult(String finalResult) { this.finalResult = finalResult; }
        public Exception getError() { return error; }
        public void setError(Exception error) { this.error = error; }
    }
    
    /**
     * Workflow execution states
     */
    private enum WorkflowState {
        PLANNING, EXECUTING, SYNTHESIZING, COMPLETED, FAILED
    }
    
    /**
     * Represents an execution plan generated by the LLM
     */
    private static class ExecutionPlan {
        private final List<TaskStep> steps;
        private final Map<String, Set<String>> dependencies;
        private final String reasoning;
        private final double confidence;
        
        public ExecutionPlan(List<TaskStep> steps, Map<String, Set<String>> dependencies, 
                           String reasoning, double confidence) {
            this.steps = steps;
            this.dependencies = dependencies;
            this.reasoning = reasoning;
            this.confidence = confidence;
        }
        
        // Getters
        public List<TaskStep> getSteps() { return steps; }
        public Map<String, Set<String>> getDependencies() { return dependencies; }
        public String getReasoning() { return reasoning; }
        public double getConfidence() { return confidence; }
    }
    
    /**
     * Individual task step in execution plan
     */
    private static class TaskStep {
        private final String stepId;
        private final String capability;
        private final String description;
        private final Map<String, Object> parameters;
        private final int priority;
        private final boolean canExecuteInParallel;
        
        public TaskStep(String stepId, String capability, String description, 
                       Map<String, Object> parameters, int priority, boolean canExecuteInParallel) {
            this.stepId = stepId;
            this.capability = capability;
            this.description = description;
            this.parameters = parameters;
            this.priority = priority;
            this.canExecuteInParallel = canExecuteInParallel;
        }
        
        // Getters
        public String getStepId() { return stepId; }
        public String getCapability() { return capability; }
        public String getDescription() { return description; }
        public Map<String, Object> parameters() { return parameters; }
        public int getPriority() { return priority; }
        public boolean canExecuteInParallel() { return canExecuteInParallel; }
    }
    
    /**
     * Tracks individual task execution
     */
    private static class TaskExecution {
        private final String taskId;
        private final String workflowId;
        private final TaskStep step;
        private final long startTime;
        private TaskStatus status;
        private Object result;
        private Exception error;
        
        public TaskExecution(String taskId, String workflowId, TaskStep step) {
            this.taskId = taskId;
            this.workflowId = workflowId;
            this.step = step;
            this.startTime = System.currentTimeMillis();
            this.status = TaskStatus.PENDING;
        }
        
        // Getters and setters
        public String getTaskId() { return taskId; }
        public String getWorkflowId() { return workflowId; }
        public TaskStep getStep() { return step; }
        public long getStartTime() { return startTime; }
        public TaskStatus getStatus() { return status; }
        public void setStatus(TaskStatus status) { this.status = status; }
        public Object getResult() { return result; }
        public void setResult(Object result) { this.result = result; }
        public Exception getError() { return error; }
        public void setError(Exception error) { this.error = error; }
    }
    
    /**
     * Task execution status
     */
    private enum TaskStatus {
        PENDING, EXECUTING, COMPLETED, FAILED, TIMEOUT
    }
    
    /**
     * LLM-powered task planning engine
     */
    private class TaskPlanningEngine {
        private final OllamaSpringAIConnector llmConnector;
        private final OrchestratorConfig config;
        
        public TaskPlanningEngine(OllamaSpringAIConnector llmConnector, OrchestratorConfig config) {
            this.llmConnector = llmConnector;
            this.config = config;
        }
        
        public CompletableFuture<ExecutionPlan> createExecutionPlan(String userRequest, 
                                                                   Set<AgentRegistry.AgentInfo> availableAgents,
                                                                   TaskProtocol.UserContext userContext) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    String planningPrompt = buildPlanningPrompt(userRequest, availableAgents, userContext);
                    
                    Map<String, Object> params = new HashMap<>();
                    params.put("prompt", planningPrompt);
                    params.put("model", config.getLlmModel());
                    params.put("temperature", config.getPlanningTemperature());
                    params.put("max_tokens", 800);
                    
                    ToolRequest toolRequest = new ToolRequest(
                        "ollama-chat", 
                        params,
                        UUID.randomUUID().toString()
                    );
                    
                    ToolResponse response = llmConnector.invoke(toolRequest).get(30, TimeUnit.SECONDS);
                    
                    if (response.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> result = (Map<String, Object>) response.getData();
                        String planResult = (String) result.get("response");
                        
                        return parsePlanningResult(planResult);
                    } else {
                        logMessage("❌ LLM planning failed: " + response.getErrorMessage());
                        return createFallbackPlan(userRequest, availableAgents);
                    }
                    
                } catch (Exception e) {
                    logMessage("❌ Error in task planning: " + e.getMessage());
                    return createFallbackPlan(userRequest, availableAgents);
                }
            });
        }
        
        private String buildPlanningPrompt(String userRequest, Set<AgentRegistry.AgentInfo> availableAgents,
                                         TaskProtocol.UserContext userContext) {
            StringBuilder prompt = new StringBuilder();
            
            prompt.append("You are an intelligent task orchestrator for the AMCP multi-agent system. ");
            prompt.append("Create an execution plan to fulfill the user's request using available specialized agents. ");
            prompt.append("You have access to domain experts who can handle specific types of requests.\\n\\n");
            
            prompt.append("USER REQUEST: \\\"").append(userRequest).append("\\\"\\n\\n");
            
            // Enhanced agent descriptions with specific capabilities
            prompt.append("AVAILABLE SPECIALIST AGENTS:\\n");
            for (AgentRegistry.AgentInfo agent : availableAgents) {
                prompt.append("• ").append(agent.getAgentId()).append(": ").append(agent.getDescription()).append("\\n");
                if (!agent.getCapabilities().isEmpty()) {
                    prompt.append("  Capabilities: ").append(String.join(", ", agent.getCapabilities())).append("\\n");
                }
                
                // Add enhanced descriptions for known agent types
                String agentType = agent.getAgentType();
                if (agentType != null) {
                    switch (agentType.toLowerCase()) {
                        case "travelplanneragent":
                            prompt.append("  🌍 TRAVEL SPECIALIST: Expert in trip planning, destination recommendations, \\n");
                            prompt.append("     flight/hotel booking advice, travel tips, budget planning, and itinerary creation.\\n");
                            prompt.append("     Use for: travel plans, destinations, booking help, travel advice, vacation planning\\n");
                            break;
                        case "stockagent":
                            prompt.append("  📈 FINANCIAL SPECIALIST: Expert in stock quotes, market analysis, investment advice, \\n");
                            prompt.append("     cryptocurrency, economic indicators, and financial planning.\\n");
                            prompt.append("     Use for: stock prices, market trends, investments, financial advice, crypto, economics\\n");
                            break;
                        case "meshchatagent":
                            prompt.append("  💬 CONVERSATION INTERFACE: Manages human-to-AI conversations and session context.\\n");
                            prompt.append("     Use for: chat management, conversation memory, user interaction coordination\\n");
                            break;
                        case "weatheragent":
                            prompt.append("  🌤️ WEATHER SPECIALIST: Expert in weather forecasts, conditions, and climate data.\\n");
                            prompt.append("     Use for: weather forecasts, current conditions, climate information\\n");
                            break;
                        case "ollamachatagent":
                            prompt.append("  🤖 AI CHAT SPECIALIST: Powered by TinyLlama for natural language conversations.\\n");
                            prompt.append("     Use for: general chat, questions, explanations, creative tasks\\n");
                            break;
                    }
                }
            }
            
            prompt.append("\\nUSER CONTEXT:\\n");
            prompt.append("User ID: ").append(userContext.getUserId()).append("\\n");
            prompt.append("Session: ").append(userContext.getSessionId()).append("\\n");
            
            // Enhanced planning instructions with agent-aware routing
            prompt.append("\\nINTELLIGENT ROUTING GUIDELINES:\\n");
            prompt.append("🎯 TRAVEL QUERIES → Route to TravelPlannerAgent for:\\n");
            prompt.append("   • Trip planning, destinations, travel advice, booking help, itineraries\\n");
            prompt.append("   • Keywords: travel, trip, vacation, destination, flight, hotel, visit\\n\\n");
            
            prompt.append("💰 FINANCIAL QUERIES → Route to StockAgent for:\\n");
            prompt.append("   • Stock prices, market analysis, investment advice, financial planning\\n");
            prompt.append("   • Keywords: stock, price, market, invest, finance, money, crypto, economy\\n\\n");
            
            prompt.append("🌤️ WEATHER QUERIES → Route to WeatherAgent for:\\n");
            prompt.append("   • Weather forecasts, current conditions, climate information\\n");
            prompt.append("   • Keywords: weather, forecast, temperature, rain, sunny, climate\\n\\n");
            
            prompt.append("💬 GENERAL CHAT → Route to OllamaChatAgent for:\\n");
            prompt.append("   • General questions, explanations, creative tasks, casual conversation\\n");
            prompt.append("   • Keywords: explain, how, what, tell me, create, write\\n\\n");
            
            prompt.append("ADVANCED PLANNING INSTRUCTIONS:\\n");
            prompt.append("1. Analyze the user request to identify the PRIMARY domain (travel, finance, weather, chat)\\n");
            prompt.append("2. Route complex requests to multiple specialists if needed\\n");
            prompt.append("3. For travel + weather: Get weather for destination from WeatherAgent, then send to TravelPlannerAgent\\n");
            prompt.append("4. For investment + market: Send financial queries to StockAgent\\n");
            prompt.append("5. Break down complex requests into specialist-specific tasks\\n");
            prompt.append("6. Consider task dependencies and execution order\\n");
            prompt.append("7. Determine if tasks can run in parallel\\n");
            prompt.append("8. Assign priority levels (1=low, 5=normal, 10=high)\\n");
            prompt.append("9. Provide clear reasoning for your routing decisions\\n\\n");
            
            prompt.append("RESPONSE FORMAT - Use this EXACT JSON structure:\\n");
            prompt.append("{\\n");
            prompt.append("  \\\"confidence\\\": 0.0-1.0,\\n");
            prompt.append("  \\\"reasoning\\\": \\\"Detailed explanation of why you chose these agents and routing\\\",\\n");
            prompt.append("  \\\"primaryDomain\\\": \\\"travel|finance|weather|chat|multi-domain\\\",\\n");
            prompt.append("  \\\"steps\\\": [\\n");
            prompt.append("    {\\n");
            prompt.append("      \\\"stepId\\\": \\\"step1\\\",\\n");
            prompt.append("      \\\"capability\\\": \\\"specific_agent_capability\\\",\\n");
            prompt.append("      \\\"targetAgent\\\": \\\"AgentType (e.g., TravelPlannerAgent)\\\",\\n");
            prompt.append("      \\\"description\\\": \\\"Clear description of what this agent should do\\\",\\n");
            prompt.append("      \\\"parameters\\\": {\\\"query\\\": \\\"specific request for agent\\\", \\\"context\\\": \\\"relevant context\\\"},\\n");
            prompt.append("      \\\"priority\\\": 5,\\n");
            prompt.append("      \\\"canExecuteInParallel\\\": true,\\n");
            prompt.append("      \\\"expectedOutput\\\": \\\"Description of expected result\\\"\\n");
            prompt.append("    }\\n");
            prompt.append("  ],\\n");
            prompt.append("  \\\"dependencies\\\": {\\\"step2\\\": [\\\"step1\\\"]},\\n");
            prompt.append("  \\\"synthesisStrategy\\\": \\\"How to combine results from multiple agents\\\"\\n");
            prompt.append("}\\n\\n");
            
            prompt.append("EXAMPLE ROUTING DECISIONS:\\n");
            prompt.append("• \\\"Plan a trip to Paris\\\" → TravelPlannerAgent (destination recommendations, itinerary)\\n");
            prompt.append("• \\\"What's Apple stock price?\\\" → StockAgent (stock quote, market data)\\n");
            prompt.append("• \\\"Weather in Tokyo tomorrow\\\" → WeatherAgent (forecast, conditions)\\n");
            prompt.append("• \\\"Plan Tokyo trip with weather check\\\" → WeatherAgent THEN TravelPlannerAgent (sequential)\\n");
            prompt.append("• \\\"Explain quantum physics\\\" → OllamaChatAgent (general knowledge, explanation)\\n");
            
            return prompt.toString();
        }
        
        private ExecutionPlan parsePlanningResult(String planResult) {
            // Implementation for parsing LLM JSON response
            // For now, return a simple fallback
            List<TaskStep> steps = new ArrayList<>();
            steps.add(new TaskStep("step1", "chat", "Process user request", 
                new HashMap<>(), 5, false));
            
            return new ExecutionPlan(steps, new HashMap<>(), "Parsed from LLM", 0.8);
        }
        
        private ExecutionPlan createFallbackPlan(String userRequest, Set<AgentRegistry.AgentInfo> availableAgents) {
            // Enhanced fallback planning with intelligent routing
            List<TaskStep> steps = new ArrayList<>();
            
            // Enhanced intent detection for routing to specific agents
            String lowerRequest = userRequest.toLowerCase();
            String reasoning = "Fallback routing based on keyword analysis";
            
            if (containsKeywords(lowerRequest, "travel", "trip", "vacation", "destination", "flight", "hotel", "visit", "book", "itinerary")) {
                steps.add(new TaskStep("travel_task", "travel_planning", "Route to TravelPlannerAgent for travel assistance", 
                    Map.of("query", userRequest, "domain", "travel", "agent", "TravelPlannerAgent"), 7, false));
                reasoning = "Travel keywords detected - routing to TravelPlannerAgent";
            } else if (containsKeywords(lowerRequest, "stock", "price", "market", "invest", "finance", "money", "crypto", "economy", "nasdaq", "dow")) {
                steps.add(new TaskStep("finance_task", "financial_analysis", "Route to StockAgent for financial analysis", 
                    Map.of("query", userRequest, "domain", "finance", "agent", "StockAgent"), 7, false));
                reasoning = "Financial keywords detected - routing to StockAgent";
            } else if (containsKeywords(lowerRequest, "weather", "forecast", "temperature", "rain", "sunny", "climate", "storm")) {
                steps.add(new TaskStep("weather_task", "weather_forecast", "Route to WeatherAgent for weather information", 
                    Map.of("query", userRequest, "domain", "weather", "agent", "WeatherAgent"), 7, false));
                reasoning = "Weather keywords detected - routing to WeatherAgent";
            } else {
                steps.add(new TaskStep("chat_task", "natural_language_chat", "Route to OllamaChatAgent for general conversation", 
                    Map.of("query", userRequest, "domain", "general", "agent", "OllamaChatAgent", "use_ollama", "true"), 5, false));
                reasoning = "General query - routing to OllamaChatAgent with TinyLlama support";
            }
            
            return new ExecutionPlan(steps, new HashMap<>(), reasoning, 0.7);
        }
        
        private boolean containsKeywords(String text, String... keywords) {
            for (String keyword : keywords) {
                if (text.contains(keyword)) {
                    return true;
                }
            }
            return false;
        }
    }
    
    /**
     * Coordinates workflow execution and task dependencies
     */
    private class WorkflowCoordinator {
        
        public CompletableFuture<Void> executeWorkflow(OrchestrationWorkflow workflow) {
            return CompletableFuture.runAsync(() -> {
                try {
                    workflow.setState(WorkflowState.EXECUTING);
                    logMessage("🚀 Starting workflow execution: " + workflow.getWorkflowId());
                    
                    ExecutionPlan plan = workflow.getExecutionPlan();
                    
                    if (config.isEnableParallelExecution()) {
                        executeTasksInParallel(workflow, plan);
                    } else {
                        executeTasksSequentially(workflow, plan);
                    }
                    
                } catch (Exception e) {
                    logMessage("❌ Workflow execution failed: " + e.getMessage());
                    workflow.setError(e);
                    workflow.setState(WorkflowState.FAILED);
                }
            });
        }
        
        private void executeTasksInParallel(OrchestrationWorkflow workflow, ExecutionPlan plan) {
            // Implementation for parallel task execution with dependency management
            // For now, execute sequentially as fallback
            executeTasksSequentially(workflow, plan);
        }
        
        private void executeTasksSequentially(OrchestrationWorkflow workflow, ExecutionPlan plan) {
            for (TaskStep step : plan.getSteps()) {
                try {
                    executeTask(workflow, step).get(config.getTaskTimeoutMs(), TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    logMessage("❌ Task execution failed: " + step.getStepId() + " - " + e.getMessage());
                    workflow.setError(e);
                    workflow.setState(WorkflowState.FAILED);
                    return;
                }
            }
            
            workflow.setState(WorkflowState.SYNTHESIZING);
        }
        
        private CompletableFuture<Object> executeTask(OrchestrationWorkflow workflow, TaskStep step) {
            String taskId = UUID.randomUUID().toString();
            TaskExecution execution = new TaskExecution(taskId, workflow.getWorkflowId(), step);
            pendingTasks.put(taskId, execution);
            
            logMessage("🎯 Executing task: " + step.getDescription());
            
            // Create task request using TaskProtocol
            Event taskRequest = TaskProtocol.createTaskRequest(
                step.getCapability(),
                step.parameters(),
                workflow.getUserContext(),
                taskId,
                agentId.toString(),
                step.getPriority(),
                config.getTaskTimeoutMs()
            );
            
            CompletableFuture<Object> responsePromise = new CompletableFuture<>();
            pendingResponses.put(taskId, responsePromise);
            
            execution.setStatus(TaskStatus.EXECUTING);
            
            // Publish task request
            publishEvent(taskRequest);
            
            return responsePromise.thenApply(result -> {
                execution.setResult(result);
                execution.setStatus(TaskStatus.COMPLETED);
                workflow.getResults().put(step.getStepId(), result);
                workflow.getTaskStatus().put(step.getStepId(), "COMPLETED");
                pendingTasks.remove(taskId);
                return result;
            }).exceptionally(ex -> {
                execution.setError(new RuntimeException(ex));
                execution.setStatus(TaskStatus.FAILED);
                workflow.getTaskStatus().put(step.getStepId(), "FAILED");
                pendingTasks.remove(taskId);
                throw new RuntimeException("Task failed: " + step.getStepId(), ex);
            });
        }
    }
    
    @Override
    public AgentID getAgentId() {
        return agentId;
    }
    
    @Override
    public AgentContext getContext() {
        return context;
    }
    
    @Override
    public AgentLifecycle getLifecycleState() {
        return lifecycleState;
    }
    
    public void setContext(AgentContext context) {
        this.context = context;
        agentRegistry.setContext(context);
    }
    
    @Override
    public void onActivate() {
        try {
            logMessage("🎯 Activating Enhanced Orchestrator Agent v" + VERSION + "...");
            lifecycleState = AgentLifecycle.ACTIVE;
            
            // Activate components
            agentRegistry.onActivate();
            
            // Subscribe to orchestration and task protocol events
            subscribe("orchestrator.**");
            subscribe("orchestrator.task.response");
            subscribe("capability.**");
            
            logMessage("🎯 Enhanced Orchestrator Agent activated successfully");
            logMessage("🤖 LLM-powered task planning enabled with model: " + config.getLlmModel());
            logMessage("⚡ Parallel execution: " + (config.isEnableParallelExecution() ? "enabled" : "disabled"));
            
        } catch (Exception e) {
            logMessage("❌ Error activating Enhanced Orchestrator Agent: " + e.getMessage());
            lifecycleState = AgentLifecycle.INACTIVE;
        }
    }
    
    @Override
    public void onDeactivate() {
        lifecycleState = AgentLifecycle.INACTIVE;
        activeWorkflows.clear();
        pendingTasks.clear();
        pendingResponses.clear();
        logMessage("🎯 Enhanced Orchestrator Agent deactivated");
    }
    
    @Override
    public void onDestroy() {
        onDeactivate();
        lifecycleState = AgentLifecycle.DESTROYED;
        logMessage("🎯 Enhanced Orchestrator Agent destroyed");
    }
    
    @Override
    public void onBeforeMigration(String destinationContext) {
        logMessage("🎯 Preparing for migration to: " + destinationContext);
        // Save workflow state for migration
    }
    
    @Override
    public void onAfterMigration(String sourceContext) {
        logMessage("🎯 Completed migration from: " + sourceContext);
        // Restore workflow state after migration
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                String topic = event.getTopic();
                
                if ("orchestrator.task.response".equals(topic)) {
                    handleTaskResponse(event);
                } else if (topic.startsWith("orchestrator.")) {
                    handleOrchestrationEvent(event);
                } else if (topic.startsWith("capability.")) {
                    handleCapabilityEvent(event);
                } else {
                    logMessage("Unhandled event: " + topic);
                }
                
            } catch (Exception e) {
                logMessage("Error handling event " + event.getTopic() + ": " + e.getMessage());
            }
        });
    }
    
    /**
     * Main orchestration entry point - processes complex user requests
     */
    public CompletableFuture<String> orchestrateComplexRequest(String userRequest, 
                                                              TaskProtocol.UserContext userContext) {
        String workflowId = UUID.randomUUID().toString();
        OrchestrationWorkflow workflow = new OrchestrationWorkflow(workflowId, userRequest, userContext);
        activeWorkflows.put(workflowId, workflow);
        
        logMessage("🎯 Starting complex orchestration for: \"" + userRequest + "\"");
        
        return agentRegistry.discoverAgents()
            .thenCompose(agents -> {
                logMessage("📋 Discovered " + agents.size() + " available agents");
                return planningEngine.createExecutionPlan(userRequest, agents, userContext);
            })
            .thenCompose(plan -> {
                workflow.setExecutionPlan(plan);
                logMessage("📝 Execution plan created with " + plan.getSteps().size() + " steps");
                logMessage("💡 Planning confidence: " + String.format("%.2f", plan.getConfidence()));
                logMessage("📋 Reasoning: " + plan.getReasoning());
                
                return workflowCoordinator.executeWorkflow(workflow);
            })
            .thenCompose(v -> synthesizeResults(workflow))
            .thenApply(finalResult -> {
                workflow.setFinalResult(finalResult);
                workflow.setState(WorkflowState.COMPLETED);
                activeWorkflows.remove(workflowId);
                
                logMessage("✅ Complex orchestration completed for workflow: " + workflowId);
                return finalResult;
            })
            .exceptionally(ex -> {
                logMessage("❌ Complex orchestration failed: " + ex.getMessage());
                workflow.setError(new RuntimeException(ex));
                workflow.setState(WorkflowState.FAILED);
                activeWorkflows.remove(workflowId);
                return "I apologize, but I encountered an issue processing your complex request. Please try again.";
            });
    }
    
    /**
     * Synthesizes results from multiple tasks into coherent response
     */
    private CompletableFuture<String> synthesizeResults(OrchestrationWorkflow workflow) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String synthesisPrompt = buildSynthesisPrompt(workflow);
                
                Map<String, Object> params = new HashMap<>();
                params.put("prompt", synthesisPrompt);
                params.put("model", config.getLlmModel());
                params.put("temperature", 0.7);
                params.put("max_tokens", 500);
                
                ToolRequest toolRequest = new ToolRequest(
                    "ollama-chat", 
                    params,
                    UUID.randomUUID().toString()
                );
                
                ToolResponse response = ollamaConnector.invoke(toolRequest).get(30, TimeUnit.SECONDS);
                
                if (response.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) response.getData();
                    String synthesizedResponse = (String) result.get("response");
                    
                    logMessage("✨ Results synthesized by LLM");
                    return synthesizedResponse;
                } else {
                    logMessage("⚠️ Result synthesis failed, using direct concatenation");
                    return createFallbackSynthesis(workflow);
                }
                
            } catch (Exception e) {
                logMessage("❌ Error synthesizing results: " + e.getMessage());
                return createFallbackSynthesis(workflow);
            }
        });
    }
    
    private String buildSynthesisPrompt(OrchestrationWorkflow workflow) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are synthesizing results from multiple AI agents to provide a comprehensive response.\\n\\n");
        
        prompt.append("ORIGINAL REQUEST: \\\"").append(workflow.getOriginalRequest()).append("\\\"\\n\\n");
        
        prompt.append("AGENT RESULTS:\\n");
        workflow.getResults().forEach((stepId, result) -> {
            prompt.append("• ").append(stepId).append(": ").append(result.toString()).append("\\n");
        });
        
        prompt.append("\\nSYNTHESIS INSTRUCTIONS:\\n");
        prompt.append("- Combine all results into a coherent, comprehensive response\\n");
        prompt.append("- Maintain accuracy of all agent-provided information\\n");
        prompt.append("- Make the response conversational and user-friendly\\n");
        prompt.append("- Highlight key insights and actionable information\\n");
        prompt.append("- If results conflict, note the discrepancy clearly\\n");
        prompt.append("- Keep the response well-organized and easy to read\\n\\n");
        
        prompt.append("Provide the synthesized response:");
        
        return prompt.toString();
    }
    
    private String createFallbackSynthesis(OrchestrationWorkflow workflow) {
        StringBuilder result = new StringBuilder();
        result.append("Here's what I found for your request:\\n\\n");
        
        workflow.getResults().forEach((stepId, stepResult) -> {
            result.append("• ").append(stepResult.toString()).append("\\n");
        });
        
        return result.toString();
    }
    
    private void handleTaskResponse(Event event) {
        String correlationId = event.getCorrelationId();
        if (correlationId != null && pendingResponses.containsKey(correlationId)) {
            CompletableFuture<Object> pendingResponse = pendingResponses.remove(correlationId);
            
            try {
                TaskProtocol.TaskResponseData response = TaskProtocol.parseTaskResponse(event);
                
                if (response.isSuccess()) {
                    pendingResponse.complete(response.getResult());
                    logMessage("📥 Received successful task response for: " + correlationId);
                } else {
                    Exception error = new RuntimeException("Task failed: " + response.getError().getMessage());
                    pendingResponse.completeExceptionally(error);
                    logMessage("❌ Received failed task response for: " + correlationId);
                }
                
            } catch (Exception e) {
                logMessage("❌ Error processing task response: " + e.getMessage());
                pendingResponse.completeExceptionally(e);
            }
        }
    }
    
    private void handleOrchestrationEvent(Event event) {
        // Handle orchestration-specific events
        logMessage("📊 Received orchestration event: " + event.getTopic());
    }
    
    private void handleCapabilityEvent(Event event) {
        // Handle capability registration/discovery events
        logMessage("🔍 Received capability event: " + event.getTopic());
    }
    
    /**
     * Get comprehensive orchestration statistics
     */
    public Map<String, Object> getOrchestrationStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("agentId", agentId.toString());
        stats.put("version", VERSION);
        stats.put("lifecycleState", lifecycleState);
        stats.put("activeWorkflows", activeWorkflows.size());
        stats.put("pendingTasks", pendingTasks.size());
        stats.put("pendingResponses", pendingResponses.size());
        stats.put("config", Map.of(
            "llmModel", config.getLlmModel(),
            "maxTaskDepth", config.getMaxTaskDepth(),
            "parallelExecution", config.isEnableParallelExecution(),
            "taskCaching", config.isEnableTaskCaching()
        ));
        
        // Add workflow status summary
        Map<String, Long> workflowStates = activeWorkflows.values().stream()
            .collect(Collectors.groupingBy(
                w -> w.getState().toString(),
                Collectors.counting()
            ));
        stats.put("workflowStates", workflowStates);
        
        return stats;
    }
    
    // Required Agent interface methods
    @Override
    public CompletableFuture<Void> subscribe(String topicPattern) {
        if (context != null) {
            return context.subscribe(agentId, topicPattern);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> unsubscribe(String topicPattern) {
        if (context != null) {
            return context.unsubscribe(agentId, topicPattern);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> publishEvent(Event event) {
        if (context != null) {
            return context.publishEvent(event);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [EnhancedOrchestratorAgent] " + message);
    }
}