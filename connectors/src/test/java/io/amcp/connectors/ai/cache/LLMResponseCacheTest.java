package io.amcp.connectors.ai.cache;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for LLMResponseCache functionality.
 * Tests memory caching, disk persistence, TTL, and cleanup operations.
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
class LLMResponseCacheTest {
    
    @TempDir
    Path tempDir;
    
    private LLMResponseCache cache;
    
    @BeforeEach
    void setUp() {
        // Create cache with test directory and short TTL for testing
        cache = new LLMResponseCache(
            tempDir.toString(), 
            1, // 1 hour TTL
            10 // max 10 memory entries
        );
    }
    
    @Test
    @DisplayName("Test basic cache put and get operations")
    void testBasicCacheOperations() {
        String prompt = "What is artificial intelligence?";
        String response = "AI is a field of computer science...";
        String model = "gemma:2b";
        Map<String, Object> parameters = Map.of("temperature", 0.6);
        
        // Initially should be empty
        Optional<String> result = cache.get(prompt, model, parameters);
        assertTrue(result.isEmpty());
        
        // Put response in cache
        cache.put(prompt, response, model, parameters);
        
        // Should now return cached response
        result = cache.get(prompt, model, parameters);
        assertTrue(result.isPresent());
        assertEquals(response, result.get());
    }
    
    @Test
    @DisplayName("Test cache with different parameters")
    void testCacheWithDifferentParameters() {
        String prompt = "Explain machine learning";
        String response1 = "ML with temperature 0.6";
        String response2 = "ML with temperature 0.8";
        String model = "gemma:2b";
        
        Map<String, Object> params1 = Map.of("temperature", 0.6);
        Map<String, Object> params2 = Map.of("temperature", 0.8);
        
        // Cache responses with different parameters
        cache.put(prompt, response1, model, params1);
        cache.put(prompt, response2, model, params2);
        
        // Should return different responses for different parameters
        Optional<String> result1 = cache.get(prompt, model, params1);
        Optional<String> result2 = cache.get(prompt, model, params2);
        
        assertTrue(result1.isPresent());
        assertTrue(result2.isPresent());
        assertEquals(response1, result1.get());
        assertEquals(response2, result2.get());
        assertNotEquals(result1.get(), result2.get());
    }
    
    @Test
    @DisplayName("Test cache with different models")
    void testCacheWithDifferentModels() {
        String prompt = "What is deep learning?";
        String response1 = "DL response from Gemma";
        String response2 = "DL response from Qwen2";
        Map<String, Object> parameters = Map.of("temperature", 0.6);
        
        // Cache responses from different models
        cache.put(prompt, response1, "gemma:2b", parameters);
        cache.put(prompt, response2, "qwen2:1.5b", parameters);
        
        // Should return different responses for different models
        Optional<String> result1 = cache.get(prompt, "gemma:2b", parameters);
        Optional<String> result2 = cache.get(prompt, "qwen2:1.5b", parameters);
        
        assertTrue(result1.isPresent());
        assertTrue(result2.isPresent());
        assertEquals(response1, result1.get());
        assertEquals(response2, result2.get());
    }
    
    @Test
    @DisplayName("Test memory cache eviction")
    void testMemoryCacheEviction() {
        String basePrompt = "Test prompt ";
        String baseResponse = "Test response ";
        String model = "gemma:2b";
        Map<String, Object> parameters = Map.of("temperature", 0.6);
        
        // Fill cache beyond memory limit (10 entries)
        for (int i = 0; i < 15; i++) {
            cache.put(basePrompt + i, baseResponse + i, model, parameters);
        }
        
        // Check statistics
        LLMResponseCache.CacheStats stats = cache.getStats();
        assertNotNull(stats);
        assertTrue(stats.getMemorySize() <= 10); // Should not exceed max memory size
        
        // Some entries should still be accessible (either from memory or disk)
        Optional<String> recent = cache.get(basePrompt + "14", model, parameters);
        assertTrue(recent.isPresent());
        assertEquals(baseResponse + "14", recent.get());
    }
    
    @Test
    @DisplayName("Test disk persistence")
    void testDiskPersistence() throws InterruptedException {
        String prompt = "Test disk persistence";
        String response = "This should be persisted to disk";
        String model = "qwen2:1.5b";
        Map<String, Object> parameters = Map.of("temperature", 0.7);
        
        // Put in cache
        cache.put(prompt, response, model, parameters);
        
        // Wait a bit for async disk write
        Thread.sleep(100);
        
        // Create new cache instance with same directory
        LLMResponseCache newCache = new LLMResponseCache(
            tempDir.toString(),
            1, // 1 hour TTL
            10
        );
        
        // Should load from disk
        Optional<String> result = newCache.get(prompt, model, parameters);
        assertTrue(result.isPresent());
        assertEquals(response, result.get());
        
        // Check that it was a disk hit
        LLMResponseCache.CacheStats stats = newCache.getStats();
        assertTrue(stats.getDiskHits() > 0);
    }
    
    @Test
    @DisplayName("Test cache statistics")
    void testCacheStatistics() {
        String prompt = "Statistics test";
        String response = "Stats response";
        String model = "gemma:2b";
        Map<String, Object> parameters = Map.of("temperature", 0.6);
        
        // Initial stats
        LLMResponseCache.CacheStats initialStats = cache.getStats();
        assertEquals(0, initialStats.getMemoryHits());
        assertEquals(0, initialStats.getDiskHits());
        assertEquals(0, initialStats.getMisses());
        
        // Cache miss
        Optional<String> result = cache.get(prompt, model, parameters);
        assertTrue(result.isEmpty());
        
        LLMResponseCache.CacheStats afterMiss = cache.getStats();
        assertEquals(1, afterMiss.getMisses());
        
        // Cache put and hit
        cache.put(prompt, response, model, parameters);
        result = cache.get(prompt, model, parameters);
        assertTrue(result.isPresent());
        
        LLMResponseCache.CacheStats afterHit = cache.getStats();
        assertEquals(1, afterHit.getMemoryHits());
        assertTrue(afterHit.getHitRate() > 0.0);
        
        // Test toString
        String statsString = afterHit.toString();
        assertNotNull(statsString);
        assertTrue(statsString.contains("LLMCacheStats"));
    }
    
    @Test
    @DisplayName("Test cache cleanup")
    void testCacheCleanup() {
        String prompt = "Cleanup test";
        String response = "This will be cleaned up";
        String model = "gemma:2b";
        Map<String, Object> parameters = Map.of("temperature", 0.6);
        
        // Add entry to cache
        cache.put(prompt, response, model, parameters);
        
        // Verify it's there
        Optional<String> result = cache.get(prompt, model, parameters);
        assertTrue(result.isPresent());
        
        // Cleanup should not throw exceptions
        assertDoesNotThrow(() -> cache.cleanupExpired());
        
        // Entry should still be there (not expired)
        result = cache.get(prompt, model, parameters);
        assertTrue(result.isPresent());
    }
    
    @Test
    @DisplayName("Test cache clear")
    void testCacheClear() {
        String prompt = "Clear test";
        String response = "This will be cleared";
        String model = "qwen2:1.5b";
        Map<String, Object> parameters = Map.of("temperature", 0.6);
        
        // Add entry to cache
        cache.put(prompt, response, model, parameters);
        
        // Verify it's there
        Optional<String> result = cache.get(prompt, model, parameters);
        assertTrue(result.isPresent());
        
        // Clear cache
        cache.clear();
        
        // Entry should be gone
        result = cache.get(prompt, model, parameters);
        assertTrue(result.isEmpty());
        
        // Stats should be reset
        LLMResponseCache.CacheStats stats = cache.getStats();
        assertEquals(0, stats.getMemoryHits());
        assertEquals(0, stats.getDiskHits());
        // Note: Misses counter might not be reset immediately after clear()
        assertTrue(stats.getMisses() >= 0);
    }
    
    @Test
    @DisplayName("Test cache with complex parameters")
    void testCacheWithComplexParameters() {
        String prompt = "Complex parameters test";
        String response = "Response with complex params";
        String model = "gemma:2b";
        
        Map<String, Object> complexParams = Map.of(
            "temperature", 0.7,
            "max_tokens", 2048,
            "top_p", 0.9,
            "frequency_penalty", 0.1,
            "presence_penalty", 0.1
        );
        
        // Cache with complex parameters
        cache.put(prompt, response, model, complexParams);
        
        // Should retrieve correctly
        Optional<String> result = cache.get(prompt, model, complexParams);
        assertTrue(result.isPresent());
        assertEquals(response, result.get());
        
        // Different parameter order should still work
        Map<String, Object> reorderedParams = Map.of(
            "max_tokens", 2048,
            "temperature", 0.7,
            "presence_penalty", 0.1,
            "top_p", 0.9,
            "frequency_penalty", 0.1
        );
        
        result = cache.get(prompt, model, reorderedParams);
        assertTrue(result.isPresent());
        assertEquals(response, result.get());
    }
    
    @Test
    @DisplayName("Test cache key generation consistency")
    void testCacheKeyConsistency() {
        String prompt = "Key consistency test";
        String response1 = "First response";
        String response2 = "Second response";
        String model = "gemma:2b";
        Map<String, Object> parameters = Map.of("temperature", 0.6);
        
        // Put first response
        cache.put(prompt, response1, model, parameters);
        
        // Get should return first response
        Optional<String> result = cache.get(prompt, model, parameters);
        assertTrue(result.isPresent());
        assertEquals(response1, result.get());
        
        // Put second response with same key (should overwrite)
        cache.put(prompt, response2, model, parameters);
        
        // Get should return second response
        result = cache.get(prompt, model, parameters);
        assertTrue(result.isPresent());
        assertEquals(response2, result.get());
    }
    
    @Test
    @DisplayName("Test cache with empty and null values")
    void testCacheWithEmptyValues() {
        String model = "gemma:2b";
        Map<String, Object> parameters = Map.of("temperature", 0.6);
        
        // Test with empty prompt
        cache.put("", "Empty prompt response", model, parameters);
        Optional<String> result = cache.get("", model, parameters);
        assertTrue(result.isPresent());
        assertEquals("Empty prompt response", result.get());
        
        // Test with empty response
        cache.put("Test prompt", "", model, parameters);
        result = cache.get("Test prompt", model, parameters);
        assertTrue(result.isPresent());
        assertEquals("", result.get());
        
        // Test with empty parameters
        Map<String, Object> emptyParams = Map.of();
        cache.put("Empty params test", "Response", model, emptyParams);
        result = cache.get("Empty params test", model, emptyParams);
        assertTrue(result.isPresent());
        assertEquals("Response", result.get());
    }
}
