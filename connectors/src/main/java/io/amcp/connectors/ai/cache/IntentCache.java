package io.amcp.connectors.ai.cache;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * High-performance intent caching system for LLM orchestration.
 * Caches common user intents to reduce LLM calls and improve response times.
 * 
 * Features:
 * - Thread-safe concurrent cache
 * - TTL-based expiration
 * - LRU eviction policy
 * - Cache statistics and monitoring
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class IntentCache {
    
    private static final int DEFAULT_MAX_SIZE = 1000;
    private static final long DEFAULT_TTL_MINUTES = 60;
    
    private final Map<String, CacheEntry> cache;
    private final int maxSize;
    private final long ttlMillis;
    
    // Statistics
    private long hits = 0;
    private long misses = 0;
    private long evictions = 0;
    
    public IntentCache() {
        this(DEFAULT_MAX_SIZE, DEFAULT_TTL_MINUTES);
    }
    
    public IntentCache(int maxSize, long ttlMinutes) {
        this.cache = new ConcurrentHashMap<>();
        this.maxSize = maxSize;
        this.ttlMillis = TimeUnit.MINUTES.toMillis(ttlMinutes);
    }
    
    /**
     * Cached intent analysis result
     */
    public static class CachedIntent {
        private final String intent;
        private final String targetAgent;
        private final double confidence;
        private final Map<String, Object> parameters;
        private final String reasoning;
        private final long timestamp;
        
        public CachedIntent(String intent, String targetAgent, double confidence,
                           Map<String, Object> parameters, String reasoning) {
            this.intent = intent;
            this.targetAgent = targetAgent;
            this.confidence = confidence;
            this.parameters = parameters;
            this.reasoning = reasoning;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getIntent() { return intent; }
        public String getTargetAgent() { return targetAgent; }
        public double getConfidence() { return confidence; }
        public Map<String, Object> getParameters() { return parameters; }
        public String getReasoning() { return reasoning; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * Internal cache entry with metadata
     */
    private static class CacheEntry {
        private final CachedIntent intent;
        private final long expiryTime;
        private long lastAccessTime;
        private int accessCount;
        
        public CacheEntry(CachedIntent intent, long ttlMillis) {
            this.intent = intent;
            this.expiryTime = System.currentTimeMillis() + ttlMillis;
            this.lastAccessTime = System.currentTimeMillis();
            this.accessCount = 0;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
        
        public void recordAccess() {
            this.lastAccessTime = System.currentTimeMillis();
            this.accessCount++;
        }
        
        public CachedIntent getIntent() { return intent; }
        public long getLastAccessTime() { return lastAccessTime; }
        public int getAccessCount() { return accessCount; }
    }
    
    /**
     * Get cached intent for a query
     */
    public Optional<CachedIntent> get(String query) {
        String key = normalizeQuery(query);
        CacheEntry entry = cache.get(key);
        
        if (entry == null) {
            misses++;
            return Optional.empty();
        }
        
        if (entry.isExpired()) {
            cache.remove(key);
            misses++;
            evictions++;
            return Optional.empty();
        }
        
        entry.recordAccess();
        hits++;
        return Optional.of(entry.getIntent());
    }
    
    /**
     * Put intent in cache
     */
    public void put(String query, CachedIntent intent) {
        String key = normalizeQuery(query);
        
        // Check size limit and evict if necessary
        if (cache.size() >= maxSize) {
            evictLRU();
        }
        
        CacheEntry entry = new CacheEntry(intent, ttlMillis);
        cache.put(key, entry);
    }
    
    /**
     * Normalize query for cache key consistency
     */
    private String normalizeQuery(String query) {
        return query.toLowerCase().trim().replaceAll("\\s+", " ");
    }
    
    /**
     * Evict least recently used entry
     */
    private void evictLRU() {
        String lruKey = null;
        long oldestAccess = Long.MAX_VALUE;
        
        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (entry.getValue().getLastAccessTime() < oldestAccess) {
                oldestAccess = entry.getValue().getLastAccessTime();
                lruKey = entry.getKey();
            }
        }
        
        if (lruKey != null) {
            cache.remove(lruKey);
            evictions++;
        }
    }
    
    /**
     * Clear expired entries
     */
    public void cleanupExpired() {
        cache.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired()) {
                evictions++;
                return true;
            }
            return false;
        });
    }
    
    /**
     * Clear all cache entries
     */
    public void clear() {
        cache.clear();
        hits = 0;
        misses = 0;
        evictions = 0;
    }
    
    /**
     * Get cache statistics
     */
    public CacheStats getStats() {
        return new CacheStats(
            cache.size(),
            maxSize,
            hits,
            misses,
            evictions,
            getHitRate()
        );
    }
    
    /**
     * Calculate cache hit rate
     */
    public double getHitRate() {
        long total = hits + misses;
        return total == 0 ? 0.0 : (double) hits / total;
    }
    
    /**
     * Cache statistics
     */
    public static class CacheStats {
        private final int currentSize;
        private final int maxSize;
        private final long hits;
        private final long misses;
        private final long evictions;
        private final double hitRate;
        
        public CacheStats(int currentSize, int maxSize, long hits, long misses, 
                         long evictions, double hitRate) {
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            this.hits = hits;
            this.misses = misses;
            this.evictions = evictions;
            this.hitRate = hitRate;
        }
        
        public int getCurrentSize() { return currentSize; }
        public int getMaxSize() { return maxSize; }
        public long getHits() { return hits; }
        public long getMisses() { return misses; }
        public long getEvictions() { return evictions; }
        public double getHitRate() { return hitRate; }
        
        @Override
        public String toString() {
            return String.format(
                "CacheStats{size=%d/%d, hits=%d, misses=%d, hitRate=%.2f%%, evictions=%d}",
                currentSize, maxSize, hits, misses, hitRate * 100, evictions
            );
        }
    }
}
