package io.amcp.examples.meshchat;

import io.amcp.core.agent.AgentContext;
import io.amcp.core.agent.AgentID;
import io.amcp.core.event.Event;
import io.amcp.core.event.EventBroker;
import io.amcp.core.event.EventBrokerFactory;
import io.amcp.examples.travel.TravelPlannerAgent;
import io.amcp.examples.stock.StockAgent;
import io.amcp.examples.orchestrator.EnhancedOrchestratorAgent;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for the MeshChat agent ecosystem
 * Tests agent functionality, inter-agent communication, and orchestration workflows
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MeshChatSystemTest {

    private EventBroker eventBroker;
    private AgentContext agentContext;
    private MeshChatAgent meshChatAgent;
    private TravelPlannerAgent travelAgent;
    private StockAgent stockAgent;
    private EnhancedOrchestratorAgent orchestratorAgent;

    @Mock
    private EventBroker mockEventBroker;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        
        // Create in-memory event broker for testing
        eventBroker = EventBrokerFactory.create("memory", Map.of());
        
        // Create agent context
        agentContext = new AgentContext("test-context", eventBroker);
        
        // Initialize agents
        meshChatAgent = new MeshChatAgent();
        travelAgent = new TravelPlannerAgent();
        stockAgent = new StockAgent();
        orchestratorAgent = new EnhancedOrchestratorAgent();
        
        // Set context for all agents
        meshChatAgent.setContext(agentContext);
        travelAgent.setContext(agentContext);
        stockAgent.setContext(agentContext);
        orchestratorAgent.setContext(agentContext);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Deactivate agents
        if (meshChatAgent != null) meshChatAgent.onDeactivate();
        if (travelAgent != null) travelAgent.onDeactivate();
        if (stockAgent != null) stockAgent.onDeactivate();
        if (orchestratorAgent != null) orchestratorAgent.onDeactivate();
        
        // Shutdown event broker
        if (eventBroker != null) {
            eventBroker.shutdown();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test MeshChatAgent Initialization")
    void testMeshChatAgentInitialization() throws Exception {
        // Test agent ID generation
        AgentID agentId = meshChatAgent.getAgentID();
        assertNotNull(agentId);
        assertEquals("MeshChatAgent", agentId.getType());
        
        // Test context setting
        assertEquals(agentContext, meshChatAgent.getContext());
        
        // Test activation
        CompletableFuture<Void> activationFuture = meshChatAgent.onActivate();
        assertNotNull(activationFuture);
        activationFuture.get(5, TimeUnit.SECONDS); // Should complete within 5 seconds
        
        assertTrue(meshChatAgent.isActive());
    }

    @Test
    @Order(2)
    @DisplayName("Test TravelPlannerAgent Functionality")
    void testTravelPlannerAgent() throws Exception {
        // Activate agent
        travelAgent.onActivate().get(5, TimeUnit.SECONDS);
        
        // Test destination search
        AtomicReference<String> responseRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        // Subscribe to travel responses
        eventBroker.subscribe("travel.response.**", event -> {
            responseRef.set(event.getPayload().toString());
            latch.countDown();
        });
        
        // Send travel request
        Event travelRequest = Event.builder()
            .topic("travel.request.plan")
            .payload(Map.of(
                "destination", "Tokyo",
                "duration", "7 days",
                "budget", "$2000"
            ))
            .correlationId("test-travel-001")
            .build();
            
        eventBroker.publish(travelRequest);
        
        // Wait for response
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Travel agent should respond within 10 seconds");
        assertNotNull(responseRef.get());
        assertTrue(responseRef.get().contains("Tokyo") || responseRef.get().contains("Japan"));
    }

    @Test
    @Order(3)
    @DisplayName("Test StockAgent Functionality")
    void testStockAgent() throws Exception {
        // Activate agent
        stockAgent.onActivate().get(5, TimeUnit.SECONDS);
        
        // Test stock quote request
        AtomicReference<String> responseRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        // Subscribe to stock responses
        eventBroker.subscribe("stock.response.**", event -> {
            responseRef.set(event.getPayload().toString());
            latch.countDown();
        });
        
        // Send stock request
        Event stockRequest = Event.builder()
            .topic("stock.request.quote")
            .payload(Map.of(
                "symbol", "AAPL",
                "analysis", "basic"
            ))
            .correlationId("test-stock-001")
            .build();
            
        eventBroker.publish(stockRequest);
        
        // Wait for response
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Stock agent should respond within 10 seconds");
        assertNotNull(responseRef.get());
        assertTrue(responseRef.get().contains("AAPL") || responseRef.get().contains("Apple"));
    }

    @Test
    @Order(4)
    @DisplayName("Test Enhanced Orchestrator Agent")
    void testEnhancedOrchestratorAgent() throws Exception {
        // Activate agent
        orchestratorAgent.onActivate().get(5, TimeUnit.SECONDS);
        
        // Test orchestration request
        AtomicReference<String> responseRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        // Subscribe to orchestrator responses
        eventBroker.subscribe("orchestrator.response.**", event -> {
            responseRef.set(event.getPayload().toString());
            latch.countDown();
        });
        
        // Send orchestration request
        Event orchestrationRequest = Event.builder()
            .topic("orchestrator.request.plan")
            .payload(Map.of(
                "query", "Plan a business trip to Tokyo and analyze Japanese tech stocks",
                "context", "business travel with investment research"
            ))
            .correlationId("test-orchestration-001")
            .build();
            
        eventBroker.publish(orchestrationRequest);
        
        // Wait for response
        assertTrue(latch.await(15, TimeUnit.SECONDS), "Orchestrator should respond within 15 seconds");
        assertNotNull(responseRef.get());
    }

    @Test
    @Order(5)
    @DisplayName("Test Multi-Agent Communication Flow")
    void testMultiAgentCommunication() throws Exception {
        // Activate all agents
        CompletableFuture.allOf(
            meshChatAgent.onActivate(),
            travelAgent.onActivate(),
            stockAgent.onActivate(),
            orchestratorAgent.onActivate()
        ).get(10, TimeUnit.SECONDS);
        
        // Test human message flow through MeshChat to other agents
        CountDownLatch responseLatch = new CountDownLatch(1);
        AtomicReference<String> finalResponseRef = new AtomicReference<>();
        
        // Subscribe to MeshChat responses (final output to human)
        eventBroker.subscribe("chat.response.human", event -> {
            finalResponseRef.set(event.getPayload().toString());
            responseLatch.countDown();
        });
        
        // Simulate human input to MeshChat
        Event humanMessage = Event.builder()
            .topic("chat.request.human")
            .payload(Map.of(
                "message", "I want to plan a trip to Japan and also get advice on investing in Japanese stocks",
                "sessionId", "test-session-001",
                "userId", "test-user"
            ))
            .correlationId("test-multi-agent-001")
            .build();
            
        eventBroker.publish(humanMessage);
        
        // Wait for final response
        assertTrue(responseLatch.await(30, TimeUnit.SECONDS), 
            "Multi-agent communication should complete within 30 seconds");
        assertNotNull(finalResponseRef.get());
        
        String response = finalResponseRef.get();
        // Response should contain elements from both travel and stock agents
        assertTrue(response.contains("Japan") || response.contains("Tokyo") || response.contains("travel") ||
                  response.contains("stock") || response.contains("invest"), 
                  "Response should contain travel or investment content");
    }

    @Test
    @Order(6)
    @DisplayName("Test Agent Discovery Protocol")
    void testAgentDiscoveryProtocol() throws Exception {
        // Activate all agents first
        CompletableFuture.allOf(
            meshChatAgent.onActivate(),
            travelAgent.onActivate(),
            stockAgent.onActivate(),
            orchestratorAgent.onActivate()
        ).get(10, TimeUnit.SECONDS);
        
        // Wait a moment for agents to register
        Thread.sleep(2000);
        
        // Test agent discovery request
        CountDownLatch discoveryLatch = new CountDownLatch(1);
        AtomicReference<String> discoveryResponseRef = new AtomicReference<>();
        
        // Subscribe to discovery responses
        eventBroker.subscribe("discovery.response.**", event -> {
            discoveryResponseRef.set(event.getPayload().toString());
            discoveryLatch.countDown();
        });
        
        // Send discovery request
        Event discoveryRequest = Event.builder()
            .topic("discovery.request.agents")
            .payload(Map.of(
                "capability", "travel",
                "requesterId", "test-requester"
            ))
            .correlationId("test-discovery-001")
            .build();
            
        eventBroker.publish(discoveryRequest);
        
        // Wait for discovery response
        assertTrue(discoveryLatch.await(10, TimeUnit.SECONDS), 
            "Agent discovery should respond within 10 seconds");
        assertNotNull(discoveryResponseRef.get());
        
        String discoveryResponse = discoveryResponseRef.get();
        assertTrue(discoveryResponse.contains("travel") || discoveryResponse.contains("TravelPlanner"),
                  "Discovery should find travel-capable agents");
    }

    @Test
    @Order(7)
    @DisplayName("Test Conversation Memory System")
    void testConversationMemorySystem() throws Exception {
        // Activate MeshChat agent (which uses memory system)
        meshChatAgent.onActivate().get(5, TimeUnit.SECONDS);
        
        // Test memory storage
        String sessionId = "test-memory-session";
        String userId = "test-memory-user";
        
        // Send multiple messages to build conversation history
        for (int i = 1; i <= 3; i++) {
            Event messageEvent = Event.builder()
                .topic("chat.request.human")
                .payload(Map.of(
                    "message", "Test message " + i,
                    "sessionId", sessionId,
                    "userId", userId
                ))
                .correlationId("test-memory-" + i)
                .build();
                
            eventBroker.publish(messageEvent);
            Thread.sleep(500); // Small delay between messages
        }
        
        // Give some time for memory processing
        Thread.sleep(2000);
        
        // Test memory retrieval (this would be tested through MeshChat agent's memory integration)
        // In a real implementation, we'd have access to the memory system directly
        // For now, we verify that the agent is functioning with memory enabled
        assertTrue(meshChatAgent.isActive(), "MeshChat agent should remain active with memory system");
    }

    @Test
    @Order(8)
    @DisplayName("Test Error Handling and Recovery")
    void testErrorHandlingAndRecovery() throws Exception {
        // Activate agents
        meshChatAgent.onActivate().get(5, TimeUnit.SECONDS);
        
        // Test malformed request handling
        CountDownLatch errorLatch = new CountDownLatch(1);
        AtomicReference<String> errorResponseRef = new AtomicReference<>();
        
        // Subscribe to error responses
        eventBroker.subscribe("chat.error.**", event -> {
            errorResponseRef.set(event.getPayload().toString());
            errorLatch.countDown();
        });
        
        // Send malformed request
        Event malformedRequest = Event.builder()
            .topic("chat.request.human")
            .payload(Map.of(
                "invalid_field", "invalid_value"
                // Missing required fields like message, sessionId, etc.
            ))
            .correlationId("test-error-001")
            .build();
            
        eventBroker.publish(malformedRequest);
        
        // Agent should either handle gracefully or respond with error
        // Wait a reasonable time for any response
        Thread.sleep(3000);
        
        // Agent should still be active after error
        assertTrue(meshChatAgent.isActive(), "Agent should remain active after handling errors");
    }

    @Test
    @Order(9)
    @DisplayName("Test Performance Under Load")
    void testPerformanceUnderLoad() throws Exception {
        // Activate all agents
        CompletableFuture.allOf(
            meshChatAgent.onActivate(),
            travelAgent.onActivate(),
            stockAgent.onActivate(),
            orchestratorAgent.onActivate()
        ).get(10, TimeUnit.SECONDS);
        
        // Send multiple concurrent requests
        int requestCount = 10;
        CountDownLatch loadLatch = new CountDownLatch(requestCount);
        
        // Subscribe to responses
        eventBroker.subscribe("chat.response.**", event -> {
            loadLatch.countDown();
        });
        
        // Send concurrent requests
        for (int i = 0; i < requestCount; i++) {
            Event loadRequest = Event.builder()
                .topic("chat.request.human")
                .payload(Map.of(
                    "message", "Load test message " + i,
                    "sessionId", "load-test-session-" + i,
                    "userId", "load-test-user"
                ))
                .correlationId("load-test-" + i)
                .build();
                
            eventBroker.publish(loadRequest);
        }
        
        // Wait for all responses (with generous timeout for load test)
        assertTrue(loadLatch.await(60, TimeUnit.SECONDS), 
            "System should handle " + requestCount + " concurrent requests within 60 seconds");
        
        // All agents should still be active
        assertTrue(meshChatAgent.isActive(), "MeshChat agent should survive load test");
        assertTrue(travelAgent.isActive(), "Travel agent should survive load test");
        assertTrue(stockAgent.isActive(), "Stock agent should survive load test");
        assertTrue(orchestratorAgent.isActive(), "Orchestrator agent should survive load test");
    }

    @Test
    @Order(10)
    @DisplayName("Test Agent Lifecycle Management")
    void testAgentLifecycleManagement() throws Exception {
        // Test activation
        assertFalse(meshChatAgent.isActive());
        meshChatAgent.onActivate().get(5, TimeUnit.SECONDS);
        assertTrue(meshChatAgent.isActive());
        
        // Test deactivation
        meshChatAgent.onDeactivate().get(5, TimeUnit.SECONDS);
        assertFalse(meshChatAgent.isActive());
        
        // Test reactivation
        meshChatAgent.onActivate().get(5, TimeUnit.SECONDS);
        assertTrue(meshChatAgent.isActive());
        
        // Test destruction
        meshChatAgent.onDestroy().get(5, TimeUnit.SECONDS);
        assertFalse(meshChatAgent.isActive());
    }

    /**
     * Integration test helper to verify event flow
     */
    private void verifyEventFlow(String sourceTopic, String targetTopic, Map<String, Object> payload) 
            throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Event> receivedEvent = new AtomicReference<>();
        
        // Subscribe to target topic
        eventBroker.subscribe(targetTopic, event -> {
            receivedEvent.set(event);
            latch.countDown();
        });
        
        // Publish to source topic
        Event sourceEvent = Event.builder()
            .topic(sourceTopic)
            .payload(payload)
            .correlationId("event-flow-test")
            .build();
            
        eventBroker.publish(sourceEvent);
        
        // Verify event received
        assertTrue(latch.await(10, TimeUnit.SECONDS), 
            "Event should flow from " + sourceTopic + " to " + targetTopic);
        assertNotNull(receivedEvent.get());
        assertEquals("event-flow-test", receivedEvent.get().getCorrelationId());
    }
}