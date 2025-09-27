package io.amcp.security;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Security Audit Entry for AMCP v1.5 Enterprise Edition
 * 
 * Immutable record of security-related events for compliance and monitoring.
 * Supports comprehensive audit trails for:
 * - Authentication events
 * - Authorization decisions
 * - Resource access attempts
 * - Administrative actions
 * - Security violations
 * 
 * Thread-safe and serializable for persistent audit logging.
 */
public class SecurityAuditEntry {
    
    private final String principalId;
    private final String tenantId;
    private final String sessionId;
    private final String action;
    private final String resource;
    private final boolean success;
    private final Instant timestamp;
    private final Map<String, String> metadata;
    private final String ipAddress;
    private final String userAgent;
    private final String errorMessage;
    private final long duration;
    
    private SecurityAuditEntry(Builder builder) {
        this.principalId = builder.principalId;
        this.tenantId = builder.tenantId;
        this.sessionId = builder.sessionId;
        this.action = builder.action;
        this.resource = builder.resource;
        this.success = builder.success;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.metadata = Collections.unmodifiableMap(new HashMap<>(builder.metadata));
        this.ipAddress = builder.ipAddress;
        this.userAgent = builder.userAgent;
        this.errorMessage = builder.errorMessage;
        this.duration = builder.duration;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public String getPrincipalId() {
        return principalId;
    }
    
    public String getTenantId() {
        return tenantId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public String getAction() {
        return action;
    }
    
    public String getResource() {
        return resource;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    public String getMetadata(String key) {
        return metadata.get(key);
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public long getDuration() {
        return duration;
    }
    
    /**
     * Check if this is a security violation (failed authentication, authorization, etc.)
     */
    public boolean isSecurityViolation() {
        if (success) {
            return false;
        }
        
        return action.contains("auth") || 
               action.contains("login") || 
               action.contains("access") ||
               action.contains("permission");
    }
    
    /**
     * Check if this is a high-risk operation
     */
    public boolean isHighRiskOperation() {
        return action.contains("delete") ||
               action.contains("admin") ||
               action.contains("system") ||
               resource.contains("admin") ||
               resource.contains("system");
    }
    
    /**
     * Get severity level based on action and success
     */
    public AuditSeverity getSeverity() {
        if (!success && isSecurityViolation()) {
            return AuditSeverity.HIGH;
        }
        
        if (isHighRiskOperation()) {
            return success ? AuditSeverity.MEDIUM : AuditSeverity.HIGH;
        }
        
        return success ? AuditSeverity.LOW : AuditSeverity.MEDIUM;
    }
    
    /**
     * Format as structured log entry
     */
    public String toLogEntry() {
        StringBuilder sb = new StringBuilder();
        sb.append("SECURITY_AUDIT ");
        sb.append("principal=").append(principalId).append(" ");
        sb.append("tenant=").append(tenantId).append(" ");
        sb.append("action=").append(action).append(" ");
        sb.append("resource=").append(resource).append(" ");
        sb.append("success=").append(success).append(" ");
        sb.append("timestamp=").append(timestamp).append(" ");
        sb.append("severity=").append(getSeverity()).append(" ");
        
        if (ipAddress != null) {
            sb.append("ip=").append(ipAddress).append(" ");
        }
        
        if (duration > 0) {
            sb.append("duration_ms=").append(duration).append(" ");
        }
        
        if (errorMessage != null) {
            sb.append("error=\"").append(errorMessage).append("\" ");
        }
        
        if (!metadata.isEmpty()) {
            sb.append("metadata={");
            metadata.forEach((k, v) -> sb.append(k).append("=").append(v).append(","));
            sb.setLength(sb.length() - 1); // remove last comma
            sb.append("}");
        }
        
        return sb.toString();
    }
    
    /**
     * Format as JSON for structured logging systems
     */
    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"type\":\"security_audit\",");
        json.append("\"principalId\":\"").append(escapeJson(principalId)).append("\",");
        json.append("\"tenantId\":\"").append(escapeJson(tenantId)).append("\",");
        json.append("\"sessionId\":\"").append(escapeJson(sessionId)).append("\",");
        json.append("\"action\":\"").append(escapeJson(action)).append("\",");
        json.append("\"resource\":\"").append(escapeJson(resource)).append("\",");
        json.append("\"success\":").append(success).append(",");
        json.append("\"timestamp\":\"").append(timestamp).append("\",");
        json.append("\"severity\":\"").append(getSeverity()).append("\"");
        
        if (ipAddress != null) {
            json.append(",\"ipAddress\":\"").append(escapeJson(ipAddress)).append("\"");
        }
        
        if (userAgent != null) {
            json.append(",\"userAgent\":\"").append(escapeJson(userAgent)).append("\"");
        }
        
        if (errorMessage != null) {
            json.append(",\"errorMessage\":\"").append(escapeJson(errorMessage)).append("\"");
        }
        
        if (duration > 0) {
            json.append(",\"durationMs\":").append(duration);
        }
        
        if (!metadata.isEmpty()) {
            json.append(",\"metadata\":{");
            metadata.forEach((k, v) -> {
                json.append("\"").append(escapeJson(k)).append("\":\"")
                    .append(escapeJson(v)).append("\",");
            });
            json.setLength(json.length() - 1); // remove last comma
            json.append("}");
        }
        
        json.append("}");
        return json.toString();
    }
    
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SecurityAuditEntry)) return false;
        SecurityAuditEntry that = (SecurityAuditEntry) o;
        return success == that.success &&
               duration == that.duration &&
               Objects.equals(principalId, that.principalId) &&
               Objects.equals(tenantId, that.tenantId) &&
               Objects.equals(sessionId, that.sessionId) &&
               Objects.equals(action, that.action) &&
               Objects.equals(resource, that.resource) &&
               Objects.equals(timestamp, that.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(principalId, tenantId, sessionId, action, resource, success, timestamp);
    }
    
    @Override
    public String toString() {
        return String.format(
            "SecurityAuditEntry{principal='%s', action='%s', resource='%s', success=%s, timestamp=%s}",
            principalId, action, resource, success, timestamp
        );
    }
    
    public enum AuditSeverity {
        LOW("INFO"),
        MEDIUM("WARN"), 
        HIGH("ERROR"),
        CRITICAL("FATAL");
        
        private final String logLevel;
        
        AuditSeverity(String logLevel) {
            this.logLevel = logLevel;
        }
        
        public String getLogLevel() {
            return logLevel;
        }
    }
    
    public static class Builder {
        private String principalId;
        private String tenantId;
        private String sessionId;
        private String action;
        private String resource;
        private boolean success = true;
        private Instant timestamp;
        private Map<String, String> metadata = new HashMap<>();
        private String ipAddress;
        private String userAgent;
        private String errorMessage;
        private long duration = 0;
        
        public Builder principalId(String principalId) {
            this.principalId = principalId;
            return this;
        }
        
        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }
        
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public Builder action(String action) {
            this.action = action;
            return this;
        }
        
        public Builder resource(String resource) {
            this.resource = resource;
            return this;
        }
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder metadata(String key, String value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public Builder metadata(Map<String, String> metadata) {
            this.metadata.putAll(metadata);
            return this;
        }
        
        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }
        
        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public Builder duration(long duration) {
            this.duration = duration;
            return this;
        }
        
        public SecurityAuditEntry build() {
            if (principalId == null || principalId.trim().isEmpty()) {
                throw new IllegalArgumentException("Principal ID is required");
            }
            if (action == null || action.trim().isEmpty()) {
                throw new IllegalArgumentException("Action is required");
            }
            if (resource == null || resource.trim().isEmpty()) {
                throw new IllegalArgumentException("Resource is required");
            }
            
            return new SecurityAuditEntry(this);
        }
    }
}