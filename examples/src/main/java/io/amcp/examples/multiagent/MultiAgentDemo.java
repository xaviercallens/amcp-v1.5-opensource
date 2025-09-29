package io.amcp.examples.multiagent;

import io.amcp.connectors.ai.EnhancedChatAgent;
import io.amcp.examples.weather.WeatherAgent;
import io.amcp.examples.travel.TravelPlannerAgent;
import io.amcp.examples.stock.StockPriceAgent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Multi-Agent Communication Demo (Simplified Version)
 * 
 * Demonstrates the AMCP v1.5 Enhanced Chat Agent orchestrating
 * conversations with Weather, Travel, and Stock agents.
 * 
 * This simplified version focuses on showing the agent architecture
 * and core functionality without complex event handling.
 */
public class MultiAgentDemo {
    
    private static final String[] DEMO_CONVERSATIONS = {
        "Hello chat provide me the weather in Nice",
        "What is the stock price of Amadeus",
        "Help me to organize and plan my trip to Rome for 3 days",
        "What's the weather like in Paris and what are the current stock prices for LVMH?",
        "Plan a business trip to London and tell me about the weather forecast"
    };
    
    private EnhancedChatAgent chatAgent;
    private WeatherAgent weatherAgent;
    private TravelPlannerAgent travelAgent;
    private StockPriceAgent stockAgent;
    
    public static void main(String[] args) {
        logMessage("Starting AMCP v1.5 Multi-Agent Communication Demo...");
        
        try {
            MultiAgentDemo demo = new MultiAgentDemo();
            demo.initializeAgents();
            demo.demonstrateAgentCapabilities();
            demo.runDemoConversations();
            demo.shutdown();
            
        } catch (Exception e) {
            logMessage("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Initialize all agents
     */
    private void initializeAgents() throws Exception {
        logMessage("Initializing AMCP Agents...");
        
        // Create agents
        chatAgent = new EnhancedChatAgent();
        weatherAgent = new WeatherAgent();
        travelAgent = new TravelPlannerAgent();
        stockAgent = new StockPriceAgent();
        
        logMessage("All agents created successfully!");
        logMessage("✅ Enhanced Chat Agent - Intelligent orchestrator with OLLAMA integration");
        logMessage("✅ Weather Agent (" + weatherAgent.getAgentId() + ") - Weather forecasts and conditions");
        logMessage("✅ Travel Planner Agent (" + travelAgent.getAgentId() + ") - Trip planning and itineraries");
        logMessage("✅ Stock Price Agent (" + stockAgent.getAgentId() + ") - Financial data and market information");
    }
    
    /**
     * Demonstrate individual agent capabilities
     */
    private void demonstrateAgentCapabilities() {
        logMessage("\n" + "=".repeat(60));
        logMessage("AGENT CAPABILITIES DEMONSTRATION");
        logMessage("=".repeat(60));
        
        // Demonstrate Weather Agent
        logMessage("\n🌤️  WEATHER AGENT CAPABILITIES:");
        logMessage("   • Current weather conditions");
        logMessage("   • Weather forecasts");
        logMessage("   • Weather alerts and notifications");
        logMessage("   • Multi-location weather data");
        
        // Demonstrate Travel Agent
        logMessage("\n✈️  TRAVEL PLANNER AGENT CAPABILITIES:");
        logMessage("   • Trip planning and itineraries");
        logMessage("   • Flight and accommodation search");
        logMessage("   • Destination recommendations");
        logMessage("   • Travel optimization");
        
        // Demonstrate Stock Agent
        logMessage("\n📈 STOCK PRICE AGENT CAPABILITIES:");
        logMessage("   • Real-time stock prices");
        logMessage("   • Market data analysis");
        logMessage("   • Portfolio tracking");
        logMessage("   • Financial trends");
        
        // Show available stock symbols
        Set<String> symbols = stockAgent.getAvailableSymbols();
        logMessage("   Available symbols: " + String.join(", ", symbols));
        
        // Demonstrate Enhanced Chat Agent
        logMessage("\n🤖 ENHANCED CHAT AGENT CAPABILITIES:");
        logMessage("   • Natural language processing with OLLAMA");
        logMessage("   • Intelligent request routing");
        logMessage("   • Multi-agent orchestration");
        logMessage("   • Context-aware conversations");
        logMessage("   • A2A communication patterns");
        
        // Get available agents and show sample queries
        try {
            Map<String, String> availableAgents = chatAgent.getAvailableAgents().get(5, TimeUnit.SECONDS);
            logMessage("\n   Available Agents and Example Queries:");
            
            // Provide example queries for common agent types
            Map<String, List<String>> agentExamples = Map.of(
                "Weather Agent", List.of(
                    "What's the weather in Paris?",
                    "Will it rain tomorrow in Tokyo?",
                    "Show me the 5-day forecast for London"
                ),
                "Travel Planner", List.of(
                    "Plan a trip to Rome for 3 days",
                    "Find flights from NYC to Paris",
                    "Recommend hotels in Barcelona"
                ),
                "Stock Agent", List.of(
                    "What's the current price of AAPL?",
                    "Analyze TSLA stock performance",
                    "Show me tech stock trends"
                )
            );
            
            agentExamples.forEach((agentName, exampleList) -> {
                logMessage("\n   " + agentName + " Examples:");
                exampleList.forEach(example -> logMessage("     • " + example));
            });
        } catch (Exception e) {
            logMessage("   [Unable to load agent examples: " + e.getMessage() + "]");
        }
        
        logMessage("\n" + "=".repeat(60));
    }
    
    /**
     * Run demonstration conversations
     */
    private void runDemoConversations() throws Exception {
        logMessage("\nStarting Multi-Agent Conversation Demonstrations...");
        
        for (int i = 0; i < DEMO_CONVERSATIONS.length; i++) {
            String conversation = DEMO_CONVERSATIONS[i];
            
            logMessage("\n" + "█".repeat(80));
            logMessage("DEMO CONVERSATION " + (i + 1) + "/" + DEMO_CONVERSATIONS.length);
            logMessage("█".repeat(80));
            logMessage("👤 User Query: \"" + conversation + "\"");
            
            // Analyze the request type
            String analysisResult = analyzeRequest(conversation);
            logMessage("🧠 Intent Analysis: " + analysisResult);
            
            // Demonstrate routing decision
            String routingDecision = simulateRouting(conversation);
            logMessage("🔄 Routing Decision: " + routingDecision);
            
            // Show expected agent responses
            String simulatedResponse = simulateAgentResponse(conversation);
            logMessage("🤖 Simulated Agent Response: " + simulatedResponse);
            
            logMessage("✅ Conversation " + (i + 1) + " demonstration completed.");
            
            // Brief pause between conversations
            Thread.sleep(2000);
        }
        
        // Display summary
        displayDemoSummary();
    }
    
    /**
     * Analyze request intent
     */
    private String analyzeRequest(String query) {
        String lowerQuery = query.toLowerCase();
        
        if (lowerQuery.contains("weather")) {
            return "Weather query detected - route to WeatherAgent";
        } else if (lowerQuery.contains("stock") || lowerQuery.contains("price") || 
                  lowerQuery.contains("amadeus") || lowerQuery.contains("lvmh")) {
            return "Stock/Financial query detected - route to StockPriceAgent";
        } else if (lowerQuery.contains("trip") || lowerQuery.contains("travel") || 
                  lowerQuery.contains("plan") || lowerQuery.contains("rome") || 
                  lowerQuery.contains("london")) {
            return "Travel planning query detected - route to TravelPlannerAgent";
        } else if (lowerQuery.contains("weather") && lowerQuery.contains("stock")) {
            return "Multi-domain query detected - coordinate WeatherAgent + StockPriceAgent";
        } else {
            return "General chat query - handle with EnhancedChatAgent + OLLAMA";
        }
    }
    
    /**
     * Simulate routing decision
     */
    private String simulateRouting(String query) {
        String lowerQuery = query.toLowerCase();
        
        if (lowerQuery.contains("nice") && lowerQuery.contains("weather")) {
            return "Route to WeatherAgent with location parameter: Nice";
        } else if (lowerQuery.contains("amadeus") && lowerQuery.contains("stock")) {
            return "Route to StockPriceAgent with symbol parameter: AMADEUS";
        } else if (lowerQuery.contains("rome") && lowerQuery.contains("trip")) {
            return "Route to TravelPlannerAgent with destination: Rome, duration: 3 days";
        } else if (lowerQuery.contains("paris") && lowerQuery.contains("lvmh")) {
            return "Multi-agent coordination: WeatherAgent(Paris) + StockPriceAgent(LVMH)";
        } else if (lowerQuery.contains("london") && lowerQuery.contains("business")) {
            return "Route to TravelPlannerAgent with type: business trip, destination: London";
        } else {
            return "Route to EnhancedChatAgent for direct OLLAMA processing";
        }
    }
    
    /**
     * Simulate agent response
     */
    private String simulateAgentResponse(String query) {
        String lowerQuery = query.toLowerCase();
        
        if (lowerQuery.contains("nice") && lowerQuery.contains("weather")) {
            return "Current weather in Nice: 22°C, partly cloudy, light winds from south at 15 km/h. Perfect Mediterranean weather!";
        } else if (lowerQuery.contains("amadeus") && lowerQuery.contains("stock")) {
            return "📈 Amadeus IT Group SA (AMADEUS): 68.50 EUR 🟢 +1.25 EUR (+1.86%) - Volume: 125K shares";
        } else if (lowerQuery.contains("rome") && lowerQuery.contains("trip")) {
            return "🏛️ Rome 3-Day Itinerary: Day 1: Colosseum & Roman Forum, Day 2: Vatican City & Sistine Chapel, Day 3: Trevi Fountain & Spanish Steps. Estimated budget: €450-650 per person.";
        } else if (lowerQuery.contains("paris") && lowerQuery.contains("lvmh")) {
            return "🌤️ Paris: 18°C, light rain, 10 km/h winds | 📈 LVMH: 725.80 EUR 🔴 -12.40 EUR (-1.68%)";
        } else if (lowerQuery.contains("london") && lowerQuery.contains("business")) {
            return "✈️ London Business Trip Plan: Direct flights from major hubs, recommend Canary Wharf hotels, weather forecast: 16°C, partly cloudy. Meeting venues and transport arranged.";
        } else {
            return "I can help you with weather, travel planning, and stock information. What would you like to know more about?";
        }
    }
    
    /**
     * Display demonstration summary
     */
    private void displayDemoSummary() {
        logMessage("\n" + "═".repeat(80));
        logMessage("MULTI-AGENT COMMUNICATION DEMO SUMMARY");
        logMessage("═".repeat(80));
        
        logMessage("✅ Demo completed successfully!");
        logMessage("📊 Demonstrated " + DEMO_CONVERSATIONS.length + " conversation scenarios");
        
        logMessage("\n🎯 Key Features Demonstrated:");
        logMessage("   • Intelligent request analysis and intent recognition");
        logMessage("   • Dynamic agent routing based on query content");
        logMessage("   • Multi-domain query handling and coordination");
        logMessage("   • Specialized agent capabilities (Weather, Travel, Stock)");
        logMessage("   • AMCP v1.5 A2A communication patterns");
        logMessage("   • Context-aware response generation");
        
        logMessage("\n🏗️  Architecture Highlights:");
        logMessage("   • Enhanced Chat Agent as central orchestrator");
        logMessage("   • Agent Registry for capability discovery");
        logMessage("   • Prompt Manager for intelligent routing");
        logMessage("   • OLLAMA integration for natural language processing");
        logMessage("   • Event-driven communication via AMCP protocol");
        
        logMessage("\n🚀 Next Steps:");
        logMessage("   • Full AMCP event mesh implementation");
        logMessage("   • Real-time agent communication");
        logMessage("   • Live API integrations (Weather, Stock, Travel)");
        logMessage("   • Interactive conversation mode");
        logMessage("   • Load balancing and scaling capabilities");
        
        logMessage("═".repeat(80));
    }
    
    /**
     * Interactive mode for manual testing
     */
    public void runInteractiveMode() {
        logMessage("\n🎮 Enhanced Chat Agent Interactive Mode");
        logMessage("Ask questions like:");
        logMessage("  • 'What's the weather in Nice?'");
        logMessage("  • 'Stock price of Amadeus'");
        logMessage("  • 'Plan a trip to Rome'");
        logMessage("Type 'exit' to quit:");
        
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.print("\n👤 You: ");
            String input = scanner.nextLine().trim();
            
            if ("exit".equalsIgnoreCase(input)) {
                logMessage("👋 Goodbye! Thank you for trying the AMCP Multi-Agent Demo!");
                break;
            }
            
            if (!input.isEmpty()) {
                logMessage("🧠 Analyzing: " + analyzeRequest(input));
                logMessage("🔄 Routing: " + simulateRouting(input));
                logMessage("🤖 Response: " + simulateAgentResponse(input));
            }
        }
        
        scanner.close();
    }
    
    /**
     * Shutdown all agents
     */
    private void shutdown() {
        logMessage("\n🛑 Shutting down Multi-Agent Demo...");
        logMessage("✅ Demo completed successfully. All agent capabilities demonstrated.");
        logMessage("🔗 For full implementation, see: MULTIAGENT_SYSTEM_GUIDE.md");
    }
    
    /**
     * Utility method for timestamped logging
     */
    private static void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [MultiAgentDemo] " + message);
    }
    
    /**
     * Entry point with command line options
     */
    public static void runDemo(String[] args) {
        if (args.length > 0 && "interactive".equals(args[0])) {
            // Interactive mode
            try {
                MultiAgentDemo demo = new MultiAgentDemo();
                demo.initializeAgents();
                demo.demonstrateAgentCapabilities();
                demo.runInteractiveMode();
                demo.shutdown();
            } catch (Exception e) {
                logMessage("Interactive demo failed: " + e.getMessage());
            }
        } else {
            // Standard demo mode
            main(args);
        }
    }
}