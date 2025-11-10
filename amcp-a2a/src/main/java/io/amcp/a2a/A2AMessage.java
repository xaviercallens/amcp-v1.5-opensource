package io.amcp.a2a;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Represents an Agent-to-Agent (A2A) Protocol message.
 * 
 * Based on the A2A specification for agent communication.
 * Supports standard A2A performatives and content types.
 * 
 * @see <a href="https://github.com/google/a2a">A2A Protocol Spec</a>
 */
public class A2AMessage {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("sender")
    private String sender;
    
    @JsonProperty("receiver")
    private String receiver;
    
    @JsonProperty("performative")
    private String performative; // REQUEST, INFORM, QUERY, ACTION, etc.
    
    @JsonProperty("content")
    private Object content;
    
    @JsonProperty("content-type")
    private String contentType;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    @JsonProperty("reply-to")
    private String replyTo;
    
    @JsonProperty("conversation-id")
    private String conversationId;

    public A2AMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public A2AMessage(String id, String sender, String receiver, 
                      String performative, Object content) {
        this();
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.performative = performative;
        this.content = content;
    }

    // Getters and Setters
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getPerformative() {
        return performative;
    }

    public void setPerformative(String performative) {
        this.performative = performative;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    @Override
    public String toString() {
        return "A2AMessage{" +
                "id='" + id + '\'' +
                ", sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", performative='" + performative + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
