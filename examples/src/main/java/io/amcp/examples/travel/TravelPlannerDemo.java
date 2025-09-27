package io.amcp.examples.travel;

import io.amcp.core.AgentID;
import io.amcp.core.AgentContext;
import io.amcp.core.Event;
import io.amcp.core.DeliveryOptions;
import io.amcp.messaging.EventBroker;
import io.amcp.messaging.impl.InMemoryEventBroker;
import io.amcp.mobility.MobilityManager;
import io.amcp.core.impl.SimpleAgentContext;
import io.amcp.mobility.impl.SimpleMobilityManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * AMCP v1.5 Travel Planner Demo CLI
 * 
 * Interactive demonstration of the TravelPlannerAgent capabilities
 * including travel request processing, weather integration, and agent mobility.
 */
public class TravelPlannerDemo {
    
    private static final String DEMO_VERSION = "1.5.0";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private TravelPlannerAgent travelAgent;
    private AgentContext agentContext;
    private EventBroker eventBroker;
    private MobilityManager mobilityManager;
    private Scanner scanner;
    private volatile boolean running = true;
    
    public static void main(String[] args) {
        TravelPlannerDemo demo = new TravelPlannerDemo();
        demo.run();
    }
    
    public void run() {
        try {
            initializeDemo();
            runInteractiveCLI();
        } catch (Exception e) {
            logMessage("Demo error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }
    
    private void initializeDemo() {
        logMessage("Initializing AMCP v" + DEMO_VERSION + " Travel Planner Demo...");
        
        // Initialize components
        eventBroker = new InMemoryEventBroker();
        mobilityManager = new SimpleMobilityManager();
        
        // Create agent context
        agentContext = new SimpleAgentContext(eventBroker, mobilityManager);
        
        // Create and activate travel agent
        travelAgent = new TravelPlannerAgent(AgentID.named("travel-planner-demo"));
        
        try {
            // Register and activate travel agent
            agentContext.registerAgent(travelAgent).get(5, TimeUnit.SECONDS);
            agentContext.activateAgent(travelAgent.getAgentId()).get(5, TimeUnit.SECONDS);
            logMessage("Travel planner agent activated successfully");
        } catch (Exception e) {
            logMessage("Failed to activate travel agent: " + e.getMessage());
            throw new RuntimeException(e);
        }
        
        // Subscribe to travel events for demo purposes
        eventBroker.subscribe(new DemoEventSubscriber(), "travel.**");
        eventBroker.subscribe(new DemoEventSubscriber(), "system.**");
        
        scanner = new Scanner(System.in);
        
        logMessage("Demo initialization completed!");
    }
    
    private void runInteractiveCLI() {
        showWelcomeMessage();
        showHelp();
        
        while (running) {
            System.out.print("\\ntravel> ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) {
                continue;
            }
            
            processCommand(input);
        }
    }
    
    private void showWelcomeMessage() {
        System.out.println("\\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║           AMCP v1.5 Travel Planner Agent Demo                ║");
        System.out.println("║          Agent Mesh Communication Protocol Demo              ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("🌍 Welcome to the AMCP Travel Planning System!");
        System.out.println("This demo showcases intelligent travel planning with weather integration,");
        System.out.println("real-time event processing, and agent mobility capabilities.");
        System.out.println();
    }
    
    private void showHelp() {
        System.out.println("Available Commands:");
        System.out.println("  plan <origin> <destination> <start-date> [end-date]  - Create travel plan");
        System.out.println("  status                                                - Show system status");
        System.out.println("  plans                                                 - List active plans");
        System.out.println("  weather <city>                                        - Get weather info");
        System.out.println("  dispatch <device>                                     - Demonstrate mobility");
        System.out.println("  help                                                  - Show this help");
        System.out.println("  quit                                                  - Exit demo");
        System.out.println();
        System.out.println("Example: plan \"New York\" \"Paris\" \"2025-10-15\" \"2025-10-22\"");
    }
    
    private void processCommand(String input) {
        String[] parts = parseCommand(input);
        String command = parts[0].toLowerCase();
        
        try {
            switch (command) {
                case "plan":
                    handlePlanCommand(parts);
                    break;
                case "status":
                    handleStatusCommand();
                    break;
                case "plans":
                    handlePlansCommand();
                    break;
                case "weather":
                    handleWeatherCommand(parts);
                    break;
                case "dispatch":
                    handleDispatchCommand(parts);
                    break;
                case "help":
                case "h":
                    showHelp();
                    break;
                case "quit":
                case "exit":
                case "q":
                    handleQuitCommand();
                    break;
                default:
                    System.out.println("Unknown command: " + command);
                    System.out.println("Type 'help' for available commands.");
            }
        } catch (Exception e) {
            System.out.println("Error processing command: " + e.getMessage());
        }
    }
    
    private String[] parseCommand(String input) {
        // Simple parsing - split by spaces but respect quotes
        return input.split("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
    }
    
    private void handlePlanCommand(String[] parts) {
        if (parts.length < 4) {
            System.out.println("Usage: plan <origin> <destination> <start-date> [end-date]");
            System.out.println("Example: plan \"New York\" \"Paris\" \"2025-10-15\" \"2025-10-22\"");
            return;
        }
        
        String origin = parts[1].replaceAll("\"", "");
        String destination = parts[2].replaceAll("\"", "");
        String startDate = parts[3].replaceAll("\"", "");
        String endDate = parts.length > 4 ? parts[4].replaceAll("\"", "") : null;
        
        String requestId = "req-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("requestId", requestId);
        requestData.put("origin", origin);
        requestData.put("destination", destination);
        requestData.put("startDate", startDate);
        requestData.put("userId", "demo-user");
        
        if (endDate != null) {
            requestData.put("endDate", endDate);
        }
        
        // Create and publish travel request event
        Event travelRequest = Event.builder()
            .topic("travel.request.plan")
            .payload(requestData)
            .sender(AgentID.named("travel-demo-cli"))
            .timestamp(LocalDateTime.now())
            .correlationId(requestId)
            .deliveryOptions(DeliveryOptions.reliable())
            .build();
        
        System.out.println("🚀 Creating travel plan...");
        System.out.println("   Request ID: " + requestId);
        System.out.println("   Route: " + origin + " → " + destination);
        System.out.println("   Start Date: " + startDate);
        if (endDate != null) {
            System.out.println("   End Date: " + endDate);
        }
        
        eventBroker.publish(travelRequest);
        System.out.println("✅ Travel request submitted successfully!");
        System.out.println("   The travel agent will process your request and provide updates.");
    }
    
    private void handleStatusCommand() {
        System.out.println("\\n╔═══════════════════ SYSTEM STATUS ═══════════════════╗");
        System.out.println("║ Travel Agent: " + (travelAgent != null ? "ACTIVE" : "INACTIVE") + "                              ║");
        System.out.println("║ Agent ID: " + travelAgent.getAgentId() + "                    ║");
        System.out.println("║ Event Broker: " + (eventBroker != null ? "RUNNING" : "STOPPED") + "                            ║");
        System.out.println("║ Demo Version: " + DEMO_VERSION + "                                ║");
        System.out.println("║ Status: OPERATIONAL                                  ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
    }
    
    private void handlePlansCommand() {
        System.out.println("\\n📋 Active Travel Plans:");
        System.out.println("   (This feature would display current travel plans)");
        System.out.println("   In a full implementation, this would show:");
        System.out.println("   • Plan IDs and status");
        System.out.println("   • Origin and destination");
        System.out.println("   • Travel dates");
        System.out.println("   • Weather conditions");
        System.out.println("   • Recommendations");
    }
    
    private void handleWeatherCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: weather <city>");
            return;
        }
        
        String city = parts[1].replaceAll("\"", "");
        System.out.println("🌤️  Getting weather for " + city + "...");
        System.out.println("   (This would integrate with weather APIs)");
        System.out.println("   Temperature: 22°C");
        System.out.println("   Conditions: Partly cloudy");
        System.out.println("   Humidity: 65%");
        System.out.println("   Wind: 15 km/h");
    }
    
    private void handleDispatchCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("Usage: dispatch <device>");
            return;
        }
        
        String device = parts[1];
        System.out.println("🚀 Demonstrating agent mobility to: " + device);
        
        // Simulate agent dispatch
        Map<String, Object> mobilityData = new HashMap<>();
        mobilityData.put("targetDevice", device);
        mobilityData.put("agentId", travelAgent.getAgentId().toString());
        mobilityData.put("action", "dispatch");
        
        Event mobilityEvent = Event.builder()
            .topic("system.mobility.dispatch")
            .payload(mobilityData)
            .sender(AgentID.named("travel-demo-cli"))
            .timestamp(LocalDateTime.now())
            .deliveryOptions(DeliveryOptions.reliable())
            .build();
        
        eventBroker.publish(mobilityEvent);
        System.out.println("✅ Mobility demonstration event sent!");
    }
    
    private void handleQuitCommand() {
        System.out.println("\\n🛑 Shutting down travel planner demo...");
        running = false;
    }
    
    private void cleanup() {
        try {
            if (travelAgent != null) {
                agentContext.deactivateAgent(travelAgent.getAgentId()).get(3, TimeUnit.SECONDS);
                logMessage("Travel agent deactivated successfully");
            }
            
            if (eventBroker != null) {
                eventBroker.stop();
                logMessage("Event broker stopped");
            }
            
            if (scanner != null) {
                scanner.close();
            }
            
            System.out.println("✅ Travel planner demo shutdown complete.");
            System.out.println("Thank you for using AMCP v" + DEMO_VERSION + " Travel Planner!");
            
        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }
    
    private void logMessage(String message) {
        System.out.println("[" + TIME_FORMAT.format(LocalDateTime.now()) + "] " + message);
    }
    
    // Event subscriber for demo purposes
    private class DemoEventSubscriber implements EventBroker.EventSubscriber {
        
        @Override
        public CompletableFuture<Void> handleEvent(Event event) {
            return CompletableFuture.runAsync(() -> {
                String topic = event.getTopic();
                
                if (topic.startsWith("travel.")) {
                    System.out.println("\\n📨 Travel Event: " + topic);
                    if (event.getPayload() instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) event.getPayload();
                        
                        if (topic.contains("plan.created")) {
                            System.out.println("   ✅ Travel plan created successfully!");
                        } else if (topic.contains("plan.updated")) {
                            System.out.println("   📝 Travel plan updated");
                        } else if (topic.contains("weather.analyzed")) {
                            System.out.println("   🌤️  Weather analysis completed");
                        } else {
                            System.out.println("   📊 Processing: " + data.getOrDefault("status", "unknown"));
                        }
                    }
                    System.out.print("\\ntravel> ");
                }
            });
        }
        
        @Override
        public String getSubscriberId() {
            return "demo-subscriber";
        }
    }
}