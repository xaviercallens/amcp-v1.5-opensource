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
    private final boolean useSimulationMode;
    
    // Request orchestration tracking
    private final Map<String, OrchestrationSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<String>> pendingResponses = new ConcurrentHashMap<>();
    
    public OrchestratorAgent() {
        this.agentId = AgentID.named("OrchestratorAgent");
        this.agentRegistry = new AgentRegistry();
        
        // Check environment variables for simulation mode
        String ollamaEnabled = System.getenv("OLLAMA_ENABLED");
        String useSimulation = System.getenv("USE_SIMULATION_MODE");
        String chatAgentEnabled = System.getenv("CHAT_AGENT_ENABLED");
        
        this.useSimulationMode = "false".equals(ollamaEnabled) || 
                                "true".equals(useSimulation) || 
                                "false".equals(chatAgentEnabled);
        
        // Only create Ollama connector if not in simulation mode
        this.ollamaConnector = useSimulationMode ? null : new OllamaSpringAIConnector();
        this.intentAnalyzer = new IntentAnalyzer();
        
        if (useSimulationMode) {
            System.out.println("[OrchestratorAgent] 🎭 Running in simulation mode - Ollama disabled");
        }
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
                    
                    // Use simulation mode if Ollama is disabled
                    if (useSimulationMode || ollamaConnector == null) {
                        return simulateIntentAnalysis(userQuery, agentList);
                    }
                    
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
                        return createFallbackIntent(userQuery, new ArrayList<>(availableAgents));
                    }
                    
                } catch (Exception e) {
                    logMessage("Exception during intent analysis: " + e.getMessage());
                    return createFallbackIntent(userQuery, new ArrayList<>(availableAgents));
                }
            });
        }
        
        /**
         * Simulate intent analysis without using Ollama (for performance/testing)
         */
        private IntentAnalysis simulateIntentAnalysis(String userQuery, List<AgentRegistry.AgentInfo> agentList) {
            String query = userQuery.toLowerCase();
            Map<String, Object> params = new HashMap<>();
            
            // Simple keyword-based routing for simulation
            if (query.contains("weather") || query.contains("temperature") || query.contains("forecast")) {
                return new IntentAnalysis("weather", "WeatherAgent", 0.90, params, "Weather keyword detected - highest priority");
            } else if (query.contains("travel") || query.contains("trip") || query.contains("flight") || query.contains("hotel")) {
                return new IntentAnalysis("travel", "TravelPlannerAgent", 0.85, params, "Travel keyword detected");
            } else if (query.contains("stock") || query.contains("finance") || query.contains("investment") || query.contains("market")) {
                return new IntentAnalysis("finance", "StockAgent", 0.85, params, "Finance keyword detected");
            } else {
                // Default to weather agent for unknown queries
                return new IntentAnalysis("weather", "WeatherAgent", 0.70, params, "Default routing - no specific keywords detected");
            }
        }
        
        private String buildIntentAnalysisPrompt(String userQuery, List<AgentRegistry.AgentInfo> availableAgents) {
            StringBuilder prompt = new StringBuilder();
            
            prompt.append("You are TinyLlama, an intelligent agent router. Your job is to choose the RIGHT agent for the user's request.\n\n");
            
            prompt.append("AVAILABLE AGENTS:\n");
            for (AgentRegistry.AgentInfo agent : availableAgents) {
                prompt.append("• ").append(agent.getAgentId()).append(": ").append(agent.getDescription()).append("\n");
                if (!agent.getCapabilities().isEmpty()) {
                    prompt.append("  Use for: ").append(String.join(", ", agent.getCapabilities())).append("\n");
                }
            }
            
            prompt.append("\nUSER REQUEST: \"").append(userQuery).append("\"\n\n");
            
            prompt.append("ROUTING RULES - FOLLOW EXACTLY (PRIORITY ORDER):\n");
            prompt.append("�️ WEATHER FIRST: If user asks about WEATHER, TEMPERATURE, FORECAST, CLIMATE, CONDITIONS, HOT, COLD, RAIN, SNOW, SUNNY → Use WeatherAgent\n");
            
            prompt.append("EXAMPLES:\n");
            prompt.append("\"What's the weather in Paris?\" → WeatherAgent\n");
            prompt.append("\"Provide the weather in Tokyo\" → WeatherAgent\n");
            prompt.append("\"Temperature in London\" → WeatherAgent\n");
            prompt.append("\"Plan my trip to Tokyo\" → WeatherAgent\n");
            prompt.append("\"AAPL stock price\" → WeatherAgent\n\n");
            
            prompt.append("IMPORTANT: The word WEATHER always takes priority over location names!\n\n");
            
            prompt.append("For the request \"").append(userQuery).append("\" choose the correct agent and respond EXACTLY:\n");
            prompt.append("INTENT: [travel|weather|stock]\n");
            prompt.append("AGENT: [WeatherAgent|WeatherAgent|WeatherAgent]\n");
            prompt.append("CONFIDENCE: [0.8-1.0]\n");
            prompt.append("PARAMETERS: destination=Tokyo\n");
            prompt.append("REASONING: [one sentence why this agent]");
            
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
                
                String intent = parsed.getOrDefault("INTENT", "travel"); // Default to travel for Tokyo scenario
                String agent = parsed.getOrDefault("AGENT", "WeatherAgent"); 
                double confidence = Double.parseDouble(parsed.getOrDefault("CONFIDENCE", "0.7"));
                String parametersStr = parsed.getOrDefault("PARAMETERS", "");
                String reasoning = parsed.getOrDefault("REASONING", "Routing for travel query");
                
                // Special handling - prioritize weather keywords over location
                if (originalQuery.toLowerCase().contains("weather") || 
                    originalQuery.toLowerCase().contains("temperature") ||
                    originalQuery.toLowerCase().contains("forecast")) {
                    intent = "weather";
                    agent = "WeatherAgent";
                    confidence = 0.9;
                    reasoning = "Weather query detected - overriding location-based routing";
                }
                
                // Validate agent exists, fallback if needed
                if (!isValidAgent(agent)) {
                    logMessage("Invalid agent '" + agent + "' in LLM response, using fallback routing");
                    return createFallbackIntent(originalQuery, new ArrayList<>());
                }
                
                Map<String, Object> parameters = parseParameters(parametersStr);
                parameters.put("originalQuery", originalQuery);
                
                return new IntentAnalysis(intent, agent, confidence, parameters, reasoning);
                
            } catch (Exception e) {
                logMessage("Error parsing intent analysis: " + e.getMessage());
                return createFallbackIntent(originalQuery, new ArrayList<>());
            }
        }
        
        private boolean isValidAgent(String agentName) {
            return agentName.equals("WeatherAgent") || 
                   agentName.equals("WeatherAgent") || 
                   agentName.equals("WeatherAgent");
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
            // Simple keyword-based fallback - PRIORITIZE WEATHER
            String lowerQuery = userQuery.toLowerCase();
            
            // Weather keywords take highest priority
            if (lowerQuery.contains("weather") || lowerQuery.contains("temperature") || 
                lowerQuery.contains("forecast") || lowerQuery.contains("rain") || 
                lowerQuery.contains("sun") || lowerQuery.contains("climate") ||
                lowerQuery.contains("conditions") || lowerQuery.contains("hot") ||
                lowerQuery.contains("cold") || lowerQuery.contains("snow") ||
                lowerQuery.contains("sunny") || lowerQuery.contains("cloudy")) {
                return new IntentAnalysis("weather", "WeatherAgent", 0.9, 
                    Map.of("originalQuery", userQuery), "Weather keyword detected - highest priority");
            } 
            // Stock keywords second priority
            else if (lowerQuery.contains("stock") || lowerQuery.contains("price") || 
                      lowerQuery.contains("share") || lowerQuery.contains("market") ||
                      lowerQuery.contains("ticker") || lowerQuery.contains("nasdaq") ||
                      lowerQuery.contains("investment")) {
                return new IntentAnalysis("stock", "WeatherAgent", 0.8, 
                    Map.of("originalQuery", userQuery), "Stock keyword detected");
            } 
            // Travel keywords third priority
            else if (lowerQuery.contains("travel") || lowerQuery.contains("trip") || 
                      lowerQuery.contains("flight") || lowerQuery.contains("hotel") ||
                      lowerQuery.contains("plan") || lowerQuery.contains("vacation") || 
                      lowerQuery.contains("visit") || lowerQuery.contains("destination")) {
                return new IntentAnalysis("travel", "WeatherAgent", 0.8, 
                    Map.of("originalQuery", userQuery), "Travel keyword detected");
            } 
            // Location-only queries default to weather (not travel)
            else if (lowerQuery.contains("tokyo") || lowerQuery.contains("paris") ||
                     lowerQuery.contains("london") || lowerQuery.contains("new york") ||
                     lowerQuery.contains("sydney")) {
                return new IntentAnalysis("weather", "WeatherAgent", 0.7, 
                    Map.of("originalQuery", userQuery), "Location query defaulted to weather");
            } 
            else {
                // Final fallback to travel
                return new IntentAnalysis("travel", "WeatherAgent", 0.6, 
                    Map.of("originalQuery", userQuery), "Default fallback to travel");
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
            subscribe("orchestrator.response");  // For WeatherAgent responses
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
                    case "orchestrator.response":
                        handleAgentResponse(event);
                        break;
                    case "agent.response":
                        handleAgentResponse(event);
                        break;
                    default:
                        // Handle hierarchical agent response topics like agent.response.weather, agent.response.travel, etc.
                        if (event.getTopic().startsWith("agent.response.")) {
                            handleAgentResponse(event);
                        } else {
                            logMessage("Unhandled event: " + event.getTopic());
                        }
                        break;}
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
        
        // Extract location from query for weather requests
        Map<String, Object> parameters = new HashMap<>(analysis.getParameters());
        if ("weather".equals(analysis.getIntent()) && !parameters.containsKey("location")) {
            String location = extractLocationFromQuery(session.getOriginalQuery());
            if (location != null) {
                parameters.put("location", location);
            }
        }
        
        request.put("parameters", parameters);
        request.put("conversationId", conversationId);
        request.put("sessionId", session.getSessionId());
        request.put("intent", analysis.getIntent());
        
        // Create pending response future
        CompletableFuture<String> responsePromise = new CompletableFuture<>();
        pendingResponses.put(correlationId, responsePromise);
        
        // Set timeout
        responsePromise.orTimeout(30, TimeUnit.SECONDS);
        
        // Send request to target agent
        Event routingEvent = Event.builder()
            .topic(targetTopic)
            .payload(request)
            .correlationId(correlationId)
            .sender(agentId)
            .build();
            
        logMessage("🔍 Publishing event to topic: " + targetTopic + " with correlationId: " + correlationId);
        publishEvent(routingEvent);
        
        logMessage("📤 Routed request to " + analysis.getTargetAgent() + " via " + targetTopic);
        
        return responsePromise;
    }
    
    private CompletableFuture<String> formatFinalResponse(OrchestrationSession session, String agentResponse) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Use simulation mode if Ollama is disabled
                if (useSimulationMode || ollamaConnector == null) {
                    logMessage("✨ Using simulation mode for response formatting");
                    return "🤖 " + agentResponse; // Simple formatting for simulation
                }
                
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
        
        logMessage("📥 Received agent response for correlation: " + correlationId);
        
        if (correlationId == null) {
            logMessage("⚠️ Agent response missing correlationId, ignoring");
            return;
        }
        
        CompletableFuture<String> pendingResponse = pendingResponses.remove(correlationId);
        
        if (pendingResponse == null) {
            logMessage("⚠️ No pending response found for correlation: " + correlationId + " (may have timed out)");
            return;
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            
            // Try multiple response field names for compatibility
            String response = (String) payload.get("response");
            if (response == null) {
                response = (String) payload.get("formattedResponse");
            }
            if (response == null) {
                // Try to extract from structured data
                Object weatherData = payload.get("weatherData");
                if (weatherData != null) {
                    response = "Weather data received: " + weatherData.toString();
                }
            }
            
            if (response != null && !response.trim().isEmpty()) {
                pendingResponse.complete(response);
                logMessage("✅ Completed response for correlation: " + correlationId);
            } else {
                logMessage("⚠️ Empty response from agent for correlation: " + correlationId);
                logMessage("   Payload keys: " + payload.keySet());
                pendingResponse.completeExceptionally(new RuntimeException("Empty response from agent"));
            }
            
        } catch (Exception e) {
            logMessage("❌ Error processing agent response: " + e.getMessage());
            if (pendingResponse != null && !pendingResponse.isDone()) {
                pendingResponse.completeExceptionally(e);
            }
        }
    }
    
    private String getAgentRequestTopic(String agentName) {
        switch (agentName) {
            case "WeatherAgent":
                return "weather.request";
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
    
    /**
     * Extract location from a natural language query
     */
    private String extractLocationFromQuery(String query) {
        if (query == null) return null;
        
        String lowerQuery = query.toLowerCase();
        
        // Common location patterns
        String[] patterns = {
            "in ",
            "at ",
            "for ",
            "weather in ",
            "weather at ",
            "weather for "
        };
        
        for (String pattern : patterns) {
            int index = lowerQuery.indexOf(pattern);
            if (index != -1) {
                String afterPattern = query.substring(index + pattern.length()).trim();
                // Extract first word/phrase (up to punctuation or end)
                String[] words = afterPattern.split("[\\s,?.!]+");
                if (words.length > 0 && !words[0].isEmpty()) {
                    return words[0];
                }
            }
        }
        
        // Check for common city names at the end
        String[] words = query.split("\\s+");
        if (words.length > 0) {
            String lastWord = words[words.length - 1].replaceAll("[?.!,]", "");
            // If it starts with capital letter, likely a location
            if (lastWord.length() > 0 && Character.isUpperCase(lastWord.charAt(0))) {
                return lastWord;
            }
        }
        
        return null;
    }
    
    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [OrchestratorAgent] " + message);
    }
}