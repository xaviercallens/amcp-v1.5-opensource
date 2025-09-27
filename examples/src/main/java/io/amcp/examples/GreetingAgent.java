package io.amcp.examples;

import io.amcp.core.*;
import io.amcp.core.impl.DefaultAgentContext;
import io.amcp.messaging.impl.InMemoryEventBroker;
import java.util.concurrent.CompletableFuture;

/**
 * A simple example agent that demonstrates basic AMCP functionality.
 * This agent responds to greetings and can count events.
 * 
 * @since AMCP 1.0
 */
public class GreetingAgent implements Agent {
    private static final long serialVersionUID = 1L;
    
    private AgentID agentId;
    private AgentContext context;
    private AgentLifecycle lifecycleState = AgentLifecycle.INACTIVE;
    private int eventCount = 0;
    
    @Override
    public AgentID getAgentId() {
        return agentId;
    }
    
    @Override
    public String getAgentType() {
        return "GreetingAgent";
    }
    
    @Override
    public AgentLifecycle getLifecycleState() {
        return lifecycleState;
    }
    
    @Override
    public AgentContext getAgentContext() {
        return context;
    }
    
    @Override
    public void onCreate(Object initData) {
        System.out.println("GreetingAgent " + agentId + " created with data: " + initData);
        lifecycleState = AgentLifecycle.INACTIVE;
    }
    
    @Override
    public void onActivate() {
        System.out.println("GreetingAgent " + agentId + " activated");
        lifecycleState = AgentLifecycle.ACTIVE;
        
        // Subscribe to greeting events
        subscribe("greetings.*");
        subscribe("system.ping");
    }
    
    @Override
    public void onDeactivate() {
        System.out.println("GreetingAgent " + agentId + " deactivated");
        lifecycleState = AgentLifecycle.INACTIVE;
    }
    
    @Override
    public void onDestroy() {
        System.out.println("GreetingAgent " + agentId + " destroyed after processing " + 
                         eventCount + " events");
        lifecycleState = AgentLifecycle.TERMINATED;
    }
    
    @Override
    public void onBeforeDispatch(String destinationContext) {
        System.out.println("GreetingAgent " + agentId + " preparing for migration to " + 
                         destinationContext);
        lifecycleState = AgentLifecycle.MIGRATING;
    }
    
    @Override
    public void onArrival(String previousContext) {
        System.out.println("GreetingAgent " + agentId + " arrived from " + previousContext);
        lifecycleState = AgentLifecycle.ACTIVE;
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            eventCount++;
            String topic = event.getTopic();
            Object payload = event.getPayload();
            
            System.out.println("GreetingAgent " + agentId + " received event on topic '" + 
                             topic + "' with payload: " + payload);
            
            if (topic.startsWith("greetings.")) {
                handleGreeting(event);
            } else if ("system.ping".equals(topic)) {
                handlePing(event);
            }
        });
    }
    
    private void handleGreeting(Event event) {
        String greeting = event.getPayload(String.class);
        if (greeting != null) {
            // Create a response
            String response = "Hello! You said: " + greeting;
            
            // Publish response back
            publishEvent("greetings.responses", response);
        }
    }
    
    private void handlePing(Event event) {
        // Respond to ping with pong
        publishEvent("system.pong", "pong from " + agentId);
    }
    
    // Internal method to set agent context (called by framework)
    public void setAgentContext(AgentContext context, AgentID agentId) {
        this.context = context;
        this.agentId = agentId;
    }
    
    /**
     * Example main method showing how to use the AMCP SDK.
     */
    public static void main(String[] args) throws Exception {
        // Create the event broker (in-memory for this example)
        InMemoryEventBroker broker = new InMemoryEventBroker();
        broker.connect(null).get(); // Connect with default config
        
        // Create agent context
        DefaultAgentContext context = new DefaultAgentContext("example", broker);
        context.start().get();
        
        // Create a greeting agent
        CompletableFuture<Agent> agentFuture = context.createAgent(GreetingAgent.class.getName(), "Hello World!");
        Agent greetingAgent = agentFuture.get();
        
        // Set the context and ID (this would normally be done by the framework)
        if (greetingAgent instanceof GreetingAgent) {
            ((GreetingAgent) greetingAgent).setAgentContext(context, greetingAgent.getAgentId());
        }
        
        // Activate the agent
        context.activateAgent(greetingAgent.getAgentId()).get();
        
        // Create another agent to send greetings
        CompletableFuture<Agent> senderFuture = context.createAgent(GreetingAgent.class.getName(), null);
        Agent senderAgent = senderFuture.get();
        if (senderAgent instanceof GreetingAgent) {
            ((GreetingAgent) senderAgent).setAgentContext(context, senderAgent.getAgentId());
        }
        context.activateAgent(senderAgent.getAgentId()).get();
        
        // Give some time for setup
        Thread.sleep(100);
        
        // Send some test events
        System.out.println("\n--- Sending test events ---");
        
        Event greetingEvent = Event.builder()
            .topic("greetings.hello")
            .payload("Hello from another agent!")
            .sender(senderAgent.getAgentId())
            .build();
        context.publishEvent(greetingEvent).get();
        
        Event pingEvent = Event.builder()
            .topic("system.ping")
            .payload("ping")
            .sender(senderAgent.getAgentId())
            .build();
        context.publishEvent(pingEvent).get();
        
        // Wait a bit for event processing
        Thread.sleep(1000);
        
        // Test control events
        System.out.println("\n--- Testing control events ---");
        ControlEvent pingControl = ControlEvent.create(ControlEvent.Command.PING, greetingAgent.getAgentId());
        context.sendControlEvent(pingControl).get();
        
        // Wait for processing
        Thread.sleep(500);
        
        // Shutdown
        System.out.println("\n--- Shutting down ---");
        context.shutdown().get();
        broker.disconnect().get();
        
        System.out.println("Example completed successfully!");
    }
}