package io.amcp.core;

import io.amcp.core.*;
import io.amcp.messaging.EventBroker;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Default implementation of AgentContext that provides agent lifecycle management,
 * messaging capabilities, and resource management.
 * 
 * @since AMCP 1.0
 */
public class DefaultAgentContext implements AgentContext {
    
    private final String contextId;
    private final String contextName;
    private final EventBroker eventBroker;
    private final ConcurrentMap<AgentID, Agent> agents = new ConcurrentHashMap<>();
    private final ConcurrentMap<AgentID, AgentMetadata> agentMetadata = new ConcurrentHashMap<>();
    private final ConcurrentMap<AgentID, Set<String>> subscriptions = new ConcurrentHashMap<>();
    private final Map<String, Object> properties = new ConcurrentHashMap<>();
    private final Map<String, Object> metadata = new ConcurrentHashMap<>();
    private volatile boolean active = false;
    private volatile boolean shutdown = false;
    
    private static class AgentMetadata {
        final String agentType;
        final long creationTime;
        final Map<String, Object> properties;
        volatile AgentLifecycle state;
        volatile String lastError;
        
        AgentMetadata(String agentType) {
            this.agentType = agentType;
            this.creationTime = System.currentTimeMillis();
            this.properties = new ConcurrentHashMap<>();
            this.state = AgentLifecycle.INACTIVE; // Start as inactive, activated later
        }
    }
    
    public DefaultAgentContext(String namespace, EventBroker eventBroker) {
        // Generate a unique ID for this context
        this.contextId = namespace + ".context." + 
            Long.toHexString(System.currentTimeMillis()) + "." +
            Integer.toHexString(ThreadLocalRandom.current().nextInt());
        this.contextName = namespace + " Agent Context";
        this.eventBroker = eventBroker;
        
        // Initialize metadata
        this.metadata.put("namespace", namespace);
        this.metadata.put("createdTime", System.currentTimeMillis());
        this.metadata.put("version", "1.0.0");
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
        return active && !shutdown;
    }
    
    @Override
    public CompletableFuture<Void> start() {
        if (shutdown) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Context has been shutdown"));
            return future;
        }
        
        return CompletableFuture.runAsync(() -> {
            active = true;
            System.out.println("Agent context " + contextId + " started");
        });
    }
    
    @Override
    public CompletableFuture<Agent> createAgent(String agentType, Object initData) {
        if (!isActive()) {
            CompletableFuture<Agent> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Context is not active"));
            return future;
        }
        
        // Generate unique ID for the agent
        String namespace = (String) metadata.get("namespace");
        AgentID agentId = AgentID.fromString(namespace + ".agent." + UUID.randomUUID().toString());
        
        return createAgent(agentId, agentType, initData);
    }
    
    @Override
    public CompletableFuture<Agent> createAgent(AgentID agentId, String agentType, Object initData) {
        if (!isActive()) {
            CompletableFuture<Agent> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Context is not active"));
            return future;
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Load the agent class
                Class<?> agentClass = Class.forName(agentType);
                
                if (!Agent.class.isAssignableFrom(agentClass)) {
                    throw new IllegalArgumentException("Class " + agentType + " does not implement Agent interface");
                }
                
                // Create instance
                Agent agent = (Agent) agentClass.newInstance();
                
                // Initialize metadata
                AgentMetadata meta = new AgentMetadata(agentType);
                
                // Store agent and metadata
                agents.put(agentId, agent);
                agentMetadata.put(agentId, meta);
                subscriptions.put(agentId, new HashSet<>());
                
                // Initialize the agent
                agent.onCreate(initData);
                meta.state = AgentLifecycle.INACTIVE;
                
                // Subscribe to control events for this agent
                subscribeToControlEvents(agentId);
                
                return agent;
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to create agent: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> activateAgent(AgentID agentId) {
        Agent agent = agents.get(agentId);
        if (agent == null) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Agent not found: " + agentId));
            return future;
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                AgentMetadata meta = agentMetadata.get(agentId);
                if (meta != null && meta.state == AgentLifecycle.INACTIVE) {
                    agent.onActivate();
                    meta.state = AgentLifecycle.ACTIVE;
                }
            } catch (Exception e) {
                AgentMetadata meta = agentMetadata.get(agentId);
                if (meta != null) {
                    meta.lastError = e.getMessage();
                }
                throw new RuntimeException("Failed to activate agent: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> deactivateAgent(AgentID agentId) {
        Agent agent = agents.get(agentId);
        if (agent == null) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Agent not found: " + agentId));
            return future;
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                AgentMetadata meta = agentMetadata.get(agentId);
                if (meta != null && meta.state == AgentLifecycle.ACTIVE) {
                    agent.onDeactivate();
                    meta.state = AgentLifecycle.INACTIVE;
                }
            } catch (Exception e) {
                AgentMetadata meta = agentMetadata.get(agentId);
                if (meta != null) {
                    meta.lastError = e.getMessage();
                }
                throw new RuntimeException("Failed to deactivate agent: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Agent> cloneAgent(AgentID sourceAgentId) {
        Agent sourceAgent = agents.get(sourceAgentId);
        if (sourceAgent == null) {
            CompletableFuture<Agent> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Source agent not found: " + sourceAgentId));
            return future;
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                AgentMetadata sourceMeta = agentMetadata.get(sourceAgentId);
                if (sourceMeta == null) {
                    throw new RuntimeException("Source agent metadata not found");
                }
                
                // Generate new ID for clone
                String namespace = (String) metadata.get("namespace");
                AgentID cloneId = AgentID.fromString(namespace + ".agent." + UUID.randomUUID().toString());
                
                // Create clone with same type
                CompletableFuture<Agent> cloneFuture = createAgent(cloneId, sourceMeta.agentType, null);
                Agent clone = cloneFuture.get();
                
                // Copy properties
                AgentMetadata cloneMeta = agentMetadata.get(cloneId);
                if (cloneMeta != null) {
                    cloneMeta.properties.putAll(sourceMeta.properties);
                }
                
                return clone;
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to clone agent: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> dispatchAgent(AgentID agentId, String destinationContext) {
        Agent agent = agents.get(agentId);
        if (agent == null) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Agent not found: " + agentId));
            return future;
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                AgentMetadata meta = agentMetadata.get(agentId);
                if (meta != null) {
                    // Notify agent of migration
                    agent.onBeforeDispatch(destinationContext);
                    
                    // For now, just log the migration - actual implementation would
                    // involve serializing agent state and transferring to target
                    System.out.println("Agent " + agentId + " dispatched to " + destinationContext + 
                                     " (simulation)");
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to dispatch agent: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Agent> receiveAgent(AgentID agentId, String agentType, 
                                               Object agentState, String sourceContext) {
        // Create the agent from received state
        return createAgent(agentId, agentType, agentState)
            .thenApply(agent -> {
                System.out.println("Agent " + agentId + " received from " + sourceContext);
                return agent;
            });
    }
    
    @Override
    public CompletableFuture<Void> destroyAgent(AgentID agentId) {
        Agent agent = agents.get(agentId);
        if (agent == null) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Agent not found: " + agentId));
            return future;
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                agent.onDestroy();
                
                // Remove from local collections
                agents.remove(agentId);
                agentMetadata.remove(agentId);
                subscriptions.remove(agentId);
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to destroy agent: " + e.getMessage(), e);
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
        List<Agent> result = new ArrayList<>();
        for (Map.Entry<AgentID, Agent> entry : agents.entrySet()) {
            AgentMetadata meta = agentMetadata.get(entry.getKey());
            if (meta != null && agentType.equals(meta.agentType)) {
                result.add(entry.getValue());
            }
        }
        return result;
    }
    
    @Override
    public CompletableFuture<Void> publishEvent(Event event) {
        if (!isActive()) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Context is not active"));
            return future;
        }
        
        return eventBroker.createPublisher("context." + contextId).publish(event);
    }
    
    @Override
    public void subscribe(AgentID agentId, String topicPattern) {
        if (!agents.containsKey(agentId)) {
            throw new IllegalArgumentException("Agent not found: " + agentId);
        }
        
        subscriptions.computeIfAbsent(agentId, k -> new HashSet<>()).add(topicPattern);
        
        // Create actual subscription
        try {
            eventBroker.createSubscriber("agent." + agentId.getId())
                .subscribe(topicPattern, event -> {
                    Agent agent = agents.get(agentId);
                    if (agent != null) {
                        agent.handleEvent(event);
                    }
                });
        } catch (Exception e) {
            System.err.println("Failed to create subscription for agent " + agentId + 
                             ": " + e.getMessage());
        }
    }
    
    @Override
    public void unsubscribe(AgentID agentId, String topicPattern) {
        Set<String> agentSubs = subscriptions.get(agentId);
        if (agentSubs != null) {
            agentSubs.remove(topicPattern);
            // Note: Actual unsubscription from broker would need subscription tracking
        }
    }
    
    @Override
    public Collection<String> getSubscriptions(AgentID agentId) {
        Set<String> agentSubs = subscriptions.get(agentId);
        return agentSubs != null ? new HashSet<>(agentSubs) : Collections.emptySet();
    }
    
    @Override
    public CompletableFuture<Void> sendControlEvent(ControlEvent controlEvent) {
        if (!isActive()) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Context is not active"));
            return future;
        }
        
        // Create an Event containing the control event
        Event event = Event.builder()
            .topic(controlEvent.getTargetAgent().getControlTopic())
            .payload(controlEvent)
            .messageId(UUID.randomUUID().toString())
            .sender(AgentID.fromString(contextId))
            .build();
            
        return publishEvent(event);
    }
    
    @Override
    public CompletableFuture<Void> shutdown() {
        shutdown = true;
        active = false;
        
        // Deactivate and destroy all agents
        List<CompletableFuture<Void>> shutdownTasks = new ArrayList<>();
        
        for (AgentID agentId : new HashSet<>(agents.keySet())) {
            CompletableFuture<Void> task = deactivateAgent(agentId)
                .thenCompose(v -> destroyAgent(agentId))
                .exceptionally(e -> {
                    System.err.println("Error shutting down agent " + agentId + ": " + e.getMessage());
                    return null;
                });
            shutdownTasks.add(task);
        }
        
        return CompletableFuture.allOf(shutdownTasks.toArray(new CompletableFuture[0]));
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
        return new HashMap<>(metadata);
    }
    
    /**
     * Subscribe to control events for the specified agent.
     */
    private void subscribeToControlEvents(AgentID agentId) {
        String controlTopic = agentId.getControlTopic();
        
        try {
            eventBroker.createSubscriber("control." + contextId)
                .subscribe(controlTopic, event -> handleControlEvent(agentId, event));
        } catch (Exception e) {
            System.err.println("Failed to subscribe to control events for agent " + agentId + 
                             ": " + e.getMessage());
        }
    }
    
    /**
     * Handle control events for agents.
     */
    private void handleControlEvent(AgentID agentId, Event event) {
        Agent agent = agents.get(agentId);
        if (agent == null) return;
        
        try {
            // Parse control event
            ControlEvent controlEvent = event.getPayload(ControlEvent.class);
            if (controlEvent != null) {
                agent.handleControlEvent(controlEvent);
            }
        } catch (Exception e) {
            System.err.println("Error handling control event for agent " + agentId + 
                             ": " + e.getMessage());
            
            AgentMetadata meta = agentMetadata.get(agentId);
            if (meta != null) {
                meta.lastError = e.getMessage();
            }
        }
    }
}