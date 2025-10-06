package io.amcp.connectors.ai;

import io.amcp.core.Agent;
import io.amcp.core.AgentContext;
import io.amcp.core.AgentID;
import io.amcp.core.AgentLifecycle;
import io.amcp.core.Event;
import io.amcp.tools.ToolRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AMCP v1.5 Enterprise AI Chat Agent with Multi-Agent Orchestration.
 * 
 * This intelligent agent provides conversational AI capabilities using OLLAMA and Spring AI,
 * while orchestrating calls to other specialized agents (Travel, Stock, Weather) based on
 * conversation context and user requests.
 * 
 * Features:
 * - Natural language conversation with humans
 * - Intelligent routing to specialized agents
 * - Context-aware agent orchestration  
 * - Conversation history and memory
 * - Multi-turn dialogue support
 * - Integration with OLLAMA local AI models
 * 
 * Agent Orchestration Patterns:
 * - Travel queries → WeatherAgent
 * - Weather requests → WeatherAgent  
 * - Stock/finance → WeatherAgent
 * - General chat → OLLAMA AI models
 * 
 * @author Xavier Callens
 * @version 1.5.0
 * @since 2024-12-19
 */
public class AIChatAgent implements Agent {

    private static final String AGENT_TYPE = "AI_CHAT_ORCHESTRATOR";
    private static final String VERSION = "1.5.0";

    // Agent state
    private AgentID agentId;
    private AgentContext context;
    private volatile boolean active = false;
    
    // OLLAMA integration
    private OllamaSpringAIConnector ollamaConnector;
    
    // Conversation management
    private final Map<String, ConversationContext> conversations = new ConcurrentHashMap<>();
    private final Set<String> subscribedTopics = new CopyOnWriteArraySet<>();
    
    // Intent recognition patterns
    private final Map<Pattern, String> intentPatterns = new HashMap<>();
    
    /**
     * Conversation context for maintaining chat history and state.
     */
    private static class ConversationContext {
        private final String conversationId;
        private final List<ChatMessage> history = new ArrayList<>();
        private final Map<String, Object> metadata = new HashMap<>();
        private long lastActivity = System.currentTimeMillis();
        
        public ConversationContext(String conversationId) {
            this.conversationId = conversationId;
        }
        
        public void addMessage(String role, String content) {
            history.add(new ChatMessage(role, content, System.currentTimeMillis()));
            lastActivity = System.currentTimeMillis();
        }
        
        public List<ChatMessage> getHistory() {
            return new ArrayList<>(history);
        }
        
        @SuppressWarnings("unused")
        public String getConversationId() { return conversationId; }
        @SuppressWarnings("unused")
        public long getLastActivity() { return lastActivity; }
        @SuppressWarnings("unused")
        public Map<String, Object> getMetadata() { return metadata; }
    }
    
    /**
     * Represents a single chat message in conversation history.
     */
    private static class ChatMessage {
        private final String role;
        private final String content;
        private final long timestamp;
        
        public ChatMessage(String role, String content, long timestamp) {
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
        }
        
        public String getRole() { return role; }
        public String getContent() { return content; }
        @SuppressWarnings("unused")
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Constructs an AI Chat Agent with the given agent ID.
     * 
     * @param agentId unique identifier for this agent
     */
    public AIChatAgent(AgentID agentId) {
        this.agentId = agentId;
        this.ollamaConnector = new OllamaSpringAIConnector();
        initializeIntentPatterns();
    }

    @Override
    public AgentID getAgentId() {
        return agentId;
    }

    /**
     * Gets the agent type (not in interface but useful for identification).
     * 
     * @return agent type string
     */
    public String getAgentType() {
        return AGENT_TYPE;
    }

    /**
     * Gets the agent version (not in interface but useful for identification).
     * 
     * @return version string
     */
    public String getVersion() {
        return VERSION;
    }

    /**
     * Sets the agent context (not in interface but needed for initialization).
     * 
     * @param context the agent context
     */
    public void setContext(AgentContext context) {
        this.context = context;
    }

    @Override
    public AgentContext getContext() {
        return context;
    }

    @Override
    public AgentLifecycle getLifecycleState() {
        return active ? AgentLifecycle.ACTIVE : AgentLifecycle.INACTIVE;
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
        subscribedTopics.add(topicPattern);
        if (context != null) {
            return context.subscribe(agentId, topicPattern);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> unsubscribe(String topicPattern) {
        subscribedTopics.remove(topicPattern);
        if (context != null) {
            return context.unsubscribe(agentId, topicPattern);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onActivate() {
        active = true;
        logMessage("Activating AI Chat Agent: " + agentId);
        
        try {
            // Initialize OLLAMA connector
            ollamaConnector.initialize(Map.of()).join();
            
            // Subscribe to relevant topics
            subscribeToTopics();
            
            logMessage("AI Chat Agent activated successfully");
            
        } catch (Exception e) {
            logMessage("Failed to activate AI Chat Agent: " + e.getMessage());
            throw new RuntimeException("Activation failed", e);
        }
    }

    @Override
    public void onDeactivate() {
        active = false;
        logMessage("Deactivating AI Chat Agent: " + agentId);
        
        // Clean up conversations
        conversations.clear();
        subscribedTopics.clear();
        
        // Shutdown OLLAMA connector
        if (ollamaConnector != null) {
            ollamaConnector.shutdown().join();
        }
        
        logMessage("AI Chat Agent deactivated");
    }

    @Override
    public void onDestroy() {
        conversations.clear();
        subscribedTopics.clear();
        if (ollamaConnector != null) {
            ollamaConnector.shutdown();
        }
        logMessage("AI Chat Agent onDestroy callback - resources cleaned up");
    }

    @Override
    public void onBeforeMigration(String destinationContext) {
        logMessage("AI Chat Agent preparing for migration to: " + destinationContext);
        // Save conversation state that could be serialized
    }

    @Override
    public void onAfterMigration(String sourceContext) {
        logMessage("AI Chat Agent completed migration from: " + sourceContext);
        // Restore context-specific resources
        if (ollamaConnector != null) {
            ollamaConnector.initialize(Map.of());
        }
    }

    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        if (!active) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                String topic = event.getTopic();
                logMessage("Processing event: " + topic);

                switch (topic) {
                    case "chat.user.message":
                        handleUserMessage(event);
                        break;
                    
                    case "travel.response":
                        handleTravelResponse(event);
                        break;
                        
                    case "weather.response":
                        handleWeatherResponse(event);
                        break;
                        
                    case "stock.response":
                        handleStockResponse(event);
                        break;
                        
                    default:
                        logMessage("Unhandled event topic: " + topic);
                }

            } catch (Exception e) {
                logMessage("Error handling event: " + e.getMessage());
            }
        });
    }

    /**
     * Handles incoming user chat messages.
     * Determines intent and either responds directly via OLLAMA or orchestrates other agents.
     * 
     * @param event the user message event
     */
    private void handleUserMessage(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            
            String message = (String) payload.get("message");
            String conversationId = (String) payload.get("conversationId");
            String userId = (String) payload.get("userId");

            if (message == null || conversationId == null) {
                logMessage("Invalid user message event - missing required fields");
                return;
            }

            logMessage("Processing user message: " + message.substring(0, Math.min(50, message.length())) + "...");

            // Get or create conversation context
            ConversationContext conversation = conversations.computeIfAbsent(
                conversationId, ConversationContext::new);
            conversation.addMessage("user", message);

            // Detect intent and route accordingly
            String intent = detectIntent(message);
            logMessage("Detected intent: " + intent);

            switch (intent) {
                case "travel":
                    orchestrateTravelRequest(message, conversationId, userId, conversation);
                    break;
                    
                case "weather":
                    orchestrateWeatherRequest(message, conversationId, userId, conversation);
                    break;
                    
                case "stock":
                    orchestrateStockRequest(message, conversationId, userId, conversation);
                    break;
                    
                default:
                    handleGeneralChat(message, conversationId, userId, conversation);
                    break;
            }

        } catch (Exception e) {
            logMessage("Error processing user message: " + e.getMessage());
        }
    }

    /**
     * Orchestrates a travel request by calling the WeatherAgent.
     * 
     * @param message the user's travel request
     * @param conversationId the conversation identifier
     * @param userId the user identifier  
     * @param conversation the conversation context
     */
    private void orchestrateTravelRequest(String message, String conversationId, 
                                        String userId, ConversationContext conversation) {
        logMessage("Orchestrating travel request for conversation: " + conversationId);
        
        // Extract travel details from message (simplified)
        String destination = extractDestination(message);
        String startDate = extractDate(message, "from|start|leaving");
        String endDate = extractDate(message, "to|until|return");
        
        // Create travel request event
        Event travelRequest = Event.builder()
            .topic("travel.request.plan")
            .payload(Map.of(
                "destination", destination != null ? destination : "Paris",
                "startDate", startDate != null ? startDate : "2024-12-25",
                "endDate", endDate != null ? endDate : "2024-12-28",
                "travelers", 2,
                "conversationId", conversationId,
                "userId", userId,
                "originalMessage", message
            ))
            .correlationId("chat-travel-" + conversationId)
            .build();

        context.publishEvent(travelRequest);
        
        // Add to conversation context
        conversation.addMessage("assistant", "I'm planning your travel request. Let me get the details for you...");
    }

    /**
     * Orchestrates a weather request by calling the WeatherAgent.
     * 
     * @param message the user's weather request
     * @param conversationId the conversation identifier
     * @param userId the user identifier
     * @param conversation the conversation context
     */
    private void orchestrateWeatherRequest(String message, String conversationId, 
                                         String userId, ConversationContext conversation) {
        logMessage("Orchestrating weather request for conversation: " + conversationId);
        
        String location = extractLocation(message);
        
        Event weatherRequest = Event.builder()
            .topic("weather.request.current")
            .payload(Map.of(
                "location", location != null ? location : "New York",
                "conversationId", conversationId,
                "userId", userId,
                "originalMessage", message
            ))
            .correlationId("chat-weather-" + conversationId)
            .build();

        context.publishEvent(weatherRequest);
        
        conversation.addMessage("assistant", "Let me check the weather for you...");
    }

    /**
     * Orchestrates a stock request by calling the WeatherAgent.
     * 
     * @param message the user's stock request
     * @param conversationId the conversation identifier
     * @param userId the user identifier
     * @param conversation the conversation context
     */
    private void orchestrateStockRequest(String message, String conversationId, 
                                       String userId, ConversationContext conversation) {
        logMessage("Orchestrating stock request for conversation: " + conversationId);
        
        String symbol = extractStockSymbol(message);
        
        Event stockRequest = Event.builder()
            .topic("stock.request.quote")
            .payload(Map.of(
                "symbol", symbol != null ? symbol : "AAPL",
                "conversationId", conversationId,
                "userId", userId,
                "originalMessage", message
            ))
            .correlationId("chat-stock-" + conversationId)
            .build();

        context.publishEvent(stockRequest);
        
        conversation.addMessage("assistant", "Looking up the stock information for you...");
    }

    /**
     * Handles general chat using OLLAMA AI models.
     * 
     * @param message the user's message
     * @param conversationId the conversation identifier
     * @param userId the user identifier
     * @param conversation the conversation context
     */
    private void handleGeneralChat(String message, String conversationId, 
                                 String userId, ConversationContext conversation) {
        logMessage("Processing general chat for conversation: " + conversationId);
        
        // Build context from conversation history
        StringBuilder contextPrompt = new StringBuilder();
        contextPrompt.append("You are a helpful AI assistant in a multi-agent system. ");
        contextPrompt.append("You can help with general questions and coordinate with specialized agents for travel, weather, and stock information. ");
        contextPrompt.append("Previous conversation:\n");
        
        List<ChatMessage> recentHistory = conversation.getHistory();
        for (int i = Math.max(0, recentHistory.size() - 6); i < recentHistory.size(); i++) {
            ChatMessage msg = recentHistory.get(i);
            contextPrompt.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }
        
        contextPrompt.append("Human: ").append(message).append("\nAssistant: ");
        
        // Call OLLAMA
        ToolRequest ollamaRequest = new ToolRequest("chat", Map.of(
            "prompt", contextPrompt.toString(),
            "conversationId", conversationId,
            "model", "llama3.2"
        ));
        
        ollamaConnector.invoke(ollamaRequest)
            .thenAccept(response -> {
                if (response.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) response.getData();
                    String aiResponse = (String) data.get("response");
                    
                    conversation.addMessage("assistant", aiResponse);
                    
                    // Publish chat response event
                    Event chatResponse = Event.builder()
                        .topic("chat.ai.response")
                        .payload(Map.of(
                            "response", aiResponse,
                            "conversationId", conversationId,
                            "userId", userId,
                            "source", "ollama-ai"
                        ))
                        .correlationId("chat-ai-" + conversationId)
                        .build();
                    
                    context.publishEvent(chatResponse);
                    logMessage("AI response sent for conversation: " + conversationId);
                    
                } else {
                    logMessage("OLLAMA request failed: " + response.getErrorMessage());
                    
                    Event errorResponse = Event.builder()
                        .topic("chat.ai.error")
                        .payload(Map.of(
                            "error", "AI service temporarily unavailable",
                            "conversationId", conversationId,
                            "userId", userId
                        ))
                        .build();
                    
                    context.publishEvent(errorResponse);
                }
            })
            .exceptionally(throwable -> {
                logMessage("OLLAMA request exception: " + throwable.getMessage());
                return null;
            });
    }

    /**
     * Handles travel agent responses and forwards them to the user.
     * 
     * @param event the travel response event
     */
    private void handleTravelResponse(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            
            String conversationId = (String) payload.get("conversationId");
            if (conversationId == null) return;
            
            ConversationContext conversation = conversations.get(conversationId);
            if (conversation == null) return;
            
            // Format travel response for the user
            String travelInfo = formatTravelResponse(payload);
            conversation.addMessage("assistant", travelInfo);
            
            Event chatResponse = Event.builder()
                .topic("chat.ai.response")
                .payload(Map.of(
                    "response", travelInfo,
                    "conversationId", conversationId,
                    "source", "travel-agent"
                ))
                .build();
            
            context.publishEvent(chatResponse);
            logMessage("Travel response forwarded for conversation: " + conversationId);
            
        } catch (Exception e) {
            logMessage("Error handling travel response: " + e.getMessage());
        }
    }

    /**
     * Handles weather agent responses and forwards them to the user.
     * 
     * @param event the weather response event  
     */
    private void handleWeatherResponse(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            
            String conversationId = (String) payload.get("conversationId");
            if (conversationId == null) return;
            
            ConversationContext conversation = conversations.get(conversationId);
            if (conversation == null) return;
            
            String weatherInfo = formatWeatherResponse(payload);
            conversation.addMessage("assistant", weatherInfo);
            
            Event chatResponse = Event.builder()
                .topic("chat.ai.response")
                .payload(Map.of(
                    "response", weatherInfo,
                    "conversationId", conversationId,
                    "source", "weather-agent"
                ))
                .build();
            
            context.publishEvent(chatResponse);
            logMessage("Weather response forwarded for conversation: " + conversationId);
            
        } catch (Exception e) {
            logMessage("Error handling weather response: " + e.getMessage());
        }
    }

    /**
     * Handles stock agent responses and forwards them to the user.
     * 
     * @param event the stock response event
     */
    private void handleStockResponse(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            
            String conversationId = (String) payload.get("conversationId");
            if (conversationId == null) return;
            
            ConversationContext conversation = conversations.get(conversationId);
            if (conversation == null) return;
            
            String stockInfo = formatStockResponse(payload);
            conversation.addMessage("assistant", stockInfo);
            
            Event chatResponse = Event.builder()
                .topic("chat.ai.response")
                .payload(Map.of(
                    "response", stockInfo,
                    "conversationId", conversationId,
                    "source", "stock-agent"
                ))
                .build();
            
            context.publishEvent(chatResponse);
            logMessage("Stock response forwarded for conversation: " + conversationId);
            
        } catch (Exception e) {
            logMessage("Error handling stock response: " + e.getMessage());
        }
    }

    // Helper methods for intent detection and data extraction

    /**
     * Initializes regex patterns for intent recognition.
     */
    private void initializeIntentPatterns() {
        intentPatterns.put(
            Pattern.compile("(?i).*(travel|trip|vacation|flight|hotel|book.*travel|plan.*trip).*"),
            "travel"
        );
        intentPatterns.put(
            Pattern.compile("(?i).*(weather|temperature|forecast|rain|snow|sunny|cloudy).*"),
            "weather"
        );
        intentPatterns.put(
            Pattern.compile("(?i).*(stock|share|price|ticker|market|nasdaq|nyse|investment).*"),
            "stock"
        );
    }

    /**
     * Detects the intent from user message using regex patterns.
     * 
     * @param message the user message
     * @return detected intent or "general"
     */
    private String detectIntent(String message) {
        for (Map.Entry<Pattern, String> entry : intentPatterns.entrySet()) {
            if (entry.getKey().matcher(message).matches()) {
                return entry.getValue();
            }
        }
        return "general";
    }

    /**
     * Extracts destination from travel message.
     * 
     * @param message the travel message
     * @return extracted destination or null
     */
    private String extractDestination(String message) {
        Pattern pattern = Pattern.compile("(?i)(?:to|visit|travel to|go to|trip to)\\s+([A-Za-z\\s]+?)(?:\\s|$|,|\\.|!)");
        Matcher matcher = pattern.matcher(message);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    /**
     * Extracts location from weather message.
     * 
     * @param message the weather message
     * @return extracted location or null
     */
    private String extractLocation(String message) {
        Pattern pattern = Pattern.compile("(?i)(?:in|at|for|weather in|temperature in)\\s+([A-Za-z\\s]+?)(?:\\s|$|,|\\.|!)");
        Matcher matcher = pattern.matcher(message);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    /**
     * Extracts stock symbol from message.
     * 
     * @param message the stock message
     * @return extracted symbol or null
     */
    private String extractStockSymbol(String message) {
        Pattern pattern = Pattern.compile("(?i)(?:stock|price of|shares of|ticker)\\s+([A-Z]{1,5})(?:\\s|$|,|\\.|!)");
        Matcher matcher = pattern.matcher(message);
        return matcher.find() ? matcher.group(1).trim().toUpperCase() : null;
    }

    /**
     * Extracts date from message using pattern.
     * 
     * @param message the message
     * @param pattern the regex pattern
     * @return extracted date or null
     */
    private String extractDate(String message, String pattern) {
        Pattern datePattern = Pattern.compile("(?i)(?:" + pattern + ")\\s+(\\d{4}-\\d{2}-\\d{2}|\\d{2}/\\d{2}/\\d{4})");
        Matcher matcher = datePattern.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * Formats travel response for user display.
     * 
     * @param payload the travel response payload
     * @return formatted travel information
     */
    private String formatTravelResponse(Map<String, Object> payload) {
        // Simplified formatting - in reality would parse the travel plan structure
        return "Here's your travel plan:\n\n" +
               "🏨 **Accommodation**: " + payload.getOrDefault("hotel", "Best Western") + "\n" +
               "✈️ **Flight**: " + payload.getOrDefault("flight", "Available options") + "\n" +
               "📍 **Destination**: " + payload.getOrDefault("destination", "Paris") + "\n" +
               "📅 **Dates**: " + payload.getOrDefault("startDate", "TBD") + " to " + payload.getOrDefault("endDate", "TBD") + "\n\n" +
               "Would you like me to help you with any specific aspects of your trip?";
    }

    /**
     * Formats weather response for user display.
     * 
     * @param payload the weather response payload
     * @return formatted weather information
     */
    private String formatWeatherResponse(Map<String, Object> payload) {
        return "🌤️ **Weather Update**:\n\n" +
               "📍 **Location**: " + payload.getOrDefault("location", "Unknown") + "\n" +
               "🌡️ **Temperature**: " + payload.getOrDefault("temperature", "N/A") + "°C\n" +
               "☁️ **Conditions**: " + payload.getOrDefault("description", "Clear") + "\n" +
               "💧 **Humidity**: " + payload.getOrDefault("humidity", "N/A") + "%\n\n" +
               "Is there anything else you'd like to know about the weather?";
    }

    /**
     * Formats stock response for user display.
     * 
     * @param payload the stock response payload
     * @return formatted stock information
     */
    private String formatStockResponse(Map<String, Object> payload) {
        return "📈 **Stock Information**:\n\n" +
               "🏢 **Symbol**: " + payload.getOrDefault("symbol", "N/A") + "\n" +
               "💰 **Price**: $" + payload.getOrDefault("price", "N/A") + "\n" +
               "📊 **Change**: " + payload.getOrDefault("change", "N/A") + "\n" +
               "📈 **Volume**: " + payload.getOrDefault("volume", "N/A") + "\n\n" +
               "Would you like more detailed analysis or information about other stocks?";
    }

    /**
     * Subscribes to relevant event topics.
     */
    private void subscribeToTopics() {
        String[] topics = {
            "chat.user.message",
            "travel.response",
            "weather.response", 
            "stock.response"
        };

        for (String topic : topics) {
            subscribe(topic);
            logMessage("Subscribed to topic: " + topic);
        }
    }

    /**
     * Logs a message with timestamp (following AMCP logging pattern).
     * 
     * @param message the message to log
     */
    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [AIChatAgent:" + agentId + "] " + message);
    }
}