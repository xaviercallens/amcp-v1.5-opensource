# AMCP Deployment Guide

Complete guide for deploying AMCP (Agent Mesh Communication Protocol) v1.5 in production environments.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Deployment Options](#deployment-options)
- [Kubernetes Deployment](#kubernetes-deployment)
- [Docker Deployment](#docker-deployment)
- [Cloud Platform Deployment](#cloud-platform-deployment)
- [Event Broker Configuration](#event-broker-configuration)
- [Security Configuration](#security-configuration)
- [Monitoring and Observability](#monitoring-and-observability)
- [High Availability](#high-availability)
- [Performance Tuning](#performance-tuning)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

### System Requirements

- **Java:** OpenJDK 21 or higher
- **Maven:** 3.8+ (for building)
- **Memory:** Minimum 2GB RAM per agent context
- **CPU:** 2+ cores recommended
- **Storage:** 10GB+ for logs and state

### Network Requirements

- **Ports:**
  - Agent Context: 8080 (HTTP), 8443 (HTTPS)
  - Kafka: 9092 (PLAINTEXT), 9093 (SSL)
  - NATS: 4222 (client), 6222 (cluster), 8222 (monitoring)
  - Prometheus: 9090
  - Grafana: 3000

---

## Deployment Options

### 1. **Local Development**
- In-memory EventBroker
- Single agent context
- No external dependencies
- Fast startup for testing

### 2. **Small Scale (< 100 agents)**
- Docker Compose
- Kafka or NATS broker
- Single-node deployment
- Basic monitoring

### 3. **Production Scale (100+ agents)**
- Kubernetes cluster
- Distributed Kafka cluster
- Multi-region deployment
- Full observability stack

---

## Kubernetes Deployment

### Basic Deployment

#### 1. Create Namespace

```bash
kubectl create namespace amcp
```

#### 2. Deploy Event Broker (Kafka)

```yaml
# kafka-deployment.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: kafka
  namespace: amcp
spec:
  serviceName: kafka
  replicas: 3
  selector:
    matchLabels:
      app: kafka
  template:
    metadata:
      labels:
        app: kafka
    spec:
      containers:
      - name: kafka
        image: confluentinc/cp-kafka:latest
        ports:
        - containerPort: 9092
          name: plaintext
        - containerPort: 9093
          name: ssl
        env:
        - name: KAFKA_ADVERTISED_LISTENERS
          value: "PLAINTEXT://kafka:9092,SSL://kafka:9093"
        - name: KAFKA_ZOOKEEPER_CONNECT
          value: "zookeeper:2181"
        - name: KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR
          value: "3"
        volumeMounts:
        - name: kafka-data
          mountPath: /var/lib/kafka/data
  volumeClaimTemplates:
  - metadata:
      name: kafka-data
    spec:
      accessModes: [ "ReadWriteOnce" ]
      resources:
        requests:
          storage: 100Gi
```

```bash
kubectl apply -f kafka-deployment.yaml
```

#### 3. Deploy AMCP Agent Context

```yaml
# amcp-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: amcp-context
  namespace: amcp
spec:
  replicas: 3
  selector:
    matchLabels:
      app: amcp-context
  template:
    metadata:
      labels:
        app: amcp-context
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/metrics"
    spec:
      containers:
      - name: amcp
        image: amcp/agent-context:1.5.0
        ports:
        - containerPort: 8080
          name: http
        - containerPort: 8443
          name: https
        env:
        - name: AMCP_EVENT_BROKER_TYPE
          value: "kafka"
        - name: KAFKA_BOOTSTRAP_SERVERS
          value: "kafka:9092"
        - name: AMCP_MIGRATION_ENABLED
          value: "true"
        - name: AMCP_SECURITY_ENABLED
          value: "true"
        - name: JAVA_OPTS
          value: "-Xms2g -Xmx4g -XX:+UseG1GC"
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /ready
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
```

```bash
kubectl apply -f amcp-deployment.yaml
```

#### 4. Create Service

```yaml
# amcp-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: amcp-service
  namespace: amcp
spec:
  selector:
    app: amcp-context
  ports:
  - name: http
    port: 8080
    targetPort: 8080
  - name: https
    port: 8443
    targetPort: 8443
  type: LoadBalancer
```

```bash
kubectl apply -f amcp-service.yaml
```

### Helm Deployment

```bash
# Add AMCP Helm repository
helm repo add amcp https://charts.amcp.io
helm repo update

# Install with custom values
helm install amcp amcp/amcp \
  --namespace amcp \
  --create-namespace \
  --set broker.type=kafka \
  --set kafka.replicas=3 \
  --set context.replicas=3 \
  --set monitoring.enabled=true
```

#### Custom Values (values.yaml)

```yaml
broker:
  type: kafka  # kafka, nats, or memory
  
kafka:
  enabled: true
  replicas: 3
  storage: 100Gi
  bootstrapServers: "kafka:9092"
  
context:
  replicas: 3
  image:
    repository: amcp/agent-context
    tag: "1.5.0"
  resources:
    requests:
      memory: "2Gi"
      cpu: "1000m"
    limits:
      memory: "4Gi"
      cpu: "2000m"
  
migration:
  enabled: true
  timeout: 30s
  
security:
  enabled: true
  tls:
    enabled: true
    certManager:
      enabled: true
      issuer: letsencrypt-prod
  
monitoring:
  enabled: true
  prometheus:
    enabled: true
  grafana:
    enabled: true
    adminPassword: "changeme"
  
istio:
  enabled: true
  mtls:
    mode: STRICT
```

---

## Docker Deployment

### Docker Compose

#### Full Stack with Monitoring

```yaml
# docker-compose.yml
version: '3.8'

services:
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    volumes:
      - zookeeper-data:/var/lib/zookeeper/data
      - zookeeper-log:/var/lib/zookeeper/log
    networks:
      - amcp-network

  kafka:
    image: confluentinc/cp-kafka:latest
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    volumes:
      - kafka-data:/var/lib/kafka/data
    networks:
      - amcp-network

  amcp-context:
    image: amcp/agent-context:1.5.0
    depends_on:
      - kafka
    ports:
      - "8080:8080"
      - "8443:8443"
    environment:
      AMCP_EVENT_BROKER_TYPE: kafka
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      AMCP_MIGRATION_ENABLED: "true"
      JAVA_OPTS: "-Xms2g -Xmx4g"
    volumes:
      - amcp-data:/app/data
      - amcp-logs:/app/logs
    networks:
      - amcp-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
    networks:
      - amcp-network

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
      GF_INSTALL_PLUGINS: grafana-piechart-panel
    volumes:
      - grafana-data:/var/lib/grafana
      - ./grafana-dashboards:/etc/grafana/provisioning/dashboards
    networks:
      - amcp-network

volumes:
  zookeeper-data:
  zookeeper-log:
  kafka-data:
  amcp-data:
  amcp-logs:
  prometheus-data:
  grafana-data:

networks:
  amcp-network:
    driver: bridge
```

### Build Custom Docker Image

```dockerfile
# Dockerfile
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="amcp@example.com"
LABEL version="1.5.0"

# Install required packages
RUN apk add --no-cache curl bash

# Create app directory
WORKDIR /app

# Copy application JARs
COPY core/target/amcp-core-*.jar /app/lib/
COPY connectors/target/amcp-connectors-*.jar /app/lib/
COPY examples/target/amcp-examples-*.jar /app/lib/

# Copy configuration
COPY config/application.properties /app/config/

# Expose ports
EXPOSE 8080 8443

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

# Run application
ENTRYPOINT ["java", \
            "-Xms2g", "-Xmx4g", \
            "-XX:+UseG1GC", \
            "-XX:MaxGCPauseMillis=200", \
            "-Dconfig.file=/app/config/application.properties", \
            "-jar", "/app/lib/amcp-core-*.jar"]
```

```bash
# Build image
docker build -t amcp/agent-context:1.5.0 .

# Run container
docker run -d \
  --name amcp-context \
  -p 8080:8080 \
  -p 8443:8443 \
  -e AMCP_EVENT_BROKER_TYPE=kafka \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  amcp/agent-context:1.5.0
```

---

## Cloud Platform Deployment

### AWS ECS

```json
{
  "family": "amcp-context",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "2048",
  "memory": "4096",
  "containerDefinitions": [
    {
      "name": "amcp-context",
      "image": "amcp/agent-context:1.5.0",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "AMCP_EVENT_BROKER_TYPE",
          "value": "kafka"
        },
        {
          "name": "KAFKA_BOOTSTRAP_SERVERS",
          "value": "kafka.amazonaws.com:9092"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/amcp-context",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

### Azure Container Instances

```bash
az container create \
  --resource-group amcp-rg \
  --name amcp-context \
  --image amcp/agent-context:1.5.0 \
  --cpu 2 \
  --memory 4 \
  --ports 8080 8443 \
  --environment-variables \
    AMCP_EVENT_BROKER_TYPE=kafka \
    KAFKA_BOOTSTRAP_SERVERS=kafka.servicebus.windows.net:9093
```

### Google Cloud Run

```bash
gcloud run deploy amcp-context \
  --image amcp/agent-context:1.5.0 \
  --platform managed \
  --region us-central1 \
  --memory 4Gi \
  --cpu 2 \
  --port 8080 \
  --set-env-vars AMCP_EVENT_BROKER_TYPE=kafka,KAFKA_BOOTSTRAP_SERVERS=kafka:9092
```

---

## Event Broker Configuration

### Kafka Configuration

```properties
# application.properties
amcp.event.broker.type=kafka
amcp.kafka.bootstrap.servers=kafka-1:9092,kafka-2:9092,kafka-3:9092
amcp.kafka.security.protocol=SASL_SSL
amcp.kafka.sasl.mechanism=SCRAM-SHA-512
amcp.kafka.sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required username="amcp" password="secret";
amcp.kafka.ssl.truststore.location=/certs/truststore.jks
amcp.kafka.ssl.truststore.password=changeit
amcp.kafka.consumer.group.id=amcp-agents
amcp.kafka.auto.offset.reset=earliest
```

### NATS Configuration

```properties
amcp.event.broker.type=nats
amcp.nats.servers=nats://nats-1:4222,nats://nats-2:4222,nats://nats-3:4222
amcp.nats.connection.name=amcp-context
amcp.nats.max.reconnect=60
amcp.nats.reconnect.wait=2s
amcp.nats.tls.enabled=true
amcp.nats.tls.cert=/certs/client.crt
amcp.nats.tls.key=/certs/client.key
```

---

## Security Configuration

### TLS/mTLS

```properties
# Enable TLS
amcp.security.tls.enabled=true
amcp.security.tls.keystore.path=/certs/keystore.jks
amcp.security.tls.keystore.password=changeit
amcp.security.tls.truststore.path=/certs/truststore.jks
amcp.security.tls.truststore.password=changeit

# Enable mTLS (mutual TLS)
amcp.security.mtls.enabled=true
amcp.security.mtls.require.client.auth=true
```

### Authentication

```properties
# OAuth2 configuration
amcp.security.oauth2.enabled=true
amcp.security.oauth2.issuer.uri=https://auth.example.com
amcp.security.oauth2.jwk.set.uri=https://auth.example.com/.well-known/jwks.json
```

---

## Monitoring and Observability

### Prometheus Metrics

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'amcp-context'
    kubernetes_sd_configs:
      - role: pod
        namespaces:
          names:
            - amcp
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
      - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
        action: replace
        regex: ([^:]+)(?::\d+)?;(\d+)
        replacement: $1:$2
        target_label: __address__
```

### Grafana Dashboards

Import the AMCP dashboard: [dashboard.json](../deploy/monitoring/grafana-dashboards/amcp-dashboard.json)

**Key Metrics:**
- Agent count (active, inactive, migrating)
- Event throughput (messages/sec)
- Event latency (p50, p95, p99)
- Migration success rate
- Error rates by topic

---

## High Availability

### Multi-Region Deployment

```yaml
# Primary Region (us-east-1)
region: us-east-1
contexts:
  - id: us-east-1-az1
    replicas: 3
  - id: us-east-1-az2
    replicas: 3

# Secondary Region (us-west-2)
region: us-west-2
contexts:
  - id: us-west-2-az1
    replicas: 3
  - id: us-west-2-az2
    replicas: 3

# Failover configuration
failover:
  enabled: true
  health_check_interval: 10s
  unhealthy_threshold: 3
  primary_region: us-east-1
  secondary_region: us-west-2
```

---

## Performance Tuning

### JVM Options

```bash
JAVA_OPTS="
  -Xms4g -Xmx8g
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:ParallelGCThreads=8
  -XX:ConcGCThreads=2
  -XX:InitiatingHeapOccupancyPercent=45
  -XX:+UseStringDeduplication
  -Djava.awt.headless=true
"
```

### Kafka Producer Tuning

```properties
amcp.kafka.producer.batch.size=16384
amcp.kafka.producer.linger.ms=10
amcp.kafka.producer.compression.type=lz4
amcp.kafka.producer.buffer.memory=33554432
```

---

## Troubleshooting

### Common Issues

1. **Agent Not Activating**
   - Check EventBroker connectivity
   - Verify authentication credentials
   - Review logs for exceptions

2. **Migration Failures**
   - Ensure network connectivity between contexts
   - Check serialization of agent state
   - Verify timeout settings

3. **High Latency**
   - Check broker performance
   - Review network latency
   - Tune JVM garbage collection

For more help, see [GitHub Issues](https://github.com/xaviercallens/amcp-v1.5-opensource/issues).

---

**For production deployments, always consult the [Security Guide](./SECURITY.md) and follow best practices!**
