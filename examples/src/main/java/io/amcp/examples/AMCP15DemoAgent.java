package io.amcp.examples;

import io.amcp.core.*;
import io.amcp.core.impl.SimpleAgent;
import io.amcp.core.impl.SimpleAgentContext;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * AMCP v1.5 Enhanced Demonstration Agent
 * 
 * Showcases the new v1.5 features including:
 * - CloudEvents 1.0 compliance
 * - Enhanced Agent API with convenience methods
 * - Improved error handling and lifecycle management
 * - Multi-content type support
 * 
 * @since AMCP 1.5.0
 */
public class AMCP15DemoAgent extends SimpleAgent {
    
    private int messageCount = 0;
    
    public AMCP15DemoAgent(AgentID agentId, AgentContext context) {
        super(agentId, "amcp15-demo", context);
    }
    
    @Override
    public void onActivate() {
        super.onActivate();
        
        // Subscribe to various topic patterns using v1.5 convenience methods
        subscribeToAllBroadcasts();
        subscribeToBroadcasts("demo");
        subscribe("amcp.v15.**");
        
        logInfo("AMCP v1.5 Demo Agent activated - showcasing enhanced features!");
        
        // Demonstrate CloudEvents publishing with different content types
        demonstrateCloudEventsFeatures();
    }
    
    @Override
    protected void processEvent(Event event) {
        messageCount++;
        
        logInfo("Processing event #" + messageCount + " - " + event.getTopic());
        
        // Demonstrate CloudEvents compliance checking
        if (event.isCloudEventsCompliant()) {
            logInfo("✓ Event is CloudEvents 1.0 compliant");
            logInfo("  CloudEvents ID: " + event.getId());
            logInfo("  CloudEvents Type: " + event.getType());
            logInfo("  CloudEvents Source: " + event.getSource());
            logInfo("  Content Type: " + event.getDataContentType());
        } else {
            logWarn("⚠ Event is not CloudEvents compliant");
        }
        
        // Handle different event types using pattern matching
        String topic = event.getTopic();
        if (topic.contains("ping")) {
            handlePingRequest(event);
        } else if (topic.contains("demo")) {
            handleDemoEvent(event);
        } else if (topic.contains("broadcast")) {
            handleBroadcastEvent(event);
        } else {
            handleGenericEvent(event);
        }
    }
    
    private void handlePingRequest(Event event) {
        logInfo("Handling ping request");
        
        // Demonstrate different response methods
        if (event.getSender() != null) {
            // Direct response to sender
            sendMessage(event.getSender(), "pong", 
                       "Pong from " + getAgentId() + " at " + Instant.now());
        } else {
            // Broadcast pong response
            broadcastEvent("pong", "Anonymous pong from " + getAgentId());
        }
    }
    
    private void handleDemoEvent(Event event) {
        logInfo("Handling demo event with payload: " + event.getPayload());
        
        // Demonstrate type-safe payload access
        try {
            if (event.getPayload() instanceof String) {
                String stringPayload = event.getPayload(String.class);
                logInfo("String payload received: " + stringPayload);
                
                // Echo back with CloudEvents compliance
                publishCloudEvent("amcp.v15.demo.echo", 
                                "Echo: " + stringPayload, 
                                "text/plain");
            }
        } catch (ClassCastException e) {
            logError("Type casting error", e);
            onError(event, e);
        }
    }
    
    private void handleBroadcastEvent(Event event) {
        logInfo("Received broadcast: " + event.getTopic());
        
        // Demonstrate metadata access
        Object timestamp = event.getMetadata("timestamp");
        if (timestamp != null) {
            logInfo("Broadcast timestamp: " + timestamp);
        }
    }
    
    private void handleGenericEvent(Event event) {
        logInfo("Generic event handler for: " + event.getTopic());
        
        // Demonstrate trace correlation
        if (event.getTraceId() != null) {
            logInfo("Trace ID: " + event.getTraceId());
        }
    }
    
    @Override
    public CompletableFuture<Void> onError(Event event, Throwable error) {
        logError("Error processing event " + event.getMessageId() + 
                ": " + error.getMessage());
        
        // Publish error notification using v1.5 features
        publishJsonEvent("amcp.v15.error", 
                        createErrorReport(event, error));
        
        return super.onError(event, error);
    }
    
    @Override
    public void onEventProcessed(Event event) {
        super.onEventProcessed(event);
        
        // Demonstrate success metrics
        if (messageCount % 10 == 0) {
            logInfo("Processed " + messageCount + " events so far");
            
            // Publish metrics using CloudEvents
            publishCloudEvent("amcp.v15.metrics", 
                            "{ \"processedEvents\": " + messageCount + " }", 
                            "application/json");
        }
    }
    
    private void demonstrateCloudEventsFeatures() {
        // Demonstrate different CloudEvents publishing methods
        
        // JSON event with schema
        Event jsonEvent = Event.builder()
                .topic("amcp.v15.demo.json")
                .payload("{ \"message\": \"Hello AMCP v1.5!\", \"version\": \"1.5.0\" }")
                .sender(getAgentId())
                .dataContentType("application/json")
                .dataSchema("https://schemas.amcp.io/demo/v1.5")
                .metadata("demo", "true")
                .traceId("trace-" + System.currentTimeMillis())
                .build();
        
        publishEvent(jsonEvent).whenComplete((result, throwable) -> {
            if (throwable == null) {
                logInfo("✓ CloudEvents JSON demo published successfully");
            } else {
                logError("✗ Failed to publish CloudEvents JSON demo", throwable);
            }
        });
        
        // Text event using convenience method
        publishTextEvent("amcp.v15.demo.text", 
                        "This is a plain text CloudEvents message");
        
        // XML-style event
        publishCloudEvent("amcp.v15.demo.xml", 
                         "<demo><version>1.5.0</version><message>XML Demo</message></demo>", 
                         "application/xml");
        
        logInfo("✓ Demonstrated CloudEvents features with multiple content types");
    }
    
    private Object createErrorReport(Event event, Throwable error) {
        return "{ \"eventId\": \"" + event.getMessageId() + "\", " +
               "\"errorType\": \"" + error.getClass().getSimpleName() + "\", " +
               "\"errorMessage\": \"" + error.getMessage() + "\", " +
               "\"agentId\": \"" + getAgentId() + "\", " +
               "\"timestamp\": \"" + Instant.now() + "\" }";
    }
    
    @Override
    public boolean isHealthy() {
        // Custom health check logic
        return super.isHealthy() && messageCount < 1000; // Prevent runaway processing
    }
    
    @Override
    public void onStateChange(AgentLifecycle previousState, AgentLifecycle newState) {
        super.onStateChange(previousState, newState);
        
        // Publish state change events
        publishJsonEvent("amcp.v15.lifecycle", 
                        "{ \"agentId\": \"" + getAgentId() + "\", " +
                        "\"previousState\": \"" + previousState + "\", " +
                        "\"newState\": \"" + newState + "\", " +
                        "\"timestamp\": \"" + Instant.now() + "\" }");
    }
}