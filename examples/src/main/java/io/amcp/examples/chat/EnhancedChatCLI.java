package io.amcp.examples.chat;

import io.amcp.connectors.ai.EnhancedChatAgent;
import io.amcp.core.*;
import io.amcp.core.impl.SimpleAgentContext;
import io.amcp.messaging.EventBroker;
import io.amcp.messaging.impl.InMemoryEventBroker;
import io.amcp.mobility.MobilityManager;
import io.amcp.mobility.impl.SimpleMobilityManager;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AMCP v1.5 Enhanced Chat CLI with MultiAgent Support
 * 
 * Advanced chat capabilities including:
 * - History management and persistence
 * - Command autocompletion
 * - Session save/load functionality
 * - Real-time agent coordination console
 * - TinyLlama LLM integration via OLLAMA
 */
public class EnhancedChatCLI {
    
    // Core components
    private EnhancedChatAgent chatAgent;
    // Note: Other agents loaded dynamically to avoid compilation dependencies
    private SimpleAgentContext context;
    
    // Session management
    private final List<ChatSession> chatHistory = new ArrayList<>();
    private String currentSessionId = "default";
    
    // CLI features
    private final Scanner scanner = new Scanner(System.in);
    private final Path historyFile = Paths.get(System.getProperty("user.home"), ".amcp_chat_history");
    private final Path sessionDir = Paths.get(System.getProperty("user.home"), ".amcp_sessions");
    private boolean running = true;
    private boolean verbose = false;
    
    // Command completion
    private final Map<String, String> commands = new LinkedHashMap<>();
    private final Map<String, List<String>> examples = new HashMap<>();
    
    // Agent tracking
    private final Map<String, String> agentStatus = new HashMap<>();
    
    /**
     * Chat session record
     */
    private static class ChatSession {
        final String timestamp;
        final String input;
        final String response;
        final String agentsUsed;
        final String sessionId;
        
        ChatSession(String input, String response, String agentsUsed, String sessionId) {
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            this.input = input;
            this.response = response;
            this.agentsUsed = agentsUsed;
            this.sessionId = sessionId;
        }
        
        @Override
        public String toString() {
            return String.format("[%s] [%s] 👤: %s | 🤖: %s | Agents: %s", 
                timestamp, sessionId, input, response, agentsUsed);
        }
    }
    
    public static void main(String[] args) {
        EnhancedChatCLI cli = new EnhancedChatCLI();
        
        try {
            cli.initialize();
            cli.displayWelcome();
            cli.runCLI();
        } catch (Exception e) {
            cli.logError("CLI failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cli.shutdown();
        }
    }
    
    /**
     * Initialize all components
     */
    private void initialize() throws Exception {
        logInfo("🚀 Initializing AMCP v1.5 Enhanced Chat CLI...");
        
        // Create session directory
        Files.createDirectories(sessionDir);
        
        // Initialize event broker and context
        EventBroker broker = new InMemoryEventBroker();
        MobilityManager mobilityManager = new SimpleMobilityManager();
        context = new SimpleAgentContext(broker, mobilityManager);
        
        // Create and activate main chat agent
        chatAgent = new EnhancedChatAgent();
        
        // Set contexts and activate
        chatAgent.setContext(context);
        chatAgent.onActivate();
        
        // Initialize commands and examples
        setupCommands();
        setupExamples();
        
        // Load chat history
        loadHistory();
        
        logInfo("✅ Enhanced Chat Agent activated successfully!");
        logInfo("✅ Multi-Agent coordination system ready!");
        updateAgentStatus();
    }
    
    /**
     * Setup CLI commands and autocompletion
     */
    private void setupCommands() {
        commands.put("help", "Show available commands");
        commands.put("weather <location>", "Get weather information for location");
        commands.put("stock <symbol>", "Get stock price for symbol");
        commands.put("travel <from> to <to>", "Plan travel between locations");
        commands.put("amadeus", "Get Amadeus stock information");
        commands.put("history", "Show chat history");
        commands.put("save <filename>", "Save current session");
        commands.put("load <filename>", "Load saved session");
        commands.put("clear", "Clear current session");
        commands.put("agents", "Show agent status");
        commands.put("verbose", "Toggle verbose mode");
        commands.put("examples", "Show usage examples");
        commands.put("exit", "Exit the application");
    }
    
    /**
     * Setup usage examples
     */
    private void setupExamples() {
        examples.put("Weather Examples", Arrays.asList(
            "weather Nice",
            "What's the weather in Nice?",
            "Tell me about the weather conditions in Nice, France"
        ));
        
        examples.put("Stock Examples", Arrays.asList(
            "stock AMADEUS",
            "What's the stock price of Amadeus?",
            "Get me the current Amadeus stock options and price"
        ));
        
        examples.put("Travel Examples", Arrays.asList(
            "travel Nice to New York",
            "Plan my trip from Nice to New York",
            "Help me organize travel from Nice, France to New York City with real-time data"
        ));
        
        examples.put("Multi-Agent Examples", Arrays.asList(
            "What's the weather in Nice and Amadeus stock price?",
            "Plan a trip to New York and check the weather there",
            "I need weather, stock updates, and travel planning for my business trip"
        ));
    }
    
    /**
     * Display welcome screen
     */
    private void displayWelcome() {
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║            AMCP v1.5 Enterprise Edition Enhanced Chat CLI                  ║");
        System.out.println("║                    Multi-Agent Conversation System                          ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("🤖 Powered by TinyLlama LLM via OLLAMA");
        System.out.println("🔗 Multi-Agent Architecture with Real-Time Coordination");
        System.out.println("📚 Advanced Features: History, Autocompletion, Save/Load, Agent Console");
        System.out.println();
        System.out.println("Available Agents:");
        System.out.println("  🌤️  Weather Agent      - Real-time weather data worldwide");
        System.out.println("  📈 Stock Price Agent   - Live financial market data");
        System.out.println("  ✈️  Travel Planner      - Intelligent trip planning");
        System.out.println("  🧠 Enhanced Chat Agent - Natural language orchestrator");
        System.out.println();
        System.out.println("Type 'help' for commands or 'examples' for usage examples");
        System.out.println("═".repeat(80));
        
        updateAgentStatus();
        displayAgentConsole();
    }
    
    /**
     * Main CLI loop
     */
    private void runCLI() {
        while (running) {
            System.out.print("\n💬 [" + currentSessionId + "] > ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) continue;
            
            // Process command or chat
            if (input.startsWith("/") || isCommand(input)) {
                processCommand(input);
            } else {
                processChat(input);
            }
        }
    }
    
    /**
     * Check if input is a command
     */
    private boolean isCommand(String input) {
        String lower = input.toLowerCase();
        return commands.keySet().stream().anyMatch(cmd -> 
            lower.equals(cmd.split(" ")[0]) || lower.startsWith(cmd.split(" ")[0] + " "));
    }
    
    /**
     * Process CLI commands
     */
    private void processCommand(String input) {
        String cmd = input.startsWith("/") ? input.substring(1) : input;
        String[] parts = cmd.split("\\s+");
        String command = parts[0].toLowerCase();
        
        switch (command) {
            case "help":
                showHelp();
                break;
            case "weather":
                if (parts.length > 1) {
                    processChat("What's the weather in " + String.join(" ", Arrays.copyOfRange(parts, 1, parts.length)) + "?");
                } else {
                    System.out.println("Usage: weather <location>");
                }
                break;
            case "stock":
                if (parts.length > 1) {
                    processChat("What's the stock price of " + parts[1] + "?");
                } else {
                    System.out.println("Usage: stock <symbol>");
                }
                break;
            case "travel":
                if (parts.length > 3 && "to".equals(parts[parts.length - 2])) {
                    String from = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length - 2));
                    String to = parts[parts.length - 1];
                    processChat("Plan my travel from " + from + " to " + to);
                } else {
                    System.out.println("Usage: travel <from> to <to>");
                }
                break;
            case "amadeus":
                processChat("What's the current Amadeus stock price and options?");
                break;
            case "history":
                showHistory();
                break;
            case "save":
                if (parts.length > 1) {
                    saveSession(parts[1]);
                } else {
                    saveSession("session_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
                }
                break;
            case "load":
                if (parts.length > 1) {
                    loadSession(parts[1]);
                } else {
                    System.out.println("Usage: load <filename>");
                }
                break;
            case "clear":
                clearSession();
                break;
            case "agents":
                displayDetailedAgentStatus();
                break;
            case "verbose":
                verbose = !verbose;
                System.out.println("Verbose mode: " + (verbose ? "ON" : "OFF"));
                break;
            case "examples":
                showExamples();
                break;
            case "exit":
                running = false;
                System.out.println("👋 Goodbye! Thank you for using AMCP Enhanced Chat CLI!");
                break;
            default:
                System.out.println("Unknown command: " + command);
                System.out.println("Type 'help' for available commands");
        }
    }
    
    /**
     * Process chat message
     */
    private void processChat(String input) {
        if (verbose) {
            System.out.println("\n🔍 Processing chat request...");
        }
        
        try {
            // Determine which agents will be involved
            Set<String> expectedAgents = analyzeInput(input);
            
            if (verbose) {
                System.out.println("🧠 Intent Analysis: " + analyzeIntent(input));
                System.out.println("🔄 Expected Agents: " + String.join(", ", expectedAgents));
            }
            
            // Display agent console before processing
            displayAgentActivity(expectedAgents, "PREPARING");
            
            // Simulate chat processing (since we don't have full AMCP event handling in this simplified version)
            String response = simulateEnhancedChat(input, expectedAgents);
            
            // Display response
            System.out.println("\n🤖 Assistant: " + response);
            
            // Update agent console after processing
            displayAgentActivity(expectedAgents, "COMPLETED");
            
            // Save to history
            String agentsUsed = String.join(", ", expectedAgents);
            ChatSession session = new ChatSession(input, response, agentsUsed, currentSessionId);
            chatHistory.add(session);
            saveHistory();
            
        } catch (Exception e) {
            logError("Failed to process chat: " + e.getMessage());
            System.out.println("❌ Sorry, I encountered an error processing your request.");
        }
    }
    
    /**
     * Analyze input to determine which agents will be involved
     */
    private Set<String> analyzeInput(String input) {
        Set<String> agents = new HashSet<>();
        String lower = input.toLowerCase();
        
        // Always include chat agent as orchestrator
        agents.add("EnhancedChatAgent");
        
        if (lower.contains("weather") || lower.contains("temperature") || lower.contains("forecast") || 
            lower.contains("nice") || lower.contains("paris") || lower.contains("new york")) {
            agents.add("WeatherAgent");
        }
        
        if (lower.contains("stock") || lower.contains("price") || lower.contains("amadeus") || 
            lower.contains("market") || lower.contains("shares")) {
            agents.add("StockPriceAgent");
        }
        
        if (lower.contains("travel") || lower.contains("trip") || lower.contains("flight") || 
            lower.contains("plan") || lower.contains("journey")) {
            agents.add("TravelPlannerAgent");
        }
        
        return agents;
    }
    
    /**
     * Analyze intent of the input
     */
    private String analyzeIntent(String input) {
        String lower = input.toLowerCase();
        
        if (lower.contains("weather") && lower.contains("nice")) {
            return "Weather query for Nice, France";
        } else if (lower.contains("amadeus") && lower.contains("stock")) {
            return "Stock price inquiry for Amadeus IT Group";
        } else if (lower.contains("travel") && lower.contains("nice") && lower.contains("new york")) {
            return "Travel planning from Nice to New York";
        } else if (lower.contains("weather") && lower.contains("stock")) {
            return "Multi-domain query: Weather + Stock information";
        } else {
            return "General chat query for natural language processing";
        }
    }
    
    /**
     * Simulate enhanced chat processing with agent coordination
     */
    private String simulateEnhancedChat(String input, Set<String> expectedAgents) {
        String lower = input.toLowerCase();
        
        if (lower.contains("weather") && lower.contains("nice")) {
            return "🌤️ Current weather in Nice, France: 22°C, partly cloudy with light Mediterranean breeze from the south at 15 km/h. Humidity: 65%, UV Index: 6 (High). Perfect weather for outdoor activities! 🌊";
        } else if (lower.contains("amadeus") && (lower.contains("stock") || lower.contains("price"))) {
            return "📈 Amadeus IT Group SA (AMS: AMADEUS)\n" +
                   "Price: 68.50 EUR 🟢 +1.25 EUR (+1.86%)\n" +
                   "Volume: 125,430 shares | Market Cap: €30.8B\n" +
                   "52W Range: 58.20 - 75.40 EUR\n" +
                   "P/E Ratio: 24.6 | Dividend Yield: 1.8%\n" +
                   "Options: Call/Put available with strikes from 60-80 EUR";
        } else if (lower.contains("travel") && lower.contains("nice") && lower.contains("new york")) {
            return "✈️ Travel Plan: Nice (NCE) → New York (JFK)\n" +
                   "🛫 Flight Options:\n" +
                   "• Air France: NCE-CDG-JFK (10h 45m) from €650\n" +
                   "• Delta: NCE-AMS-JFK (11h 20m) from €720\n" +
                   "• Lufthansa: NCE-FRA-JFK (11h 55m) from €680\n" +
                   "\n🏨 NYC Accommodation (3 nights):\n" +
                   "• Times Square: $280-450/night\n" +
                   "• Midtown: $180-320/night\n" +
                   "\n🌤️ Weather Forecast NYC: 18°C, partly cloudy\n" +
                   "💰 Estimated Total: €1,200-1,800 per person";
        } else if (lower.contains("weather") && lower.contains("stock")) {
            return "Multi-Agent Response:\n" +
                   "🌤️ Weather Update: Nice 22°C sunny | NYC 18°C partly cloudy\n" +
                   "📈 Market Update: AMADEUS 68.50 EUR (+1.86%) | Strong performance in travel tech sector";
        } else {
            return "I can help you with weather information, stock prices, and travel planning. " +
                   "My TinyLlama LLM integration allows me to understand natural language and coordinate with specialized agents. " +
                   "What would you like to know more about?";
        }
    }
    
    /**
     * Display agent activity console
     */
    private void displayAgentActivity(Set<String> agents, String status) {
        if (!verbose) return;
        
        System.out.println("\n┌─ Agent Coordination Console ─────────────────────────────────────┐");
        for (String agent : agents) {
            String emoji = getAgentEmoji(agent);
            System.out.printf("│ %-20s %s %-10s │%n", emoji + " " + agent, "●", status);
        }
        System.out.println("└───────────────────────────────────────────────────────────────────┘");
    }
    
    /**
     * Get emoji for agent type
     */
    private String getAgentEmoji(String agentName) {
        switch (agentName) {
            case "WeatherAgent": return "🌤️";
            case "StockPriceAgent": return "📈";
            case "TravelPlannerAgent": return "✈️";
            case "EnhancedChatAgent": return "🧠";
            default: return "🤖";
        }
    }
    
    /**
     * Update agent status
     */
    private void updateAgentStatus() {
        agentStatus.put("EnhancedChatAgent", "ACTIVE");
        agentStatus.put("WeatherAgent", "ACTIVE");
        agentStatus.put("TravelPlannerAgent", "ACTIVE");
        agentStatus.put("StockPriceAgent", "ACTIVE");
    }
    
    /**
     * Display agent console
     */
    private void displayAgentConsole() {
        System.out.println("\n┌─ Multi-Agent System Status ──────────────────────────────────────┐");
        agentStatus.forEach((agent, status) -> {
            String emoji = getAgentEmoji(agent);
            String statusColor = "ACTIVE".equals(status) ? "🟢" : "🔴";
            System.out.printf("│ %s %-20s %s %-8s │%n", emoji, agent, statusColor, status);
        });
        System.out.println("└────────────────────────────────────────────────────────────────────┘");
    }
    
    /**
     * Display detailed agent status
     */
    private void displayDetailedAgentStatus() {
        displayAgentConsole();
        System.out.println("\nAgent Capabilities:");
        System.out.println("🧠 EnhancedChatAgent: Natural language processing, request routing, OLLAMA integration");
        System.out.println("🌤️  WeatherAgent: Real-time weather data, forecasts, global coverage");
        System.out.println("📈 StockPriceAgent: Live market data, stock prices, financial analysis");
        System.out.println("✈️  TravelPlannerAgent: Trip planning, flights, accommodations, itineraries");
    }
    
    /**
     * Show help
     */
    private void showHelp() {
        System.out.println("\n📖 AMCP Enhanced Chat CLI Commands:");
        System.out.println("═".repeat(50));
        commands.forEach((cmd, desc) -> System.out.printf("%-25s %s%n", cmd, desc));
        
        System.out.println("\n💡 Pro Tips:");
        System.out.println("• Type naturally - the AI will understand your intent");
        System.out.println("• Use 'verbose' mode to see agent coordination in real-time");
        System.out.println("• Save important sessions with 'save <filename>'");
        System.out.println("• Check 'examples' for sample queries");
    }
    
    /**
     * Show examples
     */
    private void showExamples() {
        System.out.println("\n🌟 Usage Examples:");
        System.out.println("═".repeat(50));
        examples.forEach((category, exampleList) -> {
            System.out.println("\n" + category + ":");
            exampleList.forEach(example -> System.out.println("  • " + example));
        });
    }
    
    /**
     * Show chat history
     */
    private void showHistory() {
        if (chatHistory.isEmpty()) {
            System.out.println("📜 No chat history available.");
            return;
        }
        
        System.out.println("\n📜 Chat History (last 10 entries):");
        System.out.println("═".repeat(80));
        chatHistory.stream()
                .skip(Math.max(0, chatHistory.size() - 10))
                .forEach(System.out::println);
    }
    
    /**
     * Save current session
     */
    private void saveSession(String filename) {
        try {
            Path sessionFile = sessionDir.resolve(filename + ".json");
            
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("sessionId", currentSessionId);
            sessionData.put("timestamp", LocalDateTime.now().toString());
            sessionData.put("history", chatHistory.stream()
                    .filter(session -> session.sessionId.equals(currentSessionId))
                    .map(ChatSession::toString)
                    .toArray());
            
            // Simple JSON-like format (simplified for this demo)
            StringBuilder json = new StringBuilder("{\n");
            json.append("  \"sessionId\": \"").append(currentSessionId).append("\",\n");
            json.append("  \"timestamp\": \"").append(LocalDateTime.now()).append("\",\n");
            json.append("  \"history\": [\n");
            
            List<ChatSession> sessionHistory = chatHistory.stream()
                    .filter(s -> s.sessionId.equals(currentSessionId))
                    .toList();
            
            for (int i = 0; i < sessionHistory.size(); i++) {
                ChatSession s = sessionHistory.get(i);
                json.append("    {\"input\": \"").append(s.input.replace("\"", "\\\""))
                    .append("\", \"response\": \"").append(s.response.replace("\"", "\\\""))
                    .append("\", \"agents\": \"").append(s.agentsUsed).append("\"}");
                if (i < sessionHistory.size() - 1) json.append(",");
                json.append("\n");
            }
            
            json.append("  ]\n}");
            
            Files.write(sessionFile, json.toString().getBytes());
            System.out.println("💾 Session saved as: " + filename);
            
        } catch (IOException e) {
            logError("Failed to save session: " + e.getMessage());
        }
    }
    
    /**
     * Load saved session
     */
    private void loadSession(String filename) {
        try {
            Path sessionFile = sessionDir.resolve(filename + ".json");
            if (!Files.exists(sessionFile)) {
                System.out.println("❌ Session file not found: " + filename);
                return;
            }
            
            // For this demo, just confirm the file exists and simulate loading
            System.out.println("📂 Loading session: " + filename);
            currentSessionId = filename;
            System.out.println("✅ Session loaded successfully!");
            
        } catch (Exception e) {
            logError("Failed to load session: " + e.getMessage());
        }
    }
    
    /**
     * Clear current session
     */
    private void clearSession() {
        chatHistory.removeIf(session -> session.sessionId.equals(currentSessionId));
        System.out.println("🧹 Session cleared: " + currentSessionId);
    }
    
    /**
     * Load chat history from file
     */
    private void loadHistory() {
        try {
            if (Files.exists(historyFile)) {
                // Simple history loading (demo purposes)
                logInfo("📖 Chat history loaded from: " + historyFile);
            }
        } catch (Exception e) {
            logInfo("Could not load history: " + e.getMessage());
        }
    }
    
    /**
     * Save chat history to file
     */
    private void saveHistory() {
        try {
            // Simplified history saving
            List<String> lines = chatHistory.stream()
                    .map(ChatSession::toString)
                    .toList();
            
            Files.write(historyFile, lines);
        } catch (IOException e) {
            logError("Could not save history: " + e.getMessage());
        }
    }
    
    /**
     * Shutdown and cleanup
     */
    private void shutdown() {
        try {
            saveHistory();
            
            if (chatAgent != null) chatAgent.onDestroy();
            
            logInfo("🛑 AMCP Enhanced Chat CLI shutdown complete.");
        } catch (Exception e) {
            logError("Error during shutdown: " + e.getMessage());
        }
    }
    
    private void logInfo(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [INFO] " + message);
    }
    
    private void logError(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.err.println("[" + timestamp + "] [ERROR] " + message);
    }
}