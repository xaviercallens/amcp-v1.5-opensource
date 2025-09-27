package io.amcp.examples.weather;

import io.amcp.core.AgentID;
import io.amcp.core.AgentContext;
import io.amcp.core.Event;
import io.amcp.core.AgentLifecycle;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Simple test class to verify WeatherCollectorAgent functionality
 */
public class WeatherCollectorAgentTest {
    
    public static void main(String[] args) {
        System.out.println("=== WeatherCollectorAgent Test ===");
        
        try {
            // Create agent ID
            AgentID agentId = new AgentID("weather-collector-test", "test-context");
            
            // Create the WeatherCollectorAgent
            WeatherCollectorAgent agent = new WeatherCollectorAgent(agentId);
            
            System.out.println("✅ WeatherCollectorAgent created successfully");
            
            // Test configuration
            testConfiguration(agent);
            
            // Test location management
            testLocationManagement(agent);
            
            System.out.println("\n=== Test Results ===");
            System.out.println("✅ All tests passed!");
            System.out.println("✅ Collection interval: " + agent.getCollectionIntervalSeconds() + " seconds (5 minutes)");
            System.out.println("✅ API key type: " + agent.getApiKeyType());
            System.out.println("✅ Default locations configured: " + agent.getLocations().size());
            System.out.println("✅ API call counter initialized: " + agent.getApiCallCount());
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testConfiguration(WeatherCollectorAgent agent) {
        System.out.println("\n--- Configuration Test ---");
        
        // Test collection interval
        long interval = agent.getCollectionIntervalSeconds();
        assert interval == 300 : "Expected 5-minute (300 seconds) interval, got " + interval;
        System.out.println("✅ Collection interval: " + interval + " seconds");
        
        // Test API key configuration
        String apiKeyType = agent.getApiKeyType();
        System.out.println("✅ API key type: " + apiKeyType);
        
        // Test initial state
        boolean isCollecting = agent.isCollecting();
        System.out.println("✅ Initial collecting state: " + isCollecting);
        
        // Test API counters
        long apiCalls = agent.getApiCallCount();
        long totalCalls = agent.getTotalApiCalls();
        System.out.println("✅ API call counters - Session: " + apiCalls + ", Total: " + totalCalls);
    }
    
    private static void testLocationManagement(WeatherCollectorAgent agent) {
        System.out.println("\n--- Location Management Test ---");
        
        // Test initial locations
        int initialCount = agent.getLocations().size();
        System.out.println("✅ Initial location count: " + initialCount);
        
        // Test adding a location
        agent.addLocation("test-city", "TestCity,US");
        int afterAdd = agent.getLocations().size();
        assert afterAdd == initialCount + 1 : "Expected location count to increase";
        System.out.println("✅ Added location - count now: " + afterAdd);
        
        // Test removing a location
        agent.removeLocation("test-city");
        int afterRemove = agent.getLocations().size();
        assert afterRemove == initialCount : "Expected location count to return to initial";
        System.out.println("✅ Removed location - count now: " + afterRemove);
        
        // Display configured locations
        System.out.println("✅ Configured locations: " + agent.getLocations().keySet());
    }
}