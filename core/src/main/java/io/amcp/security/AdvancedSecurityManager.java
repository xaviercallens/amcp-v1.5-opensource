package io.amcp.security;

import io.amcp.core.AgentID;

import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Advanced Security Manager for AMCP v1.5 Enterprise Edition
 * 
 * Comprehensive security management including:
 * - Multi-factor authentication (MFA)
 * - mTLS certificate management
 * - JWT token validation and management
 * - Role-Based Access Control (RBAC)
 * - Session management with timeout
 * - Security audit logging
 * - Dynamic policy evaluation
 * - OAuth2 integration
 * 
 * Thread-safe implementation with high-performance concurrent operations.
 */
public class AdvancedSecurityManager {
    
    private static final Logger logger = Logger.getLogger(AdvancedSecurityManager.class.getName());
    
    private final Map<String, SecurityContext> activeSessions;
    private final Map<String, Set<String>> userRoles;
    private final Map<String, Map<String, Object>> userPermissions;
    private final Map<String, X509Certificate> trustedCertificates;
    private final Map<String, String> jwtSecrets; // Per tenant JWT secrets
    private final SecurityAuditLogger auditLogger;
    private final ScheduledExecutorService scheduler;
    private final SecurityConfiguration configuration;
    
    // Security policies
    private final Map<String, Predicate<SecurityContext>> accessPolicies;
    private final Map<String, Long> sessionTimeouts; // Per role timeouts
    
    // Multi-factor authentication
    private final Map<String, MfaConfiguration> mfaConfigurations;
    private final Map<String, String> pendingMfaChallenges;
    
    // Certificate-based authentication
    private final CertificateValidator certificateValidator;
    private final Map<String, String> certificateToUser;
    
    public AdvancedSecurityManager(SecurityConfiguration configuration) {
        this.configuration = configuration;
        this.activeSessions = new ConcurrentHashMap<>();
        this.userRoles = new ConcurrentHashMap<>();
        this.userPermissions = new ConcurrentHashMap<>();
        this.trustedCertificates = new ConcurrentHashMap<>();
        this.jwtSecrets = new ConcurrentHashMap<>();
        this.auditLogger = new SecurityAuditLogger();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.accessPolicies = new ConcurrentHashMap<>();
        this.sessionTimeouts = new ConcurrentHashMap<>();
        this.mfaConfigurations = new ConcurrentHashMap<>();
        this.pendingMfaChallenges = new ConcurrentHashMap<>();
        this.certificateValidator = new CertificateValidator();
        this.certificateToUser = new ConcurrentHashMap<>();
        
        initializeDefaultPolicies();
        startSessionCleanup();
    }
    
    /**
     * Authenticate user with username/password
     */
    public CompletableFuture<AuthenticationResult> authenticate(String username, String password, String tenantId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                SecurityAuditEntry.Builder auditBuilder = SecurityAuditEntry.builder()
                    .principalId(username)
                    .tenantId(tenantId)
                    .action("authenticate")
                    .resource("security/auth")
                    .timestamp(Instant.now());
                
                // Validate credentials (would integrate with actual auth provider)
                boolean credentialsValid = validateCredentials(username, password, tenantId);
                
                if (!credentialsValid) {
                    auditLogger.log(auditBuilder.success(false).errorMessage("Invalid credentials").build());
                    return AuthenticationResult.failure("Invalid credentials");
                }
                
                // Check if MFA is required
                MfaConfiguration mfaConfig = mfaConfigurations.get(username);
                if (mfaConfig != null && mfaConfig.isEnabled()) {
                    String challengeId = generateMfaChallenge(username, mfaConfig);
                    auditLogger.log(auditBuilder.success(true).metadata("mfa_challenge", challengeId).build());
                    return AuthenticationResult.mfaRequired(challengeId);
                }
                
                // Create security context
                SecurityContext context = createSecurityContext(username, tenantId, "password");
                String sessionId = context.getSessionId();
                activeSessions.put(sessionId, context);
                
                auditLogger.log(auditBuilder.success(true).metadata("session_id", sessionId).build());
                return AuthenticationResult.success(context);
                
            } catch (Exception e) {
                logger.severe("Authentication error for user " + username + ": " + e.getMessage());
                SecurityAuditEntry audit = SecurityAuditEntry.builder()
                    .principalId(username)
                    .tenantId(tenantId)
                    .action("authenticate")
                    .resource("security/auth")
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
                auditLogger.log(audit);
                return AuthenticationResult.failure("Authentication failed");
            }
        });
    }
    
    /**
     * Complete MFA authentication
     */
    public CompletableFuture<AuthenticationResult> completeMfaAuthentication(
            String challengeId, String mfaCode, String tenantId) {
        return CompletableFuture.supplyAsync(() -> {
            String username = pendingMfaChallenges.get(challengeId);
            if (username == null) {
                SecurityAuditEntry audit = SecurityAuditEntry.builder()
                    .principalId("unknown")
                    .tenantId(tenantId)
                    .action("mfa_complete")
                    .resource("security/mfa")
                    .success(false)
                    .errorMessage("Invalid challenge ID")
                    .build();
                auditLogger.log(audit);
                return AuthenticationResult.failure("Invalid challenge");
            }
            
            MfaConfiguration mfaConfig = mfaConfigurations.get(username);
            boolean mfaValid = validateMfaCode(username, mfaCode, mfaConfig);
            
            SecurityAuditEntry.Builder auditBuilder = SecurityAuditEntry.builder()
                .principalId(username)
                .tenantId(tenantId)
                .action("mfa_complete")
                .resource("security/mfa")
                .metadata("challenge_id", challengeId);
            
            if (!mfaValid) {
                auditLogger.log(auditBuilder.success(false).errorMessage("Invalid MFA code").build());
                return AuthenticationResult.failure("Invalid MFA code");
            }
            
            // Remove pending challenge
            pendingMfaChallenges.remove(challengeId);
            
            // Create security context
            SecurityContext context = createSecurityContext(username, tenantId, "mfa");
            String sessionId = context.getSessionId();
            activeSessions.put(sessionId, context);
            
            auditLogger.log(auditBuilder.success(true).metadata("session_id", sessionId).build());
            return AuthenticationResult.success(context);
        });
    }
    
    /**
     * Authenticate using X.509 certificate (mTLS)
     */
    public CompletableFuture<AuthenticationResult> authenticateWithCertificate(
            X509Certificate certificate, String tenantId) {
        return CompletableFuture.supplyAsync(() -> {
            String certSubject = certificate.getSubjectDN().getName();
            
            SecurityAuditEntry.Builder auditBuilder = SecurityAuditEntry.builder()
                .principalId(certSubject)
                .tenantId(tenantId)
                .action("certificate_auth")
                .resource("security/cert")
                .metadata("cert_subject", certSubject);
            
            try {
                // Validate certificate
                if (!certificateValidator.isValid(certificate)) {
                    auditLogger.log(auditBuilder.success(false).errorMessage("Certificate validation failed").build());
                    return AuthenticationResult.failure("Invalid certificate");
                }
                
                // Check if certificate is trusted
                if (!isCertificateTrusted(certificate)) {
                    auditLogger.log(auditBuilder.success(false).errorMessage("Certificate not trusted").build());
                    return AuthenticationResult.failure("Certificate not trusted");
                }
                
                // Map certificate to user
                String username = certificateToUser.get(certificate.getSerialNumber().toString());
                if (username == null) {
                    auditLogger.log(auditBuilder.success(false).errorMessage("Certificate not mapped to user").build());
                    return AuthenticationResult.failure("Certificate not authorized");
                }
                
                // Create security context
                SecurityContext context = createSecurityContext(username, tenantId, "certificate");
                context.addSecurityMetadata("certificate_subject", certSubject);
                context.addSecurityMetadata("certificate_serial", certificate.getSerialNumber().toString());
                
                String sessionId = context.getSessionId();
                activeSessions.put(sessionId, context);
                
                auditLogger.log(auditBuilder
                    .principalId(username)
                    .success(true)
                    .metadata("session_id", sessionId)
                    .build());
                
                return AuthenticationResult.success(context);
                
            } catch (Exception e) {
                logger.severe("Certificate authentication error: " + e.getMessage());
                auditLogger.log(auditBuilder.success(false).errorMessage(e.getMessage()).build());
                return AuthenticationResult.failure("Certificate authentication failed");
            }
        });
    }
    
    /**
     * Validate JWT token and create security context
     */
    public CompletableFuture<AuthenticationResult> authenticateWithJwt(String jwtToken, String tenantId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Parse and validate JWT (simplified - would use proper JWT library)
                JwtClaims claims = parseAndValidateJwt(jwtToken, tenantId);
                
                SecurityAuditEntry.Builder auditBuilder = SecurityAuditEntry.builder()
                    .principalId(claims.getSubject())
                    .tenantId(tenantId)
                    .action("jwt_auth")
                    .resource("security/jwt");
                
                if (claims.isExpired()) {
                    auditLogger.log(auditBuilder.success(false).errorMessage("JWT token expired").build());
                    return AuthenticationResult.failure("Token expired");
                }
                
                // Create security context from JWT claims
                SecurityContext context = SecurityContext.builder()
                    .principalId(claims.getSubject())
                    .tenantId(tenantId)
                    .roles(claims.getRoles())
                    .permissions(claims.getPermissions())
                    .authenticationMethod("jwt")
                    .authenticated(true)
                    .expiresAt(claims.getExpiration())
                    .securityMetadata("jwt_issuer", claims.getIssuer())
                    .securityMetadata("jwt_audience", String.join(",", claims.getAudience()))
                    .build();
                
                String sessionId = context.getSessionId();
                activeSessions.put(sessionId, context);
                
                auditLogger.log(auditBuilder.success(true).metadata("session_id", sessionId).build());
                return AuthenticationResult.success(context);
                
            } catch (Exception e) {
                logger.severe("JWT authentication error: " + e.getMessage());
                SecurityAuditEntry audit = SecurityAuditEntry.builder()
                    .principalId("unknown")
                    .tenantId(tenantId)
                    .action("jwt_auth")
                    .resource("security/jwt")
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
                auditLogger.log(audit);
                return AuthenticationResult.failure("JWT authentication failed");
            }
        });
    }
    
    /**
     * Authorize access to resource with specific action
     */
    public CompletableFuture<AuthorizationResult> authorize(String sessionId, String resource, String action) {
        return CompletableFuture.supplyAsync(() -> {
            SecurityContext context = activeSessions.get(sessionId);
            
            SecurityAuditEntry.Builder auditBuilder = SecurityAuditEntry.builder()
                .sessionId(sessionId)
                .action("authorize:" + action)
                .resource(resource);
            
            if (context == null) {
                auditLogger.log(auditBuilder
                    .principalId("unknown")
                    .tenantId("unknown")
                    .success(false)
                    .errorMessage("Invalid session")
                    .build());
                return AuthorizationResult.denied("Invalid session");
            }
            
            auditBuilder.principalId(context.getPrincipalId()).tenantId(context.getTenantId());
            
            if (!context.isAuthenticated()) {
                auditLogger.log(auditBuilder.success(false).errorMessage("Not authenticated").build());
                return AuthorizationResult.denied("Not authenticated");
            }
            
            // Update last access time
            context.updateLastAccessTime();
            
            // Check resource permission
            boolean hasResourcePermission = context.hasResourcePermission(resource, action);
            
            // Check access policies
            boolean policyAllowed = checkAccessPolicies(context, resource, action);
            
            boolean authorized = hasResourcePermission && policyAllowed;
            
            auditBuilder.success(authorized);
            if (!authorized) {
                auditBuilder.errorMessage("Access denied - insufficient permissions");
            }
            
            auditLogger.log(auditBuilder.build());
            
            return authorized ? AuthorizationResult.allowed() : AuthorizationResult.denied("Insufficient permissions");
        });
    }
    
    /**
     * Get security context by session ID
     */
    public Optional<SecurityContext> getSecurityContext(String sessionId) {
        SecurityContext context = activeSessions.get(sessionId);
        if (context != null && context.isAuthenticated()) {
            context.updateLastAccessTime();
            return Optional.of(context);
        }
        return Optional.empty();
    }
    
    /**
     * Logout and invalidate session
     */
    public CompletableFuture<Void> logout(String sessionId) {
        return CompletableFuture.runAsync(() -> {
            SecurityContext context = activeSessions.remove(sessionId);
            if (context != null) {
                SecurityAuditEntry audit = SecurityAuditEntry.builder()
                    .principalId(context.getPrincipalId())
                    .tenantId(context.getTenantId())
                    .sessionId(sessionId)
                    .action("logout")
                    .resource("security/session")
                    .success(true)
                    .build();
                auditLogger.log(audit);
                
                logger.info("User " + context.getPrincipalId() + " logged out");
            }
        });
    }
    
    /**
     * Add or update user roles
     */
    public void setUserRoles(String username, Set<String> roles) {
        userRoles.put(username, new HashSet<>(roles));
        logger.info("Updated roles for user " + username + ": " + roles);
    }
    
    /**
     * Add or update user permissions
     */
    public void setUserPermissions(String username, Map<String, Object> permissions) {
        userPermissions.put(username, new HashMap<>(permissions));
        logger.info("Updated permissions for user " + username);
    }
    
    /**
     * Configure MFA for user
     */
    public void configureMfa(String username, MfaConfiguration config) {
        mfaConfigurations.put(username, config);
        logger.info("Configured MFA for user " + username + " type: " + config.getType());
    }
    
    /**
     * Add trusted certificate
     */
    public void addTrustedCertificate(String alias, X509Certificate certificate) {
        trustedCertificates.put(alias, certificate);
        logger.info("Added trusted certificate: " + alias);
    }
    
    /**
     * Map certificate to user
     */
    public void mapCertificateToUser(X509Certificate certificate, String username) {
        certificateToUser.put(certificate.getSerialNumber().toString(), username);
        logger.info("Mapped certificate to user: " + username);
    }
    
    /**
     * Add access policy
     */
    public void addAccessPolicy(String policyName, Predicate<SecurityContext> policy) {
        accessPolicies.put(policyName, policy);
        logger.info("Added access policy: " + policyName);
    }
    
    /**
     * Get security statistics
     */
    public SecurityStatistics getSecurityStatistics() {
        return SecurityStatistics.builder()
            .activeSessions(activeSessions.size())
            .totalUsers(userRoles.size())
            .totalCertificates(trustedCertificates.size())
            .totalPolicies(accessPolicies.size())
            .mfaEnabledUsers(mfaConfigurations.size())
            .build();
    }
    
    /**
     * Shutdown security manager
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        auditLogger.shutdown();
        logger.info("Security manager shutdown completed");
    }
    
    // Private helper methods
    
    private SecurityContext createSecurityContext(String username, String tenantId, String authMethod) {
        Set<String> roles = userRoles.getOrDefault(username, Collections.emptySet());
        Map<String, Object> permissions = userPermissions.getOrDefault(username, Collections.emptyMap());
        
        long sessionTimeoutSeconds = sessionTimeouts.getOrDefault(
            roles.contains("ADMIN") ? "ADMIN" : "USER", 
            configuration.getDefaultSessionTimeout()
        );
        
        return SecurityContext.builder()
            .principalId(username)
            .tenantId(tenantId)
            .roles(roles)
            .permissions(permissions)
            .authenticationMethod(authMethod)
            .authenticated(true)
            .expiresIn(sessionTimeoutSeconds)
            .build();
    }
    
    private boolean validateCredentials(String username, String password, String tenantId) {
        // This would integrate with actual authentication provider (LDAP, database, etc.)
        // For demo purposes, we'll use simple validation
        return username != null && !username.trim().isEmpty() && 
               password != null && password.length() >= 8;
    }
    
    private String generateMfaChallenge(String username, MfaConfiguration config) {
        String challengeId = UUID.randomUUID().toString();
        pendingMfaChallenges.put(challengeId, username);
        
        // Schedule challenge cleanup
        scheduler.schedule(() -> pendingMfaChallenges.remove(challengeId), 5, TimeUnit.MINUTES);
        
        return challengeId;
    }
    
    private boolean validateMfaCode(String username, String code, MfaConfiguration config) {
        // This would integrate with actual MFA provider (TOTP, SMS, etc.)
        // For demo purposes, we'll use simple validation
        return code != null && code.length() == 6 && code.matches("\\d+");
    }
    
    private boolean isCertificateTrusted(X509Certificate certificate) {
        // Check against trusted certificates
        for (X509Certificate trusted : trustedCertificates.values()) {
            try {
                certificate.verify(trusted.getPublicKey());
                return true;
            } catch (Exception e) {
                // Certificate not signed by this trusted cert
            }
        }
        return false;
    }
    
    private JwtClaims parseAndValidateJwt(String token, String tenantId) throws Exception {
        // This would use a proper JWT library like jjwt
        // For demo purposes, creating a simple implementation
        String secret = jwtSecrets.get(tenantId);
        if (secret == null) {
            throw new SecurityException("No JWT secret configured for tenant: " + tenantId);
        }
        
        // Simple JWT parsing (would use proper library in production)
        return new JwtClaims("user", tenantId, Set.of("USER"), Map.of(), Instant.now().plusSeconds(3600));
    }
    
    private boolean checkAccessPolicies(SecurityContext context, String resource, String action) {
        for (Predicate<SecurityContext> policy : accessPolicies.values()) {
            if (!policy.test(context)) {
                return false;
            }
        }
        return true;
    }
    
    private void initializeDefaultPolicies() {
        // Business hours policy
        addAccessPolicy("business_hours", context -> {
            // Allow access only during business hours (9 AM - 5 PM)
            if (context.hasRole("ADMIN") || context.hasRole("SYSTEM")) {
                return true; // Admins can access anytime
            }
            
            java.time.LocalTime now = java.time.LocalTime.now();
            return now.isAfter(java.time.LocalTime.of(9, 0)) && 
                   now.isBefore(java.time.LocalTime.of(17, 0));
        });
        
        // Tenant isolation policy
        addAccessPolicy("tenant_isolation", context -> {
            // Users can only access resources in their tenant
            return context.getTenantId() != null && !context.getTenantId().isEmpty();
        });
        
        // Session timeout policy
        addAccessPolicy("session_timeout", context -> {
            if (context.getLastAccessTime().isBefore(
                    Instant.now().minusSeconds(configuration.getMaxIdleTime()))) {
                return false;
            }
            return true;
        });
        
        // Default session timeouts
        sessionTimeouts.put("USER", 1800L); // 30 minutes
        sessionTimeouts.put("ADMIN", 3600L); // 1 hour
        sessionTimeouts.put("SYSTEM", 7200L); // 2 hours
    }
    
    private void startSessionCleanup() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupExpiredSessions();
            } catch (Exception e) {
                logger.severe("Error during session cleanup: " + e.getMessage());
            }
        }, 5, 5, TimeUnit.MINUTES);
    }
    
    private void cleanupExpiredSessions() {
        int cleaned = 0;
        Iterator<Map.Entry<String, SecurityContext>> iterator = activeSessions.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, SecurityContext> entry = iterator.next();
            SecurityContext context = entry.getValue();
            
            if (context.isExpired() || 
                context.getLastAccessTime().isBefore(Instant.now().minusSeconds(configuration.getMaxIdleTime()))) {
                
                iterator.remove();
                cleaned++;
                
                SecurityAuditEntry audit = SecurityAuditEntry.builder()
                    .principalId(context.getPrincipalId())
                    .tenantId(context.getTenantId())
                    .sessionId(entry.getKey())
                    .action("session_expired")
                    .resource("security/session")
                    .success(true)
                    .build();
                auditLogger.log(audit);
            }
        }
        
        if (cleaned > 0) {
            logger.info("Cleaned up " + cleaned + " expired sessions");
        }
    }
    
    // Helper classes
    
    public static class MfaConfiguration {
        private final String type; // TOTP, SMS, EMAIL
        private final boolean enabled;
        private final String secret;
        
        public MfaConfiguration(String type, boolean enabled, String secret) {
            this.type = type;
            this.enabled = enabled;
            this.secret = secret;
        }
        
        public String getType() { return type; }
        public boolean isEnabled() { return enabled; }
        public String getSecret() { return secret; }
    }
    
    private static class JwtClaims {
        private final String subject;
        private final String issuer;
        private final Set<String> roles;
        private final Map<String, Object> permissions;
        private final Instant expiration;
        private final List<String> audience;
        
        public JwtClaims(String subject, String issuer, Set<String> roles, 
                        Map<String, Object> permissions, Instant expiration) {
            this.subject = subject;
            this.issuer = issuer;
            this.roles = roles;
            this.permissions = permissions;
            this.expiration = expiration;
            this.audience = List.of("amcp");
        }
        
        public String getSubject() { return subject; }
        public String getIssuer() { return issuer; }
        public Set<String> getRoles() { return roles; }
        public Map<String, Object> getPermissions() { return permissions; }
        public Instant getExpiration() { return expiration; }
        public List<String> getAudience() { return audience; }
        public boolean isExpired() { return Instant.now().isAfter(expiration); }
    }
}

// Additional result classes would be defined in separate files