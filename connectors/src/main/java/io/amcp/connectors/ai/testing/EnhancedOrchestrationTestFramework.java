package io.amcp.connectors.ai.testing;

import io.amcp.connectors.ai.planning.TaskPlanningEngine;
import io.amcp.connectors.ai.planning.TaskDefinition;
import io.amcp.connectors.ai.planning.TaskPlan;
import io.amcp.connectors.ai.correlation.CorrelationTrackingManager;
import io.amcp.connectors.ai.correlation.CorrelationTrackingManager.CorrelationContext;
import io.amcp.connectors.ai.normalization.DataNormalizationEngine;
import io.amcp.connectors.ai.fallback.FallbackStrategyManager;
import io.amcp.connectors.ai.prompts.PromptOptimizationEngine;
import io.amcp.core.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive Testing Framework for AMCP v1.5 Enhanced Orchestrator Components
 * 
 * Provides validation capabilities for:
 * - Unit testing of all enhancement components
 * - Integration testing of multi-agent workflows
 * - CloudEvents schema verification and compliance
 * - End-to-end orchestration validation
 * - Performance benchmarking and regression testing
 * - Error handling and fallback strategy validation
 * 
 * This framework ensures the reliability and correctness of all 
 * enhanced orchestration features before deployment.
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 * @since 1.5.0
 */
public class EnhancedOrchestrationTestFramework {
    
    // Test components
    private final TaskPlanningEngine taskPlanningEngine;
    private final CorrelationTrackingManager correlationManager;
    private final DataNormalizationEngine normalizationEngine;
    private final FallbackStrategyManager fallbackManager;
    private final PromptOptimizationEngine promptEngine;
    
    // Test execution tracking
    private final Map<String, TestResult> testResults = new HashMap<>();
    private final AtomicInteger testCounter = new AtomicInteger(0);
    
    // Test data
    private final List<TestScenario> testScenarios = new ArrayList<>();
    private final Map<String, Object> testConfig = new HashMap<>();
    
    public EnhancedOrchestrationTestFramework() {
        this.taskPlanningEngine = new TaskPlanningEngine();
        this.correlationManager = new CorrelationTrackingManager();
        this.normalizationEngine = new DataNormalizationEngine();
        this.fallbackManager = new FallbackStrategyManager();
        this.promptEngine = new PromptOptimizationEngine();
        
        initializeTestScenarios();
        initializeTestConfig();
    }
    
    /**
     * Run all enhancement component tests
     */
    public TestSuiteResult runAllTests() {
        logMessage("Starting comprehensive enhancement testing suite");
        
        List<TestResult> allResults = new ArrayList<>();
        
        // Component unit tests
        allResults.addAll(runTaskPlanningTests());
        allResults.addAll(runCorrelationTrackingTests());
        allResults.addAll(runDataNormalizationTests());
        allResults.addAll(runFallbackStrategyTests());
        allResults.addAll(runPromptOptimizationTests());
        
        // Integration tests
        allResults.addAll(runIntegrationTests());
        
        // CloudEvents compliance tests
        allResults.addAll(runCloudEventsComplianceTests());
        
        // End-to-end validation tests
        allResults.addAll(runEndToEndTests());
        
        // Performance benchmarks
        allResults.addAll(runPerformanceTests());
        
        return createTestSuiteResult(allResults);
    }
    
    /**
     * Test Task Planning Engine functionality
     */
    public List<TestResult> runTaskPlanningTests() {
        logMessage("Running Task Planning Engine tests");
        List<TestResult> results = new ArrayList<>();
        
        // Test 1: Simple weather query planning
        try {
            String query = "What's the weather in Paris?";
            Set<TaskPlanningEngine.AgentInfo> agents = createMockAgents();
            
            TaskPlan taskPlan = taskPlanningEngine.generateTaskPlan(query, agents, "test-correlation-001");
            List<TaskDefinition> tasks = taskPlan.getTasks();
            
            boolean success = !tasks.isEmpty() && 
                            tasks.get(0).getName().contains("weather") &&
                            tasks.get(0).getAgentType().equals("WeatherAgent");
            
            results.add(new TestResult("TaskPlanning_SimpleWeather", success, 
                success ? null : "Failed to generate correct weather task"));
                
        } catch (Exception e) {
            results.add(new TestResult("TaskPlanning_SimpleWeather", false, e.getMessage()));
        }
        
        // Test 2: Complex multi-agent planning
        try {
            String query = "Book a flight to London and check the weather there";
            Set<TaskPlanningEngine.AgentInfo> agents = createMockAgents();
            
            TaskPlan taskPlan2 = taskPlanningEngine.generateTaskPlan(query, agents, "test-correlation-002");
            List<TaskDefinition> tasks = taskPlan2.getTasks();
            
            boolean success = tasks.size() >= 2 && 
                            tasks.stream().anyMatch(t -> t.getName().contains("travel")) &&
                            tasks.stream().anyMatch(t -> t.getName().contains("weather"));
            
            results.add(new TestResult("TaskPlanning_MultiAgent", success,
                success ? null : "Failed to generate multi-agent task plan"));
                
        } catch (Exception e) {
            results.add(new TestResult("TaskPlanning_MultiAgent", false, e.getMessage()));
        }
        
        // Test 3: Dependency handling
        try {
            String query = "Find the cheapest hotel in the city with the best weather forecast";
            Set<TaskPlanningEngine.AgentInfo> agents = createMockAgents();
            
            TaskPlan taskPlan3 = taskPlanningEngine.generateTaskPlan(query, agents, "test-correlation-003");
            List<TaskDefinition> tasks = taskPlan3.getTasks();
            
            boolean hasDependencies = tasks.stream().anyMatch(t -> !t.getDependencies().isEmpty());
            
            results.add(new TestResult("TaskPlanning_Dependencies", hasDependencies,
                hasDependencies ? null : "Failed to generate task dependencies"));
                
        } catch (Exception e) {
            results.add(new TestResult("TaskPlanning_Dependencies", false, e.getMessage()));
        }
        
        return results;
    }
    
    /**
     * Test Correlation Tracking Manager functionality
     */
    public List<TestResult> runCorrelationTrackingTests() {
        logMessage("Running Correlation Tracking Manager tests");
        List<TestResult> results = new ArrayList<>();
        
        // Test 1: Basic correlation creation and tracking
        try {
            CorrelationContext correlationContext = correlationManager.createCorrelation("test-request-" + System.nanoTime(), 
                                                                                         "test", 
                                                                                         new HashMap<>(), 
                                                                                         5);
            String correlationId = correlationContext.getCorrelationId();
            boolean created = correlationId != null && !correlationId.isEmpty();
            
            results.add(new TestResult("CorrelationTracking_Create", created,
                created ? null : "Failed to create correlation"));
                
            if (created) {
                // Test response tracking
                Map<String, Object> responseData = Map.of("result", "test-response");
                correlationManager.trackResponse(correlationId, responseData);
                
                @SuppressWarnings("unchecked")
                CompletableFuture<Object> future = correlationManager.waitForResponse(correlationId);
                Object responseObj = future.get(1, TimeUnit.SECONDS);
                Map<String, Object> response = (Map<String, Object>) responseObj;
                
                boolean responseTracked = response != null && "test-response".equals(response.get("result"));
                
                results.add(new TestResult("CorrelationTracking_Response", responseTracked,
                    responseTracked ? null : "Failed to track response"));
            }
            
        } catch (Exception e) {
            results.add(new TestResult("CorrelationTracking_Create", false, e.getMessage()));
        }
        
        // Test 2: Timeout handling
        try {
            CorrelationContext timeoutContext = correlationManager.createCorrelation("timeout-test-" + System.nanoTime(), 
                                                                                    "timeout", 
                                                                                    new HashMap<>(), 
                                                                                    1);
            String correlationId = timeoutContext.getCorrelationId();
            @SuppressWarnings("unchecked")
            CompletableFuture<Object> future = correlationManager.waitForResponse(correlationId);
            
            // Don't provide response, let it timeout
            boolean timedOut = false;
            try {
                future.get(2, TimeUnit.SECONDS);
            } catch (Exception e) {
                timedOut = true;
            }
            
            results.add(new TestResult("CorrelationTracking_Timeout", timedOut,
                timedOut ? null : "Timeout handling failed"));
                
        } catch (Exception e) {
            results.add(new TestResult("CorrelationTracking_Timeout", false, e.getMessage()));
        }
        
        return results;
    }
    
    /**
     * Test Data Normalization Engine functionality
     */
    public List<TestResult> runDataNormalizationTests() {
        logMessage("Running Data Normalization Engine tests");
        List<TestResult> results = new ArrayList<>();
        
        // Test 1: Location normalization
        try {
            Map<String, Object> input = Map.of("location", "paris, france");
            Map<String, Object> normalized = normalizationEngine.normalizeParameters(input, "weather.get");
            
            String normalizedLocation = (String) normalized.get("location");
            boolean success = "Paris,FR".equals(normalizedLocation);
            
            results.add(new TestResult("DataNormalization_Location", success,
                success ? null : "Location normalization failed: " + normalizedLocation));
                
        } catch (Exception e) {
            results.add(new TestResult("DataNormalization_Location", false, e.getMessage()));
        }
        
        // Test 2: Date normalization
        try {
            Map<String, Object> input = Map.of("date", "tomorrow");
            Map<String, Object> normalized = normalizationEngine.normalizeParameters(input, "weather.forecast");
            
            String normalizedDate = (String) normalized.get("date");
            boolean success = normalizedDate != null && normalizedDate.matches("\\d{4}-\\d{2}-\\d{2}");
            
            results.add(new TestResult("DataNormalization_Date", success,
                success ? null : "Date normalization failed: " + normalizedDate));
                
        } catch (Exception e) {
            results.add(new TestResult("DataNormalization_Date", false, e.getMessage()));
        }
        
        // Test 3: Currency normalization
        try {
            Map<String, Object> input = Map.of("price", "100 euros");
            Map<String, Object> normalized = normalizationEngine.normalizeParameters(input, "travel.price");
            
            String currency = (String) normalized.get("currency");
            boolean success = "EUR".equals(currency);
            
            results.add(new TestResult("DataNormalization_Currency", success,
                success ? null : "Currency normalization failed: " + currency));
                
        } catch (Exception e) {
            results.add(new TestResult("DataNormalization_Currency", false, e.getMessage()));
        }
        
        return results;
    }
    
    /**
     * Test Fallback Strategy Manager functionality
     */
    public List<TestResult> runFallbackStrategyTests() {
        logMessage("Running Fallback Strategy Manager tests");
        List<TestResult> results = new ArrayList<>();
        
        // Test 1: Emergency response generation
        try {
            String emergencyResponse = fallbackManager.generateEmergencyResponse("llm_failure", "test query");
            boolean success = emergencyResponse != null && 
                            emergencyResponse.contains("test query") &&
                            emergencyResponse.length() > 20;
            
            results.add(new TestResult("FallbackStrategy_Emergency", success,
                success ? null : "Emergency response generation failed"));
                
        } catch (Exception e) {
            results.add(new TestResult("FallbackStrategy_Emergency", false, e.getMessage()));
        }
        
        // Test 2: Malformed LLM output handling
        try {
            String originalPrompt = "Generate JSON for weather query";
            String malformedOutput = "This is not JSON at all";
            String correlationId = "test-correlation";
            
            String improvedResponse = fallbackManager.handleMalformedLLMOutput(
                originalPrompt, malformedOutput, correlationId, null);
            
            boolean success = improvedResponse != null && improvedResponse.length() > 0;
            
            results.add(new TestResult("FallbackStrategy_MalformedLLM", success,
                success ? null : "Malformed LLM output handling failed"));
                
        } catch (Exception e) {
            results.add(new TestResult("FallbackStrategy_MalformedLLM", false, e.getMessage()));
        }
        
        return results;
    }
    
    /**
     * Test Prompt Optimization Engine functionality
     */
    public List<TestResult> runPromptOptimizationTests() {
        logMessage("Running Prompt Optimization Engine tests");
        List<TestResult> results = new ArrayList<>();
        
        // Test 1: Task planning prompt generation
        try {
            String query = "What's the weather in Paris?";
            Set<String> agents = Set.of("WeatherAgent", "ChatAgent");
            String prompt = promptEngine.generateTaskPlanningPrompt(query, null, agents, "gpt-4");
            
            boolean success = prompt.contains("JSON") && 
                            prompt.contains("WeatherAgent") &&
                            prompt.contains("Paris") &&
                            prompt.length() > 500;
            
            results.add(new TestResult("PromptOptimization_TaskPlanning", success,
                success ? null : "Task planning prompt generation failed"));
                
        } catch (Exception e) {
            results.add(new TestResult("PromptOptimization_TaskPlanning", false, e.getMessage()));
        }
        
        // Test 2: Prompt quality validation
        try {
            String prompt = "Simple short prompt";
            PromptOptimizationEngine.PromptQualityScore score = 
                promptEngine.validatePrompt(prompt, PromptOptimizationEngine.PromptType.TASK_PLANNING);
            
            boolean success = score.getScore() < 80 && !score.getIssues().isEmpty();
            
            results.add(new TestResult("PromptOptimization_Validation", success,
                success ? null : "Prompt quality validation failed"));
                
        } catch (Exception e) {
            results.add(new TestResult("PromptOptimization_Validation", false, e.getMessage()));
        }
        
        return results;
    }
    
    /**
     * Run integration tests across multiple components
     */
    public List<TestResult> runIntegrationTests() {
        logMessage("Running integration tests");
        List<TestResult> results = new ArrayList<>();
        
        // Test 1: End-to-end orchestration flow
        try {
            String query = "What's the weather in Paris?";
            
            // Step 1: Normalize input
            Map<String, Object> input = Map.of("query", query, "location", "paris");
            Map<String, Object> normalized = normalizationEngine.normalizeParameters(input, "weather.get");
            
            // Step 2: Generate task plan
            Set<TaskPlanningEngine.AgentInfo> agents = createMockAgents();
            TaskPlan taskPlan4 = taskPlanningEngine.generateTaskPlan(query, agents, "test-correlation-004");
            List<TaskDefinition> tasks = taskPlan4.getTasks();
            
            // Step 3: Create correlation for tracking
            CorrelationContext queryContext = correlationManager.createCorrelation("query-" + System.nanoTime(), 
                                                                                   "integration-test", 
                                                                                   Map.of("query", query), 
                                                                                   5);
            String correlationId = queryContext.getCorrelationId();
            
            // Step 4: Generate optimized prompt
            String prompt = promptEngine.generateTaskPlanningPrompt(query, null, 
                agents.stream().map(a -> a.getAgentId()).collect(java.util.stream.Collectors.toSet()), "gpt-4");
            
            boolean success = normalized != null && 
                            !tasks.isEmpty() && 
                            correlationId != null && 
                            prompt.length() > 100;
            
            results.add(new TestResult("Integration_EndToEndFlow", success,
                success ? null : "End-to-end integration failed"));
                
        } catch (Exception e) {
            results.add(new TestResult("Integration_EndToEndFlow", false, e.getMessage()));
        }
        
        return results;
    }
    
    /**
     * Test CloudEvents compliance
     */
    public List<TestResult> runCloudEventsComplianceTests() {
        logMessage("Running CloudEvents compliance tests");
        List<TestResult> results = new ArrayList<>();
        
        // Test 1: CloudEvents metadata structure
        try {
            Event testEvent = Event.builder()
                .topic("io.amcp.agent.response")
                .payload(Map.of("test", "data"))
                .correlationId("test-correlation")
                .metadata("ce-specversion", "1.0")
                .metadata("ce-type", "io.amcp.agent.response")
                .metadata("ce-source", "amcp://agents/test")
                .metadata("ce-time", LocalDateTime.now().toString())
                .build();
            
            boolean hasRequiredFields = testEvent.getMetadata().containsKey("ce-specversion") &&
                                      testEvent.getMetadata().containsKey("ce-type") &&
                                      testEvent.getMetadata().containsKey("ce-source");
            
            results.add(new TestResult("CloudEvents_Metadata", hasRequiredFields,
                hasRequiredFields ? null : "Missing required CloudEvents metadata"));
                
        } catch (Exception e) {
            results.add(new TestResult("CloudEvents_Metadata", false, e.getMessage()));
        }
        
        // Test 2: Reverse-DNS topic naming
        try {
            String[] validTopics = {
                "io.amcp.agent.response",
                "io.amcp.orchestrator.task.complete",
                "io.amcp.system.health.check"
            };
            
            boolean allValid = true;
            for (String topic : validTopics) {
                if (!topic.startsWith("io.amcp.") || !topic.contains(".")) {
                    allValid = false;
                    break;
                }
            }
            
            results.add(new TestResult("CloudEvents_TopicNaming", allValid,
                allValid ? null : "Invalid reverse-DNS topic naming"));
                
        } catch (Exception e) {
            results.add(new TestResult("CloudEvents_TopicNaming", false, e.getMessage()));
        }
        
        return results;
    }
    
    /**
     * Run end-to-end validation tests
     */
    public List<TestResult> runEndToEndTests() {
        logMessage("Running end-to-end validation tests");
        List<TestResult> results = new ArrayList<>();
        
        // Test each scenario from test data
        for (TestScenario scenario : testScenarios) {
            try {
                TestResult result = executeScenario(scenario);
                results.add(result);
            } catch (Exception e) {
                results.add(new TestResult("E2E_" + scenario.getName(), false, e.getMessage()));
            }
        }
        
        return results;
    }
    
    /**
     * Run performance benchmark tests
     */
    public List<TestResult> runPerformanceTests() {
        logMessage("Running performance benchmark tests");
        List<TestResult> results = new ArrayList<>();
        
        // Test 1: Task planning performance
        try {
            long startTime = System.currentTimeMillis();
            Set<TaskPlanningEngine.AgentInfo> agents = createMockAgents();
            
            for (int i = 0; i < 100; i++) {
                taskPlanningEngine.generateTaskPlan("What's the weather in city" + i + "?", agents, "load-test-" + i);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            boolean performant = duration < 5000; // Under 5 seconds for 100 calls
            
            results.add(new TestResult("Performance_TaskPlanning", performant,
                performant ? null : "Task planning too slow: " + duration + "ms"));
                
        } catch (Exception e) {
            results.add(new TestResult("Performance_TaskPlanning", false, e.getMessage()));
        }
        
        // Test 2: Prompt generation performance
        try {
            long startTime = System.currentTimeMillis();
            Set<String> agents = Set.of("WeatherAgent", "ChatAgent");
            
            for (int i = 0; i < 50; i++) {
                promptEngine.generateTaskPlanningPrompt("Test query " + i, null, agents, "gpt-4");
            }
            
            long duration = System.currentTimeMillis() - startTime;
            boolean performant = duration < 2000; // Under 2 seconds for 50 calls
            
            results.add(new TestResult("Performance_PromptGeneration", performant,
                performant ? null : "Prompt generation too slow: " + duration + "ms"));
                
        } catch (Exception e) {
            results.add(new TestResult("Performance_PromptGeneration", false, e.getMessage()));
        }
        
        return results;
    }
    
    /**
     * Execute a specific test scenario
     */
    private TestResult executeScenario(TestScenario scenario) {
        logMessage("Executing scenario: " + scenario.getName());
        
        try {
            // Normalize input
            Map<String, Object> normalizedInput = normalizationEngine.normalizeParameters(
                scenario.getInput(), scenario.getCapability());
            
            // Generate task plan
            Set<TaskPlanningEngine.AgentInfo> agents = createMockAgents();
            TaskPlan taskPlan = taskPlanningEngine.generateTaskPlan(
                scenario.getQuery(), agents, "scenario-" + scenario.getName());
            List<TaskDefinition> tasks = taskPlan.getTasks();
            
            // Validate expected outcomes
            boolean success = validateScenarioOutcome(scenario, tasks, normalizedInput);
            
            return new TestResult("E2E_" + scenario.getName(), success,
                success ? null : "Scenario validation failed");
                
        } catch (Exception e) {
            return new TestResult("E2E_" + scenario.getName(), false, 
                "Scenario execution failed: " + e.getMessage());
        }
    }
    
    /**
     * Validate scenario outcome
     */
    private boolean validateScenarioOutcome(TestScenario scenario, 
                                          List<TaskDefinition> tasks,
                                          Map<String, Object> normalizedInput) {
        // Check if expected capability is present
        boolean hasExpectedCapability = tasks.stream()
            .anyMatch(task -> task.getName().contains(scenario.getExpectedCapability()));
        
        // Check if expected agent is assigned
        boolean hasExpectedAgent = tasks.stream()
            .anyMatch(task -> task.getAgentType().equals(scenario.getExpectedAgent()));
        
        return hasExpectedCapability && hasExpectedAgent;
    }
    
    /**
     * Create test suite result summary
     */
    private TestSuiteResult createTestSuiteResult(List<TestResult> allResults) {
        int totalTests = allResults.size();
        int passedTests = (int) allResults.stream().filter(TestResult::isSuccess).count();
        int failedTests = totalTests - passedTests;
        
        double successRate = totalTests > 0 ? (double) passedTests / totalTests : 0.0;
        
        List<String> failedTestNames = allResults.stream()
            .filter(result -> !result.isSuccess())
            .map(TestResult::getTestName)
            .collect(java.util.stream.Collectors.toList());
        
        logMessage("Test suite completed: " + passedTests + "/" + totalTests + " passed (" + 
                  String.format("%.1f%%", successRate * 100) + ")");
        
        return new TestSuiteResult(totalTests, passedTests, failedTests, successRate, 
                                 allResults, failedTestNames);
    }
    
    // Helper methods
    
    private Set<TaskPlanningEngine.AgentInfo> createMockAgents() {
        Set<TaskPlanningEngine.AgentInfo> agents = new HashSet<>();
        
        agents.add(new TaskPlanningEngine.AgentInfo("WeatherAgent", 
            List.of("weather.get", "weather.forecast")));
        agents.add(new TaskPlanningEngine.AgentInfo("TravelAgent", 
            List.of("travel.flight.search", "travel.hotel.search")));
        agents.add(new TaskPlanningEngine.AgentInfo("StockAgent", 
            List.of("stock.price", "stock.analysis")));
        agents.add(new TaskPlanningEngine.AgentInfo("ChatAgent", 
            List.of("chat.response", "general.query")));
        
        return agents;
    }
    
    private void initializeTestScenarios() {
        testScenarios.add(new TestScenario(
            "SimpleWeatherQuery",
            "What's the weather in Paris?",
            Map.of("location", "Paris"),
            "weather.get",
            "weather.get",
            "WeatherAgent"
        ));
        
        testScenarios.add(new TestScenario(
            "StockPriceQuery",
            "What's the price of Apple stock?",
            Map.of("symbol", "AAPL"),
            "stock.price",
            "stock.price",
            "StockAgent"
        ));
        
        testScenarios.add(new TestScenario(
            "TravelBooking",
            "Find flights from New York to London",
            Map.of("origin", "New York", "destination", "London"),
            "travel.flight.search",
            "travel.flight.search",
            "TravelAgent"
        ));
    }
    
    private void initializeTestConfig() {
        testConfig.put("maxExecutionTime", 30000); // 30 seconds
        testConfig.put("retryAttempts", 3);
        testConfig.put("enablePerformanceTesting", true);
        testConfig.put("enableIntegrationTesting", true);
    }
    
    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [EnhancedOrchestrationTestFramework] " + message);
    }
    
    // Supporting classes
    
    public static class TestResult {
        private final String testName;
        private final boolean success;
        private final String errorMessage;
        private final long executionTime;
        
        public TestResult(String testName, boolean success, String errorMessage) {
            this.testName = testName;
            this.success = success;
            this.errorMessage = errorMessage;
            this.executionTime = System.currentTimeMillis();
        }
        
        public String getTestName() { return testName; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public long getExecutionTime() { return executionTime; }
    }
    
    public static class TestSuiteResult {
        private final int totalTests;
        private final int passedTests;
        private final int failedTests;
        private final double successRate;
        private final List<TestResult> allResults;
        private final List<String> failedTestNames;
        
        public TestSuiteResult(int totalTests, int passedTests, int failedTests, 
                             double successRate, List<TestResult> allResults, 
                             List<String> failedTestNames) {
            this.totalTests = totalTests;
            this.passedTests = passedTests;
            this.failedTests = failedTests;
            this.successRate = successRate;
            this.allResults = new ArrayList<>(allResults);
            this.failedTestNames = new ArrayList<>(failedTestNames);
        }
        
        public int getTotalTests() { return totalTests; }
        public int getPassedTests() { return passedTests; }
        public int getFailedTests() { return failedTests; }
        public double getSuccessRate() { return successRate; }
        public List<TestResult> getAllResults() { return new ArrayList<>(allResults); }
        public List<String> getFailedTestNames() { return new ArrayList<>(failedTestNames); }
        public boolean isAllTestsPassed() { return failedTests == 0; }
    }
    
    public static class TestScenario {
        private final String name;
        private final String query;
        private final Map<String, Object> input;
        private final String capability;
        private final String expectedCapability;
        private final String expectedAgent;
        
        public TestScenario(String name, String query, Map<String, Object> input,
                          String capability, String expectedCapability, String expectedAgent) {
            this.name = name;
            this.query = query;
            this.input = new HashMap<>(input);
            this.capability = capability;
            this.expectedCapability = expectedCapability;
            this.expectedAgent = expectedAgent;
        }
        
        public String getName() { return name; }
        public String getQuery() { return query; }
        public Map<String, Object> getInput() { return new HashMap<>(input); }
        public String getCapability() { return capability; }
        public String getExpectedCapability() { return expectedCapability; }
        public String getExpectedAgent() { return expectedAgent; }
    }
}