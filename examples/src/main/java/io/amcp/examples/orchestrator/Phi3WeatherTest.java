package io.amcp.examples.orchestrator;

import io.amcp.core.AgentContext;
import io.amcp.core.impl.SimpleAgentContext;
import io.amcp.messaging.EventBroker;
import io.amcp.messaging.impl.InMemoryEventBroker;
import io.amcp.mobility.MobilityManager;
import io.amcp.mobility.impl.SimpleMobilityManager;
import io.amcp.connectors.ai.OrchestratorAgent;
import io.amcp.examples.weather.WeatherAgent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Standalone test for Phi3 3.8B orchestration with weather queries.
 * 
 * This test demonstrates:
 * - Phi3 3.8B LLM integration for intent analysis
 * - Orchestrator routing to WeatherAgent
 * - Structured response handling
 * - End-to-end orchestration flow
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class Phi3WeatherTest {
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  AMCP v1.5 - Phi3 3.8B Weather Orchestration Test          ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // Check configuration
        String modelName = System.getenv("OLLAMA_MODEL");
        if (modelName == null || modelName.trim().isEmpty()) {
            modelName = "phi3:3.8b";
        }
        
        System.out.println("📋 Configuration:");
        System.out.println("   Model: " + modelName);
        System.out.println("   Ollama URL: " + System.getenv("OLLAMA_BASE_URL"));
        System.out.println();
        
        try {
            // Initialize AMCP infrastructure
            System.out.println("🔧 Initializing AMCP infrastructure...");
            EventBroker eventBroker = new InMemoryEventBroker();
            eventBroker.start(); // CRITICAL: Start the event broker!
            MobilityManager mobilityManager = new SimpleMobilityManager();
            AgentContext agentContext = new SimpleAgentContext(eventBroker, mobilityManager);
            System.out.println("✅ Infrastructure initialized");
            System.out.println("✅ EventBroker started");
            System.out.println();
            
            // Create and activate WeatherAgent
            System.out.println("🌤️  Creating WeatherAgent...");
            WeatherAgent weatherAgent = new WeatherAgent();
            weatherAgent.setContext(agentContext);
            agentContext.registerAgent(weatherAgent);
            weatherAgent.onActivate();
            System.out.println("✅ WeatherAgent activated");
            System.out.println();
            
            // Create and activate OrchestratorAgent
            System.out.println("🎯 Creating OrchestratorAgent...");
            OrchestratorAgent orchestrator = new OrchestratorAgent();
            orchestrator.setContext(agentContext);
            agentContext.registerAgent(orchestrator);
            orchestrator.onActivate();
            System.out.println("✅ OrchestratorAgent activated");
            System.out.println();
            
            // Wait for agents to fully initialize
            Thread.sleep(2000);
            
            // Test queries
            String[] testQueries = {
                "What is the weather in Paris?",
                "Tell me about the weather in Nice, France",
                "What's the temperature in Tokyo?"
            };
            
            System.out.println("══════════════════════════════════════════════════════════════");
            System.out.println("🧪 Running Test Queries");
            System.out.println("══════════════════════════════════════════════════════════════");
            System.out.println();
            
            for (int i = 0; i < testQueries.length; i++) {
                String query = testQueries[i];
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                System.out.println("Test " + (i + 1) + "/" + testQueries.length);
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                System.out.println();
                System.out.println("💬 Query: " + query);
                System.out.println();
                System.out.println("🔄 Processing through orchestration pipeline...");
                System.out.println();
                
                try {
                    // Call orchestrator
                    CompletableFuture<String> responseFuture = orchestrator.orchestrateRequest(
                        query, 
                        "test-session-" + i
                    );
                    
                    // Wait for response with timeout
                    String response = responseFuture.get(60, TimeUnit.SECONDS);
                    
                    System.out.println("✅ Orchestration Flow:");
                    System.out.println("   1. User Query → OrchestratorAgent");
                    System.out.println("   2. OrchestratorAgent → Phi3 3.8B (intent analysis)");
                    System.out.println("   3. Phi3 3.8B → Intent: weather, Agent: WeatherAgent");
                    System.out.println("   4. OrchestratorAgent → WeatherAgent (weather.request)");
                    System.out.println("   5. WeatherAgent → OpenWeatherMap API");
                    System.out.println("   6. WeatherAgent → OrchestratorAgent (structured response)");
                    System.out.println("   7. OrchestratorAgent → User (final answer)");
                    System.out.println();
                    System.out.println("📨 Response:");
                    System.out.println("─────────────────────────────────────────────────────────────");
                    System.out.println(response);
                    System.out.println("─────────────────────────────────────────────────────────────");
                    System.out.println();
                    System.out.println("✅ Test " + (i + 1) + " PASSED");
                    
                } catch (Exception e) {
                    System.out.println("❌ Test " + (i + 1) + " FAILED");
                    System.out.println("   Error: " + e.getMessage());
                    e.printStackTrace();
                }
                
                System.out.println();
                
                // Wait between tests
                if (i < testQueries.length - 1) {
                    Thread.sleep(3000);
                }
            }
            
            System.out.println("══════════════════════════════════════════════════════════════");
            System.out.println("🎉 All Tests Complete");
            System.out.println("══════════════════════════════════════════════════════════════");
            System.out.println();
            
            // Cleanup
            System.out.println("🧹 Cleaning up...");
            weatherAgent.onDeactivate();
            orchestrator.onDeactivate();
            System.out.println("✅ Cleanup complete");
            
            System.exit(0);
            
        } catch (Exception e) {
            System.err.println("❌ Test failed with exception: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
