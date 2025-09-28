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
 * Enhanced AMCP v1.5 Chat Agent with OrchestratorAgent Integration
 * 
 * This intelligent chat orchestrator provides:
 * - Natural language conversation management
 * - Integration with OrchestratorAgent for intelligent routing
 * - Multi-agent coordination and response synthesis
 * - Context-aware conversation management
 * - A2A pattern implementation for agent-to-agent communication
 * 
 * The EnhancedChatAgent now acts as the user-facing interface while delegating
 * intelligent routing decisions to the OrchestratorAgent powered by TinyLlama.
 */
public class EnhancedChatAgent implements Agent {
    
    private static final String AGENT_TYPE = "ENHANCED_CHAT_INTERFACE";
    private static final String VERSION = "1.5.0";
    
    private final AgentID agentId;
    private AgentContext context;
    private AgentLifecycle lifecycleState = AgentLifecycle.INACTIVE;
    private final Set<String> subscriptions = new CopyOnWriteArraySet<>();
    
    // Core orchestration integration
    private OrchestratorAgent orchestratorAgent;
    private final AgentRegistry agentRegistry;
    
    // Conversation management
    private final Map<String, ConversationContext> activeConversations = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();
    
    public EnhancedChatAgent() {
        this.agentId = AgentID.named("EnhancedChatAgent");
        this.agentRegistry = new AgentRegistry();
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
            logMessage("🎯 Activating Enhanced Chat Agent...");
            lifecycleState = AgentLifecycle.ACTIVE;
            
            // Initialize and activate OrchestratorAgent
            orchestratorAgent = new OrchestratorAgent();
            orchestratorAgent.setContext(context);
            orchestratorAgent.onActivate();
            
            // Activate registry
            agentRegistry.onActivate();
            
            // Subscribe to chat and response events
            subscriptions.add("chat.request.**");
            subscriptions.add("chat.response.**");
            subscriptions.add("orchestrator.response.**");
            
            for (String topic : subscriptions) {
                subscribe(topic);
            }
            
            logMessage("✅ Enhanced Chat Agent activated with OrchestratorAgent integration");
            logMessage("🤖 Ready for intelligent multi-agent conversations");
            
        } catch (Exception e) {
            logMessage("❌ Failed to activate Enhanced Chat Agent: " + e.getMessage());
            throw new RuntimeException("Enhanced Chat Agent activation failed", e);
        }
    }
    
    @Override
    public void onDeactivate() {
        logMessage("🎯 Deactivating Enhanced Chat Agent...");
        lifecycleState = AgentLifecycle.INACTIVE;
        
        // Deactivate OrchestratorAgent
        if (orchestratorAgent != null) {
            orchestratorAgent.onDeactivate();
        }
        
        agentRegistry.onDeactivate();
        subscriptions.clear();
        activeConversations.clear();
        pendingRequests.clear();
    }
    
    @Override
    public void onDestroy() {
        logMessage("🎯 Destroying Enhanced Chat Agent...");
        
        // Destroy OrchestratorAgent
        if (orchestratorAgent != null) {
            orchestratorAgent.onDestroy();
        }
        
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
     * Handles incoming chat requests and orchestrates multi-agent responses using OrchestratorAgent
     */
    private void handleChatRequest(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = event.getPayload(Map.class);
            String message = (String) payload.get("message");
            String conversationId = (String) payload.getOrDefault("conversationId", "default");
            
            logMessage("📝 Processing chat request: \"" + message + "\"");
            
            // Get or create conversation context
            ConversationContext conversation = activeConversations.computeIfAbsent(
                conversationId, ConversationContext::new);
            
            conversation.addMessage("user", message, "user");
            
            // Use OrchestratorAgent for intelligent routing and processing
            orchestratorAgent.orchestrateRequest(message, conversationId)
                    .thenAccept(response -> {
                        logMessage("✅ Received orchestrated response");
                        conversation.addMessage("assistant", response, "OrchestratorAgent");
                        sendFinalResponse(conversation, event, response, "OrchestratorAgent");
                    })
                    .exceptionally(ex -> {
                        logMessage("❌ Error in orchestration: " + ex.getMessage());
                        handleFallbackResponse(message, conversation, event);
                        return null;
                    });
                    
        } catch (Exception e) {
            logMessage("❌ Error handling chat request: " + e.getMessage());
        }
    }
    
    /**
     * Handles responses from the orchestration system
     */
    private void handleAgentResponse(Event event) {
        // This now handles orchestrated responses
        logMessage("📥 Received orchestration response: " + event.getTopic());
    }
    
    /**
     * Fallback response when orchestration fails
     */
    private void handleFallbackResponse(String message, ConversationContext conversation, Event originalEvent) {
        logMessage("⚠️ Using fallback response for: " + message);
        
        String fallbackResponse = "I apologize, but I'm currently experiencing some difficulties processing your request. " +
                                "The intelligent agent orchestration system is temporarily unavailable. " +
                                "Please try again in a moment.";
        
        conversation.addMessage("assistant", fallbackResponse, "FallbackSystem");
        sendFinalResponse(conversation, originalEvent, fallbackResponse, "FallbackSystem");
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
     * Get available agents from the AgentRegistry
     */
    public CompletableFuture<Map<String, String>> getAvailableAgents() {
        return agentRegistry.discoverAgents()
            .thenApply(agents -> {
                Map<String, String> agentMap = new HashMap<>();
                agents.forEach(agent -> agentMap.put(agent.getAgentId(), agent.getDescription()));
                return agentMap;
            });
    }
    
    /**
     * Get orchestration statistics
     */
    public Map<String, Object> getOrchestrationStats() {
        if (orchestratorAgent != null) {
            return orchestratorAgent.getOrchestrationStats();
        }
        return Map.of("status", "OrchestratorAgent not initialized");
    }
    
    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [EnhancedChatAgent] " + message);
    }
}