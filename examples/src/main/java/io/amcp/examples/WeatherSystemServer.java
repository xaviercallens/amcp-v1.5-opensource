package io.amcp.examples.weather;

import io.amcp.core.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Weather System orchestration server that manages multiple weather agents,
 * provides CLI interface, and coordinates the entire weather monitoring system.
 */
public class WeatherSystemServer {
    
    private static final String DEFAULT_CONTEXT_ID = "weather-system-context";
    
    private final Map<String, String> locations = new ConcurrentHashMap<>();
    private final Map<String, AgentID> collectorAgents = new ConcurrentHashMap<>();
    private final Map<String, AgentID> consumerAgents = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    
    private AgentContext agentContext;
    private SystemOrchestratorAgent orchestratorAgent;
    private boolean isRunning = false;
    
    public WeatherSystemServer() {
        // Default locations for testing
        initializeDefaultLocations();
    }
    
    private void initializeDefaultLocations() {
        locations.put("toronto", "Toronto,CA");
        locations.put("vancouver", "Vancouver,CA");
        locations.put("montreal", "Montreal,CA");
        locations.put("calgary", "Calgary,CA");
    }
    
    /**
     * Starts the weather system server
     */
    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(() -> {
            try {
                System.out.println("=== Weather System Server Starting ===");
                
                // Initialize agent context (this would normally be injected)
                initializeAgentContext();
                
                // Create orchestrator agent
                createOrchestratorAgent();
                
                // Deploy default agents for configured locations
                deployDefaultAgents();
                
                isRunning = true;
                System.out.println("Weather System Server started successfully");
                
            } catch (Exception e) {
                System.err.println("Failed to start Weather System Server: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }
    
    private void initializeAgentContext() {
        // In a real system, this would be dependency injected
        // For now, we'll create a mock context
        System.out.println("Initializing agent context: " + DEFAULT_CONTEXT_ID);
    }
    
    private void createOrchestratorAgent() {
        try {
            AgentID orchestratorId = new AgentID("weather-system-orchestrator", DEFAULT_CONTEXT_ID);
            orchestratorAgent = new SystemOrchestratorAgent(orchestratorId, this);
            
            // In a real system, we would add the agent to the context
            System.out.println("System orchestrator agent created: " + orchestratorId);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create orchestrator agent", e);
        }
    }
    
    private void deployDefaultAgents() {
        System.out.println("Deploying agents for " + locations.size() + " locations...");
        
        for (Map.Entry<String, String> location : locations.entrySet()) {
            deployAgentsForLocation(location.getKey(), location.getValue());
        }
        
        System.out.println("Default agents deployed successfully");
    }
    
    /**
     * Deploys collector and consumer agents for a specific location
     */
    public void deployAgentsForLocation(String locationId, String locationName) {
        try {
            // Create collector agent
            AgentID collectorId = new AgentID("weather-collector-" + locationId, DEFAULT_CONTEXT_ID);
            WeatherCollectorAgent collector = new WeatherCollectorAgent(collectorId);
            
            // Configure collector for this location
            collector.addLocation(locationId, locationName);
            
            collectorAgents.put(locationId, collectorId);
            System.out.println("Deployed collector for " + locationName + " [" + collectorId + "]");
            
            // Create consumer agents (alert consumer and aggregation consumer)
            deployConsumerAgents(locationId, locationName);
            
        } catch (Exception e) {
            System.err.println("Failed to deploy agents for location " + locationName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void deployConsumerAgents(String locationId, String locationName) {
        // Deploy alert consumer
        AgentID alertConsumerId = new AgentID("weather-alert-consumer-" + locationId, DEFAULT_CONTEXT_ID);
        WeatherConsumerAgent alertConsumer = new WeatherConsumerAgent(
            alertConsumerId, "AlertConsumer", true, false, 100);
        
        // Configure thresholds for this location
        alertConsumer.setTemperatureThreshold(locationId, 30.0); // 30°C threshold
        alertConsumer.setWindSpeedThreshold(locationId, 25.0);   // 25 km/h threshold
        alertConsumer.setPressureThreshold(locationId, 1000.0);  // 1000 hPa threshold
        
        consumerAgents.put("alert-" + locationId, alertConsumerId);
        System.out.println("Deployed alert consumer for " + locationName + " [" + alertConsumerId + "]");
        
        // Deploy aggregation consumer
        AgentID aggregationConsumerId = new AgentID("weather-aggregation-consumer-" + locationId, DEFAULT_CONTEXT_ID);
        WeatherConsumerAgent aggregationConsumer = new WeatherConsumerAgent(
            aggregationConsumerId, "AggregationConsumer", false, true, 1000);
        
        consumerAgents.put("aggregation-" + locationId, aggregationConsumerId);
        System.out.println("Deployed aggregation consumer for " + locationName + " [" + aggregationConsumerId + "]");
    }
    
    /**
     * CLI Command processing
     */
    public void processCommand(String command) {
        String[] parts = command.trim().split("\\s+");
        if (parts.length == 0) return;
        
        String cmd = parts[0].toLowerCase();
        
        switch (cmd) {
            case "help":
                showHelp();
                break;
                
            case "status":
                showSystemStatus();
                break;
                
            case "locations":
                showLocations();
                break;
                
            case "add-location":
                if (parts.length >= 3) {
                    addLocation(parts[1], String.join(" ", Arrays.copyOfRange(parts, 2, parts.length)));
                } else {
                    System.out.println("Usage: add-location <id> <name>");
                }
                break;
                
            case "remove-location":
                if (parts.length >= 2) {
                    removeLocation(parts[1]);
                } else {
                    System.out.println("Usage: remove-location <id>");
                }
                break;
                
            case "agents":
                showAgents();
                break;
                
            case "start-collection":
                if (parts.length >= 2) {
                    startCollection(parts[1]);
                } else {
                    System.out.println("Usage: start-collection <location-id>");
                }
                break;
                
            case "stop-collection":
                if (parts.length >= 2) {
                    stopCollection(parts[1]);
                } else {
                    System.out.println("Usage: stop-collection <location-id>");
                }
                break;
                
            case "set-threshold":
                if (parts.length >= 4) {
                    setThreshold(parts[1], parts[2], Double.parseDouble(parts[3]));
                } else {
                    System.out.println("Usage: set-threshold <location-id> <temperature|wind|pressure> <value>");
                }
                break;
                
            case "generate-report":
                generateSystemReport();
                break;
                
            case "shutdown":
                shutdown();
                break;
                
            default:
                System.out.println("Unknown command: " + cmd + ". Type 'help' for available commands.");
        }
    }
    
    private void showHelp() {
        System.out.println("\n=== Weather System Server Commands ===");
        System.out.println("help                                 - Show this help message");
        System.out.println("status                               - Show system status");
        System.out.println("locations                            - List all configured locations");
        System.out.println("add-location <id> <name>             - Add a new location to monitor");
        System.out.println("remove-location <id>                 - Remove a location from monitoring");
        System.out.println("agents                               - List all deployed agents");
        System.out.println("start-collection <location-id>       - Start weather collection for location");
        System.out.println("stop-collection <location-id>        - Stop weather collection for location");
        System.out.println("set-threshold <loc> <type> <value>   - Set alert threshold (temperature|wind|pressure)");
        System.out.println("generate-report                      - Generate comprehensive system report");
        System.out.println("shutdown                             - Shutdown the weather system");
        System.out.println();
    }
    
    private void showSystemStatus() {
        System.out.println("\n=== System Status ===");
        System.out.println("Server Status: " + (isRunning ? "RUNNING" : "STOPPED"));
        System.out.println("Context ID: " + DEFAULT_CONTEXT_ID);
        System.out.println("Locations Monitored: " + locations.size());
        System.out.println("Collector Agents: " + collectorAgents.size());
        System.out.println("Consumer Agents: " + consumerAgents.size());
        System.out.println("System Start Time: " + Instant.now());
        System.out.println();
    }
    
    private void showLocations() {
        System.out.println("\n=== Configured Locations ===");
        if (locations.isEmpty()) {
            System.out.println("No locations configured");
        } else {
            for (Map.Entry<String, String> location : locations.entrySet()) {
                String status = collectorAgents.containsKey(location.getKey()) ? "ACTIVE" : "INACTIVE";
                System.out.printf("%-15s %-25s [%s]\n", location.getKey(), location.getValue(), status);
            }
        }
        System.out.println();
    }
    
    private void addLocation(String locationId, String locationName) {
        if (locations.containsKey(locationId)) {
            System.out.println("Location already exists: " + locationId);
            return;
        }
        
        locations.put(locationId, locationName);
        System.out.println("Location added: " + locationName + " [" + locationId + "]");
        
        // Automatically deploy agents for the new location
        deployAgentsForLocation(locationId, locationName);
    }
    
    private void removeLocation(String locationId) {
        if (!locations.containsKey(locationId)) {
            System.out.println("Location not found: " + locationId);
            return;
        }
        
        String locationName = locations.remove(locationId);
        
        // Remove associated agents
        AgentID collectorId = collectorAgents.remove(locationId);
        consumerAgents.remove("alert-" + locationId);
        consumerAgents.remove("aggregation-" + locationId);
        
        System.out.println("Location removed: " + locationName + " [" + locationId + "]");
        if (collectorId != null) {
            System.out.println("Associated agents stopped");
        }
    }
    
    private void showAgents() {
        System.out.println("\n=== Deployed Agents ===");
        
        System.out.println("Collector Agents:");
        for (Map.Entry<String, AgentID> entry : collectorAgents.entrySet()) {
            System.out.println("  " + entry.getKey() + " -> " + entry.getValue());
        }
        
        System.out.println("Consumer Agents:");
        for (Map.Entry<String, AgentID> entry : consumerAgents.entrySet()) {
            System.out.println("  " + entry.getKey() + " -> " + entry.getValue());
        }
        System.out.println();
    }
    
    private void startCollection(String locationId) {
        if (!collectorAgents.containsKey(locationId)) {
            System.out.println("No collector agent found for location: " + locationId);
            return;
        }
        
        // In a real system, we would send a command to the agent
        System.out.println("Starting weather collection for " + locationId);
        // Send start command to collector agent
    }
    
    private void stopCollection(String locationId) {
        if (!collectorAgents.containsKey(locationId)) {
            System.out.println("No collector agent found for location: " + locationId);
            return;
        }
        
        System.out.println("Stopping weather collection for " + locationId);
        // Send stop command to collector agent
    }
    
    private void setThreshold(String locationId, String thresholdType, double value) {
        String alertConsumerKey = "alert-" + locationId;
        
        if (!consumerAgents.containsKey(alertConsumerKey)) {
            System.out.println("No alert consumer found for location: " + locationId);
            return;
        }
        
        // In a real system, we would send configuration to the agent
        System.out.println("Setting " + thresholdType + " threshold for " + locationId + " to " + value);
        // Send configuration command to consumer agent
    }
    
    private void generateSystemReport() {
        System.out.println("\n=== Weather System Report ===");
        System.out.println("Generated at: " + Instant.now());
        System.out.println();
        
        // System Overview
        showSystemStatus();
        
        // Location Details
        showLocations();
        
        // Agent Details
        showAgents();
        
        // Performance Metrics
        System.out.println("=== Performance Metrics ===");
        System.out.println("Total Events Processed: N/A (not implemented yet)");
        System.out.println("Average Response Time: N/A (not implemented yet)");
        System.out.println("Memory Usage: " + 
            (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024 + " MB");
        System.out.println();
    }
    
    public void shutdown() {
        System.out.println("Shutting down Weather System Server...");
        
        isRunning = false;
        
        // Shutdown scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
        
        // Stop all agents
        collectorAgents.clear();
        consumerAgents.clear();
        
        System.out.println("Weather System Server shutdown complete");
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Main method to run the Weather System Server
     */
    public static void main(String[] args) {
        WeatherSystemServer server = new WeatherSystemServer();
        
        // Start the server
        server.start().join();
        
        // Create CLI interface
        Scanner scanner = new Scanner(System.in);
        System.out.println("\nWeather System Server CLI ready. Type 'help' for commands.");
        
        while (server.isRunning()) {
            System.out.print("weather-system> ");
            String command = scanner.nextLine();
            
            if (command.trim().isEmpty()) {
                continue;
            }
            
            try {
                server.processCommand(command);
            } catch (Exception e) {
                System.err.println("Error processing command: " + e.getMessage());
            }
            
            if ("shutdown".equals(command.trim().toLowerCase())) {
                break;
            }
        }
        
        scanner.close();
    }
    
    /**
     * Inner class for system orchestration
     */
    private static class SystemOrchestratorAgent extends AbstractAgent {
        
        private final WeatherSystemServer server;
        
        public SystemOrchestratorAgent(AgentID agentId, WeatherSystemServer server) {
            super(agentId, "SystemOrchestrator");
            this.server = server;
        }
        
        @Override
        public CompletableFuture<Void> handleEvent(Event event) {
            return CompletableFuture.runAsync(() -> {
                try {
                    String topic = event.getTopic();
                    
                    if (topic.startsWith("weather.system.")) {
                        handleSystemEvent(event);
                    } else if (topic.startsWith("weather.alert.")) {
                        handleAlertEvent(event);
                    } else if (topic.startsWith("weather.report.")) {
                        handleReportEvent(event);
                    }
                    
                } catch (Exception e) {
                    System.err.println("Orchestrator error handling event: " + e.getMessage());
                }
            });
        }
        
        private void handleSystemEvent(Event event) {
            // Handle system-level events
            System.out.println("System event: " + event.getTopic());
        }
        
        private void handleAlertEvent(Event event) {
            // Handle weather alerts
            if (event.getPayload() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> alertData = (Map<String, Object>) event.getPayload();
                
                String location = (String) alertData.get("locationName");
                String level = (String) alertData.get("alertLevel");
                
                System.out.println("SYSTEM ALERT: " + level + " weather alert for " + location);
            }
        }
        
        private void handleReportEvent(Event event) {
            // Handle report requests
            System.out.println("Report request: " + event.getTopic());
        }
    }
}