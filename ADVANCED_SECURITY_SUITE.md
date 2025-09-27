# AMCP v1.5 Enterprise Edition - Advanced Security Suite

## 🔒 Overview

The **Advanced Security Suite** provides enterprise-grade security for AMCP v1.5 Enterprise Edition, delivering comprehensive authentication, authorization, and audit capabilities with industry-standard protocols and best practices.

## ✨ Enterprise Security Features

### 🔐 Multi-Factor Authentication (MFA)
- **TOTP Support** - Time-based One-Time Password authentication
- **SMS Integration** - SMS-based verification codes  
- **Email Tokens** - Email-delivered authentication tokens
- **Hardware Keys** - FIDO2/WebAuthn compatible security keys
- **Backup Codes** - Recovery codes for account access
- **Configurable Policies** - Flexible MFA enforcement rules

### 📋 Certificate-Based Authentication (mTLS)
- **X.509 Certificate Validation** - Complete certificate chain verification
- **Certificate Revocation** - CRL and OCSP support for revocation checking
- **Custom CA Support** - Private Certificate Authority integration
- **Key Usage Validation** - Proper key usage and extended key usage checking
- **Certificate Mapping** - Dynamic certificate-to-user mapping
- **Smart Card Integration** - PKI smart card authentication support

### 🎫 JWT Token Management
- **Standards Compliance** - Full JWT/JWS/JWE specification support
- **Per-Tenant Signing** - Tenant-specific JWT signing keys
- **Claims Processing** - Rich claims extraction and validation
- **Token Refresh** - Automatic token refresh and rotation
- **OAuth2/OIDC Integration** - Industry-standard protocol support
- **Audience Validation** - Multi-audience token validation

### 🛡️ Role-Based Access Control (RBAC)
- **Fine-Grained Permissions** - Resource and action-specific permissions
- **Role Hierarchies** - Inherited permissions from parent roles
- **Dynamic Policies** - Runtime policy evaluation and enforcement
- **Tenant Isolation** - Multi-tenant security boundaries
- **Resource-Based Access** - Context-aware authorization decisions
- **Policy Templates** - Pre-defined security policy templates

### 📊 Security Audit & Compliance
- **Comprehensive Logging** - All security events logged with context
- **Structured Formats** - JSON and text-based log formats
- **Real-Time Monitoring** - Live security event streaming
- **Compliance Reports** - Automated compliance reporting
- **SIEM Integration** - Security Information and Event Management ready
- **Threat Detection** - Anomaly detection and alerting

### ⏰ Advanced Session Management
- **Session Timeouts** - Configurable session and idle timeouts
- **Concurrent Sessions** - Multi-session management and limits
- **Session Persistence** - Distributed session storage support
- **Grace Periods** - Configurable session extension policies
- **Device Tracking** - Device fingerprinting and recognition
- **Geographic Restrictions** - Location-based access controls

## 🏗️ Security Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                Advanced Security Suite                          │
├─────────────────────────────────────────────────────────────────┤
│  Authentication    │  Authorization   │  Audit        │  Session │
│  - Password/MFA    │  - RBAC          │  - Logging    │  - Mgmt   │
│  - Certificate    │  - Policies      │  - Monitoring │  - Timeout│
│  - JWT/OAuth2     │  - Permissions   │  - Compliance │  - Cleanup│
├─────────────────────────────────────────────────────────────────┤
│               Security Context & Policy Engine                  │
│  - Principal Management    │  - Dynamic Policy Evaluation       │
│  - Role Resolution        │  - Resource-based Authorization     │
│  - Permission Calculation │  - Multi-tenant Isolation          │
├─────────────────────────────────────────────────────────────────┤
│                    Security Infrastructure                      │
│  Certificate Store │  JWT Key Store   │  Audit Queue  │  Metrics │
│  - Trusted CAs     │  - Signing Keys  │  - Async Log  │  - Stats  │
│  - CRL/OCSP       │  - Validation    │  - Structured │  - Health │
└─────────────────────────────────────────────────────────────────┘
```

## 📚 API Documentation

### Basic Security Setup

```java
// Create security configuration
SecurityConfiguration config = SecurityConfiguration.builder()
    .defaultSessionTimeout(3600) // 1 hour
    .maxIdleTime(900) // 15 minutes
    .requireMfa(true)
    .auditEnabled(true)
    .certificateAuthEnabled(true)
    .jwtExpiration(1800) // 30 minutes
    .build();

// Initialize Advanced Security Manager
AdvancedSecurityManager securityManager = new AdvancedSecurityManager(config);

// Setup user roles and permissions
securityManager.setUserRoles("john.doe", Set.of("USER", "DEVELOPER"));
securityManager.setUserPermissions("john.doe", Map.of(
    "event:publish", true,
    "event:subscribe", true,
    "agent:create", true,
    "agent:read", true,
    "project:deploy", false
));
```

### Password Authentication with MFA

```java
// Step 1: Initial authentication
CompletableFuture<AuthenticationResult> authFuture = 
    securityManager.authenticate("admin", "SecurePassword123", "tenant-1");

AuthenticationResult result = authFuture.get();

if (result.isMfaRequired()) {
    // Step 2: Complete MFA challenge
    String challengeId = result.getMfaChallengeId();
    CompletableFuture<AuthenticationResult> mfaFuture = 
        securityManager.completeMfaAuthentication(challengeId, "123456", "tenant-1");
    
    AuthenticationResult mfaResult = mfaFuture.get();
    if (mfaResult.isSuccess()) {
        SecurityContext context = mfaResult.getSecurityContext();
        System.out.println("Authenticated: " + context.getPrincipalId());
    }
}
```

### Certificate-Based Authentication (mTLS)

```java
// Load client certificate
X509Certificate clientCertificate = loadCertificateFromStore();

// Authenticate with certificate
CompletableFuture<AuthenticationResult> certAuthFuture = 
    securityManager.authenticateWithCertificate(clientCertificate, "tenant-1");

AuthenticationResult result = certAuthFuture.get();
if (result.isSuccess()) {
    SecurityContext context = result.getSecurityContext();
    System.out.println("Certificate authentication successful");
    System.out.println("Subject: " + context.getSecurityMetadata("certificate_subject"));
    System.out.println("Serial: " + context.getSecurityMetadata("certificate_serial"));
}

// Configure certificate validation
CertificateValidator validator = new CertificateValidator(
    Set.of("digitalSignature", "keyEncipherment", "clientAuth"), // allowed key usage
    Set.of("2.5.29.37"), // required extended key usage OID
    true, // check revocation
    300 // clock skew tolerance (5 minutes)
);
```

### JWT Token Authentication

```java
// Configure JWT authentication
securityManager.setJwtSecret("tenant-1", "your-secret-key-here");

// Authenticate with JWT token
String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
CompletableFuture<AuthenticationResult> jwtAuthFuture = 
    securityManager.authenticateWithJwt(jwtToken, "tenant-1");

AuthenticationResult result = jwtAuthFuture.get();
if (result.isSuccess()) {
    SecurityContext context = result.getSecurityContext();
    System.out.println("JWT authentication successful");
    System.out.println("Issuer: " + context.getSecurityMetadata("jwt_issuer"));
    System.out.println("Audience: " + context.getSecurityMetadata("jwt_audience"));
}
```

### Authorization and Access Control

```java
// Get security context
Optional<SecurityContext> contextOpt = securityManager.getSecurityContext(sessionId);
if (contextOpt.isPresent()) {
    SecurityContext context = contextOpt.get();
    
    // Check specific permissions
    boolean canPublish = context.hasPermission("event:publish");
    boolean canAdmin = context.hasResourcePermission("system", "admin");
    
    // Check roles
    boolean isAdmin = context.hasRole("ADMIN");
    boolean hasAnyRole = context.hasAnyRole("USER", "DEVELOPER", "ADMIN");
    
    // Authorize specific actions
    CompletableFuture<AuthorizationResult> authzFuture = 
        securityManager.authorize(sessionId, "project", "deploy");
    
    AuthorizationResult authzResult = authzFuture.get();
    if (authzResult.isAllowed()) {
        System.out.println("Authorization granted");
    } else {
        System.out.println("Access denied: " + authzResult.getReason());
    }
}
```

### Security Audit Logging

```java
// Create audit entry
SecurityAuditEntry auditEntry = SecurityAuditEntry.builder()
    .principalId("john.doe")
    .tenantId("tenant-1")
    .sessionId("session-123")
    .action("data_access")
    .resource("sensitive/customer_data")
    .success(true)
    .ipAddress("192.168.1.100")
    .userAgent("AMCP-Client/1.5")
    .duration(250) // milliseconds
    .metadata("record_count", "150")
    .build();

// Log the audit entry
SecurityAuditLogger auditLogger = new SecurityAuditLogger();
auditLogger.log(auditEntry);

// Get structured log formats
String logEntry = auditEntry.toLogEntry(); // Human-readable
String jsonEntry = auditEntry.toJson(); // Machine-readable

// Check audit entry properties
AuditSeverity severity = auditEntry.getSeverity();
boolean isViolation = auditEntry.isSecurityViolation();
boolean isHighRisk = auditEntry.isHighRiskOperation();
```

### Dynamic Security Policies

```java
// Add custom security policy
securityManager.addAccessPolicy("business_hours", context -> {
    LocalTime now = LocalTime.now();
    boolean businessHours = now.isAfter(LocalTime.of(9, 0)) && 
                           now.isBefore(LocalTime.of(17, 0));
    
    // Admins can access anytime
    if (context.hasRole("ADMIN")) {
        return true;
    }
    
    return businessHours;
});

// Add geographic restriction policy
securityManager.addAccessPolicy("geographic_restriction", context -> {
    String userLocation = context.getAttribute("location");
    Set<String> allowedRegions = Set.of("US", "EU", "CANADA");
    
    return userLocation != null && allowedRegions.contains(userLocation);
});

// Add session security policy
securityManager.addAccessPolicy("session_security", context -> {
    // Require recent authentication for sensitive operations
    long lastAuthTime = Long.parseLong(
        context.getAttribute("last_auth_time", "0"));
    long currentTime = System.currentTimeMillis() / 1000;
    
    return (currentTime - lastAuthTime) < 1800; // 30 minutes
});
```

### Session Management

```java
// Configure session timeouts per role
securityManager.setSessionTimeout("USER", 1800L); // 30 minutes
securityManager.setSessionTimeout("ADMIN", 3600L); // 1 hour
securityManager.setSessionTimeout("SERVICE", 7200L); // 2 hours

// Manage sessions
SecurityContext context = securityManager.getSecurityContext(sessionId).orElse(null);
if (context != null) {
    // Check session validity
    boolean isValid = context.isAuthenticated() && !context.isExpired();
    
    // Update last access time
    context.updateLastAccessTime();
    
    // Check session metadata
    Instant created = context.getCreatedAt();
    Instant expires = context.getExpiresAt();
    Instant lastAccess = context.getLastAccessTime();
    
    System.out.println("Session created: " + created);
    System.out.println("Session expires: " + expires);
    System.out.println("Last access: " + lastAccess);
}

// Logout and cleanup
securityManager.logout(sessionId).thenRun(() -> {
    System.out.println("Session terminated and cleaned up");
});
```

## 🔧 Configuration Guide

### Security Configuration

```java
SecurityConfiguration config = SecurityConfiguration.builder()
    // Session settings
    .defaultSessionTimeout(3600) // 1 hour default
    .maxIdleTime(1800) // 30 minutes max idle
    .maxConcurrentSessions(5) // Max 5 concurrent sessions per user
    
    // Authentication settings
    .requireMfa(true) // Require MFA for all users
    .certificateAuthEnabled(true) // Enable certificate authentication
    .passwordMinLength(12) // Minimum password length
    .passwordComplexityRequired(true) // Require complex passwords
    
    // Security policies
    .maxFailedAttempts(3) // Max failed login attempts
    .lockoutDuration(900) // 15 minute lockout
    
    // JWT settings
    .jwtIssuer("amcp-security") // JWT issuer
    .jwtExpiration(1800) // 30 minute JWT expiration
    
    // Audit settings
    .auditEnabled(true) // Enable comprehensive auditing
    .build();
```

### MFA Configuration

```java
// Configure TOTP MFA for user
AdvancedSecurityManager.MfaConfiguration totpConfig = 
    new AdvancedSecurityManager.MfaConfiguration("TOTP", true, generateTotpSecret());
securityManager.configureMfa("admin", totpConfig);

// Configure SMS MFA for user  
AdvancedSecurityManager.MfaConfiguration smsConfig = 
    new AdvancedSecurityManager.MfaConfiguration("SMS", true, "+1234567890");
securityManager.configureMfa("user", smsConfig);
```

### Certificate Configuration

```java
// Add trusted certificate authority
X509Certificate rootCA = loadRootCACertificate();
securityManager.addTrustedCertificate("RootCA", rootCA);

// Map certificate to user
X509Certificate userCert = loadUserCertificate();
securityManager.mapCertificateToUser(userCert, "john.doe");

// Configure certificate validator
CertificateValidator validator = new CertificateValidator(
    Set.of("digitalSignature", "keyEncipherment"), // Required key usages
    Set.of("2.5.29.37"), // Required extensions (Extended Key Usage)
    true, // Enable revocation checking
    300 // Clock skew tolerance (5 minutes)
);
```

### Application Properties

```properties
# AMCP Advanced Security Configuration
amcp.security.session.timeout=3600
amcp.security.session.max-idle=1800
amcp.security.session.max-concurrent=5

# Authentication
amcp.security.auth.mfa.required=true
amcp.security.auth.certificate.enabled=true
amcp.security.auth.password.min-length=12
amcp.security.auth.password.complexity=true

# JWT Configuration
amcp.security.jwt.issuer=amcp-security
amcp.security.jwt.expiration=1800
amcp.security.jwt.algorithm=HS256

# Audit Configuration
amcp.security.audit.enabled=true
amcp.security.audit.structured-logging=true
amcp.security.audit.queue-size=10000

# Certificate Settings
amcp.security.certificate.validation.crl-check=true
amcp.security.certificate.validation.ocsp-check=true
amcp.security.certificate.clock-skew-tolerance=300
```

## 🔒 Security Best Practices

### Authentication Best Practices

1. **Strong Password Policies**
   - Minimum 12 characters
   - Require uppercase, lowercase, numbers, and special characters
   - Prevent password reuse
   - Regular password rotation

2. **Multi-Factor Authentication**
   - Enable MFA for all administrative accounts
   - Use TOTP applications (Google Authenticator, Authy)
   - Provide backup recovery codes
   - Consider hardware security keys for high-privilege accounts

3. **Certificate Management**
   - Use strong key sizes (RSA 2048+ or ECC P-256+)
   - Regular certificate rotation
   - Proper certificate chain validation
   - Monitor certificate expiration dates

### Authorization Best Practices

1. **Principle of Least Privilege**
   - Grant minimum required permissions
   - Regular permission audits
   - Time-bounded elevated privileges
   - Separation of duties

2. **Role Management**
   - Define clear role hierarchies
   - Avoid overly broad roles
   - Regular role-to-user mapping reviews
   - Document role responsibilities

### Session Management Best Practices

1. **Session Security**
   - Secure session token generation
   - Session fixation protection
   - Proper session invalidation
   - Monitor concurrent sessions

2. **Timeout Configuration**
   - Short timeouts for sensitive operations
   - Idle timeout enforcement
   - Grace period for user activity
   - Automatic cleanup of expired sessions

### Audit and Monitoring Best Practices

1. **Comprehensive Logging**
   - Log all authentication attempts
   - Record authorization decisions
   - Track administrative actions
   - Monitor failed access attempts

2. **Real-time Monitoring**
   - Set up security alerts
   - Monitor for suspicious patterns
   - Automated threat detection
   - Integration with SIEM systems

## 📊 Security Metrics and Monitoring

### Key Security Metrics

```java
SecurityStatistics stats = securityManager.getSecurityStatistics();

System.out.println("Security Health Dashboard:");
System.out.println("========================");
System.out.println("Active Sessions: " + stats.getActiveSessions());
System.out.println("Total Users: " + stats.getTotalUsers());
System.out.println("MFA Adoption: " + stats.getMfaAdoptionRate() + "%");
System.out.println("Certificate Count: " + stats.getTotalCertificates());
System.out.println("Active Policies: " + stats.getTotalPolicies());
```

### Health Checks

```java
// Security manager health check
boolean securityHealthy = securityManager.isHealthy();
boolean auditHealthy = auditLogger.isHealthy();

// Detailed health information
Map<String, Object> healthInfo = Map.of(
    "security_manager", securityHealthy,
    "audit_logger", auditHealthy,
    "active_sessions", stats.getActiveSessions(),
    "queue_size", auditLogger.getQueueSize()
);
```

### Alerting Rules

1. **High Priority Alerts**
   - Multiple failed authentication attempts
   - Privilege escalation attempts
   - Unusual access patterns
   - Certificate validation failures

2. **Medium Priority Alerts**
   - High number of concurrent sessions
   - Long-running sessions
   - Geographic anomalies
   - Policy violations

3. **Low Priority Alerts**
   - Session timeout warnings
   - Certificate expiration notices
   - Configuration changes
   - Performance degradation

## 🧪 Testing and Validation

### Security Testing

```java
@Test
public void testPasswordAuthentication() {
    AdvancedSecurityManager security = new AdvancedSecurityManager(testConfig());
    
    // Test successful authentication
    AuthenticationResult result = security.authenticate(
        "testuser", "SecurePass123", "tenant-1").get();
    assertTrue(result.isSuccess());
    
    // Test failed authentication
    AuthenticationResult failResult = security.authenticate(
        "testuser", "WrongPassword", "tenant-1").get();
    assertFalse(failResult.isSuccess());
}

@Test
public void testRbacAuthorization() {
    // Setup test context
    SecurityContext context = SecurityContext.builder()
        .principalId("testuser")
        .tenantId("tenant-1")
        .roles("USER")
        .permission("event:publish", true)
        .permission("admin:access", false)
        .build();
    
    // Test permissions
    assertTrue(context.hasPermission("event:publish"));
    assertFalse(context.hasPermission("admin:access"));
}
```

### Integration Testing

- **Authentication Providers** - Test integration with LDAP, Active Directory
- **Certificate Authorities** - Validate certificate chain processing
- **JWT Providers** - Test OAuth2/OIDC integration
- **Audit Systems** - Verify SIEM integration
- **Session Stores** - Test distributed session management

### Performance Testing

- **Authentication Throughput** - 1000+ authentications/second
- **Authorization Latency** - <5ms authorization decisions
- **Session Scaling** - 10,000+ concurrent sessions
- **Audit Performance** - 5000+ audit entries/second
- **Memory Usage** - <500MB for 10,000 active sessions

## 🚨 Security Incident Response

### Incident Types

1. **Authentication Failures**
   - Brute force attacks
   - Credential stuffing
   - Account lockouts

2. **Authorization Violations**
   - Privilege escalation attempts
   - Unauthorized access
   - Policy bypasses

3. **Session Attacks**
   - Session hijacking
   - Session fixation
   - Concurrent session abuse

### Response Procedures

1. **Immediate Response**
   - Lock compromised accounts
   - Invalidate suspicious sessions
   - Enable additional logging
   - Alert security team

2. **Investigation**
   - Analyze audit logs
   - Identify attack patterns
   - Assess impact scope
   - Collect evidence

3. **Recovery**
   - Reset compromised credentials
   - Update security policies
   - Patch vulnerabilities
   - Restore normal operations

## 📋 Compliance and Standards

### Supported Standards

- **NIST Cybersecurity Framework** - Complete implementation
- **ISO 27001** - Information security management
- **SOC 2 Type II** - Security and availability controls
- **GDPR** - Data protection and privacy compliance
- **HIPAA** - Healthcare information security (when applicable)
- **PCI DSS** - Payment card industry standards (when applicable)

### Audit Reports

```java
// Generate compliance report
ComplianceReport report = securityManager.generateComplianceReport();

System.out.println("Compliance Status:");
System.out.println("=================");
report.getStandards().forEach((standard, status) -> {
    System.out.println(standard + ": " + status);
});

// Export audit trail
List<SecurityAuditEntry> auditTrail = securityManager.getAuditTrail(
    Instant.now().minusDays(30), // Last 30 days
    Instant.now()
);
```

---

*The Advanced Security Suite provides enterprise-grade security while maintaining the flexibility and performance characteristics of the AMCP framework.*