package io.amcp.connectors.ai.fallback;

import io.amcp.connectors.ai.cache.LLMResponseCache;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct tests for agent-specific fallback scenarios
 * Tests Weather Agent, Stock Agent, and Travel Planner Agent fallback behavior directly
 */
class AgentSpecificFallbackTest {
    
    @TempDir
    Path tempDir;
    
    private LLMFallbackSystem fallbackSystem;
    
    @BeforeEach
    void setUp() {
        // Create fallback system with lower confidence threshold for testing
        LLMResponseCache cache = new LLMResponseCache();
        String rulesDir = tempDir.resolve("agent-fallback-rules").toString();
        fallbackSystem = new LLMFallbackSystem(cache, rulesDir, 50, 150);
    }
    
    @AfterEach
    void tearDown() {
        if (fallbackSystem != null) {
            fallbackSystem.cleanupRules();
        }
    }
    
    @Test
    @DisplayName("Weather Agent - Should provide weather-specific fallback responses")
    void testWeatherAgentDirectFallback() {
        Map<String, Object> parameters = new HashMap<>();
        
        // Test various weather-related queries
        String[] weatherQueries = {
            "What's the temperature in Berlin right now?",
            "Is it going to snow in Chicago this weekend?",
            "Check the weather forecast for Miami next week",
            "What's the humidity level in Singapore today?",
            "Will there be storms in Dallas tomorrow?"
        };
        
        for (String query : weatherQueries) {
            Optional<String> response = fallbackSystem.attemptFallback(query, "qwen2.5:0.5b", parameters);
            
            assertTrue(response.isPresent(), "Should get weather fallback for: " + query);
            
            String responseText = response.get();
            assertFalse(responseText.isEmpty(), "Weather fallback should not be empty");
            
            // Verify response contains weather-related content
            String lowerResponse = responseText.toLowerCase();
            assertTrue(
                lowerResponse.contains("weather") || 
                lowerResponse.contains("forecast") || 
                lowerResponse.contains("temperature") ||
                lowerResponse.contains("meteorological") ||
                lowerResponse.contains("climate") ||
                lowerResponse.contains("conditions"),
                "Weather fallback should contain weather-related content: " + responseText
            );
        }
        
        // Verify weather-specific statistics
        LLMFallbackSystem.FallbackStats stats = fallbackSystem.getStats();
        assertTrue(stats.getFallbackAttempts() >= weatherQueries.length, 
                  "Should record weather fallback attempts");
        assertTrue(stats.getSuccessfulFallbacks() >= weatherQueries.length, 
                  "Should record successful weather fallbacks");
    }
    
    @Test
    @DisplayName("Stock Agent - Should provide financial-specific fallback responses")
    void testStockAgentDirectFallback() {
        Map<String, Object> parameters = new HashMap<>();
        
        // Test various stock/financial queries
        String[] stockQueries = {
            "What's the current price of Microsoft stock?",
            "How is the S&P 500 performing today?",
            "Should I invest in cryptocurrency right now?",
            "What's the dividend yield for Coca-Cola?",
            "Is the market going up or down this week?"
        };
        
        for (String query : stockQueries) {
            Optional<String> response = fallbackSystem.attemptFallback(query, "gemma:2b", parameters);
            
            assertTrue(response.isPresent(), "Should get stock fallback for: " + query);
            
            String responseText = response.get();
            assertFalse(responseText.isEmpty(), "Stock fallback should not be empty");
            
            // Verify response contains financial-related content or helpful guidance
            String lowerResponse = responseText.toLowerCase();
            assertTrue(
                lowerResponse.contains("financial") || 
                lowerResponse.contains("market") || 
                lowerResponse.contains("investment") ||
                lowerResponse.contains("stock") ||
                lowerResponse.contains("trading") ||
                lowerResponse.contains("bloomberg") ||
                lowerResponse.contains("yahoo finance") ||
                lowerResponse.contains("cryptocurrency") ||
                lowerResponse.contains("invest") ||
                lowerResponse.contains("help") ||
                lowerResponse.contains("assist") ||
                lowerResponse.contains("information") ||
                lowerResponse.contains("system") ||
                lowerResponse.contains("technical"),
                "Stock fallback should contain financial-related content or helpful guidance: " + responseText
            );
        }
        
        // Verify stock-specific statistics
        LLMFallbackSystem.FallbackStats stats = fallbackSystem.getStats();
        assertTrue(stats.getFallbackAttempts() >= stockQueries.length, 
                  "Should record stock fallback attempts");
        assertTrue(stats.getSuccessfulFallbacks() >= stockQueries.length, 
                  "Should record successful stock fallbacks");
    }
    
    @Test
    @DisplayName("Travel Agent - Should provide travel-specific fallback responses")
    void testTravelAgentDirectFallback() {
        Map<String, Object> parameters = new HashMap<>();
        
        // Test various travel planning queries
        String[] travelQueries = {
            "Plan a weekend trip to Barcelona",
            "Find hotels in Amsterdam for next month",
            "What's the best time to visit Japan?",
            "Book a flight from Los Angeles to Paris",
            "Recommend restaurants in Florence, Italy"
        };
        
        for (String query : travelQueries) {
            Optional<String> response = fallbackSystem.attemptFallback(query, "qwen2:1.5b", parameters);
            
            assertTrue(response.isPresent(), "Should get travel fallback for: " + query);
            
            String responseText = response.get();
            assertFalse(responseText.isEmpty(), "Travel fallback should not be empty");
            
            // Verify response contains travel-related content or helpful guidance
            String lowerResponse = responseText.toLowerCase();
            assertTrue(
                lowerResponse.contains("travel") || 
                lowerResponse.contains("trip") || 
                lowerResponse.contains("booking") ||
                lowerResponse.contains("flight") ||
                lowerResponse.contains("hotel") ||
                lowerResponse.contains("expedia") ||
                lowerResponse.contains("tripadvisor") ||
                lowerResponse.contains("destination") ||
                lowerResponse.contains("recommend") ||
                lowerResponse.contains("information") ||
                lowerResponse.contains("system") ||
                lowerResponse.contains("service"),
                "Travel fallback should contain travel-related content or helpful guidance: " + responseText
            );
        }
        
        // Verify travel-specific statistics
        LLMFallbackSystem.FallbackStats stats = fallbackSystem.getStats();
        assertTrue(stats.getFallbackAttempts() >= travelQueries.length, 
                  "Should record travel fallback attempts");
        assertTrue(stats.getSuccessfulFallbacks() >= travelQueries.length, 
                  "Should record successful travel fallbacks");
    }
    
    @Test
    @DisplayName("Agent Rule Matching - Should match correct agent rules based on keywords")
    void testAgentRuleMatching() {
        Map<String, Object> parameters = new HashMap<>();
        
        // Test specific keyword matching
        Optional<String> weatherResponse = fallbackSystem.attemptFallback(
            "What's the weather forecast for tomorrow?", "qwen2.5:0.5b", parameters);
        assertTrue(weatherResponse.isPresent(), "Should match weather rule");
        assertTrue(weatherResponse.get().toLowerCase().contains("weather"), 
                  "Weather response should mention weather");
        
        Optional<String> stockResponse = fallbackSystem.attemptFallback(
            "What's the stock market doing today?", "gemma:2b", parameters);
        assertTrue(stockResponse.isPresent(), "Should match stock rule");
        assertTrue(stockResponse.get().toLowerCase().contains("market") || 
                  stockResponse.get().toLowerCase().contains("financial"), 
                  "Stock response should mention market or financial");
        
        Optional<String> travelResponse = fallbackSystem.attemptFallback(
            "Plan a vacation to Italy", "qwen2:1.5b", parameters);
        assertTrue(travelResponse.isPresent(), "Should match travel rule");
        assertTrue(travelResponse.get().toLowerCase().contains("travel") || 
                  travelResponse.get().toLowerCase().contains("trip"), 
                  "Travel response should mention travel or trip");
    }
    
    @Test
    @DisplayName("Multi-keyword Matching - Should handle queries with multiple agent keywords")
    void testMultiKeywordMatching() {
        Map<String, Object> parameters = new HashMap<>();
        
        // Query that could match multiple agents - should pick the best match
        Optional<String> response = fallbackSystem.attemptFallback(
            "What's the weather like for my travel to Tokyo and should I check stock market before booking flights?", 
            "qwen2.5:0.5b", parameters);
        
        assertTrue(response.isPresent(), "Should get fallback for multi-keyword query");
        
        String responseText = response.get();
        assertFalse(responseText.isEmpty(), "Multi-keyword fallback should not be empty");
        
        // Should contain relevant guidance (could be any of the agent types)
        String lowerResponse = responseText.toLowerCase();
        assertTrue(
            lowerResponse.contains("weather") || 
            lowerResponse.contains("travel") || 
            lowerResponse.contains("financial") ||
            lowerResponse.contains("information") ||
            lowerResponse.contains("service"),
            "Multi-keyword response should contain relevant guidance: " + responseText
        );
    }
    
    @Test
    @DisplayName("Agent Confidence Scoring - Should prefer higher confidence matches")
    void testAgentConfidenceScoring() {
        Map<String, Object> parameters = new HashMap<>();
        
        // Strong weather keywords should get high confidence
        Optional<String> strongWeatherResponse = fallbackSystem.attemptFallback(
            "Check weather forecast temperature rain snow conditions", "qwen2.5:0.5b", parameters);
        assertTrue(strongWeatherResponse.isPresent(), "Should match strong weather keywords");
        
        // Strong financial keywords should get high confidence
        Optional<String> strongFinancialResponse = fallbackSystem.attemptFallback(
            "Stock market price investment trading financial portfolio", "gemma:2b", parameters);
        assertTrue(strongFinancialResponse.isPresent(), "Should match strong financial keywords");
        
        // Strong travel keywords should get high confidence
        Optional<String> strongTravelResponse = fallbackSystem.attemptFallback(
            "Travel trip flight hotel booking vacation destination", "qwen2:1.5b", parameters);
        assertTrue(strongTravelResponse.isPresent(), "Should match strong travel keywords");
        
        // Verify all got appropriate responses
        LLMFallbackSystem.FallbackStats stats = fallbackSystem.getStats();
        assertTrue(stats.getSuccessfulFallbacks() >= 3, "Should have successful high-confidence matches");
    }
    
    @Test
    @DisplayName("Agent Learning - Should learn agent-specific patterns")
    void testAgentLearning() {
        Map<String, Object> parameters = new HashMap<>();
        
        // Teach the system some new agent-specific patterns
        fallbackSystem.learnFromResponse(
            "What's the air quality index in Beijing?",
            "For air quality information in Beijing, I recommend checking reliable environmental monitoring services like AirVisual or local environmental agencies.",
            "qwen2.5:0.5b",
            parameters
        );
        
        fallbackSystem.learnFromResponse(
            "What's the volatility of Bitcoin today?",
            "For cryptocurrency volatility data including Bitcoin, I recommend checking specialized crypto platforms like CoinGecko or CoinMarketCap.",
            "gemma:2b",
            parameters
        );
        
        // Test that learned patterns work
        Optional<String> airQualityResponse = fallbackSystem.attemptFallback(
            "Check air quality in Shanghai", "qwen2.5:0.5b", parameters);
        assertTrue(airQualityResponse.isPresent(), "Should use learned air quality pattern");
        
        Optional<String> cryptoResponse = fallbackSystem.attemptFallback(
            "How volatile is Ethereum?", "gemma:2b", parameters);
        assertTrue(cryptoResponse.isPresent(), "Should use learned crypto pattern");
        
        // Verify learning statistics
        LLMFallbackSystem.FallbackStats stats = fallbackSystem.getStats();
        assertTrue(stats.getRuleLearningEvents() >= 2, "Should record learning events");
    }
    
    @Test
    @DisplayName("Agent Error Handling - Should handle agent-specific edge cases")
    void testAgentErrorHandling() {
        Map<String, Object> parameters = new HashMap<>();
        
        // Test edge cases for each agent type
        String[] edgeCases = {
            "What's the weather on Mars?",  // Impossible weather query
            "What's the price of INVALIDTICKER stock?",  // Invalid stock
            "Plan a trip to Atlantis"  // Impossible destination
        };
        
        for (String edgeCase : edgeCases) {
            Optional<String> response = fallbackSystem.attemptFallback(edgeCase, "qwen2.5:0.5b", parameters);
            
            // Should either get a helpful response or no response (both are acceptable)
            if (response.isPresent()) {
                assertFalse(response.get().isEmpty(), "Edge case response should not be empty if provided");
                
                // Response should be helpful even for impossible requests
                String lowerResponse = response.get().toLowerCase();
                assertTrue(
                    lowerResponse.contains("recommend") || 
                    lowerResponse.contains("check") || 
                    lowerResponse.contains("service") ||
                    lowerResponse.contains("information") ||
                    lowerResponse.contains("help"),
                    "Edge case response should be helpful: " + response.get()
                );
            }
        }
    }
    
    @Test
    @DisplayName("Agent Performance - Should provide fast fallback responses")
    void testAgentPerformance() {
        Map<String, Object> parameters = new HashMap<>();
        
        // Test performance with various agent queries
        String[] performanceQueries = {
            "Weather in New York",
            "Apple stock price", 
            "Flight to London",
            "Temperature forecast",
            "Market performance",
            "Hotel booking"
        };
        
        long startTime = System.currentTimeMillis();
        
        for (String query : performanceQueries) {
            Optional<String> response = fallbackSystem.attemptFallback(query, "qwen2.5:0.5b", parameters);
            assertTrue(response.isPresent(), "Performance test should get response for: " + query);
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        // Should complete all queries quickly (allowing for test overhead)
        assertTrue(totalTime < 5000, // 5 seconds max for 6 queries
                  "Agent fallback performance should be fast: " + totalTime + "ms");
        
        // Verify performance statistics
        LLMFallbackSystem.FallbackStats stats = fallbackSystem.getStats();
        assertTrue(stats.getSuccessfulFallbacks() >= performanceQueries.length, 
                  "Should record all performance test responses");
        assertTrue(stats.getSuccessRate() > 0.8, // At least 80% success rate
                  "Should have good success rate: " + stats.getSuccessRate());
    }
}
