package io.amcp.core;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unique identifier for agents within the AMCP v1.4 framework.
 * 
 * <p>AgentID provides a globally unique identifier for each agent instance,
 * ensuring proper routing, tracking, and management throughout the agent's
 * lifecycle, including mobility operations across different contexts.</p>
 * 
 * <p>Key characteristics:
 * <ul>
 *   <li>Globally unique across all contexts and time</li>
 *   <li>Serializable for mobility operations</li>
 *   <li>Immutable for thread safety</li>
 *   <li>Human-readable string representation</li>
 *   <li>Metadata support for additional context</li>
 * </ul>
 * </p>
 * 
 * @author AMCP Development Team
 * @version 1.4.0
 * @since 1.0.0
 */
public final class AgentID implements Serializable, Comparable<AgentID> {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String name;
    private final LocalDateTime createdAt;
    private final Map<String, String> metadata;

    /**
     * Creates a new AgentID with a generated UUID and default name.
     */
    public AgentID() {
        this(null, null);
    }

    /**
     * Creates a new AgentID with the specified name.
     * 
     * @param name the human-readable name for this agent (can be null)
     */
    public AgentID(String name) {
        this(null, name);
    }

    /**
     * Creates a new AgentID with the specified ID and name.
     * 
     * @param id the unique identifier (if null, a UUID will be generated)
     * @param name the human-readable name (if null, a default name will be used)
     */
    public AgentID(String id, String name) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = name != null ? name : "Agent-" + this.id.substring(0, 8);
        this.createdAt = LocalDateTime.now();
        this.metadata = new ConcurrentHashMap<>();
    }

    /**
     * Gets the unique identifier string.
     * 
     * @return the unique identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the human-readable name.
     * 
     * @return the agent name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the creation timestamp.
     * 
     * @return when this AgentID was created
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the metadata map for this AgentID.
     * 
     * @return mutable map of metadata key-value pairs
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * Adds metadata to this AgentID.
     * 
     * @param key the metadata key
     * @param value the metadata value
     * @return this AgentID for method chaining
     */
    public AgentID withMetadata(String key, String value) {
        if (key != null && value != null) {
            metadata.put(key, value);
        }
        return this;
    }

    /**
     * Gets metadata value by key.
     * 
     * @param key the metadata key
     * @return the metadata value, or null if not found
     */
    public String getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * Creates a new AgentID from a string representation.
     * 
     * <p>Parses AgentID strings in the format: "name@id" or just "id"</p>
     * 
     * @param agentIdString the string representation
     * @return parsed AgentID
     * @throws IllegalArgumentException if the string format is invalid
     */
    public static AgentID fromString(String agentIdString) {
        if (agentIdString == null || agentIdString.trim().isEmpty()) {
            throw new IllegalArgumentException("AgentID string cannot be null or empty");
        }

        String trimmed = agentIdString.trim();
        if (trimmed.contains("@")) {
            String[] parts = trimmed.split("@", 2);
            return new AgentID(parts[1], parts[0]);
        } else {
            return new AgentID(trimmed, null);
        }
    }

    /**
     * Creates a new AgentID with a specific name.
     * 
     * @param name the agent name
     * @return new AgentID with the specified name
     */
    public static AgentID named(String name) {
        return new AgentID(name);
    }

    /**
     * Creates a new AgentID with randomly generated values.
     * 
     * @return new random AgentID
     */
    public static AgentID random() {
        return new AgentID();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AgentID other = (AgentID) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public int compareTo(AgentID other) {
        if (other == null) {
            return 1;
        }
        int result = id.compareTo(other.id);
        if (result == 0) {
            result = Objects.compare(name, other.name, String::compareTo);
        }
        return result;
    }

    @Override
    public String toString() {
        if (name != null && !name.equals("Agent-" + id.substring(0, 8))) {
            return name + "@" + id;
        } else {
            return id;
        }
    }

    /**
     * Returns a detailed string representation including metadata.
     * 
     * @return detailed string with all AgentID information
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AgentID{");
        sb.append("id='").append(id).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", createdAt=").append(createdAt);
        if (!metadata.isEmpty()) {
            sb.append(", metadata=").append(metadata);
        }
        sb.append('}');
        return sb.toString();
    }

}