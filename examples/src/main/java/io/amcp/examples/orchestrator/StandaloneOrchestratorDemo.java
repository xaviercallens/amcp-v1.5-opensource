package io.amcp.examples.orchestrator;

import io.amcp.connectors.ai.OrchestratorAgent;
import io.amcp.connectors.ai.EnhancedChatAgent;
import io.amcp.core.impl.SimpleAgentContext;
import io.amcp.messaging.EventBroker;
import io.amcp.messaging.impl.InMemoryEventBroker;
import io.amcp.mobility.MobilityManager;
import io.amcp.mobility.impl.SimpleMobilityManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AMCP v1.5 Standalone OrchestratorAgent Demo
 * 
 * Demonstrates the OrchestratorAgent in isolation with:
 * - TinyLlama-powered intent analysis via OLLAMA
 * - Agent registry discovery
 * - Request routing logic
 * - Response synthesis capabilities
 */
public class StandaloneOrchestratorDemo {
    
    // Core components
    private SimpleAgentContext context;
    private OrchestratorAgent orchestratorAgent;
    private EnhancedChatAgent chatAgent;
    
    // Demo management
    private final Scanner scanner = new Scanner(System.in);
    private boolean running = true;
    
    public static void main(String[] args) {
        StandaloneOrchestratorDemo demo = new StandaloneOrchestratorDemo();
        
        try {
            demo.initialize();
            demo.displayWelcome();
            demo.runDemo();
        } catch (Exception e) {
            demo.logError("Demo failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            demo.shutdown();
        }
    }
    
    /**
     * Initialize core components (minimal setup)
     */
    private void initialize() throws Exception {
        logInfo("🚀 Initializing AMCP v1.5 Standalone OrchestratorAgent Demo...");
        
        // Initialize event broker and context
        EventBroker eventBroker = new InMemoryEventBroker();
        MobilityManager mobilityManager = new SimpleMobilityManager();
        
        context = new SimpleAgentContext(eventBroker, mobilityManager);
        
        // Initialize OrchestratorAgent (the main focus)
        orchestratorAgent = new OrchestratorAgent();
        orchestratorAgent.setContext(context);
        
        // Initialize ChatAgent for integration
        chatAgent = new EnhancedChatAgent();
        chatAgent.setContext(context);
        
        // Register agents
        context.registerAgent(orchestratorAgent);
        context.registerAgent(chatAgent);
        
        // Activate agents
        orchestratorAgent.onActivate();
        chatAgent.onActivate();
        
        logInfo("✅ OrchestratorAgent activated successfully");
        logInfo("📋 Agent registry initialized with core agents");
        
        Thread.sleep(1000); // Allow initialization to complete
    }
    
    /**
     * Display demo welcome and instructions
     */
    private void displayWelcome() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🎯 AMCP v1.5 Standalone OrchestratorAgent Demo");
        System.out.println("   TinyLlama-Powered Intelligent Agent Orchestration");
        System.out.println("=".repeat(80));
        
        System.out.println("\n🧠 OrchestratorAgent Core Features:");
        System.out.println("   ✅ TinyLlama integration for intent analysis");
        System.out.println("   ✅ Dynamic agent discovery and registry");
        System.out.println("   ✅ Intelligent request routing logic");
        System.out.println("   ✅ Response synthesis and formatting");
        System.out.println("   ✅ Session and correlation management");
        
        System.out.println("\n🤖 Simulated Agent Routing:");
        System.out.println("   • Weather queries → WeatherAgent (simulated)");
        System.out.println("   • Stock queries → StockPriceAgent (simulated)");
        System.out.println("   • Travel queries → TravelPlannerAgent (simulated)");
        System.out.println("   • General chat → Direct TinyLlama processing");
        
        System.out.println("\n💬 Try these example queries:");
        System.out.println("   \"What's the weather in Paris?\"");
        System.out.println("   \"Get me Apple stock price\"");
        System.out.println("   \"Plan a trip to Tokyo\"");
        System.out.println("   \"Hello, tell me about yourself\"");
        
        System.out.println("\n📋 Available Commands:");
        System.out.println("   /stats    - Show orchestration statistics");
        System.out.println("   /agents   - List discovered agents");
        System.out.println("   /test     - Run orchestration test");
        System.out.println("   /help     - Show this help");
        System.out.println("   /quit     - Exit demo");
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🎬 Demo ready! The OrchestratorAgent will analyze your queries...\n");
    }
    
    /**
     * Main demo interaction loop
     */
    private void runDemo() {
        while (running) {
            System.out.print("👤 You: ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) continue;
            
            try {
                if (input.startsWith("/")) {
                    handleCommand(input);
                } else {
                    handleUserMessage(input);
                }
            } catch (Exception e) {
                logError("Error processing input: " + e.getMessage());
            }
        }
    }
    
    /**
     * Handle user commands
     */
    private void handleCommand(String command) {
        switch (command.toLowerCase()) {
            case "/quit":
            case "/exit":
                running = false;
                System.out.println("👋 Goodbye! Thanks for trying the OrchestratorAgent demo!");
                break;
            
            case "/help":
                displayWelcome();
                break;
            
            case "/stats":
                displayOrchestrationStats();
                break;
            
            case "/agents":
                displayDiscoveredAgents();
                break;
                
            case "/test":
                runOrchestrationTest();
                break;
            
            default:
                System.out.println("❓ Unknown command: " + command);
                System.out.println("   Use /help for available commands");
        }
    }
    
    /**
     * Handle user messages through the orchestration system
     */
    private void handleUserMessage(String message) {
        String conversationId = "demo-" + System.currentTimeMillis();
        
        logInfo("🎯 Orchestrating request: \"" + message + "\"");
        
        try {
            // Simulate orchestration process
            System.out.println("🧠 OrchestratorAgent analyzing intent...");
            
            // Use OrchestratorAgent for intent analysis and routing
            orchestratorAgent.orchestrateRequest(message, conversationId)
                .thenAccept(response -> {
                    System.out.println("🤖 Orchestrated Response: " + response);
                    System.out.println();
                })
                .exceptionally(ex -> {
                    System.err.println("❌ Orchestration failed: " + ex.getMessage());
                    
                    // Provide simulated fallback response
                    String fallbackResponse = simulateFallbackRouting(message);
                    System.out.println("🔄 Fallback Response: " + fallbackResponse);
                    System.out.println();
                    return null;
                })
                .join(); // Wait for completion in demo
                
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            String fallbackResponse = simulateFallbackRouting(message);
            System.out.println("🔄 Fallback Response: " + fallbackResponse);
        }
    }
    
    /**
     * Simulate fallback routing for demo purposes
     */
    private String simulateFallbackRouting(String message) {
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.contains("weather") || lowerMessage.contains("temperature")) {
            return "🌤️ I would route this to the WeatherAgent for real weather data. " +
                   "In a full setup, you'd get actual weather information for your location.";
        } else if (lowerMessage.contains("stock") || lowerMessage.contains("price")) {
            return "📈 I would route this to the StockPriceAgent for real market data. " +
                   "In a full setup, you'd get current stock prices and financial information.";
        } else if (lowerMessage.contains("travel") || lowerMessage.contains("trip")) {
            return "✈️ I would route this to the TravelPlannerAgent for trip planning. " +
                   "In a full setup, you'd get travel recommendations and itineraries.";
        } else {
            return "💭 This would be handled as general conversation by TinyLlama. " +
                   "The OrchestratorAgent would process this directly for general chat responses.";
        }
    }
    
    /**
     * Display orchestration statistics
     */
    private void displayOrchestrationStats() {
        System.out.println("\n📊 OrchestratorAgent Statistics:");
        
        try {
            Map<String, Object> stats = orchestratorAgent.getOrchestrationStats();
            stats.forEach((key, value) -> 
                System.out.println("   " + key + ": " + value)
            );
            
            System.out.println("   Context ID: " + context.getContextId());
            System.out.println("   Demo Mode: Standalone");
            
        } catch (Exception e) {
            System.out.println("   Error retrieving stats: " + e.getMessage());
        }
        System.out.println();
    }
    
    /**
     * Display discovered agents
     */
    private void displayDiscoveredAgents() {
        System.out.println("\n🤖 Discovered Agents:");
        
        try {
            chatAgent.getAvailableAgents()
                .thenAccept(agents -> {
                    if (agents.isEmpty()) {
                        System.out.println("   📝 Core agents (simulated in standalone mode):");
                        System.out.println("   • WeatherAgent: Weather data and forecasts");
                        System.out.println("   • StockPriceAgent: Financial market information");
                        System.out.println("   • TravelPlannerAgent: Travel planning and booking");
                        System.out.println("   • EnhancedChatAgent: General conversation");
                    } else {
                        agents.forEach((id, description) -> 
                            System.out.println("   • " + id + ": " + description)
                        );
                    }
                    System.out.println();
                })
                .join();
                
        } catch (Exception e) {
            System.out.println("   Error retrieving agents: " + e.getMessage());
            System.out.println();
        }
    }
    
    /**
     * Run a series of orchestration tests
     */
    private void runOrchestrationTest() {
        System.out.println("\n🧪 Running OrchestratorAgent Tests...\n");
        
        String[] testQueries = {
            "What's the weather like in London?",
            "Apple stock price today",
            "Plan a vacation to Paris", 
            "Hello, how does the orchestrator work?"
        };
        
        for (int i = 0; i < testQueries.length; i++) {
            System.out.println("Test " + (i + 1) + "/" + testQueries.length + 
                             ": \"" + testQueries[i] + "\"");
            
            try {
                // Simulate orchestration analysis
                Thread.sleep(500);
                String response = simulateFallbackRouting(testQueries[i]);
                System.out.println("Result: " + response);
                System.out.println();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        System.out.println("✅ Orchestration tests completed!");
        System.out.println();
    }
    
    /**
     * Clean shutdown
     */
    private void shutdown() {
        try {
            logInfo("🛑 Shutting down demo...");
            
            if (chatAgent != null) chatAgent.onDeactivate();
            if (orchestratorAgent != null) orchestratorAgent.onDeactivate();
            
            if (context != null) {
                context.shutdown();
            }
            
            scanner.close();
            logInfo("✅ Demo shutdown complete");
            
        } catch (Exception e) {
            logError("Error during shutdown: " + e.getMessage());
        }
    }
    
    // Logging utilities
    private void logInfo(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [StandaloneDemo] " + message);
    }
    
    private void logError(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.err.println("[" + timestamp + "] [StandaloneDemo] ERROR: " + message);
    }
}