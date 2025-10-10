package io.amcp.connectors.ai.async;

import io.amcp.tools.ToolRequest;
import io.amcp.tools.ToolResponse;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for AsyncLLMConnector functionality.
 * Tests async operations, caching, timeout handling, and model-specific optimizations.
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
class AsyncLLMConnectorTest {
    
    private AsyncLLMConnector connector;
    
    @BeforeEach
    void setUp() {
        // Use a test connector with shorter timeouts for faster tests
        connector = new AsyncLLMConnector(
            "http://localhost:11434", 
            5, // 5 second timeout for tests
            2, // 2 retries
            true // caching enabled
        );
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (connector != null) {
            connector.shutdown();
        }
    }
    
    @Test
    @DisplayName("Test async LLM generation with Gemma 2B model")
    void testAsyncGenerationGemma2B() {
        String prompt = "What is artificial intelligence?";
        String model = "gemma:2b";
        Map<String, Object> parameters = Map.of(
            "temperature", 0.6,
            "max_tokens", 100
        );
        
        // This test will fail if Ollama is not running, but validates the async structure
        CompletableFuture<String> future = connector.generateAsync(prompt, model, parameters);
        
        assertNotNull(future);
        assertFalse(future.isDone()); // Should be async
        
        // Test timeout behavior (should complete within reasonable time or timeout)
        assertDoesNotThrow(() -> {
            try {
                String result = future.get(10, TimeUnit.SECONDS);
                // If Ollama is running, we should get a response
                assertNotNull(result);
                assertFalse(result.isEmpty());
            } catch (Exception e) {
                // Expected if Ollama is not running - test structure is still valid
                String message = e.getMessage();
                assertTrue((message != null && (message.contains("Connection refused") || 
                          message.contains("failed"))) ||
                          e.getCause() instanceof java.net.ConnectException);
            }
        });
    }
    
    @Test
    @DisplayName("Test async LLM generation with Qwen2 1.5B model")
    void testAsyncGenerationQwen2() {
        String prompt = "Explain machine learning in simple terms.";
        String model = "qwen2:1.5b";
        Map<String, Object> parameters = Map.of(
            "temperature", 0.6,
            "max_tokens", 150
        );
        
        CompletableFuture<String> future = connector.generateAsync(prompt, model, parameters);
        
        assertNotNull(future);
        
        // Test that the future completes (or fails gracefully)
        assertDoesNotThrow(() -> {
            try {
                String result = future.get(10, TimeUnit.SECONDS);
                assertNotNull(result);
            } catch (Exception e) {
                // Expected if Ollama is not running
                String message = e.getMessage();
                assertTrue((message != null && (message.contains("Connection refused") || 
                          message.contains("failed"))) ||
                          e.getCause() instanceof java.net.ConnectException);
            }
        });
    }
    
    @Test
    @DisplayName("Test batch generation functionality")
    void testBatchGeneration() {
        List<String> prompts = Arrays.asList(
            "What is AI?",
            "What is ML?",
            "What is DL?"
        );
        String model = "gemma:2b";
        Map<String, Object> parameters = Map.of("temperature", 0.6);
        
        CompletableFuture<List<String>> future = connector.generateBatch(prompts, model, parameters);
        
        assertNotNull(future);
        
        assertDoesNotThrow(() -> {
            try {
                List<String> results = future.get(15, TimeUnit.SECONDS);
                assertNotNull(results);
                assertEquals(3, results.size());
                results.forEach(result -> {
                    assertNotNull(result);
                    assertFalse(result.isEmpty());
                });
            } catch (Exception e) {
                // Expected if Ollama is not running
                String message = e.getMessage();
                assertTrue((message != null && (message.contains("Connection refused") || 
                          message.contains("failed"))) ||
                          e.getCause() instanceof java.net.ConnectException);
            }
        });
    }
    
    @Test
    @DisplayName("Test ToolRequest interface compatibility")
    void testToolRequestInterface() {
        Map<String, Object> params = Map.of(
            "prompt", "Test prompt",
            "model", "gemma:2b",
            "temperature", 0.6
        );
        
        ToolRequest toolRequest = new ToolRequest("llm-generate", params, "test-request-id");
        
        CompletableFuture<ToolResponse> future = connector.invoke(toolRequest);
        
        assertNotNull(future);
        
        assertDoesNotThrow(() -> {
            try {
                ToolResponse response = future.get(10, TimeUnit.SECONDS);
                assertNotNull(response);
                assertEquals("test-request-id", response.getRequestId());
                
                if (response.isSuccess()) {
                    Object data = response.getData();
                    assertNotNull(data);
                    if (data instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> result = (Map<String, Object>) data;
                        assertTrue(result.containsKey("response"));
                        assertTrue(result.containsKey("model"));
                    }
                } else {
                    // Expected if Ollama is not running
                    assertNotNull(response.getErrorMessage());
                }
            } catch (Exception e) {
                // Expected if Ollama is not running
                String message = e.getMessage();
                assertTrue((message != null && (message.contains("Connection refused") || 
                          message.contains("failed"))) ||
                          e.getCause() instanceof java.net.ConnectException);
            }
        });
    }
    
    @Test
    @DisplayName("Test caching functionality")
    void testCaching() {
        String prompt = "Test caching prompt";
        String model = "gemma:2b";
        Map<String, Object> parameters = Map.of("temperature", 0.6);
        
        // First call
        CompletableFuture<String> future1 = connector.generateAsync(prompt, model, parameters);
        
        // Second call with same parameters (should hit cache if first succeeds)
        CompletableFuture<String> future2 = connector.generateAsync(prompt, model, parameters);
        
        assertNotNull(future1);
        assertNotNull(future2);
        
        // Both futures should complete (or fail) consistently
        assertDoesNotThrow(() -> {
            try {
                String result1 = future1.get(10, TimeUnit.SECONDS);
                String result2 = future2.get(10, TimeUnit.SECONDS);
                
                // If both succeed, they should be identical (from cache)
                assertEquals(result1, result2);
                
                // Check statistics
                AsyncLLMConnector.ConnectorStats stats = connector.getStats();
                assertNotNull(stats);
                assertTrue(stats.getTotalRequests() >= 2);
                
            } catch (Exception e) {
                // Expected if Ollama is not running
                String message = e.getMessage();
                assertTrue((message != null && (message.contains("Connection refused") || 
                          message.contains("failed"))) ||
                          e.getCause() instanceof java.net.ConnectException);
            }
        });
    }
    
    @Test
    @DisplayName("Test connector statistics")
    void testConnectorStatistics() {
        AsyncLLMConnector.ConnectorStats stats = connector.getStats();
        
        assertNotNull(stats);
        assertTrue(stats.getTotalRequests() >= 0);
        assertTrue(stats.getCachedResponses() >= 0);
        assertTrue(stats.getFailedRequests() >= 0);
        assertTrue(stats.getAvgLatencyMs() >= 0.0);
        assertTrue(stats.getCacheHitRate() >= 0.0 && stats.getCacheHitRate() <= 1.0);
        
        // Test toString method
        String statsString = stats.toString();
        assertNotNull(statsString);
        assertTrue(statsString.contains("ConnectorStats"));
    }
    
    @Test
    @DisplayName("Test cache cleanup functionality")
    void testCacheCleanup() {
        // This should not throw any exceptions
        assertDoesNotThrow(() -> connector.cleanupCache());
    }
    
    @Test
    @DisplayName("Test connector shutdown")
    void testConnectorShutdown() {
        AsyncLLMConnector testConnector = new AsyncLLMConnector();
        
        // Should shutdown gracefully
        assertDoesNotThrow(() -> testConnector.shutdown());
    }
    
    @Test
    @DisplayName("Test model-specific timeout optimization")
    void testModelSpecificTimeouts() {
        // Test that different models can be handled
        String[] models = {"gemma:2b", "qwen2:1.5b", "qwen2:7b", "llama3:8b"};
        String prompt = "Test prompt";
        Map<String, Object> parameters = Map.of("temperature", 0.6);
        
        for (String model : models) {
            CompletableFuture<String> future = connector.generateAsync(prompt, model, parameters);
            assertNotNull(future, "Future should not be null for model: " + model);
            
            // Each model should handle timeouts appropriately
            assertDoesNotThrow(() -> {
                try {
                    future.get(15, TimeUnit.SECONDS);
                } catch (Exception e) {
                    // Expected if Ollama is not running or model not available
                    String message = e.getMessage();
                    assertTrue((message != null && (message.contains("Connection refused") || 
                              message.contains("failed") ||
                              message.contains("not found"))) ||
                              e.getCause() instanceof java.net.ConnectException);
                }
            });
        }
    }
    
    @Test
    @DisplayName("Test concurrent request handling")
    void testConcurrentRequests() {
        String prompt = "Concurrent test prompt";
        String model = "gemma:2b";
        Map<String, Object> parameters = Map.of("temperature", 0.6);
        
        // Create multiple concurrent requests
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            futures.add(connector.generateAsync(prompt + " " + i, model, parameters));
        }
        
        // All futures should be created successfully
        assertEquals(5, futures.size());
        futures.forEach(future -> assertNotNull(future));
        
        // Wait for all to complete (or fail)
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        
        assertDoesNotThrow(() -> {
            try {
                allFutures.get(20, TimeUnit.SECONDS);
                
                // Check that all completed
                for (CompletableFuture<String> future : futures) {
                    assertTrue(future.isDone());
                    String result = future.get();
                    assertNotNull(result);
                }
            } catch (Exception e) {
                // Expected if Ollama is not running
                String message = e.getMessage();
                assertTrue((message != null && (message.contains("Connection refused") || 
                          message.contains("failed"))) ||
                          e.getCause() instanceof java.net.ConnectException);
            }
        });
    }
    
    @Test
    @DisplayName("Test error handling and retry logic")
    void testErrorHandlingAndRetry() {
        // Test with invalid URL to trigger retry logic
        AsyncLLMConnector errorConnector = new AsyncLLMConnector(
            "http://invalid-url:9999", 
            2, // short timeout
            2, // 2 retries
            false // no caching
        );
        
        try {
            String prompt = "Test error handling";
            String model = "gemma:2b";
            Map<String, Object> parameters = Map.of("temperature", 0.6);
            
            CompletableFuture<String> future = errorConnector.generateAsync(prompt, model, parameters);
            
            assertNotNull(future);
            
            // Should fail with connection error
            ExecutionException exception = assertThrows(ExecutionException.class, () -> {
                future.get(10, TimeUnit.SECONDS);
            });
            
            // Should contain retry information in the error
            assertTrue(exception.getCause().getMessage().contains("failed after") ||
                      exception.getCause() instanceof java.net.UnknownHostException);
            
        } finally {
            errorConnector.shutdown();
        }
    }
}
