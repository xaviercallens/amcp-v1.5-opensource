package io.amcp.a2a;

import io.amcp.core.AbstractMobileAgent;
import io.amcp.core.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gateway agent for Agent-to-Agent (A2A) Protocol integration.
 * 
 * This agent acts as a bridge between the A2A protocol and AMCP's internal event mesh:
 * - Receives A2A messages via HTTP/REST and converts them to AMCP events
 * - Listens for AMCP events and can send them as A2A messages to external agents
 * - Maintains conversation state for request/response patterns
 * - Supports all A2A performatives (REQUEST, INFORM, QUERY, ACTION, etc.)
 * 
 * Spec Reference: Quarkus AMCP Extension.md §2.3 (Lines 142-148)
 */
public class A2AGatewayAgent extends AbstractMobileAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(A2AGatewayAgent.class);
    
    private static final String A2A_TOPIC_PREFIX = "a2a";
    private static final String A2A_INBOUND_TOPIC = "a2a.inbound";
    private static final String A2A_OUTBOUND_TOPIC = "a2a.outbound";
    
    private final Map<String, A2AMessage> conversationState = new ConcurrentHashMap<>();
    private final A2AMessageTranslator translator = new A2AMessageTranslator();
    
    @Override
    public void onActivate() {
        super.onActivate();
        
        // Subscribe to A2A inbound topic (messages from REST endpoint)
        subscribe(A2A_INBOUND_TOPIC);
        
        // Subscribe to A2A outbound pattern (messages to send externally)
        subscribe(A2A_OUTBOUND_TOPIC + ".**");
        
        logger.info("🌐 A2A Gateway Agent activated - Ready for protocol bridging");
    }

    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                String topic = event.getTopic();
                
                if (topic.equals(A2A_INBOUND_TOPIC)) {
                    handleInboundA2AMessage(event);
                } else if (topic.startsWith(A2A_OUTBOUND_TOPIC)) {
                    handleOutboundMessage(event);
                } else {
                    logger.warn("Received event on unexpected topic: {}", topic);
                }
                
            } catch (Exception e) {
                logger.error("Error handling A2A event: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Handles inbound A2A messages (from external agents via HTTP).
     * Translates A2A message to AMCP event and publishes to internal mesh.
     */
    private void handleInboundA2AMessage(Event event) {
        try {
            A2AMessage a2aMessage = event.getPayload(A2AMessage.class);
            
            logger.debug("📨 Received A2A message: {} from {}", 
                a2aMessage.getPerformative(), a2aMessage.getSender());
            
            // Track conversation for potential replies
            if (a2aMessage.getConversationId() != null) {
                conversationState.put(a2aMessage.getConversationId(), a2aMessage);
            }
            
            // Translate A2A message to AMCP event
            Event amcpEvent = translator.a2aToAmcpEvent(a2aMessage);
            
            // Publish to internal mesh
            publishEvent(amcpEvent);
            
            logger.debug("✅ Translated A2A -> AMCP and published to: {}", 
                amcpEvent.getTopic());
            
        } catch (Exception e) {
            logger.error("Error handling inbound A2A message: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles outbound messages (from AMCP mesh to external A2A agents).
     * Translates AMCP event to A2A message and sends via HTTP client.
     */
    private void handleOutboundMessage(Event event) {
        try {
            logger.debug("📤 Processing outbound message for A2A protocol");
            
            // Extract target A2A agent endpoint from event metadata
            String targetAgent = (String) event.getMetadata().get("a2a.target");
            if (targetAgent == null) {
                logger.warn("Outbound A2A message missing target agent");
                return;
            }
            
            // Translate AMCP event to A2A message
            A2AMessage a2aMessage = translator.amcpEventToA2A(event, targetAgent);
            
            // TODO: Send A2A message via HTTP client to target agent
            // This will be implemented with Quarkus REST Client
            logger.info("📮 Would send A2A message to: {} (HTTP client pending)", 
                targetAgent);
            
        } catch (Exception e) {
            logger.error("Error handling outbound A2A message: {}", e.getMessage(), e);
        }
    }

    /**
     * Sends an A2A message to an external agent.
     * Called from REST endpoint or other agents.
     */
    public void sendA2AMessage(A2AMessage message) {
        try {
            // Wrap A2A message in AMCP event
            Event event = Event.create(A2A_OUTBOUND_TOPIC, message);
            
            // Add target endpoint to metadata
            event.getMetadata().put("a2a.target", message.getReceiver());
            
            // Publish to outbound topic
            publishEvent(event);
            
        } catch (Exception e) {
            logger.error("Error sending A2A message: {}", e.getMessage(), e);
        }
    }

    /**
     * Receives an A2A message from external agent (called by REST endpoint).
     */
    public void receiveA2AMessage(A2AMessage message) {
        try {
            // Wrap A2A message in AMCP event
            Event event = Event.create(A2A_INBOUND_TOPIC, message);
            
            // Publish to inbound topic for processing
            publishEvent(event);
            
        } catch (Exception e) {
            logger.error("Error receiving A2A message: {}", e.getMessage(), e);
        }
    }

    /**
     * Gets conversation state for debugging/monitoring.
     */
    public Map<String, A2AMessage> getConversationState() {
        return new HashMap<>(conversationState);
    }

    @Override
    public void onDeactivate() {
        conversationState.clear();
        super.onDeactivate();
        logger.info("🔌 A2A Gateway Agent deactivated");
    }
}
