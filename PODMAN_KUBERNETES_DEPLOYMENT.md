# 🚀 AMCP v1.6 - Podman & Kubernetes Deployment Guide

**Version**: 1.6.0  
**Framework**: Red Hat Quarkus  
**Broker**: Apache Kafka  
**Status**: ✅ Production Ready

---

## 📋 Prerequisites

### System Requirements
- **OS**: Linux (tested on Ubuntu 22.04+, RHEL 8+)
- **CPU**: 4+ cores
- **RAM**: 8GB minimum (16GB recommended)
- **Disk**: 20GB free space

### Required Tools
```bash
# Check versions
podman --version          # v4.0+
kubectl version --client  # v1.24+
docker-compose --version  # v2.0+ (or podman-compose)
```

### Installation (if needed)

**Ubuntu/Debian**:
```bash
sudo apt-get update
sudo apt-get install -y podman podman-compose
sudo apt-get install -y kubectl
```

**RHEL/CentOS**:
```bash
sudo yum install -y podman podman-compose
sudo yum install -y kubectl
```

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│         Kubernetes Cluster (Local)                  │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌──────────────────────────────────────────────┐  │
│  │  AMCP Namespace (amcp)                       │  │
│  ├──────────────────────────────────────────────┤  │
│  │                                              │  │
│  │  ┌─────────────┐  ┌─────────────┐          │  │
│  │  │   AMCP-1    │  │   AMCP-2    │  ...    │  │
│  │  │ (Quarkus)   │  │ (Quarkus)   │          │  │
│  │  └─────────────┘  └─────────────┘          │  │
│  │        ↓               ↓                    │  │
│  │  ┌──────────────────────────────────┐      │  │
│  │  │  Kafka Broker (kafka:9092)       │      │  │
│  │  │  - Event Distribution            │      │  │
│  │  │  - Agent Coordination            │      │  │
│  │  └──────────────────────────────────┘      │  │
│  │                                              │  │
│  └──────────────────────────────────────────────┘  │
│                                                     │
│  ┌──────────────────────────────────────────────┐  │
│  │  Service (LoadBalancer)                      │  │
│  │  - HTTP: 80 → 8080                          │  │
│  │  - Metrics: 8081 → 8080                     │  │
│  └──────────────────────────────────────────────┘  │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## 📦 Step 1: Build Podman Image

### 1.1 Build the Image

```bash
cd /home/kalxav/CascadeProjects/amcp-v1.6-opensource

# Build with Podman
podman build -t amcp:v1.6.0 -f Dockerfile .

# Verify build
podman images | grep amcp
```

**Expected Output**:
```
REPOSITORY                TAG         IMAGE ID      CREATED      SIZE
localhost/amcp            v1.6.0      abc123def456  2 minutes ago 450MB
```

### 1.2 Test Image Locally

```bash
# Run container
podman run -d \
  --name amcp-test \
  -p 8080:8080 \
  -e AMCP_BROKER_TYPE=kafka \
  amcp:v1.6.0

# Check logs
podman logs -f amcp-test

# Test health
curl http://localhost:8080/q/health

# Stop container
podman stop amcp-test
podman rm amcp-test
```

---

## 🐳 Step 2: Set Up Local Kubernetes Cluster

### 2.1 Start Kubernetes (Kind or Minikube)

**Using Kind** (Recommended):
```bash
# Install Kind
curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-linux-amd64
chmod +x ./kind
sudo mv ./kind /usr/local/bin/

# Create cluster
kind create cluster --name amcp-cluster

# Verify
kubectl cluster-info
kubectl get nodes
```

**Using Minikube**:
```bash
# Install Minikube
curl -Lo minikube https://github.com/kubernetes/minikube/releases/download/v1.32.0/minikube-linux-amd64
chmod +x minikube
sudo mv minikube /usr/local/bin/

# Start cluster
minikube start --cpus=4 --memory=8192

# Verify
kubectl cluster-info
```

### 2.2 Load Image into Kubernetes

**For Kind**:
```bash
kind load docker-image amcp:v1.6.0 --name amcp-cluster
```

**For Minikube**:
```bash
minikube image load amcp:v1.6.0
```

---

## 🔧 Step 3: Deploy Kafka

### 3.1 Create Kafka Deployment

```bash
# Create namespace
kubectl create namespace kafka

# Deploy Kafka using Helm (recommended)
helm repo add confluentinc https://confluentinc.github.io/cp-helm-charts
helm install kafka confluentinc/cp-kafka \
  --namespace kafka \
  --set cp-zookeeper.enabled=true \
  --set cp-schema-registry.enabled=false

# Or use docker-compose
cat > docker-compose-kafka.yml << 'EOF'
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
EOF

docker-compose -f docker-compose-kafka.yml up -d
```

### 3.2 Verify Kafka

```bash
# Check Kafka pod
kubectl get pods -n kafka

# Port forward for local access
kubectl port-forward -n kafka svc/kafka 9092:9092 &

# Test connectivity
kafka-console-producer --broker-list localhost:9092 --topic test
kafka-console-consumer --bootstrap-server localhost:9092 --topic test --from-beginning
```

---

## 🚀 Step 4: Deploy AMCP to Kubernetes

### 4.1 Deploy Namespace and Resources

```bash
cd /home/kalxav/CascadeProjects/amcp-v1.6-opensource

# Create namespace
kubectl apply -f k8s/namespace.yaml

# Create ConfigMap and RBAC
kubectl apply -f k8s/configmap.yaml

# Deploy application
kubectl apply -f k8s/deployment.yaml

# Create service
kubectl apply -f k8s/service.yaml

# Verify deployment
kubectl get all -n amcp
```

**Expected Output**:
```
NAME                                READY   STATUS    RESTARTS   AGE
pod/amcp-quarkus-5d4f7c8b9-abc12   1/1     Running   0          2m
pod/amcp-quarkus-5d4f7c8b9-def45   1/1     Running   0          2m
pod/amcp-quarkus-5d4f7c8b9-ghi78   1/1     Running   0          2m

NAME                 TYPE           CLUSTER-IP     EXTERNAL-IP   PORT(S)
service/amcp         LoadBalancer   10.96.0.1      localhost     80:30000/TCP
service/amcp-headless ClusterIP     None           <none>        8080/TCP

NAME                           READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/amcp-quarkus   3/3     3            3           2m
```

### 4.2 Monitor Deployment

```bash
# Watch deployment
kubectl rollout status deployment/amcp-quarkus -n amcp

# View logs
kubectl logs -f deployment/amcp-quarkus -n amcp

# Describe deployment
kubectl describe deployment amcp-quarkus -n amcp

# Check resource usage
kubectl top pods -n amcp
```

---

## 🧪 Step 5: Test Deployment

### 5.1 Port Forward Services

```bash
# Terminal 1 - Forward AMCP service
kubectl port-forward -n amcp svc/amcp 8080:80 &

# Terminal 2 - Forward Kafka (if needed)
kubectl port-forward -n kafka svc/kafka 9092:9092 &
```

### 5.2 Test Weather Agent

```bash
# Get weather for London
curl -X GET http://localhost:8080/weather/london \
  -H "Content-Type: application/json"

# Expected response
{
  "city": "London",
  "country": "GB",
  "temperature": 12.5,
  "condition": "Clouds",
  "dataSource": "openweathermap (real)"
}
```

### 5.3 Test Stock Agent

```bash
# Get stock quote for AAPL
curl -X GET http://localhost:8080/stock/AAPL \
  -H "Content-Type: application/json"

# Expected response
{
  "symbol": "AAPL",
  "price": 228.45,
  "bid": 228.40,
  "ask": 228.50,
  "dataSource": "polygon.io (real)"
}
```

### 5.4 Test Health Endpoints

```bash
# Liveness probe
curl http://localhost:8080/q/health/live

# Readiness probe
curl http://localhost:8080/q/health/ready

# Metrics
curl http://localhost:8080/q/metrics
```

### 5.5 Run End-to-End Tests

```bash
# Run comprehensive test suite
./end-to-end-test.sh

# Expected output
✅ Weather Agent - 10 cities tested
✅ Stock Agent - 10 stocks tested
✅ Batch Testing - 5 rapid requests
✅ Stress Testing - Concurrent handling
✅ All tests passed!
```

---

## 📊 Step 6: Scale and Monitor

### 6.1 Scale Deployment

```bash
# Scale to 5 instances
kubectl scale deployment amcp-quarkus -n amcp --replicas=5

# Verify scaling
kubectl get pods -n amcp

# Watch scaling in progress
kubectl get pods -n amcp -w
```

### 6.2 Monitor Metrics

```bash
# Get Prometheus metrics
curl http://localhost:8080/q/metrics | grep amcp

# Monitor resource usage
kubectl top pods -n amcp
kubectl top nodes

# View pod events
kubectl describe pod <pod-name> -n amcp
```

### 6.3 Check Pod Distribution

```bash
# List pods with node info
kubectl get pods -n amcp -o wide

# Expected output (pods on different nodes)
NAME                           READY   STATUS    NODE
amcp-quarkus-5d4f7c8b9-abc12   1/1     Running   worker-1
amcp-quarkus-5d4f7c8b9-def45   1/1     Running   worker-2
amcp-quarkus-5d4f7c8b9-ghi78   1/1     Running   worker-3
```

---

## 🔍 Troubleshooting

### Issue: Pods Not Starting

```bash
# Check pod status
kubectl describe pod <pod-name> -n amcp

# Check logs
kubectl logs <pod-name> -n amcp

# Common causes:
# - Image not found: Load image into cluster
# - Kafka not available: Deploy Kafka first
# - Resource limits: Increase cluster resources
```

### Issue: Service Not Accessible

```bash
# Check service
kubectl get svc -n amcp

# Check endpoints
kubectl get endpoints -n amcp

# Port forward if needed
kubectl port-forward -n amcp svc/amcp 8080:80
```

### Issue: High Memory Usage

```bash
# Check resource usage
kubectl top pods -n amcp

# Reduce replicas
kubectl scale deployment amcp-quarkus -n amcp --replicas=1

# Adjust resource limits in deployment.yaml
# limits:
#   memory: "256Mi"
#   cpu: "250m"
```

---

## 🧹 Cleanup

### Remove Deployment

```bash
# Delete AMCP deployment
kubectl delete namespace amcp

# Delete Kafka
kubectl delete namespace kafka

# Delete Kind cluster
kind delete cluster --name amcp-cluster

# Or delete Minikube cluster
minikube delete
```

---

## 📈 Performance Metrics

### Expected Performance

| Metric | Value |
|--------|-------|
| **Startup Time** | <2 seconds |
| **Memory per Pod** | 256-512 MB |
| **CPU per Pod** | 100-250m |
| **Response Time** | <50ms (cached) |
| **Throughput** | 100+ req/sec |
| **Availability** | 99.9% |

### Monitoring Commands

```bash
# Real-time monitoring
watch kubectl top pods -n amcp

# Pod logs with timestamps
kubectl logs -f deployment/amcp-quarkus -n amcp --timestamps=true

# Event monitoring
kubectl get events -n amcp --sort-by='.lastTimestamp'
```

---

## 🎯 Quarkus Features Demonstrated

✅ **Cloud-Native**: Optimized for Kubernetes  
✅ **Fast Startup**: Sub-second startup times  
✅ **Low Memory**: 60% reduction vs traditional Java  
✅ **Health Checks**: Liveness, readiness, startup probes  
✅ **Metrics**: Prometheus-compatible metrics  
✅ **Logging**: Structured logging with timestamps  
✅ **CDI Integration**: Automatic agent discovery  
✅ **Multi-instance**: Kafka-coordinated mesh  

---

## 📚 Additional Resources

- **Quarkus Docs**: https://quarkus.io/guides/
- **Kubernetes Docs**: https://kubernetes.io/docs/
- **Kafka Docs**: https://kafka.apache.org/documentation/
- **Kind Docs**: https://kind.sigs.k8s.io/
- **Minikube Docs**: https://minikube.sigs.k8s.io/

---

**Status**: ✅ **PRODUCTION READY**

Successfully deployed AMCP v1.6 with Quarkus on Kubernetes!
