# 🏥 Phase 1: MicroProfile Health & Metrics Implementation

**Timeline**: Week 1-2  
**Priority**: HIGH  
**Status**: 📋 Ready for Implementation

---

## Overview

Add Kubernetes-native observability to AMCP agents running in Quarkus.

---

## 1.1 Liveness Check

**File**: `quarkus-amcp/runtime/src/main/java/io/quarkus/amcp/runtime/AmcpHealthCheck.java`

```java
package io.quarkus.amcp.runtime;

import io.amcp.core.AgentContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

@Liveness
@ApplicationScoped
public class AmcpHealthCheck implements HealthCheck {
    
    @Inject
    AgentContext agentContext;
    
    @Override
    public HealthCheckResponse call() {
        try {
            boolean isHealthy = agentContext != null && 
                              agentContext.getBroker() != null &&
                              agentContext.getBroker().isRunning();
            
            var builder = HealthCheckResponse.named("amcp-agent-mesh")
                    .status(isHealthy);
            
            if (agentContext != null && agentContext.getBroker() != null) {
                builder.withData("broker_type", agentContext.getBroker().getBrokerType());
                builder.withData("instance_id", agentContext.getInstanceId());
                builder.withData("agents_active", agentContext.getAgentCount());
            }
            
            return builder.build();
        } catch (Exception e) {
            return HealthCheckResponse.named("amcp-agent-mesh")
                    .down()
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}
```

**Endpoint**: `GET /q/health/live`

---

## 1.2 Readiness Check

**File**: `quarkus-amcp/runtime/src/main/java/io/quarkus/amcp/runtime/AmcpReadinessCheck.java`

```java
package io.quarkus.amcp.runtime;

import io.amcp.core.AgentContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class AmcpReadinessCheck implements HealthCheck {
    
    @Inject
    AgentContext agentContext;
    
    @Override
    public HealthCheckResponse call() {
        try {
            boolean isReady = agentContext != null && 
                            agentContext.getBroker() != null &&
                            agentContext.getBroker().isRunning() &&
                            agentContext.getAgentCount() > 0;
            
            var builder = HealthCheckResponse.named("amcp-agent-mesh-ready")
                    .status(isReady);
            
            if (agentContext != null) {
                builder.withData("agents_registered", agentContext.getAgentCount());
                builder.withData("broker_connected", 
                    agentContext.getBroker() != null && 
                    agentContext.getBroker().isRunning());
            }
            
            return builder.build();
        } catch (Exception e) {
            return HealthCheckResponse.named("amcp-agent-mesh-ready")
                    .down()
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}
```

**Endpoint**: `GET /q/health/ready`

---

## 1.3 Metrics

**File**: `quarkus-amcp/runtime/src/main/java/io/quarkus/amcp/runtime/AmcpMetrics.java`

```java
package io.quarkus.amcp.runtime;

import io.amcp.core.AgentContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.metrics.annotation.Gauge;

@ApplicationScoped
public class AmcpMetrics {
    
    @Inject
    AgentContext agentContext;
    
    @Gauge(name = "amcp_agents_total", unit = "agents")
    public long getAgentCount() {
        return agentContext != null ? agentContext.getAgentCount() : 0;
    }
    
    @Gauge(name = "amcp_broker_connected", unit = "status")
    public long getBrokerStatus() {
        if (agentContext != null && agentContext.getBroker() != null && 
            agentContext.getBroker().isRunning()) {
            return 1;
        }
        return 0;
    }
    
    @Gauge(name = "amcp_mesh_running", unit = "status")
    public long getMeshStatus() {
        if (agentContext != null && agentContext.getBroker() != null) {
            return agentContext.getBroker().isRunning() ? 1 : 0;
        }
        return 0;
    }
}
```

**Endpoint**: `GET /q/metrics`

---

## 1.4 Dependencies

Add to `quarkus-amcp/runtime/pom.xml`:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-health</artifactId>
</dependency>

<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-micrometer-registry-prometheus</artifactId>
</dependency>
```

---

## 1.5 Configuration

```properties
quarkus.smallrye-health.root-path=/q/health
quarkus.micrometer.export.prometheus.enabled=true
```

---

## 1.6 Testing

```bash
#!/bin/bash
echo "Liveness:"
curl -s http://localhost:8080/q/health/live | jq .

echo "Readiness:"
curl -s http://localhost:8080/q/health/ready | jq .

echo "Metrics:"
curl -s http://localhost:8080/q/metrics | grep amcp_
```

---

## Success Criteria

- ✅ Health endpoints return correct status
- ✅ Metrics exposed in Prometheus format
- ✅ Kubernetes probes work
- ✅ Zero performance impact
