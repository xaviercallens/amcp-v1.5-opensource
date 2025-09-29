package io.amcp.cli;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Command History Manager with persistent storage and search capabilities
 */
public class HistoryManager {
    
    private final String historyFilePath;
    private final List<HistoryEntry> history = new ArrayList<>();
    private final int maxHistorySize;
    
    public HistoryManager(String historyFilePath) {
        this(historyFilePath, 1000);
    }
    
    public HistoryManager(String historyFilePath, int maxHistorySize) {
        this.historyFilePath = historyFilePath;
        this.maxHistorySize = maxHistorySize;
    }
    
    /**
     * Add a command to history
     */
    public void addCommand(String command) {
        HistoryEntry entry = new HistoryEntry(command, LocalDateTime.now());
        history.add(entry);
        
        // Trim history if too large
        while (history.size() > maxHistorySize) {
            history.remove(0);
        }
    }
    
    /**
     * Get recent command history
     */
    public List<String> getRecentHistory(int count) {
        int start = Math.max(0, history.size() - count);
        return history.subList(start, history.size())
            .stream()
            .map(HistoryEntry::getCommand)
            .collect(Collectors.toList());
    }
    
    /**
     * Search command history
     */
    public List<HistoryEntry> searchHistory(String query) {
        return history.stream()
            .filter(entry -> entry.getCommand().toLowerCase().contains(query.toLowerCase()))
            .collect(Collectors.toList());
    }
    
    /**
     * Get command suggestions based on partial input
     */
    public List<String> getCommandSuggestions(String partial) {
        if (partial == null || partial.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        return history.stream()
            .map(HistoryEntry::getCommand)
            .filter(cmd -> cmd.toLowerCase().startsWith(partial.toLowerCase()))
            .distinct()
            .sorted((a, b) -> Integer.compare(a.length(), b.length())) // Prefer shorter matches
            .limit(10)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all history entries
     */
    public List<HistoryEntry> getAllHistory() {
        return new ArrayList<>(history);
    }
    
    /**
     * Clear history
     */
    public void clearHistory() {
        history.clear();
    }
    
    /**
     * Load history from file
     */
    public void loadHistory() {
        try {
            Path path = Paths.get(historyFilePath);
            if (!Files.exists(path)) {
                return;
            }
            
            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                
                try {
                    // Parse format: "timestamp|command"
                    String[] parts = line.split("\\|", 2);
                    if (parts.length == 2) {
                        LocalDateTime timestamp = LocalDateTime.parse(parts[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        String command = parts[1];
                        history.add(new HistoryEntry(command, timestamp));
                    } else {
                        // Legacy format - just command
                        history.add(new HistoryEntry(line.trim(), LocalDateTime.now()));
                    }
                } catch (Exception e) {
                    // Skip malformed entries
                    System.err.println("⚠️  Skipping malformed history entry: " + line);
                }
            }
            
            System.out.println("📚 Loaded " + history.size() + " commands from history");
            
        } catch (IOException e) {
            System.err.println("⚠️  Failed to load history: " + e.getMessage());
        }
    }
    
    /**
     * Save history to file
     */
    public void saveHistory() throws IOException {
        Path path = Paths.get(historyFilePath);
        
        // Create parent directories if needed
        Files.createDirectories(path.getParent());
        
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            for (HistoryEntry entry : history) {
                writer.write(entry.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                writer.write("|");
                writer.write(entry.getCommand());
                writer.newLine();
            }
        }
    }
    
    /**
     * Get history statistics
     */
    public HistoryStatistics getStatistics() {
        Map<String, Integer> commandCounts = new HashMap<>();
        
        for (HistoryEntry entry : history) {
            String command = entry.getCommand().split("\\s+")[0].toLowerCase();
            commandCounts.put(command, commandCounts.getOrDefault(command, 0) + 1);
        }
        
        return new HistoryStatistics(
            history.size(),
            commandCounts,
            history.isEmpty() ? null : history.get(0).getTimestamp(),
            history.isEmpty() ? null : history.get(history.size() - 1).getTimestamp()
        );
    }
    
    /**
     * History entry with timestamp
     */
    public static class HistoryEntry {
        private final String command;
        private final LocalDateTime timestamp;
        
        public HistoryEntry(String command, LocalDateTime timestamp) {
            this.command = command;
            this.timestamp = timestamp;
        }
        
        public String getCommand() {
            return command;
        }
        
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String toString() {
            return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " " + command;
        }
    }
    
    /**
     * History statistics
     */
    public static class HistoryStatistics {
        private final int totalCommands;
        private final Map<String, Integer> commandCounts;
        private final LocalDateTime firstCommand;
        private final LocalDateTime lastCommand;
        
        public HistoryStatistics(int totalCommands, Map<String, Integer> commandCounts, 
                               LocalDateTime firstCommand, LocalDateTime lastCommand) {
            this.totalCommands = totalCommands;
            this.commandCounts = commandCounts;
            this.firstCommand = firstCommand;
            this.lastCommand = lastCommand;
        }
        
        public int getTotalCommands() { return totalCommands; }
        public Map<String, Integer> getCommandCounts() { return commandCounts; }
        public LocalDateTime getFirstCommand() { return firstCommand; }
        public LocalDateTime getLastCommand() { return lastCommand; }
        
        public List<Map.Entry<String, Integer>> getTopCommands(int limit) {
            return commandCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
        }
    }
}