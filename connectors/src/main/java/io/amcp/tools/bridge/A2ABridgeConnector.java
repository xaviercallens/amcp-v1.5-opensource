package io.amcp.tools.bridge;

import io.amcp.tools.ToolConnector;
import io.amcp.tools.ToolRequest;
import io.amcp.tools.ToolResponse;
import io.amcp.core.Event;
import io.amcp.core.AgentID;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AMCP v1.4 Agent-to-Agent (A2A) Bridge Connector.
 * Enables secure communication between AMCP agents across different
 * environments, networks, and runtime contexts.
 */
public class A2ABridgeConnector implements ToolConnector {
    
    private static final String TOOL_ID = "a2a-bridge";
    private static final String TOOL_NAME = "Agent-to-Agent Bridge";
    private static final String VERSION = "1.4.0";
    
    private volatile boolean initialized = false;
    private final Map<String, BridgeConnection> connections = new ConcurrentHashMap<>();
    private final Map<String, MessageQueue> messageQueues = new ConcurrentHashMap<>();
    private final AtomicLong messageCounter = new AtomicLong(0);
    
    private ScheduledExecutorService heartbeatExecutor;
    private String bridgeId;
    private int heartbeatIntervalSec = 30;
    private int messageTimeoutSec = 60;
    private SecurityConfig securityConfig;
    
    @Override
    public String getToolId() {
        return TOOL_ID;
    }
    
    @Override
    public String getToolName() {
        return TOOL_NAME;
    }
    
    @Override
    public String getVersion() {
        return VERSION;
    }
    
    @Override
    public CompletableFuture<Boolean> isHealthy() {
        return CompletableFuture.supplyAsync(() -> {
            if (!initialized) return false;
            
            // Check if at least one connection is healthy
            return connections.values().stream()
                    .anyMatch(conn -> conn.isHealthy());
        });
    }
    
    @Override
    public CompletableFuture<Void> initialize(Map<String, Object> config) {
        return CompletableFuture.runAsync(() -> {
            if (config != null) {
                this.bridgeId = (String) config.getOrDefault("bridgeId", "bridge_" + System.currentTimeMillis());
                
                Integer heartbeat = (Integer) config.get("heartbeatIntervalSec");
                if (heartbeat != null && heartbeat > 0) {
                    this.heartbeatIntervalSec = heartbeat;
                }
                
                Integer timeout = (Integer) config.get("messageTimeoutSec");
                if (timeout != null && timeout > 0) {
                    this.messageTimeoutSec = timeout;
                }
                
                // Initialize security configuration
                @SuppressWarnings("unchecked")
                Map<String, Object> secConfig = (Map<String, Object>) config.get("security");
                this.securityConfig = new SecurityConfig(secConfig);
            } else {
                this.bridgeId = "bridge_" + System.currentTimeMillis();
                this.securityConfig = new SecurityConfig(null);
            }
            
            // Start heartbeat service
            this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "A2A-Bridge-Heartbeat");
                t.setDaemon(true);
                return t;
            });
            
            this.heartbeatExecutor.scheduleWithFixedDelay(
                this::performHeartbeat, 
                heartbeatIntervalSec, 
                heartbeatIntervalSec, 
                TimeUnit.SECONDS
            );
            
            this.initialized = true;
        });
    }
    
    @Override
    public CompletableFuture<ToolResponse> invoke(ToolRequest request) {
        if (!initialized) {
            return CompletableFuture.completedFuture(
                ToolResponse.error("A2A Bridge not initialized", request.getRequestId(), 0)
            );
        }
        
        long startTime = System.currentTimeMillis();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String operation = request.getOperation();
                
                switch (operation) {
                    case "establish_connection":
                        return establishConnection(request, startTime);
                    case "send_message":
                        return sendMessage(request, startTime);
                    case "receive_messages":
                        return receiveMessages(request, startTime);
                    case "close_connection":
                        return closeConnection(request, startTime);
                    case "list_connections":
                        return listConnections(request, startTime);
                    case "get_connection_status":
                        return getConnectionStatus(request, startTime);
                    case "broadcast_event":
                        return broadcastEvent(request, startTime);
                    default:
                        long duration = System.currentTimeMillis() - startTime;
                        return ToolResponse.error("Unsupported operation: " + operation, 
                                                request.getRequestId(), duration);
                }
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                return ToolResponse.error("A2A Bridge operation failed: " + e.getMessage(), 
                                        request.getRequestId(), duration);
            }
        });
    }
    
    private ToolResponse establishConnection(ToolRequest request, long startTime) {
        String targetBridgeUrl = request.getStringParameter("bridgeUrl");
        String connectionId = request.getStringParameter("connectionId");
        
        if (targetBridgeUrl == null || targetBridgeUrl.trim().isEmpty()) {
            long duration = System.currentTimeMillis() - startTime;
            return ToolResponse.error("bridgeUrl parameter is required", request.getRequestId(), duration);
        }
        
        if (connectionId == null || connectionId.trim().isEmpty()) {
            connectionId = "conn_" + System.currentTimeMillis();
        }
        
        try {
            BridgeConnection connection = new BridgeConnection(
                connectionId, 
                targetBridgeUrl, 
                securityConfig
            );
            
            // Perform handshake
            boolean handshakeSuccess = connection.performHandshake();
            if (!handshakeSuccess) {
                long duration = System.currentTimeMillis() - startTime;
                return ToolResponse.error("Handshake failed with target bridge", 
                                        request.getRequestId(), duration);
            }
            
            connections.put(connectionId, connection);
            messageQueues.put(connectionId, new MessageQueue(connectionId));
            
            Map<String, Object> result = new HashMap<>();
            result.put("connection_id", connectionId);
            result.put("target_bridge_url", targetBridgeUrl);
            result.put("status", "connected");
            result.put("handshake_completed", true);
            result.put("security_enabled", securityConfig.isEnabled());
            
            long duration = System.currentTimeMillis() - startTime;
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("operation", "establish_connection");
            metadata.put("bridge_id", bridgeId);
            
            return ToolResponse.success(result, request.getRequestId(), duration, metadata);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return ToolResponse.error("Failed to establish connection: " + e.getMessage(), 
                                    request.getRequestId(), duration);
        }
    }
    
    private ToolResponse sendMessage(ToolRequest request, long startTime) {
        String connectionId = request.getStringParameter("connectionId");
        String targetAgentId = request.getStringParameter("targetAgentId");
        Map<String, Object> messageData = request.getParameters();
        
        if (connectionId == null || !connections.containsKey(connectionId)) {
            long duration = System.currentTimeMillis() - startTime;
            return ToolResponse.error("Invalid or missing connection ID", request.getRequestId(), duration);
        }
        
        if (targetAgentId == null || targetAgentId.trim().isEmpty()) {
            long duration = System.currentTimeMillis() - startTime;
            return ToolResponse.error("targetAgentId parameter is required", request.getRequestId(), duration);
        }
        
        try {
            BridgeConnection connection = connections.get(connectionId);
            
            A2AMessage message = new A2AMessage(
                messageCounter.incrementAndGet(),
                bridgeId,
                targetAgentId,
                messageData,
                System.currentTimeMillis()
            );
            
            boolean success = connection.sendMessage(message);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message_sent", success);
            result.put("message_id", message.getMessageId());
            result.put("connection_id", connectionId);
            result.put("target_agent_id", targetAgentId);
            
            long duration = System.currentTimeMillis() - startTime;
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("operation", "send_message");
            metadata.put("bridge_id", bridgeId);
            
            return ToolResponse.success(result, request.getRequestId(), duration, metadata);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return ToolResponse.error("Failed to send message: " + e.getMessage(), 
                                    request.getRequestId(), duration);
        }
    }
    
    private ToolResponse receiveMessages(ToolRequest request, long startTime) {
        String connectionId = request.getStringParameter("connectionId");
        Integer maxMessages = request.getIntegerParameter("maxMessages");
        
        if (maxMessages == null) maxMessages = 10; // Default
        if (maxMessages > 100) maxMessages = 100; // Limit
        
        if (connectionId == null || !messageQueues.containsKey(connectionId)) {
            long duration = System.currentTimeMillis() - startTime;
            return ToolResponse.error("Invalid or missing connection ID", request.getRequestId(), duration);
        }
        
        try {
            MessageQueue queue = messageQueues.get(connectionId);
            List<A2AMessage> messages = queue.getMessages(maxMessages);
            
            List<Map<String, Object>> messageList = new ArrayList<>();
            for (A2AMessage msg : messages) {
                Map<String, Object> msgData = new HashMap<>();
                msgData.put("message_id", msg.getMessageId());
                msgData.put("sender_bridge_id", msg.getSenderBridgeId());
                msgData.put("target_agent_id", msg.getTargetAgentId());
                msgData.put("data", msg.getData());
                msgData.put("timestamp", msg.getTimestamp());
                messageList.add(msgData);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("messages", messageList);
            result.put("message_count", messages.size());
            result.put("connection_id", connectionId);
            result.put("has_more_messages", queue.hasMoreMessages());
            
            long duration = System.currentTimeMillis() - startTime;
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("operation", "receive_messages");
            metadata.put("bridge_id", bridgeId);
            
            return ToolResponse.success(result, request.getRequestId(), duration, metadata);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return ToolResponse.error("Failed to receive messages: " + e.getMessage(), 
                                    request.getRequestId(), duration);
        }
    }
    
    private ToolResponse closeConnection(ToolRequest request, long startTime) {
        String connectionId = request.getStringParameter("connectionId");
        
        if (connectionId == null || !connections.containsKey(connectionId)) {
            long duration = System.currentTimeMillis() - startTime;
            return ToolResponse.error("Invalid or missing connection ID", request.getRequestId(), duration);
        }
        
        try {
            BridgeConnection connection = connections.get(connectionId);
            connection.close();
            
            connections.remove(connectionId);
            messageQueues.remove(connectionId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("connection_closed", true);
            result.put("connection_id", connectionId);
            
            long duration = System.currentTimeMillis() - startTime;
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("operation", "close_connection");
            metadata.put("bridge_id", bridgeId);
            
            return ToolResponse.success(result, request.getRequestId(), duration, metadata);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return ToolResponse.error("Failed to close connection: " + e.getMessage(), 
                                    request.getRequestId(), duration);
        }
    }
    
    private ToolResponse listConnections(ToolRequest request, long startTime) {
        try {
            List<Map<String, Object>> connectionList = new ArrayList<>();
            
            for (Map.Entry<String, BridgeConnection> entry : connections.entrySet()) {
                String connId = entry.getKey();
                BridgeConnection conn = entry.getValue();
                
                Map<String, Object> connInfo = new HashMap<>();
                connInfo.put("connection_id", connId);
                connInfo.put("target_url", conn.getTargetUrl());
                connInfo.put("is_healthy", conn.isHealthy());
                connInfo.put("last_heartbeat", conn.getLastHeartbeat());
                connInfo.put("messages_sent", conn.getMessagesSent());
                connInfo.put("messages_received", conn.getMessagesReceived());
                
                connectionList.add(connInfo);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("connections", connectionList);
            result.put("connection_count", connections.size());
            result.put("bridge_id", bridgeId);
            
            long duration = System.currentTimeMillis() - startTime;
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("operation", "list_connections");
            metadata.put("bridge_id", bridgeId);
            
            return ToolResponse.success(result, request.getRequestId(), duration, metadata);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return ToolResponse.error("Failed to list connections: " + e.getMessage(), 
                                    request.getRequestId(), duration);
        }
    }
    
    private ToolResponse getConnectionStatus(ToolRequest request, long startTime) {
        String connectionId = request.getStringParameter("connectionId");
        
        if (connectionId == null || !connections.containsKey(connectionId)) {
            long duration = System.currentTimeMillis() - startTime;
            return ToolResponse.error("Invalid or missing connection ID", request.getRequestId(), duration);
        }
        
        try {
            BridgeConnection connection = connections.get(connectionId);
            MessageQueue queue = messageQueues.get(connectionId);
            
            Map<String, Object> status = new HashMap<>();
            status.put("connection_id", connectionId);
            status.put("is_healthy", connection.isHealthy());
            status.put("target_url", connection.getTargetUrl());
            status.put("last_heartbeat", connection.getLastHeartbeat());
            status.put("messages_sent", connection.getMessagesSent());
            status.put("messages_received", connection.getMessagesReceived());
            status.put("pending_messages", queue != null ? queue.size() : 0);
            status.put("connection_established", connection.getConnectionTime());
            
            long duration = System.currentTimeMillis() - startTime;
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("operation", "get_connection_status");
            metadata.put("bridge_id", bridgeId);
            
            return ToolResponse.success(status, request.getRequestId(), duration, metadata);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return ToolResponse.error("Failed to get connection status: " + e.getMessage(), 
                                    request.getRequestId(), duration);
        }
    }
    
    private ToolResponse broadcastEvent(ToolRequest request, long startTime) {
        Map<String, Object> eventData = request.getParameters();
        String eventTopic = request.getStringParameter("topic");
        
        if (eventTopic == null || eventTopic.trim().isEmpty()) {
            long duration = System.currentTimeMillis() - startTime;
            return ToolResponse.error("topic parameter is required for broadcast", 
                                    request.getRequestId(), duration);
        }
        
        try {
            int successCount = 0;
            int totalConnections = connections.size();
            
            for (Map.Entry<String, BridgeConnection> entry : connections.entrySet()) {
                BridgeConnection connection = entry.getValue();
                
                if (connection.isHealthy()) {
                    A2AMessage broadcastMsg = new A2AMessage(
                        messageCounter.incrementAndGet(),
                        bridgeId,
                        "BROADCAST", // Special target for broadcast messages
                        eventData,
                        System.currentTimeMillis()
                    );
                    
                    if (connection.sendMessage(broadcastMsg)) {
                        successCount++;
                    }
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("broadcast_sent", true);
            result.put("total_connections", totalConnections);
            result.put("successful_sends", successCount);
            result.put("failed_sends", totalConnections - successCount);
            result.put("event_topic", eventTopic);
            
            long duration = System.currentTimeMillis() - startTime;
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("operation", "broadcast_event");
            metadata.put("bridge_id", bridgeId);
            
            return ToolResponse.success(result, request.getRequestId(), duration, metadata);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return ToolResponse.error("Failed to broadcast event: " + e.getMessage(), 
                                    request.getRequestId(), duration);
        }
    }
    
    private void performHeartbeat() {
        for (BridgeConnection connection : connections.values()) {
            if (connection.isHealthy()) {
                try {
                    connection.sendHeartbeat();
                } catch (Exception e) {
                    // Log error but continue with other connections
                    System.err.println("Heartbeat failed for connection " + 
                                     connection.getConnectionId() + ": " + e.getMessage());
                }
            }
        }
    }
    
    @Override
    public Map<String, Object> getRequestSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        // Connection parameters
        Map<String, Object> bridgeUrlParam = new HashMap<>();
        bridgeUrlParam.put("type", "string");
        bridgeUrlParam.put("description", "Target bridge URL for establishing connection");
        properties.put("bridgeUrl", bridgeUrlParam);
        
        Map<String, Object> connectionIdParam = new HashMap<>();
        connectionIdParam.put("type", "string");
        connectionIdParam.put("description", "Unique connection identifier");
        properties.put("connectionId", connectionIdParam);
        
        Map<String, Object> targetAgentIdParam = new HashMap<>();
        targetAgentIdParam.put("type", "string");
        targetAgentIdParam.put("description", "Target agent ID for message delivery");
        properties.put("targetAgentId", targetAgentIdParam);
        
        Map<String, Object> maxMessagesParam = new HashMap<>();
        maxMessagesParam.put("type", "integer");
        maxMessagesParam.put("description", "Maximum number of messages to retrieve");
        maxMessagesParam.put("minimum", 1);
        maxMessagesParam.put("maximum", 100);
        properties.put("maxMessages", maxMessagesParam);
        
        Map<String, Object> topicParam = new HashMap<>();
        topicParam.put("type", "string");
        topicParam.put("description", "Event topic for broadcast operations");
        properties.put("topic", topicParam);
        
        schema.put("properties", properties);
        
        return schema;
    }
    
    @Override
    public String[] getSupportedOperations() {
        return new String[]{
            "establish_connection", 
            "send_message", 
            "receive_messages", 
            "close_connection",
            "list_connections", 
            "get_connection_status", 
            "broadcast_event"
        };
    }
    
    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            this.initialized = false;
            
            // Close all connections
            for (BridgeConnection connection : connections.values()) {
                try {
                    connection.close();
                } catch (Exception e) {
                    // Log but continue shutdown
                    System.err.println("Error closing connection during shutdown: " + e.getMessage());
                }
            }
            connections.clear();
            messageQueues.clear();
            
            // Shutdown heartbeat executor
            if (heartbeatExecutor != null) {
                heartbeatExecutor.shutdown();
                try {
                    if (!heartbeatExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                        heartbeatExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    heartbeatExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        });
    }
    
    // Inner classes for bridge functionality
    
    private static class BridgeConnection {
        private final String connectionId;
        private final String targetUrl;
        private final SecurityConfig securityConfig;
        private volatile boolean healthy;
        private volatile long lastHeartbeat;
        private volatile long connectionTime;
        private final AtomicLong messagesSent = new AtomicLong(0);
        private final AtomicLong messagesReceived = new AtomicLong(0);
        
        public BridgeConnection(String connectionId, String targetUrl, SecurityConfig securityConfig) {
            this.connectionId = connectionId;
            this.targetUrl = targetUrl;
            this.securityConfig = securityConfig;
            this.connectionTime = System.currentTimeMillis();
            this.healthy = false;
        }
        
        public boolean performHandshake() {
            // Simulate handshake process
            try {
                // In a real implementation, this would perform:
                // 1. TCP/TLS connection establishment
                // 2. Authentication exchange
                // 3. Protocol version negotiation
                // 4. Capability exchange
                
                Thread.sleep(100); // Simulate network delay
                this.healthy = true;
                this.lastHeartbeat = System.currentTimeMillis();
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        public boolean sendMessage(A2AMessage message) {
            if (!healthy) return false;
            
            try {
                // Simulate message sending
                Thread.sleep(10); // Simulate network delay
                messagesSent.incrementAndGet();
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        public void sendHeartbeat() {
            if (healthy) {
                this.lastHeartbeat = System.currentTimeMillis();
            }
        }
        
        public void close() {
            this.healthy = false;
        }
        
        public boolean isHealthy() { return healthy; }
        public String getConnectionId() { return connectionId; }
        public String getTargetUrl() { return targetUrl; }
        public long getLastHeartbeat() { return lastHeartbeat; }
        public long getMessagesSent() { return messagesSent.get(); }
        public long getMessagesReceived() { return messagesReceived.get(); }
        public long getConnectionTime() { return connectionTime; }
    }
    
    private static class A2AMessage {
        private final long messageId;
        private final String senderBridgeId;
        private final String targetAgentId;
        private final Map<String, Object> data;
        private final long timestamp;
        
        public A2AMessage(long messageId, String senderBridgeId, String targetAgentId,
                         Map<String, Object> data, long timestamp) {
            this.messageId = messageId;
            this.senderBridgeId = senderBridgeId;
            this.targetAgentId = targetAgentId;
            this.data = new HashMap<>(data);
            this.timestamp = timestamp;
        }
        
        public long getMessageId() { return messageId; }
        public String getSenderBridgeId() { return senderBridgeId; }
        public String getTargetAgentId() { return targetAgentId; }
        public Map<String, Object> getData() { return data; }
        public long getTimestamp() { return timestamp; }
    }
    
    private static class MessageQueue {
        private final String connectionId;
        private final List<A2AMessage> messages = new ArrayList<>();
        private final int maxSize = 1000;
        
        public MessageQueue(String connectionId) {
            this.connectionId = connectionId;
        }
        
        public synchronized void addMessage(A2AMessage message) {
            if (messages.size() >= maxSize) {
                messages.remove(0); // Remove oldest message
            }
            messages.add(message);
        }
        
        public synchronized List<A2AMessage> getMessages(int maxMessages) {
            int count = Math.min(maxMessages, messages.size());
            List<A2AMessage> result = new ArrayList<>(messages.subList(0, count));
            messages.subList(0, count).clear(); // Remove retrieved messages
            return result;
        }
        
        public synchronized int size() {
            return messages.size();
        }
        
        public synchronized boolean hasMoreMessages() {
            return !messages.isEmpty();
        }
    }
    
    private static class SecurityConfig {
        private boolean enabled = false;
        private String encryptionMethod = "AES-256-GCM";
        private String authenticationMethod = "HMAC-SHA256";
        private boolean requireMutualTLS = false;
        
        public SecurityConfig(Map<String, Object> config) {
            if (config != null) {
                Boolean enabledConfig = (Boolean) config.get("enabled");
                if (enabledConfig != null) {
                    this.enabled = enabledConfig;
                }
                
                String encryption = (String) config.get("encryptionMethod");
                if (encryption != null && !encryption.trim().isEmpty()) {
                    this.encryptionMethod = encryption;
                }
                
                String auth = (String) config.get("authenticationMethod");
                if (auth != null && !auth.trim().isEmpty()) {
                    this.authenticationMethod = auth;
                }
                
                Boolean mtls = (Boolean) config.get("requireMutualTLS");
                if (mtls != null) {
                    this.requireMutualTLS = mtls;
                }
            }
        }
        
        public boolean isEnabled() { return enabled; }
        public String getEncryptionMethod() { return encryptionMethod; }
        public String getAuthenticationMethod() { return authenticationMethod; }
        public boolean requiresMutualTLS() { return requireMutualTLS; }
    }
}