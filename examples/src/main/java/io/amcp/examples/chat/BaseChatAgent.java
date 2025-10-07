package io.amcp.examples.chat;

import io.amcp.core.*;
import io.amcp.mobility.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Base class for all chat agents providing common functionality
 * for multi-turn conversations and message handling.
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public abstract class BaseChatAgent implements MobileAgent {
    
    protected final AgentID agentId;
    protected AgentContext context;
    protected AgentLifecycle lifecycleState = AgentLifecycle.INACTIVE;
    
    // Conversation history tracking
    protected final Map<String, List<ChatMessage>> conversationHistory = new HashMap<>();
    
    protected BaseChatAgent(String agentName) {
        this.agentId = AgentID.named(agentName);
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
    }
    
    @Override
    public void onActivate() {
        lifecycleState = AgentLifecycle.ACTIVE;
        logMessage("Activated");
        subscribeToTopics();
    }
    
    @Override
    public void onDeactivate() {
        lifecycleState = AgentLifecycle.INACTIVE;
        logMessage("Deactivated");
    }
    
    @Override
    public void onDestroy() {
        lifecycleState = AgentLifecycle.DESTROYED;
        logMessage("Destroyed");
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                String topic = event.getTopic();
                logMessage("Processing event: " + topic);
                
                if (topic.startsWith("chat.request") || topic.startsWith("orchestrator.task.chat")) {
                    handleChatRequest(event);
                } else {
                    logMessage("Unhandled topic: " + topic);
                }
                
            } catch (Exception e) {
                logMessage("Error handling event: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Handles incoming chat requests with prior message context
     */
    protected void handleChatRequest(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            
            String query = (String) payload.get("query");
            String conversationId = (String) payload.get("conversationId");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> priorMessages = (List<Map<String, Object>>) payload.get("priorMessages");
            
            if (conversationId == null) {
                conversationId = event.getCorrelationId();
            }
            
            // Update conversation history
            updateConversationHistory(conversationId, priorMessages);
            
            // Generate response based on agent's specialty
            String response = generateResponse(query, conversationId, priorMessages);
            
            // Send structured response
            sendChatResponse(response, event.getCorrelationId(), conversationId);
            
        } catch (Exception e) {
            logMessage("Error processing chat request: " + e.getMessage());
            sendErrorResponse(event.getCorrelationId(), e.getMessage());
        }
    }
    
    /**
     * Abstract method for each agent to implement their specialized response generation
     */
    protected abstract String generateResponse(String query, String conversationId, List<Map<String, Object>> priorMessages);
    
    /**
     * Returns the agent's persona description
     */
    protected abstract String getPersona();
    
    /**
     * Returns the agent's specialty area
     */
    protected abstract String getSpecialty();
    
    /**
     * Subscribe to relevant topics
     */
    protected abstract void subscribeToTopics();
    
    /**
     * Updates conversation history with prior messages
     */
    protected void updateConversationHistory(String conversationId, List<Map<String, Object>> priorMessages) {
        if (priorMessages == null || priorMessages.isEmpty()) {
            return;
        }
        
        List<ChatMessage> history = conversationHistory.computeIfAbsent(conversationId, k -> new ArrayList<>());
        
        for (Map<String, Object> msg : priorMessages) {
            String role = (String) msg.get("role");
            String content = (String) msg.get("content");
            String timestamp = (String) msg.get("timestamp");
            
            if (role != null && content != null) {
                history.add(new ChatMessage(role, content, timestamp));
            }
        }
    }
    
    /**
     * Sends structured chat response
     */
    protected void sendChatResponse(String response, String correlationId, String conversationId) {
        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("response", response);
        responsePayload.put("conversationId", conversationId);
        responsePayload.put("correlationId", correlationId);
        responsePayload.put("sourceAgent", getAgentId().getId());
        responsePayload.put("agentType", getClass().getSimpleName());
        responsePayload.put("specialty", getSpecialty());
        responsePayload.put("timestamp", LocalDateTime.now().toString());
        
        publishEvent(Event.builder()
            .topic("agent.response.chat")
            .payload(responsePayload)
            .correlationId(correlationId)
            .sender(getAgentId())
            .build());
            
        logMessage("Sent chat response for conversation: " + conversationId);
    }
    
    /**
     * Sends error response
     */
    protected void sendErrorResponse(String correlationId, String errorMessage) {
        Map<String, Object> errorPayload = new HashMap<>();
        errorPayload.put("error", "ChatRequestFailed");
        errorPayload.put("errorMessage", errorMessage);
        errorPayload.put("correlationId", correlationId);
        errorPayload.put("sourceAgent", getAgentId().getId());
        
        publishEvent(Event.builder()
            .topic("agent.response.chat")
            .payload(errorPayload)
            .correlationId(correlationId)
            .sender(getAgentId())
            .build());
    }
    
    /**
     * Health check for orchestrator integration
     */
    public boolean isHealthy() {
        return lifecycleState == AgentLifecycle.ACTIVE && context != null;
    }
    
    protected void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [" + getClass().getSimpleName() + "] " + message);
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
    
    @Override
    public void onBeforeMigration(String destinationContext) {}
    
    @Override
    public void onAfterMigration(String sourceContext) {}
    
    // Required MobileAgent interface methods
    @Override
    public CompletableFuture<Void> dispatch(String destinationContext) {
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<AgentID> clone(String destinationContext) {
        return CompletableFuture.completedFuture(AgentID.random());
    }
    
    @Override
    public CompletableFuture<Void> retract(String sourceContext) {
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> migrate(MigrationOptions options) {
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<List<AgentID>> replicate(String... contexts) {
        return CompletableFuture.completedFuture(Arrays.asList(AgentID.random()));
    }
    
    @Override
    public CompletableFuture<Void> federateWith(List<AgentID> agents, String federationId) {
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public MobilityState getMobilityState() {
        return MobilityState.STATIONARY;
    }
    
    @Override
    public List<MigrationEvent> getMigrationHistory() {
        return new ArrayList<>();
    }
    
    @Override
    public CompletableFuture<MobilityAssessment> assessMobility(String destinationContext) {
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public void setMobilityStrategy(MobilityStrategy strategy) {}
    
    @Override
    public MobilityStrategy getMobilityStrategy() {
        return null;
    }
    
    /**
     * Chat message data class
     */
    protected static class ChatMessage {
        public final String role;
        public final String content;
        public final String timestamp;
        
        public ChatMessage(String role, String content, String timestamp) {
            this.role = role;
            this.content = content;
            this.timestamp = timestamp != null ? timestamp : LocalDateTime.now().toString();
        }
    }
}
