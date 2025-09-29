package io.amcp.cli;

import io.amcp.core.Agent;
import io.amcp.core.AgentContext;
import io.amcp.core.AgentID;
import io.amcp.core.AgentLifecycle;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Agent Registry for AMCP CLI
 * 
 * Manages registration, activation, and lifecycle of all available agents.
 * Provides dynamic discovery and monitoring capabilities.
 */
public class AgentRegistry {
    
    private final AgentContext agentContext;
    private final Map<String, AgentDefinition> registeredAgents = new ConcurrentHashMap<>();
    private final Map<String, Agent> activeAgents = new ConcurrentHashMap<>();
    
    public AgentRegistry(AgentContext agentContext) {
        this.agentContext = agentContext;
    }
    
    /**
     * Register an agent type with the registry
     */
    public void registerAgent(String name, Supplier<Agent> agentFactory, String description) {
        AgentDefinition definition = new AgentDefinition(name, agentFactory, description);
        registeredAgents.put(name, definition);
        System.out.println("🔧 Registered agent: " + name + " - " + description);
    }
    
    /**
     * Activate an agent by name
     */
    public CommandResult activateAgent(String agentName) {
        try {
            if (activeAgents.containsKey(agentName)) {
                return CommandResult.warning("Agent '" + agentName + "' is already active");
            }
            
            AgentDefinition definition = registeredAgents.get(agentName);
            if (definition == null) {
                return CommandResult.error("Agent '" + agentName + "' is not registered");
            }
            
            // Create agent instance
            Agent agent = definition.getAgentFactory().get();
            
            // Set context if the agent supports it
            if (agent instanceof io.amcp.examples.stockprice.StockPriceAgent) {
                ((io.amcp.examples.stockprice.StockPriceAgent) agent).setContext(agentContext);
            } else if (agent instanceof io.amcp.examples.weather.WeatherAgent) {
                ((io.amcp.examples.weather.WeatherAgent) agent).setContext(agentContext);
            } else if (agent instanceof io.amcp.examples.travel.TravelPlannerAgent) {
                ((io.amcp.examples.travel.TravelPlannerAgent) agent).setContext(agentContext);
            }
            
            // Register with context
            agentContext.registerAgent(agent).get(5, TimeUnit.SECONDS);
            
            // Activate agent
            agentContext.activateAgent(agent.getAgentId()).get(5, TimeUnit.SECONDS);
            
            // Add to active agents
            activeAgents.put(agentName, agent);
            
            return CommandResult.success("Agent '" + agentName + "' activated successfully", 
                Map.of("agentId", agent.getAgentId().toString(),
                       "status", agent.getLifecycleState().toString()));
                       
        } catch (Exception e) {
            return CommandResult.error("Failed to activate agent '" + agentName + "': " + e.getMessage());
        }
    }
    
    /**
     * Deactivate an agent by name
     */
    public CommandResult deactivateAgent(String agentName) {
        try {
            Agent agent = activeAgents.get(agentName);
            if (agent == null) {
                return CommandResult.warning("Agent '" + agentName + "' is not active");
            }
            
            // Deactivate agent
            agentContext.deactivateAgent(agent.getAgentId());
            
            // Remove from active agents
            activeAgents.remove(agentName);
            
            return CommandResult.success("Agent '" + agentName + "' deactivated successfully");
            
        } catch (Exception e) {
            return CommandResult.error("Failed to deactivate agent '" + agentName + "': " + e.getMessage());
        }
    }
    
    /**
     * Get status of all agents
     */
    public List<AgentStatus> getAgentStatus() {
        List<AgentStatus> statusList = new ArrayList<>();
        
        // Add registered agents
        for (Map.Entry<String, AgentDefinition> entry : registeredAgents.entrySet()) {
            String name = entry.getKey();
            AgentDefinition definition = entry.getValue();
            Agent activeAgent = activeAgents.get(name);
            
            AgentStatus status = new AgentStatus(
                name,
                definition.getDescription(),
                activeAgent != null,
                activeAgent != null ? activeAgent.getAgentId().toString() : null,
                activeAgent != null ? activeAgent.getLifecycleState() : AgentLifecycle.INACTIVE
            );
            
            statusList.add(status);
        }
        
        return statusList;
    }
    
    /**
     * Get active agent by name
     */
    public Agent getActiveAgent(String name) {
        return activeAgents.get(name);
    }
    
    /**
     * Get all active agents
     */
    public Map<String, Agent> getActiveAgents() {
        return new HashMap<>(activeAgents);
    }
    
    /**
     * Get registered agent names
     */
    public Set<String> getRegisteredAgentNames() {
        return new HashSet<>(registeredAgents.keySet());
    }
    
    /**
     * Get count of registered agents
     */
    public int getRegisteredAgentCount() {
        return registeredAgents.size();
    }
    
    /**
     * Get count of active agents
     */
    public int getActiveAgentCount() {
        return activeAgents.size();
    }
    
    /**
     * Shutdown all active agents
     */
    public void shutdownAllAgents() {
        for (String agentName : new ArrayList<>(activeAgents.keySet())) {
            try {
                deactivateAgent(agentName);
            } catch (Exception e) {
                System.err.println("⚠️  Failed to shutdown agent " + agentName + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Agent definition holder
     */
    private static class AgentDefinition {
        private final String name;
        private final Supplier<Agent> agentFactory;
        private final String description;
        
        public AgentDefinition(String name, Supplier<Agent> agentFactory, String description) {
            this.name = name;
            this.agentFactory = agentFactory;
            this.description = description;
        }
        
        public String getName() { return name; }
        public Supplier<Agent> getAgentFactory() { return agentFactory; }
        public String getDescription() { return description; }
    }
    
    /**
     * Agent status information
     */
    public static class AgentStatus {
        private final String name;
        private final String description;
        private final boolean active;
        private final String agentId;
        private final AgentLifecycle lifecycle;
        
        public AgentStatus(String name, String description, boolean active, String agentId, AgentLifecycle lifecycle) {
            this.name = name;
            this.description = description;
            this.active = active;
            this.agentId = agentId;
            this.lifecycle = lifecycle;
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
        public boolean isActive() { return active; }
        public String getAgentId() { return agentId; }
        public AgentLifecycle getLifecycle() { return lifecycle; }
        
        @Override
        public String toString() {
            String status = active ? "🟢 ACTIVE" : "⚪ INACTIVE";
            return String.format("%-15s %s - %s", name, status, description);
        }
    }
}