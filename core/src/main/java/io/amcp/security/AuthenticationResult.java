package io.amcp.security;

/**
 * Authentication Result for AMCP v1.5 Enterprise Edition
 * 
 * Represents the result of an authentication attempt including:
 * - Success/failure status
 * - Security context (if successful)
 * - Error messages
 * - MFA requirements
 * - Additional metadata
 */
public class AuthenticationResult {
    
    private final boolean success;
    private final SecurityContext securityContext;
    private final String errorMessage;
    private final boolean mfaRequired;
    private final String mfaChallengeId;
    private final AuthenticationType type;
    
    private AuthenticationResult(boolean success, SecurityContext securityContext, 
                               String errorMessage, boolean mfaRequired, 
                               String mfaChallengeId, AuthenticationType type) {
        this.success = success;
        this.securityContext = securityContext;
        this.errorMessage = errorMessage;
        this.mfaRequired = mfaRequired;
        this.mfaChallengeId = mfaChallengeId;
        this.type = type;
    }
    
    public static AuthenticationResult success(SecurityContext context) {
        return new AuthenticationResult(true, context, null, false, null, AuthenticationType.SUCCESS);
    }
    
    public static AuthenticationResult failure(String errorMessage) {
        return new AuthenticationResult(false, null, errorMessage, false, null, AuthenticationType.FAILURE);
    }
    
    public static AuthenticationResult mfaRequired(String challengeId) {
        return new AuthenticationResult(false, null, null, true, challengeId, AuthenticationType.MFA_REQUIRED);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public SecurityContext getSecurityContext() {
        return securityContext;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public boolean isMfaRequired() {
        return mfaRequired;
    }
    
    public String getMfaChallengeId() {
        return mfaChallengeId;
    }
    
    public AuthenticationType getType() {
        return type;
    }
    
    @Override
    public String toString() {
        return String.format(
            "AuthenticationResult{success=%s, type=%s, mfaRequired=%s, errorMessage='%s'}",
            success, type, mfaRequired, errorMessage
        );
    }
    
    public enum AuthenticationType {
        SUCCESS,
        FAILURE,
        MFA_REQUIRED
    }
}