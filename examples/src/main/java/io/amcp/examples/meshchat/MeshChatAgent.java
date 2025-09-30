package io.amcp.examples.meshchat;

import io.amcp.core.*;
import io.amcp.mobility.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * MeshChat Agent - Human-to-AI Gateway for AMCP v1.5
 * 
 * This agent serves as the primary interface for human users to interact with
 * the AMCP mesh through natural language conversations. It leverages the
 * Enhanced Orchestrator Agent with TinyLlama integration to coordinate
 * complex multi-agent workflows based on user requests.
 * 
 * Key Features:
 * - Natural language conversation interface
 * - Session management and context persistence
 * - Integration with Enhanced Orchestrator for multi-agent coordination
 * - Support for Travel, Stock, Weather, and other specialized agents
 * - Real-time conversation history and memory
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class MeshChatAgent implements MobileAgent {
    
    private static final String AGENT_TYPE = "meshchat";
    private static final String VERSION = "1.5.0";
    
    private final AgentID agentId;
    private AgentContext context;
    private AgentLifecycle lifecycleState = AgentLifecycle.INACTIVE;
    
    private final Map<String, ChatSession> activeSessions = new ConcurrentHashMap<>();
    private final Set<String> subscribedTopics = new CopyOnWriteArraySet<>();
    
    // Conversation and orchestration topics
    private static final String CHAT_INPUT_TOPIC = "meshchat.user.input";
    private static final String CHAT_OUTPUT_TOPIC = "meshchat.user.output";
    private static final String ORCHESTRATOR_REQUEST_TOPIC = "orchestrator.request";
    private static final String ORCHESTRATOR_RESPONSE_TOPIC = "orchestrator.response";
    
    /**
     * Chat session data for tracking user conversations
     */
    public static class ChatSession {
        private final String sessionId;
        private final String userId;
        private final LocalDateTime startTime;
        private final List<ChatMessage> messages;
        private final Map<String, Object> context;
        private String currentCorrelationId;
        
        public ChatSession(String sessionId, String userId) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.startTime = LocalDateTime.now();
            this.messages = new ArrayList<>();
            this.context = new HashMap<>();
        }
        
        public void addMessage(ChatMessage message) {
            messages.add(message);
        }
        
        public void updateContext(String key, Object value) {
            context.put(key, value);
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public String getUserId() { return userId; }
        public LocalDateTime getStartTime() { return startTime; }
        public List<ChatMessage> getMessages() { return new ArrayList<>(messages); }
        public Map<String, Object> getContext() { return new HashMap<>(context); }
        public String getCurrentCorrelationId() { return currentCorrelationId; }
        public void setCurrentCorrelationId(String correlationId) { this.currentCorrelationId = correlationId; }
    }
    
    /**
     * Individual chat message
     */
    public static class ChatMessage {
        private final String messageId;
        private final String sender; // "user" or "assistant"
        private final String content;
        private final LocalDateTime timestamp;
        private final Map<String, Object> metadata;
        
        public ChatMessage(String sender, String content) {
            this.messageId = UUID.randomUUID().toString();
            this.sender = sender;
            this.content = content;
            this.timestamp = LocalDateTime.now();
            this.metadata = new HashMap<>();
        }
        
        public ChatMessage(String messageId, String sender, String content, Map<String, Object> metadata) {
            this.messageId = messageId;
            this.sender = sender;
            this.content = content;
            this.timestamp = LocalDateTime.now();
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        }
        
        public ChatMessage(String sender, String content, Map<String, Object> metadata) {
            this.messageId = UUID.randomUUID().toString();
            this.sender = sender;
            this.content = content;
            this.timestamp = LocalDateTime.now();
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        }
        
        // Getters
        public String getMessageId() { return messageId; }
        public String getSender() { return sender; }
        public String getContent() { return content; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    }
    
    public MeshChatAgent() {
        this.agentId = new AgentID("meshchat-agent-" + UUID.randomUUID().toString().substring(0, 8));
    }
    
    @Override
    public AgentID getAgentId() {
        return agentId;
    }
    
    @Override
    public AgentContext getContext() {
        return context;
    }
    
    public void setContext(AgentContext context) {
        this.context = context;
    }
    
    @Override
    public AgentLifecycle getLifecycleState() {
        return lifecycleState;
    }
    
    @Override
    public void onActivate() {
        this.lifecycleState = AgentLifecycle.ACTIVE;
        
        logMessage("🤖 MeshChat Agent activating...");
        
        // Subscribe to user input messages
        subscribe(CHAT_INPUT_TOPIC);
        
        // Subscribe to orchestrator responses
        subscribe(ORCHESTRATOR_RESPONSE_TOPIC + ".**");
        
        // Register capabilities with the registry
        registerCapabilities();
        
        logMessage("✅ MeshChat Agent activated successfully");
        logMessage("📡 Listening for user conversations on: " + CHAT_INPUT_TOPIC);
        logMessage("🔗 Connected to orchestrator at: " + ORCHESTRATOR_REQUEST_TOPIC);
    }
    
    @Override
    public void onDeactivate() {
        logMessage("🛑 MeshChat Agent deactivating...");
        
        this.lifecycleState = AgentLifecycle.INACTIVE;
        
        // Save active sessions (in a real implementation, this would persist to storage)
        logMessage("💾 Saving " + activeSessions.size() + " active chat sessions");
        
        // Clear resources
        activeSessions.clear();
        subscribedTopics.clear();
        
        logMessage("👋 MeshChat Agent deactivated");
    }
    
    @Override
    public void onDestroy() {
        this.lifecycleState = AgentLifecycle.DESTROYED;
        onDeactivate();
    }
    
    @Override
    public void onBeforeMigration(String destinationContext) {
        logMessage("� Preparing for migration to: " + destinationContext);
        // Save state for migration
    }
    
    @Override
    public void onAfterMigration(String sourceContext) {
        logMessage("🎯 Migration completed from: " + sourceContext);
        // Restore state after migration
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                String topic = event.getTopic();
                logMessage("📨 Received event: " + topic);
                
                if (topic.startsWith(CHAT_INPUT_TOPIC)) {
                    handleUserInput(event);
                } else if (topic.startsWith(ORCHESTRATOR_RESPONSE_TOPIC)) {
                    handleOrchestratorResponse(event);
                } else {
                    logMessage("⚠️ Unknown event topic: " + topic);
                }
                
            } catch (Exception e) {
                logMessage("❌ Error handling event: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> publishEvent(Event event) {
        if (context != null) {
            return context.publishEvent(event);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> subscribe(String topicPattern) {
        try {
            if (context != null) {
                subscribedTopics.add(topicPattern);
                return context.subscribe(agentId, topicPattern);
            }
        } catch (Exception e) {
            logMessage("❌ Failed to subscribe to " + topicPattern + ": " + e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> unsubscribe(String topicPattern) {
        try {
            if (context != null) {
                subscribedTopics.remove(topicPattern);
                return context.unsubscribe(agentId, topicPattern);
            }
        } catch (Exception e) {
            logMessage("❌ Failed to unsubscribe from " + topicPattern + ": " + e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }
    
    // Mobile Agent methods - basic implementations
    @Override
    public CompletableFuture<Void> dispatch(String destinationContext) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Dispatch not implemented"));
    }
    
    @Override
    public CompletableFuture<AgentID> clone(String destinationContext) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Clone not implemented"));
    }
    
    @Override
    public CompletableFuture<Void> retract(String sourceContext) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Retract not implemented"));
    }
    
    @Override
    public CompletableFuture<Void> migrate(MigrationOptions options) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Migrate not implemented"));
    }
    
    @Override
    public CompletableFuture<List<AgentID>> replicate(String... contexts) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Replicate not implemented"));
    }
    
    @Override
    public CompletableFuture<Void> federateWith(List<AgentID> agents, String federationId) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Federation not implemented"));
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
    public CompletableFuture<MobilityAssessment> assessMobility(String targetContext) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Mobility assessment not implemented"));
    }
    
    @Override
    public void setMobilityStrategy(MobilityStrategy strategy) {
        // Not implemented for this agent
    }
    
    @Override
    public MobilityStrategy getMobilityStrategy() {
        return null;
    }
    
    /**
     * Handles incoming user input messages
     */
    private void handleUserInput(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            
            String sessionId = (String) payload.get("sessionId");
            String userId = (String) payload.get("userId");
            String userMessage = (String) payload.get("message");
            
            if (sessionId == null || userId == null || userMessage == null) {
                logMessage("❌ Invalid user input - missing required fields");
                return;
            }
            
            logMessage("💬 User input [" + sessionId + "]: " + userMessage);
            
            // Get or create chat session
            ChatSession session = getOrCreateSession(sessionId, userId);
            
            // Add user message to session
            ChatMessage userMsg = new ChatMessage("user", userMessage);
            session.addMessage(userMsg);
            
            // Process the message through orchestration
            processUserMessage(session, userMessage, event.getCorrelationId());
            
        } catch (Exception e) {
            logMessage("❌ Error processing user input: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Processes user message by sending it to the orchestrator
     */
    private void processUserMessage(ChatSession session, String userMessage, String correlationId) {
        try {
            // Generate correlation ID if not provided
            if (correlationId == null) {
                correlationId = "meshchat-" + System.currentTimeMillis();
            }
            
            session.setCurrentCorrelationId(correlationId);
            
            // Prepare orchestrator request with enhanced context
            Map<String, Object> orchestratorPayload = new HashMap<>();
            orchestratorPayload.put("query", userMessage);
            orchestratorPayload.put("userId", session.getUserId());
            orchestratorPayload.put("sessionId", session.getSessionId());
            orchestratorPayload.put("conversationHistory", buildConversationContext(session));
            orchestratorPayload.put("userPreferences", session.getContext());
            orchestratorPayload.put("timestamp", LocalDateTime.now().toString());
            
            // Create orchestrator request event
            Event orchestratorRequest = Event.builder()
                .topic(ORCHESTRATOR_REQUEST_TOPIC)
                .payload(orchestratorPayload)
                .correlationId(correlationId)
                .metadata("source", "meshchat")
                .metadata("sessionId", session.getSessionId())
                .metadata("messageType", "user_query")
                .build();
            
            logMessage("🎯 Sending to orchestrator [" + correlationId + "]: " + userMessage);
            
            // Send to orchestrator
            publishEvent(orchestratorRequest);
            
        } catch (Exception e) {
            logMessage("❌ Error processing user message: " + e.getMessage());
            sendErrorResponse(session, "I'm sorry, I encountered an error processing your request.");
        }
    }
    
    /**
     * Handles responses from the orchestrator
     */
    private void handleOrchestratorResponse(Event event) {
        try {
            String correlationId = event.getCorrelationId();
            if (correlationId == null) {
                logMessage("⚠️ Orchestrator response missing correlation ID");
                return;
            }
            
            // Find session by correlation ID
            ChatSession session = findSessionByCorrelationId(correlationId);
            if (session == null) {
                logMessage("⚠️ No session found for correlation ID: " + correlationId);
                return;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            
            String responseText = (String) payload.get("response");
            if (responseText == null) {
                responseText = (String) payload.get("summary");
            }
            if (responseText == null) {
                responseText = "I processed your request, but didn't receive a clear response.";
            }
            
            logMessage("🎤 Orchestrator response [" + correlationId + "]: " + responseText);
            
            // Add assistant response to session
            Map<String, Object> responseMetadata = new HashMap<>();
            responseMetadata.put("correlationId", correlationId);
            responseMetadata.put("executionTime", payload.get("executionTime"));
            responseMetadata.put("agentsInvolved", payload.get("agentsInvolved"));
            
            ChatMessage assistantMsg = new ChatMessage("assistant", responseText, responseMetadata);
            session.addMessage(assistantMsg);
            
            // Send response to user
            sendUserResponse(session, responseText, responseMetadata);
            
        } catch (Exception e) {
            logMessage("❌ Error handling orchestrator response: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Sends response back to the user
     */
    private void sendUserResponse(ChatSession session, String responseText, Map<String, Object> metadata) {
        try {
            Map<String, Object> responsePayload = new HashMap<>();
            responsePayload.put("sessionId", session.getSessionId());
            responsePayload.put("userId", session.getUserId());
            responsePayload.put("message", responseText);
            responsePayload.put("messageType", "assistant_response");
            responsePayload.put("timestamp", LocalDateTime.now().toString());
            responsePayload.put("metadata", metadata);
            
            Event userResponse = Event.builder()
                .topic(CHAT_OUTPUT_TOPIC)
                .payload(responsePayload)
                .correlationId(session.getCurrentCorrelationId())
                .metadata("source", "meshchat")
                .metadata("sessionId", session.getSessionId())
                .build();
            
            publishEvent(userResponse);
            
            logMessage("📤 Sent response to user [" + session.getSessionId() + "]");
            
        } catch (Exception e) {
            logMessage("❌ Error sending user response: " + e.getMessage());
        }
    }
    
    /**
     * Sends error response to user
     */
    private void sendErrorResponse(ChatSession session, String errorMessage) {
        Map<String, Object> errorMetadata = new HashMap<>();
        errorMetadata.put("error", true);
        errorMetadata.put("timestamp", LocalDateTime.now().toString());
        
        sendUserResponse(session, errorMessage, errorMetadata);
    }
    
    /**
     * Gets or creates a chat session
     */
    private ChatSession getOrCreateSession(String sessionId, String userId) {
        return activeSessions.computeIfAbsent(sessionId, id -> {
            logMessage("🆕 Creating new chat session: " + sessionId + " for user: " + userId);
            return new ChatSession(sessionId, userId);
        });
    }
    
    /**
     * Finds session by correlation ID
     */
    private ChatSession findSessionByCorrelationId(String correlationId) {
        return activeSessions.values().stream()
            .filter(session -> correlationId.equals(session.getCurrentCorrelationId()))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Builds conversation context for the orchestrator
     */
    private List<Map<String, Object>> buildConversationContext(ChatSession session) {
        List<Map<String, Object>> context = new ArrayList<>();
        
        // Include recent messages (last 10) for context
        List<ChatMessage> messages = session.getMessages();
        int startIndex = Math.max(0, messages.size() - 10);
        
        for (int i = startIndex; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            Map<String, Object> msgContext = new HashMap<>();
            msgContext.put("sender", msg.getSender());
            msgContext.put("content", msg.getContent());
            msgContext.put("timestamp", msg.getTimestamp().toString());
            context.add(msgContext);
        }
        
        return context;
    }
    
    /**
     * Registers capabilities with the agent registry
     */
    private void registerCapabilities() {
        try {
            Map<String, Object> capabilities = new HashMap<>();
            capabilities.put("meshchat.conversation", "Human-to-AI conversation interface");
            capabilities.put("meshchat.orchestration", "Multi-agent workflow coordination");
            capabilities.put("meshchat.session", "Session and context management");
            capabilities.put("meshchat.memory", "Conversation history and memory");
            
            Map<String, Object> registrationData = new HashMap<>();
            registrationData.put("agentId", agentId.toString());
            registrationData.put("agentType", AGENT_TYPE);
            registrationData.put("version", VERSION);
            registrationData.put("capabilities", capabilities);
            registrationData.put("status", "active");
            registrationData.put("timestamp", LocalDateTime.now().toString());
            
            Event registrationEvent = Event.builder()
                .topic("registry.agent.register")
                .payload(registrationData)
                .correlationId("meshchat-registration-" + System.currentTimeMillis())
                .build();
            
            publishEvent(registrationEvent);
            
            logMessage("📋 Registered capabilities with agent registry");
            
        } catch (Exception e) {
            logMessage("⚠️ Failed to register capabilities: " + e.getMessage());
        }
    }
    
    /**
     * Gets active session statistics
     */
    public Map<String, Object> getSessionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeSessions", activeSessions.size());
        stats.put("totalMessages", activeSessions.values().stream()
            .mapToInt(session -> session.getMessages().size())
            .sum());
        
        Map<String, Integer> userCounts = new HashMap<>();
        activeSessions.values().forEach(session -> {
            userCounts.merge(session.getUserId(), 1, Integer::sum);
        });
        stats.put("activeUsers", userCounts.size());
        stats.put("userBreakdown", userCounts);
        
        return stats;
    }
    
    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [MeshChatAgent] " + message);
    }
}