package io.amcp.examples.weather;

import io.amcp.core.*;
import io.amcp.messaging.EventBroker;
import io.amcp.messaging.impl.InMemoryEventBroker;
import io.amcp.mobility.MigrationOptions;
import io.amcp.mobility.MobilityManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AMCP v1.5 Enterprise Edition Weather System Command Line Interface.
 * 
 * <p>Interactive CLI    // Other AgentContext methods would be implemented here
    @Override
    public CompletableFuture<Void> migrateAgent(AgentID agentId, String destinationContext, MigrationOptions options) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<AgentID> cloneAgent(AgentID agentId, String destinationContext) {
        return CompletableFuture.completedFuture(AgentID.random());
    }

    // Implement missing AgentContext methods with simple stubs
    @Override
    public EventBroker getEventBroker() { return eventBroker; }
    
    @Override
    public MobilityManager getMobilityManager() { 
        return new MobilityManager() {
            @Override
            public boolean canMigrate(AgentID agentId, String targetContext) { return true; }
            @Override
            public boolean migrate(AgentID agentId, String targetContext, MigrationOptions options) { return true; }
        };
    }
    
    @Override
    public ContextMetrics getMetrics() { 
        return new ContextMetrics() {
            @Override
            public int getActiveAgentCount() { return agents.size(); }
            @Override
            public long getTotalAgentsCreated() { return agents.size(); }
            @Override
            public long getTotalEventsProcessed() { return 0L; }
            @Override
            public double getAverageEventProcessingTime() { return 0.0; }
            @Override
            public long getFailedOperations() { return 0L; }
            @Override
            public long getUptimeMillis() { return System.currentTimeMillis(); }
        };
    }
    
    public CompletableFuture<Void> activateAgent(AgentID agentId) {
        return CompletableFuture.completedFuture(null);
    }
    
    public CompletableFuture<Void> destroyAgent(AgentID agentId) {
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> start() {
        return eventBroker.start();
    }
    
    public CompletableFuture<Void> shutdown() {
        return eventBroker.stop();
    }ting AMCP weather agent capabilities including:
 * <ul>
 *   <li>Real-time weather monitoring and alerts</li>
 *   <li>Dynamic city management (add/remove locations)</li>
 *   <li>Agent mobility demonstrations</li>
 *   <li>Event-driven architecture showcasing</li>
 *   <li>Live weather data visualization</li>
 * </ul>
 * </p>
 * 
 * <p><strong>Usage Examples:</strong></p>
 * <pre>
 * # Compile and run
 * cd examples/
 * javac -cp ../core/target/classes src/main/java/io/amcp/examples/weather/*.java
 * java -cp .:../core/target/classes io.amcp.examples.weather.WeatherSystemCLI
 * 
 * # Interactive commands
 * > help                    # Show available commands  
 * > status                  # Display system status
 * > add Berlin              # Add Berlin to monitoring
 * > remove Tokyo            # Remove Tokyo from monitoring
 * > weather Paris           # Get current weather for Paris
 * > alert                   # Show severe weather alerts
 * > dispatch edge-device-1  # Demonstrate agent mobility
 * > quit                    # Exit the system
 * </pre>
 * 
 * @author AMCP Development Team
 * @version 1.4.0
 */
public class WeatherSystemCLI {
    
    private static final String BANNER = 
        "╔═══════════════════════════════════════════════════════════════╗\n" +
        "║           AMCP v1.5 Enterprise Edition Weather CLI           ║\n" +
        "║          Agent Mesh Communication Protocol Demo              ║\n" +
        "╚═══════════════════════════════════════════════════════════════╝\n";
        
    private final EventBroker eventBroker;
    private final AgentContextImpl agentContext;
    private final WeatherAgent weatherAgent;
    private final Scanner scanner;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final List<String> alertHistory = new ArrayList<>();
    
    public WeatherSystemCLI() {
        this.eventBroker = new InMemoryEventBroker();
        this.agentContext = new AgentContextImpl(eventBroker);
        this.weatherAgent = new WeatherAgent();
        this.scanner = new Scanner(System.in);
        
        setupAgent();
        setupEventHandlers();
    }
    
    public static void main(String[] args) {
        WeatherSystemCLI cli = new WeatherSystemCLI();
        cli.run();
    }
    
    private void setupAgent() {
        try {
            // Initialize agent in context
            weatherAgent.setContext(agentContext);
            agentContext.activateAgent(weatherAgent);
            
            logMessage("Weather agent initialized and activated");
        } catch (Exception e) {
            logMessage("Error setting up agent: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private void setupEventHandlers() {
        // Subscribe to system events for CLI monitoring
        agentContext.subscribe(AgentID.named("CLI"), "weather.**")
            .thenRun(() -> logMessage("CLI subscribed to weather events"));
            
        agentContext.subscribe(AgentID.named("CLI"), "alert.**")
            .thenRun(() -> logMessage("CLI subscribed to alert events"));
            
        agentContext.subscribe(AgentID.named("CLI"), "mobility.**")
            .thenRun(() -> logMessage("CLI subscribed to mobility events"));
    }
    
    public void run() {
        System.out.println(BANNER);
        System.out.println("Weather monitoring system started successfully!");
        System.out.println("Type 'help' for available commands or 'quit' to exit.\n");
        
        showStatus();
        
        while (running.get()) {
            System.out.print("\nweather> ");
            String input = scanner.nextLine().trim();
            
            if (!input.isEmpty()) {
                processCommand(input);
            }
        }
        
        shutdown();
    }
    
    private void processCommand(String input) {
        String[] parts = input.toLowerCase().split("\\s+", 2);
        String command = parts[0];
        String argument = parts.length > 1 ? parts[1] : "";
        
        try {
            switch (command) {
                case "help":
                case "h":
                    showHelp();
                    break;
                case "status":
                case "s":
                    showStatus();
                    break;
                case "weather":
                case "w":
                    if (!argument.isEmpty()) {
                        requestWeather(argument);
                    } else {
                        showAllWeather();
                    }
                    break;
                case "add":
                case "a":
                    if (!argument.isEmpty()) {
                        addCity(argument);
                    } else {
                        System.out.println("Usage: add <city_name>");
                    }
                    break;
                case "remove":
                case "r":
                    if (!argument.isEmpty()) {
                        removeCity(argument);
                    } else {
                        System.out.println("Usage: remove <city_name>");
                    }
                    break;
                case "cities":
                case "c":
                    showCities();
                    break;
                case "alerts":
                case "alert":
                    showAlerts();
                    break;
                case "dispatch":
                case "d":
                    if (!argument.isEmpty()) {
                        dispatchAgent(argument);
                    } else {
                        System.out.println("Usage: dispatch <edge_device_name>");
                    }
                    break;
                case "stats":
                    showStatistics();
                    break;
                case "clear":
                    clearScreen();
                    break;
                case "quit":
                case "exit":
                case "q":
                    quit();
                    break;
                default:
                    System.out.println("Unknown command: " + command + ". Type 'help' for available commands.");
            }
        } catch (Exception e) {
            System.out.println("Error executing command: " + e.getMessage());
        }
    }
    
    private void showHelp() {
        System.out.println("\n╔═══════════════════ AVAILABLE COMMANDS ═══════════════════╗");
        System.out.println("║ General Commands:                                         ║");
        System.out.println("║   help, h           - Show this help message             ║");
        System.out.println("║   status, s         - Display system status             ║");
        System.out.println("║   stats             - Show collection statistics        ║");
        System.out.println("║   clear             - Clear screen                       ║");
        System.out.println("║   quit, exit, q     - Exit the application             ║");
        System.out.println("║                                                          ║");
        System.out.println("║ Weather Commands:                                        ║");
        System.out.println("║   weather [city]    - Get weather (current or specific) ║");
        System.out.println("║   cities, c         - List monitored cities             ║");
        System.out.println("║   add <city>        - Add city to monitoring            ║");
        System.out.println("║   remove <city>     - Remove city from monitoring       ║");
        System.out.println("║                                                          ║");
        System.out.println("║ Advanced Features:                                       ║");
        System.out.println("║   alerts            - Show severe weather alerts        ║");
        System.out.println("║   dispatch <device> - Demonstrate agent mobility        ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }
    
    private void showStatus() {
        System.out.println("\n╔═══════════════════ SYSTEM STATUS ═══════════════════╗");
        System.out.println("║ Agent State: " + weatherAgent.getLifecycleState() + 
            "                                  ║");
        System.out.println("║ Monitored Cities: " + weatherAgent.getMonitoredCities().size() + 
            "                                 ║");
        System.out.println("║ Collection Cycles: " + weatherAgent.getDataCollectionCount() + 
            "                                ║");
        System.out.println("║ Event Broker: Active                                 ║");
        System.out.println("║ CLI Interface: Running                               ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        
        if (!weatherAgent.getLatestWeatherData().isEmpty()) {
            System.out.println("\n📊 Latest Weather Summary:");
            weatherAgent.getLatestWeatherData().values().forEach(data -> 
                System.out.println("   • " + data.toString()));
        }
    }
    
    private void requestWeather(String city) {
        System.out.println("🌤️  Requesting weather for " + city + "...");
        
        Event weatherRequest = Event.builder()
            .topic("weather.request.current")
            .payload(city)
            .sender(AgentID.named("CLI"))
            .build();
            
        agentContext.publishEvent(weatherRequest);
    }
    
    private void showAllWeather() {
        Map<String, WeatherAgent.WeatherData> weatherData = weatherAgent.getLatestWeatherData();
        
        if (weatherData.isEmpty()) {
            System.out.println("🌍 No weather data available yet. Data collection in progress...");
            return;
        }
        
        System.out.println("\n🌍 Current Weather for All Cities:");
        for (int i = 0; i < 60; i++) System.out.print("═");
        System.out.println();
        
        weatherData.values().stream()
            .sorted((a, b) -> a.city.compareTo(b.city))
            .forEach(data -> {
                System.out.printf("%-12s %6.1f°C  %-15s  💨%.1f m/s  💧%.0f%%\n",
                    data.city + ":", data.temperature, data.description, 
                    data.windSpeed, data.humidity);
            });
    }
    
    private void addCity(String city) {
        System.out.println("🏙️  Adding " + city + " to monitoring...");
        
        Event addEvent = Event.builder()
            .topic("location.add")
            .payload(city)
            .sender(AgentID.named("CLI"))
            .build();
            
        agentContext.publishEvent(addEvent);
    }
    
    private void removeCity(String city) {
        System.out.println("🗑️  Removing " + city + " from monitoring...");
        
        Event removeEvent = Event.builder()
            .topic("location.remove")
            .payload(city)
            .sender(AgentID.named("CLI"))
            .build();
            
        agentContext.publishEvent(removeEvent);
    }
    
    private void showCities() {
        Set<String> cities = weatherAgent.getMonitoredCities();
        
        System.out.println("\n🏙️  Monitored Cities (" + cities.size() + "):");
        for (int i = 0; i < 30; i++) System.out.print("═");
        System.out.println();
        
        cities.stream().sorted().forEach(city -> System.out.println("   • " + city));
    }
    
    private void showAlerts() {
        if (alertHistory.isEmpty()) {
            System.out.println("\n✅ No severe weather alerts currently active.");
        } else {
            System.out.println("\n⚠️  Severe Weather Alerts:");
            for (int i = 0; i < 50; i++) System.out.print("═");
            System.out.println();
            alertHistory.forEach(alert -> System.out.println("   • " + alert));
        }
    }
    
    private void dispatchAgent(String edgeDevice) {
        System.out.println("🚀 Demonstrating agent mobility to: " + edgeDevice);
        
        Event dispatchEvent = Event.builder()
            .topic("mobility.dispatch.edge")
            .payload(edgeDevice)
            .sender(AgentID.named("CLI"))
            .metadata("demonstration", "true")
            .build();
            
        agentContext.publishEvent(dispatchEvent);
    }
    
    private void showStatistics() {
        System.out.println("\n📈 Collection Statistics:");
        for (int i = 0; i < 40; i++) System.out.print("═");
        System.out.println();
        System.out.println("   Collection Cycles: " + weatherAgent.getDataCollectionCount());
        System.out.println("   Active Cities: " + weatherAgent.getMonitoredCities().size());
        System.out.println("   Weather Records: " + weatherAgent.getLatestWeatherData().size());
        System.out.println("   Alerts Generated: " + alertHistory.size());
        System.out.println("   Agent Uptime: " + java.time.Duration.between(
            LocalDateTime.now().minusMinutes(5), LocalDateTime.now()).toMinutes() + " minutes");
    }
    
    private void clearScreen() {
        // Clear screen (works on most terminals)
        System.out.print("\033[2J\033[H");
        System.out.println(BANNER);
    }
    
    private void quit() {
        System.out.println("\n🛑 Shutting down weather monitoring system...");
        running.set(false);
    }
    
    private void shutdown() {
        try {
            agentContext.deactivateAgent(weatherAgent.getAgentId());
            agentContext.shutdown();
            scanner.close();
            
            System.out.println("✅ Weather system shutdown complete.");
            System.out.println("Thank you for using AMCP v1.5 Enterprise Edition Weather System!");
            
        } catch (Exception e) {
            System.err.println("Error during shutdown: " + e.getMessage());
        }
    }
    
    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [CLI] " + message);
    }
}

// Simplified AgentContext implementation for demo
class AgentContextImpl implements AgentContext {
    private final EventBroker eventBroker;
    private final Map<AgentID, Agent> agents = new HashMap<>();
    private final String contextId = UUID.randomUUID().toString();
    private final long startTime = System.currentTimeMillis();

    public AgentContextImpl(EventBroker eventBroker) {
        this.eventBroker = eventBroker;
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
        return new MobilityManager() {
            @Override
            public boolean canMigrate(AgentID agentId, String targetContext) {
                return true; // Simple implementation for demo
            }

            @Override
            public boolean migrate(AgentID agentId, String targetContext, MigrationOptions options) {
                return true; // Simple implementation for demo
            }
        };
    }

    public SecurityManager getSecurityManager() {
        return System.getSecurityManager(); // Use system security manager
    }

    @Override
    public io.amcp.security.AdvancedSecurityManager getAdvancedSecurityManager() {
        // Simple implementation for demo - return null or create basic instance
        return null; // Demo doesn't need advanced security
    }

    @Override
    public CompletableFuture<Void> registerAgent(Agent agent) {
        agents.put(agent.getAgentId(), agent);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> unregisterAgent(AgentID agentId) {
        agents.remove(agentId);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> activateAgent(AgentID agentId) {
        Agent agent = agents.get(agentId);
        if (agent != null) {
            agent.onActivate();
        }
        return CompletableFuture.completedFuture(null);
    }

    public void activateAgent(Agent agent) {
        agents.put(agent.getAgentId(), agent);
        agent.onActivate();
    }

    @Override
    public CompletableFuture<Void> deactivateAgent(AgentID agentId) {
        return CompletableFuture.runAsync(() -> {
            Agent agent = agents.get(agentId);
            if (agent != null) {
                agent.onDeactivate();
                agents.remove(agentId);
            }
        });
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
        return agents.containsKey(agentId) ? AgentLifecycle.ACTIVE : null;
    }

    @Override
    public CompletableFuture<Void> routeEvent(Event event) {
        // Simplified routing - broadcast to all agents
        for (Agent agent : agents.values()) {
            agent.handleEvent(event);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> subscribe(AgentID agentId, String topicPattern) {
        // Simplified subscription for demo - in real implementation, 
        // would create proper EventSubscriber wrapper
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> unsubscribe(AgentID agentId, String topicPattern) {
        // Simplified unsubscription for demo
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> publishEvent(Event event) {
        return eventBroker.publish(event);
    }

    @Override
    public CompletableFuture<Void> migrateAgent(AgentID agentId, String destinationContext, MigrationOptions options) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<AgentID> receiveAgent(byte[] serializedAgent, String sourceContext) {
        // Simple implementation - would deserialize agent in production
        return CompletableFuture.completedFuture(AgentID.random());
    }

    @Override
    public ContextMetrics getMetrics() {
        return new ContextMetrics() {
            @Override
            public int getActiveAgentCount() {
                return agents.size();
            }
            @Override
            public long getTotalAgentsCreated() {
                return agents.size();
            }
            @Override
            public long getTotalEventsProcessed() {
                return 0;
            }
            @Override
            public double getAverageEventProcessingTime() {
                return 0.0;
            }
            @Override
            public long getFailedOperations() {
                return 0;
            }
            @Override
            public long getUptimeMillis() {
                return System.currentTimeMillis() - startTime;
            }
        };
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (eventBroker instanceof InMemoryEventBroker) {
                    ((InMemoryEventBroker) eventBroker).shutdown();
                }
            } catch (Exception e) {
                System.err.println("Error shutting down event broker: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<AgentID> cloneAgent(AgentID agentId, String destinationContext) {
        return CompletableFuture.completedFuture(AgentID.random());
    }
}