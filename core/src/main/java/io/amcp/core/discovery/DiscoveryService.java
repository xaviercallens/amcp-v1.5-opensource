package io.amcp.core.discovery;

import io.amcp.core.registry.AgentRegistry;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Agent Discovery Service for AMCP v1.5 Enterprise Edition
 * 
 * Provides a simple service layer for agent discovery and capability resolution.
 * This service acts as a facade over the AgentRegistry, providing convenient
 * methods for common discovery operations.
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class DiscoveryService {
    
    private final AgentRegistry registry;
    private volatile boolean isRunning;
    
    public DiscoveryService(AgentRegistry registry) {
        this.registry = registry;
        this.isRunning = false;
    }
    
    /**
     * Starts the discovery service
     */
    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(() -> {
            if (!isRunning) {
                isRunning = true;
                logMessage("🔍 Discovery Service started");
            }
        });
    }
    
    /**
     * Stops the discovery service
     */
    public CompletableFuture<Void> stop() {
        return CompletableFuture.runAsync(() -> {
            if (isRunning) {
                isRunning = false;
                logMessage("🛑 Discovery Service stopped");
            }
        });
    }
    
    /**
     * Discovers agents that can handle a specific capability
     */
    public CompletableFuture<List<AgentInfo>> discoverAgentsByCapability(String capability) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isRunning) {
                throw new IllegalStateException("Discovery service is not running");
            }
            
            Set<AgentRegistry.AgentRegistration> registrations = registry.findAgentsByCapability(capability);
            List<AgentInfo> result = new ArrayList<>();
            
            for (AgentRegistry.AgentRegistration registration : registrations) {
                result.add(AgentInfo.fromRegistration(registration));
            }
            
            logMessage("🔍 Found " + result.size() + " agents for capability: " + capability);
            return result;
        });
    }
    
    /**
     * Discovers agents of a specific type
     */
    public CompletableFuture<List<AgentInfo>> discoverAgentsByType(String agentType) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isRunning) {
                throw new IllegalStateException("Discovery service is not running");
            }
            
            Set<AgentRegistry.AgentRegistration> registrations = registry.findAgentsByType(agentType);
            List<AgentInfo> result = new ArrayList<>();
            
            for (AgentRegistry.AgentRegistration registration : registrations) {
                result.add(AgentInfo.fromRegistration(registration));
            }
            
            logMessage("🔍 Found " + result.size() + " agents of type: " + agentType);
            return result;
        });
    }
    
    /**
     * Gets all available capabilities in the system
     */
    public CompletableFuture<Set<String>> getAvailableCapabilities() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isRunning) {
                throw new IllegalStateException("Discovery service is not running");
            }
            
            Set<String> capabilities = new HashSet<>();
            Set<AgentRegistry.AgentRegistration> allAgents = registry.getAllActiveAgents();
            
            for (AgentRegistry.AgentRegistration registration : allAgents) {
                capabilities.addAll(registration.getCapabilities());
            }
            
            logMessage("🔍 Available capabilities: " + capabilities.size());
            return capabilities;
        });
    }
    
    /**
     * Performs intelligent agent matching for multiple capabilities
     */
    public CompletableFuture<AgentMatchResult> matchAgentsForTask(Set<String> requiredCapabilities) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isRunning) {
                throw new IllegalStateException("Discovery service is not running");
            }
            
            AgentRegistry.AgentMatchResult registryResult = registry.matchAgentsForCapabilities(requiredCapabilities);
            
            // Convert to discovery service result
            AgentMatchResult result = new AgentMatchResult();
            
            for (Map.Entry<String, AgentRegistry.AgentRegistration> entry : registryResult.getMatches().entrySet()) {
                result.addMatch(entry.getKey(), AgentInfo.fromRegistration(entry.getValue()));
            }
            
            result.setMissingCapabilities(registryResult.getMissingCapabilities());
            result.setCompleteness(registryResult.getCompleteness());
            
            logMessage("🎯 Agent matching completed: " + result.getCompleteness() * 100 + "% coverage");
            return result;
        });
    }
    
    /**
     * Gets discovery service statistics
     */
    public CompletableFuture<DiscoveryStatistics> getStatistics() {
        return CompletableFuture.supplyAsync(() -> {
            AgentRegistry.RegistryStatistics registryStats = registry.getStatistics();
            return new DiscoveryStatistics(registryStats);
        });
    }
    
    private void logMessage(String message) {
        String timestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [DiscoveryService] " + message);
    }
    
    /**
     * Simplified agent information for discovery operations
     */
    public static class AgentInfo {
        private final String agentId;
        private final String agentType;
        private final String contextId;
        private final Set<String> capabilities;
        private final String description;
        private final Map<String, Object> metadata;
        
        public AgentInfo(String agentId, String agentType, String contextId, 
                        Set<String> capabilities, String description, Map<String, Object> metadata) {
            this.agentId = agentId;
            this.agentType = agentType;
            this.contextId = contextId;
            this.capabilities = new HashSet<>(capabilities);
            this.description = description;
            this.metadata = new HashMap<>(metadata);
        }
        
        public static AgentInfo fromRegistration(AgentRegistry.AgentRegistration registration) {
            String description = (String) registration.getMetadata().getOrDefault("description", 
                "Agent of type " + registration.getAgentType());
            
            return new AgentInfo(
                registration.getAgentId().toString(),
                registration.getAgentType(),
                registration.getContextId(),
                registration.getCapabilities(),
                description,
                registration.getMetadata()
            );
        }
        
        // Getters
        public String getAgentId() { return agentId; }
        public String getAgentType() { return agentType; }
        public String getContextId() { return contextId; }
        public Set<String> getCapabilities() { return Collections.unmodifiableSet(capabilities); }
        public String getDescription() { return description; }
        public Map<String, Object> getMetadata() { return Collections.unmodifiableMap(metadata); }
        
        @Override
        public String toString() {
            return String.format("AgentInfo{id=%s, type=%s, capabilities=%s}", 
                agentId, agentType, capabilities);
        }
    }
    
    /**
     * Result of agent matching for task requirements
     */
    public static class AgentMatchResult {
        private final Map<String, AgentInfo> matches;
        private Set<String> missingCapabilities;
        private double completeness;
        
        public AgentMatchResult() {
            this.matches = new HashMap<>();
            this.missingCapabilities = new HashSet<>();
            this.completeness = 0.0;
        }
        
        public void addMatch(String capability, AgentInfo agent) {
            matches.put(capability, agent);
        }
        
        public void setMissingCapabilities(Set<String> missing) {
            this.missingCapabilities = new HashSet<>(missing);
        }
        
        public void setCompleteness(double completeness) {
            this.completeness = completeness;
        }
        
        public Map<String, AgentInfo> getMatches() {
            return Collections.unmodifiableMap(matches);
        }
        
        public Set<String> getMissingCapabilities() {
            return Collections.unmodifiableSet(missingCapabilities);
        }
        
        public double getCompleteness() {
            return completeness;
        }
        
        public boolean isComplete() {
            return missingCapabilities.isEmpty();
        }
        
        @Override
        public String toString() {
            return String.format("AgentMatchResult{matches=%d, missing=%d, completeness=%.1f%%}", 
                matches.size(), missingCapabilities.size(), completeness * 100);
        }
    }
    
    /**
     * Discovery service statistics
     */
    public static class DiscoveryStatistics {
        private final int totalAgents;
        private final int activeAgents;
        private final int availableCapabilities;
        private final int activeContexts;
        
        public DiscoveryStatistics(AgentRegistry.RegistryStatistics registryStats) {
            this.totalAgents = registryStats.getTotalAgents();
            this.activeAgents = registryStats.getActiveAgents();
            this.availableCapabilities = registryStats.getTotalCapabilities();
            this.activeContexts = registryStats.getTotalContexts();
        }
        
        public int getTotalAgents() { return totalAgents; }
        public int getActiveAgents() { return activeAgents; }
        public int getAvailableCapabilities() { return availableCapabilities; }
        public int getActiveContexts() { return activeContexts; }
        
        @Override
        public String toString() {
            return String.format("DiscoveryStats{total=%d, active=%d, capabilities=%d, contexts=%d}",
                totalAgents, activeAgents, availableCapabilities, activeContexts);
        }
    }
}