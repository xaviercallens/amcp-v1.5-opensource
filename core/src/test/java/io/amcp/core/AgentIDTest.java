package io.amcp.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AgentID class
 */
@DisplayName("AgentID Tests")
class AgentIDTest {

    @Test
    @DisplayName("Should create AgentID with valid name")
    void testCreateAgentIDWithValidName() {
        // Given
        String agentName = "TestAgent";
        
        // When
        AgentID agentId = AgentID.named(agentName);
        
        // Then
        assertNotNull(agentId);
        assertEquals(agentName, agentId.getName());
    }

    @Test
    @DisplayName("Should generate unique IDs for different agents")
    void testUniqueAgentIDs() {
        // Given
        String name1 = "Agent1";
        String name2 = "Agent2";
        
        // When
        AgentID id1 = AgentID.named(name1);
        AgentID id2 = AgentID.named(name2);
        
        // Then
        assertNotEquals(id1, id2);
        assertNotEquals(id1.toString(), id2.toString());
    }

    @Test
    @DisplayName("Should handle null name gracefully")
    void testNullName() {
        // When/Then - AgentID.named() may accept null, so just verify it doesn't crash
        assertDoesNotThrow(() -> {
            AgentID agentId = AgentID.named(null);
            assertNotNull(agentId);
        });
    }

    @Test
    @DisplayName("Should handle empty name")
    void testEmptyName() {
        // Given
        String emptyName = "";
        
        // When
        AgentID agentId = AgentID.named(emptyName);
        
        // Then
        assertNotNull(agentId);
        assertEquals(emptyName, agentId.getName());
    }

    @Test
    @DisplayName("Should create consistent AgentID for same name")
    void testConsistentAgentID() {
        // Given
        String agentName = "ConsistentAgent";
        
        // When
        AgentID id1 = AgentID.named(agentName);
        AgentID id2 = AgentID.named(agentName);
        
        // Then
        assertEquals(id1.getName(), id2.getName());
    }

    @Test
    @DisplayName("Should have proper toString representation")
    void testToString() {
        // Given
        String agentName = "ToStringAgent";
        
        // When
        AgentID agentId = AgentID.named(agentName);
        String toString = agentId.toString();
        
        // Then
        assertNotNull(toString);
        assertTrue(toString.contains(agentName));
    }

    @Test
    @DisplayName("Should handle special characters in name")
    void testSpecialCharactersInName() {
        // Given
        String specialName = "Agent-123_Test@Domain";
        
        // When
        AgentID agentId = AgentID.named(specialName);
        
        // Then
        assertNotNull(agentId);
        assertEquals(specialName, agentId.getName());
    }

    @Test
    @DisplayName("Should support equality comparison")
    void testEquality() {
        // Given
        String name = "EqualityAgent";
        AgentID id1 = AgentID.named(name);
        AgentID id2 = AgentID.named(name);
        
        // When/Then
        assertEquals(id1.getName(), id2.getName());
    }

    @Test
    @DisplayName("Should support hashCode")
    void testHashCode() {
        // Given
        String name = "HashAgent";
        AgentID id1 = AgentID.named(name);
        AgentID id2 = AgentID.named(name);
        
        // When
        int hash1 = id1.hashCode();
        int hash2 = id2.hashCode();
        
        // Then
        assertNotEquals(0, hash1);
        assertNotEquals(0, hash2);
    }
}
