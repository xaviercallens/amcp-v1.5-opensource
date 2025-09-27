package io.amcp.examples;

import io.amcp.core.*;
import io.amcp.core.impl.SimpleAgentContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * AMCP v1.5 Demo Application
 * 
 * Interactive demonstration of AMCP v1.5 enhanced features including:
 * - CloudEvents 1.0 compliance
 * - Enhanced Agent API with convenience methods
 * - Improved lifecycle management
 * - Multi-content type event handling
 * 
 * @since AMCP 1.5.0
 */
public class AMCP15DemoLauncher {
    
    private static SimpleAgentContext context;
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   AMCP v1.5 Enhanced Features Demo");
        System.out.println("========================================");
        System.out.println();
        
        try {
            // Initialize the agent context
            context = new SimpleAgentContext("demo-context");
            context.start().get(5, TimeUnit.SECONDS);
            
            System.out.println("✓ Agent context started: " + context.getContextId());
            System.out.println();
            
            // Create and activate demo agents
            runDemoScenario();
            
            // Keep running for demo
            System.out.println("Demo running... Press Ctrl+C to stop");
            Thread.currentThread().join();
            
        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (context != null) {
                try {
                    context.shutdown().get(5, TimeUnit.SECONDS);
                    System.out.println("✓ Context shutdown complete");
                } catch (Exception e) {
                    System.err.println("Error during shutdown: " + e.getMessage());
                }
            }
        }
    }
    
    private static void runDemoScenario() throws Exception {
        System.out.println("--- Creating and activating demo agents ---");
        
        // Create first demo agent
        Agent agent1 = context.createAgent("amcp15-demo", "Demo Agent 1").get();
        AgentID agent1Id = agent1.getAgentId();
        System.out.println("✓ Created agent: " + agent1Id);
        
        // Activate the agent
        context.activateAgent(agent1Id).get();
        System.out.println("✓ Activated agent: " + agent1Id);
        System.out.println();
        
        // Wait a moment for activation to complete
        Thread.sleep(1000);
        
        // Create second demo agent to interact with the first
        Agent agent2 = context.createAgent("amcp15-demo", "Demo Agent 2").get();
        AgentID agent2Id = agent2.getAgentId();
        context.activateAgent(agent2Id).get();
        System.out.println("✓ Created and activated second agent: " + agent2Id);
        System.out.println();
        
        // Demonstrate CloudEvents publishing
        System.out.println("--- Demonstrating CloudEvents v1.0 compliance ---");
        demonstrateCloudEvents(agent1);
        
        Thread.sleep(2000);
        
        // Demonstrate inter-agent messaging
        System.out.println("--- Demonstrating enhanced Agent API ---");
        demonstrateAgentMessaging(agent1, agent2);
        
        Thread.sleep(2000);
        
        // Demonstrate broadcast messaging
        System.out.println("--- Demonstrating broadcast messaging ---");
        demonstrateBroadcasting(agent1);
        
        Thread.sleep(2000);
        
        // Demonstrate error handling
        System.out.println("--- Demonstrating enhanced error handling ---");
        demonstrateErrorHandling(agent1);
        
        Thread.sleep(2000);
        
        // Demonstrate lifecycle management
        System.out.println("--- Demonstrating lifecycle management ---");
        demonstrateLifecycleManagement(agent1, agent2);
        
        System.out.println();
        System.out.println("✓ All demo scenarios completed successfully!");
        System.out.println();
    }
    
    private static void demonstrateCloudEvents(Agent agent) throws Exception {
        System.out.println("Publishing CloudEvents with different content types...");
        
        // JSON CloudEvent
        Event jsonEvent = Event.builder()
                .topic("amcp.v15.demo.cloudevents")
                .payload("{ \"type\": \"demo\", \"message\": \"Hello CloudEvents!\", \"timestamp\": \"" + 
                        java.time.Instant.now() + "\" }")
                .sender(agent.getAgentId())
                .dataContentType("application/json")
                .dataSchema("https://schemas.amcp.io/demo/v1.5")
                .metadata("demo-type", "cloudevents")
                .traceId("trace-cloudevents-" + System.currentTimeMillis())
                .build();
        
        agent.publishEvent(jsonEvent).get();
        
        // Validate CloudEvents compliance
        System.out.println("CloudEvents compliance check:");
        System.out.println("  ✓ Is CloudEvents compliant: " + jsonEvent.isCloudEventsCompliant());
        System.out.println("  ✓ CloudEvents ID: " + jsonEvent.getId());
        System.out.println("  ✓ CloudEvents Type: " + jsonEvent.getType());
        System.out.println("  ✓ CloudEvents Source: " + jsonEvent.getSource());
        System.out.println("  ✓ Data Content Type: " + jsonEvent.getDataContentType());
        
        // Demonstrate CloudEvents map format
        System.out.println("CloudEvents format:");
        jsonEvent.toCloudEventsMap().forEach((key, value) -> 
            System.out.println("  " + key + ": " + value));
        
        System.out.println();
    }
    
    private static void demonstrateAgentMessaging(Agent sender, Agent receiver) throws Exception {
        System.out.println("Demonstrating direct agent messaging...");
        
        // Direct message using convenience method
        sender.sendMessage(receiver.getAgentId(), "greeting", 
                          "Hello from " + sender.getAgentId() + "!").get();
        
        // Publish with acknowledgment
        sender.publishEventAndWait("amcp.v15.demo.ack", 
                                  "Message requiring acknowledgment").get();
        
        // Publish different content types using convenience methods
        sender.publishJsonEvent("amcp.v15.demo.json", 
                               "{ \"sender\": \"" + sender.getAgentId() + "\", " +
                               "\"message\": \"JSON message using convenience method\" }");
        
        sender.publishTextEvent("amcp.v15.demo.text", 
                               "Plain text message from " + sender.getAgentId());
        
        System.out.println("✓ Direct messaging demonstrated");
        System.out.println();
    }
    
    private static void demonstrateBroadcasting(Agent agent) throws Exception {
        System.out.println("Demonstrating broadcast messaging...");
        
        // Broadcast to all agents
        agent.broadcastEvent("announcement", 
                           "This is a system-wide announcement from " + agent.getAgentId()).get();
        
        // Broadcast specific event type
        agent.broadcastEvent("demo", 
                           "Demo broadcast event at " + java.time.Instant.now()).get();
        
        System.out.println("✓ Broadcasting demonstrated");
        System.out.println();
    }
    
    private static void demonstrateErrorHandling(Agent agent) throws Exception {
        System.out.println("Demonstrating enhanced error handling...");
        
        // Create an event that might cause an error (invalid JSON payload)
        Event errorEvent = Event.builder()
                .topic("amcp.v15.demo.error-test")
                .payload("{ invalid json content }")
                .sender(agent.getAgentId())
                .dataContentType("application/json")
                .build();
        
        agent.publishEvent(errorEvent).get();
        
        System.out.println("✓ Error handling demonstrated (check agent logs)");
        System.out.println();
    }
    
    private static void demonstrateLifecycleManagement(Agent agent1, Agent agent2) throws Exception {
        System.out.println("Demonstrating lifecycle management...");
        
        // Check agent health
        System.out.println("Agent 1 healthy: " + agent1.isHealthy());
        System.out.println("Agent 2 healthy: " + agent2.isHealthy());
        
        // Demonstrate state transitions
        System.out.println("Deactivating agent 2...");
        context.deactivateAgent(agent2.getAgentId()).get();
        
        Thread.sleep(1000);
        
        System.out.println("Reactivating agent 2...");
        context.activateAgent(agent2.getAgentId()).get();
        
        // Clone an agent
        System.out.println("Cloning agent 1...");
        Agent clonedAgent = context.cloneAgent(agent1.getAgentId()).get();
        System.out.println("✓ Created clone: " + clonedAgent.getAgentId());
        
        context.activateAgent(clonedAgent.getAgentId()).get();
        System.out.println("✓ Activated clone: " + clonedAgent.getAgentId());
        
        System.out.println("✓ Lifecycle management demonstrated");
        System.out.println();
    }
}