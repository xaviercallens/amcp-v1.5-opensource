package io.amcp.connectors.ai.fallback;

import io.amcp.connectors.ai.cache.LLMResponseCache;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for LLMFallbackSystem
 */
class LLMFallbackSystemTest {
    
    @TempDir
    Path tempDir;
    
    private LLMResponseCache mockCache;
    private LLMFallbackSystem fallbackSystem;
    
    @BeforeEach
    void setUp() {
        mockCache = new LLMResponseCache();
        String rulesDir = tempDir.resolve("fallback-rules").toString();
        fallbackSystem = new LLMFallbackSystem(mockCache, rulesDir, 70, 100);
    }
    
    @AfterEach
    void tearDown() {
        if (fallbackSystem != null) {
            fallbackSystem.cleanupRules();
        }
    }
    
    @Test
    @DisplayName("Should initialize with basic fallback rules")
    void testInitialization() {
        LLMFallbackSystem.FallbackStats stats = fallbackSystem.getStats();
        
        assertTrue(stats.getTotalRules() >= 3, "Should have at least 3 basic rules");
        assertEquals(0, stats.getFallbackAttempts(), "Should start with 0 attempts");
        assertEquals(0, stats.getSuccessfulFallbacks(), "Should start with 0 successful fallbacks");
    }
    
    @Test
    @DisplayName("Should provide fallback for coding-related prompts")
    void testCodingFallback() {
        String codingPrompt = "How do I create a Java function to calculate fibonacci numbers?";
        Map<String, Object> parameters = new HashMap<>();
        
        Optional<String> response = fallbackSystem.attemptFallback(codingPrompt, "qwen2.5:0.5b", parameters);
        
        assertTrue(response.isPresent(), "Should provide fallback for coding prompt");
        String responseText = response.get().toLowerCase();
        assertTrue(responseText.contains("coding") || 
                  responseText.contains("function") ||
                  responseText.contains("java") ||
                  responseText.contains("fibonacci") ||
                  responseText.contains("programming") ||
                  responseText.contains("method"), 
                  "Response should be relevant to coding: " + response.get());
        
        LLMFallbackSystem.FallbackStats stats = fallbackSystem.getStats();
        assertEquals(1, stats.getFallbackAttempts(), "Should record fallback attempt");
        assertEquals(1, stats.getSuccessfulFallbacks(), "Should record successful fallback");
    }
    
    @Test
    @DisplayName("Should provide fallback for help requests")
    void testHelpFallback() {
        String helpPrompt = "Can you help me understand how neural networks work?";
        Map<String, Object> parameters = new HashMap<>();
        
        Optional<String> response = fallbackSystem.attemptFallback(helpPrompt, "gemma:2b", parameters);
        
        assertTrue(response.isPresent(), "Should provide fallback for help prompt");
        assertTrue(response.get().toLowerCase().contains("help") || 
                  response.get().toLowerCase().contains("assist"), 
                  "Response should be relevant to help request");
    }
    
    @Test
    @DisplayName("Should provide fallback for explanation requests")
    void testExplanationFallback() {
        String explanationPrompt = "What is machine learning and how does it work?";
        Map<String, Object> parameters = new HashMap<>();
        
        Optional<String> response = fallbackSystem.attemptFallback(explanationPrompt, "qwen2:1.5b", parameters);
        
        assertTrue(response.isPresent(), "Should provide fallback for explanation prompt");
        assertTrue(response.get().toLowerCase().contains("explain") || 
                  response.get().toLowerCase().contains("topic"), 
                  "Response should be relevant to explanation request");
    }
    
    @Test
    @DisplayName("Should handle low-confidence prompts gracefully")
    void testLowConfidencePrompt() {
        String vaguePr = "xyz abc def";
        Map<String, Object> parameters = new HashMap<>();
        
        Optional<String> response = fallbackSystem.attemptFallback(vaguePr, "qwen2.5:0.5b", parameters);
        
        // Should either provide a generic response or no response
        if (response.isPresent()) {
            assertFalse(response.get().isEmpty(), "Response should not be empty if provided");
        }
    }
    
    @Test
    @DisplayName("Should learn from successful LLM interactions")
    void testLearningFromSuccessfulInteractions() {
        String prompt = "How to implement a binary search algorithm in Java?";
        String response = "Here's how to implement binary search in Java: [implementation details]";
        Map<String, Object> parameters = new HashMap<>();
        
        LLMFallbackSystem.FallbackStats initialStats = fallbackSystem.getStats();
        
        fallbackSystem.learnFromResponse(prompt, response, "qwen2.5:0.5b", parameters);
        
        LLMFallbackSystem.FallbackStats updatedStats = fallbackSystem.getStats();
        assertEquals(initialStats.getRuleLearningEvents() + 1, updatedStats.getRuleLearningEvents(),
                    "Should record rule learning event");
    }
    
    @Test
    @DisplayName("Should extract keywords correctly")
    void testKeywordExtraction() {
        String prompt = "How do I create a REST API using Spring Boot framework?";
        Map<String, Object> parameters = new HashMap<>();
        
        // Learn from this interaction to test keyword extraction
        fallbackSystem.learnFromResponse(prompt, "Sample response", "qwen2:1.5b", parameters);
        
        // Now test fallback with similar keywords
        String similarPrompt = "Can you help me build a REST API with Spring Boot?";
        Optional<String> response = fallbackSystem.attemptFallback(similarPrompt, "qwen2:1.5b", parameters);
        
        assertTrue(response.isPresent(), "Should provide fallback for similar prompt");
    }
    
    @Test
    @DisplayName("Should handle pattern matching correctly")
    void testPatternMatching() {
        // Test question pattern
        String questionPrompt = "What is the difference between ArrayList and LinkedList?";
        Map<String, Object> parameters = new HashMap<>();
        
        Optional<String> response = fallbackSystem.attemptFallback(questionPrompt, "gemma:2b", parameters);
        assertTrue(response.isPresent(), "Should handle question patterns");
        
        // Test command pattern
        String commandPrompt = "Please explain the concept of polymorphism in OOP";
        response = fallbackSystem.attemptFallback(commandPrompt, "gemma:2b", parameters);
        assertTrue(response.isPresent(), "Should handle command patterns");
    }
    
    @Test
    @DisplayName("Should categorize prompts correctly")
    void testPromptCategorization() {
        Map<String, Object> parameters = new HashMap<>();
        
        // Test different categories
        String[] codingPrompts = {
            "How to write a function in Python?",
            "Explain object-oriented programming concepts",
            "What is the difference between class and method?"
        };
        
        String[] helpPrompts = {
            "Can you help me with my homework?",
            "I need assistance with debugging",
            "Please support me in understanding this concept"
        };
        
        String[] questionPrompts = {
            "What is artificial intelligence?",
            "How does machine learning work?",
            "Why is software testing important?"
        };
        
        // Test coding prompts
        for (String prompt : codingPrompts) {
            Optional<String> response = fallbackSystem.attemptFallback(prompt, "qwen2.5:0.5b", parameters);
            assertTrue(response.isPresent(), "Should provide fallback for coding prompt: " + prompt);
        }
        
        // Test help prompts
        for (String prompt : helpPrompts) {
            Optional<String> response = fallbackSystem.attemptFallback(prompt, "gemma:2b", parameters);
            assertTrue(response.isPresent(), "Should provide fallback for help prompt: " + prompt);
        }
        
        // Test question prompts
        for (String prompt : questionPrompts) {
            Optional<String> response = fallbackSystem.attemptFallback(prompt, "qwen2:1.5b", parameters);
            assertTrue(response.isPresent(), "Should provide fallback for question prompt: " + prompt);
        }
    }
    
    @Test
    @DisplayName("Should maintain statistics correctly")
    void testStatisticsTracking() {
        Map<String, Object> parameters = new HashMap<>();
        
        // Initial stats
        LLMFallbackSystem.FallbackStats initialStats = fallbackSystem.getStats();
        long initialAttempts = initialStats.getFallbackAttempts();
        long initialSuccessful = initialStats.getSuccessfulFallbacks();
        
        // Successful fallback
        String goodPrompt = "How to create a Java class?";
        Optional<String> response1 = fallbackSystem.attemptFallback(goodPrompt, "qwen2.5:0.5b", parameters);
        assertTrue(response1.isPresent());
        
        // Another successful fallback
        String anotherPrompt = "Can you help me with Python programming?";
        Optional<String> response2 = fallbackSystem.attemptFallback(anotherPrompt, "gemma:2b", parameters);
        assertTrue(response2.isPresent());
        
        // Check updated stats
        LLMFallbackSystem.FallbackStats updatedStats = fallbackSystem.getStats();
        assertEquals(initialAttempts + 2, updatedStats.getFallbackAttempts(), 
                    "Should track all fallback attempts");
        assertEquals(initialSuccessful + 2, updatedStats.getSuccessfulFallbacks(), 
                    "Should track successful fallbacks");
        
        // Calculate success rate
        double expectedSuccessRate = (double) updatedStats.getSuccessfulFallbacks() / updatedStats.getFallbackAttempts();
        assertEquals(expectedSuccessRate, updatedStats.getSuccessRate(), 0.01, 
                    "Should calculate success rate correctly");
    }
    
    @Test
    @DisplayName("Should handle rule persistence and loading")
    void testRulePersistence() {
        String prompt = "How to implement quicksort algorithm?";
        String response = "Here's a quicksort implementation...";
        Map<String, Object> parameters = new HashMap<>();
        
        // Learn from interaction
        fallbackSystem.learnFromResponse(prompt, response, "qwen2:1.5b", parameters);
        
        // Create new fallback system instance (simulating restart)
        LLMFallbackSystem newFallbackSystem = new LLMFallbackSystem(
            mockCache, tempDir.resolve("fallback-rules").toString(), 70, 100);
        
        // Should be able to provide fallback using persisted rules
        String similarPrompt = "Can you show me how to implement quicksort?";
        Optional<String> fallbackResponse = newFallbackSystem.attemptFallback(similarPrompt, "qwen2:1.5b", parameters);
        
        assertTrue(fallbackResponse.isPresent(), "Should use persisted rules for fallback");
    }
    
    @Test
    @DisplayName("Should handle rule cleanup correctly")
    void testRuleCleanup() {
        LLMFallbackSystem.FallbackStats initialStats = fallbackSystem.getStats();
        int initialRules = initialStats.getTotalRules();
        
        // Cleanup should not remove recently used rules
        fallbackSystem.cleanupRules();
        
        LLMFallbackSystem.FallbackStats afterCleanupStats = fallbackSystem.getStats();
        assertTrue(afterCleanupStats.getTotalRules() <= initialRules, 
                  "Cleanup should not increase rule count");
    }
    
    @Test
    @DisplayName("Should generate appropriate responses for different contexts")
    void testContextualResponses() {
        Map<String, Object> parameters = new HashMap<>();
        
        // Technical context
        String technicalPrompt = "Explain microservices architecture patterns";
        Optional<String> techResponse = fallbackSystem.attemptFallback(technicalPrompt, "qwen2.5:0.5b", parameters);
        assertTrue(techResponse.isPresent());
        
        // Educational context
        String educationalPrompt = "What are the fundamentals of computer science?";
        Optional<String> eduResponse = fallbackSystem.attemptFallback(educationalPrompt, "gemma:2b", parameters);
        assertTrue(eduResponse.isPresent());
        
        // Problem-solving context
        String problemPrompt = "How do I debug a null pointer exception?";
        Optional<String> problemResponse = fallbackSystem.attemptFallback(problemPrompt, "qwen2:1.5b", parameters);
        assertTrue(problemResponse.isPresent());
        
        // All responses should be different and contextually appropriate
        assertNotEquals(techResponse.get(), eduResponse.get(), 
                       "Different contexts should generate different responses");
        assertNotEquals(techResponse.get(), problemResponse.get(), 
                       "Different contexts should generate different responses");
    }
    
    @Test
    @DisplayName("Should handle edge cases gracefully")
    void testEdgeCases() {
        Map<String, Object> parameters = new HashMap<>();
        
        // Empty prompt - should handle gracefully
        assertDoesNotThrow(() -> {
            fallbackSystem.attemptFallback("", "qwen2.5:0.5b", parameters);
        }, "Should handle empty prompt without crashing");
        
        // Very long prompt - should handle without crashing
        String longPrompt = "a".repeat(10000);
        assertDoesNotThrow(() -> {
            fallbackSystem.attemptFallback(longPrompt, "gemma:2b", parameters);
        }, "Should handle very long prompt without crashing");
        
        // Special characters
        String specialPrompt = "How to handle @#$%^&*() characters in programming?";
        Optional<String> specialResponse = fallbackSystem.attemptFallback(specialPrompt, "qwen2:1.5b", parameters);
        assertTrue(specialResponse.isPresent(), "Should handle special characters");
        
        // Non-English characters (basic test) - should handle without crashing
        String unicodePrompt = "Comment créer une fonction en Java? 如何创建Java函数？";
        assertDoesNotThrow(() -> {
            fallbackSystem.attemptFallback(unicodePrompt, "qwen2.5:0.5b", parameters);
        }, "Should handle unicode characters without crashing");
    }
    
    @Test
    @DisplayName("Should integrate well with caching system")
    void testCacheIntegration() {
        Map<String, Object> parameters = new HashMap<>();
        String prompt = "How to use Spring Boot annotations?";
        String cachedResponse = "Spring Boot annotations are used for...";
        
        // Pre-populate cache
        mockCache.put(prompt, cachedResponse, "qwen2.5:0.5b", parameters);
        
        // Learn from this cached interaction
        fallbackSystem.learnFromResponse(prompt, cachedResponse, "qwen2.5:0.5b", parameters);
        
        // Test fallback with similar prompt
        String similarPrompt = "Can you explain Spring Boot annotations usage?";
        Optional<String> fallbackResponse = fallbackSystem.attemptFallback(similarPrompt, "qwen2.5:0.5b", parameters);
        
        assertTrue(fallbackResponse.isPresent(), "Should provide fallback based on learned patterns");
        
        LLMFallbackSystem.FallbackStats stats = fallbackSystem.getStats();
        assertTrue(stats.getRuleLearningEvents() > 0, "Should have learned from cached interactions");
    }
}
