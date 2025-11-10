package io.amcp.examples;

import io.amcp.core.AbstractMobileAgent;
import io.amcp.core.Event;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * FileSystemAgent - demonstrates file system operations in AMCP.
 * 
 * Capabilities:
 * - List files in a directory
 * - Get file information
 * - Search for files by pattern
 * - Monitor directory changes (future)
 * 
 * Events:
 * - fs.list.request -> fs.list.response
 * - fs.info.request -> fs.info.response
 * - fs.search.request -> fs.search.response
 */
@ApplicationScoped
public class FileSystemAgent extends AbstractMobileAgent {

    private static final long MAX_FILE_SIZE = 10_000_000; // 10MB limit for info

    @Override
    public void onActivate() {
        super.onActivate();
        
        // Subscribe to file system events
        subscribe("fs.**");
        
        logMessage("📁 FileSystemAgent activated and ready!");
        logMessage("   Capabilities: list, info, search");
    }

    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            String topic = event.getTopic();
            logger.debug("FileSystemAgent handling: {}", topic);
            
            try {
                switch (topic) {
                    case "fs.list.request" -> handleListRequest(event);
                    case "fs.info.request" -> handleInfoRequest(event);
                    case "fs.search.request" -> handleSearchRequest(event);
                    default -> logger.debug("Unknown topic: {}", topic);
                }
            } catch (Exception e) {
                logger.error("Error handling {}: {}", topic, e.getMessage(), e);
                publishError(event, e.getMessage());
            }
        });
    }

    /**
     * Handles directory listing requests.
     * Payload: { "path": "/path/to/dir", "includeHidden": false }
     */
    private void handleListRequest(Event event) {
        @SuppressWarnings("unchecked")
        Map<String, Object> request = event.getPayload(Map.class);
        String pathStr = (String) request.get("path");
        boolean includeHidden = Boolean.TRUE.equals(request.get("includeHidden"));
        
        logMessage("📂 Listing directory: " + pathStr);
        
        Path path = Paths.get(pathStr);
        
        if (!Files.exists(path)) {
            publishError(event, "Path does not exist: " + pathStr);
            return;
        }
        
        if (!Files.isDirectory(path)) {
            publishError(event, "Path is not a directory: " + pathStr);
            return;
        }
        
        try {
            List<Map<String, Object>> files = Files.list(path)
                .filter(p -> includeHidden || !p.getFileName().toString().startsWith("."))
                .map(this::fileToMap)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("path", pathStr);
            response.put("count", files.size());
            response.put("files", files);
            
            publishEvent("fs.list.response", response);
            logMessage("✅ Listed " + files.size() + " files");
            
        } catch (Exception e) {
            publishError(event, "Error listing directory: " + e.getMessage());
        }
    }

    /**
     * Handles file info requests.
     * Payload: { "path": "/path/to/file" }
     */
    private void handleInfoRequest(Event event) {
        @SuppressWarnings("unchecked")
        Map<String, Object> request = event.getPayload(Map.class);
        String pathStr = (String) request.get("path");
        
        logMessage("ℹ️  Getting file info: " + pathStr);
        
        Path path = Paths.get(pathStr);
        
        if (!Files.exists(path)) {
            publishError(event, "File does not exist: " + pathStr);
            return;
        }
        
        try {
            Map<String, Object> info = fileToMap(path);
            
            // Add additional details
            if (Files.isRegularFile(path)) {
                long size = Files.size(path);
                info.put("readable", Files.isReadable(path));
                info.put("writable", Files.isWritable(path));
                info.put("executable", Files.isExecutable(path));
                
                // Add size in human-readable format
                info.put("sizeFormatted", formatFileSize(size));
            }
            
            publishEvent("fs.info.response", info);
            logMessage("✅ Retrieved file info");
            
        } catch (Exception e) {
            publishError(event, "Error getting file info: " + e.getMessage());
        }
    }

    /**
     * Handles file search requests.
     * Payload: { "path": "/path/to/search", "pattern": "*.java", "maxDepth": 3 }
     */
    private void handleSearchRequest(Event event) {
        @SuppressWarnings("unchecked")
        Map<String, Object> request = event.getPayload(Map.class);
        String pathStr = (String) request.get("path");
        String pattern = (String) request.getOrDefault("pattern", "*");
        int maxDepth = ((Number) request.getOrDefault("maxDepth", 3)).intValue();
        
        logMessage("🔍 Searching in: " + pathStr + " for pattern: " + pattern);
        
        Path path = Paths.get(pathStr);
        
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            publishError(event, "Invalid search path: " + pathStr);
            return;
        }
        
        try {
            // Convert glob pattern to regex
            String regex = pattern.replace(".", "\\.").replace("*", ".*").replace("?", ".");
            
            List<Map<String, Object>> results = Files.walk(path, maxDepth)
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().matches(regex))
                .limit(100) // Limit results
                .map(this::fileToMap)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("path", pathStr);
            response.put("pattern", pattern);
            response.put("count", results.size());
            response.put("results", results);
            response.put("limited", results.size() >= 100);
            
            publishEvent("fs.search.response", response);
            logMessage("✅ Found " + results.size() + " matching files");
            
        } catch (Exception e) {
            publishError(event, "Error searching files: " + e.getMessage());
        }
    }

    /**
     * Converts a Path to a Map with file information.
     */
    private Map<String, Object> fileToMap(Path path) {
        Map<String, Object> info = new HashMap<>();
        
        try {
            info.put("name", path.getFileName().toString());
            info.put("path", path.toString());
            info.put("absolutePath", path.toAbsolutePath().toString());
            
            if (Files.isDirectory(path)) {
                info.put("type", "directory");
                info.put("size", 0L);
            } else {
                info.put("type", "file");
                long size = Files.size(path);
                info.put("size", size);
            }
            
            info.put("lastModified", Files.getLastModifiedTime(path).toMillis());
            info.put("hidden", path.getFileName().toString().startsWith("."));
            
        } catch (Exception e) {
            logger.warn("Error getting file info for {}: {}", path, e.getMessage());
            info.put("error", e.getMessage());
        }
        
        return info;
    }

    /**
     * Formats file size in human-readable format.
     */
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    /**
     * Publishes an error response.
     */
    private void publishError(Event originalEvent, String error) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", error);
        errorResponse.put("success", false);
        
        String responseTopic = originalEvent.getTopic().replace(".request", ".error");
        publishEvent(responseTopic, errorResponse);
        
        logMessage("❌ Error: " + error);
    }

    @Override
    public void onDeactivate() {
        logMessage("👋 FileSystemAgent deactivating");
        super.onDeactivate();
    }
}
