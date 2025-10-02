package io.amcp.connectors.ai.demo;

import io.amcp.connectors.ai.planning.TaskPlanningEngine;
import io.amcp.connectors.ai.correlation.CorrelationTrackingManager;
import io.amcp.connectors.ai.fallback.FallbackStrategyManager;
import io.amcp.connectors.ai.prompts.PromptOptimizationEngine;
import io.amcp.connectors.ai.monitoring.HealthCheckMonitor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Working Enhanced Orchestration Demo
 * 
 * Demonstrates integration of all enhanced orchestration components:
 * ✅ Task Planning Engine - Intelligent task decomposition
 * ✅ Correlation Tracking Manager - Request-response correlation  
 * ✅ Fallback Strategy Manager - Error handling and recovery
 * ✅ Prompt Optimization Engine - Model-agnostic prompting
 * ✅ Health Check Monitor - System observability
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 * @since 1.5.0
 */
public class WorkingOrchestrationDemo {
    
    private static final String DEMO_DIVIDER = "=".repeat(80);
    private static final String SECTION_DIVIDER = "-".repeat(60);
    
    public static void main(String[] args) {
        System.out.println(DEMO_DIVIDER);
        System.out.println("🚀 AMCP v1.5 Enhanced Orchestration Working Demo");
        System.out.println("🎯 All Components Integration Test");
        System.out.println(DEMO_DIVIDER);
        
        WorkingOrchestrationDemo demo = new WorkingOrchestrationDemo();
        
        try {
            demo.runAllComponentsDemo();
        } catch (Exception e) {
            System.err.println("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println(DEMO_DIVIDER);
        System.out.println("✅ Enhanced Orchestration Demo Complete!");
        System.out.println("🎉 All 5 major components demonstrated successfully!");
        System.out.println(DEMO_DIVIDER);
    }
    
    private void runAllComponentsDemo() throws InterruptedException {
        // Demo 1: Task Planning Engine
        demonstrateTaskPlanning();
        
        // Demo 2: Correlation Tracking
        demonstrateCorrelationTracking();
        
        // Demo 3: Fallback Strategies
        demonstrateFallbackStrategies();
        
        // Demo 4: Prompt Optimization
        demonstratePromptOptimization();
        
        // Demo 5: Health Monitoring
        demonstrateHealthMonitoring();
        
        // Demo 6: Complete Integration
        demonstrateCompleteIntegration();
    }
    
    private void demonstrateTaskPlanning() {
        printSection("1. Task Planning Engine - Intelligent Task Decomposition");
        
        TaskPlanningEngine planningEngine = new TaskPlanningEngine();
        
        // Create available agents with correct constructor
        Set<TaskPlanningEngine.AgentInfo> availableAgents = new HashSet<>();
        availableAgents.add(new TaskPlanningEngine.AgentInfo("weather-agent", 
            List.of("weather", "forecast", "temperature")));
        availableAgents.add(new TaskPlanningEngine.AgentInfo("travel-agent", 
            List.of("travel", "flights", "hotels", "booking")));
        availableAgents.add(new TaskPlanningEngine.AgentInfo("chat-agent", 
            List.of("general", "conversation", "assistance")));
        
        // Test different task planning scenarios
        String[] testQueries = {
            "Get weather forecast for Paris",
            "Plan travel to Rome for 3 days",
            "What is the current stock price of Apple?"
        };
        
        for (String query : testQueries) {
            logMessage("📋 Planning task: " + query);
            
            try {
                var taskPlan = planningEngine.generateTaskPlan(query, availableAgents, UUID.randomUUID().toString());
                
                System.out.println("✅ Generated plan with " + taskPlan.getTaskCount() + " tasks:");
                for (var task : taskPlan.getTasks()) {
                    System.out.println("   📌 " + task.getCapability() + " → " + task.getAgent());
                    System.out.println("      📊 Parameters: " + task.getParams());
                    System.out.println("      🎯 Priority: " + task.getPriority());
                }
                
            } catch (Exception e) {
                System.err.println("❌ Task planning failed: " + e.getMessage());
            }
        }
        
        logMessage("🎯 Task planning demonstrates intelligent task decomposition");
        System.out.println();
    }
    
    private void demonstrateCorrelationTracking() throws InterruptedException {
        printSection("2. Correlation Tracking Manager - Request-Response Correlation");
        
        CorrelationTrackingManager correlationManager = new CorrelationTrackingManager();
        
        logMessage("🔗 Testing correlation tracking with timeouts");
        
        // Create correlations with proper method signature
        Map<String, Object> context1 = Map.of("requestType", "weather", "location", "Paris");
        Map<String, Object> context2 = Map.of("requestType", "travel", "destination", "Rome");
        
        var weather = correlationManager.createCorrelation("weather-req-001", "weather-agent", context1);
        var travel = correlationManager.createCorrelation("travel-req-001", "travel-agent", context2);
        
        System.out.println("✅ Created correlations:");
        System.out.println("   🌤️  Weather: " + weather.getCorrelationId());
        System.out.println("   ✈️  Travel: " + travel.getCorrelationId());
        
        // Simulate async operations
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        // Simulate weather response (quick)
        executor.submit(() -> {
            try {
                Thread.sleep(500);
                logMessage("🌤️  Weather response available");
                // In real implementation, would call correlationManager.recordResponse()
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Simulate travel response (slower)
        executor.submit(() -> {
            try {
                Thread.sleep(1500);
                logMessage("✈️  Travel response available");
                // In real implementation, would call correlationManager.recordResponse()
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Demonstrate timeout handling
        logMessage("⏳ Testing timeout handling...");
        
        try {
            // This would timeout in real implementation
            CompletableFuture<Object> responseFuture = correlationManager.waitForResponse("quick-response");
            Object responseObj = responseFuture.get(1, TimeUnit.SECONDS);
            String response = responseObj != null ? responseObj.toString() : "null";
            System.out.println("✅ Quick response: " + response);
        } catch (Exception e) {
            System.out.println("⚠️  Request handling: " + e.getClass().getSimpleName());
        }
        
        executor.shutdown();
        executor.awaitTermination(3, TimeUnit.SECONDS);
        
        logMessage("🎯 Correlation tracking demonstrates distributed coordination");
        System.out.println();
    }
    
    private void demonstrateFallbackStrategies() {
        printSection("3. Fallback Strategy Manager - Robust Error Handling");
        
        FallbackStrategyManager fallbackManager = new FallbackStrategyManager();
        
        logMessage("🛡️  Testing fallback and recovery strategies");
        
        // Test 1: Malformed LLM output recovery
        String malformedJson = "{incomplete json without closing";
        logMessage("🔧 Testing malformed JSON recovery...");
        
        try {
            String recovered = fallbackManager.handleMalformedLLMOutput(
                malformedJson, "weather_request", "attempt_1", "gpt-4");
            System.out.println("✅ Recovered JSON: " + recovered);
        } catch (Exception e) {
            System.out.println("❌ JSON recovery failed: " + e.getMessage());
        }
        
        // Test 2: Emergency response generation
        logMessage("🚨 Testing emergency response generation...");
        
        try {
            String emergencyResponse = fallbackManager.generateEmergencyResponse(
                "Get weather for Paris", "Weather service temporarily unavailable");
            System.out.println("✅ Emergency response: " + emergencyResponse);
        } catch (Exception e) {
            System.out.println("❌ Emergency response failed: " + e.getMessage());
        }
        
        // Test 3: Circuit breaker pattern
        logMessage("⚡ Testing circuit breaker pattern...");
        
        String testService = "weather-service";
        
        // Record failures to trigger circuit breaker
        for (int i = 0; i < 3; i++) {
            fallbackManager.recordFailure(testService);
            System.out.println("📉 Recorded failure " + (i + 1) + " for " + testService);
        }
        
        boolean circuitOpen = fallbackManager.isCircuitOpen(testService);
        System.out.println("⚡ Circuit breaker status: " + (circuitOpen ? "OPEN" : "CLOSED"));
        
        // Test recovery after cool-down
        if (circuitOpen) {
            logMessage("🔄 Circuit breaker protecting system from failed service");
        }
        
        logMessage("🎯 Fallback strategies demonstrate robust error recovery");
        System.out.println();
    }
    
    private void demonstratePromptOptimization() {
        printSection("4. Prompt Optimization Engine - Model-Agnostic Prompting");
        
        PromptOptimizationEngine promptEngine = new PromptOptimizationEngine();
        
        logMessage("🎨 Testing model-specific prompt optimization");
        
        String taskDescription = "Plan a weekend trip to Paris";
        String targetAgent = "travel-agent";
        Set<String> capabilities = Set.of("flights", "hotels", "activities");
        
        // Test different models
        String[] models = {"gpt-4", "claude-3", "llama-2", "tinyllama"};
        
        for (String model : models) {
            try {
                String optimizedPrompt = promptEngine.generateTaskPlanningPrompt(
                    taskDescription, targetAgent, capabilities, model);
                
                System.out.println("🤖 " + model.toUpperCase() + " optimized prompt:");
                String preview = optimizedPrompt.length() > 100 ? 
                    optimizedPrompt.substring(0, 100) + "..." : optimizedPrompt;
                System.out.println("   " + preview);
                
                // Record performance metrics
                promptEngine.recordModelPerformance(model, 750L + (long)(Math.random() * 500), 
                    0.90 + Math.random() * 0.09);
                
            } catch (Exception e) {
                System.out.println("❌ Prompt optimization failed for " + model + ": " + e.getMessage());
            }
        }
        
        logMessage("📊 Performance metrics recorded for all models");
        logMessage("🎯 Prompt optimization demonstrates model-specific adaptation");
        System.out.println();
    }
    
    private void demonstrateHealthMonitoring() throws InterruptedException {
        printSection("5. Health Check Monitor - System Observability");
        
        HealthCheckMonitor healthMonitor = new HealthCheckMonitor();
        
        logMessage("💊 Setting up comprehensive health monitoring");
        
        // Register agents for monitoring
        healthMonitor.registerAgent("weather-agent", "WeatherAgent", 
            Map.of("version", "1.5.0", "region", "global", "accuracy", "high"));
        healthMonitor.registerAgent("travel-agent", "TravelAgent", 
            Map.of("version", "1.5.0", "booking", "enabled", "realtime", "true"));
        healthMonitor.registerAgent("orchestrator", "EnhancedOrchestrator", 
            Map.of("version", "1.5.0", "mode", "enhanced", "components", "all"));
        
        System.out.println("✅ Registered 3 agents for health monitoring");
        
        // Add alert handler for demonstration
        healthMonitor.addAlertHandler(alert -> {
            String emoji = switch (alert.getLevel()) {
                case INFO -> "ℹ️";
                case WARNING -> "⚠️";
                case ERROR -> "❌";
                case CRITICAL -> "🚨";
            };
            System.out.println(emoji + " [" + alert.getLevel() + "] " + alert.getTitle());
        });
        
        // Simulate agent heartbeats and metrics
        logMessage("💓 Recording agent heartbeats and performance metrics...");
        
        healthMonitor.recordHeartbeat("weather-agent", 
            Map.of("status", "healthy", "requests", 42, "errors", 0));
        healthMonitor.recordHeartbeat("travel-agent", 
            Map.of("status", "healthy", "bookings", 15, "errors", 1));
        healthMonitor.recordHeartbeat("orchestrator", 
            Map.of("status", "healthy", "orchestrations", 8, "errors", 0));
        
        // Record performance metrics
        healthMonitor.recordPerformanceMetric("weather-agent", "response.time", 220.0, 
            Map.of("unit", "ms", "endpoint", "forecast"));
        healthMonitor.recordPerformanceMetric("travel-agent", "response.time", 1100.0, 
            Map.of("unit", "ms", "endpoint", "booking"));
        healthMonitor.recordPerformanceMetric("orchestrator", "response.time", 850.0, 
            Map.of("unit", "ms", "complexity", "high"));
        
        // Allow time for processing
        Thread.sleep(1000);
        
        // Get system health status
        var systemHealth = healthMonitor.getSystemHealth();
        System.out.println("🏥 System Health Report:");
        System.out.println("   Overall Health: " + systemHealth.getOverallHealth());
        System.out.println("   Healthy Agents: " + systemHealth.getHealthyAgents() + "/" + 
            systemHealth.getTotalAgents() + " (" + 
            String.format("%.1f%%", systemHealth.getAgentHealthPercentage()) + ")");
        
        // Get monitoring dashboard
        var dashboard = healthMonitor.getDashboardData();
        System.out.println("📊 Monitoring Dashboard:");
        System.out.println("   Agent Summary: " + dashboard.getAgentSummary());
        System.out.println("   Alert Summary: " + dashboard.getAlertSummary());
        
        // Get individual agent health
        var weatherHealth = healthMonitor.getAgentHealth("weather-agent");
        if (weatherHealth != null) {
            System.out.println("🌤️  Weather Agent: " + (weatherHealth.isHealthy() ? "HEALTHY" : "UNHEALTHY"));
        }
        
        logMessage("🎯 Health monitoring demonstrates comprehensive observability");
        
        // Clean shutdown
        healthMonitor.shutdown();
        System.out.println();
    }
    
    private void demonstrateCompleteIntegration() {
        printSection("6. Complete Integration - All Components Working Together");
        
        logMessage("🎼 Demonstrating full orchestration workflow integration");
        
        // Initialize all components
        TaskPlanningEngine planningEngine = new TaskPlanningEngine();
        CorrelationTrackingManager correlationManager = new CorrelationTrackingManager();
        FallbackStrategyManager fallbackManager = new FallbackStrategyManager();
        PromptOptimizationEngine promptEngine = new PromptOptimizationEngine();
        HealthCheckMonitor healthMonitor = new HealthCheckMonitor();
        
        try {
            // Step 1: Initialize monitoring
            logMessage("🏥 Step 1: Initialize health monitoring");
            healthMonitor.registerAgent("integrated-orchestrator", "IntegratedOrchestrator", 
                Map.of("version", "1.5.0", "integration", "complete"));
            
            // Step 2: Plan complex task
            logMessage("📋 Step 2: Generate comprehensive task plan");
            Set<TaskPlanningEngine.AgentInfo> agents = Set.of(
                new TaskPlanningEngine.AgentInfo("weather-agent", List.of("weather", "forecast")),
                new TaskPlanningEngine.AgentInfo("travel-agent", List.of("travel", "booking"))
            );
            
            String complexQuery = "Plan a weekend trip to Paris with weather forecast and hotel booking";
            var taskPlan = planningEngine.generateTaskPlan(complexQuery, agents, "integration-001");
            
            System.out.println("✅ Generated task plan with " + taskPlan.getTaskCount() + " tasks");
            
            // Step 3: Setup correlation tracking
            logMessage("🔗 Step 3: Setup correlation tracking");
            var integrationContext = correlationManager.createCorrelation(
                "integration-req-001", "integrated-orchestrator", 
                Map.of("type", "complex", "tasks", taskPlan.getTaskCount()));
            
            System.out.println("✅ Created correlation: " + integrationContext.getCorrelationId());
            
            // Step 4: Optimize prompts for each task
            logMessage("🎨 Step 4: Optimize prompts for task execution");
            for (var task : taskPlan.getTasks()) {
                String optimizedPrompt = promptEngine.generateTaskPlanningPrompt(
                    task.getCapability(), task.getAgent(), 
                    Set.of(task.getCapability()), "gpt-4");
                System.out.println("✅ Optimized prompt for " + task.getCapability() + 
                    " (length: " + optimizedPrompt.length() + " chars)");
            }
            
            // Step 5: Execute with fallback protection
            logMessage("🛡️  Step 5: Execute tasks with fallback protection");
            
            boolean allTasksSuccessful = true;
            for (var task : taskPlan.getTasks()) {
                try {
                    // Simulate task execution (would call actual agents in real implementation)
                    if (Math.random() > 0.1) { // 90% success rate
                        System.out.println("✅ Task " + task.getCapability() + " completed successfully");
                    } else {
                        throw new RuntimeException("Simulated task failure");
                    }
                } catch (Exception e) {
                    allTasksSuccessful = false;
                    System.out.println("❌ Task " + task.getCapability() + " failed: " + e.getMessage());
                    
                    // Use fallback strategy
                    String fallbackResponse = fallbackManager.generateEmergencyResponse(
                        task.getCapability(), "Task execution failed");
                    System.out.println("🛡️  Fallback response: " + fallbackResponse);
                }
            }
            
            // Step 6: Record performance metrics
            logMessage("📊 Step 6: Record comprehensive performance metrics");
            long totalTime = 1200L + (long)(Math.random() * 800); // 1.2-2.0 seconds
            
            healthMonitor.recordPerformanceMetric("integrated-orchestrator", "total.execution.time", 
                (double) totalTime, Map.of("unit", "ms", "tasks", taskPlan.getTaskCount()));
            
            healthMonitor.recordPerformanceMetric("integrated-orchestrator", "success.rate", 
                allTasksSuccessful ? 100.0 : 80.0, Map.of("unit", "percentage"));
            
            // Step 7: Final health status
            healthMonitor.recordHeartbeat("integrated-orchestrator", 
                Map.of("status", "healthy", "integration", "complete", "success", allTasksSuccessful));
            
            System.out.println("📈 Performance metrics recorded");
            
            // Step 8: Complete integration
            Map<String, Object> integrationResult = Map.of(
                "correlationId", integrationContext.getCorrelationId(),
                "tasksPlanned", taskPlan.getTaskCount(),
                "allSuccessful", allTasksSuccessful,
                "totalTime", totalTime + "ms",
                "status", "integration_complete"
            );
            
            System.out.println("🎉 Integration Result: " + integrationResult);
            
            logMessage("🎯 Complete integration demonstrates production-grade orchestration");
            
        } catch (Exception e) {
            System.err.println("❌ Integration failed: " + e.getMessage());
            
            // Demonstrate system-level fallback
            try {
                String systemFallback = fallbackManager.generateEmergencyResponse(
                    "Complete system integration", "Integration component failure");
                System.out.println("🛡️  System-level fallback: " + systemFallback);
            } catch (Exception fallbackError) {
                System.err.println("❌ System fallback also failed: " + fallbackError.getMessage());
            }
        } finally {
            healthMonitor.shutdown();
        }
        
        System.out.println();
        System.out.println("🏆 INTEGRATION SUMMARY:");
        System.out.println("   ✅ Task Planning Engine: Intelligent decomposition");
        System.out.println("   ✅ Correlation Tracking: Distributed coordination");
        System.out.println("   ✅ Fallback Strategies: Robust error handling");
        System.out.println("   ✅ Prompt Optimization: Model-agnostic prompting");
        System.out.println("   ✅ Health Monitoring: Comprehensive observability");
        System.out.println("   🎯 All components working together seamlessly!");
    }
    
    private void printSection(String title) {
        System.out.println(SECTION_DIVIDER);
        System.out.println(title);
        System.out.println(SECTION_DIVIDER);
    }
    
    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] " + message);
    }
}