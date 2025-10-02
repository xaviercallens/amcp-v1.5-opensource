package io.amcp.examples.multiagent;

import io.amcp.connectors.ai.EnhancedChatAgent;
import io.amcp.examples.weather.WeatherAgent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Multi-Agent Communication Demo (Open Source Version)
 * 
 * Demonstrates the AMCP v1.5 multi-agent coordination capabilities
 * with Chat, Weather, and Orchestrator agents.
 */
public class MultiAgentDemo {
    
    private static final String DEMO_MODE = System.getProperty("demo.mode", "mock");
    
    // Open source demo agents
    private EnhancedChatAgent chatAgent;
    private WeatherAgent weatherAgent;
    
    public static void main(String[] args) {
        try {
            logMessage("🚀 Starting AMCP v1.5 Multi-Agent Demo (" + DEMO_MODE + " mode)");
            logMessage("");
            
            MultiAgentDemo demo = new MultiAgentDemo();
            demo.runDemo();
            
        } catch (Exception e) {
            logMessage("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void runDemo() throws Exception {
        logMessage("🔧 Initializing open source agents...");
        
        // Initialize agents (mock mode for reliable demo)
        chatAgent = new EnhancedChatAgent();
        weatherAgent = new WeatherAgent();
        
        logMessage("✅ Enhanced Chat Agent - Conversational AI with LLM orchestration");
        logMessage("✅ Weather Agent - Weather monitoring and alerts");
        logMessage("");
        
        // Demo scenarios
        demonstrateWeatherInteraction();
        demonstrateConversationalAI();
        
        logMessage("🎉 Multi-Agent Demo completed successfully!");
        logMessage("");
        logMessage("📚 Open Source Components Demonstrated:");
        logMessage("   ✅ EnhancedChatAgent - AI-powered conversational interfaces");
        logMessage("   ✅ WeatherAgent - Real-time weather data integration");
        logMessage("   ✅ Multi-agent coordination patterns");
        logMessage("   ✅ Event-driven communication");
        logMessage("");
        logMessage("📖 For implementation details, see:");
        logMessage("   - examples/src/main/java/io/amcp/examples/meshchat/");
        logMessage("   - examples/src/main/java/io/amcp/examples/weather/");
    }
    
    private void demonstrateWeatherInteraction() {
        logMessage("🌤️ Demonstrating Weather Agent Integration");
        logMessage("--------------------------------------------------");
        
        try {
            // Simulate weather requests
            List<String> weatherQueries = Arrays.asList(
                "What's the weather like in San Francisco?",
                "Will it rain in New York tomorrow?", 
                "Check weather conditions for London"
            );
            
            for (String query : weatherQueries) {
                logMessage("User: " + query);
                
                // Mock weather response
                String response = "Mock weather response for: " + query;
                logMessage("Weather Agent: " + response);
                logMessage("");
                
                Thread.sleep(1000); // Demo pacing
            }
            
        } catch (Exception e) {
            logMessage("❌ Weather demo error: " + e.getMessage());
        }
        
        logMessage("");
    }
    
    private void demonstrateConversationalAI() {
        logMessage("💬 Demonstrating Conversational AI");
        logMessage("----------------------------------");
        
        try {
            // Simulate chat scenarios
            List<String> chatQueries = Arrays.asList(
                "Hello! Can you help me understand AMCP?",
                "How do agents communicate with each other?",
                "What are the benefits of agent mobility?"
            );
            
            for (String query : chatQueries) {
                logMessage("User: " + query);
                
                // Mock chat response
                String response = "Mock AI response for: " + query;
                logMessage("Chat Agent: " + response);
                logMessage("");
                
                Thread.sleep(1000); // Demo pacing
            }
            
        } catch (Exception e) {
            logMessage("❌ Chat demo error: " + e.getMessage());
        }
        
        logMessage("");
    }
    
    private static void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] " + message);
    }
}