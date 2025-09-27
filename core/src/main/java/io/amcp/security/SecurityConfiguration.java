package io.amcp.security;

/**
 * Security Configuration for AMCP v1.5 Enterprise Edition
 * 
 * Configuration settings for the Advanced Security Suite including:
 * - Session management parameters
 * - Authentication settings
 * - Security policies
 * - Audit configuration
 */
public class SecurityConfiguration {
    
    private final long defaultSessionTimeout; // seconds
    private final long maxIdleTime; // seconds
    private final int maxConcurrentSessions;
    private final boolean requireMfa;
    private final boolean auditEnabled;
    private final String jwtIssuer;
    private final long jwtExpiration; // seconds
    private final boolean certificateAuthEnabled;
    private final int passwordMinLength;
    private final boolean passwordComplexityRequired;
    private final int maxFailedAttempts;
    private final long lockoutDuration; // seconds
    
    private SecurityConfiguration(Builder builder) {
        this.defaultSessionTimeout = builder.defaultSessionTimeout;
        this.maxIdleTime = builder.maxIdleTime;
        this.maxConcurrentSessions = builder.maxConcurrentSessions;
        this.requireMfa = builder.requireMfa;
        this.auditEnabled = builder.auditEnabled;
        this.jwtIssuer = builder.jwtIssuer;
        this.jwtExpiration = builder.jwtExpiration;
        this.certificateAuthEnabled = builder.certificateAuthEnabled;
        this.passwordMinLength = builder.passwordMinLength;
        this.passwordComplexityRequired = builder.passwordComplexityRequired;
        this.maxFailedAttempts = builder.maxFailedAttempts;
        this.lockoutDuration = builder.lockoutDuration;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static SecurityConfiguration defaultConfig() {
        return builder().build();
    }
    
    public long getDefaultSessionTimeout() {
        return defaultSessionTimeout;
    }
    
    public long getMaxIdleTime() {
        return maxIdleTime;
    }
    
    public int getMaxConcurrentSessions() {
        return maxConcurrentSessions;
    }
    
    public boolean isRequireMfa() {
        return requireMfa;
    }
    
    public boolean isAuditEnabled() {
        return auditEnabled;
    }
    
    public String getJwtIssuer() {
        return jwtIssuer;
    }
    
    public long getJwtExpiration() {
        return jwtExpiration;
    }
    
    public boolean isCertificateAuthEnabled() {
        return certificateAuthEnabled;
    }
    
    public int getPasswordMinLength() {
        return passwordMinLength;
    }
    
    public boolean isPasswordComplexityRequired() {
        return passwordComplexityRequired;
    }
    
    public int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }
    
    public long getLockoutDuration() {
        return lockoutDuration;
    }
    
    public static class Builder {
        private long defaultSessionTimeout = 1800; // 30 minutes
        private long maxIdleTime = 900; // 15 minutes
        private int maxConcurrentSessions = 5;
        private boolean requireMfa = false;
        private boolean auditEnabled = true;
        private String jwtIssuer = "amcp-security";
        private long jwtExpiration = 3600; // 1 hour
        private boolean certificateAuthEnabled = true;
        private int passwordMinLength = 8;
        private boolean passwordComplexityRequired = true;
        private int maxFailedAttempts = 3;
        private long lockoutDuration = 300; // 5 minutes
        
        public Builder defaultSessionTimeout(long seconds) {
            this.defaultSessionTimeout = seconds;
            return this;
        }
        
        public Builder maxIdleTime(long seconds) {
            this.maxIdleTime = seconds;
            return this;
        }
        
        public Builder maxConcurrentSessions(int max) {
            this.maxConcurrentSessions = max;
            return this;
        }
        
        public Builder requireMfa(boolean require) {
            this.requireMfa = require;
            return this;
        }
        
        public Builder auditEnabled(boolean enabled) {
            this.auditEnabled = enabled;
            return this;
        }
        
        public Builder jwtIssuer(String issuer) {
            this.jwtIssuer = issuer;
            return this;
        }
        
        public Builder jwtExpiration(long seconds) {
            this.jwtExpiration = seconds;
            return this;
        }
        
        public Builder certificateAuthEnabled(boolean enabled) {
            this.certificateAuthEnabled = enabled;
            return this;
        }
        
        public Builder passwordMinLength(int length) {
            this.passwordMinLength = length;
            return this;
        }
        
        public Builder passwordComplexityRequired(boolean required) {
            this.passwordComplexityRequired = required;
            return this;
        }
        
        public Builder maxFailedAttempts(int max) {
            this.maxFailedAttempts = max;
            return this;
        }
        
        public Builder lockoutDuration(long seconds) {
            this.lockoutDuration = seconds;
            return this;
        }
        
        public SecurityConfiguration build() {
            return new SecurityConfiguration(this);
        }
    }
}