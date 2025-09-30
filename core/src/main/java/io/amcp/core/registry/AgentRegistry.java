package io.amcp.core.registry;

import io.amcp.core.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Dynamic Agent Registry for AMCP v1.5 Enterprise Edition
 * 
 * Provides a comprehensive system for agent discovery, capability registration,
 * and runtime coordination. Supports distributed agent mesh awareness and
 * intelligent capability matching for orchestration.
 * 
 * Key Features:
 * - Dynamic agent registration and deregistration
 * - Capability-based agent discovery
 * - Real-time agent status monitoring
 * - Distributed registry synchronization
 * - Health checking and automatic cleanup
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class AgentRegistry {
    
    // Registry storage
    private final ConcurrentHashMap<AgentID, AgentRegistration> registrations;
    private final ConcurrentHashMap<String, CopyOnWriteArraySet<AgentID>> capabilityIndex;
    private final ConcurrentHashMap<String, CopyOnWriteArraySet<AgentID>> typeIndex;
    private final ConcurrentHashMap<String, CopyOnWriteArraySet<AgentID>> contextIndex;
    
    // Registry configuration
    private final RegistryConfiguration configuration;
    private final Set<RegistryListener> listeners;
    
    // Registry state
    private volatile boolean isRunning;
    private volatile long lastCleanup;
    
    public AgentRegistry() {
        this(new RegistryConfiguration());
    }
    
    public AgentRegistry(RegistryConfiguration configuration) {
        this.configuration = configuration;
        this.registrations = new ConcurrentHashMap<>();
        this.capabilityIndex = new ConcurrentHashMap<>();
        this.typeIndex = new ConcurrentHashMap<>();
        this.contextIndex = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArraySet<>();
        this.isRunning = false;
        this.lastCleanup = System.currentTimeMillis();
    }
    
    /**
     * Starts the agent registry with health monitoring
     */
    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(() -> {
            if (isRunning) {
                return;
            }
            
            isRunning = true;
            logMessage("🚀 Agent Registry started with configuration: " + configuration);
            
            // Start background health monitoring
            if (configuration.isHealthCheckEnabled()) {
                startHealthMonitoring();
            }
            
            // Notify listeners
            notifyListeners(RegistryEvent.RegistryStarted());
        });
    }
    
    /**
     * Stops the agent registry and cleanup resources
     */
    public CompletableFuture<Void> stop() {
        return CompletableFuture.runAsync(() -> {
            if (!isRunning) {
                return;
            }
            
            isRunning = false;
            logMessage("🛑 Agent Registry stopping...");
            
            // Cleanup all registrations
            registrations.clear();
            capabilityIndex.clear();
            typeIndex.clear();
            contextIndex.clear();
            
            // Notify listeners
            notifyListeners(RegistryEvent.RegistryStopped());
            
            logMessage("✅ Agent Registry stopped");
        });
    }
    
    /**
     * Registers an agent with its capabilities and metadata
     */
    public CompletableFuture<AgentRegistration> registerAgent(Agent agent, 
                                                           Set<String> capabilities,
                                                           Map<String, Object> metadata) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isRunning) {
                throw new IllegalStateException("Registry is not running");
            }
            
            AgentID agentId = agent.getAgentId();
            String agentType = agent.getClass().getSimpleName();
            String contextId = agent.getContext() != null ? agent.getContext().getContextId() : "unknown";
            
            AgentRegistration registration = new AgentRegistration(
                agentId,
                agentType,
                contextId,
                capabilities,
                metadata,
                Instant.now(),
                AgentStatus.ACTIVE
            );
            
            // Store registration
            registrations.put(agentId, registration);
            
            // Update indices
            updateCapabilityIndex(agentId, capabilities, true);
            updateTypeIndex(agentId, agentType, true);
            updateContextIndex(agentId, contextId, true);
            
            logMessage("📝 Agent registered: " + agentId + " (" + agentType + ") with capabilities: " + capabilities);
            
            // Notify listeners
            notifyListeners(RegistryEvent.AgentRegistered(registration));
            
            return registration;
        });
    }
    
    /**
     * Unregisters an agent from the registry
     */
    public CompletableFuture<Void> unregisterAgent(AgentID agentId) {
        return CompletableFuture.runAsync(() -> {
            AgentRegistration registration = registrations.remove(agentId);
            if (registration != null) {
                // Update indices
                updateCapabilityIndex(agentId, registration.getCapabilities(), false);
                updateTypeIndex(agentId, registration.getAgentType(), false);
                updateContextIndex(agentId, registration.getContextId(), false);
                
                logMessage("🗑️ Agent unregistered: " + agentId);
                
                // Notify listeners
                notifyListeners(RegistryEvent.AgentUnregistered(registration));
            }
        });
    }
    
    /**
     * Updates agent status (e.g., ACTIVE, INACTIVE, MIGRATING)
     */
    public CompletableFuture<Void> updateAgentStatus(AgentID agentId, AgentStatus newStatus) {
        return CompletableFuture.runAsync(() -> {
            AgentRegistration registration = registrations.get(agentId);
            if (registration != null) {
                AgentRegistration updated = registration.withStatus(newStatus);
                registrations.put(agentId, updated);
                
                logMessage("🔄 Agent status updated: " + agentId + " -> " + newStatus);
                
                // Notify listeners
                notifyListeners(RegistryEvent.AgentStatusChanged(updated));
            }
        });
    }
    
    /**
     * Finds agents by capability
     */
    public Set<AgentRegistration> findAgentsByCapability(String capability) {
        Set<AgentID> agentIds = capabilityIndex.getOrDefault(capability, new CopyOnWriteArraySet<>());
        Set<AgentRegistration> result = new HashSet<>();
        
        for (AgentID agentId : agentIds) {
            AgentRegistration registration = registrations.get(agentId);
            if (registration != null && registration.getStatus() == AgentStatus.ACTIVE) {
                result.add(registration);
            }
        }
        
        return result;
    }
    
    /**
     * Finds agents by type
     */
    public Set<AgentRegistration> findAgentsByType(String agentType) {
        Set<AgentID> agentIds = typeIndex.getOrDefault(agentType, new CopyOnWriteArraySet<>());
        Set<AgentRegistration> result = new HashSet<>();
        
        for (AgentID agentId : agentIds) {
            AgentRegistration registration = registrations.get(agentId);
            if (registration != null && registration.getStatus() == AgentStatus.ACTIVE) {
                result.add(registration);
            }
        }
        
        return result;
    }
    
    /**
     * Finds agents in a specific context
     */
    public Set<AgentRegistration> findAgentsByContext(String contextId) {
        Set<AgentID> agentIds = contextIndex.getOrDefault(contextId, new CopyOnWriteArraySet<>());
        Set<AgentRegistration> result = new HashSet<>();
        
        for (AgentID agentId : agentIds) {
            AgentRegistration registration = registrations.get(agentId);
            if (registration != null) {
                result.add(registration);
            }
        }
        
        return result;
    }
    
    /**
     * Gets all active agents
     */
    public Set<AgentRegistration> getAllActiveAgents() {
        Set<AgentRegistration> result = new HashSet<>();
        for (AgentRegistration registration : registrations.values()) {
            if (registration.getStatus() == AgentStatus.ACTIVE) {
                result.add(registration);
            }
        }
        return result;
    }
    
    /**
     * Gets registration for specific agent
     */
    public Optional<AgentRegistration> getAgentRegistration(AgentID agentId) {
        return Optional.ofNullable(registrations.get(agentId));
    }
    
    /**
     * Performs capability-based agent matching for orchestration
     */
    public AgentMatchResult matchAgentsForCapabilities(Set<String> requiredCapabilities) {
        AgentMatchResult result = new AgentMatchResult();
        
        for (String capability : requiredCapabilities) {
            Set<AgentRegistration> candidates = findAgentsByCapability(capability);
            
            if (candidates.isEmpty()) {
                result.addMissingCapability(capability);
            } else {
                // Select best agent for this capability
                AgentRegistration bestAgent = selectBestAgent(candidates, capability);
                result.addAgentMatch(capability, bestAgent);
            }
        }
        
        return result;
    }
    
    /**
     * Gets registry statistics
     */
    public RegistryStatistics getStatistics() {
        int totalAgents = registrations.size();
        int activeAgents = (int) registrations.values().stream()
            .filter(r -> r.getStatus() == AgentStatus.ACTIVE)
            .count();
        int totalCapabilities = capabilityIndex.size();
        int totalContexts = contextIndex.size();
        
        return new RegistryStatistics(totalAgents, activeAgents, totalCapabilities, totalContexts);
    }
    
    /**
     * Adds a registry listener for events
     */
    public void addListener(RegistryListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Removes a registry listener
     */
    public void removeListener(RegistryListener listener) {
        listeners.remove(listener);
    }
    
    // Private helper methods
    
    private void updateCapabilityIndex(AgentID agentId, Set<String> capabilities, boolean add) {
        for (String capability : capabilities) {
            capabilityIndex.computeIfAbsent(capability, k -> new CopyOnWriteArraySet<>());
            if (add) {
                capabilityIndex.get(capability).add(agentId);
            } else {
                capabilityIndex.get(capability).remove(agentId);
            }
        }
    }
    
    private void updateTypeIndex(AgentID agentId, String agentType, boolean add) {
        typeIndex.computeIfAbsent(agentType, k -> new CopyOnWriteArraySet<>());
        if (add) {
            typeIndex.get(agentType).add(agentId);
        } else {
            typeIndex.get(agentType).remove(agentId);
        }
    }
    
    private void updateContextIndex(AgentID agentId, String contextId, boolean add) {
        contextIndex.computeIfAbsent(contextId, k -> new CopyOnWriteArraySet<>());
        if (add) {
            contextIndex.get(contextId).add(agentId);
        } else {
            contextIndex.get(contextId).remove(agentId);
        }
    }
    
    private AgentRegistration selectBestAgent(Set<AgentRegistration> candidates, String capability) {
        // Simple selection based on registration time (oldest first)
        return candidates.stream()
            .min(Comparator.comparing(AgentRegistration::getRegistrationTime))
            .orElse(null);
    }
    
    private void startHealthMonitoring() {
        CompletableFuture.runAsync(() -> {
            while (isRunning) {
                try {
                    performHealthCheck();
                    Thread.sleep(configuration.getHealthCheckInterval());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logMessage("❌ Health check error: " + e.getMessage());
                }
            }
        });
    }
    
    private void performHealthCheck() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup > configuration.getCleanupInterval()) {
            cleanupStaleRegistrations();
            lastCleanup = now;
        }
    }
    
    private void cleanupStaleRegistrations() {
        long staleThreshold = System.currentTimeMillis() - configuration.getStaleAgentTimeout();
        
        for (Map.Entry<AgentID, AgentRegistration> entry : registrations.entrySet()) {
            AgentRegistration registration = entry.getValue();
            if (registration.getLastHeartbeat().toEpochMilli() < staleThreshold) {
                unregisterAgent(entry.getKey());
                logMessage("🧹 Cleaned up stale agent: " + entry.getKey());
            }
        }
    }
    
    private void notifyListeners(RegistryEvent event) {
        for (RegistryListener listener : listeners) {
            try {
                listener.onRegistryEvent(event);
            } catch (Exception e) {
                logMessage("❌ Listener error: " + e.getMessage());
            }
        }
    }
    
    private void logMessage(String message) {
        String timestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [AgentRegistry] " + message);
    }
    
    // Inner classes for registry data structures
    
    /**
     * Agent registration information
     */
    public static class AgentRegistration {
        private final AgentID agentId;
        private final String agentType;
        private final String contextId;
        private final Set<String> capabilities;
        private final Map<String, Object> metadata;
        private final Instant registrationTime;
        private final AgentStatus status;
        private final Instant lastHeartbeat;
        
        public AgentRegistration(AgentID agentId, String agentType, String contextId,
                               Set<String> capabilities, Map<String, Object> metadata,
                               Instant registrationTime, AgentStatus status) {
            this.agentId = agentId;
            this.agentType = agentType;
            this.contextId = contextId;
            this.capabilities = new HashSet<>(capabilities);
            this.metadata = new HashMap<>(metadata);
            this.registrationTime = registrationTime;
            this.status = status;
            this.lastHeartbeat = Instant.now();
        }
        
        private AgentRegistration(AgentID agentId, String agentType, String contextId,
                                Set<String> capabilities, Map<String, Object> metadata,
                                Instant registrationTime, AgentStatus status, Instant lastHeartbeat) {
            this.agentId = agentId;
            this.agentType = agentType;
            this.contextId = contextId;
            this.capabilities = capabilities;
            this.metadata = metadata;
            this.registrationTime = registrationTime;
            this.status = status;
            this.lastHeartbeat = lastHeartbeat;
        }
        
        public AgentRegistration withStatus(AgentStatus newStatus) {
            return new AgentRegistration(agentId, agentType, contextId, capabilities,
                metadata, registrationTime, newStatus, Instant.now());
        }
        
        // Getters
        public AgentID getAgentId() { return agentId; }
        public String getAgentType() { return agentType; }
        public String getContextId() { return contextId; }
        public Set<String> getCapabilities() { return Collections.unmodifiableSet(capabilities); }
        public Map<String, Object> getMetadata() { return Collections.unmodifiableMap(metadata); }
        public Instant getRegistrationTime() { return registrationTime; }
        public AgentStatus getStatus() { return status; }
        public Instant getLastHeartbeat() { return lastHeartbeat; }
        
        @Override
        public String toString() {
            return String.format("AgentRegistration{id=%s, type=%s, context=%s, capabilities=%s, status=%s}",
                agentId, agentType, contextId, capabilities, status);
        }
    }
    
    /**
     * Agent status enumeration
     */
    public enum AgentStatus {
        ACTIVE,      // Agent is running and ready to handle requests
        INACTIVE,    // Agent is stopped or deactivated
        MIGRATING,   // Agent is in the process of migration
        BUSY,        // Agent is handling requests and may not accept new ones
        ERROR,       // Agent has encountered an error
        UNKNOWN      // Status cannot be determined
    }
    
    /**
     * Result of capability-based agent matching
     */
    public static class AgentMatchResult {
        private final Map<String, AgentRegistration> matches;
        private final Set<String> missingCapabilities;
        
        public AgentMatchResult() {
            this.matches = new HashMap<>();
            this.missingCapabilities = new HashSet<>();
        }
        
        public void addAgentMatch(String capability, AgentRegistration agent) {
            matches.put(capability, agent);
        }
        
        public void addMissingCapability(String capability) {
            missingCapabilities.add(capability);
        }
        
        public Map<String, AgentRegistration> getMatches() {
            return Collections.unmodifiableMap(matches);
        }
        
        public Set<String> getMissingCapabilities() {
            return Collections.unmodifiableSet(missingCapabilities);
        }
        
        public boolean isComplete() {
            return missingCapabilities.isEmpty();
        }
        
        public double getCompleteness() {
            int total = matches.size() + missingCapabilities.size();
            return total == 0 ? 1.0 : (double) matches.size() / total;
        }
    }
    
    /**
     * Registry configuration
     */
    public static class RegistryConfiguration {
        private boolean healthCheckEnabled = true;
        private long healthCheckInterval = 30000; // 30 seconds
        private long cleanupInterval = 60000; // 1 minute
        private long staleAgentTimeout = 300000; // 5 minutes
        
        // Getters and setters
        public boolean isHealthCheckEnabled() { return healthCheckEnabled; }
        public void setHealthCheckEnabled(boolean enabled) { this.healthCheckEnabled = enabled; }
        
        public long getHealthCheckInterval() { return healthCheckInterval; }
        public void setHealthCheckInterval(long interval) { this.healthCheckInterval = interval; }
        
        public long getCleanupInterval() { return cleanupInterval; }
        public void setCleanupInterval(long interval) { this.cleanupInterval = interval; }
        
        public long getStaleAgentTimeout() { return staleAgentTimeout; }
        public void setStaleAgentTimeout(long timeout) { this.staleAgentTimeout = timeout; }
        
        @Override
        public String toString() {
            return String.format("RegistryConfig{healthCheck=%s, interval=%dms, cleanup=%dms, stale=%dms}",
                healthCheckEnabled, healthCheckInterval, cleanupInterval, staleAgentTimeout);
        }
    }
    
    /**
     * Registry statistics
     */
    public static class RegistryStatistics {
        private final int totalAgents;
        private final int activeAgents;
        private final int totalCapabilities;
        private final int totalContexts;
        
        public RegistryStatistics(int totalAgents, int activeAgents, int totalCapabilities, int totalContexts) {
            this.totalAgents = totalAgents;
            this.activeAgents = activeAgents;
            this.totalCapabilities = totalCapabilities;
            this.totalContexts = totalContexts;
        }
        
        public int getTotalAgents() { return totalAgents; }
        public int getActiveAgents() { return activeAgents; }
        public int getTotalCapabilities() { return totalCapabilities; }
        public int getTotalContexts() { return totalContexts; }
        
        @Override
        public String toString() {
            return String.format("RegistryStats{total=%d, active=%d, capabilities=%d, contexts=%d}",
                totalAgents, activeAgents, totalCapabilities, totalContexts);
        }
    }
    
    /**
     * Registry event interface
     */
    public interface RegistryListener {
        void onRegistryEvent(RegistryEvent event);
    }
    
    /**
     * Registry events
     */
    public static abstract class RegistryEvent {
        public static RegistryEvent RegistryStarted() {
            return new RegistryStartedEvent();
        }
        
        public static RegistryEvent RegistryStopped() {
            return new RegistryStoppedEvent();
        }
        
        public static RegistryEvent AgentRegistered(AgentRegistration registration) {
            return new AgentRegisteredEvent(registration);
        }
        
        public static RegistryEvent AgentUnregistered(AgentRegistration registration) {
            return new AgentUnregisteredEvent(registration);
        }
        
        public static RegistryEvent AgentStatusChanged(AgentRegistration registration) {
            return new AgentStatusChangedEvent(registration);
        }
    }
    
    private static class RegistryStartedEvent extends RegistryEvent {}
    private static class RegistryStoppedEvent extends RegistryEvent {}
    
    private static class AgentRegisteredEvent extends RegistryEvent {
        public final AgentRegistration registration;
        AgentRegisteredEvent(AgentRegistration registration) { this.registration = registration; }
    }
    
    private static class AgentUnregisteredEvent extends RegistryEvent {
        public final AgentRegistration registration;
        AgentUnregisteredEvent(AgentRegistration registration) { this.registration = registration; }
    }
    
    private static class AgentStatusChangedEvent extends RegistryEvent {
        public final AgentRegistration registration;
        AgentStatusChangedEvent(AgentRegistration registration) { this.registration = registration; }
    }
}