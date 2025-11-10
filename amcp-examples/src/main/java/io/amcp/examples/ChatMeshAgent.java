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
 * ChatMeshAgent - v1.6 Quarkus Integration with Kafka Broker
 * 
 * Handles distributed chat messaging across the agent mesh.
 * Features:
 * - CloudEvents v1.0 compliance
 * - Async message processing
 * - Multi-instance Kafka coordination
 * - Message routing and delivery
 * - Quarkus CDI integration
 */
@ApplicationScoped
public class ChatMeshAgent extends AbstractMobileAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatMeshAgent.class);
    
    @Override
    public void onActivate() {
        super.onActivate();
        subscribe("chat.**");
        subscribe("chat.message");
        subscribe("chat.broadcast");
        logMessage("💬 ChatMeshAgent activated - v1.6 Quarkus + Kafka ready");
    }

    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                String topic = event.getTopic();
                logger.debug("ChatMeshAgent handling event: {}", topic);
                
                if (topic.equals("chat.message")) {
                    handleDirectMessage(event);
                } else if (topic.equals("chat.broadcast")) {
                    handleBroadcast(event);
                } else if (topic.equals("chat.status")) {
                    handleStatusRequest(event);
                }
            } catch (Exception e) {
                logger.error("Error handling chat event: {}", e.getMessage(), e);
            }
        });
    }

    private void handleDirectMessage(Event event) {
        try {
            Map<String, Object> message = event.getPayload(Map.class);
            String from = (String) message.getOrDefault("from", "unknown");
            String to = (String) message.getOrDefault("to", "broadcast");
            String content = (String) message.getOrDefault("content", "");
            
            logMessage("📨 Direct message from " + from + " to " + to + ": " + content);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "delivered");
            response.put("from", from);
            response.put("to", to);
            response.put("timestamp", System.currentTimeMillis());
            response.put("messageId", System.nanoTime());
            
            publishEvent("chat.message.delivered", response);
            logMessage("✅ Message delivered from " + from + " to " + to);
            
        } catch (Exception e) {
            logger.error("Error handling direct message: {}", e.getMessage(), e);
        }
    }

    private void handleBroadcast(Event event) {
        try {
            Map<String, Object> message = event.getPayload(Map.class);
            String from = (String) message.getOrDefault("from", "unknown");
            String content = (String) message.getOrDefault("content", "");
            
            logMessage("📢 Broadcast from " + from + ": " + content);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "broadcast");
            response.put("from", from);
            response.put("content", content);
            response.put("timestamp", System.currentTimeMillis());
            response.put("recipients", "all");
            
            publishEvent("chat.broadcast.delivered", response);
            logMessage("✅ Broadcast sent from " + from + " to all agents");
            
        } catch (Exception e) {
            logger.error("Error handling broadcast: {}", e.getMessage(), e);
        }
    }

    private void handleStatusRequest(Event event) {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("agent", "ChatMeshAgent");
            status.put("version", "1.6.0");
            status.put("status", "active");
            status.put("timestamp", System.currentTimeMillis());
            status.put("broker", "kafka");
            status.put("features", new String[]{"direct_messaging", "broadcasting", "mesh_coordination"});
            
            publishEvent("chat.status.response", status);
            logMessage("✅ ChatMeshAgent status reported");
            
        } catch (Exception e) {
            logger.error("Error handling status request: {}", e.getMessage(), e);
        }
    }

    @Override
    public void onDeactivate() {
        logMessage("👋 ChatMeshAgent shutting down");
        super.onDeactivate();
    }
}
