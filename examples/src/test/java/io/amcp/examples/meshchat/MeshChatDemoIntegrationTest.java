package io.amcp.examples.meshchat;

import org.junit.jupiter.api.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the comprehensive demo script and CLI functionality
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MeshChatDemoIntegrationTest {

    private final ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outputCapture));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    @Order(1)
    @DisplayName("Test CLI Help Command")
    void testCLIHelpCommand() {
        assertDoesNotThrow(() -> {
            MeshChatCLI.main(new String[]{"help"});
        });
        
        String output = outputCapture.toString();
        assertTrue(output.contains("AMCP v1.5 MeshChat CLI - Help"));
        assertTrue(output.contains("interactive"));
        assertTrue(output.contains("scenario"));
    }

    @Test
    @Order(2)
    @DisplayName("Test Travel Scenario")
    void testTravelScenario() {
        assertDoesNotThrow(() -> {
            MeshChatCLI.main(new String[]{"scenario", "travel", "Plan a trip to Japan"});
        });
        
        String output = outputCapture.toString();
        assertTrue(output.contains("travel") || output.contains("Japan"));
        assertTrue(output.contains("Scenario completed successfully"));
    }

    @Test
    @Order(3)
    @DisplayName("Test Stock Scenario")
    void testStockScenario() {
        assertDoesNotThrow(() -> {
            MeshChatCLI.main(new String[]{"scenario", "stock", "Analyze AAPL stock"});
        });
        
        String output = outputCapture.toString();
        assertTrue(output.contains("AAPL") || output.contains("stock"));
        assertTrue(output.contains("Scenario completed successfully"));
    }

    @Test
    @Order(4)
    @DisplayName("Test Multi-Agent Scenario")
    void testMultiAgentScenario() {
        assertDoesNotThrow(() -> {
            MeshChatCLI.main(new String[]{"scenario", "multi", "Business trip to Tokyo with investment analysis"});
        });
        
        String output = outputCapture.toString();
        assertTrue(output.contains("Multi-Agent") || output.contains("Tokyo") || output.contains("investment"));
        assertTrue(output.contains("Scenario completed successfully"));
    }

    @Test
    @Order(5)
    @DisplayName("Test Invalid Scenario Handling")
    void testInvalidScenarioHandling() {
        try {
            MeshChatCLI.main(new String[]{"scenario", "invalid"});
            fail("Should have thrown an exception for invalid scenario");
        } catch (Exception e) {
            // Expected - not enough arguments
        }
    }

    @Test
    @Order(6)
    @DisplayName("Test Unknown Command Handling")
    void testUnknownCommandHandling() {
        try {
            MeshChatCLI.main(new String[]{"unknown-command"});
            fail("Should have thrown an exception for unknown command");
        } catch (Exception e) {
            // Expected - unknown command
        }
    }

    @Test
    @Order(7)
    @DisplayName("Test Response Generation Quality")
    void testResponseGenerationQuality() {
        // Create a CLI instance to test response generation
        MeshChatCLI cli = new MeshChatCLI();
        
        // Test travel response
        assertDoesNotThrow(() -> {
            cli.runScenario("travel", "Plan a trip to Barcelona for 5 days");
        });
        
        String output = outputCapture.toString();
        
        // Response should be substantial (more than just a simple acknowledgment)
        assertTrue(output.length() > 100, "Response should be detailed");
        assertTrue(output.contains("Barcelona") || output.contains("travel"), 
                  "Response should reference the query");
    }

    @Test
    @Order(8)
    @DisplayName("Test CLI Performance")
    void testCLIPerformance() {
        long startTime = System.currentTimeMillis();
        
        assertDoesNotThrow(() -> {
            MeshChatCLI.main(new String[]{"scenario", "travel", "Quick trip test"});
        });
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Scenario should complete within reasonable time (5 seconds for demo)
        assertTrue(duration < 5000, "CLI scenario should complete within 5 seconds, took: " + duration + "ms");
    }

    @Test
    @Order(9)
    @DisplayName("Test Error Recovery")
    void testErrorRecovery() {
        // Test that the CLI can handle multiple operations in sequence
        assertDoesNotThrow(() -> {
            // Run multiple scenarios to test stability
            MeshChatCLI.main(new String[]{"scenario", "travel", "Test 1"});
            outputCapture.reset(); // Clear output
            MeshChatCLI.main(new String[]{"scenario", "stock", "Test 2"});
            outputCapture.reset(); // Clear output
            MeshChatCLI.main(new String[]{"scenario", "multi", "Test 3"});
        });
        
        String output = outputCapture.toString();
        assertTrue(output.contains("Scenario completed successfully"));
    }

    @Test
    @Order(10)
    @DisplayName("Test Agent Instantiation")
    void testAgentInstantiation() {
        // Test that the core agents can be instantiated without errors
        assertDoesNotThrow(() -> {
            MeshChatAgent meshChat = new MeshChatAgent();
            assertNotNull(meshChat);
            assertNotNull(meshChat.getAgentId());
            
            // Test cleanup
            meshChat.onDestroy();
        });
    }
}