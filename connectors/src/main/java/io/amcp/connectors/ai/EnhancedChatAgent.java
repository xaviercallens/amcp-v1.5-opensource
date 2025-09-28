package io.amcp.connectors.ai;

import io.amcp.core.Agent;
import io.amcp.core.AgentContext;
import io.amcp.core.AgentID;
import io.amcp.core.AgentLifecycle;
import io.amcp.core.Event;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * Enhanced AMCP v1.5 Chat Agent with Inter-Agent Communication
 * 
 * This intelligent orchestrator agent provides:
 * - Natural language conversation with OLLAMA integration
 * - Intelligent routing to specialized agents via AMCP protocol
 * - Multi-agent coordination and response synthesis
 * - Context-aware conversation management
 * - A2A pattern implementation for agent-to-agent communication
 */
public class EnhancedChatAgent implements Agent {
    
    private static final String AGENT_TYPE = "ENHANCED_CHAT_ORCHESTRATOR";
    private static final String VERSION = "1.5.0";
    
    private final AgentID agentId;
    private AgentContext context;
    private AgentLifecycle lifecycleState = AgentLifecycle.INACTIVE;
    private final Set<String> subscriptions = new CopyOnWriteArraySet<>();
    
    // Core components
    private final AgentRegistry agentRegistry;
    private final PromptManager promptManager;
    private final OllamaSpringAIConnector ollamaConnector;
    
    // Conversation management
    private final Map<String, ConversationContext> activeConversations = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();
    
    public EnhancedChatAgent() {
        this.agentId = AgentID.named("EnhancedChatAgent");
        this.agentRegistry = new AgentRegistry();
        this.promptManager = new PromptManager(agentRegistry);
        this.ollamaConnector = new OllamaSpringAIConnector();
    }
    
    /**
     * Conversation context with multi-agent coordination
     */
    private static class ConversationContext {
        private final String conversationId;
        private final List<ChatMessage> history = new ArrayList<>();
        private final Map<String, Object> metadata = new HashMap<>();
        private final Map<String, String> agentResponses = new HashMap<>();
        private long lastActivity = System.currentTimeMillis();
        
        public ConversationContext(String conversationId) {
            this.conversationId = conversationId;
        }
        
        public void addMessage(String role, String content, String agentSource) {
            history.add(new ChatMessage(role, content, agentSource, System.currentTimeMillis()));
            lastActivity = System.currentTimeMillis();
        }
        
        public void addAgentResponse(String agentId, String response) {
            agentResponses.put(agentId, response);
        }
        
        // Getters
        public String getConversationId() { return conversationId; }
        public List<ChatMessage> getHistory() { return new ArrayList<>(history); }
        public Map<String, Object> getMetadata() { return metadata; }
        public Map<String, String> getAgentResponses() { return new HashMap<>(agentResponses); }
        public long getLastActivity() { return lastActivity; }
    }
    
    /**
     * Enhanced chat message with agent source tracking
     */
    private static class ChatMessage {
        private final String role;
        private final String content;
        private final String agentSource;
        private final long timestamp;
        
        public ChatMessage(String role, String content, String agentSource, long timestamp) {
            this.role = role;
            this.content = content;
            this.agentSource = agentSource;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getRole() { return role; }
        public String getContent() { return content; }
        public String getAgentSource() { return agentSource; }
        public long getTimestamp() { return timestamp; }
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
        // Set context for sub-components
        agentRegistry.setContext(context);
    }
    
    @Override
    public void onActivate() {
        try {
            logMessage("Activating Enhanced Chat Agent...");
            lifecycleState = AgentLifecycle.ACTIVE;
            
            // Activate registry first
            agentRegistry.onActivate();
            
            // Subscribe to chat and response events
            subscriptions.add("chat.request.**");
            subscriptions.add("chat.response.**");
            subscriptions.add("agent.response.**");
            subscriptions.add("weather.response.**");
            subscriptions.add("travel.response.**");
            subscriptions.add("stock.response.**");
            
            for (String topic : subscriptions) {
                subscribe(topic);
            }
            
            logMessage("Enhanced Chat Agent activated with " + subscriptions.size() + " subscriptions");
            
        } catch (Exception e) {
            logMessage("Failed to activate Enhanced Chat Agent: " + e.getMessage());
            throw new RuntimeException("Enhanced Chat Agent activation failed", e);
        }
    }
    
    @Override
    public void onDeactivate() {
        logMessage("Deactivating Enhanced Chat Agent...");
        lifecycleState = AgentLifecycle.INACTIVE;
        agentRegistry.onDeactivate();
        subscriptions.clear();
        activeConversations.clear();
        pendingRequests.clear();
    }
    
    @Override
    public void onDestroy() {
        logMessage("Destroying Enhanced Chat Agent...");
        agentRegistry.onDestroy();
        subscriptions.clear();
        activeConversations.clear();
        pendingRequests.clear();
        lifecycleState = AgentLifecycle.INACTIVE;
    }
    
    @Override
    public void onBeforeMigration(String destinationContext) {
        logMessage("Preparing for migration to: " + destinationContext);
    }
    
    @Override
    public void onAfterMigration(String sourceContext) {
        logMessage("Completed migration from: " + sourceContext);
    }
    
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
            context.publishEvent(event);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                String topic = event.getTopic();
                logMessage("Processing event: " + topic);
                
                if (topic.startsWith("chat.request.")) {
                    handleChatRequest(event);
                } else if (topic.contains(".response.")) {
                    handleAgentResponse(event);
                }
                
            } catch (Exception e) {
                logMessage("Failed to handle event: " + event.getTopic() + " - " + e.getMessage());
            }
        });
    }
    
    /**
     * Handles incoming chat requests and orchestrates multi-agent responses
     */
    private void handleChatRequest(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = event.getPayload(Map.class);
            String message = (String) payload.get("message");
            String conversationId = (String) payload.getOrDefault("conversationId", "default");
            
            logMessage("Processing chat request: \"" + message + "\"");
            
            // Get or create conversation context
            ConversationContext conversation = activeConversations.computeIfAbsent(
                conversationId, ConversationContext::new);
            
            conversation.addMessage("user", message, "user");
            
            // Route the request using PromptManager
            promptManager.routeRequest(message)
                    .thenAccept(result -> processRoutingResult(result, conversation, event))
                    .exceptionally(ex -> {
                        logMessage("Error in request routing: " + ex.getMessage());
                        handleFallbackResponse(message, conversation, event);
                        return null;
                    });
                    
        } catch (Exception e) {
            logMessage("Error handling chat request: " + e.getMessage());
        }
    }
    
    /**
     * Processes the routing result and delegates to appropriate agents
     */
    private void processRoutingResult(PromptManager.RoutingResult result, 
                                    ConversationContext conversation, Event originalEvent) {
        
        logMessage("Routing result: " + result.getIntent() + " -> " + result.getTargetAgent() + 
                  " (confidence: " + String.format("%.2f", result.getConfidence()) + ")");
        
        String correlationId = UUID.randomUUID().toString();
        
        if ("ChatAgent".equals(result.getTargetAgent())) {
            // Handle with OLLAMA directly
            handleWithOllama(result.getProcessedPrompt(), conversation, originalEvent);
        } else {
            // Delegate to specialized agent
            delegateToAgent(result, conversation, originalEvent, correlationId);
        }
    }
    
    /**
     * Delegates request to specialized agents via AMCP events
     */
    private void delegateToAgent(PromptManager.RoutingResult result, 
                                ConversationContext conversation, 
                                Event originalEvent, String correlationId) {
        
        String targetTopic = getAgentTopic(result.getTargetAgent());
        
        Map<String, Object> request = new HashMap<>();
        request.put("query", result.getProcessedPrompt());
        request.put("parameters", result.getParameters());
        request.put("conversationId", conversation.getConversationId());
        request.put("originalQuery", result.getParameters().get("originalInput"));
        
        // Create pending request
        CompletableFuture<String> pendingResponse = new CompletableFuture<>();
        pendingRequests.put(correlationId, pendingResponse);
        
        // Set timeout for agent response
        pendingResponse.orTimeout(30, TimeUnit.SECONDS)
                .thenAccept(response -> {
                    conversation.addAgentResponse(result.getTargetAgent(), response);
                    sendFinalResponse(conversation, originalEvent, response, result.getTargetAgent());
                })
                .exceptionally(ex -> {
                    logMessage("Agent response timeout or error for " + result.getTargetAgent());
                    handleFallbackResponse((String) result.getParameters().get("originalInput"), 
                                        conversation, originalEvent);
                    return null;
                });
        
        // Send request to target agent
        publishEvent(Event.builder()
                .topic(targetTopic)
                .payload(request)
                .correlationId(correlationId)
                .sender(agentId)
                .build());
        
        logMessage("Delegated request to " + result.getTargetAgent() + " via topic: " + targetTopic);
    }
    
    /**
     * Handles responses from specialized agents
     */
    private void handleAgentResponse(Event event) {
        String correlationId = event.getCorrelationId();
        if (correlationId != null && pendingRequests.containsKey(correlationId)) {
            
            CompletableFuture<String> pendingRequest = pendingRequests.remove(correlationId);
            
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = event.getPayload(Map.class);
                String response = (String) payload.get("response");
                
                if (response != null) {
                    pendingRequest.complete(response);
                    logMessage("Received response from agent: " + event.getTopic());
                } else {
                    pendingRequest.complete("I received a response but couldn't process it properly.");
                }
                
            } catch (Exception e) {
                logMessage("Error processing agent response: " + e.getMessage());
                pendingRequest.complete("I encountered an error processing the agent response.");
            }
        }
    }
    
    /**
     * Handles requests directly with OLLAMA for general chat
     */
    private void handleWithOllama(String message, ConversationContext conversation, Event originalEvent) {
        try {
            // Build context-aware prompt
            String contextualPrompt = buildContextualPrompt(message, conversation);
            
            // Create tool request for OLLAMA
            Map<String, Object> params = new HashMap<>();
            params.put("prompt", contextualPrompt);
            params.put("model", "tinyllama");
            params.put("conversationId", conversation.getConversationId());
            
            io.amcp.tools.ToolRequest toolRequest = new io.amcp.tools.ToolRequest(
                "ollama-chat", 
                params,
                UUID.randomUUID().toString()
            );
            
            ollamaConnector.invoke(toolRequest)
                    .thenAccept(toolResponse -> {
                        if (toolResponse.isSuccess()) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> result = (Map<String, Object>) toolResponse.getData();
                            String response = (String) result.get("response");
                            conversation.addMessage("assistant", response, "OLLAMA");
                            sendFinalResponse(conversation, originalEvent, response, "ChatAgent");
                        } else {
                            logMessage("OLLAMA tool error: " + toolResponse.getErrorMessage());
                            handleFallbackResponse(message, conversation, originalEvent);
                        }
                    })
                    .exceptionally(ex -> {
                        logMessage("OLLAMA processing error: " + ex.getMessage());
                        handleFallbackResponse(message, conversation, originalEvent);
                        return null;
                    });
                    
        } catch (Exception e) {
            logMessage("Error handling with OLLAMA: " + e.getMessage());
            handleFallbackResponse(message, conversation, originalEvent);
        }
    }
    
    /**
     * Builds contextual prompt including conversation history and agent registry info
     */
    private String buildContextualPrompt(String message, ConversationContext conversation) {
        StringBuilder prompt = new StringBuilder();
        
        // Enhanced system prompt with agent routing intelligence
        prompt.append("You are TinyLlama, an AI orchestrator in the AMCP (Agent Mesh Communication Protocol) system. ");
        prompt.append("Your primary role is to intelligently route user requests to specialized agents and synthesize their responses.\n\n");
        
        prompt.append("AVAILABLE AGENTS AND CAPABILITIES:\n");
        prompt.append("• WeatherAgent: Provides real-time weather data for ANY location worldwide using OpenWeatherMap API\n");
        prompt.append("  - Handles queries like: 'weather in [LOCATION]', 'temperature in [CITY]', 'forecast for [PLACE]'\n");
        prompt.append("  - Supports ANY city/location, not just hardcoded ones\n");
        prompt.append("• StockPriceAgent: Provides live financial market data using real-time APIs\n");
        prompt.append("  - Handles queries about ANY stock symbol or company name\n");
        prompt.append("  - Supports: stock prices, market data, trading information\n");
        prompt.append("• TravelPlannerAgent: Intelligent trip planning and travel coordination\n");
        prompt.append("  - Route planning, flight search, accommodation, itineraries\n\n");
        
        prompt.append("ROUTING INTELLIGENCE:\n");
        prompt.append("- For weather queries: ALWAYS route to WeatherAgent, extract location from user query\n");
        prompt.append("- For stock/financial queries: ALWAYS route to StockPriceAgent, extract symbol/company\n");
        prompt.append("- For travel queries: ALWAYS route to TravelPlannerAgent\n");
        prompt.append("- Only handle general conversation directly\n\n");
        
        prompt.append("LOCATION EXTRACTION:\n");
        prompt.append("- Extract ANY city, country, or location mentioned in weather queries\n");
        prompt.append("- Examples: 'weather in Nice' → location: Nice\n");
        prompt.append("- Examples: 'temperature in London, UK' → location: London, UK\n");
        prompt.append("- Examples: 'how's the weather in Tokyo today?' → location: Tokyo\n\n");
        
        prompt.append("STOCK SYMBOL EXTRACTION:\n");
        prompt.append("- Extract ANY company name or stock symbol mentioned\n");
        prompt.append("- Examples: 'Amadeus stock price' → symbol: AMADEUS\n");
        prompt.append("- Examples: 'Apple stock' → symbol: AAPL\n");
        prompt.append("- Examples: 'Tesla shares' → symbol: TSLA\n\n");
        
        // Add recent conversation history
        List<ChatMessage> history = conversation.getHistory();
        if (history.size() > 3) {
            prompt.append("RECENT CONVERSATION:\n");
            history.stream()
                    .skip(Math.max(0, history.size() - 3))
                    .forEach(msg -> prompt.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n"));
            prompt.append("\n");
        }
        
        prompt.append("USER REQUEST: ").append(message).append("\n\n");
        prompt.append("Analyze this request and determine if it should be routed to a specialized agent. ");
        prompt.append("If routing to an agent, identify the key parameters (location for weather, symbol for stocks). ");
        prompt.append("If handling directly, provide a helpful response.");
        
        return prompt.toString();
    }
    
    /**
     * Fallback response when specialized agents are unavailable
     */
    private void handleFallbackResponse(String message, ConversationContext conversation, Event originalEvent) {
        logMessage("Using fallback response for: " + message);
        
        // Try OLLAMA as final fallback
        handleWithOllama("I understand you're asking about: \"" + message + "\". " +
                        "However, I'm currently unable to connect with the specialized agents. " +
                        "Let me try to help you directly with what I know.", 
                        conversation, originalEvent);
    }
    
    /**
     * Sends final response back to the user
     */
    private void sendFinalResponse(ConversationContext conversation, Event originalEvent, 
                                 String response, String sourceAgent) {
        
        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("response", response);
        responsePayload.put("conversationId", conversation.getConversationId());
        responsePayload.put("sourceAgent", sourceAgent);
        responsePayload.put("timestamp", LocalDateTime.now().toString());
        
        publishEvent(Event.builder()
                .topic("chat.response.final")
                .payload(responsePayload)
                .correlationId(originalEvent.getCorrelationId())
                .sender(agentId)
                .build());
        
        logMessage("Sent final response from " + sourceAgent + " for conversation: " + 
                  conversation.getConversationId());
    }
    
    /**
     * Maps agent names to their corresponding AMCP topics
     */
    private String getAgentTopic(String agentName) {
        switch (agentName) {
            case "WeatherAgent":
                return "weather.request";
            case "TravelPlannerAgent":
                return "travel.request";
            case "StockPriceAgent":
                return "stock.request";
            default:
                return "agent.request." + agentName.toLowerCase();
        }
    }
    
    /**
     * Public API for direct chat interaction
     */
    public CompletableFuture<String> chat(String message, String conversationId) {
        Map<String, Object> request = new HashMap<>();
        request.put("message", message);
        request.put("conversationId", conversationId != null ? conversationId : "default");
        
        String correlationId = UUID.randomUUID().toString();
        
        // Create a response future
        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        
        // Publish the request
        publishEvent(Event.builder()
                .topic("chat.request.message")
                .payload(request)
                .correlationId(correlationId)
                .sender(agentId)
                .build());
        
        return responseFuture.orTimeout(60, TimeUnit.SECONDS);
    }
    
    /**
     * Get conversation history
     */
    public List<ChatMessage> getConversationHistory(String conversationId) {
        ConversationContext conversation = activeConversations.get(conversationId);
        return conversation != null ? conversation.getHistory() : new ArrayList<>();
    }
    
    /**
     * Get available agents
     */
    public CompletableFuture<Map<String, String>> getAvailableAgents() {
        return promptManager.getAvailableAgents();
    }
    
    /**
     * Get examples for demonstration
     */
    public Map<String, List<String>> getAgentExamples() {
        return promptManager.getAgentExamples();
    }
    
    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [EnhancedChatAgent] " + message);
    }
}