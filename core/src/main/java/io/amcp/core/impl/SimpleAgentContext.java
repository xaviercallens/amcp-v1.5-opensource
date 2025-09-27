package io.amcp.core.impl;

import io.amcp.core.*;
import io.amcp.messaging.EventBroker;
import io.amcp.messaging.impl.InMemoryEventBroker;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Simple implementation of AgentContext for AMCP v1.5.
 * Provides basic agent lifecycle management and event routing.
 * 
 * @since AMCP 1.5.0
 */
public class SimpleAgentContext implements AgentContext {
    
    private final String contextId;
    private final String contextName;
    private final EventBroker eventBroker;
    private final ConcurrentMap<AgentID, Agent> agents;
    private final ConcurrentMap<AgentID, AgentLifecycle> agentStates;
    private final ConcurrentMap<String, Object> properties;
    private final Map<String, Object> metadata;
    private volatile boolean active = false;
    
    public SimpleAgentContext(String contextId) {
        this.contextId = contextId;
        this.contextName = contextId + "-context";
        this.eventBroker = new InMemoryEventBroker();
        this.agents = new ConcurrentHashMap<>();
        this.agentStates = new ConcurrentHashMap<>();
        this.properties = new ConcurrentHashMap<>();
        this.metadata = new HashMap<>();
        metadata.put("type", "SimpleAgentContext");
        metadata.put("version", "1.5.0");
    }
    
    @Override
    public String getContextId() {
        return contextId;
    }
    
    @Override
    public String getContextName() {
        return contextName;
    }
    
    @Override
    public boolean isActive() {
        return active;
    }
    
    @Override
    public CompletableFuture<Void> publishEvent(Event event) {
        EventBroker.Publisher publisher = eventBroker.createPublisher("context-" + contextId);
        return publisher.publish(event);
    }
    
    @Override
    public void subscribe(AgentID agentId, String topicPattern) {
        Agent agent = agents.get(agentId);
        if (agent != null) {
            EventBroker.Subscriber subscriber = eventBroker.createSubscriber(agentId.toString());
            subscriber.subscribe(topicPattern, event -> agent.handleEvent(event));
        }
    }
    
    @Override
    public void unsubscribe(AgentID agentId, String topicPattern) {
        // In a real implementation, we'd track subscriptions to unsubscribe
        // For simplicity, this is a no-op in the simple implementation
    }
    
    @Override
    public Collection<String> getSubscriptions(AgentID agentId) {
        return Collections.emptyList(); // Simple implementation returns empty
    }
    
    @Override
    public CompletableFuture<Agent> createAgent(String agentType, Object initData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                AgentID agentId = AgentID.random();
                return createAgentInternal(agentId, agentType, initData);
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to create agent of type: " + agentType, e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Agent> createAgent(AgentID agentId, String agentType, Object initData) {
        return CompletableFuture.supplyAsync(() -> createAgentInternal(agentId, agentType, initData));
    }
    
    private Agent createAgentInternal(AgentID agentId, String agentType, Object initData) {
        Agent agent = createAgentInstance(agentType, agentId);
        agents.put(agentId, agent);
        agentStates.put(agentId, AgentLifecycle.INACTIVE);
        agent.onCreate(initData);
        return agent;
    }
    
    private Agent createAgentInstance(String agentType, AgentID agentId) {
        // Simple factory - in production this would use reflection or DI
        switch (agentType.toLowerCase()) {
            case "greeting":
                return new SimpleAgent(agentId, agentType, this);
            case "weather":
                return new SimpleAgent(agentId, agentType, this);
            default:
                return new SimpleAgent(agentId, agentType, this);
        }
    }
    
    @Override
    public CompletableFuture<Void> activateAgent(AgentID agentId) {
        return CompletableFuture.runAsync(() -> {
            Agent agent = agents.get(agentId);
            if (agent != null) {
                AgentLifecycle previousState = agentStates.put(agentId, AgentLifecycle.ACTIVE);
                agent.onActivate();
                if (agent instanceof SimpleAgent) {
                    ((SimpleAgent) agent).onStateChange(previousState, AgentLifecycle.ACTIVE);
                }
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> deactivateAgent(AgentID agentId) {
        return CompletableFuture.runAsync(() -> {
            Agent agent = agents.get(agentId);
            if (agent != null) {
                AgentLifecycle previousState = agentStates.put(agentId, AgentLifecycle.INACTIVE);
                agent.onDeactivate();
                if (agent instanceof SimpleAgent) {
                    ((SimpleAgent) agent).onStateChange(previousState, AgentLifecycle.INACTIVE);
                }
            }
        });
    }
    
    @Override
    public CompletableFuture<Agent> cloneAgent(AgentID sourceAgentId) {
        return CompletableFuture.supplyAsync(() -> {
            Agent sourceAgent = agents.get(sourceAgentId);
            if (sourceAgent != null) {
                AgentID cloneId = AgentID.random(sourceAgentId.getNamespace());
                Agent clone = createAgentInstance(sourceAgent.getAgentType(), cloneId);
                agents.put(cloneId, clone);
                agentStates.put(cloneId, AgentLifecycle.INACTIVE);
                
                sourceAgent.onBeforeClone(cloneId);
                clone.restoreState(sourceAgent.captureState());
                sourceAgent.onAfterClone(clone);
                
                return clone;
            }
            throw new RuntimeException("Source agent not found: " + sourceAgentId);
        });
    }
    
    @Override
    public CompletableFuture<Void> dispatchAgent(AgentID agentId, String destinationContext) {
        return CompletableFuture.runAsync(() -> {
            Agent agent = agents.get(agentId);
            if (agent != null) {
                agent.onBeforeDispatch(destinationContext);
                // In a real implementation, this would serialize and send the agent
                // For now, just remove from this context
                agents.remove(agentId);
                agentStates.remove(agentId);
            }
        });
    }
    
    @Override
    public CompletableFuture<Agent> receiveAgent(AgentID agentId, String agentType, 
                                               Object agentState, String sourceContext) {
        return CompletableFuture.supplyAsync(() -> {
            Agent agent = createAgentInstance(agentType, agentId);
            agents.put(agentId, agent);
            agentStates.put(agentId, AgentLifecycle.INACTIVE);
            agent.restoreState(agentState);
            agent.onArrival(sourceContext);
            return agent;
        });
    }
    
    @Override
    public CompletableFuture<Void> destroyAgent(AgentID agentId) {
        return CompletableFuture.runAsync(() -> {
            Agent agent = agents.remove(agentId);
            if (agent != null) {
                agentStates.put(agentId, AgentLifecycle.TERMINATED);
                agent.onDestroy();
                agentStates.remove(agentId);
            }
        });
    }
    
    @Override
    public Agent getAgent(AgentID agentId) {
        return agents.get(agentId);
    }
    
    @Override
    public Collection<Agent> getAllAgents() {
        return new ArrayList<>(agents.values());
    }
    
    @Override
    public Collection<Agent> getAgentsByType(String agentType) {
        return agents.values().stream()
                .filter(agent -> agent.getAgentType().equals(agentType))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    @Override
    public CompletableFuture<Void> sendControlEvent(ControlEvent controlEvent) {
        return CompletableFuture.runAsync(() -> {
            Agent targetAgent = agents.get(controlEvent.getTargetAgent());
            if (targetAgent != null) {
                targetAgent.handleControlEvent(controlEvent);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(() -> {
            active = true;
        });
    }
    
    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            active = false;
            // Deactivate all agents
            agents.keySet().forEach(this::deactivateAgent);
        });
    }
    
    @Override
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    @Override
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    @Override
    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }
    
    public boolean containsAgent(AgentID agentId) {
        return agents.containsKey(agentId);
    }
}