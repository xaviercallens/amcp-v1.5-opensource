package io.amcp.a2a;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * REST endpoint for A2A Protocol messages.
 * 
 * Exposes HTTP endpoints for external agents to communicate with AMCP agents:
 * - POST /a2a/message - Receive A2A messages from external agents
 * - GET /a2a/status - Check A2A gateway status
 * - GET /a2a/conversations - List active conversations
 * 
 * Spec Reference: Quarkus AMCP Extension.md §2.3 (Line 147)
 * "expose an HTTP endpoint (REST or gRPC) for A2A... POST /a2a/message"
 * 
 * Security: Should be protected with OAuth2/JWT in production (§2.5)
 */
@Path("/a2a")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class A2AResource {
    
    private static final Logger logger = LoggerFactory.getLogger(A2AResource.class);
    
    // Will be injected by CDI in Quarkus
    private A2AGatewayAgent gatewayAgent;

    /**
     * Default constructor for CDI.
     */
    public A2AResource() {
    }

    /**
     * Constructor for manual injection (tests).
     */
    public A2AResource(A2AGatewayAgent gatewayAgent) {
        this.gatewayAgent = gatewayAgent;
    }

    /**
     * Receives an A2A message from an external agent.
     * 
     * @param message The A2A message
     * @return Response with status
     */
    @POST
    @Path("/message")
    public Response receiveMessage(A2AMessage message) {
        try {
            logger.info("📨 Received A2A {} message from: {}", 
                message.getPerformative(), message.getSender());
            
            // Validate message
            if (message.getId() == null || message.getSender() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Missing required fields: id, sender"))
                    .build();
            }
            
            // Forward to gateway agent
            if (gatewayAgent != null) {
                gatewayAgent.receiveA2AMessage(message);
                
                return Response.status(Response.Status.ACCEPTED)
                    .entity(Map.of(
                        "status", "accepted",
                        "messageId", message.getId(),
                        "timestamp", System.currentTimeMillis()
                    ))
                    .build();
            } else {
                logger.error("A2A Gateway Agent not initialized");
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "A2A Gateway not available"))
                    .build();
            }
            
        } catch (Exception e) {
            logger.error("Error processing A2A message: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }

    /**
     * Sends an A2A message to an external agent.
     * 
     * @param message The A2A message to send
     * @return Response with status
     */
    @POST
    @Path("/send")
    public Response sendMessage(A2AMessage message) {
        try {
            logger.info("📤 Sending A2A {} message to: {}", 
                message.getPerformative(), message.getReceiver());
            
            // Validate message
            if (message.getReceiver() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Missing required field: receiver"))
                    .build();
            }
            
            // Forward to gateway agent
            if (gatewayAgent != null) {
                gatewayAgent.sendA2AMessage(message);
                
                return Response.ok(Map.of(
                    "status", "sent",
                    "messageId", message.getId(),
                    "timestamp", System.currentTimeMillis()
                )).build();
            } else {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "A2A Gateway not available"))
                    .build();
            }
            
        } catch (Exception e) {
            logger.error("Error sending A2A message: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }

    /**
     * Gets the status of the A2A gateway.
     * 
     * @return Gateway status information
     */
    @GET
    @Path("/status")
    public Response getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "A2A Protocol Bridge");
        status.put("version", "1.6.0");
        status.put("status", gatewayAgent != null ? "active" : "unavailable");
        status.put("timestamp", System.currentTimeMillis());
        
        if (gatewayAgent != null) {
            status.put("conversationCount", gatewayAgent.getConversationState().size());
        }
        
        return Response.ok(status).build();
    }

    /**
     * Lists active conversations.
     * 
     * @return Map of conversation IDs to messages
     */
    @GET
    @Path("/conversations")
    public Response getConversations() {
        if (gatewayAgent != null) {
            return Response.ok(gatewayAgent.getConversationState()).build();
        } else {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(Map.of("error", "A2A Gateway not available"))
                .build();
        }
    }

    /**
     * Sets the gateway agent (called by CDI or manually).
     */
    public void setGatewayAgent(A2AGatewayAgent gatewayAgent) {
        this.gatewayAgent = gatewayAgent;
        logger.info("A2A Gateway Agent connected to REST endpoint");
    }
}
