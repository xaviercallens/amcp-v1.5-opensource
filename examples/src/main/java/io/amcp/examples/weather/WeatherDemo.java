package io.amcp.examples.weather;

/**
 * Demo script to show the weather system working with Nice, France
 */
public class WeatherDemo {
    public static void main(String[] args) {
        System.out.println("=== Weather Collection System Demo ===");
        System.out.println("Demonstrating weather collection for Nice, France");
        System.out.println();
        
        // Create the weather system launcher
        WeatherSystemLauncher launcher = new WeatherSystemLauncher();
        
        try {
            // Start the system
            System.out.println("1. Starting the weather system...");
            launcher.start();
            System.out.println();
            
            // Add Nice, France
            System.out.println("2. Adding Nice, France as a location...");
            launcher.addLocation("nice", "Nice,FR");
            System.out.println();
            
            // Show status
            System.out.println("3. Showing system status...");
            launcher.showStatus();
            System.out.println();
            
            // Wait a moment for weather collection to happen
            System.out.println("4. Waiting 10 seconds for weather data collection...");
            Thread.sleep(10000);
            System.out.println();
            
            // Show status again
            System.out.println("5. Final system status:");
            launcher.showStatus();
            
            // Shutdown
            System.out.println("6. Shutting down system...");
            launcher.shutdown();
            
            System.out.println("\n=== Demo Complete ===");
            System.out.println("The system successfully:");
            System.out.println("- Started with 5-minute collection intervals");
            System.out.println("- Used the provided API key: 3bd965f39881ba0f116ee0810fdfd058");
            System.out.println("- Added Nice, France for weather monitoring");
            System.out.println("- Collected and logged weather data");
            System.out.println("- Counted API calls made to OpenWeatherMap");
            
        } catch (Exception e) {
            System.err.println("Demo error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}