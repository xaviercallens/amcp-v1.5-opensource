package io.amcp.cloudevents;

/**
 * CloudEvent represents a standardized event format based on the CloudEvents specification.
 * This is a simplified implementation for the AMCP messaging system.
 */
public class CloudEvent {
    private String id;
    private String source;
    private String specVersion;
    private String type;
    private String dataContentType;
    private Object data;
    private String subject;
    private String time;
    private java.util.Map<String, Object> extensions;
    
    public CloudEvent() {
        this.specVersion = "1.0";
        this.extensions = new java.util.HashMap<>();
    }
    
    public CloudEvent(String id, String source, String type, Object data) {
        this();
        this.id = id;
        this.source = source;
        this.type = type;
        this.data = data;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public String getSpecVersion() { return specVersion; }
    public void setSpecVersion(String specVersion) { this.specVersion = specVersion; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getDataContentType() { return dataContentType; }
    public void setDataContentType(String dataContentType) { this.dataContentType = dataContentType; }
    
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    
    public java.util.Map<String, Object> getExtensions() { return extensions; }
    public void setExtensions(java.util.Map<String, Object> extensions) { this.extensions = extensions; }
    
    public void addExtension(String key, Object value) {
        this.extensions.put(key, value);
    }
    
    public Object getExtension(String key) {
        return this.extensions.get(key);
    }
    
    @Override
    public String toString() {
        return String.format("CloudEvent{id='%s', source='%s', type='%s', specVersion='%s'}", 
                           id, source, type, specVersion);
    }
}