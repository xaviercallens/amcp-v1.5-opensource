package io.amcp.connectors.ai;

import io.amcp.core.Agent;
import io.amcp.core.AgentContext;
import io.amcp.core.AgentID;
import io.amcp.core.AgentLifecycle;
import io.amcp.core.Event;
import io.amcp.core.DeliveryOptions;
import io.amcp.core.lifecycle.CloudEventsAgentLifecycle;
import io.amcp.tools.ToolRequest;
import io.amcp.tools.ToolResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AMCP v1.5 CloudEvents-Compliant Orchestrator Agent with Enhanced LLM Intelligence
 * 
 * This intelligent orchestrator provides enterprise-grade event orchestration with:
 * - Full CloudEvents 1.0 specification compliance
 * - Reverse-DNS event type naming (io.amcp.*)
 * - Proper correlation handling with CloudEvents extensions
 * - Structured Agent Join/Leave lifecycle events
 * - Enhanced error handling with structured error events
 * - Comprehensive logging and monitoring with event tracing
 * - LLM coordination with structured JSON responses and capability grounding
 * - Multi-turn conversation handling with conversation history
 * 
 * The CloudEventsCompliantOrchestratorAgent ensures enterprise interoperability
 * and provides a foundation for complex multi-agent workflows.
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 * @since 1.5.0
 */
public class CloudEventsCompliantOrchestratorAgent implements Agent {
    
    private static final String AGENT_TYPE = "CLOUDEVENTS_ORCHESTRATOR";
    private static final String VERSION = "1.5.0";
    @SuppressWarnings("unused")
    private static final String DOMAIN_PREFIX = "io.amcp";
    
    // CloudEvents compliant event types
    private static final String EVENT_TYPE_ORCHESTRATOR_REQUEST = "io.amcp.orchestrator.request";
    private static final String EVENT_TYPE_ORCHESTRATOR_RESPONSE = "io.amcp.orchestrator.response";
    private static final String EVENT_TYPE_ORCHESTRATOR_ERROR = "io.amcp.orchestrator.error";
    private static final String EVENT_TYPE_AGENT_JOIN = "io.amcp.agent.join";
    private static final String EVENT_TYPE_AGENT_LEAVE = "io.amcp.agent.leave";
    private static final String EVENT_TYPE_TASK_REQUEST = "io.amcp.task.request";
    private static final String EVENT_TYPE_TASK_RESPONSE = "io.amcp.task.response";
    private static final String EVENT_TYPE_TASK_ERROR = "io.amcp.task.error";
    
    private final AgentID agentId;
    private AgentContext context;
    private AgentLifecycle lifecycleState = AgentLifecycle.INACTIVE;
    private URI sourceUri;
    
    // Core orchestration components
    private final AgentRegistry agentRegistry;
    private final OllamaSpringAIConnector ollamaConnector;
    private final CloudEventsIntentAnalyzer intentAnalyzer;
    private final OrchestrationLogger orchestrationLogger;
    private final ErrorHandler errorHandler;
    
    // Request orchestration tracking
    private final Map<String, OrchestrationSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<String>> pendingResponses = new ConcurrentHashMap<>();
    
    // Metrics and monitoring
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong averageResponseTime = new AtomicLong(0);
    private final long startupTime = System.currentTimeMillis();
    
    public CloudEventsCompliantOrchestratorAgent() {
        this.agentId = AgentID.named("CloudEventsOrchestratorAgent");
        this.agentRegistry = new AgentRegistry();
        this.ollamaConnector = new OllamaSpringAIConnector();
        this.intentAnalyzer = new CloudEventsIntentAnalyzer();
        this.orchestrationLogger = new OrchestrationLogger();
        this.errorHandler = new ErrorHandler();
        
        try {
            this.sourceUri = URI.create("urn:amcp:agent:" + agentId.toString().toLowerCase());
        } catch (Exception e) {
            this.sourceUri = URI.create("urn:amcp:agent:orchestrator");
        }
    }
    
    /**
     * Enhanced orchestration session with CloudEvents compliance tracking
     */
    private static class OrchestrationSession {
        private final String sessionId;
        private final String originalQuery;
        private final long startTime;
        private final String correlationId;
        private final List<String> eventIds = new ArrayList<>();
        private String detectedIntent;
        private String targetAgent;
        private String agentResponse;
        private String finalResponse;
        private final Map<String, Object> metadata = new HashMap<>();
        private final List<Map<String, Object>> conversationHistory = new ArrayList<>();
        private String errorMessage;
        private boolean isCloudEventsCompliant = true;
        
        public OrchestrationSession(String sessionId, String originalQuery, String correlationId) {
            this.sessionId = sessionId;
            this.originalQuery = originalQuery;
            this.correlationId = correlationId;
            this.startTime = System.currentTimeMillis();
        }
        
        // Enhanced getters and setters with validation
        public String getSessionId() { return sessionId; }
        public String getOriginalQuery() { return originalQuery; }
        public long getStartTime() { return startTime; }
        public String getCorrelationId() { return correlationId; }
        public List<String> getEventIds() { return new ArrayList<>(eventIds); }
        public void addEventId(String eventId) { this.eventIds.add(eventId); }
        public String getDetectedIntent() { return detectedIntent; }
        public void setDetectedIntent(String detectedIntent) { this.detectedIntent = detectedIntent; }
        public String getTargetAgent() { return targetAgent; }
        public void setTargetAgent(String targetAgent) { this.targetAgent = targetAgent; }
        public String getAgentResponse() { return agentResponse; }
        public void setAgentResponse(String agentResponse) { this.agentResponse = agentResponse; }
        public String getFinalResponse() { return finalResponse; }
        public void setFinalResponse(String finalResponse) { this.finalResponse = finalResponse; }
        public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
        public void addMetadata(String key, Object value) { this.metadata.put(key, value); }
        public List<Map<String, Object>> getConversationHistory() { return new ArrayList<>(conversationHistory); }
        public void addConversationTurn(String speaker, String message) {
            Map<String, Object> turn = new HashMap<>();
            turn.put("speaker", speaker);
            turn.put("message", message);
            turn.put("timestamp", LocalDateTime.now().toString());
            this.conversationHistory.add(turn);
        }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public boolean isCloudEventsCompliant() { return isCloudEventsCompliant; }
        public void setCloudEventsCompliant(boolean compliant) { this.isCloudEventsCompliant = compliant; }
        
        public long getDuration() { return System.currentTimeMillis() - startTime; }
    }
    
    /**
     * Enhanced intent analyzer with CloudEvents compliance and structured JSON responses
     */
    private class CloudEventsIntentAnalyzer {
        
        public CompletableFuture<IntentAnalysis> analyzeIntent(String userQuery, 
                                                              Set<AgentRegistry.AgentInfo> availableAgents,
                                                              String correlationId) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    List<AgentRegistry.AgentInfo> agentList = new ArrayList<>(availableAgents);
                    String analysisPrompt = buildStructuredIntentAnalysisPrompt(userQuery, agentList);
                    
                    orchestrationLogger.logIntentAnalysisStart(correlationId, userQuery, agentList.size());
                    
                    // Call TinyLlama with structured JSON response requirement
                    Map<String, Object> params = new HashMap<>();
                    params.put("prompt", analysisPrompt);
                    params.put("model", "tinyllama");
                    params.put("temperature", 0.2); // Lower temperature for more consistent structured responses
                    params.put("max_tokens", 300);
                    params.put("format", "json"); // Request JSON format
                    
                    ToolRequest toolRequest = new ToolRequest(
                        "ollama-chat", 
                        params,
                        correlationId
                    );
                    
                    ToolResponse response = ollamaConnector.invoke(toolRequest).get(20, TimeUnit.SECONDS);
                    
                    if (response.isSuccess()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> result = (Map<String, Object>) response.getData();
                        String analysisResult = (String) result.get("response");
                        
                        IntentAnalysis analysis = parseStructuredIntentAnalysis(analysisResult, userQuery, correlationId);
                        orchestrationLogger.logIntentAnalysisSuccess(correlationId, analysis);
                        return analysis;
                    } else {
                        orchestrationLogger.logIntentAnalysisFailure(correlationId, response.getErrorMessage());
                        return createFallbackIntent(userQuery, agentList, correlationId);
                    }
                    
                } catch (Exception e) {
                    orchestrationLogger.logIntentAnalysisError(correlationId, e);
                    return createFallbackIntent(userQuery, new ArrayList<>(availableAgents), correlationId);
                }
            });
        }
        
        private String buildStructuredIntentAnalysisPrompt(String userQuery, List<AgentRegistry.AgentInfo> agents) {
            StringBuilder prompt = new StringBuilder();
            
            prompt.append("You are TinyLlama, an intelligent agent router for AMCP v1.5. Respond ONLY with valid JSON.\n\n");
            
            prompt.append("AVAILABLE AGENTS AND CAPABILITIES:\n");
            for (AgentRegistry.AgentInfo agent : agents) {
                prompt.append("• ").append(agent.getAgentId()).append(":\n");
                prompt.append("  Description: ").append(agent.getDescription()).append("\n");
                if (!agent.getCapabilities().isEmpty()) {
                    prompt.append("  Capabilities: [").append(String.join(", ", agent.getCapabilities())).append("]\n");
                }
            }
            
            prompt.append("\nUSER REQUEST: \"").append(userQuery).append("\"\n\n");
            
            prompt.append("ROUTING PRIORITY (EXACT MATCH RULES):\n");
            prompt.append("1. WEATHER: Keywords like 'weather', 'temperature', 'forecast', 'climate', 'rain', 'sunny' → WeatherAgent\n");
            prompt.append("2. STOCKS: Keywords like 'stock', 'price', 'market', 'investment', 'ticker', 'nasdaq' → StockPriceAgent\n");
            prompt.append("3. TRAVEL: Keywords like 'travel', 'trip', 'vacation', 'hotel', 'flight', 'destination' → TravelPlannerAgent\n\n");
            
            prompt.append("IMPORTANT: Weather queries ALWAYS take priority over location names!\n\n");
            
            prompt.append("RESPOND WITH EXACT JSON FORMAT:\n");
            prompt.append("{\n");
            prompt.append("  \"intent\": \"weather|stock|travel|chat\",\n");
            prompt.append("  \"targetAgent\": \"WeatherAgent|StockPriceAgent|TravelPlannerAgent\",\n");
            prompt.append("  \"confidence\": 0.9,\n");
            prompt.append("  \"parameters\": {\n");
            prompt.append("    \"location\": \"extracted_location\",\n");
            prompt.append("    \"originalQuery\": \"" + userQuery + "\"\n");
            prompt.append("  },\n");
            prompt.append("  \"reasoning\": \"Brief explanation for this routing decision\"\n");
            prompt.append("}\n\n");
            
            prompt.append("For query: \"").append(userQuery).append("\" - analyze and respond with JSON:");
            
            return prompt.toString();
        }
        
        private IntentAnalysis parseStructuredIntentAnalysis(String analysisResult, String originalQuery, String correlationId) {
            try {
                // Try to parse as JSON first
                if (analysisResult.trim().startsWith("{")) {
                    return parseJsonIntentAnalysis(analysisResult, originalQuery, correlationId);
                } else {
                    // Fallback to text parsing
                    return parseTextIntentAnalysis(analysisResult, originalQuery, correlationId);
                }
                
            } catch (Exception e) {
                orchestrationLogger.logIntentParsingError(correlationId, e, analysisResult);
                return createFallbackIntent(originalQuery, new ArrayList<>(), correlationId);
            }
        }
        
        private IntentAnalysis parseJsonIntentAnalysis(String jsonResult, String originalQuery, String correlationId) {
            try {
                // Simple JSON parsing - in production, use a proper JSON library
                Map<String, Object> parsed = parseSimpleJson(jsonResult);
                
                String intent = (String) parsed.getOrDefault("intent", "travel");
                String agent = (String) parsed.getOrDefault("targetAgent", "TravelPlannerAgent");
                double confidence = parseDouble(parsed.get("confidence"), 0.7);
                String reasoning = (String) parsed.getOrDefault("reasoning", "JSON-based routing");
                
                @SuppressWarnings("unchecked")
                Map<String, Object> parameters = (Map<String, Object>) parsed.getOrDefault("parameters", new HashMap<>());
                parameters.put("originalQuery", originalQuery);
                
                // Apply priority override for weather
                if (originalQuery.toLowerCase().contains("weather")) {
                    intent = "weather";
                    agent = "WeatherAgent";
                    confidence = 0.95;
                    reasoning = "Weather keyword detected - highest priority override";
                }
                
                // Validate agent exists
                if (!isValidAgent(agent)) {
                    return createFallbackIntent(originalQuery, new ArrayList<>(), correlationId);
                }
                
                return new IntentAnalysis(intent, agent, confidence, parameters, reasoning, correlationId);
                
            } catch (Exception e) {
                orchestrationLogger.logJsonParsingError(correlationId, e);
                return parseTextIntentAnalysis(jsonResult, originalQuery, correlationId);
            }
        }
        
        private IntentAnalysis parseTextIntentAnalysis(String analysisResult, String originalQuery, String correlationId) {
            // Fallback text parsing implementation (similar to original)
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
            
            String intent = parsed.getOrDefault("INTENT", "travel");
            String agent = parsed.getOrDefault("AGENT", "TravelPlannerAgent");
            double confidence = parseDouble(parsed.get("CONFIDENCE"), 0.7);
            String reasoning = parsed.getOrDefault("REASONING", "Text-based fallback routing");
            
            // Weather priority override
            if (originalQuery.toLowerCase().contains("weather")) {
                intent = "weather";
                agent = "WeatherAgent";
                confidence = 0.9;
                reasoning = "Weather keyword detected - text parsing override";
            }
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("originalQuery", originalQuery);
            
            return new IntentAnalysis(intent, agent, confidence, parameters, reasoning, correlationId);
        }
        
        private Map<String, Object> parseSimpleJson(String json) {
            // Simplified JSON parser - replace with proper JSON library in production
            Map<String, Object> result = new HashMap<>();
            
            // Remove braces and split by comma
            String content = json.trim().replaceAll("^\\{|\\}$", "");
            String[] pairs = content.split(",");
            
            for (String pair : pairs) {
                String[] kv = pair.split(":", 2);
                if (kv.length == 2) {
                    String key = kv[0].trim().replaceAll("\"", "");
                    String value = kv[1].trim().replaceAll("\"", "");
                    
                    if (key.equals("parameters")) {
                        // Handle nested parameters object
                        result.put(key, parseParametersObject(value));
                    } else if (key.equals("confidence")) {
                        result.put(key, parseDouble(value, 0.7));
                    } else {
                        result.put(key, value);
                    }
                }
            }
            
            return result;
        }
        
        private Map<String, Object> parseParametersObject(String params) {
            Map<String, Object> result = new HashMap<>();
            // Simplified parameter parsing
            if (params.contains("{")) {
                // Extract content between braces
                int start = params.indexOf("{");
                int end = params.lastIndexOf("}");
                if (start >= 0 && end > start) {
                    String content = params.substring(start + 1, end);
                    String[] pairs = content.split(",");
                    for (String pair : pairs) {
                        String[] kv = pair.split(":", 2);
                        if (kv.length == 2) {
                            String key = kv[0].trim().replaceAll("\"", "");
                            String value = kv[1].trim().replaceAll("\"", "");
                            result.put(key, value);
                        }
                    }
                }
            }
            return result;
        }
        
        private double parseDouble(Object value, double defaultValue) {
            if (value == null) return defaultValue;
            try {
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                return Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        
        private boolean isValidAgent(String agentName) {
            return agentName != null && (
                agentName.equals("WeatherAgent") || 
                agentName.equals("TravelPlannerAgent") || 
                agentName.equals("StockPriceAgent")
            );
        }
        
        private IntentAnalysis createFallbackIntent(String userQuery, List<AgentRegistry.AgentInfo> agents, String correlationId) {
            String lowerQuery = userQuery.toLowerCase();
            
            // Weather keywords (highest priority)
            if (lowerQuery.contains("weather") || lowerQuery.contains("temperature") || 
                lowerQuery.contains("forecast") || lowerQuery.contains("climate") ||
                lowerQuery.contains("rain") || lowerQuery.contains("sun") || 
                lowerQuery.contains("conditions")) {
                return new IntentAnalysis("weather", "WeatherAgent", 0.9, 
                    Map.of("originalQuery", userQuery), "Weather keyword fallback", correlationId);
            }
            // Stock keywords 
            else if (lowerQuery.contains("stock") || lowerQuery.contains("price") || 
                     lowerQuery.contains("market") || lowerQuery.contains("investment")) {
                return new IntentAnalysis("stock", "StockPriceAgent", 0.8, 
                    Map.of("originalQuery", userQuery), "Stock keyword fallback", correlationId);
            }
            // Travel keywords
            else if (lowerQuery.contains("travel") || lowerQuery.contains("trip") || 
                     lowerQuery.contains("flight") || lowerQuery.contains("hotel")) {
                return new IntentAnalysis("travel", "TravelPlannerAgent", 0.8, 
                    Map.of("originalQuery", userQuery), "Travel keyword fallback", correlationId);
            }
            // Default to weather for location queries
            else {
                return new IntentAnalysis("weather", "WeatherAgent", 0.6, 
                    Map.of("originalQuery", userQuery), "Default weather fallback", correlationId);
            }
        }
    }
    
    /**
     * Enhanced intent analysis result with CloudEvents compliance
     */
    private static class IntentAnalysis {
        private final String intent;
        private final String targetAgent;
        private final double confidence;
        private final Map<String, Object> parameters;
        private final String reasoning;
        private final String correlationId;
        private final long analysisTime;
        
        public IntentAnalysis(String intent, String targetAgent, double confidence, 
                             Map<String, Object> parameters, String reasoning, String correlationId) {
            this.intent = intent;
            this.targetAgent = targetAgent;
            this.confidence = confidence;
            this.parameters = new HashMap<>(parameters);
            this.reasoning = reasoning;
            this.correlationId = correlationId;
            this.analysisTime = System.currentTimeMillis();
        }
        
        // Getters
        public String getIntent() { return intent; }
        public String getTargetAgent() { return targetAgent; }
        public double getConfidence() { return confidence; }
        public Map<String, Object> getParameters() { return new HashMap<>(parameters); }
        public String getReasoning() { return reasoning; }
        public String getCorrelationId() { return correlationId; }
        public long getAnalysisTime() { return analysisTime; }
    }
    
    /**
     * Enhanced orchestration logger with CloudEvents compliance tracking
     */
    private class OrchestrationLogger {
        
        public void logIntentAnalysisStart(String correlationId, String query, int agentCount) {
            logMessage("🎯 [" + correlationId + "] Starting intent analysis for: \"" + query + "\" with " + agentCount + " agents");
        }
        
        public void logIntentAnalysisSuccess(String correlationId, IntentAnalysis analysis) {
            logMessage("🧠 [" + correlationId + "] Intent Analysis Result:");
            logMessage("   Intent: " + analysis.getIntent());
            logMessage("   Target Agent: " + analysis.getTargetAgent());
            logMessage("   Confidence: " + String.format("%.2f", analysis.getConfidence()));
            logMessage("   Reasoning: " + analysis.getReasoning());
        }
        
        public void logIntentAnalysisFailure(String correlationId, String error) {
            logMessage("❌ [" + correlationId + "] Intent analysis failed: " + error);
        }
        
        public void logIntentAnalysisError(String correlationId, Exception e) {
            logMessage("❌ [" + correlationId + "] Intent analysis error: " + e.getMessage());
        }
        
        public void logIntentParsingError(String correlationId, Exception e, String rawResult) {
            logMessage("⚠️ [" + correlationId + "] Intent parsing error: " + e.getMessage());
            logMessage("Raw result: " + rawResult.substring(0, Math.min(100, rawResult.length())));
        }
        
        public void logJsonParsingError(String correlationId, Exception e) {
            logMessage("⚠️ [" + correlationId + "] JSON parsing failed, falling back to text parsing: " + e.getMessage());
        }
        
        public void logEventPublish(String correlationId, String eventType, String topic, String eventId) {
            logMessage("📤 [" + correlationId + "] Published " + eventType + " to topic: " + topic + " (ID: " + eventId + ")");
        }
        
        public void logEventReceived(String correlationId, String eventType, String eventId, String source) {
            logMessage("📥 [" + correlationId + "] Received " + eventType + " (ID: " + eventId + ") from: " + source);
        }
        
        public void logOrchestrationSuccess(String correlationId, long duration) {
            logMessage("✅ [" + correlationId + "] Orchestration completed successfully in " + duration + "ms");
        }
        
        public void logOrchestrationError(String correlationId, String error, long duration) {
            logMessage("❌ [" + correlationId + "] Orchestration failed after " + duration + "ms: " + error);
        }
        
        public void logCloudEventsValidation(String correlationId, String eventId, boolean isCompliant) {
            if (isCompliant) {
                logMessage("✅ [" + correlationId + "] Event " + eventId + " is CloudEvents 1.0 compliant");
            } else {
                logMessage("⚠️ [" + correlationId + "] Event " + eventId + " failed CloudEvents 1.0 validation");
            }
        }
        
        public void logAgentJoin(String agentId, String agentType, Map<String, Object> capabilities) {
            logMessage("🔄 Agent joined: " + agentId + " (type: " + agentType + ") with " + capabilities.size() + " capabilities");
        }
        
        public void logAgentLeave(String agentId, String reason) {
            logMessage("🔄 Agent left: " + agentId + " (reason: " + reason + ")");
        }
    }
    
    /**
     * Enhanced error handler with structured error events
     */
    private class ErrorHandler {
        
        public void handleOrchestrationError(String correlationId, String sessionId, Exception error, OrchestrationSession session) {
            String errorMessage = "Orchestration failed: " + error.getMessage();
            session.setErrorMessage(errorMessage);
            
            // Create structured error event
            Map<String, Object> errorData = createErrorEventData(correlationId, sessionId, error, session);
            
            Event errorEvent = createCloudEventsCompliantEvent(
                EVENT_TYPE_ORCHESTRATOR_ERROR,
                "io.amcp.orchestrator.error",
                errorData,
                correlationId
            );
            
            publishEvent(errorEvent);
            orchestrationLogger.logOrchestrationError(correlationId, errorMessage, session.getDuration());
            
            failedRequests.incrementAndGet();
        }
        
        public void handleTaskError(String correlationId, String targetAgent, String errorMessage) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("targetAgent", targetAgent);
            errorData.put("errorMessage", errorMessage);
            errorData.put("timestamp", LocalDateTime.now().toString());
            errorData.put("severity", "ERROR");
            
            Event taskErrorEvent = createCloudEventsCompliantEvent(
                EVENT_TYPE_TASK_ERROR,
                "io.amcp.task.error." + targetAgent.toLowerCase(),
                errorData,
                correlationId
            );
            
            publishEvent(taskErrorEvent);
            orchestrationLogger.logOrchestrationError(correlationId, "Task error for " + targetAgent + ": " + errorMessage, 0);
        }
        
        private Map<String, Object> createErrorEventData(String correlationId, String sessionId, Exception error, OrchestrationSession session) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("sessionId", sessionId);
            errorData.put("originalQuery", session.getOriginalQuery());
            errorData.put("errorMessage", error.getMessage());
            errorData.put("errorType", error.getClass().getSimpleName());
            errorData.put("timestamp", LocalDateTime.now().toString());
            errorData.put("duration", session.getDuration());
            errorData.put("detectedIntent", session.getDetectedIntent());
            errorData.put("targetAgent", session.getTargetAgent());
            errorData.put("severity", "ERROR");
            
            // Add stack trace for debugging (first 5 lines)
            StackTraceElement[] stackTrace = error.getStackTrace();
            List<String> truncatedStack = new ArrayList<>();
            for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
                truncatedStack.add(stackTrace[i].toString());
            }
            errorData.put("stackTrace", truncatedStack);
            
            return errorData;
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
            logMessage("🎯 Activating AMCP CloudEvents-Compliant Orchestrator Agent...");
            lifecycleState = AgentLifecycle.ACTIVE;
            
            // Activate agent registry
            agentRegistry.onActivate();
            
            // Subscribe to CloudEvents-compliant orchestration events
            subscribe("io.amcp.orchestrator.request.**");
            subscribe("io.amcp.orchestrator.analyze.**");
            subscribe("io.amcp.orchestrator.response");
            subscribe("io.amcp.agent.response.**");
            subscribe("io.amcp.task.response.**");
            subscribe("io.amcp.agent.join");
            subscribe("io.amcp.agent.leave");
            
            // Backward compatibility subscriptions
            subscribe("orchestrator.request.**");
            subscribe("orchestrator.response");
            subscribe("agent.response.**");
            
            // Publish Agent Join event
            publishAgentJoinEvent();
            
            logMessage("🎯 CloudEvents-Compliant Orchestrator Agent activated successfully");
            logMessage("🤖 TinyLlama integration ready with structured JSON responses");
            logMessage("☁️ CloudEvents 1.0 specification compliance enabled");
            
        } catch (Exception e) {
            logMessage("❌ Error activating CloudEvents Orchestrator Agent: " + e.getMessage());
            lifecycleState = AgentLifecycle.INACTIVE;
        }
    }
    
    @Override
    public void onDeactivate() {
        // Publish Agent Leave event
        publishAgentLeaveEvent("Agent deactivation");
        
        lifecycleState = AgentLifecycle.INACTIVE;
        logMessage("🎯 CloudEvents-Compliant Orchestrator Agent deactivated");
    }
    
    @Override
    public void onDestroy() {
        publishAgentLeaveEvent("Agent destruction");
        
        activeSessions.clear();
        pendingResponses.clear();
        lifecycleState = AgentLifecycle.DESTROYED;
        logMessage("🎯 CloudEvents-Compliant Orchestrator Agent destroyed");
    }
    
    @Override
    public void onBeforeMigration(String destinationContext) {
        logMessage("🎯 Preparing CloudEvents Orchestrator for migration to: " + destinationContext);
        // Save active sessions state with CloudEvents metadata
    }
    
    @Override
    public void onAfterMigration(String sourceContext) {
        logMessage("🎯 Completed CloudEvents Orchestrator migration from: " + sourceContext);
        // Restore session state and re-publish Agent Join event
        publishAgentJoinEvent();
    }
    
    private void publishAgentJoinEvent() {
        // Use CloudEventsAgentLifecycle mixin for standardized lifecycle events
        List<String> capabilities = CloudEventsAgentLifecycle.createStandardCapabilities("orchestrator");
        capabilities.addAll(Arrays.asList(
            "cloudevents-compliance", "structured-responses", "error-handling",
            "correlation-tracking", "enterprise-monitoring"
        ));
        
        Map<String, Object> metadata = CloudEventsAgentLifecycle.createStandardMetadata("orchestrator");
        metadata.put("sourceUri", sourceUri.toString());
        metadata.put("llm-enabled", true);
        metadata.put("enterprise-features", true);
        
        CloudEventsAgentLifecycle.publishAgentJoinEvent(this, AGENT_TYPE, capabilities, metadata);
        logMessage("Published CloudEvents-compliant agent join event");
    }
    
    private void publishAgentLeaveEvent(String reason) {
        // Create session statistics for leave event
        Map<String, Object> sessionStats = new HashMap<>();
        sessionStats.put("totalRequests", totalRequests.get());
        sessionStats.put("successfulRequests", successfulRequests.get());
        sessionStats.put("failedRequests", failedRequests.get());
        sessionStats.put("activeSessions", activeSessions.size());
        sessionStats.put("uptime", System.currentTimeMillis() - startupTime);
        
        CloudEventsAgentLifecycle.publishAgentLeaveEvent(this, reason, sessionStats);
        logMessage("Published CloudEvents-compliant agent leave event: " + reason);
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Validate CloudEvents compliance
                boolean isCompliant = event.isCloudEventsCompliant();
                orchestrationLogger.logCloudEventsValidation(
                    event.getCorrelationId(), event.getId(), isCompliant);
                
                // Handle events based on topic
                switch (event.getTopic()) {
                    case "orchestrator.request.analyze":
                    case "io.amcp.orchestrator.request.analyze":
                        handleAnalyzeRequest(event);
                        break;
                    case "orchestrator.response":
                    case "io.amcp.orchestrator.response":
                        handleAgentResponse(event);
                        break;
                    case "io.amcp.agent.join":
                        handleAgentJoinEvent(event);
                        break;
                    case "io.amcp.agent.leave":
                        handleAgentLeaveEvent(event);
                        break;
                    default:
                        // Handle hierarchical agent response topics
                        if (event.getTopic().startsWith("agent.response.") || 
                            event.getTopic().startsWith("io.amcp.agent.response.") ||
                            event.getTopic().startsWith("io.amcp.task.response.")) {
                            handleAgentResponse(event);
                        } else {
                            logMessage("Unhandled event: " + event.getTopic());
                        }
                        break;
                }
            } catch (Exception e) {
                logMessage("Error handling event " + event.getTopic() + ": " + e.getMessage());
                if (event.getCorrelationId() != null) {
                    errorHandler.handleTaskError(event.getCorrelationId(), "EventHandler", e.getMessage());
                }
            }
        });
    }
    
    /**
     * Enhanced orchestration method with CloudEvents compliance and structured responses
     */
    public CompletableFuture<String> orchestrateRequest(String userQuery, String conversationId) {
        String correlationId = UUID.randomUUID().toString();
        String sessionId = UUID.randomUUID().toString();
        OrchestrationSession session = new OrchestrationSession(sessionId, userQuery, correlationId);
        activeSessions.put(sessionId, session);
        
        totalRequests.incrementAndGet();
        long startTime = System.currentTimeMillis();
        
        orchestrationLogger.logIntentAnalysisStart(correlationId, userQuery, 0);
        
        return agentRegistry.discoverAgents()
            .thenCompose(agents -> {
                orchestrationLogger.logIntentAnalysisStart(correlationId, userQuery, agents.size());
                return intentAnalyzer.analyzeIntent(userQuery, agents, correlationId);
            })
            .thenCompose(analysis -> {
                session.setDetectedIntent(analysis.getIntent());
                session.setTargetAgent(analysis.getTargetAgent());
                session.addMetadata("confidence", analysis.getConfidence());
                session.addMetadata("reasoning", analysis.getReasoning());
                
                // Route to appropriate agent with CloudEvents compliance
                return routeToAgentWithCloudEvents(analysis, session, conversationId);
            })
            .thenCompose(agentResponse -> {
                session.setAgentResponse(agentResponse);
                session.addConversationTurn("agent", agentResponse);
                
                // Format final response using LLM
                return formatFinalResponseWithStructuredPrompt(session, agentResponse);
            })
            .thenApply(finalResponse -> {
                session.setFinalResponse(finalResponse);
                session.addConversationTurn("orchestrator", finalResponse);
                
                // Calculate metrics
                long duration = System.currentTimeMillis() - startTime;
                updateAverageResponseTime(duration);
                successfulRequests.incrementAndGet();
                
                // Clean up
                activeSessions.remove(sessionId);
                orchestrationLogger.logOrchestrationSuccess(correlationId, duration);
                
                return finalResponse;
            })
            .exceptionally(ex -> {
                long duration = System.currentTimeMillis() - startTime;
                errorHandler.handleOrchestrationError(correlationId, sessionId, 
                    ex instanceof Exception ? (Exception) ex : new RuntimeException(ex), session);
                activeSessions.remove(sessionId);
                return "I apologize, but I encountered an issue processing your request. Please try again. (Error ID: " + correlationId + ")";
            });
    }
    
    private CompletableFuture<String> routeToAgentWithCloudEvents(IntentAnalysis analysis, 
                                                                 OrchestrationSession session, 
                                                                 String conversationId) {
        String targetTopic = getCloudEventsAgentRequestTopic(analysis.getTargetAgent());
        String correlationId = analysis.getCorrelationId();
        
        // Prepare CloudEvents-compliant request
        Map<String, Object> request = new HashMap<>();
        request.put("query", session.getOriginalQuery());
        request.put("parameters", analysis.getParameters());
        request.put("conversationId", conversationId);
        request.put("sessionId", session.getSessionId());
        request.put("intent", analysis.getIntent());
        request.put("conversationHistory", session.getConversationHistory());
        request.put("timestamp", LocalDateTime.now().toString());
        request.put("orchestratorId", agentId.toString());
        
        // Create pending response future
        CompletableFuture<String> responsePromise = new CompletableFuture<>();
        pendingResponses.put(correlationId, responsePromise);
        
        // Set timeout
        responsePromise.orTimeout(30, TimeUnit.SECONDS);
        
        // Create CloudEvents-compliant routing event
        Event routingEvent = createCloudEventsCompliantEvent(
            EVENT_TYPE_TASK_REQUEST,
            targetTopic,
            request,
            correlationId
        );
        
        session.addEventId(routingEvent.getId());
        
        orchestrationLogger.logEventPublish(correlationId, "TASK_REQUEST", targetTopic, routingEvent.getId());
        publishEvent(routingEvent);
        
        return responsePromise;
    }
    
    private CompletableFuture<String> formatFinalResponseWithStructuredPrompt(OrchestrationSession session, String agentResponse) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String formattingPrompt = buildStructuredResponseFormattingPrompt(session, agentResponse);
                
                Map<String, Object> params = new HashMap<>();
                params.put("prompt", formattingPrompt);
                params.put("model", "tinyllama");
                params.put("temperature", 0.7);
                params.put("max_tokens", 400);
                params.put("format", "text"); // Text format for user-friendly response
                
                ToolRequest toolRequest = new ToolRequest(
                    "ollama-chat", 
                    params,
                    session.getCorrelationId()
                );
                
                ToolResponse response = ollamaConnector.invoke(toolRequest).get(20, TimeUnit.SECONDS);
                
                if (response.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) response.getData();
                    String formattedResponse = (String) result.get("response");
                    
                    logMessage("✨ [" + session.getCorrelationId() + "] Response formatted by TinyLlama");
                    return formattedResponse;
                } else {
                    logMessage("⚠️ [" + session.getCorrelationId() + "] Response formatting failed, using agent response directly");
                    return agentResponse;
                }
                
            } catch (Exception e) {
                logMessage("❌ [" + session.getCorrelationId() + "] Error formatting response: " + e.getMessage());
                return agentResponse; // Fallback to original response
            }
        });
    }
    
    private String buildStructuredResponseFormattingPrompt(OrchestrationSession session, String agentResponse) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are TinyLlama, formatting responses for users in AMCP v1.5 Enterprise Agent System.\n\n");
        
        prompt.append("CONTEXT:\n");
        prompt.append("User asked: \"").append(session.getOriginalQuery()).append("\"\n");
        prompt.append("Intent detected: ").append(session.getDetectedIntent()).append("\n");
        prompt.append("Agent used: ").append(session.getTargetAgent()).append("\n");
        prompt.append("Confidence: ").append(session.getMetadata().getOrDefault("confidence", "N/A")).append("\n\n");
        
        if (!session.getConversationHistory().isEmpty()) {
            prompt.append("CONVERSATION HISTORY:\n");
            for (Map<String, Object> turn : session.getConversationHistory()) {
                prompt.append("- ").append(turn.get("speaker")).append(": ").append(turn.get("message")).append("\n");
            }
            prompt.append("\n");
        }
        
        prompt.append("AGENT RESPONSE:\n");
        prompt.append(agentResponse).append("\n\n");
        
        prompt.append("FORMATTING REQUIREMENTS:\n");
        prompt.append("- Make the response conversational and user-friendly\n");
        prompt.append("- Keep technical details but explain them clearly\n");
        prompt.append("- Add relevant emojis for visual appeal\n");
        prompt.append("- If it's structured data (weather, stock), present it clearly\n");
        prompt.append("- Maintain accuracy - don't change facts, numbers, or data\n");
        prompt.append("- Keep response concise but complete\n");
        prompt.append("- Acknowledge the context of the conversation\n\n");
        
        prompt.append("Provide the final user-friendly response:");
        
        return prompt.toString();
    }
    
    private Event createCloudEventsCompliantEvent(String eventType, String topic, Object data, String correlationId) {
        Event.Builder eventBuilder = Event.builder()
            .topic(topic)
            .payload(data)
            .correlationId(correlationId)
            .sender(agentId)
            .deliveryOptions(DeliveryOptions.reliable())
            .metadata("ce-specversion", "1.0")
            .metadata("ce-type", eventType)
            .metadata("ce-source", sourceUri.toString())
            .metadata("ce-datacontenttype", "application/json")
            .metadata("amcp-version", VERSION)
            .metadata("amcp-agent-type", AGENT_TYPE);
        
        return eventBuilder.build();
    }
    
    private void updateAverageResponseTime(long duration) {
        long currentAvg = averageResponseTime.get();
        long totalReqs = totalRequests.get();
        
        if (totalReqs > 0) {
            long newAvg = ((currentAvg * (totalReqs - 1)) + duration) / totalReqs;
            averageResponseTime.set(newAvg);
        }
    }
    
    private void handleAnalyzeRequest(Event event) {
        orchestrationLogger.logEventReceived(
            event.getCorrelationId(), "ANALYZE_REQUEST", event.getId(), 
            event.getSender() != null ? event.getSender().toString() : "unknown");
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
                    orchestrationLogger.logEventReceived(correlationId, "AGENT_RESPONSE", 
                        event.getId(), event.getSender() != null ? event.getSender().toString() : "unknown");
                } else {
                    pendingResponse.completeExceptionally(new RuntimeException("Empty response from agent"));
                    errorHandler.handleTaskError(correlationId, "Unknown", "Empty response received");
                }
                
            } catch (Exception e) {
                logMessage("❌ [" + correlationId + "] Error processing agent response: " + e.getMessage());
                pendingResponse.completeExceptionally(e);
                errorHandler.handleTaskError(correlationId, "ResponseHandler", e.getMessage());
            }
        }
    }
    
    private void handleAgentJoinEvent(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            String joinedAgentId = (String) payload.get("agentId");
            String agentType = (String) payload.get("agentType");
            
            orchestrationLogger.logAgentJoin(joinedAgentId, agentType, payload);
            
            // Note: AgentRegistry refresh would be implemented when available
            // agentRegistry.refreshAgentList();
            
        } catch (Exception e) {
            logMessage("Error handling agent join event: " + e.getMessage());
        }
    }
    
    private void handleAgentLeaveEvent(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            String leftAgentId = (String) payload.get("agentId");
            String reason = (String) payload.get("reason");
            
            orchestrationLogger.logAgentLeave(leftAgentId, reason);
            
            // Note: AgentRegistry refresh would be implemented when available
            // agentRegistry.refreshAgentList();
            
        } catch (Exception e) {
            logMessage("Error handling agent leave event: " + e.getMessage());
        }
    }
    
    private String getCloudEventsAgentRequestTopic(String agentName) {
        switch (agentName) {
            case "WeatherAgent":
                return "io.amcp.weather.request";
            case "StockPriceAgent":
                return "io.amcp.stock.request";
            case "TravelPlannerAgent":
                return "io.amcp.travel.request";
            case "EnhancedChatAgent":
                return "io.amcp.chat.request";
            default:
                return "io.amcp.agent.request." + agentName.toLowerCase();
        }
    }
    
    /**
     * Get comprehensive orchestration statistics with CloudEvents metrics
     */
    public Map<String, Object> getOrchestrationStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("agentId", agentId.toString());
        stats.put("agentType", AGENT_TYPE);
        stats.put("version", VERSION);
        stats.put("lifecycleState", lifecycleState);
        stats.put("sourceUri", sourceUri.toString());
        stats.put("activeSessions", activeSessions.size());
        stats.put("pendingResponses", pendingResponses.size());
        stats.put("totalRequests", totalRequests.get());
        stats.put("successfulRequests", successfulRequests.get());
        stats.put("failedRequests", failedRequests.get());
        stats.put("averageResponseTime", averageResponseTime.get());
        stats.put("successRate", calculateSuccessRate());
        stats.put("isCloudEventsCompliant", true);
        stats.put("supportedEventTypes", Arrays.asList(
            EVENT_TYPE_ORCHESTRATOR_REQUEST,
            EVENT_TYPE_ORCHESTRATOR_RESPONSE,
            EVENT_TYPE_ORCHESTRATOR_ERROR,
            EVENT_TYPE_AGENT_JOIN,
            EVENT_TYPE_AGENT_LEAVE,
            EVENT_TYPE_TASK_REQUEST,
            EVENT_TYPE_TASK_RESPONSE,
            EVENT_TYPE_TASK_ERROR
        ));
        return stats;
    }
    
    private double calculateSuccessRate() {
        long total = totalRequests.get();
        if (total == 0) return 0.0;
        return (double) successfulRequests.get() / total * 100.0;
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
        System.out.println("[" + timestamp + "] [CloudEventsOrchestratorAgent] " + message);
    }
}