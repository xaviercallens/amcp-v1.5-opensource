# Security Policy

## Supported Versions

We release security updates for the following versions of AMCP:

| Version | Supported          |
| ------- | ------------------ |
| 1.5.x   | :white_check_mark: |
| 1.4.x   | :white_check_mark: |
| 1.3.x   | :x:                |
| < 1.3   | :x:                |

## Reporting a Vulnerability

We take the security of AMCP seriously. If you believe you have found a security vulnerability, please report it to us as described below.

### How to Report

**Please do NOT report security vulnerabilities through public GitHub issues.**

Instead, please report them via email to:
- **Security Team:** security@amcp.io
- **Lead Maintainer:** callensxavier@gmail.com

Include the following information:
- Type of vulnerability
- Full paths of source file(s) related to the vulnerability
- Location of the affected code (tag/branch/commit or direct URL)
- Step-by-step instructions to reproduce the issue
- Proof-of-concept or exploit code (if possible)
- Impact of the vulnerability

### What to Expect

- **Acknowledgment:** We will acknowledge your email within 48 hours
- **Initial Assessment:** We will provide an initial assessment within 5 business days
- **Updates:** We will keep you informed about our progress
- **Resolution:** We aim to address critical vulnerabilities within 30 days
- **Credit:** With your permission, we will credit you in the security advisory

### Disclosure Policy

- We follow a coordinated disclosure timeline
- Security advisories will be published after fixes are released
- We will credit reporters (unless they prefer to remain anonymous)
- We may request a grace period before public disclosure

---

## Security Best Practices

### 1. Authentication and Authorization

#### Enable Authentication

```properties
# application.properties
amcp.security.enabled=true
amcp.security.oauth2.enabled=true
amcp.security.oauth2.issuer.uri=https://your-auth-server.com
```

#### Configure Authorization

```java
// SecurityConfig.java
@Bean
public SecurityManager securityManager() {
    return SecurityManager.builder()
        .enableTopicAuthorization(true)
        .addPolicy("weather.**", Role.WEATHER_AGENT)
        .addPolicy("admin.**", Role.ADMIN)
        .build();
}
```

### 2. Network Security

#### Enable TLS

```properties
# Enable TLS for agent context
amcp.security.tls.enabled=true
amcp.security.tls.keystore.path=/certs/keystore.jks
amcp.security.tls.keystore.password=${KEYSTORE_PASSWORD}

# Enable TLS for Kafka
amcp.kafka.security.protocol=SSL
amcp.kafka.ssl.truststore.location=/certs/truststore.jks
amcp.kafka.ssl.truststore.password=${TRUSTSTORE_PASSWORD}
```

#### Enable Mutual TLS (mTLS)

```properties
amcp.security.mtls.enabled=true
amcp.security.mtls.require.client.auth=true
amcp.security.tls.truststore.path=/certs/truststore.jks
```

### 3. Secrets Management

#### Use Environment Variables

```bash
# Never hardcode secrets!
export KAFKA_PASSWORD="secure-password"
export OAUTH_CLIENT_SECRET="client-secret"
export KEYSTORE_PASSWORD="keystore-password"
```

#### Use Kubernetes Secrets

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: amcp-secrets
  namespace: amcp
type: Opaque
stringData:
  kafka-password: ${KAFKA_PASSWORD}
  oauth-secret: ${OAUTH_CLIENT_SECRET}
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: amcp-context
spec:
  template:
    spec:
      containers:
      - name: amcp
        env:
        - name: KAFKA_PASSWORD
          valueFrom:
            secretKeyRef:
              name: amcp-secrets
              key: kafka-password
```

#### Use HashiCorp Vault

```java
// VaultConfig.java
@Configuration
public class VaultConfig {
    @Bean
    public VaultTemplate vaultTemplate() {
        return new VaultTemplate(vaultEndpoint(), 
            new TokenAuthentication(token));
    }
}
```

### 4. Input Validation

#### Validate Event Payloads

```java
public class SecureAgent extends AbstractMobileAgent {
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            // Validate payload before processing
            if (!validatePayload(event.getPayload())) {
                log.warn("Invalid payload rejected: {}", event.getId());
                return;
            }
            
            processEvent(event);
        });
    }
    
    private boolean validatePayload(Object payload) {
        // Implement validation logic
        if (payload == null) return false;
        
        // Check for injection attacks
        String str = payload.toString();
        if (containsMaliciousContent(str)) {
            return false;
        }
        
        return true;
    }
}
```

### 5. Agent Isolation

#### Topic-Based Isolation

```properties
# Restrict agents to specific topic patterns
amcp.security.topic.restrictions.enabled=true
amcp.security.topic.whitelist.weather-agent=weather.**
amcp.security.topic.whitelist.travel-agent=travel.**
```

#### Context Isolation

```java
// Create isolated contexts
AgentContext publicContext = AgentContext.builder()
    .contextId("public-context")
    .securityLevel(SecurityLevel.PUBLIC)
    .allowedTopics("public.**")
    .build();

AgentContext privateContext = AgentContext.builder()
    .contextId("private-context")
    .securityLevel(SecurityLevel.PRIVATE)
    .allowedTopics("internal.**", "private.**")
    .build();
```

### 6. Audit Logging

#### Enable Comprehensive Logging

```properties
# Enable audit logging
amcp.audit.enabled=true
amcp.audit.log.path=/var/log/amcp/audit.log
amcp.audit.log.level=INFO

# Log all security events
amcp.audit.events=AUTHENTICATION,AUTHORIZATION,MIGRATION,TOOL_CALL
```

#### Structured Audit Events

```java
public class AuditLogger {
    public void logSecurityEvent(SecurityEvent event) {
        Map<String, Object> auditLog = Map.of(
            "timestamp", Instant.now(),
            "eventType", event.getType(),
            "agentId", event.getAgentId(),
            "userId", event.getUserId(),
            "action", event.getAction(),
            "result", event.getResult(),
            "ipAddress", event.getIpAddress(),
            "correlationId", event.getCorrelationId()
        );
        
        logger.info("AUDIT: {}", toJson(auditLog));
    }
}
```

### 7. Rate Limiting

```java
@RateLimiter(
    requestsPerSecond = 100,
    burstCapacity = 200
)
public class RateLimitedAgent extends AbstractMobileAgent {
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        // Agent will automatically be rate limited
        return processEvent(event);
    }
}
```

### 8. Dependency Management

#### Regular Updates

```bash
# Check for vulnerable dependencies
mvn dependency:analyze
mvn org.owasp:dependency-check-maven:check

# Update dependencies
mvn versions:display-dependency-updates
```

#### Minimal Dependencies

Only include necessary dependencies to reduce attack surface:

```xml
<dependencies>
    <!-- Core dependencies only -->
    <dependency>
        <groupId>io.amcp</groupId>
        <artifactId>amcp-core</artifactId>
        <version>${amcp.version}</version>
    </dependency>
</dependencies>
```

---

## Security Checklist

### Before Deployment

- [ ] All secrets stored in secure vault (not in code/config)
- [ ] TLS enabled for all network communication
- [ ] Authentication enabled for all contexts
- [ ] Authorization policies configured
- [ ] Audit logging enabled
- [ ] Rate limiting configured
- [ ] Input validation implemented
- [ ] Dependencies scanned for vulnerabilities
- [ ] Security policies reviewed
- [ ] Incident response plan documented

### Production Environment

- [ ] mTLS enabled between all components
- [ ] Network segmentation in place
- [ ] Firewall rules configured
- [ ] Monitoring and alerting configured
- [ ] Regular security audits scheduled
- [ ] Backup and disaster recovery tested
- [ ] Access controls reviewed
- [ ] Log aggregation and SIEM integration
- [ ] Penetration testing completed
- [ ] Compliance requirements met

---

## Security Features by Component

### Core Framework

- ✅ Authentication context propagation
- ✅ Topic-level authorization
- ✅ Secure agent serialization
- ✅ Audit logging with correlation IDs
- ✅ TLS/mTLS support

### Event Broker

- ✅ Encrypted connections (TLS/SSL)
- ✅ SASL authentication
- ✅ ACL-based topic authorization
- ✅ Message encryption at rest
- ✅ Network isolation

### Agent Mobility

- ✅ Secure state transfer
- ✅ Authentication during migration
- ✅ Integrity verification
- ✅ Migration audit trails
- ✅ Rollback on failure

### Tool Connectors

- ✅ OAuth2 token management
- ✅ API key rotation
- ✅ Request signing
- ✅ Response validation
- ✅ Timeout handling

---

## Known Security Considerations

### 1. Agent Serialization

**Risk:** Malicious agent state could be injected during migration

**Mitigation:**
- Use allowlist for serializable classes
- Validate state integrity before migration
- Sign serialized agent state

### 2. Event Payload Injection

**Risk:** Malicious payloads could exploit vulnerabilities

**Mitigation:**
- Validate all event payloads
- Use schema validation
- Sanitize user input
- Implement content security policies

### 3. Broker Access

**Risk:** Unauthorized access to event broker

**Mitigation:**
- Enable broker authentication (SASL/mTLS)
- Configure ACLs for topic access
- Use network segmentation
- Monitor broker access logs

### 4. Denial of Service

**Risk:** Agent flooding with events

**Mitigation:**
- Implement rate limiting
- Configure event quotas
- Use circuit breakers
- Monitor resource usage

---

## Compliance

### GDPR

- ✅ Data encryption in transit and at rest
- ✅ Access control and audit logging
- ✅ Data retention policies configurable
- ✅ Right to deletion supported

### SOC 2

- ✅ Access controls implemented
- ✅ Change management procedures
- ✅ Monitoring and alerting
- ✅ Incident response capability

### HIPAA

- ✅ Encryption of PHI
- ✅ Access controls and audit logs
- ✅ Integrity controls
- ✅ Transmission security

---

## Security Resources

### Documentation

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [CWE Top 25](https://cwe.mitre.org/top25/)
- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)

### Tools

- [OWASP Dependency Check](https://owasp.org/www-project-dependency-check/)
- [Snyk](https://snyk.io/)
- [Trivy](https://github.com/aquasecurity/trivy)

### Contact

For security concerns:
- Email: security@amcp.io
- GitHub: Report privately via [Security Advisories](https://github.com/xaviercallens/amcp-v1.5-opensource/security/advisories)

---

**Last Updated:** October 2, 2025  
**Version:** 1.5.0
