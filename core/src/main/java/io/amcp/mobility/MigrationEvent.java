package io.amcp.mobility;

import io.amcp.core.AgentID;
import java.time.LocalDateTime;
import java.io.Serializable;

/**
 * Records a migration event.
 */
public class MigrationEvent implements Serializable {
    private final AgentID agentId;
    private final String sourceContext;
    private final String targetContext;
    private final LocalDateTime timestamp;
    private final String type;
    private final boolean successful;
    private final String details;
    
    public MigrationEvent(AgentID agentId, String sourceContext, String targetContext, 
                         String type, boolean successful, String details) {
        this.agentId = agentId;
        this.sourceContext = sourceContext;
        this.targetContext = targetContext;
        this.timestamp = LocalDateTime.now();
        this.type = type;
        this.successful = successful;
        this.details = details;
    }
    
    public AgentID getAgentId() { return agentId; }
    public String getSourceContext() { return sourceContext; }
    public String getTargetContext() { return targetContext; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getType() { return type; }
    public boolean isSuccessful() { return successful; }
    public String getDetails() { return details; }
}