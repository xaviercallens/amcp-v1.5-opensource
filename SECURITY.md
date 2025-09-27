# Security Policy

## Supported Versions

We actively support the following versions of AMCP with security updates:

| Version | Supported          |
| ------- | ------------------ |
| 1.5.x   | :white_check_mark: |
| 1.4.x   | :x:                |
| < 1.4   | :x:                |

## Reporting a Vulnerability

We take security vulnerabilities seriously. If you discover a security vulnerability in AMCP v1.5, please report it responsibly.

### Reporting Process

1. **Do NOT** create a public GitHub issue for security vulnerabilities
2. Email security details to: `security@amcp-project.org`
3. Include the following information:
   - AMCP version affected
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact assessment
   - Suggested mitigation (if any)

### Response Timeline

- **24 hours**: Initial acknowledgment of your report
- **72 hours**: Initial assessment and severity classification
- **7 days**: Detailed response with our planned approach
- **30 days**: Target resolution for critical vulnerabilities
- **90 days**: Target resolution for non-critical vulnerabilities

### Severity Classification

We use the following severity levels:

#### Critical (CVSS 9.0-10.0)
- Remote code execution
- Privilege escalation to system level
- Complete system compromise

#### High (CVSS 7.0-8.9)
- Significant data exposure
- Authentication bypass
- Privilege escalation

#### Medium (CVSS 4.0-6.9)
- Limited data exposure
- Denial of service
- Information disclosure

#### Low (CVSS 0.1-3.9)
- Minor information leaks
- Limited impact vulnerabilities

### Security Response Process

1. **Triage**: We assess the report and classify severity
2. **Investigation**: Our security team investigates the vulnerability
3. **Fix Development**: We develop and test a security fix
4. **Coordinated Disclosure**: We work with you on disclosure timing
5. **Release**: We release the security update
6. **Advisory**: We publish a security advisory (if appropriate)

### Recognition

We appreciate security researchers who help keep AMCP secure:

- We'll acknowledge your contribution in our security advisory
- We may feature your research in our project documentation
- For significant vulnerabilities, we'll recognize you in our release notes

### Security Best Practices for Users

#### Deployment Security

- **Network Security**: Deploy AMCP behind firewalls and use network segmentation
- **Authentication**: Enable authentication for all production deployments
- **Encryption**: Use TLS/SSL for all network communications
- **Access Control**: Implement least-privilege access principles

#### Configuration Security

```properties
# Secure configuration examples
amcp.security.authentication.enabled=true
amcp.security.encryption.enabled=true
amcp.security.audit.enabled=true
amcp.network.bind.address=127.0.0.1  # Don't bind to 0.0.0.0 in production
```

#### Agent Security

- **Input Validation**: Always validate event payloads
- **Resource Limits**: Implement proper resource limits for agents
- **Sanitization**: Sanitize all external inputs
- **Error Handling**: Don't expose sensitive information in error messages

```java
@Override
public CompletableFuture<Void> handleEvent(Event event) {
    return CompletableFuture.runAsync(() -> {
        try {
            // Validate input
            if (!isValidPayload(event.getPayload())) {
                logger.warn("Invalid payload received from {}", event.getSender());
                return;
            }
            
            // Process safely
            processEvent(event);
            
        } catch (Exception e) {
            logger.error("Event processing failed", e);
            // Don't expose internal details
        }
    });
}
```

### Known Security Considerations

#### Event System Security

- **Topic Authorization**: Implement topic-based access control
- **Payload Validation**: Validate all event payloads
- **Rate Limiting**: Implement rate limiting to prevent DoS

#### Agent Mobility Security

- **Code Signing**: Verify agent code integrity during migration
- **Sandboxing**: Run agents in restricted environments
- **State Validation**: Validate agent state during serialization/deserialization

#### External Integration Security

- **API Key Management**: Store API keys securely
- **Input Sanitization**: Sanitize data from external services
- **Connection Security**: Use secure connections for external APIs

### Security Testing

We perform regular security testing including:

- **Static Analysis**: Automated code analysis for vulnerabilities
- **Dependency Scanning**: Regular dependency vulnerability scans
- **Penetration Testing**: Periodic security assessments
- **Code Reviews**: Security-focused code reviews

### Security Updates

Security updates are distributed through:

- **GitHub Releases**: Security patches in new releases
- **Security Advisories**: GitHub Security Advisories
- **Mailing List**: Security notifications to subscribers
- **Documentation**: Updated security guidelines

### Contact Information

- **Security Email**: security@amcp-project.org
- **General Contact**: info@amcp-project.org
- **GitHub Issues**: For non-security bugs and features

### Legal

This security policy is subject to our project's license terms. Security researchers acting in good faith will not face legal action for their research.

---

Thank you for helping keep AMCP v1.5 secure!