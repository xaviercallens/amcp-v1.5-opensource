package io.amcp.mobility.atp;

import io.amcp.core.AgentID;
import io.amcp.mobility.MobilityState;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * AMCP v1.4 Agent Transfer Protocol (ATP) Implementation.
 * Provides secure, reliable agent migration capabilities between AMCP environments
 * following the IBM Aglets ATP specification with modern enhancements.
 */
public class ATPManager {
    
    private static final String ATP_VERSION = "1.4.0";
    private static final int DEFAULT_PORT = 4434; // Traditional ATP port
    
    private final Map<String, ATPConnection> connections = new ConcurrentHashMap<>();
    private final Map<String, PendingMigration> pendingMigrations = new ConcurrentHashMap<>();
    private final ATPSecurityManager securityManager;
    private final ATPMessageHandler messageHandler;
    
    private volatile boolean started = false;
    private String localHostId;
    private int port;
    
    public ATPManager() {
        this(null, DEFAULT_PORT);
    }
    
    public ATPManager(String localHostId, int port) {
        this.localHostId = localHostId != null ? localHostId : generateHostId();
        this.port = port;
        this.securityManager = new ATPSecurityManager();
        this.messageHandler = new ATPMessageHandler(this);
    }
    
    /**
     * Start the ATP manager and begin listening for connections
     */
    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(() -> {
            if (started) {
                return;
            }
            
            try {
                // Initialize security manager
                securityManager.initialize();
                
                // Start message handler
                messageHandler.start(port);
                
                this.started = true;
                
                logMessage("ATP Manager started on host " + localHostId + " port " + port);
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to start ATP Manager", e);
            }
        });
    }
    
    /**
     * Migrate an agent to a remote host
     */
    public CompletableFuture<ATPMigrationResult> migrateAgent(AgentID agentId, String targetHost, 
                                                             int targetPort, MobilityState state) {
        return CompletableFuture.supplyAsync(() -> {
            if (!started) {
                throw new IllegalStateException("ATP Manager not started");
            }
            
            try {
                // Generate migration ID
                String migrationId = generateMigrationId(agentId);
                
                // Create ATP connection to target host
                ATPConnection connection = getOrCreateConnection(targetHost, targetPort);
                
                // Prepare migration request
                ATPMigrationRequest request = new ATPMigrationRequest(
                    migrationId,
                    agentId,
                    localHostId,
                    targetHost,
                    state,
                    System.currentTimeMillis()
                );
                
                // Register pending migration
                PendingMigration pending = new PendingMigration(
                    migrationId, agentId, request, System.currentTimeMillis()
                );
                pendingMigrations.put(migrationId, pending);
                
                // Send migration request
                ATPMessage atpMessage = new ATPMessage(
                    ATPMessage.MessageType.MIGRATE_REQUEST,
                    localHostId,
                    targetHost,
                    migrationId,
                    request
                );
                
                boolean sent = connection.sendMessage(atpMessage);
                if (!sent) {
                    pendingMigrations.remove(migrationId);
                    throw new RuntimeException("Failed to send migration request");
                }
                
                // Wait for response (with timeout)
                ATPMigrationResult result = waitForMigrationResult(migrationId, 30000); // 30 second timeout
                
                logMessage("Migration " + migrationId + " completed with status: " + result.getStatus());
                
                return result;
                
            } catch (Exception e) {
                throw new RuntimeException("Migration failed for agent " + agentId, e);
            }
        });
    }
    
    /**
     * Handle incoming agent migration from remote host
     */
    public CompletableFuture<ATPMigrationResult> receiveAgent(ATPMigrationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logMessage("Receiving agent migration: " + request.getMigrationId());
                
                // Validate migration request
                if (!validateMigrationRequest(request)) {
                    return ATPMigrationResult.failure(
                        request.getMigrationId(),
                        "Migration request validation failed"
                    );
                }
                
                // Check security permissions
                if (!securityManager.isAuthorized(request.getSourceHost(), request.getAgentId())) {
                    return ATPMigrationResult.failure(
                        request.getMigrationId(),
                        "Migration not authorized"
                    );
                }
                
                // Prepare agent environment
                boolean environmentReady = prepareAgentEnvironment(request);
                if (!environmentReady) {
                    return ATPMigrationResult.failure(
                        request.getMigrationId(),
                        "Failed to prepare agent environment"
                    );
                }
                
                // Deserialize and restore agent state
                boolean stateRestored = restoreAgentState(request.getAgentId(), request.getMobilityState());
                if (!stateRestored) {
                    return ATPMigrationResult.failure(
                        request.getMigrationId(),
                        "Failed to restore agent state"
                    );
                }
                
                // Activate agent in local context
                boolean activated = activateAgent(request.getAgentId());
                if (!activated) {
                    return ATPMigrationResult.failure(
                        request.getMigrationId(),
                        "Failed to activate agent"
                    );
                }
                
                return ATPMigrationResult.success(
                    request.getMigrationId(),
                    localHostId,
                    "Agent successfully migrated and activated"
                );
                
            } catch (Exception e) {
                return ATPMigrationResult.failure(
                    request.getMigrationId(),
                    "Migration failed: " + e.getMessage()
                );
            }
        });
    }
    
    /**
     * Establish ATP connection to remote host
     */
    public CompletableFuture<ATPConnection> establishConnection(String targetHost, int targetPort) {
        return CompletableFuture.supplyAsync(() -> {
            String connectionId = targetHost + ":" + targetPort;
            
            ATPConnection existing = connections.get(connectionId);
            if (existing != null && existing.isHealthy()) {
                return existing;
            }
            
            try {
                ATPConnection connection = new ATPConnection(
                    connectionId,
                    localHostId,
                    targetHost,
                    targetPort,
                    securityManager
                );
                
                boolean connected = connection.connect();
                if (!connected) {
                    throw new RuntimeException("Failed to connect to " + targetHost + ":" + targetPort);
                }
                
                connections.put(connectionId, connection);
                
                logMessage("Established ATP connection to " + targetHost + ":" + targetPort);
                
                return connection;
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to establish ATP connection", e);
            }
        });
    }
    
    /**
     * Get ATP protocol status and statistics
     */
    public ATPStatus getStatus() {
        return new ATPStatus(
            localHostId,
            port,
            started,
            connections.size(),
            pendingMigrations.size(),
            ATP_VERSION
        );
    }
    
    public String getLocalHostId() {
        return localHostId;
    }
    
    /**
     * Shutdown ATP manager and close all connections
     */
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            this.started = false;
            
            // Close all connections
            for (ATPConnection connection : connections.values()) {
                try {
                    connection.close();
                } catch (Exception e) {
                    logMessage("Error closing ATP connection: " + e.getMessage());
                }
            }
            connections.clear();
            
            // Clear pending migrations
            pendingMigrations.clear();
            
            // Shutdown message handler
            if (messageHandler != null) {
                messageHandler.shutdown();
            }
            
            // Shutdown security manager
            if (securityManager != null) {
                securityManager.shutdown();
            }
            
            logMessage("ATP Manager shutdown completed");
        });
    }
    
    private ATPConnection getOrCreateConnection(String targetHost, int targetPort) throws Exception {
        String connectionId = targetHost + ":" + targetPort;
        
        ATPConnection connection = connections.get(connectionId);
        if (connection != null && connection.isHealthy()) {
            return connection;
        }
        
        // Establish new connection
        return establishConnection(targetHost, targetPort).get();
    }
    
    private boolean validateMigrationRequest(ATPMigrationRequest request) {
        if (request.getAgentId() == null) return false;
        if (request.getSourceHost() == null || request.getSourceHost().trim().isEmpty()) return false;
        if (request.getMobilityState() == null) return false;
        if (request.getTimestamp() <= 0) return false;
        
        // Check if request is not too old (prevent replay attacks)
        long maxAge = 300000; // 5 minutes
        if (System.currentTimeMillis() - request.getTimestamp() > maxAge) {
            return false;
        }
        
        return true;
    }
    
    private boolean prepareAgentEnvironment(ATPMigrationRequest request) {
        // Simulate environment preparation
        try {
            Thread.sleep(10); // Simulate preparation time
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    private boolean restoreAgentState(AgentID agentId, MobilityState state) {
        // Simulate state restoration
        try {
            Thread.sleep(20); // Simulate state restoration time
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    private boolean activateAgent(AgentID agentId) {
        // Simulate agent activation
        try {
            Thread.sleep(10); // Simulate activation time
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    private ATPMigrationResult waitForMigrationResult(String migrationId, int timeoutMs) {
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            PendingMigration pending = pendingMigrations.get(migrationId);
            if (pending != null && pending.getResult() != null) {
                pendingMigrations.remove(migrationId);
                return pending.getResult();
            }
            
            try {
                Thread.sleep(100); // Poll every 100ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // Timeout
        pendingMigrations.remove(migrationId);
        return ATPMigrationResult.failure(migrationId, "Migration timeout");
    }
    
    private String generateHostId() {
        return "host_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
    
    private String generateMigrationId(AgentID agentId) {
        return "migration_" + agentId.toString() + "_" + System.currentTimeMillis();
    }
    
    private void logMessage(String message) {
        System.out.println("[ATP] " + message);
    }
    
    // Package-private method for message handler
    void handleMigrationResponse(String migrationId, ATPMigrationResult result) {
        PendingMigration pending = pendingMigrations.get(migrationId);
        if (pending != null) {
            pending.setResult(result);
        }
    }
    
    // Inner classes
    
    public static class ATPMigrationRequest implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final String migrationId;
        private final AgentID agentId;
        private final String sourceHost;
        private final String targetHost;
        private final MobilityState mobilityState;
        private final long timestamp;
        
        public ATPMigrationRequest(String migrationId, AgentID agentId, String sourceHost,
                                  String targetHost, MobilityState mobilityState, long timestamp) {
            this.migrationId = migrationId;
            this.agentId = agentId;
            this.sourceHost = sourceHost;
            this.targetHost = targetHost;
            this.mobilityState = mobilityState;
            this.timestamp = timestamp;
        }
        
        public String getMigrationId() { return migrationId; }
        public AgentID getAgentId() { return agentId; }
        public String getSourceHost() { return sourceHost; }
        public String getTargetHost() { return targetHost; }
        public MobilityState getMobilityState() { return mobilityState; }
        public long getTimestamp() { return timestamp; }
    }
    
    public static class ATPMigrationResult implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final String migrationId;
        private final boolean success;
        private final String message;
        private final String targetHost;
        private final long timestamp;
        
        private ATPMigrationResult(String migrationId, boolean success, String message, 
                                  String targetHost, long timestamp) {
            this.migrationId = migrationId;
            this.success = success;
            this.message = message;
            this.targetHost = targetHost;
            this.timestamp = timestamp;
        }
        
        public static ATPMigrationResult success(String migrationId, String targetHost, String message) {
            return new ATPMigrationResult(migrationId, true, message, targetHost, System.currentTimeMillis());
        }
        
        public static ATPMigrationResult failure(String migrationId, String message) {
            return new ATPMigrationResult(migrationId, false, message, null, System.currentTimeMillis());
        }
        
        public String getMigrationId() { return migrationId; }
        public boolean isSuccess() { return success; }
        public String getStatus() { return success ? "SUCCESS" : "FAILURE"; }
        public String getMessage() { return message; }
        public String getTargetHost() { return targetHost; }
        public long getTimestamp() { return timestamp; }
    }
    
    public static class ATPStatus {
        private final String localHostId;
        private final int port;
        private final boolean started;
        private final int activeConnections;
        private final int pendingMigrations;
        private final String version;
        
        public ATPStatus(String localHostId, int port, boolean started, int activeConnections,
                        int pendingMigrations, String version) {
            this.localHostId = localHostId;
            this.port = port;
            this.started = started;
            this.activeConnections = activeConnections;
            this.pendingMigrations = pendingMigrations;
            this.version = version;
        }
        
        public String getLocalHostId() { return localHostId; }
        public int getPort() { return port; }
        public boolean isStarted() { return started; }
        public int getActiveConnections() { return activeConnections; }
        public int getPendingMigrations() { return pendingMigrations; }
        public String getVersion() { return version; }
    }
    
    private static class PendingMigration {
        private final String migrationId;
        private final AgentID agentId;
        private final ATPMigrationRequest request;
        private final long startTime;
        private volatile ATPMigrationResult result;
        
        public PendingMigration(String migrationId, AgentID agentId, ATPMigrationRequest request, long startTime) {
            this.migrationId = migrationId;
            this.agentId = agentId;
            this.request = request;
            this.startTime = startTime;
        }
        
        public String getMigrationId() { return migrationId; }
        public AgentID getAgentId() { return agentId; }
        public ATPMigrationRequest getRequest() { return request; }
        public long getStartTime() { return startTime; }
        public ATPMigrationResult getResult() { return result; }
        public void setResult(ATPMigrationResult result) { this.result = result; }
    }
}