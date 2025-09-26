package io.amcp.mobility.atp;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AMCP v1.4 ATP Connection.
 * Represents a connection between two AMCP hosts using the Agent Transfer Protocol.
 */
public class ATPConnection {
    
    private final String connectionId;
    private final String localHostId;
    private final String remoteHost;
    private final int remotePort;
    private final ATPSecurityManager securityManager;
    
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean healthy = new AtomicBoolean(false);
    private final AtomicLong messagesSent = new AtomicLong(0);
    private final AtomicLong messagesReceived = new AtomicLong(0);
    
    private final BlockingQueue<ATPMessage> outgoingMessages = new LinkedBlockingQueue<>();
    private final BlockingQueue<ATPMessage> incomingMessages = new LinkedBlockingQueue<>();
    
    private volatile long connectionTime;
    private volatile long lastHeartbeat;
    private String authenticationToken;
    
    public ATPConnection(String connectionId, String localHostId, String remoteHost, 
                        int remotePort, ATPSecurityManager securityManager) {
        this.connectionId = connectionId;
        this.localHostId = localHostId;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.securityManager = securityManager;
    }
    
    /**
     * Establish connection to remote host
     */
    public boolean connect() {
        try {
            // Simulate connection establishment
            Thread.sleep(100); // Simulate network delay
            
            // Perform handshake
            boolean handshakeSuccess = performHandshake();
            if (!handshakeSuccess) {
                return false;
            }
            
            this.connectionTime = System.currentTimeMillis();
            this.lastHeartbeat = System.currentTimeMillis();
            this.connected.set(true);
            this.healthy.set(true);
            
            // Start message processing threads
            startMessageProcessing();
            
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Send ATP message to remote host
     */
    public boolean sendMessage(ATPMessage message) {
        if (!isHealthy()) {
            return false;
        }
        
        try {
            // Add authentication token if available
            if (authenticationToken != null) {
                message.setHeader("auth_token", authenticationToken);
            }
            
            // Add to outgoing queue
            boolean queued = outgoingMessages.offer(message);
            if (queued) {
                messagesSent.incrementAndGet();
            }
            return queued;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Receive ATP message from remote host
     */
    public ATPMessage receiveMessage(int timeoutMs) {
        try {
            return incomingMessages.poll(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
    
    /**
     * Send heartbeat to remote host
     */
    public boolean sendHeartbeat() {
        ATPMessage heartbeat = new ATPMessage(
            ATPMessage.MessageType.HEARTBEAT,
            localHostId,
            remoteHost,
            "heartbeat_" + System.currentTimeMillis(),
            null
        );
        
        boolean sent = sendMessage(heartbeat);
        if (sent) {
            this.lastHeartbeat = System.currentTimeMillis();
        }
        return sent;
    }
    
    /**
     * Close connection
     */
    public void close() {
        this.connected.set(false);
        this.healthy.set(false);
        
        // Send close message
        if (isConnected()) {
            ATPMessage closeMsg = new ATPMessage(
                ATPMessage.MessageType.CLOSE_CONNECTION,
                localHostId,
                remoteHost,
                "close_" + System.currentTimeMillis(),
                null
            );
            outgoingMessages.offer(closeMsg);
        }
        
        // Clear queues
        outgoingMessages.clear();
        incomingMessages.clear();
    }
    
    /**
     * Check if connection is established
     */
    public boolean isConnected() {
        return connected.get();
    }
    
    /**
     * Check if connection is healthy
     */
    public boolean isHealthy() {
        if (!isConnected()) return false;
        
        // Check heartbeat age (5 minutes max)
        long heartbeatAge = System.currentTimeMillis() - lastHeartbeat;
        if (heartbeatAge > 300000) { // 5 minutes
            healthy.set(false);
            return false;
        }
        
        return healthy.get();
    }
    
    private boolean performHandshake() {
        try {
            // Create handshake request
            HandshakeRequest handshakeRequest = new HandshakeRequest(
                localHostId,
                "AMCP-ATP/1.4",
                System.currentTimeMillis()
            );
            
            ATPMessage handshakeMsg = new ATPMessage(
                ATPMessage.MessageType.HANDSHAKE_REQUEST,
                localHostId,
                remoteHost,
                "handshake_" + System.currentTimeMillis(),
                handshakeRequest
            );
            
            // Simulate sending handshake
            Thread.sleep(50); // Simulate network delay
            
            // Simulate receiving handshake response
            HandshakeResponse response = new HandshakeResponse(
                remoteHost,
                "AMCP-ATP/1.4",
                true,
                "Handshake successful",
                System.currentTimeMillis()
            );
            
            // Simulate authentication
            if (securityManager != null) {
                ATPSecurityManager.AuthenticationResult authResult = 
                    securityManager.authenticateHost(remoteHost, "default_credentials").get();
                
                if (authResult.isSuccess()) {
                    this.authenticationToken = authResult.getToken();
                } else {
                    return false;
                }
            }
            
            return response.isSuccess();
            
        } catch (Exception e) {
            return false;
        }
    }
    
    private void startMessageProcessing() {
        // Start outgoing message processor
        CompletableFuture.runAsync(() -> {
            while (isConnected()) {
                try {
                    ATPMessage message = outgoingMessages.take();
                    processOutgoingMessage(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // Log error and continue
                }
            }
        });
        
        // Start incoming message processor
        CompletableFuture.runAsync(() -> {
            while (isConnected()) {
                try {
                    // Simulate receiving messages
                    Thread.sleep(1000); // Check every second
                    
                    // In a real implementation, this would read from network socket
                    // For now, we'll just maintain the connection
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // Log error and continue
                }
            }
        });
    }
    
    private void processOutgoingMessage(ATPMessage message) {
        try {
            // Simulate message transmission
            Thread.sleep(10); // Network delay
            
            // In a real implementation, this would serialize and send the message
            // For now, we'll just track that it was processed
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // Package-private method for message handler
    void handleIncomingMessage(ATPMessage message) {
        incomingMessages.offer(message);
        messagesReceived.incrementAndGet();
    }
    
    // Getters
    public String getConnectionId() { return connectionId; }
    public String getLocalHostId() { return localHostId; }
    public String getRemoteHost() { return remoteHost; }
    public int getRemotePort() { return remotePort; }
    public String getTargetUrl() { return remoteHost + ":" + remotePort; }
    public long getConnectionTime() { return connectionTime; }
    public long getLastHeartbeat() { return lastHeartbeat; }
    public long getMessagesSent() { return messagesSent.get(); }
    public long getMessagesReceived() { return messagesReceived.get(); }
    public String getAuthenticationToken() { return authenticationToken; }
    
    // Inner classes
    
    private static class HandshakeRequest {
        private final String hostId;
        private final String protocolVersion;
        private final long timestamp;
        
        public HandshakeRequest(String hostId, String protocolVersion, long timestamp) {
            this.hostId = hostId;
            this.protocolVersion = protocolVersion;
            this.timestamp = timestamp;
        }
        
        public String getHostId() { return hostId; }
        public String getProtocolVersion() { return protocolVersion; }
        public long getTimestamp() { return timestamp; }
    }
    
    private static class HandshakeResponse {
        private final String hostId;
        private final String protocolVersion;
        private final boolean success;
        private final String message;
        private final long timestamp;
        
        public HandshakeResponse(String hostId, String protocolVersion, boolean success, 
                               String message, long timestamp) {
            this.hostId = hostId;
            this.protocolVersion = protocolVersion;
            this.success = success;
            this.message = message;
            this.timestamp = timestamp;
        }
        
        public String getHostId() { return hostId; }
        public String getProtocolVersion() { return protocolVersion; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
    }
}