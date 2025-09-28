package io.amcp.connectors.ai;

import io.amcp.core.Agent;
import io.amcp.core.AgentContext;
import io.amcp.core.AgentID;
import io.amcp.core.AgentLifecycle;
import io.amcp.core.Event;
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
 * AMCP v1.5 Orchestrator Agent with TinyLlama Intelligence
 * 
 * This intelligent middleware agent provides:
 * - Natural language intent analysis using TinyLlama via OLLAMA
 * - Dynamic agent discovery and routing via AgentRegistry
 * - Request interception and intelligent delegation
 * - Response synthesis and formatting coordination
 * - End-to-end conversation orchestration
 * 
 * The OrchestratorAgent acts as the central nervous system of the AMCP
 * multi-agent ecosystem, ensuring optimal routing and response quality.
 */
public class OrchestratorAgent implements Agent {
    
    private static final String AGENT_TYPE = "INTELLIGENT_ORCHESTRATOR";
    private static final String VERSION = "1.5.0";
    
    private final AgentID agentId;
    private AgentContext context;
    private AgentLifecycle lifecycleState = AgentLifecycle.INACTIVE;
    
    // Core orchestration components
    private final AgentRegistry agentRegistry;
    private final OllamaSpringAIConnector ollamaConnector;
    private final IntentAnalyzer intentAnalyzer;
    
    // Request orchestration tracking
    private final Map<String, OrchestrationSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<String>> pendingResponses = new ConcurrentHashMap<>();
    
    public OrchestratorAgent() {
        this.agentId = AgentID.named("OrchestratorAgent");
        this.agentRegistry = new AgentRegistry();
        this.ollamaConnector = new OllamaSpringAIConnector();
        this.intentAnalyzer = new IntentAnalyzer();
    }
    
    /**
     * Orchestration session tracking user requests through the agent ecosystem
     */
    private static class OrchestrationSession {
        private final String sessionId;
        private final String originalQuery;
        private final long startTime;
        private String detectedIntent;
        private String targetAgent;
        private String agentResponse;
        private String finalResponse;
        private final Map<String, Object> metadata = new HashMap<>();
        
        public OrchestrationSession(String sessionId, String originalQuery) {
            this.sessionId = sessionId;
            this.originalQuery = originalQuery;
            this.startTime = System.currentTimeMillis();
        }
        
        // Getters and setters
        public String getSessionId() { return sessionId; }
        public String getOriginalQuery() { return originalQuery; }
        public long getStartTime() { return startTime; }
        public String getDetectedIntent() { return detectedIntent; }
        public void setDetectedIntent(String detectedIntent) { this.detectedIntent = detectedIntent; }
        public String getTargetAgent() { return targetAgent; }
        public void setTargetAgent(String targetAgent) { this.targetAgent = targetAgent; }
        public String getAgentResponse() { return agentResponse; }
        public void setAgentResponse(String agentResponse) { this.agentResponse = agentResponse; }
        public String getFinalResponse() { return finalResponse; }
        public void setFinalResponse(String finalResponse) { this.finalResponse = finalResponse; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
    
    /**
     * Intent analysis using TinyLlama for intelligent agent routing
     */
    private class IntentAnalyzer {
        
        public CompletableFuture<IntentAnalysis> analyzeIntent(String userQuery, 
                                                              Set<AgentRegistry.AgentInfo> availableAgents) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    List<AgentRegistry.AgentInfo> agentList = new ArrayList<>(availableAgents);
                    String analysisPrompt = buildIntentAnalysisPrompt(userQuery, agentList);
                    
                    // Call TinyLlama via OLLAMA for intent analysis
                    Map<String, Object> params = new HashMap<>();
                    params.put("prompt", analysisPrompt);
                    params.put("model", "tinyllama");
                    params.put("temperature", 0.3); // Lower temperature for more consistent routing
                    params.put("max_tokens", 200);
                    
                    ToolRequest toolRequest = new ToolRequest(
                        "ollama-chat", 
                        params,
                        UUID.randomUUID().toString()
                    );
                    
                    ToolResponse response = ollamaConnector.invoke(toolRequest).get(15, TimeUnit.SECONDS);
                    
                    if (response.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> result = (Map<String, Object>) response.getData();
                        String analysisResult = (String) result.get("response");
                        
                        return parseIntentAnalysis(analysisResult, userQuery);
                    } else {
                        logMessage("TinyLlama intent analysis failed: " + response.getErrorMessage());
                        return createFallbackIntent(userQuery, agentList);
                    }
                    
                } catch (Exception e) {
                    logMessage("Error in intent analysis: " + e.getMessage());
                    return createFallbackIntent(userQuery, new ArrayList<>(availableAgents));
                }
            });
        }
        
        private String buildIntentAnalysisPrompt(String userQuery, List<AgentRegistry.AgentInfo> agents) {
            StringBuilder prompt = new StringBuilder();
            
            prompt.append("You are TinyLlama, an intelligent agent orchestrator in the AMCP system. ");
            prompt.append("Analyze the user query and determine which specialized agent should handle it.\n\n");
            
            prompt.append("AVAILABLE AGENTS:\n");
            for (AgentRegistry.AgentInfo agent : agents) {
                prompt.append("• ").append(agent.getAgentId()).append(": ").append(agent.getDescription()).append("\n");
                if (!agent.getCapabilities().isEmpty()) {
                    prompt.append("  Capabilities: ").append(String.join(", ", agent.getCapabilities())).append("\n");
                }
            }
            
            prompt.append("\nUSER QUERY: \"").append(userQuery).append("\"\n\n");
            
            prompt.append("ANALYSIS INSTRUCTIONS:\n");
            prompt.append("1. Identify the main intent (weather, stock, travel, general chat)\n");
            prompt.append("2. Extract key parameters (location, stock symbol, destination, etc.)\n");
            prompt.append("3. Determine the best agent to handle this request\n");
            prompt.append("4. Provide confidence level (0.0-1.0)\n\n");
            
            prompt.append("Respond in this EXACT format:\n");
            prompt.append("INTENT: [weather|stock|travel|chat]\n");
            prompt.append("AGENT: [exact agent name from list above]\n");
            prompt.append("CONFIDENCE: [0.0-1.0]\n");
            prompt.append("PARAMETERS: key1=value1,key2=value2\n");
            prompt.append("REASONING: [brief explanation]");
            
            return prompt.toString();
        }
        
        private IntentAnalysis parseIntentAnalysis(String analysisResult, String originalQuery) {
            try {
                Map<String, String> parsed = new HashMap<>();
                String[] lines = analysisResult.split("\n");
                
                for (String line : lines) {
                    if (line.contains(":")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length == 2) {
                            parsed.put(parts[0].trim().toUpperCase(), parts[1].trim());
                        }
                    }
                }
                
                String intent = parsed.getOrDefault("INTENT", "chat");
                String agent = parsed.getOrDefault("AGENT", "EnhancedChatAgent");
                double confidence = Double.parseDouble(parsed.getOrDefault("CONFIDENCE", "0.7"));
                String parametersStr = parsed.getOrDefault("PARAMETERS", "");
                String reasoning = parsed.getOrDefault("REASONING", "Default routing");
                
                Map<String, Object> parameters = parseParameters(parametersStr);
                parameters.put("originalQuery", originalQuery);
                
                return new IntentAnalysis(intent, agent, confidence, parameters, reasoning);
                
            } catch (Exception e) {
                logMessage("Error parsing intent analysis: " + e.getMessage());
                return new IntentAnalysis("chat", "EnhancedChatAgent", 0.5, 
                    Map.of("originalQuery", originalQuery), "Parse error fallback");
            }
        }
        
        private Map<String, Object> parseParameters(String parametersStr) {
            Map<String, Object> params = new HashMap<>();
            if (parametersStr != null && !parametersStr.trim().isEmpty()) {
                String[] pairs = parametersStr.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) {
                        params.put(kv[0].trim(), kv[1].trim());
                    }
                }
            }
            return params;
        }
        
        private IntentAnalysis createFallbackIntent(String userQuery, List<AgentRegistry.AgentInfo> agents) {
            // Simple keyword-based fallback
            String lowerQuery = userQuery.toLowerCase();
            
            if (lowerQuery.contains("weather") || lowerQuery.contains("temperature") || 
                lowerQuery.contains("forecast") || lowerQuery.contains("rain") || lowerQuery.contains("sun")) {
                return new IntentAnalysis("weather", "WeatherAgent", 0.8, 
                    Map.of("originalQuery", userQuery), "Keyword-based fallback");
            } else if (lowerQuery.contains("stock") || lowerQuery.contains("price") || 
                      lowerQuery.contains("share") || lowerQuery.contains("market")) {
                return new IntentAnalysis("stock", "StockPriceAgent", 0.8, 
                    Map.of("originalQuery", userQuery), "Keyword-based fallback");
            } else if (lowerQuery.contains("travel") || lowerQuery.contains("trip") || 
                      lowerQuery.contains("flight") || lowerQuery.contains("hotel")) {
                return new IntentAnalysis("travel", "TravelPlannerAgent", 0.8, 
                    Map.of("originalQuery", userQuery), "Keyword-based fallback");
            } else {
                return new IntentAnalysis("chat", "EnhancedChatAgent", 0.6, 
                    Map.of("originalQuery", userQuery), "Default fallback");
            }
        }
    }
    
    /**
     * Result of intent analysis
     */
    private static class IntentAnalysis {
        private final String intent;
        private final String targetAgent;
        private final double confidence;
        private final Map<String, Object> parameters;
        private final String reasoning;
        
        public IntentAnalysis(String intent, String targetAgent, double confidence, 
                             Map<String, Object> parameters, String reasoning) {
            this.intent = intent;
            this.targetAgent = targetAgent;
            this.confidence = confidence;
            this.parameters = parameters;
            this.reasoning = reasoning;
        }
        
        // Getters
        public String getIntent() { return intent; }
        public String getTargetAgent() { return targetAgent; }
        public double getConfidence() { return confidence; }
        public Map<String, Object> getParameters() { return parameters; }
        public String getReasoning() { return reasoning; }
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
            logMessage("🎯 Activating AMCP Orchestrator Agent...");
            lifecycleState = AgentLifecycle.ACTIVE;
            
            // Activate agent registry
            agentRegistry.onActivate();
            
            // Subscribe to orchestration events
            subscribe("orchestrator.request.**");
            subscribe("orchestrator.analyze.**");
            subscribe("agent.response.**");
            
            logMessage("🎯 Orchestrator Agent activated successfully");
            logMessage("🤖 TinyLlama integration ready for intent analysis");
            
        } catch (Exception e) {
            logMessage("❌ Error activating Orchestrator Agent: " + e.getMessage());
            lifecycleState = AgentLifecycle.INACTIVE;
        }
    }
    
    @Override
    public void onDeactivate() {
        lifecycleState = AgentLifecycle.INACTIVE;
        logMessage("🎯 Orchestrator Agent deactivated");
    }
    
    @Override
    public void onDestroy() {
        activeSessions.clear();
        pendingResponses.clear();
        lifecycleState = AgentLifecycle.DESTROYED;
        logMessage("🎯 Orchestrator Agent destroyed");
    }
    
    @Override
    public void onBeforeMigration(String destinationContext) {
        logMessage("🎯 Preparing for migration to: " + destinationContext);
        // Save active sessions state if needed
    }
    
    @Override
    public void onAfterMigration(String sourceContext) {
        logMessage("🎯 Completed migration from: " + sourceContext);
        // Restore session state if needed
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                switch (event.getTopic()) {
                    case "orchestrator.request.analyze":
                        handleAnalyzeRequest(event);
                        break;
                    case "agent.response":
                        handleAgentResponse(event);
                        break;
                    default:
                        logMessage("Unhandled event: " + event.getTopic());
                }
            } catch (Exception e) {
                logMessage("Error handling event " + event.getTopic() + ": " + e.getMessage());
            }
        });
    }
    
    /**
     * Main orchestration method - analyzes user intent and routes to appropriate agent
     */
    public CompletableFuture<String> orchestrateRequest(String userQuery, String conversationId) {
        String sessionId = UUID.randomUUID().toString();
        OrchestrationSession session = new OrchestrationSession(sessionId, userQuery);
        activeSessions.put(sessionId, session);
        
        logMessage("🎯 Starting orchestration for query: \"" + userQuery + "\"");
        
        return agentRegistry.discoverAgents()
            .thenCompose(agents -> {
                logMessage("📋 Discovered " + agents.size() + " available agents");
                return intentAnalyzer.analyzeIntent(userQuery, agents);
            })
            .thenCompose(analysis -> {
                session.setDetectedIntent(analysis.getIntent());
                session.setTargetAgent(analysis.getTargetAgent());
                
                logMessage("🧠 Intent Analysis Result:");
                logMessage("   Intent: " + analysis.getIntent());
                logMessage("   Target Agent: " + analysis.getTargetAgent());
                logMessage("   Confidence: " + String.format("%.2f", analysis.getConfidence()));
                logMessage("   Reasoning: " + analysis.getReasoning());
                
                // Route to appropriate agent
                return routeToAgent(analysis, session, conversationId);
            })
            .thenCompose(agentResponse -> {
                session.setAgentResponse(agentResponse);
                
                // Format final response for user
                return formatFinalResponse(session, agentResponse);
            })
            .thenApply(finalResponse -> {
                session.setFinalResponse(finalResponse);
                activeSessions.remove(sessionId);
                
                logMessage("✅ Orchestration completed for session: " + sessionId);
                return finalResponse;
            })
            .exceptionally(ex -> {
                logMessage("❌ Orchestration failed: " + ex.getMessage());
                activeSessions.remove(sessionId);
                return "I apologize, but I encountered an issue processing your request. Please try again.";
            });
    }
    
    private CompletableFuture<String> routeToAgent(IntentAnalysis analysis, 
                                                  OrchestrationSession session, 
                                                  String conversationId) {
        String targetTopic = getAgentRequestTopic(analysis.getTargetAgent());
        String correlationId = UUID.randomUUID().toString();
        
        // Prepare request for target agent
        Map<String, Object> request = new HashMap<>();
        request.put("query", session.getOriginalQuery());
        request.put("parameters", analysis.getParameters());
        request.put("conversationId", conversationId);
        request.put("sessionId", session.getSessionId());
        request.put("intent", analysis.getIntent());
        
        // Create pending response future
        CompletableFuture<String> responsePromise = new CompletableFuture<>();
        pendingResponses.put(correlationId, responsePromise);
        
        // Set timeout
        responsePromise.orTimeout(30, TimeUnit.SECONDS);
        
        // Send request to target agent
        publishEvent(Event.builder()
            .topic(targetTopic)
            .payload(request)
            .correlationId(correlationId)
            .sender(agentId)
            .build());
        
        logMessage("📤 Routed request to " + analysis.getTargetAgent() + " via " + targetTopic);
        
        return responsePromise;
    }
    
    private CompletableFuture<String> formatFinalResponse(OrchestrationSession session, String agentResponse) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Use TinyLlama to format and enhance the response
                String formattingPrompt = buildResponseFormattingPrompt(session, agentResponse);
                
                Map<String, Object> params = new HashMap<>();
                params.put("prompt", formattingPrompt);
                params.put("model", "tinyllama");
                params.put("temperature", 0.7);
                params.put("max_tokens", 300);
                
                ToolRequest toolRequest = new ToolRequest(
                    "ollama-chat", 
                    params,
                    UUID.randomUUID().toString()
                );
                
                ToolResponse response = ollamaConnector.invoke(toolRequest).get(15, TimeUnit.SECONDS);
                
                if (response.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) response.getData();
                    String formattedResponse = (String) result.get("response");
                    
                    logMessage("✨ Response formatted by TinyLlama");
                    return formattedResponse;
                } else {
                    logMessage("⚠️ Response formatting failed, using agent response directly");
                    return agentResponse;
                }
                
            } catch (Exception e) {
                logMessage("❌ Error formatting response: " + e.getMessage());
                return agentResponse; // Fallback to original response
            }
        });
    }
    
    private String buildResponseFormattingPrompt(OrchestrationSession session, String agentResponse) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are TinyLlama, helping format responses for users in a conversational AI system.\n\n");
        
        prompt.append("CONTEXT:\n");
        prompt.append("User asked: \"").append(session.getOriginalQuery()).append("\"\n");
        prompt.append("Intent detected: ").append(session.getDetectedIntent()).append("\n");
        prompt.append("Agent used: ").append(session.getTargetAgent()).append("\n\n");
        
        prompt.append("AGENT RESPONSE:\n");
        prompt.append(agentResponse).append("\n\n");
        
        prompt.append("FORMAT INSTRUCTIONS:\n");
        prompt.append("- Make the response conversational and user-friendly\n");
        prompt.append("- Keep technical details but explain them clearly\n");
        prompt.append("- Add relevant emojis for visual appeal\n");
        prompt.append("- If it's data (weather, stock), present it in an organized way\n");
        prompt.append("- Maintain accuracy - don't change facts or numbers\n");
        prompt.append("- Keep response concise but complete\n\n");
        
        prompt.append("Provide the formatted response:");
        
        return prompt.toString();
    }
    
    private void handleAnalyzeRequest(Event event) {
        // Handle direct analysis requests if needed
        logMessage("📊 Received analysis request");
    }
    
    private void handleAgentResponse(Event event) {
        String correlationId = event.getCorrelationId();
        if (correlationId != null && pendingResponses.containsKey(correlationId)) {
            CompletableFuture<String> pendingResponse = pendingResponses.remove(correlationId);
            
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = (Map<String, Object>) event.getPayload();
                String response = (String) payload.get("response");
                
                if (response != null) {
                    pendingResponse.complete(response);
                    logMessage("📥 Received agent response for correlation: " + correlationId);
                } else {
                    pendingResponse.completeExceptionally(new RuntimeException("Empty response from agent"));
                }
                
            } catch (Exception e) {
                logMessage("❌ Error processing agent response: " + e.getMessage());
                pendingResponse.completeExceptionally(e);
            }
        }
    }
    
    private String getAgentRequestTopic(String agentName) {
        switch (agentName) {
            case "WeatherAgent":
                return "weather.request";
            case "StockPriceAgent":
                return "stock.request";
            case "TravelPlannerAgent":
                return "travel.request";
            case "EnhancedChatAgent":
                return "chat.request";
            default:
                return "agent.request." + agentName.toLowerCase();
        }
    }
    
    /**
     * Get orchestration statistics
     */
    public Map<String, Object> getOrchestrationStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeSessions", activeSessions.size());
        stats.put("pendingResponses", pendingResponses.size());
        stats.put("agentId", agentId.toString());
        stats.put("lifecycleState", lifecycleState);
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
        System.out.println("[" + timestamp + "] [OrchestratorAgent] " + message);
    }
}