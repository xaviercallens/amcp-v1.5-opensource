package io.amcp.examples.orchestrator;

import io.amcp.connectors.ai.OrchestratorAgent;
import io.amcp.connectors.ai.EnhancedChatAgent;
import io.amcp.examples.weather.WeatherAgent;
import io.amcp.examples.stockprice.StockPriceAgent;
import io.amcp.core.*;
import io.amcp.core.impl.SimpleAgentContext;
import io.amcp.messaging.EventBroker;
import io.amcp.messaging.impl.InMemoryEventBroker;
import io.amcp.mobility.MobilityManager;
import io.amcp.mobility.impl.SimpleMobilityManager;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AMCP v1.5 OrchestratorAgent Demo
 * 
 * Demonstrates the intelligent agent orchestration system with:
 * - TinyLlama-powered intent analysis via OLLAMA
 * - Dynamic agent discovery and routing
 * - Real-time multi-agent coordination
 * - End-to-end conversation orchestration
 */
public class OrchestratorDemo {
    
    // Core components
    private SimpleAgentContext context;
    private OrchestratorAgent orchestratorAgent;
    private EnhancedChatAgent chatAgent;
    private WeatherAgent weatherAgent;
    private StockPriceAgent stockAgent;
    
    // Demo management
    private final Scanner scanner = new Scanner(System.in);
    private boolean running = true;
    
    public static void main(String[] args) {
        OrchestratorDemo demo = new OrchestratorDemo();
        
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
     * Initialize all demo components
     */
    private void initialize() throws Exception {
        logInfo("🚀 Initializing AMCP v1.5 OrchestratorAgent Demo...");
        
        // Initialize event broker and context
        EventBroker eventBroker = new InMemoryEventBroker();
        MobilityManager mobilityManager = new SimpleMobilityManager();
        
        context = new SimpleAgentContext(eventBroker, mobilityManager);
        
        // Initialize OrchestratorAgent (the star of the show)
        orchestratorAgent = new OrchestratorAgent();
        orchestratorAgent.setContext(context);
        
        // Initialize ChatAgent with OrchestratorAgent integration
        chatAgent = new EnhancedChatAgent();
        chatAgent.setContext(context);
        
        // Initialize specialized agents for routing
        weatherAgent = new WeatherAgent();
        weatherAgent.setContext(context);
        
        stockAgent = new StockPriceAgent();
        stockAgent.setContext(context);
        
        // Register and activate agents
        context.registerAgent(orchestratorAgent);
        context.registerAgent(chatAgent);
        context.registerAgent(weatherAgent);
        context.registerAgent(stockAgent);
        
        // Activate all agents
        orchestratorAgent.onActivate();
        chatAgent.onActivate();
        weatherAgent.onActivate();
        stockAgent.onActivate();
        
        logInfo("✅ All agents activated successfully");
        Thread.sleep(1000); // Allow agent discovery to complete
    }
    
    /**
     * Display demo welcome and instructions
     */
    private void displayWelcome() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🎯 AMCP v1.5 OrchestratorAgent Demo");
        System.out.println("   Intelligent Multi-Agent Orchestration with TinyLlama");
        System.out.println("=".repeat(80));
        
        System.out.println("\n🤖 The OrchestratorAgent provides:");
        System.out.println("   • TinyLlama-powered intent analysis via OLLAMA");
        System.out.println("   • Dynamic agent discovery and intelligent routing");
        System.out.println("   • Real-time parameter extraction and delegation");
        System.out.println("   • Response synthesis and formatting");
        
        System.out.println("\n🔧 Available Specialized Agents:");
        System.out.println("   • WeatherAgent: Real-time weather data for any location");
        System.out.println("   • StockPriceAgent: Live financial market data");
        System.out.println("   • ChatAgent: General conversation and orchestration");
        
        System.out.println("\n💬 Try these example queries:");
        System.out.println("   \"What's the weather in Paris?\"");
        System.out.println("   \"Get me Apple stock price\"");
        System.out.println("   \"How's the weather in Tokyo today?\"");
        System.out.println("   \"Tesla stock price please\"");
        System.out.println("   \"Hello, how are you?\" (general chat)");
        
        System.out.println("\n📋 Available Commands:");
        System.out.println("   /stats    - Show orchestration statistics");
        System.out.println("   /agents   - List available agents");
        System.out.println("   /help     - Show this help");
        System.out.println("   /quit     - Exit demo");
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🎬 Demo ready! Start chatting with the intelligent orchestrator...\n");
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
                displayAvailableAgents();
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
            // Use OrchestratorAgent directly for demonstration
            orchestratorAgent.orchestrateRequest(message, conversationId)
                .thenAccept(response -> {
                    System.out.println("🤖 OrchestratorAgent: " + response);
                    System.out.println();
                })
                .exceptionally(ex -> {
                    System.err.println("❌ Orchestration failed: " + ex.getMessage());
                    System.out.println("🤖 Fallback: I apologize, but I'm having trouble processing your request right now.");
                    System.out.println();
                    return null;
                })
                .join(); // Wait for completion in demo
                
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }
    
    /**
     * Display orchestration statistics
     */
    private void displayOrchestrationStats() {
        System.out.println("\n📊 Orchestration Statistics:");
        
        try {
            Map<String, Object> stats = orchestratorAgent.getOrchestrationStats();
            stats.forEach((key, value) -> 
                System.out.println("   " + key + ": " + value)
            );
            
            // Additional context info
            System.out.println("   Context ID: " + context.getContextId());
            System.out.println("   Agent Context: Active");
            
        } catch (Exception e) {
            System.out.println("   Error retrieving stats: " + e.getMessage());
        }
        System.out.println();
    }
    
    /**
     * Display available agents
     */
    private void displayAvailableAgents() {
        System.out.println("\n🤖 Available Agents:");
        
        try {
            chatAgent.getAvailableAgents()
                .thenAccept(agents -> {
                    agents.forEach((id, description) -> 
                        System.out.println("   • " + id + ": " + description)
                    );
                    System.out.println();
                })
                .join();
                
        } catch (Exception e) {
            System.out.println("   Error retrieving agents: " + e.getMessage());
            System.out.println();
        }
    }
    
    /**
     * Clean shutdown
     */
    private void shutdown() {
        try {
            logInfo("🛑 Shutting down demo...");
            
            if (stockAgent != null) stockAgent.onDeactivate();
            if (weatherAgent != null) weatherAgent.onDeactivate();
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
        System.out.println("[" + timestamp + "] [OrchestratorDemo] " + message);
    }
    
    private void logError(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.err.println("[" + timestamp + "] [OrchestratorDemo] ERROR: " + message);
    }
}