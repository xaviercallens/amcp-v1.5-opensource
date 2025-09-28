package io.amcp.examples.multiagent;

import io.amcp.connectors.ai.EnhancedChatAgent;
import io.amcp.connectors.ai.AgentRegistry;
import io.amcp.core.Agent;
import io.amcp.core.AgentContext;
import io.amcp.examples.weather.WeatherAgent;
import io.amcp.examples.travel.TravelPlannerAgent;
import io.amcp.connectors.ai.StockPriceAgent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/**
 * Interactive Chat Demo - Real-time multi-agent communication
 * Demonstrates live AMCP v1.5 A2A patterns with OLLAMA integration
 */
public class InteractiveChatDemo {
    
    private EnhancedChatAgent chatAgent;
    private WeatherAgent weatherAgent;
    private TravelPlannerAgent travelAgent;
    private StockPriceAgent stockAgent;
    private AgentContext context;
    private Scanner scanner;
    
    public static void main(String[] args) {
        new InteractiveChatDemo().start();
    }
    
    public void start() {
        logMessage("Starting Interactive AMCP v1.5 Chat Demo...");
        scanner = new Scanner(System.in);
        
        try {
            initializeAgents();
            displayWelcome();
            runInteractiveLoop();
        } catch (Exception e) {
            logMessage("Error during demo: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }
    
    private void initializeAgents() {
        logMessage("Initializing AMCP agents...");
        
        // Create agent context
        context = new AgentContext("interactive-demo-context");
        
        // Create agent registry
        AgentRegistry registry = new AgentRegistry(context);
        
        // Initialize specialized agents
        weatherAgent = new WeatherAgent();
        weatherAgent.setContext(context);
        
        travelAgent = new TravelPlannerAgent();
        travelAgent.setContext(context);
        
        stockAgent = new StockPriceAgent();
        stockAgent.setContext(context);
        
        // Create enhanced chat agent with OLLAMA integration
        chatAgent = new EnhancedChatAgent(registry);
        chatAgent.setContext(context);
        
        // Register all agents
        registry.registerAgent(weatherAgent);
        registry.registerAgent(travelAgent);
        registry.registerAgent(stockAgent);
        registry.registerAgent(chatAgent);
        
        // Activate agents
        weatherAgent.onActivate();
        travelAgent.onActivate();
        stockAgent.onActivate();
        chatAgent.onActivate();
        
        logMessage("✅ All agents initialized and activated");
    }
    
    private void displayWelcome() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🤖 AMCP v1.5 INTERACTIVE MULTI-AGENT CHAT DEMO");
        System.out.println("=".repeat(80));
        System.out.println("Welcome to the live multi-agent communication demo!");
        System.out.println("Your messages will be intelligently routed to specialized agents:");
        System.out.println();
        System.out.println("🌤️  Weather Agent: \"What's the weather in Nice?\"");
        System.out.println("📈 Stock Agent: \"What's the stock price of Amadeus?\"");
        System.out.println("✈️  Travel Agent: \"Plan a trip to Rome for 3 days\"");
        System.out.println("🤖 Chat Agent: General questions and multi-domain queries");
        System.out.println();
        System.out.println("💡 Features:");
        System.out.println("   • OLLAMA TinyLlama 1.1B integration for natural language");
        System.out.println("   • A2A communication patterns via AMCP protocol");
        System.out.println("   • Intelligent request routing and agent orchestration");
        System.out.println("   • Real-time inter-agent communication");
        System.out.println();
        System.out.println("Type 'quit' or 'exit' to end the demo");
        System.out.println("=".repeat(80));
        System.out.println();
    }
    
    private void runInteractiveLoop() {
        while (true) {
            System.out.print("👤 You: ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) {
                continue;
            }
            
            if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                System.out.println("🛑 Ending interactive demo...");
                break;
            }
            
            if (input.equalsIgnoreCase("help")) {
                displayHelp();
                continue;
            }
            
            if (input.equalsIgnoreCase("status")) {
                displayStatus();
                continue;
            }
            
            // Process user input through the chat agent
            processUserInput(input);
            System.out.println();
        }
    }
    
    private void processUserInput(String input) {
        try {
            System.out.println("🤖 Processing: \"" + input + "\"");
            
            // Simulate chat agent processing with intelligent routing
            String response = chatAgent.processQuery(input);
            
            System.out.println("🤖 Response: " + response);
            
        } catch (Exception e) {
            System.out.println("❌ Error processing request: " + e.getMessage());
            logMessage("Error details: " + e.toString());
        }
    }
    
    private void displayHelp() {
        System.out.println("📖 Available commands:");
        System.out.println("   help   - Show this help message");
        System.out.println("   status - Show agent status");
        System.out.println("   quit   - Exit the demo");
        System.out.println();
        System.out.println("📝 Example queries:");
        System.out.println("   \"What's the weather in Paris?\"");
        System.out.println("   \"Show me the stock price of AAPL\"");
        System.out.println("   \"Plan a weekend trip to Barcelona\"");
        System.out.println("   \"What's the weather in Tokyo and MSFT stock price?\"");
    }
    
    private void displayStatus() {
        System.out.println("📊 Agent Status:");
        System.out.println("   🌤️  Weather Agent: Active");
        System.out.println("   📈 Stock Agent: Active"); 
        System.out.println("   ✈️  Travel Agent: Active");
        System.out.println("   🤖 Chat Agent: Active with OLLAMA integration");
        System.out.println("   🔗 AMCP Context: " + context.getContextId());
    }
    
    private void cleanup() {
        logMessage("Cleaning up agents...");
        try {
            if (chatAgent != null) chatAgent.onDeactivate();
            if (weatherAgent != null) weatherAgent.onDeactivate();
            if (travelAgent != null) travelAgent.onDeactivate();  
            if (stockAgent != null) stockAgent.onDeactivate();
            
            if (scanner != null) scanner.close();
            
        } catch (Exception e) {
            logMessage("Error during cleanup: " + e.getMessage());
        }
        logMessage("✅ Demo cleanup completed");
    }
    
    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [InteractiveChatDemo] " + message);
    }
}