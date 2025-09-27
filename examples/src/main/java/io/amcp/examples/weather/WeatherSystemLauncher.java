package io.amcp.examples.weather;

import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simple standalone launcher for the Weather Collector system.
 * This provides a CLI interface to create and manage StandaloneWeatherCollector instances.
 */
public class WeatherSystemLauncher {
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private StandaloneWeatherCollector weatherCollector;
    private boolean isRunning = false;
    
    public WeatherSystemLauncher() {
        System.out.println("=== Weather Collection System ===");
        System.out.println("Standalone Weather Collection Framework");
    }
    
    /**
     * Start the weather collection system
     */
    public void start() {
        try {
            System.out.println("Starting Weather Collection System...");
            
            // Create the weather collector
            String agentId = "weather-collector-" + System.currentTimeMillis();
            weatherCollector = new StandaloneWeatherCollector(agentId);
            
            // Start the collection process
            weatherCollector.start();
            
            System.out.println("Weather Collector created with ID: " + agentId);
            
            isRunning = true;
            System.out.println("Weather Collection System started successfully!");
            
        } catch (Exception e) {
            System.err.println("Failed to start Weather Collection System: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Add a location for weather monitoring
     */
    public void addLocation(String locationId, String locationName) {
        if (weatherCollector != null) {
            weatherCollector.addLocation(locationId, locationName);
            System.out.println("Added location: " + locationName + " [" + locationId + "]");
        } else {
            System.err.println("Weather collector not initialized. Please start the system first.");
        }
    }
    
    /**
     * Remove a location from monitoring
     */
    public void removeLocation(String locationId) {
        if (weatherCollector != null) {
            weatherCollector.removeLocation(locationId);
            System.out.println("Removed location: " + locationId);
        } else {
            System.err.println("Weather collector not initialized. Please start the system first.");
        }
    }
    
    /**
     * Show current status
     */
    public void showStatus() {
        System.out.println("\n=== Weather System Status ===");
        System.out.println("System Status: " + (isRunning ? "RUNNING" : "STOPPED"));
        
        if (weatherCollector != null) {
            System.out.println("Collection Status: " + (weatherCollector.isCollecting() ? "ACTIVE" : "INACTIVE"));
            System.out.println("Locations configured: " + weatherCollector.getLocations().size());
            System.out.println("API Key: " + weatherCollector.getApiKeyType());
            System.out.println("Collection Interval: " + weatherCollector.getCollectionIntervalSeconds() + " seconds");
            System.out.println("Total API Calls: " + weatherCollector.getApiCallCount());
            
            System.out.println("\nConfigured Locations:");
            weatherCollector.getLocations().forEach((id, name) -> 
                System.out.println("  " + id + " -> " + name));
        } else {
            System.out.println("Weather collector: NOT INITIALIZED");
        }
        System.out.println();
    }
    
    /**
     * Show help information
     */
    public void showHelp() {
        System.out.println("\n=== Weather System Commands ===");
        System.out.println("start                                - Start the weather system");
        System.out.println("add <location-id> <location-name>    - Add a location (e.g., add nice Nice,FR)");
        System.out.println("remove <location-id>                 - Remove a location from monitoring");
        System.out.println("status                               - Show system status");
        System.out.println("help                                 - Show this help message");
        System.out.println("quit                                 - Exit the system");
        System.out.println();
        System.out.println("Example workflow:");
        System.out.println("  start");
        System.out.println("  add nice Nice,FR");
        System.out.println("  status");
        System.out.println();
        System.out.println("Note: Weather collection starts automatically when the system is started");
        System.out.println("      and runs every 5 minutes for all configured locations.");
        System.out.println("      The system uses API key: 3bd965f39881ba0f116ee0810fdfd058 by default");
        System.out.println("      Set OPENWEATHER_API_KEY environment variable to use a different key.");
        System.out.println();
    }
    
    /**
     * Process CLI commands
     */
    public void processCommand(String command) {
        String[] parts = command.trim().split("\\s+", 3);
        if (parts.length == 0) return;
        
        String cmd = parts[0].toLowerCase();
        
        switch (cmd) {
            case "start":
                if (!isRunning) {
                    start();
                } else {
                    System.out.println("System is already running");
                }
                break;
                
            case "add":
                if (parts.length >= 3) {
                    addLocation(parts[1], parts[2]);
                } else {
                    System.out.println("Usage: add <location-id> <location-name>");
                    System.out.println("Example: add nice Nice,FR");
                }
                break;
                
            case "remove":
                if (parts.length >= 2) {
                    removeLocation(parts[1]);
                } else {
                    System.out.println("Usage: remove <location-id>");
                }
                break;
                
            case "status":
                showStatus();
                break;
                
            case "help":
                showHelp();
                break;
                
            case "quit":
            case "exit":
                shutdown();
                break;
                
            default:
                System.out.println("Unknown command: " + cmd + ". Type 'help' for available commands.");
        }
    }
    
    /**
     * Shutdown the system
     */
    public void shutdown() {
        System.out.println("Shutting down Weather Collection System...");
        
        if (weatherCollector != null) {
            weatherCollector.stop();
        }
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
        
        isRunning = false;
        System.out.println("Weather Collection System shutdown complete.");
    }
    
    /**
     * Main method
     */
    public static void main(String[] args) {
        WeatherSystemLauncher launcher = new WeatherSystemLauncher();
        
        // Show welcome message and help
        launcher.showHelp();
        
        // CLI loop
        Scanner scanner = new Scanner(System.in);
        System.out.println("Weather System CLI ready. Type 'help' for commands, 'quit' to exit.");
        
        while (true) {
            System.out.print("weather> ");
            String command = scanner.nextLine().trim();
            
            if (command.isEmpty()) {
                continue;
            }
            
            if ("quit".equalsIgnoreCase(command) || "exit".equalsIgnoreCase(command)) {
                launcher.shutdown();
                break;
            }
            
            try {
                launcher.processCommand(command);
            } catch (Exception e) {
                System.err.println("Error processing command: " + e.getMessage());
            }
        }
        
        scanner.close();
        System.out.println("Goodbye!");
    }
}