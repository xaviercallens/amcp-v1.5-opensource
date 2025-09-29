package io.amcp.connectors.ai;

import io.amcp.core.Agent;
import io.amcp.core.AgentContext;
import io.amcp.core.AgentID;
import io.amcp.core.AgentLifecycle;
import io.amcp.core.Event;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Agent Discovery and Registry System for AMCP v1.5
 * Enables agents to discover, register, and communicate with each other
 * via the AMCP protocol with A2A patterns.
 */
public class AgentRegistry implements Agent {
    
    private final AgentID agentId;
    private AgentContext context;
    private AgentLifecycle lifecycleState = AgentLifecycle.INACTIVE;
    private final Set<String> subscriptions = new CopyOnWriteArraySet<>();
    
    // Agent registry storage
    private final Map<String, AgentInfo> registeredAgents = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> agentCapabilities = new ConcurrentHashMap<>();
    
    public AgentRegistry() {
        this.agentId = AgentID.named("AgentRegistry");
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
            logMessage("Activating Agent Registry...");
            lifecycleState = AgentLifecycle.ACTIVE;
            
            // Subscribe to agent registration events
            subscriptions.add("agent.register.**");
            subscriptions.add("agent.discover.**");
            subscriptions.add("agent.heartbeat.**");
            subscriptions.add("agent.unregister.**");
            
            for (String topic : subscriptions) {
                subscribe(topic);
            }
            
            // Register core agents that should be available
            registerCoreAgents();
            
            logMessage("Agent Registry activated with " + subscriptions.size() + " subscriptions");
            
        } catch (Exception e) {
            logMessage("❌ Failed to activate Agent Registry: " + e.getMessage());
            throw new RuntimeException("Agent Registry activation failed", e);
        }
    }
    
    @Override
    public void onDeactivate() {
        logMessage("Deactivating Agent Registry...");
        lifecycleState = AgentLifecycle.INACTIVE;
        subscriptions.clear();
    }
    
    @Override
    public void onDestroy() {
        logMessage("Destroying Agent Registry...");
        subscriptions.clear();
        registeredAgents.clear();
        agentCapabilities.clear();
        lifecycleState = AgentLifecycle.INACTIVE;
    }
    
    @Override
    public void onBeforeMigration(String destinationContext) {
        logMessage("Preparing for migration to: " + destinationContext);
    }
    
    @Override
    public void onAfterMigration(String sourceContext) {
        logMessage("Completed migration from: " + sourceContext);
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
    
    private void registerCoreAgents() {
        // Register Weather Agent
        Set<String> weatherCapabilities = Set.of(
            "weather.current", "weather.forecast", "weather.alerts",
            "location.weather", "temperature", "humidity", "precipitation"
        );
        registerAgent(new AgentInfo(
            "WeatherAgent",
            "WeatherAgent",
            "Provides current weather conditions and forecasts for any location",
            weatherCapabilities,
            "weather.request.**"
        ));
        
        // Register Travel Planner Agent
        Set<String> travelCapabilities = Set.of(
            "travel.planning", "itinerary.creation", "destination.search",
            "accommodation.search", "flight.search", "activity.recommendations"
        );
        registerAgent(new AgentInfo(
            "TravelPlannerAgent",
            "TravelPlannerAgent", 
            "Plans and organizes travel itineraries with accommodation and activity suggestions",
            travelCapabilities,
            "travel.request.**"
        ));
        
        // Register Stock Price Agent
        Set<String> stockCapabilities = Set.of(
            "stock.price", "stock.quote", "market.data",
            "financial.analysis", "company.info", "ticker.lookup"
        );
        registerAgent(new AgentInfo(
            "StockPriceAgent",
            "StockPriceAgent",
            "Provides real-time stock prices and financial market information",
            stockCapabilities,
            "stock.request.**"
        ));
        
        logMessage("Registered " + registeredAgents.size() + " core agents");
    }
    
    private void registerAgent(AgentInfo agentInfo) {
        registeredAgents.put(agentInfo.getAgentId(), agentInfo);
        agentCapabilities.put(agentInfo.getAgentId(), agentInfo.getCapabilities());
        
        logMessage("Registered agent: " + agentInfo.getAgentId() + 
                  " (" + agentInfo.getCapabilities().size() + " capabilities)");
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                String topic = event.getTopic();
                logMessage("Processing event: " + topic);
                
                if (topic.startsWith("agent.discover.")) {
                    handleAgentDiscovery(event);
                } else if (topic.startsWith("agent.register.")) {
                    handleAgentRegistration(event);
                } else if (topic.startsWith("agent.unregister.")) {
                    handleAgentUnregistration(event);
                } else if (topic.startsWith("agent.heartbeat.")) {
                    handleAgentHeartbeat(event);
                }
                
            } catch (Exception e) {
                logMessage("❌ Failed to handle event: " + event.getTopic() + ": " + e.getMessage());
            }
        });
    }
    
    private void handleAgentDiscovery(Event event) {
        String query = event.getPayload(String.class);
        
        if (query == null || query.trim().isEmpty()) {
            // Return all registered agents
            publishAgentList(event.getCorrelationId());
        } else {
            // Search agents by capability
            publishAgentSearch(query, event.getCorrelationId());
        }
    }
    
    private void handleAgentRegistration(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = event.getPayload(Map.class);
            String agentId = (String) payload.get("agentId");
            String agentType = (String) payload.get("agentType");
            String description = (String) payload.get("description");
            @SuppressWarnings("unchecked")
            Set<String> capabilities = (Set<String>) payload.get("capabilities");
            String endpoint = (String) payload.get("endpoint");
            
            AgentInfo agentInfo = new AgentInfo(agentId, agentType, description, capabilities, endpoint);
            registerAgent(agentInfo);
            
            // Acknowledge registration
            publishEvent("agent.register.ack", Map.of(
                "agentId", agentId,
                "status", "registered",
                "timestamp", LocalDateTime.now().toString()
            ));
            
        } catch (Exception e) {
            logMessage("❌ Failed to register agent: " + e.getMessage());
        }
    }
    
    private void handleAgentUnregistration(Event event) {
        String agentId = event.getPayload(String.class);
        if (registeredAgents.remove(agentId) != null) {
            agentCapabilities.remove(agentId);
            logMessage("Unregistered agent: " + agentId);
        }
    }
    
    private void handleAgentHeartbeat(Event event) {
        String agentId = event.getPayload(String.class);
        if (registeredAgents.containsKey(agentId)) {
            logMessage("Heartbeat received from: " + agentId);
        }
    }
    
    private void publishAgentList(String correlationId) {
        Map<String, Object> response = Map.of(
            "agents", registeredAgents,
            "count", registeredAgents.size(),
            "timestamp", LocalDateTime.now().toString()
        );
        
        publishEvent(Event.builder()
                .topic("agent.discover.response")
                .payload(response)
                .correlationId(correlationId)
                .sender(agentId)
                .build());
    }
    
    private void publishAgentSearch(String query, String correlationId) {
        Map<String, AgentInfo> matchingAgents = new ConcurrentHashMap<>();
        
        // Search by capability
        for (Map.Entry<String, AgentInfo> entry : registeredAgents.entrySet()) {
            AgentInfo agent = entry.getValue();
            if (agent.getCapabilities().stream()
                    .anyMatch(cap -> cap.toLowerCase().contains(query.toLowerCase()))) {
                matchingAgents.put(entry.getKey(), agent);
            }
        }
        
        Map<String, Object> response = Map.of(
            "query", query,
            "agents", matchingAgents,
            "count", matchingAgents.size(),
            "timestamp", LocalDateTime.now().toString()
        );
        
        publishEvent(Event.builder()
                .topic("agent.discover.response")
                .payload(response)
                .correlationId(correlationId)
                .sender(agentId)
                .build());
    }
    
    /**
     * Public API methods for ChatAgent integration
     */
    public CompletableFuture<Set<AgentInfo>> discoverAgents() {
        return CompletableFuture.supplyAsync(() -> Set.copyOf(registeredAgents.values()));
    }
    
    public CompletableFuture<Set<AgentInfo>> findAgentsByCapability(String capability) {
        return CompletableFuture.supplyAsync(() -> {
            Set<AgentInfo> matches = new HashSet<>();
            for (AgentInfo agent : registeredAgents.values()) {
                if (agent.getCapabilities().stream()
                        .anyMatch(cap -> cap.toLowerCase().contains(capability.toLowerCase()))) {
                    matches.add(agent);
                }
            }
            return matches;
        });
    }
    
    public AgentInfo getAgent(String agentId) {
        return registeredAgents.get(agentId);
    }
    
    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [AgentRegistry] " + message);
    }
    
    /**
     * Agent information structure
     */
    public static class AgentInfo {
        private final String agentId;
        private final String agentType;
        private final String description;
        private final Set<String> capabilities;
        private final String endpoint;
        private final LocalDateTime registeredAt;
        
        public AgentInfo(String agentId, String agentType, String description, 
                        Set<String> capabilities, String endpoint) {
            this.agentId = agentId;
            this.agentType = agentType;
            this.description = description;
            this.capabilities = capabilities;
            this.endpoint = endpoint;
            this.registeredAt = LocalDateTime.now();
        }
        
        // Getters
        public String getAgentId() { return agentId; }
        public String getAgentType() { return agentType; }
        public String getDescription() { return description; }
        public Set<String> getCapabilities() { return capabilities; }
        public String getEndpoint() { return endpoint; }
        public LocalDateTime getRegisteredAt() { return registeredAt; }
    }
}