package io.amcp.security;

/**
 * Authorization Result for AMCP v1.5 Enterprise Edition
 * 
 * Represents the result of an authorization check including:
 * - Allow/deny decision
 * - Reason for decision
 * - Policy information
 * - Additional context
 */
public class AuthorizationResult {
    
    private final boolean allowed;
    private final String reason;
    private final String policyName;
    
    private AuthorizationResult(boolean allowed, String reason, String policyName) {
        this.allowed = allowed;
        this.reason = reason;
        this.policyName = policyName;
    }
    
    public static AuthorizationResult allowed() {
        return new AuthorizationResult(true, "Access granted", null);
    }
    
    public static AuthorizationResult allowed(String policyName) {
        return new AuthorizationResult(true, "Access granted by policy: " + policyName, policyName);
    }
    
    public static AuthorizationResult denied(String reason) {
        return new AuthorizationResult(false, reason, null);
    }
    
    public static AuthorizationResult denied(String reason, String policyName) {
        return new AuthorizationResult(false, reason, policyName);
    }
    
    public boolean isAllowed() {
        return allowed;
    }
    
    public String getReason() {
        return reason;
    }
    
    public String getPolicyName() {
        return policyName;
    }
    
    @Override
    public String toString() {
        return String.format(
            "AuthorizationResult{allowed=%s, reason='%s', policy='%s'}",
            allowed, reason, policyName
        );
    }
}