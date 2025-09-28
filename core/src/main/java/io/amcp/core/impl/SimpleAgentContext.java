package io.amcp.core.impl;

import io.amcp.core.*;
import io.amcp.messaging.EventBroker;
import io.amcp.mobility.MobilityManager;
import io.amcp.mobility.MigrationOptions;
import io.amcp.security.AdvancedSecurityManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple implementation of AgentContext for AMCP v1.5 Enterprise Edition.
 * 
 * <p>This implementation provides basic context management functionality
 * suitable for single-node deployments and testing scenarios.</p>
 */
public class SimpleAgentContext implements AgentContext {
    
    private static final AtomicLong contextIdGenerator = new AtomicLong(1);
    
    private final String contextId;
    private final EventBroker eventBroker;
    private final MobilityManager mobilityManager;
    private final AdvancedSecurityManager securityManager;
    private final ConcurrentHashMap<AgentID, Agent> agents;
    private final ConcurrentHashMap<AgentID, AgentLifecycle> agentStates;
    
    public SimpleAgentContext(EventBroker eventBroker, MobilityManager mobilityManager) {
        this(eventBroker, mobilityManager, null);
    }
    
    public SimpleAgentContext(EventBroker eventBroker, MobilityManager mobilityManager, AdvancedSecurityManager securityManager) {
        this.contextId = "context-" + contextIdGenerator.getAndIncrement();
        this.eventBroker = eventBroker;
        this.mobilityManager = mobilityManager;
        this.securityManager = securityManager;
        this.agents = new ConcurrentHashMap<>();
        this.agentStates = new ConcurrentHashMap<>();
    }
    
    @Override
    public String getContextId() {
        return contextId;
    }
    
    @Override
    public EventBroker getEventBroker() {
        return eventBroker;
    }
    
    @Override
    public MobilityManager getMobilityManager() {
        return mobilityManager;
    }
    
    @Override
    public AdvancedSecurityManager getAdvancedSecurityManager() {
        return securityManager;
    }
    
    @Override
    public CompletableFuture<Void> registerAgent(Agent agent) {
        agents.put(agent.getAgentId(), agent);
        agentStates.put(agent.getAgentId(), AgentLifecycle.INACTIVE);
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> unregisterAgent(AgentID agentId) {
        agents.remove(agentId);
        agentStates.put(agentId, AgentLifecycle.DESTROYED);
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> activateAgent(AgentID agentId) {
        Agent agent = agents.get(agentId);
        if (agent != null) {
            agentStates.put(agentId, AgentLifecycle.ACTIVE);
            return CompletableFuture.runAsync(() -> agent.onActivate());
        }
        return CompletableFuture.failedFuture(new IllegalArgumentException("Agent not found: " + agentId));
    }
    
    @Override
    public CompletableFuture<Void> deactivateAgent(AgentID agentId) {
        Agent agent = agents.get(agentId);
        if (agent != null) {
            agentStates.put(agentId, AgentLifecycle.INACTIVE);
            return CompletableFuture.runAsync(() -> agent.onDeactivate());
        }
        return CompletableFuture.failedFuture(new IllegalArgumentException("Agent not found: " + agentId));
    }
    
    @Override
    public Agent getAgent(AgentID agentId) {
        return agents.get(agentId);
    }
    
    @Override
    public boolean hasAgent(AgentID agentId) {
        return agents.containsKey(agentId);
    }
    
    @Override
    public AgentLifecycle getAgentState(AgentID agentId) {
        return agentStates.get(agentId);
    }
    
    @Override
    public CompletableFuture<Void> routeEvent(Event event) {
        // Simple routing - just publish to event broker
        return eventBroker.publish(event);
    }
    
    @Override
    public CompletableFuture<Void> publishEvent(Event event) {
        return eventBroker.publish(event);
    }
    
    @Override
    public CompletableFuture<Void> subscribe(AgentID agentId, String topicPattern) {
        Agent agent = agents.get(agentId);
        if (agent != null) {
            EventBroker.EventSubscriber subscriber = new AgentEventSubscriber(agent);
            return eventBroker.subscribe(subscriber, topicPattern);
        }
        return CompletableFuture.failedFuture(new IllegalArgumentException("Agent not found: " + agentId));
    }
    
    @Override
    public CompletableFuture<Void> unsubscribe(AgentID agentId, String topicPattern) {
        Agent agent = agents.get(agentId);
        if (agent != null) {
            EventBroker.EventSubscriber subscriber = new AgentEventSubscriber(agent);
            return eventBroker.unsubscribe(subscriber, topicPattern);
        }
        return CompletableFuture.failedFuture(new IllegalArgumentException("Agent not found: " + agentId));
    }
    
    @Override
    public CompletableFuture<Void> migrateAgent(AgentID agentId, String destinationContext, MigrationOptions options) {
        return CompletableFuture.completedFuture(null); // Simple implementation - no migration
    }
    
    @Override
    public CompletableFuture<AgentID> receiveAgent(byte[] serializedAgent, String sourceContext) {
        return CompletableFuture.completedFuture(null); // Simple implementation - no migration
    }
    
    @Override
    public ContextMetrics getMetrics() {
        return new ContextMetrics() {
            @Override
            public int getActiveAgentCount() {
                return (int) agentStates.values().stream()
                    .filter(state -> state == AgentLifecycle.ACTIVE)
                    .count();
            }
            
            @Override
            public long getTotalAgentsCreated() {
                return agents.size();
            }
            
            @Override
            public long getTotalEventsProcessed() {
                return 0; // Simple implementation
            }
            
            @Override
            public double getAverageEventProcessingTime() {
                return 0.0; // Simple implementation
            }
            
            @Override
            public long getFailedOperations() {
                return 0; // Simple implementation
            }
            
            @Override
            public long getUptimeMillis() {
                return System.currentTimeMillis() - startTime;
            }
        };
    }
    
    private final long startTime = System.currentTimeMillis();
    
    @Override
    public CompletableFuture<Void> shutdown() {
        // Deactivate all agents
        CompletableFuture<Void> allDeactivated = CompletableFuture.allOf(
            agents.keySet().stream()
                .map(this::deactivateAgent)
                .toArray(CompletableFuture[]::new)
        );
        return allDeactivated.thenCompose(v -> eventBroker.stop());
    }
    
    /**
     * Simple EventSubscriber that delegates to an Agent.
     */
    private static class AgentEventSubscriber implements EventBroker.EventSubscriber {
        private final Agent agent;
        
        public AgentEventSubscriber(Agent agent) {
            this.agent = agent;
        }
        
        @Override
        public CompletableFuture<Void> handleEvent(Event event) {
            return agent.handleEvent(event);
        }
        
        @Override
        public String getSubscriberId() {
            return agent.getAgentId().toString();
        }
    }
}