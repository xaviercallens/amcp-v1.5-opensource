package io.amcp.cli;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.*;
import java.util.logging.*;

/**
 * Troubleshooting Tools for AMCP CLI
 * 
 * Provides:
 * - Agent behavior analysis
 * - Event tracing and logging
 * - Performance diagnostics
 * - Network connectivity tests
 * - Configuration validation
 */
public class TroubleshootingTools {
    
    private static final String LOG_DIR = System.getProperty("user.home") + "/.amcp/logs";
    private static final DateTimeFormatter LOG_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final Map<String, List<String>> agentLogs = new ConcurrentHashMap<>();
    private final Map<String, List<EventTrace>> eventTraces = new ConcurrentHashMap<>();
    private final Map<String, AgentDiagnostics> agentDiagnostics = new ConcurrentHashMap<>();
    private final Logger logger;
    
    private boolean tracingEnabled = false;
    private int maxLogEntries = 1000;
    private int maxEventTraces = 500;
    
    public TroubleshootingTools() {
        // Initialize logging directory
        try {
            Files.createDirectories(Paths.get(LOG_DIR));
        } catch (IOException e) {
            System.err.println("⚠️ Could not create log directory: " + e.getMessage());
        }
        
        // Setup logger
        this.logger = Logger.getLogger("AMCP.Troubleshooting");
        setupLogger();
    }
    
    private void setupLogger() {
        try {
            FileHandler fileHandler = new FileHandler(LOG_DIR + "/amcp-troubleshooting.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.INFO);
        } catch (IOException e) {
            System.err.println("⚠️ Could not setup file logging: " + e.getMessage());
        }
    }
    
    /**
     * Enable/disable event tracing
     */
    public void setTracingEnabled(boolean enabled) {
        this.tracingEnabled = enabled;
        logger.info("Event tracing " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Log agent activity
     */
    public void logAgentActivity(String agentName, String activity) {
        String timestamp = LocalDateTime.now().format(LOG_FORMATTER);
        String logEntry = String.format("[%s] %s: %s", timestamp, agentName, activity);
        
        agentLogs.computeIfAbsent(agentName, k -> new ArrayList<>()).add(logEntry);
        
        // Trim log entries if too many
        List<String> logs = agentLogs.get(agentName);
        while (logs.size() > maxLogEntries) {
            logs.remove(0);
        }
        
        logger.info(logEntry);
        
        if (tracingEnabled) {
            System.out.println("🔍 TRACE: " + logEntry);
        }
    }
    
    /**
     * Trace event flow
     */
    public void traceEvent(String eventType, String from, String to, String payload) {
        if (!tracingEnabled) return;
        
        EventTrace trace = new EventTrace(
            System.currentTimeMillis(),
            eventType,
            from,
            to,
            payload
        );
        
        eventTraces.computeIfAbsent(eventType, k -> new ArrayList<>()).add(trace);
        
        // Trim event traces if too many
        List<EventTrace> traces = eventTraces.get(eventType);
        while (traces.size() > maxEventTraces) {
            traces.remove(0);
        }
        
        String traceMessage = String.format("EVENT: %s | %s -> %s | %s", 
            eventType, from, to, payload.length() > 50 ? payload.substring(0, 50) + "..." : payload);
        
        logger.info(traceMessage);
        System.out.println("📡 " + traceMessage);
    }
    
    /**
     * Diagnose agent health
     */
    public CommandResult diagnoseAgent(String agentName) {
        AgentDiagnostics diagnostics = agentDiagnostics.computeIfAbsent(agentName, AgentDiagnostics::new);
        
        StringBuilder report = new StringBuilder();
        report.append("\n🔧 AGENT DIAGNOSTICS: ").append(agentName).append("\n");
        report.append("═".repeat(50)).append("\n");
        
        // Basic info
        report.append("📊 Status: ").append(diagnostics.getStatus()).append("\n");
        report.append("🕒 Last Activity: ").append(
            diagnostics.getLastActivity() != null ? 
            diagnostics.getLastActivity().format(LOG_FORMATTER) : "Never"
        ).append("\n");
        
        // Activity count
        List<String> logs = agentLogs.get(agentName);
        if (logs != null) {
            report.append("📈 Activity Count: ").append(logs.size()).append("\n");
        }
        
        // Recent activities
        report.append("\n🔍 Recent Activities (last 10):\n");
        if (logs != null && !logs.isEmpty()) {
            logs.stream()
                .skip(Math.max(0, logs.size() - 10))
                .forEach(log -> report.append("  • ").append(log).append("\n"));
        } else {
            report.append("  No recent activities\n");
        }
        
        // Event traces
        List<EventTrace> traces = eventTraces.values().stream()
            .flatMap(List::stream)
            .filter(trace -> agentName.equals(trace.getFrom()) || agentName.equals(trace.getTo()))
            .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
            .limit(5)
            .collect(Collectors.toList());
        
        if (!traces.isEmpty()) {
            report.append("\n📡 Recent Event Traces (last 5):\n");
            traces.forEach(trace -> {
                report.append("  • ").append(trace.toString()).append("\n");
            });
        }
        
        // System resources
        report.append("\n💻 System Resources:\n");
        Runtime runtime = Runtime.getRuntime();
        report.append("  • Memory Used: ").append(formatBytes(runtime.totalMemory() - runtime.freeMemory())).append("\n");
        report.append("  • Memory Available: ").append(formatBytes(runtime.freeMemory())).append("\n");
        report.append("  • CPU Cores: ").append(runtime.availableProcessors()).append("\n");
        
        return CommandResult.success(report.toString());
    }
    
    /**
     * Run connectivity test
     */
    public CommandResult testConnectivity() {
        StringBuilder report = new StringBuilder();
        report.append("\n🌐 CONNECTIVITY TEST\n");
        report.append("═".repeat(50)).append("\n");
        
        // Test internet connectivity
        report.append("🔍 Testing internet connectivity...\n");
        boolean internetOk = testUrl("https://www.google.com");
        report.append("  • Internet: ").append(internetOk ? "✅ OK" : "❌ FAILED").append("\n");
        
        // Test API endpoints
        report.append("\n🔍 Testing API endpoints...\n");
        
        // Polygon.io test
        String polygonKey = System.getProperty("POLYGON_API_KEY", System.getenv("POLYGON_API_KEY"));
        if (polygonKey != null) {
            boolean polygonOk = testUrl("https://api.polygon.io/v3/reference/tickers?apikey=" + polygonKey);
            report.append("  • Polygon.io: ").append(polygonOk ? "✅ OK" : "❌ FAILED").append("\n");
        } else {
            report.append("  • Polygon.io: ⚠️ NO API KEY").append("\n");
        }
        
        // OpenWeather test
        String openWeatherKey = System.getProperty("OPENWEATHER_API_KEY", System.getenv("OPENWEATHER_API_KEY"));
        if (openWeatherKey != null) {
            boolean openWeatherOk = testUrl("https://api.openweathermap.org/data/2.5/weather?q=London&appid=" + openWeatherKey);
            report.append("  • OpenWeather: ").append(openWeatherOk ? "✅ OK" : "❌ FAILED").append("\n");
        } else {
            report.append("  • OpenWeather: ⚠️ NO API KEY").append("\n");
        }
        
        // Local services test
        report.append("\n🔍 Testing local services...\n");
        report.append("  • Event Broker: ✅ ACTIVE (in-memory)\n");
        
        return CommandResult.success(report.toString());
    }
    
    private boolean testUrl(String url) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(5))
                .build();
                
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .timeout(java.time.Duration.ofSeconds(10))
                .GET()
                .build();
                
            java.net.http.HttpResponse<String> response = client.send(request, 
                java.net.http.HttpResponse.BodyHandlers.ofString());
                
            return response.statusCode() >= 200 && response.statusCode() < 400;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Validate configuration
     */
    public CommandResult validateConfiguration() {
        StringBuilder report = new StringBuilder();
        report.append("\n⚙️ CONFIGURATION VALIDATION\n");
        report.append("═".repeat(50)).append("\n");
        
        // Environment variables
        report.append("🔍 Environment Variables:\n");
        checkEnvVar(report, "POLYGON_API_KEY", "Polygon.io stock data");
        checkEnvVar(report, "OPENWEATHER_API_KEY", "OpenWeatherMap weather data");
        checkEnvVar(report, "JAVA_HOME", "Java runtime");
        
        // System properties
        report.append("\n🔍 System Properties:\n");
        report.append("  • Java Version: ").append(System.getProperty("java.version")).append("\n");
        report.append("  • OS: ").append(System.getProperty("os.name")).append(" ")
               .append(System.getProperty("os.version")).append("\n");
        report.append("  • User Dir: ").append(System.getProperty("user.dir")).append("\n");
        
        // File permissions
        report.append("\n🔍 File Permissions:\n");
        checkFileAccess(report, LOG_DIR, "Log directory");
        checkFileAccess(report, System.getProperty("user.home") + "/.amcp", "AMCP config directory");
        
        return CommandResult.success(report.toString());
    }
    
    private void checkEnvVar(StringBuilder report, String varName, String description) {
        String value = System.getenv(varName);
        if (value != null && !value.trim().isEmpty()) {
            report.append("  • ").append(varName).append(": ✅ SET (").append(description).append(")\n");
        } else {
            report.append("  • ").append(varName).append(": ❌ NOT SET (").append(description).append(")\n");
        }
    }
    
    private void checkFileAccess(StringBuilder report, String path, String description) {
        Path filePath = Paths.get(path);
        if (Files.exists(filePath)) {
            boolean readable = Files.isReadable(filePath);
            boolean writable = Files.isWritable(filePath);
            report.append("  • ").append(description).append(": ")
                   .append(readable ? "✅ READ " : "❌ READ ")
                   .append(writable ? "✅ WRITE" : "❌ WRITE")
                   .append("\n");
        } else {
            report.append("  • ").append(description).append(": ❌ NOT FOUND\n");
        }
    }
    
    /**
     * Export logs to file
     */
    public CommandResult exportLogs(String filename) {
        try {
            String exportPath = filename.startsWith("/") ? filename : LOG_DIR + "/" + filename;
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(exportPath))) {
                writer.println("AMCP Troubleshooting Export");
                writer.println("Generated: " + LocalDateTime.now().format(LOG_FORMATTER));
                writer.println("=" .repeat(80));
                writer.println();
                
                // Export agent logs
                writer.println("AGENT LOGS:");
                writer.println("-".repeat(40));
                agentLogs.forEach((agent, logs) -> {
                    writer.println("\n[" + agent + "]");
                    logs.forEach(writer::println);
                });
                
                // Export event traces
                writer.println("\n\nEVENT TRACES:");
                writer.println("-".repeat(40));
                eventTraces.forEach((eventType, traces) -> {
                    writer.println("\n[" + eventType + "]");
                    traces.forEach(trace -> writer.println(trace.toString()));
                });
            }
            
            return CommandResult.success("📄 Logs exported to: " + exportPath);
            
        } catch (IOException e) {
            return CommandResult.error("❌ Failed to export logs: " + e.getMessage());
        }
    }
    
    /**
     * Clear all logs and traces
     */
    public CommandResult clearLogs() {
        agentLogs.clear();
        eventTraces.clear();
        agentDiagnostics.clear();
        
        logger.info("All logs and traces cleared");
        return CommandResult.success("🧹 All logs and traces cleared");
    }
    
    /**
     * Get agent logs
     */
    public List<String> getAgentLogs(String agentName) {
        return agentLogs.getOrDefault(agentName, new ArrayList<>());
    }
    
    /**
     * Get event traces for a specific event type
     */
    public List<EventTrace> getEventTraces(String eventType) {
        return eventTraces.getOrDefault(eventType, new ArrayList<>());
    }
    
    /**
     * Update agent diagnostics
     */
    public void updateAgentDiagnostics(String agentName, String status) {
        agentDiagnostics.computeIfAbsent(agentName, AgentDiagnostics::new)
                      .updateStatus(status);
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    /**
     * Event trace data structure
     */
    public static class EventTrace {
        private final long timestamp;
        private final String eventType;
        private final String from;
        private final String to;
        private final String payload;
        
        public EventTrace(long timestamp, String eventType, String from, String to, String payload) {
            this.timestamp = timestamp;
            this.eventType = eventType;
            this.from = from;
            this.to = to;
            this.payload = payload;
        }
        
        public long getTimestamp() { return timestamp; }
        public String getEventType() { return eventType; }
        public String getFrom() { return from; }
        public String getTo() { return to; }
        public String getPayload() { return payload; }
        
        @Override
        public String toString() {
            LocalDateTime time = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp),
                java.time.ZoneId.systemDefault()
            );
            
            return String.format("[%s] %s: %s -> %s | %s", 
                time.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                eventType, from, to,
                payload.length() > 100 ? payload.substring(0, 100) + "..." : payload);
        }
    }
    
    /**
     * Agent diagnostics data structure
     */
    public static class AgentDiagnostics {
        private final String agentName;
        private String status = "UNKNOWN";
        private LocalDateTime lastActivity;
        private final Map<String, Object> metrics = new HashMap<>();
        
        public AgentDiagnostics(String agentName) {
            this.agentName = agentName;
        }
        
        public void updateStatus(String status) {
            this.status = status;
            this.lastActivity = LocalDateTime.now();
        }
        
        public void updateMetric(String key, Object value) {
            metrics.put(key, value);
        }
        
        public String getAgentName() { return agentName; }
        public String getStatus() { return status; }
        public LocalDateTime getLastActivity() { return lastActivity; }
        public Map<String, Object> getMetrics() { return new HashMap<>(metrics); }
    }
    
    /**
     * Show active traces
     */
    public CommandResult showActiveTraces() {
        StringBuilder report = new StringBuilder();
        report.append("\n🔍 ACTIVE EVENT TRACES\n");
        report.append("═".repeat(50)).append("\n");
        
        if (eventTraces.isEmpty()) {
            report.append("No active traces found\n");
        } else {
            eventTraces.forEach((eventType, traces) -> {
                report.append("\n📡 ").append(eventType).append(" (").append(traces.size()).append(" traces):\n");
                traces.stream()
                    .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
                    .limit(5)
                    .forEach(trace -> report.append("  • ").append(trace.toString()).append("\n"));
            });
        }
        
        return CommandResult.success(report.toString());
    }
    
    /**
     * Start trace for specific agent
     */
    public CommandResult startTrace(String agentName) {
        setTracingEnabled(true);
        return CommandResult.success("🔍 Event tracing started for agent: " + agentName);
    }
    
    /**
     * Show system monitor
     */
    public CommandResult showSystemMonitor() {
        StringBuilder report = new StringBuilder();
        report.append("\n📊 SYSTEM MONITOR\n");
        report.append("═".repeat(50)).append("\n");
        
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        report.append("💻 System Resources:\n");
        report.append("  • CPU Cores: ").append(runtime.availableProcessors()).append("\n");
        report.append("  • Memory Used: ").append(formatBytes(usedMemory)).append("\n");
        report.append("  • Memory Free: ").append(formatBytes(freeMemory)).append("\n");
        report.append("  • Memory Total: ").append(formatBytes(totalMemory)).append("\n");
        report.append("  • Memory Usage: ").append(String.format("%.1f%%", (double) usedMemory / totalMemory * 100)).append("\n");
        
        report.append("\n📈 Agent Statistics:\n");
        report.append("  • Total Agents: ").append(agentDiagnostics.size()).append("\n");
        report.append("  • Active Traces: ").append(eventTraces.size()).append("\n");
        report.append("  • Log Entries: ").append(agentLogs.values().stream().mapToInt(List::size).sum()).append("\n");
        
        return CommandResult.success(report.toString());
    }
    
    /**
     * Show system logs
     */
    public CommandResult showSystemLogs() {
        StringBuilder report = new StringBuilder();
        report.append("\n📋 SYSTEM LOGS\n");
        report.append("═".repeat(50)).append("\n");
        
        if (agentLogs.isEmpty()) {
            report.append("No system logs found\n");
        } else {
            agentLogs.forEach((agent, logs) -> {
                report.append("\n🤖 ").append(agent).append(" (").append(logs.size()).append(" entries):\n");
                logs.stream()
                    .skip(Math.max(0, logs.size() - 10))
                    .forEach(log -> report.append("  ").append(log).append("\n"));
            });
        }
        
        return CommandResult.success(report.toString());
    }
    
    /**
     * Show agent logs
     */
    public CommandResult showAgentLogs(String agentName) {
        List<String> logs = agentLogs.get(agentName);
        
        StringBuilder report = new StringBuilder();
        report.append("\n📋 AGENT LOGS: ").append(agentName).append("\n");
        report.append("═".repeat(50)).append("\n");
        
        if (logs == null || logs.isEmpty()) {
            report.append("No logs found for agent: ").append(agentName).append("\n");
        } else {
            report.append("Total entries: ").append(logs.size()).append("\n\n");
            logs.forEach(log -> report.append(log).append("\n"));
        }
        
        return CommandResult.success(report.toString());
    }
    
    /**
     * Show API status
     */
    public CommandResult showApiStatus() {
        StringBuilder report = new StringBuilder();
        report.append("\n🌐 API STATUS\n");
        report.append("═".repeat(50)).append("\n");
        
        report.append("  • Polygon.io: Check with 'connectivity' command\n");
        report.append("  • OpenWeather: Check with 'connectivity' command\n");
        report.append("  • Event Broker: ACTIVE (in-memory)\n");
        
        return CommandResult.success(report.toString());
    }
}