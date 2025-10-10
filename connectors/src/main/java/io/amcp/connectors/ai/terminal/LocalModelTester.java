package io.amcp.connectors.ai.terminal;

import io.amcp.connectors.ai.ModelConfiguration;
import io.amcp.connectors.ai.async.AsyncLLMConnector;
import io.amcp.connectors.ai.gpu.GPUAccelerationConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Local terminal-based model testing utility for AMCP LLM orchestration.
 * Provides interactive testing of different models with real-time performance metrics.
 * 
 * Features:
 * - Interactive model selection and testing
 * - Real-time performance monitoring
 * - System resource detection
 * - Ollama integration testing
 * - Model comparison utilities
 * 
 * @author AMCP Development Team
 * @version 1.5.1
 */
public class LocalModelTester {
    
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BOLD = "\u001B[1m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_CYAN = "\u001B[36m";
    
    private final AsyncLLMConnector connector;
    private final GPUAccelerationConfig gpuConfig;
    private final BufferedReader reader;
    
    public LocalModelTester() {
        this.connector = new AsyncLLMConnector();
        this.gpuConfig = new GPUAccelerationConfig();
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }
    
    public static void main(String[] args) {
        LocalModelTester tester = new LocalModelTester();
        try {
            tester.runInteractiveSession();
        } catch (Exception e) {
            System.err.println("Error running model tester: " + e.getMessage());
            e.printStackTrace();
        } finally {
            tester.shutdown();
        }
    }
    
    /**
     * Run interactive testing session
     */
    public void runInteractiveSession() throws IOException {
        printHeader();
        printSystemInfo();
        
        boolean running = true;
        while (running) {
            printMenu();
            String choice = reader.readLine().trim();
            
            switch (choice.toLowerCase()) {
                case "1":
                    testRecommendedModel();
                    break;
                case "2":
                    testSpecificModel();
                    break;
                case "3":
                    compareModels();
                    break;
                case "4":
                    benchmarkModel();
                    break;
                case "5":
                    checkOllamaStatus();
                    break;
                case "6":
                    showModelInfo();
                    break;
                case "7":
                    testQwen25();
                    break;
                case "q":
                case "quit":
                case "exit":
                    running = false;
                    break;
                default:
                    println(ANSI_RED + "Invalid option. Please try again." + ANSI_RESET);
            }
            
            if (running) {
                println("\nPress Enter to continue...");
                reader.readLine();
            }
        }
        
        println(ANSI_GREEN + "Thank you for using AMCP Model Tester!" + ANSI_RESET);
    }
    
    /**
     * Print application header
     */
    private void printHeader() {
        println(ANSI_BOLD + ANSI_BLUE + "╔══════════════════════════════════════════════════════════════╗");
        println("║                    AMCP Model Tester v1.5.1                 ║");
        println("║              Local LLM Testing & Configuration              ║");
        println("╚══════════════════════════════════════════════════════════════╝" + ANSI_RESET);
        println();
    }
    
    /**
     * Print system information
     */
    private void printSystemInfo() {
        println(ANSI_BOLD + ANSI_CYAN + "System Information:" + ANSI_RESET);
        println(ModelConfiguration.getSystemInfo());
        println(gpuConfig.getConfigSummary());
        
        // Show recommended model
        double availableGB = Runtime.getRuntime().maxMemory() / (1024.0 * 1024.0 * 1024.0);
        ModelConfiguration.LightweightModel recommended = ModelConfiguration.getRecommendedModel(availableGB);
        println(ANSI_GREEN + "Recommended Model: " + recommended.getModelName() + 
               " (" + recommended.getDescription() + ")" + ANSI_RESET);
        println();
    }
    
    /**
     * Print main menu
     */
    private void printMenu() {
        println(ANSI_BOLD + ANSI_YELLOW + "Available Options:" + ANSI_RESET);
        println("1. Test Recommended Model");
        println("2. Test Specific Model");
        println("3. Compare Multiple Models");
        println("4. Benchmark Model Performance");
        println("5. Check Ollama Status");
        println("6. Show Model Information");
        println("7. Test NEW Qwen2.5:0.5b Model");
        println("q. Quit");
        print(ANSI_CYAN + "\nEnter your choice: " + ANSI_RESET);
    }
    
    /**
     * Test the system-recommended model
     */
    private void testRecommendedModel() throws IOException {
        double availableGB = Runtime.getRuntime().maxMemory() / (1024.0 * 1024.0 * 1024.0);
        ModelConfiguration.LightweightModel recommended = ModelConfiguration.getRecommendedModel(availableGB);
        
        println(ANSI_BOLD + ANSI_GREEN + "Testing Recommended Model: " + recommended.getModelName() + ANSI_RESET);
        println("Description: " + recommended.getDescription());
        println();
        
        testModelWithPrompt(recommended.getModelName());
    }
    
    /**
     * Test a specific model chosen by user
     */
    private void testSpecificModel() throws IOException {
        println(ANSI_BOLD + ANSI_BLUE + "Available Models:" + ANSI_RESET);
        ModelConfiguration.LightweightModel[] models = ModelConfiguration.LightweightModel.values();
        
        for (int i = 0; i < models.length; i++) {
            println((i + 1) + ". " + models[i].getModelName() + " - " + models[i].getDescription());
        }
        
        print(ANSI_CYAN + "\nEnter model number (1-" + models.length + "): " + ANSI_RESET);
        try {
            int choice = Integer.parseInt(reader.readLine().trim());
            if (choice >= 1 && choice <= models.length) {
                ModelConfiguration.LightweightModel selected = models[choice - 1];
                println(ANSI_GREEN + "Selected: " + selected.getModelName() + ANSI_RESET);
                testModelWithPrompt(selected.getModelName());
            } else {
                println(ANSI_RED + "Invalid model number." + ANSI_RESET);
            }
        } catch (NumberFormatException e) {
            println(ANSI_RED + "Invalid input. Please enter a number." + ANSI_RESET);
        }
    }
    
    /**
     * Test NEW Qwen2.5:0.5b model specifically
     */
    private void testQwen25() throws IOException {
        println(ANSI_BOLD + ANSI_GREEN + "Testing NEW Qwen2.5:0.5b - Ultra-Minimal Model" + ANSI_RESET);
        println("RAM Requirement: Only 0.4GB!");
        println("Optimized for: Resource-constrained environments");
        println("Timeout: Ultra-fast 60 seconds");
        println();
        
        // Check if model is available
        println(ANSI_YELLOW + "Checking if Qwen2.5:0.5b is available..." + ANSI_RESET);
        if (checkModelAvailability("qwen2.5:0.5b")) {
            testModelWithPrompt("qwen2.5:0.5b");
        } else {
            println(ANSI_RED + "Qwen2.5:0.5b not found. Install with:" + ANSI_RESET);
            println(ANSI_CYAN + "ollama pull qwen2.5:0.5b" + ANSI_RESET);
        }
    }
    
    /**
     * Compare multiple models
     */
    private void compareModels() throws IOException {
        println(ANSI_BOLD + ANSI_BLUE + "Model Comparison Mode" + ANSI_RESET);
        print("Enter test prompt: ");
        String prompt = reader.readLine().trim();
        
        if (prompt.isEmpty()) {
            prompt = "Explain artificial intelligence in simple terms.";
            println("Using default prompt: " + prompt);
        }
        
        // Test key models for comparison
        String[] modelsToTest = {"qwen2.5:0.5b", "qwen2:1.5b", "gemma:2b"};
        
        println(ANSI_BOLD + "\nComparing Models:" + ANSI_RESET);
        for (String model : modelsToTest) {
            println(ANSI_CYAN + "\n--- Testing " + model + " ---" + ANSI_RESET);
            if (checkModelAvailability(model)) {
                testModelWithPrompt(model, prompt, false);
            } else {
                println(ANSI_RED + model + " not available. Install with: ollama pull " + model + ANSI_RESET);
            }
        }
    }
    
    /**
     * Benchmark model performance
     */
    private void benchmarkModel() throws IOException {
        print("Enter model name to benchmark (or press Enter for recommended): ");
        String modelName = reader.readLine().trim();
        
        if (modelName.isEmpty()) {
            double availableGB = Runtime.getRuntime().maxMemory() / (1024.0 * 1024.0 * 1024.0);
            ModelConfiguration.LightweightModel recommended = ModelConfiguration.getRecommendedModel(availableGB);
            modelName = recommended.getModelName();
        }
        
        println(ANSI_BOLD + ANSI_BLUE + "Benchmarking " + modelName + ANSI_RESET);
        
        String[] testPrompts = {
            "What is AI?",
            "Explain machine learning briefly.",
            "List 3 benefits of automation.",
            "What is the future of technology?",
            "How does neural networks work?"
        };
        
        long totalTime = 0;
        int successCount = 0;
        
        for (int i = 0; i < testPrompts.length; i++) {
            println(ANSI_CYAN + "\nTest " + (i + 1) + "/5: " + testPrompts[i] + ANSI_RESET);
            
            long startTime = System.currentTimeMillis();
            try {
                CompletableFuture<String> future = connector.generateAsync(
                    testPrompts[i], 
                    modelName, 
                    getDefaultParameters()
                );
                
                String response = future.get(120, TimeUnit.SECONDS);
                long duration = System.currentTimeMillis() - startTime;
                
                totalTime += duration;
                successCount++;
                
                println(ANSI_GREEN + "✓ Response received in " + duration + "ms" + ANSI_RESET);
                println("Response: " + truncate(response, 100));
                
            } catch (Exception e) {
                println(ANSI_RED + "✗ Failed: " + e.getMessage() + ANSI_RESET);
            }
        }
        
        // Show benchmark results
        println(ANSI_BOLD + ANSI_YELLOW + "\n=== Benchmark Results ===" + ANSI_RESET);
        println("Model: " + modelName);
        println("Success Rate: " + successCount + "/5 (" + (successCount * 20) + "%)");
        if (successCount > 0) {
            println("Average Response Time: " + (totalTime / successCount) + "ms");
        }
        
        // Show connector statistics
        AsyncLLMConnector.ConnectorStats stats = connector.getStats();
        println("Total Requests: " + stats.getTotalRequests());
        println("Cache Hit Rate: " + String.format("%.1f%%", stats.getCacheHitRate() * 100));
        println("Average Latency: " + String.format("%.1fms", stats.getAvgLatencyMs()));
    }
    
    /**
     * Check Ollama service status
     */
    private void checkOllamaStatus() {
        println(ANSI_BOLD + ANSI_BLUE + "Checking Ollama Status..." + ANSI_RESET);
        
        try {
            ProcessBuilder pb = new ProcessBuilder("ollama", "list");
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                println(ANSI_GREEN + "✓ Ollama is running" + ANSI_RESET);
                
                // Read available models
                BufferedReader processReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
                );
                
                println(ANSI_CYAN + "\nInstalled Models:" + ANSI_RESET);
                String line;
                while ((line = processReader.readLine()) != null) {
                    println(line);
                }
                
            } else {
                println(ANSI_RED + "✗ Ollama not running or not installed" + ANSI_RESET);
                println("Install Ollama: curl -fsSL https://ollama.com/install.sh | sh");
            }
            
        } catch (Exception e) {
            println(ANSI_RED + "✗ Error checking Ollama: " + e.getMessage() + ANSI_RESET);
        }
    }
    
    /**
     * Show detailed model information
     */
    private void showModelInfo() {
        println(ANSI_BOLD + ANSI_BLUE + "Model Information" + ANSI_RESET);
        
        ModelConfiguration.LightweightModel[] models = ModelConfiguration.LightweightModel.values();
        
        for (ModelConfiguration.LightweightModel model : models) {
            ModelConfiguration.ModelConfig config = ModelConfiguration.getModelConfig(model);
            
            println(ANSI_BOLD + "\n" + model.getModelName() + ANSI_RESET);
            println("  Description: " + model.getDescription());
            println("  RAM Required: " + model.getRamRequirementGB() + "GB");
            println("  Temperature: " + config.getTemperature());
            println("  Max Tokens: " + config.getMaxTokens());
            println("  Context Window: " + config.getContextWindow());
            println("  Streaming: " + (config.supportsStreaming() ? "Yes" : "No"));
            
            if (model == ModelConfiguration.LightweightModel.QWEN2_5_0_5B) {
                println(ANSI_GREEN + "  ★ NEW MODEL - Ultra-minimal resource usage!" + ANSI_RESET);
            }
        }
    }
    
    /**
     * Test a model with user input or default prompt
     */
    private void testModelWithPrompt(String modelName) throws IOException {
        print("Enter test prompt (or press Enter for default): ");
        String prompt = reader.readLine().trim();
        
        if (prompt.isEmpty()) {
            prompt = "Explain artificial intelligence in simple terms.";
            println("Using default prompt: " + prompt);
        }
        
        testModelWithPrompt(modelName, prompt, true);
    }
    
    /**
     * Test a model with specific prompt
     */
    private void testModelWithPrompt(String modelName, String prompt, boolean showFullResponse) {
        println(ANSI_YELLOW + "Testing " + modelName + "..." + ANSI_RESET);
        
        long startTime = System.currentTimeMillis();
        
        try {
            CompletableFuture<String> future = connector.generateAsync(
                prompt, 
                modelName, 
                getDefaultParameters()
            );
            
            println("Request sent, waiting for response...");
            String response = future.get(120, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;
            
            println(ANSI_GREEN + "✓ Success! Response time: " + duration + "ms" + ANSI_RESET);
            println(ANSI_BOLD + "Response:" + ANSI_RESET);
            
            if (showFullResponse) {
                println(response);
            } else {
                println(truncate(response, 200) + "...");
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            println(ANSI_RED + "✗ Failed after " + duration + "ms: " + e.getMessage() + ANSI_RESET);
            
            if (e.getMessage().contains("404")) {
                println(ANSI_YELLOW + "Model not found. Install with: ollama pull " + modelName + ANSI_RESET);
            }
        }
    }
    
    /**
     * Check if a model is available
     */
    private boolean checkModelAvailability(String modelName) {
        try {
            ProcessBuilder pb = new ProcessBuilder("ollama", "list");
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                BufferedReader processReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
                );
                
                String line;
                while ((line = processReader.readLine()) != null) {
                    if (line.toLowerCase().contains(modelName.toLowerCase())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore and return false
        }
        
        return false;
    }
    
    /**
     * Get default parameters for testing
     */
    private Map<String, Object> getDefaultParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("temperature", 0.7);
        params.put("max_tokens", 1000);
        return params;
    }
    
    /**
     * Truncate string for display
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
    
    /**
     * Print with newline
     */
    private void println(String text) {
        System.out.println(text);
    }
    
    /**
     * Print without newline
     */
    private void print(String text) {
        System.out.print(text);
    }
    
    /**
     * Print empty line
     */
    private void println() {
        System.out.println();
    }
    
    /**
     * Shutdown resources
     */
    public void shutdown() {
        try {
            if (connector != null) {
                connector.shutdown();
            }
            if (reader != null) {
                reader.close();
            }
        } catch (Exception e) {
            System.err.println("Error during shutdown: " + e.getMessage());
        }
    }
}
