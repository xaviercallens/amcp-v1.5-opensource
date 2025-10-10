package io.amcp.connectors.ai.cache;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent caching layer for LLM responses.
 * Reduces redundant LLM calls by caching responses to disk and memory.
 * 
 * Features:
 * - Two-tier caching (memory + disk)
 * - Persistent storage across restarts
 * - Configurable TTL and size limits
 * - Automatic cleanup of stale entries
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class LLMResponseCache {
    
    private static final String DEFAULT_CACHE_DIR = ".amcp/llm-cache";
    private static final long DEFAULT_TTL_HOURS = 24;
    private static final int DEFAULT_MAX_MEMORY_ENTRIES = 500;
    
    private final Path cacheDirectory;
    private final long ttlMillis;
    private final int maxMemoryEntries;
    private final Map<String, CachedResponse> memoryCache;
    
    // Statistics
    private long memoryHits = 0;
    private long diskHits = 0;
    private long misses = 0;
    
    public LLMResponseCache() {
        this(DEFAULT_CACHE_DIR, DEFAULT_TTL_HOURS, DEFAULT_MAX_MEMORY_ENTRIES);
    }
    
    public LLMResponseCache(String cacheDir, long ttlHours, int maxMemoryEntries) {
        this.cacheDirectory = Paths.get(System.getProperty("user.home"), cacheDir);
        this.ttlMillis = ttlHours * 3600 * 1000;
        this.maxMemoryEntries = maxMemoryEntries;
        this.memoryCache = new ConcurrentHashMap<>();
        
        initializeCacheDirectory();
    }
    
    /**
     * Cached LLM response
     */
    public static class CachedResponse {
        private final String prompt;
        private final String response;
        private final String model;
        private final Map<String, Object> parameters;
        private final long timestamp;
        private final long expiryTime;
        
        public CachedResponse(String prompt, String response, String model,
                             Map<String, Object> parameters, long ttlMillis) {
            this.prompt = prompt;
            this.response = response;
            this.model = model;
            this.parameters = new HashMap<>(parameters);
            this.timestamp = System.currentTimeMillis();
            this.expiryTime = timestamp + ttlMillis;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
        
        public String getPrompt() { return prompt; }
        public String getResponse() { return response; }
        public String getModel() { return model; }
        public Map<String, Object> getParameters() { return parameters; }
        public long getTimestamp() { return timestamp; }
        public long getExpiryTime() { return expiryTime; }
    }
    
    /**
     * Initialize cache directory
     */
    private void initializeCacheDirectory() {
        try {
            Files.createDirectories(cacheDirectory);
        } catch (IOException e) {
            System.err.println("Failed to create cache directory: " + e.getMessage());
        }
    }
    
    /**
     * Get cached response for a prompt
     */
    public Optional<String> get(String prompt, String model, Map<String, Object> parameters) {
        String cacheKey = generateCacheKey(prompt, model, parameters);
        
        // Check memory cache first
        CachedResponse memoryEntry = memoryCache.get(cacheKey);
        if (memoryEntry != null && !memoryEntry.isExpired()) {
            memoryHits++;
            return Optional.of(memoryEntry.getResponse());
        }
        
        // Check disk cache
        Optional<CachedResponse> diskEntry = loadFromDisk(cacheKey);
        if (diskEntry.isPresent() && !diskEntry.get().isExpired()) {
            // Promote to memory cache
            memoryCache.put(cacheKey, diskEntry.get());
            diskHits++;
            return Optional.of(diskEntry.get().getResponse());
        }
        
        misses++;
        return Optional.empty();
    }
    
    /**
     * Put response in cache
     */
    public void put(String prompt, String response, String model, Map<String, Object> parameters) {
        String cacheKey = generateCacheKey(prompt, model, parameters);
        CachedResponse cachedResponse = new CachedResponse(prompt, response, model, parameters, ttlMillis);
        
        // Store in memory cache
        if (memoryCache.size() >= maxMemoryEntries) {
            evictOldestMemoryEntry();
        }
        memoryCache.put(cacheKey, cachedResponse);
        
        // Persist to disk asynchronously
        persistToDiskAsync(cacheKey, cachedResponse);
    }
    
    /**
     * Generate cache key from prompt, model, and parameters
     */
    private String generateCacheKey(String prompt, String model, Map<String, Object> parameters) {
        try {
            String combined = prompt + "|" + model + "|" + serializeParameters(parameters);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes("UTF-8"));
            return bytesToHex(hash);
        } catch (Exception e) {
            // Fallback to simple hash
            return String.valueOf((prompt + model).hashCode());
        }
    }
    
    /**
     * Serialize parameters to string
     */
    private String serializeParameters(Map<String, Object> parameters) {
        StringBuilder sb = new StringBuilder();
        parameters.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> sb.append(e.getKey()).append("=").append(e.getValue()).append(";"));
        return sb.toString();
    }
    
    /**
     * Convert bytes to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Load cached response from disk
     */
    private Optional<CachedResponse> loadFromDisk(String cacheKey) {
        Path cacheFile = cacheDirectory.resolve(cacheKey + ".cache");
        
        if (!Files.exists(cacheFile)) {
            return Optional.empty();
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(cacheFile)))) {
            
            String prompt = ois.readUTF();
            String response = ois.readUTF();
            String model = ois.readUTF();
            long timestamp = ois.readLong();
            long expiryTime = ois.readLong();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = (Map<String, Object>) ois.readObject();
            
            CachedResponse cachedResponse = new CachedResponse(
                prompt, response, model, parameters, expiryTime - timestamp
            );
            
            return Optional.of(cachedResponse);
            
        } catch (Exception e) {
            // Cache file corrupted or unreadable, delete it
            try {
                Files.deleteIfExists(cacheFile);
            } catch (IOException ex) {
                // Ignore
            }
            return Optional.empty();
        }
    }
    
    /**
     * Persist cached response to disk asynchronously
     */
    private void persistToDiskAsync(String cacheKey, CachedResponse response) {
        new Thread(() -> persistToDisk(cacheKey, response)).start();
    }
    
    /**
     * Persist cached response to disk
     */
    private void persistToDisk(String cacheKey, CachedResponse response) {
        Path cacheFile = cacheDirectory.resolve(cacheKey + ".cache");
        
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(cacheFile)))) {
            
            oos.writeUTF(response.getPrompt());
            oos.writeUTF(response.getResponse());
            oos.writeUTF(response.getModel());
            oos.writeLong(response.getTimestamp());
            oos.writeLong(response.getExpiryTime());
            oos.writeObject(new HashMap<>(response.getParameters()));
            
        } catch (IOException e) {
            System.err.println("Failed to persist cache entry: " + e.getMessage());
        }
    }
    
    /**
     * Evict oldest memory entry
     */
    private void evictOldestMemoryEntry() {
        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;
        
        for (Map.Entry<String, CachedResponse> entry : memoryCache.entrySet()) {
            if (entry.getValue().getTimestamp() < oldestTime) {
                oldestTime = entry.getValue().getTimestamp();
                oldestKey = entry.getKey();
            }
        }
        
        if (oldestKey != null) {
            memoryCache.remove(oldestKey);
        }
    }
    
    /**
     * Cleanup expired entries from disk
     */
    public void cleanupExpired() {
        try {
            Files.list(cacheDirectory)
                .filter(path -> path.toString().endsWith(".cache"))
                .forEach(path -> {
                    try {
                        String cacheKey = path.getFileName().toString().replace(".cache", "");
                        Optional<CachedResponse> response = loadFromDisk(cacheKey);
                        if (response.isEmpty() || response.get().isExpired()) {
                            Files.deleteIfExists(path);
                        }
                    } catch (IOException e) {
                        // Ignore
                    }
                });
        } catch (IOException e) {
            System.err.println("Failed to cleanup expired cache entries: " + e.getMessage());
        }
        
        // Cleanup memory cache
        memoryCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    /**
     * Clear all cache entries
     */
    public void clear() {
        memoryCache.clear();
        
        try {
            Files.list(cacheDirectory)
                .filter(path -> path.toString().endsWith(".cache"))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        // Ignore
                    }
                });
        } catch (IOException e) {
            System.err.println("Failed to clear cache: " + e.getMessage());
        }
        
        memoryHits = 0;
        diskHits = 0;
        misses = 0;
    }
    
    /**
     * Get cache statistics
     */
    public CacheStats getStats() {
        long totalHits = memoryHits + diskHits;
        long total = totalHits + misses;
        double hitRate = total == 0 ? 0.0 : (double) totalHits / total;
        
        return new CacheStats(
            memoryCache.size(),
            maxMemoryEntries,
            memoryHits,
            diskHits,
            misses,
            hitRate,
            getDiskCacheSize()
        );
    }
    
    /**
     * Get disk cache size
     */
    private long getDiskCacheSize() {
        try {
            return Files.list(cacheDirectory)
                .filter(path -> path.toString().endsWith(".cache"))
                .count();
        } catch (IOException e) {
            return 0;
        }
    }
    
    /**
     * Cache statistics
     */
    public static class CacheStats {
        private final int memorySize;
        private final int maxMemorySize;
        private final long memoryHits;
        private final long diskHits;
        private final long misses;
        private final double hitRate;
        private final long diskSize;
        
        public CacheStats(int memorySize, int maxMemorySize, long memoryHits,
                         long diskHits, long misses, double hitRate, long diskSize) {
            this.memorySize = memorySize;
            this.maxMemorySize = maxMemorySize;
            this.memoryHits = memoryHits;
            this.diskHits = diskHits;
            this.misses = misses;
            this.hitRate = hitRate;
            this.diskSize = diskSize;
        }
        
        public int getMemorySize() { return memorySize; }
        public int getMaxMemorySize() { return maxMemorySize; }
        public long getMemoryHits() { return memoryHits; }
        public long getDiskHits() { return diskHits; }
        public long getMisses() { return misses; }
        public double getHitRate() { return hitRate; }
        public long getDiskSize() { return diskSize; }
        
        @Override
        public String toString() {
            return String.format(
                "LLMCacheStats{memory=%d/%d, disk=%d, memHits=%d, diskHits=%d, misses=%d, hitRate=%.2f%%}",
                memorySize, maxMemorySize, diskSize, memoryHits, diskHits, misses, hitRate * 100
            );
        }
    }
}
