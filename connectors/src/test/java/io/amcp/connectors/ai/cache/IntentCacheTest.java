package io.amcp.connectors.ai.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Unit tests for IntentCache
 */
public class IntentCacheTest {
    
    private IntentCache cache;
    
    @BeforeEach
    public void setUp() {
        cache = new IntentCache(100, 1); // 100 entries, 1 minute TTL
    }
    
    @Test
    public void testCachePutAndGet() {
        String query = "What's the weather in Paris?";
        Map<String, Object> params = new HashMap<>();
        params.put("location", "Paris");
        
        IntentCache.CachedIntent intent = new IntentCache.CachedIntent(
            "weather", "WeatherAgent", 0.9, params, "Weather query detected"
        );
        
        cache.put(query, intent);
        
        Optional<IntentCache.CachedIntent> retrieved = cache.get(query);
        assertTrue(retrieved.isPresent());
        assertEquals("weather", retrieved.get().getIntent());
        assertEquals("WeatherAgent", retrieved.get().getTargetAgent());
        assertEquals(0.9, retrieved.get().getConfidence(), 0.01);
    }
    
    @Test
    public void testCacheMiss() {
        Optional<IntentCache.CachedIntent> retrieved = cache.get("Unknown query");
        assertFalse(retrieved.isPresent());
    }
    
    @Test
    public void testCacheNormalization() {
        String query1 = "What's the weather?";
        String query2 = "what's   the   weather?"; // Different spacing
        
        Map<String, Object> params = new HashMap<>();
        IntentCache.CachedIntent intent = new IntentCache.CachedIntent(
            "weather", "WeatherAgent", 0.9, params, "Test"
        );
        
        cache.put(query1, intent);
        
        // Should retrieve with normalized query
        Optional<IntentCache.CachedIntent> retrieved = cache.get(query2);
        assertTrue(retrieved.isPresent());
    }
    
    @Test
    public void testCacheStatistics() {
        String query = "Test query";
        Map<String, Object> params = new HashMap<>();
        IntentCache.CachedIntent intent = new IntentCache.CachedIntent(
            "test", "TestAgent", 0.8, params, "Test"
        );
        
        cache.put(query, intent);
        
        // Hit
        cache.get(query);
        
        // Miss
        cache.get("Unknown query");
        
        IntentCache.CacheStats stats = cache.getStats();
        assertEquals(1, stats.getHits());
        assertEquals(1, stats.getMisses());
        assertEquals(0.5, stats.getHitRate(), 0.01);
    }
    
    @Test
    public void testCacheClear() {
        String query = "Test query";
        Map<String, Object> params = new HashMap<>();
        IntentCache.CachedIntent intent = new IntentCache.CachedIntent(
            "test", "TestAgent", 0.8, params, "Test"
        );
        
        cache.put(query, intent);
        assertTrue(cache.get(query).isPresent());
        
        cache.clear();
        assertFalse(cache.get(query).isPresent());
        
        IntentCache.CacheStats stats = cache.getStats();
        assertEquals(0, stats.getCurrentSize());
    }
}
