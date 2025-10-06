package io.amcp.cli;

import io.amcp.core.Agent;
import io.amcp.core.Event;
import io.amcp.core.AgentID;
import io.amcp.core.DeliveryOptions;
import io.amcp.examples.weather.WeatherAgent;

import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.nio.file.*;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

/**
 * Command Processor for AMCP Interactive CLI
 * 
 * Handles all command parsing, execution, and response formatting.
 * Supports advanced features like autocompletion, history, and troubleshooting.
 */
public class CommandProcessor {
    
    private final AMCPInteractiveCLI cli;
    private final AgentRegistry agentRegistry;
    private final HistoryManager historyManager;
    private final StatusMonitor statusMonitor;
    private final TroubleshootingTools troubleshootingTools;
    
    private boolean debugMode = false;
    private boolean verboseMode = false;
    
    private final Map<String, CommandHandler> commands = new HashMap<>();
    
    public CommandProcessor(AMCPInteractiveCLI cli, AgentRegistry agentRegistry, 
                          HistoryManager historyManager, StatusMonitor statusMonitor,
                          TroubleshootingTools troubleshootingTools) {
        this.cli = cli;
        this.agentRegistry = agentRegistry;
        this.historyManager = historyManager;
        this.statusMonitor = statusMonitor;
        this.troubleshootingTools = troubleshootingTools;
        
        initializeCommands();
    }
    
    private void initializeCommands() {
        // Core commands
        commands.put("help", this::handleHelp);
        commands.put("exit", this::handleExit);
        commands.put("quit", this::handleExit);
        commands.put("clear", this::handleClear);
        commands.put("version", this::handleVersion);
        
        // Agent management
        commands.put("agents", this::handleAgents);
        commands.put("activate", this::handleActivate);
        commands.put("deactivate", this::handleDeactivate);
        commands.put("status", this::handleStatus);
        
        // Agent interaction
        commands.put("send", this::handleSend);
        commands.put("ask", this::handleAsk);
        commands.put("weather", this::handleWeather);
        commands.put("chat", this::handleChat);
        
        // Session management
        commands.put("session", this::handleSession);
        commands.put("save", this::handleSave);
        commands.put("load", this::handleLoad);
        commands.put("share", this::handleShare);
        
        // History and clipboard
        commands.put("history", this::handleHistory);
        commands.put("copy", this::handleCopy);
        commands.put("paste", this::handlePaste);
        
        // Troubleshooting
        commands.put("debug", this::handleDebug);
        commands.put("verbose", this::handleVerbose);
        commands.put("trace", this::handleTrace);
        commands.put("monitor", this::handleMonitor);
        commands.put("logs", this::handleLogs);
        commands.put("api", this::handleApi);
        
        // Quick examples
        commands.put("examples", this::handleExamples);
        commands.put("demo", this::handleDemo);
    }
    
    public CommandResult processCommand(String input) {
        try {
            String[] parts = parseCommand(input);
            if (parts.length == 0) {
                return CommandResult.info("Empty command");
            }
            
            String command = parts[0].toLowerCase();
            String[] args = Arrays.copyOfRange(parts, 1, parts.length);
            
            CommandHandler handler = commands.get(command);
            if (handler != null) {
                return handler.handle(args);
            } else {
                return handleUnknownCommand(command, args);
            }
            
        } catch (Exception e) {
            return CommandResult.error("Command processing error: " + e.getMessage());
        }
    }
    
    private String[] parseCommand(String input) {
        // Simple parsing - could be enhanced with proper shell-like parsing
        return input.trim().split("\\s+");
    }
    
    private CommandResult handleHelp(String[] args) {
        if (args.length > 0) {
            return getCommandHelp(args[0]);
        }
        
        StringBuilder help = new StringBuilder();
        help.append("\n📚 AMCP Interactive CLI - Available Commands:\n\n");
        
        help.append("🤖 Agent Management:\n");
        help.append("  agents                    - List all registered agents and their status\n");
        help.append("  activate <agent>          - Activate an agent (travel, stock, weather, chat, etc.)\n");
        help.append("  deactivate <agent>        - Deactivate an agent\n");
        help.append("  status                    - Show system and agent status\n\n");
        
        help.append("💬 Agent Interaction:\n");
        help.append("  ask <agent> <question>    - Ask a question to a specific agent\n");
        help.append("  send <agent> <message>    - Send a message to an agent\n");
        help.append("  travel <destination>      - Quick travel planning\n");
        help.append("  stock <symbol>            - Get stock price information\n");
        help.append("  weather <location>        - Get weather information\n");
        help.append("  chat <message>            - Chat with the conversational agent\n\n");
        
        help.append("💾 Session Management:\n");
        help.append("  session <name>            - Create or switch to a session\n");
        help.append("  save <filename>           - Save current session\n");
        help.append("  load <filename>           - Load a saved session\n");
        help.append("  share                     - Generate shareable session link\n\n");
        
        help.append("📋 History & Clipboard:\n");
        help.append("  history                   - Show command history\n");
        help.append("  copy <text>               - Copy text to clipboard\n");
        help.append("  paste                     - Paste from clipboard\n\n");
        
        help.append("🔧 Troubleshooting:\n");
        help.append("  debug [on|off]            - Toggle debug mode\n");
        help.append("  verbose [on|off]          - Toggle verbose output\n");
        help.append("  trace <agent>             - Trace agent behavior\n");
        help.append("  monitor                   - Show real-time monitoring\n");
        help.append("  logs <agent>              - Show agent logs\n");
        help.append("  api                       - Show API status and statistics\n\n");
        
        help.append("📖 Examples & Demos:\n");
        help.append("  examples                  - Show usage examples\n");
        help.append("  demo <scenario>           - Run demo scenarios\n\n");
        
        help.append("⚙️  General:\n");
        help.append("  help [command]            - Show help (for specific command)\n");
        help.append("  version                   - Show CLI version\n");
        help.append("  clear                     - Clear screen\n");
        help.append("  exit/quit                 - Exit CLI\n\n");
        
        help.append("💡 Tips:\n");
        help.append("  - Use Tab for autocompletion\n");
        help.append("  - Commands support partial matching\n");
        help.append("  - Use 'help <command>' for detailed help\n");
        help.append("  - Session data is saved in ~/.amcp_cli_sessions/\n");
        
        return CommandResult.info(help.toString());
    }
    
    private CommandResult getCommandHelp(String command) {
        switch (command.toLowerCase()) {
            case "activate":
                return CommandResult.info("""
                    🚀 activate <agent-name>
                    
                    Activates a registered agent and makes it available for interaction.
                    
                    Available agents:
                    • travel    - Travel planning with flight/hotel booking
                    • stock     - Stock market data with Polygon.io API
                    • weather   - Weather information with OpenWeather API
                    • chat      - Simple conversational agent
                    • multichat - Enhanced multi-agent chat system
                    • orchestrator - Master orchestrator for workflows
                    
                    Examples:
                      activate weather
                    """);
                    
            case "weather":
                return CommandResult.info("""
                    🌤️  weather <location|command>
                    
                    Get weather information using OpenWeatherMap API.
                    
                    Commands:
                      weather London          - Current weather for London
                      weather forecast NYC    - 5-day forecast for NYC
                      weather alerts          - Weather alerts
                    
                    Environment:
                      Set OPENWEATHER_API_KEY for API access
                    """);
                    
            default:
                return CommandResult.warning("No detailed help available for: " + command);
        }
    }
    
    private CommandResult handleExit(String[] args) {
        System.out.println("👋 Exiting AMCP CLI...");
        cli.shutdown();
        System.exit(0);
        return CommandResult.success("Goodbye!");
    }
    
    private CommandResult handleClear(String[] args) {
        // ANSI escape codes to clear screen
        System.out.print("\033[2J\033[H");
        System.out.flush();
        return CommandResult.success("");
    }
    
    private CommandResult handleVersion(String[] args) {
        return CommandResult.info("""
            AMCP Interactive CLI v1.5.0-OPENSOURCE
            
            🏗️  Build: Open Source Edition
            ☕ Java: """ + System.getProperty("java.version") + """
            
            🏠 OS: """ + System.getProperty("os.name") + """
            
            📦 Components:
            • Agent Mesh Communication Protocol v1.5
            • LLM-Powered Orchestration (TinyLlama/Ollama)
            • Multi-Agent Coordination and Messaging
            • Interactive CLI with Session Management
            • Real-time Monitoring and Event Streaming
            """);
    }
    
    private CommandResult handleAgents(String[] args) {
        List<AgentRegistry.AgentStatus> agentStatuses = agentRegistry.getAgentStatus();
        
        StringBuilder result = new StringBuilder();
        result.append("\n🤖 Registered Agents:\n\n");
        
        for (AgentRegistry.AgentStatus status : agentStatuses) {
            result.append(status.toString()).append("\n");
            if (status.isActive() && verboseMode) {
                result.append("    ID: ").append(status.getAgentId()).append("\n");
                result.append("    Lifecycle: ").append(status.getLifecycle()).append("\n");
            }
        }
        
        result.append("\nActive: ").append(agentRegistry.getActiveAgentCount());
        result.append(" / Total: ").append(agentRegistry.getRegisteredAgentCount()).append("\n");
        
        return CommandResult.info(result.toString());
    }
    
    private CommandResult handleActivate(String[] args) {
        if (args.length == 0) {
            return CommandResult.error("Usage: activate <agent-name>");
        }
        
        return agentRegistry.activateAgent(args[0]);
    }
    
    private CommandResult handleDeactivate(String[] args) {
        if (args.length == 0) {
            return CommandResult.error("Usage: deactivate <agent-name>");
        }
        
        return agentRegistry.deactivateAgent(args[0]);
    }
    
    private CommandResult handleStatus(String[] args) {
        Map<String, Object> status = new HashMap<>();
        status.put("Active Agents", agentRegistry.getActiveAgentCount());
        status.put("Total Agents", agentRegistry.getRegisteredAgentCount());
        status.put("Debug Mode", debugMode);
        status.put("Verbose Mode", verboseMode);
        status.put("Current Session", cli.getCurrentSession());
        status.put("API Status", statusMonitor.getApiStatus());
        
        return CommandResult.info("System Status", status);
    }
    
    private CommandResult handleWeather(String[] args) {
        if (args.length == 0) {
            return CommandResult.error("Usage: weather <location>");
        }
        
        Agent weatherAgent = agentRegistry.getActiveAgent("weather");
        if (weatherAgent == null) {
            return CommandResult.error("Weather agent is not active. Use 'activate weather' first.");
        }
        
        String location = String.join(" ", args);
        
        try {
            if (weatherAgent instanceof WeatherAgent) {
                WeatherAgent wa = (WeatherAgent) weatherAgent;
                WeatherAgent.WeatherData weatherData = wa.getWeatherForLocation(location);
                
                if (weatherData != null) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("Location", weatherData.city);
                    result.put("Temperature", String.format("%.1f°C", weatherData.temperature));
                    result.put("Humidity", String.format("%.1f%%", weatherData.humidity));
                    result.put("Pressure", String.format("%.1f hPa", weatherData.pressure));
                    result.put("Condition", weatherData.description);
                    result.put("Wind Speed", String.format("%.1f m/s", weatherData.windSpeed));
                    result.put("Updated", weatherData.timestamp.toString());
                    
                    return CommandResult.success("Weather for " + location, result);
                } else {
                    return CommandResult.error("Could not fetch weather data for " + location + ". Please check the location name.");
                }
            } else {
                return CommandResult.error("Weather agent type not supported");
            }
        } catch (Exception e) {
            return CommandResult.error("Error fetching weather data: " + e.getMessage());
        }
    }
    
    private CommandResult handleChat(String[] args) {
        if (args.length == 0) {
            return CommandResult.error("Usage: chat <message>");
        }
        
        Agent chatAgent = agentRegistry.getActiveAgent("chat");
        if (chatAgent == null) {
            return CommandResult.error("Chat agent is not active. Use 'activate chat' first.");
        }
        
        String message = String.join(" ", args);
        return CommandResult.info("Chatting: " + message);
    }
    
    private CommandResult handleSend(String[] args) {
        if (args.length < 2) {
            return CommandResult.error("Usage: send <agent> <message>");
        }
        
        String agentName = args[0];
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        Agent agent = agentRegistry.getActiveAgent(agentName);
        if (agent == null) {
            return CommandResult.error("Agent '" + agentName + "' is not active");
        }
        
        try {
            // Create and send event
            Event event = Event.builder()
                .topic("cli.message")
                .payload(message)
                .sender(AgentID.named("cli-user"))
                .deliveryOptions(DeliveryOptions.reliable())
                .build();
            
            cli.getAgentContext().publishEvent(event);
            
            return CommandResult.success("Message sent to " + agentName);
            
        } catch (Exception e) {
            return CommandResult.error("Failed to send message: " + e.getMessage());
        }
    }
    
    private CommandResult handleAsk(String[] args) {
        if (args.length < 2) {
            return CommandResult.error("Usage: ask <agent> <question>");
        }
        
        String agentName = args[0];
        String question = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        return handleSend(new String[]{agentName, question});
    }
    
    private CommandResult handleSession(String[] args) {
        if (args.length == 0) {
            if (cli.getCurrentSession() == null) {
                return CommandResult.info("No active session");
            } else {
                return CommandResult.info("Current session: " + cli.getCurrentSession());
            }
        }
        
        String sessionName = args[0];
        cli.setCurrentSession(sessionName);
        return CommandResult.success("Switched to session: " + sessionName);
    }
    
    private CommandResult handleSave(String[] args) {
        if (args.length == 0) {
            return CommandResult.error("Usage: save <filename>");
        }
        
        String filename = args[0];
        try {
            Path sessionFile = Paths.get(cli.getSessionDir(), filename + ".json");
            
            Map<String, Object> session = new HashMap<>();
            session.put("timestamp", LocalDateTime.now().toString());
            session.put("session", cli.getCurrentSession());
            session.put("activeAgents", agentRegistry.getActiveAgents().keySet());
            session.put("history", historyManager.getRecentHistory(50));
            
            // Simple JSON-like format
            Files.writeString(sessionFile, session.toString());
            
            return CommandResult.success("Session saved to: " + sessionFile);
            
        } catch (Exception e) {
            return CommandResult.error("Failed to save session: " + e.getMessage());
        }
    }
    
    private CommandResult handleLoad(String[] args) {
        if (args.length == 0) {
            return CommandResult.error("Usage: load <filename>");
        }
        
        String filename = args[0];
        try {
            Path sessionFile = Paths.get(cli.getSessionDir(), filename + ".json");
            
            if (!Files.exists(sessionFile)) {
                return CommandResult.error("Session file not found: " + sessionFile);
            }
            
            String content = Files.readString(sessionFile);
            return CommandResult.success("Session loaded from: " + sessionFile + "\nContent preview: " + 
                content.substring(0, Math.min(100, content.length())) + "...");
            
        } catch (Exception e) {
            return CommandResult.error("Failed to load session: " + e.getMessage());
        }
    }
    
    private CommandResult handleShare(String[] args) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        return CommandResult.info("Session share ID: " + sessionId + 
            "\n📝 Note: Sharing functionality requires server deployment");
    }
    
    private CommandResult handleHistory(String[] args) {
        List<String> history = historyManager.getRecentHistory(20);
        
        if (history.isEmpty()) {
            return CommandResult.info("No command history");
        }
        
        StringBuilder result = new StringBuilder("\n📚 Recent Commands:\n\n");
        for (int i = 0; i < history.size(); i++) {
            result.append(String.format("%2d. %s\n", i + 1, history.get(i)));
        }
        
        return CommandResult.info(result.toString());
    }
    
    private CommandResult handleCopy(String[] args) {
        if (args.length == 0) {
            return CommandResult.error("Usage: copy <text>");
        }
        
        String text = String.join(" ", args);
        
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection selection = new StringSelection(text);
            clipboard.setContents(selection, null);
            
            return CommandResult.success("Text copied to clipboard");
            
        } catch (Exception e) {
            return CommandResult.error("Failed to copy to clipboard: " + e.getMessage());
        }
    }
    
    private CommandResult handlePaste(String[] args) {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable contents = clipboard.getContents(null);
            
            if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String text = (String) contents.getTransferData(DataFlavor.stringFlavor);
                return CommandResult.info("Clipboard content: " + text);
            } else {
                return CommandResult.warning("No text content in clipboard");
            }
            
        } catch (Exception e) {
            return CommandResult.error("Failed to paste from clipboard: " + e.getMessage());
        }
    }
    
    private CommandResult handleDebug(String[] args) {
        if (args.length > 0) {
            debugMode = "on".equalsIgnoreCase(args[0]);
        } else {
            debugMode = !debugMode;
        }
        
        return CommandResult.success("Debug mode: " + (debugMode ? "ON" : "OFF"));
    }
    
    private CommandResult handleVerbose(String[] args) {
        if (args.length > 0) {
            verboseMode = "on".equalsIgnoreCase(args[0]);
        } else {
            verboseMode = !verboseMode;
        }
        
        return CommandResult.success("Verbose mode: " + (verboseMode ? "ON" : "OFF"));
    }
    
    private CommandResult handleTrace(String[] args) {
        if (args.length == 0) {
            return troubleshootingTools.showActiveTraces();
        }
        
        String agentName = args[0];
        return troubleshootingTools.startTrace(agentName);
    }
    
    private CommandResult handleMonitor(String[] args) {
        return troubleshootingTools.showSystemMonitor();
    }
    
    private CommandResult handleLogs(String[] args) {
        if (args.length == 0) {
            return troubleshootingTools.showSystemLogs();
        }
        
        String agentName = args[0];
        return troubleshootingTools.showAgentLogs(agentName);
    }
    
    private CommandResult handleApi(String[] args) {
        return troubleshootingTools.showApiStatus();
    }
    
    private CommandResult handleExamples(String[] args) {
        return CommandResult.info("""
            📖 AMCP CLI Usage Examples:
            
            🚀 Quick Start:
              activate stock
              stock AAPL
              activate weather
              weather London
            
            💼 Stock Trading:
              activate stock
              stock prices AAPL,GOOGL,MSFT
              stock alert AAPL 150.00 ABOVE
              stock portfolio create "My Portfolio"
              stock market
            
            ✈️ Travel Planning:
              activate travel
              travel "New York to London"
              ask travel "Find hotels in Paris for Dec 15-20"
            
            🌤️ Weather Monitoring:
              activate weather
              weather forecast "San Francisco"
              ask weather "Will it rain tomorrow in Seattle?"
            
            🤖 Multi-Agent Scenarios:
              activate orchestrator
              activate stock
              activate weather
              ask orchestrator "Plan my trading day based on weather in NYC"
            
            💾 Session Management:
              session "trading-session"
              save trading-monday
              load trading-monday
            
            🔧 Troubleshooting:
              debug on
              trace stock
              monitor
              api
            """);
    }
    
    private CommandResult handleDemo(String[] args) {
        if (args.length == 0) {
            return CommandResult.info("""
                🎭 Available Demo Scenarios:
                
                demo quickstart    - Basic CLI interaction
                demo stock         - Stock market monitoring
                demo travel        - Travel planning workflow
                demo multiagent    - Multi-agent orchestration
                demo troubleshoot  - Troubleshooting tools
                """);
        }
        
        String scenario = args[0];
        switch (scenario.toLowerCase()) {
            case "quickstart":
                return runQuickstartDemo();
            case "multiagent":
                return runMultiAgentDemo();
            case "troubleshoot":
                return runTroubleshootDemo();
            default:
                return CommandResult.error("Unknown demo scenario: " + scenario);
        }
    }
    
    private CommandResult runQuickstartDemo() {
        return CommandResult.info("""
            🎭 Quickstart Demo:
            
            1. First, activate an agent:
               > activate stock
            
            2. Check agent status:
               > agents
            
            3. Interact with the agent:
               > stock AAPL
            
            4. View system status:
               > status
            
            5. Get help:
               > help stock
            
            Try these commands now!
            """);
    }
    

    
    private CommandResult runMultiAgentDemo() {
        return CommandResult.info("""
            🤖 Multi-Agent Demo:
            
            Commands to try:
              activate orchestrator
              activate stock
              activate weather
              ask orchestrator "Combine weather and stock data for trading"
            
            This demonstrates advanced multi-agent coordination.
            """);
    }
    
    private CommandResult runTroubleshootDemo() {
        return CommandResult.info("""
            🔧 Troubleshooting Demo:
            
            Commands to try:
              debug on
              verbose on
              activate stock
              trace stock
              monitor
              logs stock
              api
            
            This demonstrates debugging and monitoring capabilities.
            """);
    }
    
    private CommandResult handleUnknownCommand(String command, String[] args) {
        // Try partial matching
        List<String> matches = commands.keySet().stream()
            .filter(cmd -> cmd.startsWith(command.toLowerCase()))
            .collect(Collectors.toList());
        
        if (matches.size() == 1) {
            // Single partial match - execute it
            return commands.get(matches.get(0)).handle(args);
        } else if (matches.size() > 1) {
            return CommandResult.error("Ambiguous command '" + command + "'. Matches: " + matches);
        } else {
            return CommandResult.error("Unknown command: " + command + ". Type 'help' for available commands.");
        }
    }
    
    public boolean isDebugMode() {
        return debugMode;
    }
    
    public boolean isVerboseMode() {
        return verboseMode;
    }
    
    @FunctionalInterface
    private interface CommandHandler {
        CommandResult handle(String[] args);
    }
}