package io.amcp.connectors.ai.fallback;

import io.amcp.connectors.ai.async.AsyncLLMConnector;
import io.amcp.connectors.ai.cache.LLMResponseCache;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for agent-specific fallback scenarios
 * Tests Weather Agent, Stock Agent, and Travel Planner Agent fallback behavior
 */
class AgentFallbackIntegrationTest {
    
    @TempDir
    Path tempDir;
    
    private AsyncLLMConnector connector;
    private LLMFallbackSystem fallbackSystem;
    
    @BeforeEach
    void setUp() {
        // Use non-existent URL to force fallback mode
        connector = new AsyncLLMConnector("http://localhost:99999", 1, 2, true);
        
        // Create fallback system for direct testing with lower confidence threshold
        LLMResponseCache cache = new LLMResponseCache();
        String rulesDir = tempDir.resolve("agent-fallback-rules").toString();
        fallbackSystem = new LLMFallbackSystem(cache, rulesDir, 50, 150);
        
        // Pre-train the system with agent-specific responses
        preTrainAgentResponses();
    }
    
    @AfterEach
    void tearDown() {
        if (connector != null) {
            connector.shutdown();
        }
        if (fallbackSystem != null) {
            fallbackSystem.cleanupRules();
        }
    }
    
    /**
     * Pre-train the fallback system with agent-specific responses
     */
    void preTrainAgentResponses() {
        Map<String, Object> parameters = new HashMap<>();
        
        // Weather Agent training data
        fallbackSystem.learnFromResponse(
            "What's the weather like in Paris today?",
            "I'd be happy to help you check the weather in Paris. While my live weather data service is temporarily unavailable, I recommend checking a reliable weather service like Weather.com or your local weather app for current conditions in Paris. For accurate weather information, you'll want to look for current temperature, precipitation, and any weather advisories.",
            "qwen2.5:0.5b",
            parameters
        );
        
        fallbackSystem.learnFromResponse(
            "Will it rain tomorrow in London?",
            "For rain forecasts in London, I typically access real-time weather data. Since my weather service is currently experiencing issues, please check a trusted weather source like BBC Weather or Met Office for tomorrow's precipitation forecast in London. They provide accurate hourly and daily forecasts including rain probability.",
            "gemma:2b",
            parameters
        );
        
        // Stock Agent training data
        fallbackSystem.learnFromResponse(
            "What's the current price of AAPL stock?",
            "I understand you're looking for Apple (AAPL) stock price information. While my real-time market data feed is temporarily unavailable, I recommend checking reliable financial sources like Yahoo Finance, Bloomberg, or your brokerage app for current AAPL stock prices and market data. These sources provide real-time quotes and trading information.",
            "qwen2:1.5b",
            parameters
        );
        
        fallbackSystem.learnFromResponse(
            "Should I buy Tesla stock now?",
            "For investment advice regarding Tesla (TSLA) or any stock, I typically provide analysis based on current market data. Since my financial data service is currently unavailable, I recommend consulting with a qualified financial advisor or conducting thorough research using reliable financial platforms. Remember that all investments carry risk and past performance doesn't guarantee future results.",
            "qwen2.5:0.5b",
            parameters
        );
        
        // Travel Planner Agent training data
        fallbackSystem.learnFromResponse(
            "Plan a 3-day trip to Rome",
            "I'd love to help you plan your 3-day Rome itinerary! While my travel booking and real-time information services are temporarily unavailable, I can suggest that a great Rome trip typically includes: Day 1 - Colosseum and Roman Forum, Day 2 - Vatican City and St. Peter's Basilica, Day 3 - Trevi Fountain and Spanish Steps. For current prices, bookings, and detailed planning, please check travel sites like TripAdvisor, Booking.com, or consult a travel agent.",
            "gemma:2b",
            parameters
        );
        
        fallbackSystem.learnFromResponse(
            "Find me flights from New York to Tokyo",
            "I understand you're looking for flights from New York to Tokyo. While my flight booking system is currently experiencing technical difficulties, I recommend checking major travel booking sites like Expedia, Kayak, Google Flights, or airline websites directly (JAL, ANA, United, Delta) for current flight availability and prices on the NYC-Tokyo route. These platforms provide real-time flight information and booking capabilities.",
            "qwen2:1.5b",
            parameters
        );
    }
    
    @Test
    @DisplayName("Weather Agent - Should provide helpful weather fallback responses")
    void testWeatherAgentFallback() {
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
            CompletableFuture<String> future = connector.generateAsync(query, "qwen2.5:0.5b", parameters);
            
            assertDoesNotThrow(() -> {
                String response = future.get();
                assertNotNull(response, "Should get weather fallback for: " + query);
                assertFalse(response.isEmpty(), "Weather fallback should not be empty");
                
                // Verify response contains weather-related guidance
                String lowerResponse = response.toLowerCase();
                assertTrue(
                    lowerResponse.contains("weather") || 
                    lowerResponse.contains("forecast") || 
                    lowerResponse.contains("temperature") ||
                    lowerResponse.contains("check") ||
                    lowerResponse.contains("service") ||
                    lowerResponse.contains("recommend") ||
                    lowerResponse.contains("data") ||
                    lowerResponse.contains("information"),
                    "Weather fallback should contain relevant guidance: " + response
                );
            }, "Weather agent fallback should work for: " + query);
        }
        
        // Verify weather-specific fallback statistics
        AsyncLLMConnector.ConnectorStats stats = connector.getStats();
        assertTrue(stats.getFallbackResponses() >= weatherQueries.length, 
                  "Should record weather fallback responses");
    }
    
    @Test
    @DisplayName("Stock Agent - Should provide helpful financial fallback responses")
    void testStockAgentFallback() {
        Map<String, Object> parameters = new HashMap<>();
        
        // Test various stock/financial queries
        String[] stockQueries = {
            "What's the current price of Microsoft stock?",
            "How is the S&P 500 performing today?",
            "Should I invest in cryptocurrency right now?",
            "What's the dividend yield for Coca-Cola?",
            "Is the market going up or down this week?",
            "Get me the latest earnings report for Google",
            "What's the PE ratio of Amazon stock?"
        };
        
        for (String query : stockQueries) {
            CompletableFuture<String> future = connector.generateAsync(query, "gemma:2b", parameters);
            
            assertDoesNotThrow(() -> {
                String response = future.get();
                assertNotNull(response, "Should get stock fallback for: " + query);
                assertFalse(response.isEmpty(), "Stock fallback should not be empty");
                
                // Verify response contains financial guidance
                String lowerResponse = response.toLowerCase();
                assertTrue(
                    lowerResponse.contains("stock") || 
                    lowerResponse.contains("financial") || 
                    lowerResponse.contains("market") ||
                    lowerResponse.contains("investment") ||
                    lowerResponse.contains("price") ||
                    lowerResponse.contains("advisor") ||
                    lowerResponse.contains("data") ||
                    lowerResponse.contains("recommend") ||
                    lowerResponse.contains("information") ||
                    lowerResponse.contains("service"),
                    "Stock fallback should contain relevant guidance: " + response
                );
            }, "Stock agent fallback should work for: " + query);
        }
        
        // Verify stock-specific fallback statistics
        AsyncLLMConnector.ConnectorStats stats = connector.getStats();
        assertTrue(stats.getFallbackResponses() >= stockQueries.length, 
                  "Should record stock fallback responses");
    }
    
    @Test
    @DisplayName("Travel Planner Agent - Should provide helpful travel fallback responses")
    void testTravelPlannerAgentFallback() {
        Map<String, Object> parameters = new HashMap<>();
        
        // Test various travel planning queries
        String[] travelQueries = {
            "Plan a weekend trip to Barcelona",
            "Find hotels in Amsterdam for next month",
            "What's the best time to visit Japan?",
            "Book a flight from Los Angeles to Paris",
            "Recommend restaurants in Florence, Italy",
            "Create an itinerary for 5 days in Thailand",
            "What documents do I need to travel to Brazil?",
            "Find car rental options in Dublin"
        };
        
        for (String query : travelQueries) {
            CompletableFuture<String> future = connector.generateAsync(query, "qwen2:1.5b", parameters);
            
            assertDoesNotThrow(() -> {
                String response = future.get();
                assertNotNull(response, "Should get travel fallback for: " + query);
                assertFalse(response.isEmpty(), "Travel fallback should not be empty");
                
                // Verify response contains travel-related guidance
                String lowerResponse = response.toLowerCase();
                assertTrue(
                    lowerResponse.contains("travel") || 
                    lowerResponse.contains("trip") || 
                    lowerResponse.contains("booking") ||
                    lowerResponse.contains("flight") ||
                    lowerResponse.contains("hotel") ||
                    lowerResponse.contains("itinerary") ||
                    lowerResponse.contains("visit") ||
                    lowerResponse.contains("recommend") ||
                    lowerResponse.contains("information") ||
                    lowerResponse.contains("service") ||
                    lowerResponse.contains("platform") ||
                    lowerResponse.contains("check"),
                    "Travel fallback should contain relevant guidance: " + response
                );
            }, "Travel agent fallback should work for: " + query);
        }
        
        // Verify travel-specific fallback statistics
        AsyncLLMConnector.ConnectorStats stats = connector.getStats();
        assertTrue(stats.getFallbackResponses() >= travelQueries.length, 
                  "Should record travel fallback responses");
    }
    
    @Test
    @DisplayName("Multi-Agent Scenario - Should handle mixed agent queries appropriately")
    void testMultiAgentScenario() {
        Map<String, Object> parameters = new HashMap<>();
        
        // Mixed queries that might come in a real conversation
        List<CompletableFuture<String>> futures = new ArrayList<>();
        
        // Weather query
        futures.add(connector.generateAsync(
            "What's the weather like in my travel destination Tokyo?", 
            "qwen2.5:0.5b", parameters));
        
        // Stock query
        futures.add(connector.generateAsync(
            "Should I check my travel budget against current exchange rates for Japanese Yen?", 
            "gemma:2b", parameters));
        
        // Travel query
        futures.add(connector.generateAsync(
            "Plan activities for Tokyo considering the weather forecast", 
            "qwen2:1.5b", parameters));
        
        // Weather query
        futures.add(connector.generateAsync(
            "Will it be sunny enough for outdoor sightseeing in Tokyo?", 
            "qwen2.5:0.5b", parameters));
        
        // Stock query
        futures.add(connector.generateAsync(
            "What's the current USD to JPY exchange rate?", 
            "gemma:2b", parameters));
        
        // All should complete successfully with appropriate fallbacks
        assertDoesNotThrow(() -> {
            for (int i = 0; i < futures.size(); i++) {
                String response = futures.get(i).get();
                assertNotNull(response, "Multi-agent query " + i + " should get fallback");
                assertFalse(response.isEmpty(), "Multi-agent fallback " + i + " should not be empty");
            }
        });
        
        // Verify mixed scenario statistics
        AsyncLLMConnector.ConnectorStats stats = connector.getStats();
        assertTrue(stats.getFallbackResponses() >= futures.size(), 
                  "Should record all multi-agent fallback responses");
        assertEquals(futures.size(), stats.getTotalRequests() - stats.getCachedResponses(), 
                    "Should process all multi-agent requests");
    }
    
    @Test
    @DisplayName("Agent Context Switching - Should maintain context appropriately")
    void testAgentContextSwitching() {
        Map<String, Object> parameters = new HashMap<>();
        
        // Simulate a conversation that switches between agent contexts
        String[] conversationFlow = {
            "What's the weather in New York?",  // Weather
            "Plan a trip to New York",          // Travel
            "What's the cost of flights to New York?", // Travel/Stock
            "Check weather for my New York trip dates", // Weather
            "What's the current price of airline stocks?", // Stock
            "Recommend indoor activities in New York if it rains", // Travel/Weather
            "Should I invest in travel industry stocks?", // Stock
            "What's the forecast for next week in New York?" // Weather
        };
        
        List<String> responses = new ArrayList<>();
        
        for (String query : conversationFlow) {
            CompletableFuture<String> future = connector.generateAsync(query, "qwen2.5:0.5b", parameters);
            
            assertDoesNotThrow(() -> {
                String response = future.get();
                responses.add(response);
                assertNotNull(response, "Context switching should work for: " + query);
                assertFalse(response.isEmpty(), "Context switching response should not be empty");
            });
        }
        
        // Verify that responses are contextually appropriate
        assertTrue(responses.size() == conversationFlow.length, 
                  "Should get responses for all context switching queries");
        
        // Check that weather responses contain weather-related terms
        String weatherResponse1 = responses.get(0); // "What's the weather in New York?"
        String weatherResponse2 = responses.get(3); // "Check weather for my New York trip dates"
        String weatherResponse3 = responses.get(7); // "What's the forecast for next week in New York?"
        
        for (String weatherResp : Arrays.asList(weatherResponse1, weatherResponse2, weatherResponse3)) {
            String lower = weatherResp.toLowerCase();
            assertTrue(lower.contains("weather") || lower.contains("forecast") || lower.contains("temperature"),
                      "Weather responses should contain weather terms: " + weatherResp);
        }
        
        // Verify context switching statistics
        AsyncLLMConnector.ConnectorStats stats = connector.getStats();
        assertTrue(stats.getFallbackResponses() >= conversationFlow.length, 
                  "Should record all context switching fallback responses");
    }
    
    @Test
    @DisplayName("Agent Error Scenarios - Should handle agent-specific errors gracefully")
    void testAgentErrorScenarios() {
        Map<String, Object> parameters = new HashMap<>();
        
        // Test edge cases and potential error scenarios for each agent type
        String[] errorScenarios = {
            // Weather edge cases
            "What's the weather on Mars?",
            "Give me weather for coordinates 999,999",
            "",  // Empty query
            
            // Stock edge cases  
            "What's the price of INVALIDTICKER stock?",
            "Should I invest my life savings in meme stocks?",
            "Get me stock data for company that doesn't exist",
            
            // Travel edge cases
            "Plan a trip to Atlantis",
            "Book a flight to the moon",
            "Find hotels in a city that doesn't exist"
        };
        
        for (String errorQuery : errorScenarios) {
            if (errorQuery.isEmpty()) {
                // Skip empty query test as it's handled elsewhere
                continue;
            }
            
            CompletableFuture<String> future = connector.generateAsync(errorQuery, "qwen2.5:0.5b", parameters);
            
            assertDoesNotThrow(() -> {
                String response = future.get();
                // Should get some response, even for edge cases
                assertNotNull(response, "Should handle error scenario: " + errorQuery);
                
                // Response should be helpful even for impossible requests
                if (!response.isEmpty()) {
                    String lower = response.toLowerCase();
                    assertTrue(
                        lower.contains("help") || 
                        lower.contains("assist") || 
                        lower.contains("recommend") ||
                        lower.contains("check") ||
                        lower.contains("service") ||
                        lower.contains("available") ||
                        lower.contains("information") ||
                        lower.contains("data") ||
                        lower.contains("system") ||
                        lower.contains("technical"),
                        "Error scenario response should be helpful: " + response
                    );
                }
            }, "Should handle error scenario gracefully: " + errorQuery);
        }
    }
    
    @Test
    @DisplayName("Agent Performance - Should maintain good performance across agent types")
    void testAgentPerformanceCharacteristics() {
        Map<String, Object> parameters = new HashMap<>();
        
        // Test performance with rapid agent queries
        List<CompletableFuture<String>> performanceFutures = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        
        // Submit 15 rapid queries across different agent types
        for (int i = 0; i < 5; i++) {
            performanceFutures.add(connector.generateAsync(
                "Weather query " + i + ": What's the temperature?", 
                "qwen2.5:0.5b", parameters));
            
            performanceFutures.add(connector.generateAsync(
                "Stock query " + i + ": What's the market doing?", 
                "gemma:2b", parameters));
            
            performanceFutures.add(connector.generateAsync(
                "Travel query " + i + ": Plan a trip somewhere", 
                "qwen2:1.5b", parameters));
        }
        
        // All should complete quickly
        assertDoesNotThrow(() -> {
            for (CompletableFuture<String> future : performanceFutures) {
                String response = future.get();
                assertNotNull(response, "Performance test should get response");
            }
        });
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        // Should complete all 15 queries in reasonable time (allowing for test overhead)
        assertTrue(totalTime < 30000, // 30 seconds max for 15 queries
                  "Performance test should complete in reasonable time: " + totalTime + "ms");
        
        // Verify performance statistics
        AsyncLLMConnector.ConnectorStats stats = connector.getStats();
        assertTrue(stats.getFallbackResponses() >= 15, 
                  "Should record all performance test fallback responses");
        
        // Average latency should be reasonable for fallback responses
        if (stats.getAvgLatencyMs() > 0) {
            assertTrue(stats.getAvgLatencyMs() < 5000, // 5 second average max
                      "Average fallback latency should be reasonable: " + stats.getAvgLatencyMs() + "ms");
        }
    }
    
    @Test
    @DisplayName("Agent Learning - Should learn from agent-specific interactions")
    void testAgentLearning() {
        // This test verifies that the system learns agent-specific patterns
        LLMFallbackSystem.FallbackStats initialStats = fallbackSystem.getStats();
        long initialLearningEvents = initialStats.getRuleLearningEvents();
        
        Map<String, Object> parameters = new HashMap<>();
        
        // Teach the system some new agent-specific patterns
        fallbackSystem.learnFromResponse(
            "What's the air quality index in Beijing?",
            "For air quality information in Beijing, I recommend checking reliable environmental monitoring services like AirVisual or local environmental agencies. Air quality can change rapidly, so real-time data is important for health and travel planning.",
            "qwen2.5:0.5b",
            parameters
        );
        
        fallbackSystem.learnFromResponse(
            "What's the volatility of Bitcoin today?",
            "For cryptocurrency volatility data including Bitcoin, I recommend checking specialized crypto platforms like CoinGecko, CoinMarketCap, or major exchanges. Crypto markets are highly volatile and require real-time data for accurate analysis.",
            "gemma:2b",
            parameters
        );
        
        fallbackSystem.learnFromResponse(
            "Find me eco-friendly hotels in Costa Rica",
            "For eco-friendly accommodations in Costa Rica, I recommend checking sustainable travel platforms like BookDifferent or looking for certified eco-lodges. Costa Rica has excellent eco-tourism options, and booking platforms often have sustainability filters.",
            "qwen2:1.5b",
            parameters
        );
        
        // Verify learning occurred
        LLMFallbackSystem.FallbackStats updatedStats = fallbackSystem.getStats();
        assertTrue(updatedStats.getRuleLearningEvents() > initialLearningEvents,
                  "Should learn from agent-specific interactions");
        
        // Test that learned patterns work
        Optional<String> airQualityResponse = fallbackSystem.attemptFallback(
            "Check air quality in Shanghai", "qwen2.5:0.5b", parameters);
        assertTrue(airQualityResponse.isPresent(), "Should use learned air quality pattern");
        
        Optional<String> cryptoResponse = fallbackSystem.attemptFallback(
            "How volatile is Ethereum?", "gemma:2b", parameters);
        assertTrue(cryptoResponse.isPresent(), "Should use learned crypto pattern");
        
        Optional<String> ecoTravelResponse = fallbackSystem.attemptFallback(
            "Find sustainable accommodations in Peru", "qwen2:1.5b", parameters);
        assertTrue(ecoTravelResponse.isPresent(), "Should use learned eco-travel pattern");
    }
}
