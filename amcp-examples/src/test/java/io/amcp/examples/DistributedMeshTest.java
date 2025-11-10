package io.amcp.examples;

import io.amcp.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Multi-instance distributed mesh testing.
 * 
 * Simulates multiple AMCP instances communicating via event broker.
 * Tests:
 * - Multi-instance agent discovery
 * - Cross-instance event routing
 * - Load balancing
 * - Fault tolerance
 */
class DistributedMeshTest {

    private AgentContext instance1;
    private AgentContext instance2;
    private AgentContext instance3;
    private EventBroker sharedBroker;

    @BeforeEach
    void setUp() {
        // Create shared in-memory broker for all instances
        sharedBroker = new InMemoryEventBroker();
        
        // Start broker
        sharedBroker.start().join();
        
        // Create 3 agent contexts (simulating 3 instances)
        instance1 = AgentContext.boot(sharedBroker);
        instance2 = AgentContext.boot(sharedBroker);
        instance3 = AgentContext.boot(sharedBroker);
        
        // Start all contexts
        instance1.start().join();
        instance2.start().join();
        instance3.start().join();
    }

    /**
     * Test: Multi-instance agent registration and discovery
     */
    @Test
    void testMultiInstanceAgentDiscovery() {
        // Register agents on different instances
        TestAgent agent1 = new TestAgent("agent1");
        TestAgent agent2 = new TestAgent("agent2");
        TestAgent agent3 = new TestAgent("agent3");
        
        instance1.registerAgent(agent1);
        instance2.registerAgent(agent2);
        instance3.registerAgent(agent3);
        
        // Verify all agents registered
        assertEquals(1, instance1.getAgents().size());
        assertEquals(1, instance2.getAgents().size());
        assertEquals(1, instance3.getAgents().size());
        
        // Activate all agents
        instance1.getAgents().values().forEach(agent -> {
            String agentId = agent.getAgentId();
            instance1.activateAgent(agentId);
        });
        instance2.getAgents().values().forEach(agent -> {
            String agentId = agent.getAgentId();
            instance2.activateAgent(agentId);
        });
        instance3.getAgents().values().forEach(agent -> {
            String agentId = agent.getAgentId();
            instance3.activateAgent(agentId);
        });
        
        // Verify all agents active
        instance1.getAgents().values().forEach(agent -> 
            assertEquals(AbstractMobileAgent.AgentState.ACTIVE, agent.getState())
        );
        instance2.getAgents().values().forEach(agent -> 
            assertEquals(AbstractMobileAgent.AgentState.ACTIVE, agent.getState())
        );
        instance3.getAgents().values().forEach(agent -> 
            assertEquals(AbstractMobileAgent.AgentState.ACTIVE, agent.getState())
        );
    }

    /**
     * Test: Cross-instance event routing
     */
    @Test
    void testCrossInstanceEventRouting() throws Exception {
        // Register agents
        TestAgent agent1 = new TestAgent("agent1");
        TestAgent agent2 = new TestAgent("agent2");
        
        instance1.registerAgent(agent1);
        instance2.registerAgent(agent2);
        
        instance1.activateAgent(agent1.getAgentId());
        instance2.activateAgent(agent2.getAgentId());
        
        // Agent1 publishes event
        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        
        String subId = sharedBroker.subscribe("test.response", event -> {
            responseFuture.complete(event.getPayload(String.class));
        });
        
        // Publish event from instance1
        Event event = Event.create("test.request", "Hello from instance1");
        instance1.getEventBroker().publish(event).join();
        
        // Wait for response (agent2 should handle it)
        String response = responseFuture.get(5, TimeUnit.SECONDS);
        assertNotNull(response);
        
        sharedBroker.unsubscribe(subId);
    }

    /**
     * Test: Load balancing across instances
     */
    @Test
    void testLoadBalancing() throws Exception {
        AtomicInteger instance1Count = new AtomicInteger(0);
        AtomicInteger instance2Count = new AtomicInteger(0);
        AtomicInteger instance3Count = new AtomicInteger(0);
        
        // Register load-balancing agents
        TestAgent agent1 = new TestAgent("lb-agent1") {
            @Override
            public void onActivate() {
                super.onActivate();
                subscribe("lb.request");
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<Void> handleEvent(Event event) {
                instance1Count.incrementAndGet();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }
        };
        
        TestAgent agent2 = new TestAgent("lb-agent2") {
            @Override
            public void onActivate() {
                super.onActivate();
                subscribe("lb.request");
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<Void> handleEvent(Event event) {
                instance2Count.incrementAndGet();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }
        };
        
        TestAgent agent3 = new TestAgent("lb-agent3") {
            @Override
            public void onActivate() {
                super.onActivate();
                subscribe("lb.request");
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<Void> handleEvent(Event event) {
                instance3Count.incrementAndGet();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }
        };
        
        instance1.registerAgent(agent1);
        instance2.registerAgent(agent2);
        instance3.registerAgent(agent3);
        
        instance1.activateAgent(agent1.getAgentId());
        instance2.activateAgent(agent2.getAgentId());
        instance3.activateAgent(agent3.getAgentId());
        
        // Publish 30 events
        for (int i = 0; i < 30; i++) {
            Event event = Event.create("lb.request", "Request " + i);
            sharedBroker.publish(event).join();
        }
        
        // Allow time for processing
        Thread.sleep(500);
        
        // Verify all agents received some events
        assertTrue(instance1Count.get() > 0, "Instance1 should receive events");
        assertTrue(instance2Count.get() > 0, "Instance2 should receive events");
        assertTrue(instance3Count.get() > 0, "Instance3 should receive events");
        
        // Verify total equals published
        assertEquals(30, instance1Count.get() + instance2Count.get() + instance3Count.get());
    }

    /**
     * Test: Fault tolerance (agent failure)
     */
    @Test
    void testFaultTolerance() throws Exception {
        // Register agents
        TestAgent agent1 = new TestAgent("ft-agent1");
        TestAgent agent2 = new TestAgent("ft-agent2");
        
        instance1.registerAgent(agent1);
        instance2.registerAgent(agent2);
        
        instance1.activateAgent(agent1.getAgentId());
        instance2.activateAgent(agent2.getAgentId());
        
        // Deactivate agent1 (simulating failure)
        instance1.deactivateAgent(agent1.getAgentId());
        
        // Agent2 should still be active
        assertEquals(AbstractMobileAgent.AgentState.DEACTIVATED, agent1.getState());
        assertEquals(AbstractMobileAgent.AgentState.ACTIVE, agent2.getState());
        
        // Reactivate agent1
        instance1.activateAgent(agent1.getAgentId());
        
        // Both should be active again
        assertEquals(AbstractMobileAgent.AgentState.ACTIVE, agent1.getState());
        assertEquals(AbstractMobileAgent.AgentState.ACTIVE, agent2.getState());
    }

    /**
     * Test: Multi-instance mesh topology
     */
    @Test
    void testMeshTopology() {
        // Create mesh with 3 instances
        int totalAgents = 0;
        
        // Register multiple agents per instance
        for (int i = 0; i < 3; i++) {
            TestAgent agent = new TestAgent("mesh-agent-" + i);
            instance1.registerAgent(agent);
            totalAgents++;
        }
        
        for (int i = 3; i < 6; i++) {
            TestAgent agent = new TestAgent("mesh-agent-" + i);
            instance2.registerAgent(agent);
            totalAgents++;
        }
        
        for (int i = 6; i < 9; i++) {
            TestAgent agent = new TestAgent("mesh-agent-" + i);
            instance3.registerAgent(agent);
            totalAgents++;
        }
        
        // Verify mesh topology
        int meshAgents = instance1.getAgents().size() + 
                        instance2.getAgents().size() + 
                        instance3.getAgents().size();
        
        assertEquals(9, meshAgents);
        assertEquals(3, instance1.getAgents().size());
        assertEquals(3, instance2.getAgents().size());
        assertEquals(3, instance3.getAgents().size());
    }

    /**
     * Test: Event propagation across mesh
     */
    @Test
    void testEventPropagation() throws Exception {
        AtomicInteger eventCount = new AtomicInteger(0);
        
        // Register agents that count events
        TestAgent agent1 = new TestAgent("prop-agent1") {
            @Override
            public void onActivate() {
                super.onActivate();
                subscribe("prop.**");
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<Void> handleEvent(Event event) {
                eventCount.incrementAndGet();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }
        };
        
        instance1.registerAgent(agent1);
        instance1.activateAgent(agent1.getAgentId());
        
        // Publish events from all instances
        for (int i = 0; i < 10; i++) {
            Event event = Event.create("prop.event", "Event " + i);
            instance1.getEventBroker().publish(event).join();
            instance2.getEventBroker().publish(event).join();
            instance3.getEventBroker().publish(event).join();
        }
        
        // Allow time for processing
        Thread.sleep(500);
        
        // Verify events propagated
        assertEquals(30, eventCount.get());
    }

    /**
     * Simple test agent for testing
     */
    static class TestAgent extends AbstractMobileAgent {
        private final String name;
        
        TestAgent(String name) {
            this.name = name;
        }
        
        @Override
        public void onActivate() {
            super.onActivate();
            subscribe("test.**");
        }
        
        @Override
        public CompletableFuture<Void> handleEvent(Event event) {
            return CompletableFuture.runAsync(() -> {
                if (event.getTopic().equals("test.request")) {
                    publishEvent("test.response", "Response from " + name);
                }
            });
        }
        
        @Override
        public String toString() {
            return "TestAgent{" + name + "}";
        }
    }
}
