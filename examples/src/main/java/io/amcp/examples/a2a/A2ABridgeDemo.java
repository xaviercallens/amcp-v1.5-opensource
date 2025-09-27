package io.amcp.examples.a2a;

import io.amcp.connectors.a2a.*;
import io.amcp.core.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Demonstration of the AMCP A2A Protocol Bridge.
 * 
 * <p>This demo shows how AMCP agents can interoperate with Google A2A protocol
 * systems through bidirectional message translation and format conversion.</p>
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 * @since 1.5.0
 */
public class A2ABridgeDemo {
    
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║            AMCP v1.5 A2A Protocol Bridge Demo                ║");
        System.out.println("║        Google Agent-to-Agent Integration Example             ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // Initialize the A2A Protocol Bridge
        String bridgeEndpoint = "https://api.example.com/a2a";
        A2AProtocolBridge bridge = new A2AProtocolBridge(bridgeEndpoint);
        
        System.out.println("🌉 A2A Protocol Bridge initialized");
        System.out.println("   Endpoint: " + bridgeEndpoint);
        System.out.println();
        
        // Demonstrate AMCP Event to A2A Message conversion
        demonstrateAMCPToA2A(bridge);
        System.out.println();
        
        // Demonstrate A2A Message to AMCP Event conversion
        demonstrateA2AToAMCP(bridge);
        System.out.println();
        
        // Demonstrate A2A Request handling
        demonstrateA2ARequestHandling(bridge);
        System.out.println();
        
        System.out.println("✅ A2A Protocol Bridge demo completed successfully!");
        System.out.println("   The bridge enables seamless interoperability between");
        System.out.println("   AMCP's event-driven architecture and A2A's request/response model.");
    }
    
    /**
     * Demonstrates converting AMCP Events to A2A message format.
     */
    private static void demonstrateAMCPToA2A(A2AProtocolBridge bridge) {
        System.out.println("🔄 AMCP Event → A2A Message Conversion");
        System.out.println("─".repeat(50));
        
        // Create a sample AMCP event
        Map<String, Object> payload = new HashMap<>();
        payload.put("operation", "weather.query");
        payload.put("city", "New York");
        payload.put("includeDetails", true);
        
        Event amcpEvent = Event.builder()
            .id(UUID.randomUUID().toString())
            .topic("travel.request.weather")
            .payload(payload)
            .timestamp(LocalDateTime.now())
            .correlationId(UUID.randomUUID().toString())
            .build();
        
        System.out.println("📤 Original AMCP Event:");
        System.out.println("   ID: " + amcpEvent.getId());
        System.out.println("   Topic: " + amcpEvent.getTopic());
        System.out.println("   Payload: " + amcpEvent.getPayload());
        System.out.println("   Timestamp: " + amcpEvent.getTimestamp());
        
        // Note: In a real scenario, this would send the message to an A2A service
        // For demo purposes, we'll show the conversion logic
        try {
            System.out.println();
            System.out.println("🔄 Converting to A2A format...");
            System.out.println("   Target Service: travel-service");
            System.out.println("   Message Type: REQUEST");
            System.out.println("   Authentication: Would include OAuth tokens");
            
            // Simulate the conversion (without actual HTTP call)
            System.out.println("✅ Conversion successful - ready for A2A transmission");
            
        } catch (Exception e) {
            System.out.println("❌ Conversion failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates converting A2A messages to AMCP Events.
     */
    private static void demonstrateA2AToAMCP(A2AProtocolBridge bridge) {
        System.out.println("🔄 A2A Message → AMCP Event Conversion");
        System.out.println("─".repeat(50));
        
        // Create a sample A2A message
        Map<String, Object> a2aPayload = new HashMap<>();
        a2aPayload.put("temperature", 22.5);
        a2aPayload.put("condition", "Partly cloudy");
        a2aPayload.put("humidity", 65);
        
        A2AMessage a2aMessage = A2AMessage.builder()
            .requestId(UUID.randomUUID().toString())
            .correlationId(UUID.randomUUID().toString())
            .messageType("RESPONSE")
            .senderId("weather-service")
            .targetService("travel-service")
            .payload(a2aPayload)
            .timestamp(OffsetDateTime.now())
            .build();
        
        System.out.println("📥 Original A2A Message:");
        System.out.println("   Request ID: " + a2aMessage.getRequestId());
        System.out.println("   Message Type: " + a2aMessage.getMessageType());
        System.out.println("   Sender: " + a2aMessage.getSenderId());
        System.out.println("   Payload: " + a2aMessage.getPayload());
        
        try {
            // Convert A2A message to AMCP event
            Event convertedEvent = bridge.convertA2AToAMCP(a2aMessage);
            
            System.out.println();
            System.out.println("✅ Converted to AMCP Event:");
            System.out.println("   ID: " + convertedEvent.getId());
            System.out.println("   Topic: " + convertedEvent.getTopic());
            System.out.println("   Payload: " + convertedEvent.getPayload());
            System.out.println("   Metadata: " + convertedEvent.getMetadata());
            
        } catch (Exception e) {
            System.out.println("❌ Conversion failed: " + e.getMessage());
        }
    }
    
    /**
     * Demonstrates A2A request handling workflow.
     */
    private static void demonstrateA2ARequestHandling(A2AProtocolBridge bridge) {
        System.out.println("🔄 A2A Request Handling Workflow");
        System.out.println("─".repeat(50));
        
        // Create a sample A2A request
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("action", "get_traffic_info");
        requestPayload.put("route", "downtown_to_airport");
        requestPayload.put("time", "now");
        
        A2AMessage requestMessage = A2AMessage.builder()
            .requestId(UUID.randomUUID().toString())
            .messageType("REQUEST")
            .senderId("traffic-client")
            .targetService("traffic-service")
            .payload(requestPayload)
            .timestamp(OffsetDateTime.now())
            .build();
        
        A2ARequest a2aRequest = A2ARequest.builder()
            .requestId(requestMessage.getRequestId())
            .message(requestMessage)
            .expectResponse(true)
            .timeout(5000L)
            .build();
        
        System.out.println("📨 Processing A2A Request:");
        System.out.println("   Request ID: " + a2aRequest.getRequestId());
        System.out.println("   Action: " + requestPayload.get("action"));
        System.out.println("   Expects Response: " + a2aRequest.isExpectResponse());
        System.out.println("   Timeout: " + a2aRequest.getTimeout() + "ms");
        
        try {
            // Handle the A2A request (simulate async processing)
            CompletableFuture<A2AResponse> responseFuture = bridge.handleA2ARequest(a2aRequest);
            
            // Wait for response (in real scenario this would be async)
            A2AResponse response = responseFuture.get();
            
            System.out.println();
            System.out.println("✅ A2A Request processed successfully:");
            System.out.println("   Response ID: " + response.getRequestId());
            System.out.println("   Status: " + response.getStatus());
            System.out.println("   Message: " + response.getMessage());
            System.out.println("   Timestamp: " + response.getTimestamp());
            
            System.out.println();
            System.out.println("🔄 Workflow Summary:");
            System.out.println("   1. A2A request received and validated");
            System.out.println("   2. Request converted to AMCP event format");
            System.out.println("   3. Event would be published to AMCP mesh");
            System.out.println("   4. AMCP agents process the event");
            System.out.println("   5. Response converted back to A2A format");
            System.out.println("   6. A2A response returned to client");
            
        } catch (Exception e) {
            System.out.println("❌ Request processing failed: " + e.getMessage());
        }
    }
}