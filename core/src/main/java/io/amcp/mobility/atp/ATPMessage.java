package io.amcp.mobility.atp;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;

/**
 * AMCP v1.4 ATP Message.
 * Represents a message in the Agent Transfer Protocol communication.
 */
public class ATPMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum MessageType {
        HANDSHAKE_REQUEST,
        HANDSHAKE_RESPONSE,
        MIGRATE_REQUEST,
        MIGRATE_RESPONSE,
        HEARTBEAT,
        ERROR,
        CLOSE_CONNECTION
    }
    
    private final MessageType type;
    private final String sourceHost;
    private final String targetHost;
    private final String messageId;
    private final Object payload;
    private final long timestamp;
    private final Map<String, Object> headers;
    
    public ATPMessage(MessageType type, String sourceHost, String targetHost, 
                     String messageId, Object payload) {
        this.type = type;
        this.sourceHost = sourceHost;
        this.targetHost = targetHost;
        this.messageId = messageId;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
        this.headers = new HashMap<>();
    }
    
    public ATPMessage(MessageType type, String sourceHost, String targetHost,
                     String messageId, Object payload, Map<String, Object> headers) {
        this.type = type;
        this.sourceHost = sourceHost;
        this.targetHost = targetHost;
        this.messageId = messageId;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
        this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
    }
    
    public MessageType getType() { return type; }
    public String getSourceHost() { return sourceHost; }
    public String getTargetHost() { return targetHost; }
    public String getMessageId() { return messageId; }
    public Object getPayload() { return payload; }
    public long getTimestamp() { return timestamp; }
    public Map<String, Object> getHeaders() { return new HashMap<>(headers); }
    
    public <T> T getPayload(Class<T> type) {
        if (payload != null && type.isAssignableFrom(payload.getClass())) {
            return type.cast(payload);
        }
        return null;
    }
    
    public Object getHeader(String key) {
        return headers.get(key);
    }
    
    public void setHeader(String key, Object value) {
        headers.put(key, value);
    }
    
    @Override
    public String toString() {
        return "ATPMessage{" +
                "type=" + type +
                ", sourceHost='" + sourceHost + '\'' +
                ", targetHost='" + targetHost + '\'' +
                ", messageId='" + messageId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}