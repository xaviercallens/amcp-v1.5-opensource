package io.amcp.a2a;

import io.amcp.core.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Translates between A2A Protocol messages and AMCP events.
 * 
 * Handles bidirectional conversion:
 * - A2A Message → AMCP Event (inbound)
 * - AMCP Event → A2A Message (outbound)
 * 
 * Maintains semantic equivalence between protocols while adapting
 * to each system's conventions.
 */
public class A2AMessageTranslator {
    
    private static final Logger logger = LoggerFactory.getLogger(A2AMessageTranslator.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Converts an A2A message to an AMCP event.
     * 
     * Mapping:
     * - A2A performative → AMCP topic pattern
     * - A2A content → AMCP payload
     * - A2A metadata → AMCP CloudEvents extensions
     */
    public Event a2aToAmcpEvent(A2AMessage a2aMessage) {
        try {
            // Determine AMCP topic from A2A performative and receiver
            String topic = buildAmcpTopic(a2aMessage);
            
            // Create AMCP event with A2A content as payload
            Event event = Event.create(topic, a2aMessage.getContent());
            
            // Transfer A2A metadata to AMCP event extensions
            if (a2aMessage.getMetadata() != null) {
                a2aMessage.getMetadata().forEach((key, value) -> 
                    event.getMetadata().put("a2a." + key, value));
            }
            
            // Add A2A-specific metadata
            event.getMetadata().put("a2a.id", a2aMessage.getId());
            event.getMetadata().put("a2a.sender", a2aMessage.getSender());
            event.getMetadata().put("a2a.performative", a2aMessage.getPerformative());
            
            if (a2aMessage.getConversationId() != null) {
                event.getMetadata().put("a2a.conversation", a2aMessage.getConversationId());
            }
            
            if (a2aMessage.getReplyTo() != null) {
                event.getMetadata().put("a2a.replyTo", a2aMessage.getReplyTo());
            }
            
            logger.debug("Translated A2A {} to AMCP topic: {}", 
                a2aMessage.getPerformative(), topic);
            
            return event;
            
        } catch (Exception e) {
            logger.error("Error translating A2A to AMCP: {}", e.getMessage(), e);
            throw new RuntimeException("A2A translation failed", e);
        }
    }

    /**
     * Converts an AMCP event to an A2A message.
     * 
     * Mapping:
     * - AMCP topic → A2A performative (inferred)
     * - AMCP payload → A2A content
     * - AMCP CloudEvents extensions → A2A metadata
     */
    public A2AMessage amcpEventToA2A(Event event, String targetAgent) {
        try {
            A2AMessage a2aMessage = new A2AMessage();
            
            // Generate A2A message ID
            a2aMessage.setId(UUID.randomUUID().toString());
            
            // Set sender (from event metadata or default)
            String sender = (String) event.getMetadata().getOrDefault("agent.id", "amcp-agent");
            a2aMessage.setSender(sender);
            
            // Set receiver
            a2aMessage.setReceiver(targetAgent);
            
            // Infer performative from AMCP topic
            String performative = inferPerformative(event.getTopic());
            a2aMessage.setPerformative(performative);
            
            // Set content from AMCP payload
            a2aMessage.setContent(event.getData());
            
            // Set content type
            a2aMessage.setContentType("application/json");
            
            // Transfer AMCP metadata to A2A metadata
            Map<String, Object> a2aMetadata = new HashMap<>();
            event.getMetadata().forEach((key, value) -> {
                if (!key.startsWith("a2a.")) {
                    a2aMetadata.put(key, value);
                }
            });
            a2aMessage.setMetadata(a2aMetadata);
            
            // Restore conversation ID if present
            if (event.getMetadata().containsKey("a2a.conversation")) {
                a2aMessage.setConversationId((String) event.getMetadata().get("a2a.conversation"));
            }
            
            logger.debug("Translated AMCP {} to A2A {}", 
                event.getTopic(), performative);
            
            return a2aMessage;
            
        } catch (Exception e) {
            logger.error("Error translating AMCP to A2A: {}", e.getMessage(), e);
            throw new RuntimeException("AMCP translation failed", e);
        }
    }

    /**
     * Builds AMCP topic from A2A message.
     * 
     * Pattern: agent.{receiver}.{performative-lowercase}
     * Examples:
     * - A2A REQUEST to "weather" → "agent.weather.request"
     * - A2A QUERY to "stock" → "agent.stock.query"
     * - A2A INFORM to "logger" → "agent.logger.inform"
     */
    private String buildAmcpTopic(A2AMessage a2aMessage) {
        String receiver = a2aMessage.getReceiver();
        String performative = a2aMessage.getPerformative().toLowerCase();
        
        // Handle receiver being an agent name or topic
        if (receiver != null && !receiver.isEmpty()) {
            return "agent." + receiver + "." + performative;
        } else {
            return "a2a." + performative;
        }
    }

    /**
     * Infers A2A performative from AMCP topic.
     * 
     * Common mappings:
     * - *.request → REQUEST
     * - *.query → QUERY
     * - *.inform → INFORM
     * - *.response → INFORM
     * - Default → ACTION
     */
    private String inferPerformative(String topic) {
        if (topic.endsWith(".request")) {
            return "REQUEST";
        } else if (topic.endsWith(".query")) {
            return "QUERY";
        } else if (topic.endsWith(".inform") || topic.endsWith(".response")) {
            return "INFORM";
        } else if (topic.endsWith(".command")) {
            return "ACTION";
        } else {
            return "ACTION"; // Default
        }
    }
}
