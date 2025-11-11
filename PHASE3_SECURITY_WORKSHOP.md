# 🔐 Phase 3: Security Workshop with Red Hat Architects

**Timeline**: Week 3-4  
**Priority**: CRITICAL  
**Status**: 📋 Ready for Execution

---

## Workshop Overview

**Duration**: 4 hours  
**Participants**: AMCP team + Red Hat security architects  
**Goal**: Design comprehensive security model for enterprise AMCP deployments

---

## Session 1: Threat Model & Requirements (1 hour)

### 1.1 Agent Authentication

**Discussion Points**:

1. **Identity Verification**
   - How do we verify an agent is who it claims to be?
   - Should agents have digital certificates?
   - Should agents have JWT tokens?
   - **Question**: mTLS, JWT, or both?

2. **Agent-to-Agent Trust**
   - Can agents trust each other implicitly?
   - Should there be a trust chain?
   - How do we handle untrusted agents?
   - **Proposal**: JWT tokens with issuer verification

3. **Token Management**
   - Who issues tokens? (Keycloak/Red Hat SSO)
   - Token lifetime? (1 hour recommended)
   - Refresh mechanism?
   - **Proposal**: Service account per agent context

4. **Agent Lifecycle**
   - How do we revoke agent access?
   - What happens when token expires?
   - Can agents renew tokens?
   - **Proposal**: Automatic refresh + revocation list

---

### 1.2 Agent Authorization

**Discussion Points**:

1. **Topic Access Control**
   - Can agents subscribe to any topic?
   - Should certain topics be restricted?
   - How do we enforce topic permissions?
   - **Proposal**: RBAC with topic patterns

2. **Agent Migration**
   - Should agents be able to migrate anywhere?
   - Should migration require special permission?
   - How do we prevent unauthorized migration?
   - **Proposal**: "mobility:migrate" role required

3. **API Access**
   - Should agents access external APIs?
   - How do we manage API credentials?
   - Should API calls be logged?
   - **Proposal**: OAuth2 client credentials

4. **Role Definition**
   - What roles should exist?
   - Should roles be hierarchical?
   - Can agents have multiple roles?
   - **Proposal**: admin, agent, guest, service roles

---

### 1.3 Message Integrity

**Discussion Points**:

1. **Encryption**
   - Should all messages be encrypted?
   - TLS 1.3 for transport?
   - End-to-end encryption needed?
   - **Proposal**: TLS 1.3 mandatory, E2E optional

2. **Signing**
   - Do we need message signatures?
   - For all messages or critical only?
   - HMAC-SHA256 or RSA?
   - **Proposal**: Optional signing for critical events

3. **Tampering Detection**
   - How do we detect message tampering?
   - What's the response?
   - Should we log tampering attempts?
   - **Proposal**: Signature verification + audit log

4. **Replay Prevention**
   - How do we prevent replay attacks?
   - Sequence numbers or timestamps?
   - Nonce-based approach?
   - **Proposal**: Timestamp + conversation_id

---

### 1.4 Audit & Compliance

**Discussion Points**:

1. **Audit Logging**
   - What events should be logged?
   - Where should logs be stored?
   - How long to retain?
   - **Proposal**: All security events, 90 days retention

2. **Compliance Standards**
   - SOC2 Type II?
   - GDPR compliance?
   - HIPAA if healthcare?
   - **Proposal**: SOC2 baseline for v1.6

3. **Log Protection**
   - Who can access logs?
   - Should logs be encrypted?
   - Can logs be tampered with?
   - **Proposal**: Elasticsearch with encryption

4. **Reporting**
   - What reports do we need?
   - Who needs access?
   - Automated or manual?
   - **Proposal**: Dashboard + monthly reports

---

## Session 2: Red Hat Security Patterns (1 hour)

### 2.1 Red Hat SSO (Keycloak) Integration

**Discussion Points**:

1. **Token Issuance**
   - Should agents use service accounts?
   - Should agents use user accounts?
   - How to obtain initial token?
   - **Proposal**: Service account per agent context

2. **Token Claims**
   - What claims should tokens have?
   - agent_id, agent_type, roles?
   - Custom claims?
   - **Proposal**: Standard OIDC + custom agent claims

3. **Token Validation**
   - Where should validation happen?
   - Quarkus security extension?
   - Custom interceptor?
   - **Proposal**: Quarkus OIDC extension

4. **Keycloak Setup**
   - Realm configuration?
   - Client setup?
   - Role mapping?
   - **Proposal**: Dedicated "amcp" realm

---

### 2.2 OpenShift Security Context

**Discussion Points**:

1. **Pod Security**
   - Pod security policies?
   - Network policies?
   - RBAC for pods?
   - **Proposal**: Restricted PSP + network policies

2. **Secret Management**
   - Store Kafka credentials in OpenShift secrets?
   - Rotate credentials?
   - Access control?
   - **Proposal**: OpenShift secrets + rotation

3. **Service Accounts**
   - One SA per agent context?
   - SA tokens for authentication?
   - SA permissions?
   - **Proposal**: One SA per deployment

4. **Network Policies**
   - Restrict pod-to-pod communication?
   - Allow only Kafka traffic?
   - Ingress/egress rules?
   - **Proposal**: Strict network policies

---

### 2.3 mTLS for Broker Communication

**Discussion Points**:

1. **Certificate Management**
   - Self-signed or CA-signed?
   - Certificate rotation?
   - Expiration handling?
   - **Proposal**: CA-signed, auto-rotation

2. **Kafka SSL/SASL**
   - SASL_SSL for Kafka?
   - Client certificates?
   - Server certificate validation?
   - **Proposal**: SASL_SSL + mTLS

3. **NATS Security**
   - NKeys or credentials?
   - How to distribute?
   - Rotation strategy?
   - **Proposal**: NKeys with rotation

4. **Certificate Pinning**
   - Should we pin certificates?
   - How to handle rotation?
   - Performance impact?
   - **Proposal**: Optional pinning

---

### 2.4 OAuth2 for External APIs

**Discussion Points**:

1. **Credential Storage**
   - Store in Quarkus vault?
   - Environment variables?
   - Kubernetes secrets?
   - **Proposal**: Quarkus vault + secrets

2. **Token Flow**
   - Client credentials flow?
   - Authorization code flow?
   - Implicit flow?
   - **Proposal**: Client credentials for services

3. **Token Refresh**
   - Automatic refresh?
   - Manual refresh?
   - Expiration handling?
   - **Proposal**: Automatic with fallback

4. **Scope Management**
   - Minimal scopes per agent?
   - Dynamic scopes?
   - Scope validation?
   - **Proposal**: Minimal scopes, predefined

---

## Session 3: Implementation Strategy (1 hour)

### 3.1 v1.6 Security Baseline

**Discussion Points**:

1. **Minimum Viable Security**
   - What's essential for v1.6?
   - What can wait for v1.7?
   - What's nice-to-have?
   - **Proposal**: Transport + Identity + RBAC

2. **Transport Encryption**
   - Mandatory TLS 1.3?
   - Broker-specific config?
   - Certificate validation?
   - **Proposal**: Mandatory, broker-specific

3. **Agent Identity**
   - JWT tokens from Keycloak?
   - Token validation in Quarkus?
   - Token claims?
   - **Proposal**: JWT + OIDC validation

4. **Basic RBAC**
   - Predefined roles?
   - Topic-based access?
   - Migration restrictions?
   - **Proposal**: 4 roles, topic patterns

---

### 3.2 v1.7 Enhancements

**Discussion Points**:

1. **Message Signing**
   - Implement for critical events?
   - HMAC-SHA256?
   - Signature verification?
   - **Proposal**: Optional signing, v1.7

2. **Fine-Grained Authorization**
   - Attribute-based access control?
   - Policy engine (OPA)?
   - Dynamic policies?
   - **Proposal**: ABAC with OPA, v1.7

3. **Audit Logging**
   - Elasticsearch integration?
   - Kibana dashboards?
   - Alert rules?
   - **Proposal**: ELK stack, v1.7

4. **Compliance Reporting**
   - SOC2 reports?
   - GDPR compliance?
   - Automated checks?
   - **Proposal**: Automated reports, v1.7

---

### 3.3 v2.0 Enterprise Features

**Discussion Points**:

1. **End-to-End Encryption**
   - Agent-to-agent encryption?
   - Key management?
   - Performance impact?
   - **Proposal**: E2E optional, v2.0

2. **Advanced Threat Detection**
   - Anomaly detection?
   - Intrusion detection?
   - ML-based?
   - **Proposal**: Baseline rules, v2.0

3. **Compliance Automation**
   - Continuous compliance?
   - Automated remediation?
   - Policy as code?
   - **Proposal**: Policy as code, v2.0

4. **Federation**
   - Cross-domain agents?
   - Trust relationships?
   - Delegation?
   - **Proposal**: Federation, v2.0

---

### 3.4 Integration Points

**Discussion Points**:

1. **Quarkus Security Extension**
   - Where does security fit?
   - Interceptor pattern?
   - Annotation-based?
   - **Proposal**: Security interceptor

2. **Event Broker Security**
   - Broker-specific config?
   - Credential management?
   - Connection pooling?
   - **Proposal**: Broker abstraction layer

3. **A2A Protocol Security**
   - OAuth2 for A2A?
   - Message signing?
   - Token in headers?
   - **Proposal**: OAuth2 + optional signing

4. **MCP Tool Security**
   - Tool authentication?
   - Authorization?
   - Audit logging?
   - **Proposal**: OAuth2 + RBAC

---

## Session 4: Roadmap & Action Items (1 hour)

### 4.1 Design Documents

**Discussion Points**:

1. **Security Architecture Doc**
   - Who writes it?
   - Timeline?
   - Review process?
   - **Action**: Assign owner, 2 weeks

2. **Threat Model**
   - Detailed threat analysis?
   - Risk ratings?
   - Mitigation strategies?
   - **Action**: Security team, 1 week

3. **Configuration Guide**
   - Keycloak setup?
   - Kafka SSL/SASL?
   - OpenShift integration?
   - **Action**: DevOps team, 2 weeks

4. **Best Practices**
   - Security guidelines?
   - Common mistakes?
   - Troubleshooting?
   - **Action**: Documentation team, 2 weeks

---

### 4.2 Implementation Tasks

**Discussion Points**:

1. **Task Breakdown**
   - Estimate effort per component?
   - Identify dependencies?
   - Resource allocation?
   - **Action**: Create JIRA tickets, 1 week

2. **Testing Strategy**
   - Unit tests?
   - Integration tests?
   - Security tests?
   - **Action**: QA team, 1 week

3. **Code Review**
   - Security review process?
   - Peer review?
   - Red Hat review?
   - **Action**: Define process, ongoing

4. **Release Criteria**
   - Security gates?
   - Compliance checks?
   - Approval process?
   - **Action**: Define criteria, 1 week

---

### 4.3 Testing & Validation

**Discussion Points**:

1. **Security Testing**
   - Penetration testing?
   - Vulnerability scanning?
   - Compliance validation?
   - **Action**: Define scope, 2 weeks

2. **Performance Testing**
   - Security overhead?
   - Throughput impact?
   - Latency impact?
   - **Action**: Benchmark, 2 weeks

3. **Integration Testing**
   - Keycloak integration?
   - Kafka SSL/SASL?
   - OpenShift deployment?
   - **Action**: Test plan, 2 weeks

4. **Production Readiness**
   - Security checklist?
   - Deployment guide?
   - Incident response?
   - **Action**: Create runbooks, 3 weeks

---

### 4.4 Documentation

**Discussion Points**:

1. **Architecture Documentation**
   - Diagrams?
   - Sequence flows?
   - Decision rationale?
   - **Action**: Assign writers, 3 weeks

2. **Configuration Guide**
   - Step-by-step setup?
   - Example configs?
   - Troubleshooting?
   - **Action**: DevOps team, 3 weeks

3. **Developer Guide**
   - Security best practices?
   - Code examples?
   - Common patterns?
   - **Action**: Dev team, 3 weeks

4. **Operator Guide**
   - Deployment guide?
   - Monitoring?
   - Incident response?
   - **Action**: Ops team, 3 weeks

---

## Pre-Workshop Preparation

### Materials to Review

1. **AMCP Architecture Overview**
   - Current design
   - Agent model
   - Event broker

2. **Quarkus Security Features**
   - OIDC integration
   - JWT support
   - Security annotations

3. **Red Hat Security Best Practices**
   - OpenShift security
   - Keycloak integration
   - mTLS patterns

4. **A2A & MCP Specs**
   - Protocol details
   - Security considerations
   - Integration points

---

## Expected Outcomes

1. ✅ **Security Architecture Document**
   - All 5 security layers defined
   - Implementation approach
   - Timeline and resources

2. ✅ **Threat Model & Risk Assessment**
   - Identified threats
   - Risk ratings
   - Mitigation strategies

3. ✅ **Implementation Roadmap**
   - v1.6 baseline
   - v1.7 enhancements
   - v2.0 enterprise features

4. ✅ **Configuration Guide**
   - Keycloak setup
   - Kafka SSL/SASL
   - OpenShift integration

5. ✅ **Testing Plan**
   - Security test cases
   - Penetration testing scope
   - Compliance validation

---

## Success Criteria

- ✅ Comprehensive threat model
- ✅ Security architecture approved
- ✅ Implementation roadmap agreed
- ✅ Red Hat alignment confirmed
- ✅ All action items assigned
- ✅ Timeline established
