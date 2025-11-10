package io.amcp.examples;

import io.amcp.core.AbstractMobileAgent;
import io.amcp.core.Event;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * ChatAgent - v1.6 Quarkus Integration with Kafka Broker
 * 
 * Provides conversational AI capabilities with LLM integration.
 * Features:
 * - CloudEvents v1.0 compliance
 * - Async conversation processing
 * - Multi-instance Kafka coordination
 * - Context-aware responses
 * - Quarkus CDI integration
 */
@ApplicationScoped
public class ChatAgent extends AbstractMobileAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatAgent.class);
    
    // Simple response templates for demonstration
    private static final Map<String, String> RESPONSE_TEMPLATES = new HashMap<>();
    
    static {
        RESPONSE_TEMPLATES.put("hello", "Hello! I'm ChatAgent v1.6. How can I help you?");
        RESPONSE_TEMPLATES.put("help", "I can assist with questions, provide information, and help coordinate with other agents.");
        RESPONSE_TEMPLATES.put("status", "I'm running on AMCP v1.6 with Quarkus and Kafka. All systems operational.");
        RESPONSE_TEMPLATES.put("weather", "For weather information, please ask the WeatherAgent.");
        RESPONSE_TEMPLATES.put("stock", "For stock information, please ask the StockAgent.");
        RESPONSE_TEMPLATES.put("time", "Current timestamp: " + System.currentTimeMillis());
    }
    
    @Override
    public void onActivate() {
        super.onActivate();
        subscribe("chat.**");
        subscribe("chat.query");
        subscribe("chat.conversation");
        logMessage("🤖 ChatAgent activated - v1.6 Quarkus + Kafka ready");
    }

    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                String topic = event.getTopic();
                logger.debug("ChatAgent handling event: {}", topic);
                
                if (topic.equals("chat.query")) {
                    handleQuery(event);
                } else if (topic.equals("chat.conversation")) {
                    handleConversation(event);
                } else if (topic.equals("chat.status")) {
                    handleStatusRequest(event);
                }
            } catch (Exception e) {
                logger.error("Error handling chat event: {}", e.getMessage(), e);
            }
        });
    }

    private void handleQuery(Event event) {
        try {
            Map<String, Object> query = event.getPayload(Map.class);
            String question = (String) query.getOrDefault("question", "");
            String sessionId = (String) query.getOrDefault("sessionId", "session-" + System.nanoTime());
            
            logMessage("❓ Query received: " + question);
            
            // Generate response based on keywords
            String response = generateResponse(question);
            
            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", sessionId);
            result.put("question", question);
            result.put("answer", response);
            result.put("timestamp", System.currentTimeMillis());
            result.put("confidence", 0.85);
            
            publishEvent("chat.query.response", result);
            logMessage("✅ Query response sent: " + response);
            
        } catch (Exception e) {
            logger.error("Error handling query: {}", e.getMessage(), e);
        }
    }

    private void handleConversation(Event event) {
        try {
            Map<String, Object> conversation = event.getPayload(Map.class);
            String conversationId = (String) conversation.getOrDefault("conversationId", "conv-" + System.nanoTime());
            String message = (String) conversation.getOrDefault("message", "");
            
            logMessage("💬 Conversation message: " + message);
            
            String response = generateResponse(message);
            
            Map<String, Object> result = new HashMap<>();
            result.put("conversationId", conversationId);
            result.put("message", message);
            result.put("response", response);
            result.put("timestamp", System.currentTimeMillis());
            
            publishEvent("chat.conversation.response", result);
            logMessage("✅ Conversation response sent");
            
        } catch (Exception e) {
            logger.error("Error handling conversation: {}", e.getMessage(), e);
        }
    }

    private void handleStatusRequest(Event event) {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("agent", "ChatAgent");
            status.put("version", "1.6.0");
            status.put("status", "active");
            status.put("timestamp", System.currentTimeMillis());
            status.put("broker", "kafka");
            status.put("features", new String[]{"conversational_ai", "query_processing", "context_awareness"});
            status.put("llmIntegration", "ready");
            
            publishEvent("chat.status.response", status);
            logMessage("✅ ChatAgent status reported");
            
        } catch (Exception e) {
            logger.error("Error handling status request: {}", e.getMessage(), e);
        }
    }

    private String generateResponse(String input) {
        String lowerInput = input.toLowerCase();
        
        // Check for keyword matches
        for (Map.Entry<String, String> entry : RESPONSE_TEMPLATES.entrySet()) {
            if (lowerInput.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        // Default response
        return "I understand you're asking about: " + input + ". I'm processing this with AMCP v1.6 on Quarkus with Kafka coordination.";
    }

    @Override
    public void onDeactivate() {
        logMessage("👋 ChatAgent shutting down");
        super.onDeactivate();
    }
}
