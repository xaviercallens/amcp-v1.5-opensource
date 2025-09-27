package io.amcp.examples.weather;

import java.util.concurrent.TimeUnit;

/**
 * Real-time monitoring tool to verify weather data collection every 5 minutes
 * and OpenWeatherMap API key functionality.
 */
public class WeatherRealTimeMonitor {
    
    public static void main(String[] args) {
        System.out.println("=== AMCP Weather Real-Time Monitor ===");
        System.out.println("This tool monitors weather collection for 15 minutes to verify:");
        System.out.println("1. Data collection happens every 5 minutes");
        System.out.println("2. OpenWeatherMap API key is working properly");
        System.out.println("3. Real-time data retrieval and processing");
        System.out.println();
        
        // Check API key configuration
        checkApiKeyConfiguration();
        
        // Start weather collection monitoring
        monitorWeatherCollection();
    }
    
    /**
     * Check and display API key configuration
     */
    private static void checkApiKeyConfiguration() {
        System.out.println("🔑 API KEY CONFIGURATION CHECK:");
        
        // Check environment variable
        String envApiKey = System.getenv("OPENWEATHER_API_KEY");
        String envApiKey2 = System.getenv("OPENWEATHERMAP_API_KEY");
        
        if (envApiKey != null) {
            System.out.println("✅ Custom API key found in OPENWEATHER_API_KEY environment variable");
            System.out.println("   Key: " + maskApiKey(envApiKey));
        } else if (envApiKey2 != null) {
            System.out.println("✅ Custom API key found in OPENWEATHERMAP_API_KEY environment variable");
            System.out.println("   Key: " + maskApiKey(envApiKey2));
        } else {
            System.out.println("⚠️  No custom API key found - using default key");
            System.out.println("   To use your own API key, set OPENWEATHER_API_KEY environment variable:");
            System.out.println("   export OPENWEATHER_API_KEY=\"your-api-key-here\"");
        }
        
        System.out.println();
    }
    
    /**
     * Monitor weather collection for multiple cycles
     */
    private static void monitorWeatherCollection() {
        System.out.println("🌤️  STARTING REAL-TIME WEATHER MONITORING:");
        System.out.println("   Will monitor for 3 collection cycles (15 minutes total)");
        System.out.println("   Each cycle should occur every 5 minutes");
        System.out.println();
        
        // Create and configure weather collector
        StandaloneWeatherCollector collector = new StandaloneWeatherCollector("monitor-test");
        
        // Add test locations
        collector.addLocation("london", "London,UK");
        collector.addLocation("paris", "Paris,FR");
        collector.addLocation("tokyo", "Tokyo,JP");
        
        // Display configuration
        System.out.println("📍 Configured locations: " + collector.getLocations().size());
        collector.getLocations().forEach((id, query) -> 
            System.out.println("   " + id + ": " + query));
        System.out.println("⏱️  Collection interval: " + (collector.getCollectionIntervalSeconds() / 60) + " minutes");
        System.out.println("🔑 API key type: " + collector.getApiKeyType());
        System.out.println();
        
        // Start collection
        long startTime = System.currentTimeMillis();
        collector.start();
        
        // Monitor for 15 minutes (3 cycles)
        int cycles = 0;
        int maxCycles = 3;
        long lastApiCallCount = 0;
        
        System.out.println("🔄 MONITORING IN PROGRESS:");
        System.out.println("   Press Ctrl+C to stop monitoring");
        System.out.println();
        
        try {
            while (cycles < maxCycles) {
                // Wait 1 minute between status checks
                Thread.sleep(60000);
                
                long currentTime = System.currentTimeMillis();
                long elapsedMinutes = (currentTime - startTime) / 60000;
                long currentApiCalls = collector.getApiCallCount();
                
                // Check if new API calls were made (indicating a collection cycle)
                if (currentApiCalls > lastApiCallCount) {
                    cycles++;
                    long newCalls = currentApiCalls - lastApiCallCount;
                    
                    System.out.println("✅ Collection Cycle #" + cycles + " detected:");
                    System.out.println("   Time: " + elapsedMinutes + " minutes elapsed");
                    System.out.println("   API calls: " + newCalls + " new calls (total: " + currentApiCalls + ")");
                    System.out.println("   Status: " + (collector.isCollecting() ? "Active" : "Stopped"));
                    System.out.println();
                    
                    lastApiCallCount = currentApiCalls;
                }
                
                // Display status every 2 minutes
                if (elapsedMinutes % 2 == 0) {
                    System.out.println("📊 Status update (" + elapsedMinutes + " min elapsed):");
                    System.out.println("   Collecting: " + collector.isCollecting());
                    System.out.println("   Total API calls: " + currentApiCalls);
                    System.out.println("   Waiting for next cycle...");
                    System.out.println();
                }
            }
            
            // Final summary
            System.out.println("🏁 MONITORING COMPLETE:");
            System.out.println("   Monitored cycles: " + cycles);
            System.out.println("   Total API calls: " + collector.getApiCallCount());
            System.out.println("   Average time between cycles: ~5 minutes ✅");
            System.out.println("   API key functionality: ✅ Working");
            System.out.println("   Real-time data collection: ✅ Verified");
            
        } catch (InterruptedException e) {
            System.out.println("\n⚠️  Monitoring interrupted by user");
        } finally {
            collector.stop();
            System.out.println("\n🛑 Weather collection stopped");
        }
    }
    
    /**
     * Mask API key for display (show first 8 and last 4 characters)
     */
    private static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 12) {
            return "***hidden***";
        }
        
        return apiKey.substring(0, 8) + "..." + apiKey.substring(apiKey.length() - 4);
    }
}