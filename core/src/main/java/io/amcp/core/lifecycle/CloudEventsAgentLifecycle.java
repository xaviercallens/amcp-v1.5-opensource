package io.amcp.core.lifecycle;

import io.amcp.core.Agent;
import io.amcp.core.AgentContext;
import io.amcp.core.AgentID;
import io.amcp.core.Event;
import io.amcp.core.DeliveryOptions;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;

/**
 * CloudEvents-compliant lifecycle management for AMCP v1.5 agents.
 * 
 * <p>This utility class provides standardized Agent Join/Leave event publishing
 * that conforms to CloudEvents v1.0 specification and the AMCP enterprise
 * agent lifecycle protocol.</p>
 * 
 * <p>Key features:
 * <ul>
 *   <li>CloudEvents v1.0 specification compliance</li>
 *   <li>Reverse-DNS event type naming (io.amcp.agent.*)</li>
 *   <li>Structured agent metadata and capabilities</li>
 *   <li>Proper correlation and source tracking</li>
 *   <li>Enterprise-grade agent discovery support</li>
 * </ul>
 * </p>
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 * @since 1.5.0
 */
public class CloudEventsAgentLifecycle {
    
    // CloudEvents compliant event types
    private static final String EVENT_TYPE_AGENT_JOIN = "io.amcp.agent.join";
    private static final String EVENT_TYPE_AGENT_LEAVE = "io.amcp.agent.leave";
    private static final String EVENT_TYPE_AGENT_HEARTBEAT = "io.amcp.agent.heartbeat";
    private static final String EVENT_TYPE_AGENT_CAPABILITY_UPDATE = "io.amcp.agent.capability.update";
    
    /**
     * Publishes a CloudEvents-compliant Agent Join event.
     * 
     * @param agent The agent that is joining
     * @param agentType The type classification of the agent
     * @param capabilities List of agent capabilities
     * @param metadata Additional metadata about the agent
     */
    public static void publishAgentJoinEvent(Agent agent, String agentType, 
                                           List<String> capabilities, 
                                           Map<String, Object> metadata) {
        AgentContext context = agent.getContext();
        if (context == null) {
            return; // Cannot publish without context
        }
        
        Map<String, Object> joinData = createAgentJoinData(
            agent.getAgentId(), agentType, capabilities, metadata);
        
        Event joinEvent = createCloudEventsCompliantEvent(
            EVENT_TYPE_AGENT_JOIN,
            "io.amcp.agent.join",
            joinData,
            UUID.randomUUID().toString(),
            agent.getAgentId()
        );
        
        context.publishEvent(joinEvent);
        logLifecycleEvent(agent.getAgentId(), "JOINED", agentType, capabilities.size());
    }
    
    /**
     * Publishes a CloudEvents-compliant Agent Leave event.
     * 
     * @param agent The agent that is leaving
     * @param reason The reason for leaving
     * @param sessionStats Optional session statistics
     */
    public static void publishAgentLeaveEvent(Agent agent, String reason, 
                                            Map<String, Object> sessionStats) {
        AgentContext context = agent.getContext();
        if (context == null) {
            return; // Cannot publish without context
        }
        
        Map<String, Object> leaveData = createAgentLeaveData(
            agent.getAgentId(), reason, sessionStats);
        
        Event leaveEvent = createCloudEventsCompliantEvent(
            EVENT_TYPE_AGENT_LEAVE,
            "io.amcp.agent.leave",
            leaveData,
            UUID.randomUUID().toString(),
            agent.getAgentId()
        );
        
        context.publishEvent(leaveEvent);
        logLifecycleEvent(agent.getAgentId(), "LEFT", reason, 0);
    }
    
    /**
     * Publishes a CloudEvents-compliant Agent Heartbeat event.
     * 
     * @param agent The agent sending heartbeat
     * @param status Current agent status
     * @param metrics Current performance metrics
     */
    public static void publishAgentHeartbeatEvent(Agent agent, String status, 
                                                 Map<String, Object> metrics) {
        AgentContext context = agent.getContext();
        if (context == null) {
            return;
        }
        
        Map<String, Object> heartbeatData = createAgentHeartbeatData(
            agent.getAgentId(), status, metrics);
        
        Event heartbeatEvent = createCloudEventsCompliantEvent(
            EVENT_TYPE_AGENT_HEARTBEAT,
            "io.amcp.agent.heartbeat",
            heartbeatData,
            UUID.randomUUID().toString(),
            agent.getAgentId()
        );
        
        context.publishEvent(heartbeatEvent);
    }
    
    /**
     * Publishes a CloudEvents-compliant Agent Capability Update event.
     * 
     * @param agent The agent with updated capabilities
     * @param newCapabilities Updated list of capabilities
     * @param removedCapabilities List of removed capabilities
     */
    public static void publishAgentCapabilityUpdateEvent(Agent agent, 
                                                        List<String> newCapabilities,
                                                        List<String> removedCapabilities) {
        AgentContext context = agent.getContext();
        if (context == null) {
            return;
        }
        
        Map<String, Object> updateData = createCapabilityUpdateData(
            agent.getAgentId(), newCapabilities, removedCapabilities);
        
        Event updateEvent = createCloudEventsCompliantEvent(
            EVENT_TYPE_AGENT_CAPABILITY_UPDATE,
            "io.amcp.agent.capability.update",
            updateData,
            UUID.randomUUID().toString(),
            agent.getAgentId()
        );
        
        context.publishEvent(updateEvent);
        logLifecycleEvent(agent.getAgentId(), "CAPABILITIES_UPDATED", 
            "added:" + newCapabilities.size() + ",removed:" + removedCapabilities.size(), 
            newCapabilities.size());
    }
    
    /**
     * Creates standardized Agent Join data payload.
     */
    private static Map<String, Object> createAgentJoinData(AgentID agentId, String agentType,
                                                          List<String> capabilities,
                                                          Map<String, Object> metadata) {
        Map<String, Object> joinData = new HashMap<>();
        
        // Required CloudEvents agent join fields
        joinData.put("agentId", agentId.toString());
        joinData.put("agentType", agentType);
        joinData.put("timestamp", LocalDateTime.now().toString());
        joinData.put("capabilities", new ArrayList<>(capabilities));
        
        // AMCP-specific agent information
        joinData.put("version", "1.5.0");
        joinData.put("protocol", "AMCP");
        joinData.put("sourceUri", createAgentSourceUri(agentId));
        
        // Context information
        joinData.put("contextInfo", createContextInfo());
        
        // Agent metadata
        if (metadata != null && !metadata.isEmpty()) {
            joinData.put("metadata", new HashMap<>(metadata));
        }
        
        // Lifecycle event metadata
        joinData.put("lifecycleEvent", "JOIN");
        joinData.put("eventVersion", "1.0");
        
        return joinData;
    }
    
    /**
     * Creates standardized Agent Leave data payload.
     */
    private static Map<String, Object> createAgentLeaveData(AgentID agentId, String reason,
                                                           Map<String, Object> sessionStats) {
        Map<String, Object> leaveData = new HashMap<>();
        
        // Required CloudEvents agent leave fields
        leaveData.put("agentId", agentId.toString());
        leaveData.put("timestamp", LocalDateTime.now().toString());
        leaveData.put("reason", reason);
        
        // AMCP-specific information
        leaveData.put("version", "1.5.0");
        leaveData.put("protocol", "AMCP");
        leaveData.put("sourceUri", createAgentSourceUri(agentId));
        
        // Session statistics
        if (sessionStats != null && !sessionStats.isEmpty()) {
            leaveData.put("sessionStats", new HashMap<>(sessionStats));
        }
        
        // Lifecycle event metadata
        leaveData.put("lifecycleEvent", "LEAVE");
        leaveData.put("eventVersion", "1.0");
        
        return leaveData;
    }
    
    /**
     * Creates standardized Agent Heartbeat data payload.
     */
    private static Map<String, Object> createAgentHeartbeatData(AgentID agentId, String status,
                                                               Map<String, Object> metrics) {
        Map<String, Object> heartbeatData = new HashMap<>();
        
        heartbeatData.put("agentId", agentId.toString());
        heartbeatData.put("timestamp", LocalDateTime.now().toString());
        heartbeatData.put("status", status);
        heartbeatData.put("sourceUri", createAgentSourceUri(agentId));
        
        if (metrics != null) {
            heartbeatData.put("metrics", new HashMap<>(metrics));
        }
        
        heartbeatData.put("lifecycleEvent", "HEARTBEAT");
        heartbeatData.put("eventVersion", "1.0");
        
        return heartbeatData;
    }
    
    /**
     * Creates capability update data payload.
     */
    private static Map<String, Object> createCapabilityUpdateData(AgentID agentId,
                                                                 List<String> newCapabilities,
                                                                 List<String> removedCapabilities) {
        Map<String, Object> updateData = new HashMap<>();
        
        updateData.put("agentId", agentId.toString());
        updateData.put("timestamp", LocalDateTime.now().toString());
        updateData.put("newCapabilities", new ArrayList<>(newCapabilities));
        updateData.put("removedCapabilities", new ArrayList<>(removedCapabilities));
        updateData.put("sourceUri", createAgentSourceUri(agentId));
        
        updateData.put("lifecycleEvent", "CAPABILITY_UPDATE");
        updateData.put("eventVersion", "1.0");
        
        return updateData;
    }
    
    /**
     * Creates CloudEvents-compliant event with proper metadata.
     */
    private static Event createCloudEventsCompliantEvent(String eventType, String topic,
                                                        Object data, String correlationId,
                                                        AgentID sender) {
        try {
            URI sourceUri = URI.create(createAgentSourceUri(sender));
            
            return Event.builder()
                .topic(topic)
                .payload(data)
                .correlationId(correlationId)
                .sender(sender)
                .deliveryOptions(DeliveryOptions.reliable())
                // CloudEvents v1.0 metadata
                .metadata("ce-specversion", "1.0")
                .metadata("ce-type", eventType)
                .metadata("ce-source", sourceUri.toString())
                .metadata("ce-datacontenttype", "application/json")
                // AMCP-specific extensions
                .metadata("amcp-version", "1.5.0")
                .metadata("amcp-protocol", "AMCP")
                .metadata("amcp-lifecycle-event", true)
                .build();
                
        } catch (Exception e) {
            // Fallback to basic event if URI creation fails
            return Event.builder()
                .topic(topic)
                .payload(data)
                .correlationId(correlationId)
                .sender(sender)
                .deliveryOptions(DeliveryOptions.reliable())
                .metadata("ce-specversion", "1.0")
                .metadata("ce-type", eventType)
                .metadata("amcp-version", "1.5.0")
                .build();
        }
    }
    
    /**
     * Creates standardized agent source URI.
     */
    private static String createAgentSourceUri(AgentID agentId) {
        try {
            return "urn:amcp:agent:" + agentId.toString().toLowerCase().replaceAll("[^a-z0-9]", "-");
        } catch (Exception e) {
            return "urn:amcp:agent:unknown";
        }
    }
    
    /**
     * Creates context information for the current environment.
     */
    private static Map<String, Object> createContextInfo() {
        Map<String, Object> contextInfo = new HashMap<>();
        
        try {
            contextInfo.put("hostname", System.getProperty("os.name", "unknown"));
            contextInfo.put("javaVersion", System.getProperty("java.version", "unknown"));
            contextInfo.put("processId", ProcessHandle.current().pid());
            contextInfo.put("systemTime", System.currentTimeMillis());
            
            // Add memory information
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> memoryInfo = new HashMap<>();
            memoryInfo.put("totalMemory", runtime.totalMemory());
            memoryInfo.put("freeMemory", runtime.freeMemory());
            memoryInfo.put("maxMemory", runtime.maxMemory());
            contextInfo.put("memory", memoryInfo);
            
        } catch (Exception e) {
            contextInfo.put("error", "Context info collection failed: " + e.getMessage());
        }
        
        return contextInfo;
    }
    
    /**
     * Logs lifecycle events for monitoring and debugging.
     */
    private static void logLifecycleEvent(AgentID agentId, String event, String details, int capabilityCount) {
        String timestamp = LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [CloudEventsLifecycle] " + 
            "Agent " + agentId + " " + event + 
            (details != null ? " (" + details + ")" : "") +
            (capabilityCount > 0 ? " with " + capabilityCount + " capabilities" : ""));
    }
    
    /**
     * Utility method to create standard capability lists for common agent types.
     */
    public static List<String> createStandardCapabilities(String agentType) {
        List<String> capabilities = new ArrayList<>();
        
        switch (agentType.toLowerCase()) {
            case "weather":
            case "weatheragent":
                capabilities.addAll(Arrays.asList(
                    "weather-data-collection", "weather-api-integration", 
                    "location-based-queries", "weather-forecasting",
                    "climate-monitoring", "weather-alerts", "real-time-updates"
                ));
                break;
                
            case "travel":
            case "travelplanner":
            case "travelplanneragent":
                capabilities.addAll(Arrays.asList(
                    "travel-planning", "destination-recommendations",
                    "itinerary-creation", "booking-assistance",
                    "travel-advice", "location-services"
                ));
                break;
                
            case "stock":
            case "stockprice":
            case "stockpriceagent":
                capabilities.addAll(Arrays.asList(
                    "stock-price-retrieval", "market-data-analysis",
                    "financial-information", "ticker-symbol-lookup",
                    "market-monitoring", "investment-insights"
                ));
                break;
                
            case "orchestrator":
            case "orchestratoragent":
                capabilities.addAll(Arrays.asList(
                    "intent-analysis", "multi-agent-orchestration",
                    "llm-coordination", "request-routing",
                    "response-aggregation", "workflow-management"
                ));
                break;
                
            default:
                capabilities.addAll(Arrays.asList(
                    "event-processing", "message-handling", "agent-communication"
                ));
                break;
        }
        
        return capabilities;
    }
    
    /**
     * Utility method to create standard metadata for agent types.
     */
    public static Map<String, Object> createStandardMetadata(String agentType) {
        Map<String, Object> metadata = new HashMap<>();
        
        metadata.put("amcp-version", "1.5.0");
        metadata.put("agent-type", agentType);
        metadata.put("startup-time", LocalDateTime.now().toString());
        metadata.put("cloudevents-compliant", true);
        metadata.put("supports-mobility", true);
        metadata.put("supports-federation", true);
        
        return metadata;
    }
}