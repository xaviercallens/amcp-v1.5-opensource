package io.amcp.connectors.ai.orchestration;

import io.amcp.core.*;
import io.amcp.connectors.ai.orchestration.TaskProtocol;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Registry Agent for AMCP v1.5 LLM Orchestration
 * 
 * Manages dynamic capability discovery and agent registration for the
 * LLM orchestrator. Provides a centralized registry of available agents
 * and their capabilities for intelligent task routing.
 * 
 * Features:
 * - Agent capability registration and discovery
 * - Real-time agent status monitoring  
 * - Capability matching for task assignment
 * - Agent health monitoring and failover
 * - Registry persistence and recovery
 */
public class RegistryAgent implements Agent {
    
    private final AgentID agentId;
    private AgentContext context;
    private AgentLifecycle lifecycleState = AgentLifecycle.INACTIVE;
    private final Set<String> subscriptions = new CopyOnWriteArraySet<>();
    
    // Registry state
    private final Map<String, AgentRegistration> agentRegistry = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> capabilityIndex = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastHeartbeat = new ConcurrentHashMap<>();
    private final AtomicLong registrationCount = new AtomicLong(0);
    
    // Configuration
    private final long heartbeatTimeoutMs = 60000; // 1 minute
    private final long cleanupIntervalMs = 30000;  // 30 seconds
    
    /**
     * Agent registration information
     */
    public static class AgentRegistration {
        private final String agentId;
        private final String agentType;
        private final List<String> capabilities;
        private final String description;
        private final String endpoint;
        private final Map<String, Object> metadata;
        private final LocalDateTime registrationTime;
        private LocalDateTime lastSeen;
        private RegistrationStatus status;
        
        public enum RegistrationStatus {
            ACTIVE, INACTIVE, UNHEALTHY, FAILED
        }
        
        public AgentRegistration(String agentId, String agentType, List<String> capabilities,
                               String description, String endpoint, Map<String, Object> metadata) {
            this.agentId = agentId;
            this.agentType = agentType;
            this.capabilities = new ArrayList<>(capabilities);
            this.description = description;
            this.endpoint = endpoint;
            this.metadata = new HashMap<>(metadata != null ? metadata : Map.of());
            this.registrationTime = LocalDateTime.now();
            this.lastSeen = LocalDateTime.now();
            this.status = RegistrationStatus.ACTIVE;
        }
        
        // Getters and setters
        public String getAgentId() { return agentId; }
        public String getAgentType() { return agentType; }
        public List<String> getCapabilities() { return new ArrayList<>(capabilities); }
        public String getDescription() { return description; }
        public String getEndpoint() { return endpoint; }
        public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
        public LocalDateTime getRegistrationTime() { return registrationTime; }
        public LocalDateTime getLastSeen() { return lastSeen; }
        public RegistrationStatus getStatus() { return status; }
        
        public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
        public void setStatus(RegistrationStatus status) { this.status = status; }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("agentId", agentId);
            map.put("agentType", agentType);
            map.put("capabilities", capabilities);
            map.put("description", description);
            map.put("endpoint", endpoint);
            map.put("metadata", metadata);
            map.put("registrationTime", registrationTime.toString());
            map.put("lastSeen", lastSeen.toString());
            map.put("status", status.toString());
            return map;
        }
    }
    
    public RegistryAgent() {
        this.agentId = AgentID.named("RegistryAgent");
    }
    
    @Override
    public AgentID getAgentId() {
        return agentId;
    }
    
    @Override
    public AgentContext getContext() {
        return context;
    }
    
    @Override
    public AgentLifecycle getLifecycleState() {
        return lifecycleState;
    }
    
    public void setContext(AgentContext context) {
        this.context = context;
    }
    
    @Override
    public void onActivate() {
        try {
            logMessage("🏛️ Activating Registry Agent...");
            lifecycleState = AgentLifecycle.ACTIVE;
            
            // Subscribe to registry-related events
            subscriptions.add(TaskProtocol.CAPABILITY_REGISTER_TOPIC);
            subscriptions.add("registry.query.**");
            subscriptions.add("registry.heartbeat.**");
            subscriptions.add("agent.status.**");
            
            for (String topic : subscriptions) {
                subscribe(topic);
            }
            
            // Start cleanup scheduler would go here (simplified for demo)
            
            logMessage("🏛️ Registry Agent activated with " + subscriptions.size() + " subscriptions");
            
        } catch (Exception e) {
            logMessage("❌ Failed to activate Registry Agent: " + e.getMessage());
            throw new RuntimeException("Registry Agent activation failed", e);
        }
    }
    
    @Override
    public void onDeactivate() {
        logMessage("🏛️ Deactivating Registry Agent...");
        lifecycleState = AgentLifecycle.INACTIVE;
        subscriptions.clear();
    }
    
    @Override
    public void onDestroy() {
        logMessage("🏛️ Destroying Registry Agent...");
        agentRegistry.clear();
        capabilityIndex.clear();
        lastHeartbeat.clear();
        subscriptions.clear();
        lifecycleState = AgentLifecycle.INACTIVE;
    }
    
    @Override
    public void onBeforeMigration(String destinationContext) {
        logMessage("🏛️ Preparing registry for migration to: " + destinationContext);
    }
    
    @Override
    public void onAfterMigration(String sourceContext) {
        logMessage("🏛️ Completed registry migration from: " + sourceContext);
    }
    
    @Override
    public CompletableFuture<Void> subscribe(String topicPattern) {
        if (context != null) {
            return context.subscribe(agentId, topicPattern);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> unsubscribe(String topicPattern) {
        if (context != null) {
            return context.unsubscribe(agentId, topicPattern);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> publishEvent(Event event) {
        if (context != null) {
            context.publishEvent(event);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                String topic = event.getTopic();
                logMessage("📨 Processing registry event: " + topic);
                
                if (TaskProtocol.CAPABILITY_REGISTER_TOPIC.equals(topic)) {
                    handleCapabilityRegistration(event);
                } else if (topic.startsWith("registry.query")) {
                    handleRegistryQuery(event);
                } else if (topic.startsWith("registry.heartbeat")) {
                    handleHeartbeat(event);
                } else if (topic.startsWith("agent.status")) {
                    handleAgentStatusUpdate(event);
                } else {
                    logMessage("⚠️ Unhandled registry event: " + topic);
                }
                
            } catch (Exception e) {
                logMessage("❌ Failed to handle registry event: " + event.getTopic() + " - " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Handle agent capability registration
     */
    private void handleCapabilityRegistration(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            
            TaskProtocol.CapabilityRegistration registration = TaskProtocol.CapabilityRegistration.fromMap(payload);
            
            String agentId = registration.getAgentId();
            String agentType = registration.getAgentType();
            List<String> capabilities = registration.getCapabilities();
            String description = registration.getDescription();
            String endpoint = registration.getEndpoint();
            Map<String, Object> metadata = registration.getMetadata();
            
            // Create agent registration
            AgentRegistration agentReg = new AgentRegistration(
                agentId, agentType, capabilities, description, endpoint, metadata
            );
            
            // Register agent
            agentRegistry.put(agentId, agentReg);
            registrationCount.incrementAndGet();
            
            // Update capability index
            for (String capability : capabilities) {
                capabilityIndex.computeIfAbsent(capability, k -> new HashSet<>()).add(agentId);
            }
            
            // Update heartbeat
            lastHeartbeat.put(agentId, LocalDateTime.now());
            
            logMessage("✅ Registered agent: " + agentId + " with " + capabilities.size() + " capabilities");
            
            // Send confirmation event
            Event confirmationEvent = Event.builder()
                .topic("registry.registration.confirmed")
                .payload(Map.of(
                    "agentId", agentId,
                    "status", "confirmed",
                    "registrationTime", agentReg.getRegistrationTime().toString(),
                    "capabilities", capabilities
                ))
                .correlationId(event.getCorrelationId())
                .build();
            
            publishEvent(confirmationEvent);
            
        } catch (Exception e) {
            logMessage("❌ Failed to process capability registration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Handle registry queries from orchestrator
     */
    private void handleRegistryQuery(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            String queryType = (String) payload.get("queryType");
            
            Object result = null;
            
            switch (queryType) {
                case "findByCapability":
                    String capability = (String) payload.get("capability");
                    result = findAgentsByCapability(capability);
                    break;
                    
                case "findByType":
                    String agentType = (String) payload.get("agentType");
                    result = findAgentsByType(agentType);
                    break;
                    
                case "getAgent":
                    String agentId = (String) payload.get("agentId");
                    result = getAgentRegistration(agentId);
                    break;
                    
                case "listAll":
                    result = getAllAgents();
                    break;
                    
                case "getStats":
                    result = getRegistryStats();
                    break;
                    
                default:
                    result = Map.of("error", "Unknown query type: " + queryType);
                    break;
            }
            
            // Send response
            Event responseEvent = Event.builder()
                .topic("registry.query.response")
                .payload(Map.of(
                    "queryType", queryType,
                    "result", result,
                    "timestamp", LocalDateTime.now().toString()
                ))
                .correlationId(event.getCorrelationId())
                .build();
            
            publishEvent(responseEvent);
            
            logMessage("📤 Processed registry query: " + queryType);
            
        } catch (Exception e) {
            logMessage("❌ Failed to process registry query: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Handle agent heartbeat updates
     */
    private void handleHeartbeat(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            String agentId = (String) payload.get("agentId");
            
            if (agentId != null && agentRegistry.containsKey(agentId)) {
                lastHeartbeat.put(agentId, LocalDateTime.now());
                AgentRegistration registration = agentRegistry.get(agentId);
                registration.setLastSeen(LocalDateTime.now());
                registration.setStatus(AgentRegistration.RegistrationStatus.ACTIVE);
                
                logMessage("💓 Heartbeat received from: " + agentId);
            }
            
        } catch (Exception e) {
            logMessage("❌ Failed to process heartbeat: " + e.getMessage());
        }
    }
    
    /**
     * Handle agent status updates
     */
    private void handleAgentStatusUpdate(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            String agentId = (String) payload.get("agentId");
            String status = (String) payload.get("status");
            
            if (agentId != null && agentRegistry.containsKey(agentId)) {
                AgentRegistration registration = agentRegistry.get(agentId);
                registration.setStatus(AgentRegistration.RegistrationStatus.valueOf(status.toUpperCase()));
                registration.setLastSeen(LocalDateTime.now());
                
                logMessage("📊 Status update for " + agentId + ": " + status);
            }
            
        } catch (Exception e) {
            logMessage("❌ Failed to process status update: " + e.getMessage());
        }
    }
    
    /**
     * Find agents by capability
     */
    public List<Map<String, Object>> findAgentsByCapability(String capability) {
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> agentIds = capabilityIndex.get(capability);
        
        if (agentIds != null) {
            for (String agentId : agentIds) {
                AgentRegistration registration = agentRegistry.get(agentId);
                if (registration != null && registration.getStatus() == AgentRegistration.RegistrationStatus.ACTIVE) {
                    result.add(registration.toMap());
                }
            }
        }
        
        return result;
    }
    
    /**
     * Find agents by type
     */
    public List<Map<String, Object>> findAgentsByType(String agentType) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (AgentRegistration registration : agentRegistry.values()) {
            if (agentType.equals(registration.getAgentType()) && 
                registration.getStatus() == AgentRegistration.RegistrationStatus.ACTIVE) {
                result.add(registration.toMap());
            }
        }
        
        return result;
    }
    
    /**
     * Get agent registration by ID
     */
    public Map<String, Object> getAgentRegistration(String agentId) {
        AgentRegistration registration = agentRegistry.get(agentId);
        return registration != null ? registration.toMap() : null;
    }
    
    /**
     * Get all registered agents
     */
    public List<Map<String, Object>> getAllAgents() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AgentRegistration registration : agentRegistry.values()) {
            result.add(registration.toMap());
        }
        return result;
    }
    
    /**
     * Get registry statistics
     */
    public Map<String, Object> getRegistryStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long activeAgents = agentRegistry.values().stream()
            .mapToLong(reg -> reg.getStatus() == AgentRegistration.RegistrationStatus.ACTIVE ? 1 : 0)
            .sum();
        
        stats.put("totalAgents", agentRegistry.size());
        stats.put("activeAgents", activeAgents);
        stats.put("totalCapabilities", capabilityIndex.size());
        stats.put("registrationCount", registrationCount.get());
        stats.put("lastUpdated", LocalDateTime.now().toString());
        
        // Capability breakdown
        Map<String, Integer> capabilityStats = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : capabilityIndex.entrySet()) {
            capabilityStats.put(entry.getKey(), entry.getValue().size());
        }
        stats.put("capabilityBreakdown", capabilityStats);
        
        return stats;
    }
    
    /**
     * Remove inactive agents (would be called by cleanup scheduler)
     */
    public void cleanupInactiveAgents() {
        LocalDateTime now = LocalDateTime.now();
        List<String> toRemove = new ArrayList<>();
        
        for (Map.Entry<String, LocalDateTime> entry : lastHeartbeat.entrySet()) {
            String agentId = entry.getKey();
            LocalDateTime lastSeen = entry.getValue();
            
            if (now.isAfter(lastSeen.plusNanos(heartbeatTimeoutMs * 1_000_000))) {
                toRemove.add(agentId);
            }
        }
        
        for (String agentId : toRemove) {
            removeAgent(agentId);
        }
        
        if (!toRemove.isEmpty()) {
            logMessage("🧹 Cleaned up " + toRemove.size() + " inactive agents");
        }
    }
    
    /**
     * Remove agent from registry
     */
    private void removeAgent(String agentId) {
        AgentRegistration registration = agentRegistry.remove(agentId);
        if (registration != null) {
            // Remove from capability index
            for (String capability : registration.getCapabilities()) {
                Set<String> agents = capabilityIndex.get(capability);
                if (agents != null) {
                    agents.remove(agentId);
                    if (agents.isEmpty()) {
                        capabilityIndex.remove(capability);
                    }
                }
            }
            
            lastHeartbeat.remove(agentId);
            logMessage("🗑️ Removed inactive agent: " + agentId);
        }
    }
    
    /**
     * Log registry activities with timestamp
     */
    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [" + getAgentId() + "] " + message);
    }
    
    @Override
    public String toString() {
        return String.format("RegistryAgent[agents=%d, capabilities=%d, registrations=%d]", 
                           agentRegistry.size(), capabilityIndex.size(), registrationCount.get());
    }
}