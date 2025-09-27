package io.amcp.core.impl;

import io.amcp.core.*;

import java.util.concurrent.CompletableFuture;

/**
 * Simple implementation of Agent for AMCP v1.5.
 * Provides basic agent functionality and can be extended for specific use cases.
 * 
 * @since AMCP 1.5.0
 */
public class SimpleAgent implements Agent {
    
    private final AgentID agentId;
    private final String agentType;
    private final AgentContext context;
    private volatile AgentLifecycle lifecycleState;
    
    public SimpleAgent(AgentID agentId, String agentType, AgentContext context) {
        this.agentId = agentId;
        this.agentType = agentType;
        this.context = context;
        this.lifecycleState = AgentLifecycle.INACTIVE;
    }
    
    @Override
    public AgentID getAgentId() {
        return agentId;
    }
    
    @Override
    public String getAgentType() {
        return agentType;
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
    public void onActivate() {
        Agent.super.onActivate();
        this.lifecycleState = AgentLifecycle.ACTIVE;
        logInfo("Agent activated");
    }
    
    @Override
    public void onDeactivate() {
        Agent.super.onDeactivate();
        this.lifecycleState = AgentLifecycle.INACTIVE;
        logInfo("Agent deactivated");
    }
    
    @Override
    public void onDestroy() {
        Agent.super.onDestroy();
        this.lifecycleState = AgentLifecycle.TERMINATED;
        logInfo("Agent destroyed");
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                logInfo("Received event: " + event.getTopic() + " from " + event.getSender());
                
                // Basic event handling - subclasses should override
                processEvent(event);
                
                onEventProcessed(event);
                
            } catch (Exception e) {
                onError(event, e);
            }
        });
    }
    
    /**
     * Override this method to implement custom event processing logic.
     */
    protected void processEvent(Event event) {
        // Default implementation: echo the event
        if (event.getTopic().contains("ping")) {
            publishEvent("pong", "pong from " + getAgentId());
        }
    }
    
    /**
     * Internal method to handle state changes.
     */
    public void onStateChange(AgentLifecycle previousState, AgentLifecycle newState) {
        this.lifecycleState = newState;
        Agent.super.onStateChange(previousState, newState);
    }
}