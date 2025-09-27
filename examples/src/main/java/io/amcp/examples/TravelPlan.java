package io.amcp.examples.agents;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Travel plan data model for TravelPlannerAgent
 */
public class TravelPlan {
    private final String planId;
    private final String destination;
    private final String startDate;
    private final String endDate;
    private final String userId;
    private String status;
    private String errorMessage;
    private final Map<String, Object> contextData;
    private final List<String> recommendations;
    private final long createdAt;
    private long lastModified;
    
    public TravelPlan(String planId, String destination, String startDate, String endDate, String userId) {
        this.planId = planId;
        this.destination = destination;
        this.startDate = startDate;
        this.endDate = endDate;
        this.userId = userId;
        this.status = "created";
        this.contextData = new ConcurrentHashMap<String, Object>();
        this.recommendations = new ArrayList<String>();
        this.createdAt = System.currentTimeMillis();
        this.lastModified = this.createdAt;
    }
    
    public String getPlanId() { return planId; }
    public String getDestination() { return destination; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getUserId() { return userId; }
    public String getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public Map<String, Object> getContextData() { return contextData; }
    public List<String> getRecommendations() { return recommendations; }
    public long getCreatedAt() { return createdAt; }
    public long getLastModified() { return lastModified; }
    
    public void setStatus(String status) {
        this.status = status;
        this.lastModified = System.currentTimeMillis();
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.lastModified = System.currentTimeMillis();
    }
    
    public void addContextData(String key, Object value) {
        this.contextData.put(key, value);
        this.lastModified = System.currentTimeMillis();
    }
    
    public void addRecommendation(String recommendation) {
        this.recommendations.add(recommendation);
        this.lastModified = System.currentTimeMillis();
    }
}

/**
 * Agent status event for communication
 */
class AgentStatusEvent {
    private final String status;
    private final String message;
    private final int activePlans;
    private final java.time.Instant timestamp;
    
    public AgentStatusEvent(String status, String message, int activePlans, java.time.Instant timestamp) {
        this.status = status;
        this.message = message;
        this.activePlans = activePlans;
        this.timestamp = timestamp;
    }
    
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public int getActivePlans() { return activePlans; }
    public java.time.Instant getTimestamp() { return timestamp; }
}

/**
 * Migration notification for agent collaboration
 */
class MigrationNotification {
    private final String agentId;
    private final String sourceContext;
    private final String targetContext;
    private final String phase;
    
    public MigrationNotification(String agentId, String sourceContext, String targetContext, String phase) {
        this.agentId = agentId;
        this.sourceContext = sourceContext;
        this.targetContext = targetContext;
        this.phase = phase;
    }
    
    public String getAgentId() { return agentId; }
    public String getSourceContext() { return sourceContext; }
    public String getTargetContext() { return targetContext; }
    public String getPhase() { return phase; }
}

/**
 * Collaboration request for multi-agent planning
 */
class CollaborationRequest {
    private final String requesterId;
    private final String planId;
    private final String destination;
    private final String collaborationType;
    
    public CollaborationRequest(String requesterId, String planId, String destination, String collaborationType) {
        this.requesterId = requesterId;
        this.planId = planId;
        this.destination = destination;
        this.collaborationType = collaborationType;
    }
    
    public String getRequesterId() { return requesterId; }
    public String getPlanId() { return planId; }
    public String getDestination() { return destination; }
    public String getCollaborationType() { return collaborationType; }
}