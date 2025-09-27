package io.amcp.security;

import java.time.Instant;

/**
 * Security Statistics for AMCP v1.5 Enterprise Edition
 * 
 * Provides comprehensive security metrics and statistics including:
 * - Active sessions count
 * - User and certificate counts
 * - Policy and MFA statistics
 * - Security health metrics
 */
public class SecurityStatistics {
    
    private final int activeSessions;
    private final int totalUsers;
    private final int totalCertificates;
    private final int totalPolicies;
    private final int mfaEnabledUsers;
    private final Instant generatedAt;
    private final long uptime;
    
    private SecurityStatistics(Builder builder) {
        this.activeSessions = builder.activeSessions;
        this.totalUsers = builder.totalUsers;
        this.totalCertificates = builder.totalCertificates;
        this.totalPolicies = builder.totalPolicies;
        this.mfaEnabledUsers = builder.mfaEnabledUsers;
        this.generatedAt = Instant.now();
        this.uptime = builder.uptime;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public int getActiveSessions() {
        return activeSessions;
    }
    
    public int getTotalUsers() {
        return totalUsers;
    }
    
    public int getTotalCertificates() {
        return totalCertificates;
    }
    
    public int getTotalPolicies() {
        return totalPolicies;
    }
    
    public int getMfaEnabledUsers() {
        return mfaEnabledUsers;
    }
    
    public Instant getGeneratedAt() {
        return generatedAt;
    }
    
    public long getUptime() {
        return uptime;
    }
    
    public double getMfaAdoptionRate() {
        return totalUsers > 0 ? (double) mfaEnabledUsers / totalUsers * 100.0 : 0.0;
    }
    
    @Override
    public String toString() {
        return String.format(
            "SecurityStatistics{activeSessions=%d, totalUsers=%d, certificates=%d, policies=%d, mfaUsers=%d (%.1f%%), uptime=%ds}",
            activeSessions, totalUsers, totalCertificates, totalPolicies, 
            mfaEnabledUsers, getMfaAdoptionRate(), uptime
        );
    }
    
    public static class Builder {
        private int activeSessions = 0;
        private int totalUsers = 0;
        private int totalCertificates = 0;
        private int totalPolicies = 0;
        private int mfaEnabledUsers = 0;
        private long uptime = 0;
        
        public Builder activeSessions(int count) {
            this.activeSessions = count;
            return this;
        }
        
        public Builder totalUsers(int count) {
            this.totalUsers = count;
            return this;
        }
        
        public Builder totalCertificates(int count) {
            this.totalCertificates = count;
            return this;
        }
        
        public Builder totalPolicies(int count) {
            this.totalPolicies = count;
            return this;
        }
        
        public Builder mfaEnabledUsers(int count) {
            this.mfaEnabledUsers = count;
            return this;
        }
        
        public Builder uptime(long seconds) {
            this.uptime = seconds;
            return this;
        }
        
        public SecurityStatistics build() {
            return new SecurityStatistics(this);
        }
    }
}