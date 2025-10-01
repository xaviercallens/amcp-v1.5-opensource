package io.amcp.examples.meshchat;

import io.amcp.core.*;
import io.amcp.core.impl.SimpleAgentContext;
import io.amcp.messaging.EventBroker;
import io.amcp.messaging.impl.InMemoryEventBroker;
import io.amcp.mobility.MobilityManager;
import io.amcp.mobility.impl.SimpleMobilityManager;
import io.amcp.connectors.ai.OrchestratorAgent;
import io.amcp.examples.weather.WeatherAgent;
import io.amcp.examples.meshchat.TravelPlannerAgent;
import io.amcp.examples.meshchat.StockAgent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Interactive CLI for the MeshChat Agent System
 * 
 * This simplified CLI demonstrates the MeshChat agent's capabilities
 * in a user-friendly command-line interface for testing and demonstration.
 */
public class MeshChatCLI {
    
    private static final String WELCOME_BANNER = """
            
            ╔══════════════════════════════════════════════════════════════╗
            ║                     AMCP MeshChat v1.5                      ║
            ║              Enterprise Agent Communication System           ║
            ╚══════════════════════════════════════════════════════════════╝
            
            🤖 Welcome to the Multi-Agent Chat Interface!
            
            Available Specialists:
            • 🌍 Travel Planning - Trip planning, destinations, bookings
            • 📈 Financial Analysis - Stock quotes, market data, investments  
            • 🌤️ Weather Information - Forecasts and current conditions
            • 💬 General Chat - AI-powered conversations via TinyLlama
            
            Commands:
            /help     - Show this help message
            /session  - Show current session info
            /history  - Show conversation history
            /clear    - Clear conversation history
            /quit     - Exit the application
            
            Just type your message to start chatting!
            """;
    
    private final String sessionId;
    private final String userId;
    private final List<ConversationEntry> conversationHistory;
    private final BufferedReader reader;
    private volatile boolean running;
    
    // AMCP Real Orchestration Components
    private AgentContext agentContext;
    private OrchestratorAgent orchestrator;
    private TravelPlannerAgent travelAgent;
    private WeatherAgent weatherAgent;
    private StockAgent stockAgent;
    private boolean realOrchestrationEnabled;
    
    public MeshChatCLI() {
        this.sessionId = "cli-session-" + System.currentTimeMillis();
        this.userId = "cli-user-" + System.getProperty("user.name", "anonymous");
        this.conversationHistory = new ArrayList<>();
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.running = true;
        this.realOrchestrationEnabled = false; // Will be set during initialization
        
        logMessage("CLI initialized with session: " + sessionId);
    }
    
    public void start() throws Exception {
        try {
            System.out.println(WELCOME_BANNER);
            
            // Try to initialize real AMCP orchestration
            initializeOrchestration();
            
            // Main interaction loop
            runInteractionLoop();
            
        } finally {
            shutdown();
        }
    }
    
    /**
     * Initialize real AMCP orchestration with OrchestratorAgent and TinyLlama
     */
    private void initializeOrchestration() {
        try {
            logMessage("🚀 Initializing AMCP Real Orchestration...");
            
            // Create in-memory event broker for the CLI
            EventBroker broker = new InMemoryEventBroker();
            broker.start().get(); // Start the broker
            
            // Create mobility manager  
            MobilityManager mobilityManager = new SimpleMobilityManager();
            
            // Create agent context
            agentContext = new SimpleAgentContext(broker, mobilityManager);
            
            // Initialize and activate the orchestrator
            orchestrator = new OrchestratorAgent();
            orchestrator.setContext(agentContext);
            agentContext.registerAgent(orchestrator).get(); // Register first
            orchestrator.onActivate();
            
            // Initialize and activate the TravelPlannerAgent
            travelAgent = new TravelPlannerAgent();
            travelAgent.setContext(agentContext);
            agentContext.registerAgent(travelAgent).get(); // Register first
            travelAgent.onActivate();
            
            // Initialize and activate the WeatherAgent
            weatherAgent = new WeatherAgent();
            weatherAgent.setContext(agentContext);
            agentContext.registerAgent(weatherAgent).get(); // Register first
            weatherAgent.onActivate();
            
            // Initialize and activate the StockAgent
            stockAgent = new StockAgent();
            stockAgent.setContext(agentContext);
            agentContext.registerAgent(stockAgent).get(); // Register first
            stockAgent.onActivate();
            
            // Give agents a moment to initialize
            Thread.sleep(2000);
            
            realOrchestrationEnabled = true;
            logMessage("✅ Real orchestration enabled with TinyLlama AI and active agents");
            System.out.println("🎯 AI Orchestration: ENABLED (TinyLlama + Live TravelPlannerAgent + WeatherAgent + StockAgent)");
            
        } catch (Exception e) {
            logMessage("⚠️ Real orchestration initialization failed: " + e.getMessage());
            logMessage("📋 Falling back to simulation mode for demo purposes");
            realOrchestrationEnabled = false;
            System.out.println("🔄 AI Orchestration: SIMULATION MODE (Real agents unavailable)");
        }
    }
    
    private void runInteractionLoop() throws Exception {
        while (running) {
            System.out.print("\\n💬 You: ");
            String input = reader.readLine();
            
            if (input == null || input.trim().isEmpty()) {
                continue;
            }
            
            String trimmedInput = input.trim();
            
            // Handle commands
            if (trimmedInput.startsWith("/")) {
                handleCommand(trimmedInput);
                continue;
            }
            
            // Process user message
            processUserMessage(trimmedInput);
        }
    }
    
    private void handleCommand(String command) {
        switch (command.toLowerCase()) {
            case "/help":
                showHelp();
                break;
            case "/session":
                showSessionInfo();
                break;
            case "/history":
                showConversationHistory();
                break;
            case "/clear":
                clearHistory();
                break;
            case "/quit":
            case "/exit":
                running = false;
                System.out.println("\\n👋 Goodbye! Thanks for using MeshChat!");
                break;
            default:
                System.out.println("❓ Unknown command: " + command);
                System.out.println("   Type /help for available commands");
        }
    }
    
    private void processUserMessage(String message) {
        try {
            // Add to conversation history
            addToHistory("User", message);
            
            // Show processing indicator
            System.out.println("🔄 Processing your request...");
            
            String response;
            if (realOrchestrationEnabled && orchestrator != null) {
                // Use real orchestration with TinyLlama AI
                response = callRealOrchestration(message);
            } else {
                // Fallback to simulation mode
                response = simulateAgentResponse(message);
            }
            
            // Show response
            System.out.println("\\n" + response);
            
        } catch (Exception e) {
            System.out.println("❌ Error processing message: " + e.getMessage());
            logMessage("Error processing user message: " + e.getMessage());
        }
    }
    
    /**
     * Call real AMCP orchestration with TinyLlama AI for intelligent agent routing
     */
    private String callRealOrchestration(String userQuery) {
        try {
            logMessage("🎯 Routing query to OrchestratorAgent with TinyLlama AI: " + userQuery);
            
            // Call the orchestrator's main orchestration method
            CompletableFuture<String> orchestrationFuture = orchestrator.orchestrateRequest(userQuery, sessionId);
            
            // Wait for response with timeout
            String orchestratedResponse = orchestrationFuture.get(30, TimeUnit.SECONDS);
            
            // Log the agent type that handled the request
            logMessage("✅ Orchestration completed successfully");
            
            return orchestratedResponse;
            
        } catch (Exception e) {
            logMessage("❌ Real orchestration failed: " + e.getMessage());
            // Fallback to simulation if real orchestration fails
            return "⚠️ Orchestration Error: " + e.getMessage() + 
                   "\\n\\n🔄 Falling back to simulation mode:\\n" + simulateAgentResponse(userQuery);
        }
    }
    
    private String simulateAgentResponse(String message) {
        String lowerMessage = message.toLowerCase();
        
        // Route to appropriate agent based on keywords
        if (containsKeywords(lowerMessage, "travel", "trip", "vacation", "destination", "flight", "hotel", "visit", "book")) {
            addToHistory("TravelPlannerAgent", "I'd be happy to help with your travel plans!");
            return "🌍 Travel Planner: I'd be happy to help with your travel plans! " +
                   "Let me suggest some destinations and create an itinerary for you. " +
                   "For a real implementation, I would coordinate with booking systems and " +
                   "provide detailed travel recommendations.";
                   
        } else if (containsKeywords(lowerMessage, "stock", "price", "market", "invest", "finance", "money", "crypto", "economy")) {
            addToHistory("StockAgent", "Let me analyze the financial markets for you.");
            return "📈 Stock Agent: Let me analyze the financial markets for you. " +
                   "I can provide stock quotes, market trends, and investment insights. " +
                   "In the full system, I would access real-time market data and " +
                   "provide comprehensive financial analysis.";
                   
        } else if (containsKeywords(lowerMessage, "weather", "forecast", "temperature", "rain", "sunny", "climate")) {
            addToHistory("WeatherAgent", "Checking weather conditions for you.");
            return "�️ Weather Agent: Checking weather conditions for you. " +
                   "I can provide forecasts, current conditions, and climate data. " +
                   "The real agent would integrate with weather APIs for accurate " +
                   "and up-to-date meteorological information.";
                   
        } else {
            addToHistory("OllamaChatAgent", "I understand your question. Let me help you.");
            return "🤖 Ollama Chat Agent: I understand your question. Let me help you. " +
                   "As an AI assistant powered by TinyLlama, I can engage in natural " +
                   "conversation, answer questions, and provide explanations on various topics.";
        }
    }
    
    private boolean containsKeywords(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private void showSessionInfo() {
        System.out.println("\\n📊 Session Information:");
        System.out.println("┌─────────────────────────────────────────────────────────┐");
        System.out.printf("│ Session ID: %-43s │%n", sessionId);
        System.out.printf("│ User ID: %-46s │%n", userId);
        System.out.printf("│ Messages: %-45d │%n", conversationHistory.size());
        System.out.printf("│ Started: %-46s │%n", 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("└─────────────────────────────────────────────────────────┘");
    }
    
    private void showConversationHistory() {
        System.out.println("\\n📜 Conversation History:");
        System.out.println("════════════════════════════════════════════════════════");
        
        if (conversationHistory.isEmpty()) {
            System.out.println("   No messages in history yet. Start chatting!");
            return;
        }
        
        for (ConversationEntry entry : conversationHistory) {
            String icon = entry.sender.equals("User") ? "👤" : getAgentIcon(entry.sender);
            System.out.printf("%s [%s] %s: %s%n", 
                icon, entry.timestamp, entry.sender, entry.message);
        }
        System.out.println("════════════════════════════════════════════════════════");
    }
    
    private String getAgentIcon(String agentType) {
        switch (agentType.toLowerCase()) {
            case "travelplanneragent":
                return "🌍";
            case "stockagent":
                return "📈";
            case "weatheragent":
                return "🌤️";
            case "ollamachatagent":
                return "🤖";
            case "meshchatagent":
                return "💬";
            default:
                return "🔧";
        }
    }
    
    private void clearHistory() {
        conversationHistory.clear();
        System.out.println("🗑️ Conversation history cleared.");
    }
    
    private void addToHistory(String sender, String message) {
        ConversationEntry entry = new ConversationEntry(
            sender, 
            message, 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        );
        conversationHistory.add(entry);
        
        // Keep history manageable (last 100 messages)
        if (conversationHistory.size() > 100) {
            conversationHistory.remove(0);
        }
    }
    
    private void shutdown() {
        try {
            logMessage("Shutting down MeshChat CLI...");
            
            // Shutdown agents if they were activated
            if (travelAgent != null) {
                travelAgent.onDeactivate();
            }
            if (weatherAgent != null) {
                weatherAgent.onDeactivate();
            }
            if (stockAgent != null) {
                stockAgent.onDeactivate();
            }
            if (orchestrator != null) {
                orchestrator.onDeactivate();
            }
            
            logMessage("Shutdown complete");
            
        } catch (Exception e) {
            System.err.println("Error during shutdown: " + e.getMessage());
        }
    }
    
    /**
     * Run automated demo scenario with real orchestration
     */
    public void runScenario(String scenarioType, String query) {
        try {
            System.out.println("🎭 Running " + scenarioType + " scenario...");
            System.out.println("Query: " + query);
            System.out.println();
            
            String agentResponse;
            if (realOrchestrationEnabled && orchestrator != null) {
                // Use real orchestration for scenarios
                agentResponse = callRealOrchestration(query);
                System.out.println("🎯 Orchestrated by TinyLlama AI");
            } else {
                // Fallback to simulation mode
                agentResponse = simulateAgentResponse(scenarioType, query);
                System.out.println("🔄 Simulation mode");
            }
            
            System.out.println("📤 User: " + query);
            System.out.println("🤖 MeshChat Agent: " + agentResponse);
            System.out.println();
            System.out.println("✅ Scenario completed successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Scenario failed: " + e.getMessage());
        }
    }
    
    /**
     * Simulate agent response for demo scenarios
     */
    private String simulateAgentResponse(String scenarioType, String query) {
        switch (scenarioType.toLowerCase()) {
            case "travel":
                return "I understand you're planning a trip to Japan! Let me coordinate with our Travel Planner Agent to help you with your 2-week spring itinerary.\n\n" +
                       "🌸 Travel Plan for Japan (Spring):\n" +
                       "• Best time: Late March to early May for cherry blossoms\n" +
                       "• Recommended cities: Tokyo (5 days), Kyoto (4 days), Osaka (3 days), Mount Fuji area (2 days)\n" +
                       "• Budget breakdown: Flights ($800), Accommodation ($1000), Food ($600), Activities ($400), Transport ($200)\n" +
                       "• Flight booking: Book 2-3 months in advance for better prices\n" +
                       "• Accommodation: Mix of ryokans and modern hotels recommended\n\n" +
                       "Would you like me to get more specific recommendations for any particular city?";
                       
            case "stock":
                return "I'll analyze those tech stocks for you using our Stock Analysis Agent.\n\n" +
                       "📊 Stock Analysis (AAPL, GOOGL, MSFT):\n\n" +
                       "🍎 AAPL (Apple Inc.):\n" +
                       "• Current: $175.50 (+1.2%)\n" +
                       "• Outlook: Strong fundamentals, iPhone sales resilient\n" +
                       "• Recommendation: BUY (Target: $190)\n\n" +
                       "🔍 GOOGL (Alphabet Inc.):\n" +
                       "• Current: $142.30 (+0.8%)\n" +
                       "• Outlook: AI leadership, cloud growth potential\n" +
                       "• Recommendation: BUY (Target: $160)\n\n" +
                       "🪟 MSFT (Microsoft Corp.):\n" +
                       "• Current: $378.90 (+0.5%)\n" +
                       "• Outlook: Azure dominance, AI integration\n" +
                       "• Recommendation: STRONG BUY (Target: $420)\n\n" +
                       "Overall portfolio allocation suggestion: 40% MSFT, 35% AAPL, 25% GOOGL";
                       
            case "multi":
                return "This is a complex request involving both travel and investment analysis! Let me coordinate with multiple agents to help you.\n\n" +
                       "🎯 Multi-Agent Coordination Results:\n\n" +
                       "✈️ Business Trip to Tokyo:\n" +
                       "• Flight recommendations: Direct flights from major US cities\n" +
                       "• Tech conference venue area: Shibuya/Shinjuku recommended\n" +
                       "• Business hotels: Imperial Hotel, Park Hyatt, Conrad Tokyo\n" +
                       "• Tech district proximity: Akihabara, Shibuya Sky\n\n" +
                       "📈 Japanese Tech Stocks Analysis:\n" +
                       "• Nintendo (7974.T): Gaming innovation leader\n" +
                       "• SoftBank (9984.T): Tech investment giant\n" +
                       "• Sony (6758.T): Hardware/entertainment hybrid\n" +
                       "• Recommendation: Consider ETF exposure (EWJ) for diversification\n\n" +
                       "🤝 Integrated Recommendation:\n" +
                       "Combine your business trip with investment research - visit Nikkei facilities, attend tech meetups, and network with local investors!";
                       
            default:
                return "I processed your request using our multi-agent system. The agents coordinated to provide you with a comprehensive response based on their specialized knowledge areas.";
        }
    }
    
    /**
     * Show help information
     */
    public void showHelp() {
        System.out.println("AMCP v1.5 MeshChat CLI - Help");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java MeshChatCLI [command] [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  interactive        Start interactive chat session (default)");
        System.out.println("  scenario <type> <query>  Run automated demo scenario");
        System.out.println("  test-tokyo         Run Tokyo travel scenario test with real orchestration");
        System.out.println("  help              Show this help message");
        System.out.println();
        System.out.println("Scenario Types:");
        System.out.println("  travel            Travel planning and recommendations");
        System.out.println("  stock             Stock analysis and investment advice");
        System.out.println("  multi             Multi-agent orchestration");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java MeshChatCLI interactive");
        System.out.println("  java MeshChatCLI scenario travel \"Plan a trip to Japan\"");
        System.out.println("  java MeshChatCLI scenario stock \"Analyze AAPL stock\"");
        System.out.println("  java MeshChatCLI test-tokyo  # Tests real orchestration flow");
    }
    
    /**
     * Test the Tokyo travel scenario specifically for user validation
     * This validates the complete flow: user input → LLM planning → task dispatch → agent responses → aggregation
     */
    public void testTokyoTravelScenario() {
        try {
            System.out.println("🧪 Testing Tokyo Travel Scenario for AMCP v1.5 Enterprise Edition");
            System.out.println("═══════════════════════════════════════════════════════════════");
            
            // Initialize orchestration if not already done
            if (!realOrchestrationEnabled) {
                initializeOrchestration();
            }
            
            String testQuery = "Plan My Trip to Tokyo Next Week";
            System.out.println("🎯 Test Query: " + testQuery);
            System.out.println();
            
            if (realOrchestrationEnabled && orchestrator != null) {
                System.out.println("🚀 Starting Real Orchestration Test...");
                System.out.println("📋 Expected Flow:");
                System.out.println("   1. TinyLlama AI analyzes intent and extracts parameters");
                System.out.println("   2. OrchestratorAgent routes to WeatherAgent (Tokyo weather)");
                System.out.println("   3. OrchestratorAgent routes to TravelPlannerAgent (flights/hotels)");
                System.out.println("   4. Responses aggregated and formatted by TinyLlama");
                System.out.println();
                
                long startTime = System.currentTimeMillis();
                String response = callRealOrchestration(testQuery);
                long endTime = System.currentTimeMillis();
                
                System.out.println("📤 User Request: " + testQuery);
                System.out.println("🤖 Orchestrated Response:");
                System.out.println(response);
                System.out.println();
                System.out.println("⏱️ Processing Time: " + (endTime - startTime) + "ms");
                System.out.println("✅ Tokyo Travel Scenario Test COMPLETED");
                
            } else {
                System.out.println("⚠️ Real orchestration not available - testing simulation mode");
                String response = simulateAgentResponse("travel", testQuery);
                System.out.println("📤 User Request: " + testQuery);
                System.out.println("🔄 Simulated Response:");
                System.out.println(response);
                System.out.println("✅ Tokyo Travel Scenario Test COMPLETED (Simulation Mode)");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Tokyo Travel Scenario Test FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.err.println("[" + timestamp + "] [CLI] " + message);
    }
    
    /**
     * Represents a conversation entry in the chat history
     */
    private static class ConversationEntry {
        final String sender;
        final String message;
        final String timestamp;
        
        ConversationEntry(String sender, String message, String timestamp) {
            this.sender = sender;
            this.message = message;
            this.timestamp = timestamp;
        }
    }
    
    public static void main(String[] args) {
        MeshChatCLI cli = new MeshChatCLI();
        
        // Add shutdown hook for cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("Shutting down MeshChat CLI...");
            cli.shutdown();
        }));
        
        try {
            if (args.length > 0) {
                String command = args[0];
                
                switch (command.toLowerCase()) {
                    case "interactive":
                        System.out.println("🚀 Starting AMCP MeshChat CLI in interactive mode...");
                        cli.start();
                        break;
                    case "scenario":
                        if (args.length >= 3) {
                            String scenarioType = args[1];
                            String query = args[2];
                            cli.runScenario(scenarioType, query);
                        } else {
                            System.err.println("Usage: java MeshChatCLI scenario <type> <query>");
                            cli.showHelp();
                            System.exit(1);
                        }
                        break;
                    case "test-tokyo":
                        System.out.println("🧪 Running Tokyo Travel Scenario Test...");
                        cli.testTokyoTravelScenario();
                        break;
                    case "help":
                        cli.showHelp();
                        break;
                    default:
                        System.err.println("Unknown command: " + command);
                        cli.showHelp();
                        System.exit(1);
                }
            } else {
                System.out.println("🚀 Starting AMCP MeshChat CLI...");
                cli.start();
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to start MeshChat CLI: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}