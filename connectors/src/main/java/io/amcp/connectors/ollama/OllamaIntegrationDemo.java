package io.amcp.connectors.ollama;

import io.amcp.connectors.ai.OllamaConnectorConfig;
import io.amcp.connectors.ai.OllamaSpringAIConnector;
import io.amcp.tools.ToolRequest;
import io.amcp.tools.ToolResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * OLLAMA Integration Demo for AMCP v1.5 Enterprise Edition
 * 
 * This demo showcases the basic integration of local OLLAMA models with AMCP.
 * It demonstrates:
 * - OLLAMA connector functionality
 * - HTTP-based AI model interaction
 * - Simple chat interface
 * 
 * Prerequisites:
 * 1. OLLAMA must be installed and running locally (http://localhost:11434)
 * 2. A model must be downloaded (e.g., `ollama pull tinyllama`)
 * 
 * Usage:
 * java -cp target/amcp-connectors-1.5.0.jar \
 *   io.amcp.connectors.ollama.OllamaIntegrationDemo
 */
public class OllamaIntegrationDemo {
    
    private static final String DEMO_MODEL = "tinyllama"; // TinyLlama 1.1B - lightweight and fast
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    
    public static void main(String[] args) {
        logMessage("=== AMCP v1.5 OLLAMA Integration Demo ===");
        logMessage("Initializing OLLAMA integration...");
        
        try {
            // Initialize OLLAMA components
            OllamaConnectorConfig ollamaConfig = new OllamaConnectorConfig();
            OllamaSpringAIConnector ollamaConnector = new OllamaSpringAIConnector(ollamaConfig);
            
            // Test OLLAMA connectivity
            logMessage("Testing OLLAMA connectivity...");
            testOllamaConnection(ollamaConnector);
            
            // Start interactive demo
            runInteractiveDemo(ollamaConnector);
            
        } catch (Exception e) {
            logError("Demo initialization failed", e);
        } finally {
            executor.shutdown();
        }
    }
    
    private static void testOllamaConnection(OllamaSpringAIConnector connector) {
        try {
            logMessage("Checking OLLAMA service health...");
            
            // Simple connectivity test
            ToolRequest request = new ToolRequest(
                "chat",
                Map.of(
                    "model", DEMO_MODEL,
                    "prompt", "Hello! Please respond with just 'OLLAMA is working correctly.'",
                    "temperature", 0.1,
                    "max_tokens", 20
                )
            );
            
            ToolResponse response = connector.invoke(request).get();
            
            if (response.isSuccess()) {
                logMessage("✓ OLLAMA connection successful!");
                String content = extractContent(response);
                logMessage("Response: " + content);
            } else {
                logError("✗ OLLAMA connection failed: " + response.getErrorMessage());
            }
            
        } catch (Exception e) {
            logError("OLLAMA connectivity test failed", e);
            logMessage("Please ensure OLLAMA is running with: ollama serve");
            logMessage("And that the model '" + DEMO_MODEL + "' is available: ollama pull " + DEMO_MODEL);
        }
    }
    
    private static void runInteractiveDemo(OllamaSpringAIConnector connector) {
        Scanner scanner = new Scanner(System.in);
        
        logMessage("");
        logMessage("=== Interactive OLLAMA Chat Demo ===");
        logMessage("Type your messages to chat with the AI.");
        logMessage("Special commands:");
        logMessage("  /help - Show available commands");
        logMessage("  /model - Show current model info");
        logMessage("  /exit - Exit the demo");
        logMessage("");
        
        while (true) {
            System.out.print("You: ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) {
                continue;
            }
            
            if (input.equals("/exit")) {
                logMessage("Goodbye!");
                break;
            }
            
            if (input.equals("/help")) {
                showHelp();
                continue;
            }
            
            if (input.equals("/model")) {
                showModelInfo();
                continue;
            }
            
            // Process chat message
            processChatMessage(connector, input);
        }
        
        scanner.close();
    }
    
    private static void processChatMessage(OllamaSpringAIConnector connector, String message) {
        try {
            logMessage("Processing message: " + message);
            
            // Create tool request
            ToolRequest request = new ToolRequest(
                "chat",
                Map.of(
                    "model", DEMO_MODEL,
                    "prompt", "You are a helpful AI assistant integrated with the AMCP (Agent Mesh Communication Protocol) system. " +
                             "Respond to this user message in a friendly and helpful way: " + message,
                    "temperature", 0.7,
                    "max_tokens", 200
                )
            );
            
            // Get response from OLLAMA
            ToolResponse response = connector.invoke(request).get();
            
            if (response.isSuccess()) {
                String content = extractContent(response);
                System.out.println("AI: " + content);
            } else {
                logError("AI Error: " + response.getErrorMessage());
            }
            
        } catch (Exception e) {
            logError("Error processing chat message", e);
        }
    }
    
    private static String extractContent(ToolResponse response) {
        Object data = response.getData();
        if (data instanceof String) {
            return (String) data;
        } else if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) data;
            Object content = dataMap.get("response");
            return content != null ? content.toString() : dataMap.toString();
        }
        return data != null ? data.toString() : "No response";
    }
    
    private static void showHelp() {
        logMessage("Available commands:");
        logMessage("  /help - Show this help message");
        logMessage("  /model - Show information about the current OLLAMA model");
        logMessage("  /exit - Exit the demo");
        logMessage("  Any other text will be sent to the AI for response");
    }
    
    private static void showModelInfo() {
        logMessage("=== Model Information ===");
        logMessage("Current Model: " + DEMO_MODEL + " (TinyLlama 1.1B)");
        logMessage("OLLAMA Endpoint: http://localhost:11434");
        logMessage("Model Size: ~637MB download, ~2-3GB RAM usage");
        logMessage("Model Features: General language tasks, basic conversation");
        logMessage("To change model, edit DEMO_MODEL in the source code");
        logMessage("Available models: Run 'ollama list' in terminal");
    }
    
    private static void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [DEMO] " + message);
    }
    
    private static void logError(String message, Exception e) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.err.println("[" + timestamp + "] [ERROR] " + message + ": " + e.getMessage());
    }
    
    private static void logError(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.err.println("[" + timestamp + "] [ERROR] " + message);
    }
}