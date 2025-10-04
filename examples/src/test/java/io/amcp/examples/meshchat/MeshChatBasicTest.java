package io.amcp.examples.meshchat;

import io.amcp.core.AgentLifecycle;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic test suite for MeshChat agent functionality
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MeshChatBasicTest {

    private MeshChatAgent meshChatAgent;
    private TravelPlannerAgent travelAgent;
    private StockAgent stockAgent;

    @BeforeEach
    void setUp() {
        // Initialize agents
        meshChatAgent = new MeshChatAgent();
        travelAgent = new TravelPlannerAgent();
        stockAgent = new StockAgent();
    }

    @Test
    @Order(1)
    @DisplayName("Test MeshChatAgent Basic Initialization")
    void testMeshChatAgentInitialization() {
        // Test agent creation
        assertNotNull(meshChatAgent);
        assertNotNull(meshChatAgent.getAgentId());
        assertEquals(AgentLifecycle.INACTIVE, meshChatAgent.getLifecycleState());
    }

    @Test
    @Order(2)
    @DisplayName("Test Agent Lifecycle")
    void testAgentLifecycle() {
        // Test activation
        assertEquals(AgentLifecycle.INACTIVE, meshChatAgent.getLifecycleState());
        meshChatAgent.onActivate();
        assertEquals(AgentLifecycle.ACTIVE, meshChatAgent.getLifecycleState());
        
        // Test deactivation
        meshChatAgent.onDeactivate();
        assertEquals(AgentLifecycle.INACTIVE, meshChatAgent.getLifecycleState());
    }

    @Test
    @Order(3)
    @DisplayName("Test TravelPlannerAgent Basic Functionality")
    void testTravelPlannerAgent() {
        assertNotNull(travelAgent);
        assertNotNull(travelAgent.getAgentId());
        assertEquals(AgentLifecycle.INACTIVE, travelAgent.getLifecycleState());
        
        // Test activation
        travelAgent.onActivate();
        assertEquals(AgentLifecycle.ACTIVE, travelAgent.getLifecycleState());
    }

    @Test
    @Order(4)
    @DisplayName("Test StockAgent Basic Functionality")
    void testStockAgent() {
        assertNotNull(stockAgent);
        assertNotNull(stockAgent.getAgentId());
        assertEquals(AgentLifecycle.INACTIVE, stockAgent.getLifecycleState());
        
        // Test activation
        stockAgent.onActivate();
        assertEquals(AgentLifecycle.ACTIVE, stockAgent.getLifecycleState());
    }

    @AfterEach
    void tearDown() {
        // Clean up agents
        if (meshChatAgent != null && meshChatAgent.getLifecycleState() == AgentLifecycle.ACTIVE) {
            meshChatAgent.onDeactivate();
        }
        if (travelAgent != null && travelAgent.getLifecycleState() == AgentLifecycle.ACTIVE) {
            travelAgent.onDeactivate();
        }
        if (stockAgent != null && stockAgent.getLifecycleState() == AgentLifecycle.ACTIVE) {
            stockAgent.onDeactivate();
        }
    }
}
