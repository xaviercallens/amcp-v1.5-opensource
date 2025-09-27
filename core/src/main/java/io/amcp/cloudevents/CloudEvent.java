package io.amcp.cloudevents;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * CloudEvents v1.0 specification implementation for AMCP Enterprise Edition.
 * 
 * <p>This class provides full compliance with the CloudEvents v1.0 specification,
 * including all required and optional context attributes, proper JSON serialization,
 * and validation capabilities.</p>
 * 
 * <p><strong>CloudEvents Context Attributes:</strong></p>
 * <ul>
 *   <li><strong>Required:</strong> specversion, type, source, id</li>
 *   <li><strong>Optional:</strong> time, datacontenttype, dataschema, subject</li>
 *   <li><strong>Extension:</strong> Custom attributes for AMCP-specific metadata</li>
 * </ul>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>
 * CloudEvent event = CloudEvent.builder()
 *     .specVersion("1.0")
 *     .type("io.amcp.weather.updated")
 *     .source(URI.create("//weather-agent/paris"))
 *     .id("weather-paris-2024-001")
 *     .time(OffsetDateTime.now())
 *     .dataContentType("application/json")
 *     .subject("weather/paris/current")
 *     .data("{\"temperature\": 22.5, \"condition\": \"sunny\"}")
 *     .extension("amcp-agent-id", "weather-agent-001")
 *     .build();
 * </pre>
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 * @see <a href="https://github.com/cloudevents/spec/blob/v1.0.2/cloudevents/spec.md">CloudEvents v1.0 Specification</a>
 */
public class CloudEvent {
    
    // Required context attributes
    @JsonProperty("specversion")
    private final String specVersion;
    
    @JsonProperty("type")
    private final String type;
    
    @JsonProperty("source")
    private final URI source;
    
    @JsonProperty("id")
    private final String id;
    
    // Optional context attributes
    @JsonProperty("time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private final OffsetDateTime time;
    
    @JsonProperty("datacontenttype")
    private final String dataContentType;
    
    @JsonProperty("dataschema")
    private final URI dataSchema;
    
    @JsonProperty("subject")
    private final String subject;
    
    // Event data
    @JsonProperty("data")
    private final Object data;
    
    // Extension attributes
    private final Map<String, Object> extensions;
    
    // Static ObjectMapper for JSON operations
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule());
    
    /**
     * Private constructor for builder pattern.
     */
    private CloudEvent(Builder builder) {
        this.specVersion = builder.specVersion;
        this.type = builder.type;
        this.source = builder.source;
        this.id = builder.id;
        this.time = builder.time;
        this.dataContentType = builder.dataContentType;
        this.dataSchema = builder.dataSchema;
        this.subject = builder.subject;
        this.data = builder.data;
        this.extensions = new HashMap<>(builder.extensions);
    }
    
    /**
     * Default constructor for JSON deserialization.
     */
    public CloudEvent() {
        this.specVersion = null;
        this.type = null;
        this.source = null;
        this.id = null;
        this.time = null;
        this.dataContentType = null;
        this.dataSchema = null;
        this.subject = null;
        this.data = null;
        this.extensions = new HashMap<>();
    }
    
    // Getters
    public String getSpecVersion() { return specVersion; }
    public String getType() { return type; }
    public URI getSource() { return source; }
    public String getId() { return id; }
    public Optional<OffsetDateTime> getTime() { return Optional.ofNullable(time); }
    public Optional<String> getDataContentType() { return Optional.ofNullable(dataContentType); }
    public Optional<URI> getDataSchema() { return Optional.ofNullable(dataSchema); }
    public Optional<String> getSubject() { return Optional.ofNullable(subject); }
    public Optional<Object> getData() { return Optional.ofNullable(data); }
    
    /**
     * Get extension attributes for JSON serialization.
     */
    @JsonAnyGetter
    public Map<String, Object> getExtensions() {
        return new HashMap<>(extensions);
    }
    
    /**
     * Set extension attributes for JSON deserialization.
     */
    @JsonAnySetter
    public void setExtension(String name, Object value) {
        if (!isContextAttribute(name)) {
            extensions.put(name, value);
        }
    }
    
    /**
     * Get a specific extension attribute.
     */
    public Optional<Object> getExtension(String name) {
        return Optional.ofNullable(extensions.get(name));
    }
    
    /**
     * Check if a given name is a standard CloudEvents context attribute.
     */
    @JsonIgnore
    private boolean isContextAttribute(String name) {
        return "specversion".equals(name) || "type".equals(name) || 
               "source".equals(name) || "id".equals(name) || 
               "time".equals(name) || "datacontenttype".equals(name) || 
               "dataschema".equals(name) || "subject".equals(name) || 
               "data".equals(name);
    }
    
    /**
     * Validate this CloudEvent according to v1.0 specification.
     * 
     * @throws CloudEventValidationException if validation fails
     */
    public void validate() throws CloudEventValidationException {
        if (specVersion == null || specVersion.trim().isEmpty()) {
            throw new CloudEventValidationException("specversion is required");
        }
        if (!"1.0".equals(specVersion)) {
            throw new CloudEventValidationException("specversion must be '1.0'");
        }
        if (type == null || type.trim().isEmpty()) {
            throw new CloudEventValidationException("type is required");
        }
        if (source == null) {
            throw new CloudEventValidationException("source is required");
        }
        if (id == null || id.trim().isEmpty()) {
            throw new CloudEventValidationException("id is required");
        }
        
        // Validate extension attribute names (must not start with 'ce-')
        for (String extName : extensions.keySet()) {
            if (extName.startsWith("ce-")) {
                throw new CloudEventValidationException(
                    "Extension attribute names must not start with 'ce-': " + extName);
            }
        }
    }
    
    /**
     * Convert this CloudEvent to JSON string.
     */
    public String toJson() throws CloudEventException {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (Exception e) {
            throw new CloudEventException("Failed to serialize CloudEvent to JSON", e);
        }
    }
    
    /**
     * Create a CloudEvent from JSON string.
     */
    public static CloudEvent fromJson(String json) throws CloudEventException {
        try {
            CloudEvent event = OBJECT_MAPPER.readValue(json, CloudEvent.class);
            event.validate();
            return event;
        } catch (CloudEventValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new CloudEventException("Failed to deserialize CloudEvent from JSON", e);
        }
    }
    
    /**
     * Create a new Builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Create a new Builder instance based on this CloudEvent.
     */
    public Builder toBuilder() {
        return new Builder()
            .specVersion(this.specVersion)
            .type(this.type)
            .source(this.source)
            .id(this.id)
            .time(this.time)
            .dataContentType(this.dataContentType)
            .dataSchema(this.dataSchema)
            .subject(this.subject)
            .data(this.data)
            .extensions(this.extensions);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CloudEvent that = (CloudEvent) o;
        return Objects.equals(specVersion, that.specVersion) &&
               Objects.equals(type, that.type) &&
               Objects.equals(source, that.source) &&
               Objects.equals(id, that.id) &&
               Objects.equals(time, that.time) &&
               Objects.equals(dataContentType, that.dataContentType) &&
               Objects.equals(dataSchema, that.dataSchema) &&
               Objects.equals(subject, that.subject) &&
               Objects.equals(data, that.data) &&
               Objects.equals(extensions, that.extensions);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(specVersion, type, source, id, time, 
                           dataContentType, dataSchema, subject, data, extensions);
    }
    
    @Override
    public String toString() {
        return "CloudEvent{" +
               "specVersion='" + specVersion + '\'' +
               ", type='" + type + '\'' +
               ", source=" + source +
               ", id='" + id + '\'' +
               ", time=" + time +
               ", dataContentType='" + dataContentType + '\'' +
               ", subject='" + subject + '\'' +
               ", extensionsCount=" + extensions.size() +
               '}';
    }
    
    /**
     * Builder class for creating CloudEvent instances.
     */
    public static class Builder {
        private String specVersion = "1.0";
        private String type;
        private URI source;
        private String id;
        private OffsetDateTime time;
        private String dataContentType;
        private URI dataSchema;
        private String subject;
        private Object data;
        private Map<String, Object> extensions = new HashMap<>();
        
        public Builder specVersion(String specVersion) {
            this.specVersion = specVersion;
            return this;
        }
        
        public Builder type(String type) {
            this.type = type;
            return this;
        }
        
        public Builder source(URI source) {
            this.source = source;
            return this;
        }
        
        public Builder source(String source) {
            this.source = URI.create(source);
            return this;
        }
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder time(OffsetDateTime time) {
            this.time = time;
            return this;
        }
        
        public Builder dataContentType(String dataContentType) {
            this.dataContentType = dataContentType;
            return this;
        }
        
        public Builder dataSchema(URI dataSchema) {
            this.dataSchema = dataSchema;
            return this;
        }
        
        public Builder dataSchema(String dataSchema) {
            this.dataSchema = URI.create(dataSchema);
            return this;
        }
        
        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }
        
        public Builder data(Object data) {
            this.data = data;
            return this;
        }
        
        public Builder extension(String name, Object value) {
            this.extensions.put(name, value);
            return this;
        }
        
        public Builder extensions(Map<String, Object> extensions) {
            this.extensions.putAll(extensions);
            return this;
        }
        
        /**
         * Build the CloudEvent instance.
         * 
         * @return A new CloudEvent instance
         * @throws CloudEventValidationException if validation fails
         */
        public CloudEvent build() throws CloudEventValidationException {
            CloudEvent event = new CloudEvent(this);
            event.validate();
            return event;
        }
    }
}