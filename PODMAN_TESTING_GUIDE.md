# 🐳 AMCP v1.6 Testing with Podman

**Date**: November 11, 2025  
**Status**: ✅ **PODMAN TESTING GUIDE READY**  
**Podman Version**: 4.3.1  
**Approach**: Native Podman + Rootless Mode

---

## 🚀 Quick Start with Podman

### Option 1: Using Podman with Rootless Mode (Recommended)

#### Step 1: Enable Rootless Podman
```bash
podman system migrate
podman info | grep rootless
```

#### Step 2: Start Kafka Container
```bash
podman run -d \
  --name amcp-zookeeper \
  -e ZOOKEEPER_CLIENT_PORT=2181 \
  -e ZOOKEEPER_TICK_TIME=2000 \
  -p 2181:2181 \
  docker.io/confluentinc/cp-zookeeper:7.5.0
```

#### Step 3: Start Kafka Broker
```bash
podman run -d \
  --name amcp-kafka \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=localhost:2181 \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  -p 9092:9092 \
  docker.io/confluentinc/cp-kafka:7.5.0
```

#### Step 4: Verify Kafka is Running
```bash
podman ps | grep kafka
podman logs amcp-kafka | tail -20
```

#### Step 5: Start AMCP Application
```bash
cd /home/kalxav/CascadeProjects/amcp-v1.6-opensource/amcp-examples
mvn quarkus:dev
```

#### Step 6: Run Tests (new terminal)
```bash
cd /home/kalxav/CascadeProjects/amcp-v1.6-opensource
./test-phase1-phase2.sh
```

---

### Option 2: Using Podman Compose (If UID/GID Issues Resolved)

```bash
cd /home/kalxav/CascadeProjects/amcp-v1.6-opensource
podman-compose -f docker-compose-kafka.yml up -d
```

---

### Option 3: Using Host Network (Fastest)

```bash
# Start Zookeeper
podman run -d \
  --name amcp-zookeeper \
  --network host \
  -e ZOOKEEPER_CLIENT_PORT=2181 \
  docker.io/confluentinc/cp-zookeeper:7.5.0

# Start Kafka
podman run -d \
  --name amcp-kafka \
  --network host \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=localhost:2181 \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  docker.io/confluentinc/cp-kafka:7.5.0
```

---

## 🧪 Testing with Podman

### Test 1: Verify Kafka is Running
```bash
podman ps | grep kafka
podman logs amcp-kafka | grep "started"
```

### Test 2: Check Kafka Connectivity
```bash
podman exec amcp-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### Test 3: Start AMCP Application
```bash
cd amcp-examples
mvn quarkus:dev
```

### Test 4: Run Automated Tests
```bash
./test-phase1-phase2.sh
```

### Test 5: Manual Endpoint Testing

**Liveness Check**:
```bash
curl -s http://localhost:8080/q/health/live | jq .
```

**Readiness Check**:
```bash
curl -s http://localhost:8080/q/health/ready | jq .
```

**Prometheus Metrics**:
```bash
curl -s http://localhost:8080/q/metrics | grep amcp_
```

**A2A Status**:
```bash
curl -s http://localhost:8080/a2a/status | jq .
```

---

## 🔧 Podman Commands Reference

### Container Management
```bash
# List all containers
podman ps -a

# View container logs
podman logs amcp-kafka

# Follow logs
podman logs -f amcp-kafka

# Stop container
podman stop amcp-kafka

# Start container
podman start amcp-kafka

# Remove container
podman rm amcp-kafka

# Execute command in container
podman exec amcp-kafka kafka-topics --list
```

### Network Management
```bash
# List networks
podman network ls

# Create network
podman network create amcp-network

# Inspect network
podman network inspect amcp-network
```

### Image Management
```bash
# List images
podman images

# Pull image
podman pull docker.io/confluentinc/cp-kafka:7.5.0

# Remove image
podman rmi docker.io/confluentinc/cp-kafka:7.5.0

# Inspect image
podman inspect docker.io/confluentinc/cp-kafka:7.5.0
```

---

## 🐛 Troubleshooting

### Issue: "short-name did not resolve to an alias"
**Solution**: Use full image paths with `docker.io/` prefix
```bash
podman pull docker.io/confluentinc/cp-kafka:7.5.0
```

### Issue: "insufficient UIDs or GIDs available"
**Solution**: Run podman system migrate
```bash
podman system migrate
```

### Issue: "Port already in use"
**Solution**: Use different port or stop existing container
```bash
podman stop amcp-kafka
podman rm amcp-kafka
```

### Issue: "Cannot connect to Kafka"
**Solution**: Check if container is running and logs
```bash
podman ps | grep kafka
podman logs amcp-kafka
```

### Issue: "Health check fails"
**Solution**: Wait longer for startup, check logs
```bash
sleep 10
curl http://localhost:8080/q/health/live
podman logs amcp-kafka | tail -50
```

---

## 📊 Expected Test Results

### Phase 1: Health & Metrics (9 tests)
```
✓ Liveness check returns amcp-agent-mesh
✓ Liveness check includes context_id
✓ Liveness check includes agents_active
✓ Readiness check returns amcp-agent-mesh-ready
✓ Readiness check includes agents_registered
✓ Readiness check includes ready_for_traffic
✓ Metrics includes amcp_agents_total
✓ Metrics includes amcp_broker_connected
✓ Metrics includes amcp_mesh_running
```

### Phase 2: A2A Gateway (6 tests)
```
✓ A2A status returns service name
✓ A2A status includes version
✓ A2A status includes status field
✓ A2A message accepted
✓ A2A message ID returned
✓ A2A conversations endpoint responds
```

---

## 📈 Performance with Podman

### Container Startup Times
- Zookeeper: ~5-10 seconds
- Kafka: ~10-15 seconds
- AMCP App: ~15-20 seconds

### Expected Response Times
- Health checks: <5ms
- Metrics: <50ms
- A2A endpoints: <20ms

---

## 🎯 Complete Testing Workflow

```bash
# 1. Start Zookeeper
podman run -d \
  --name amcp-zookeeper \
  -e ZOOKEEPER_CLIENT_PORT=2181 \
  -p 2181:2181 \
  docker.io/confluentinc/cp-zookeeper:7.5.0

# 2. Start Kafka
podman run -d \
  --name amcp-kafka \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=localhost:2181 \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  -p 9092:9092 \
  docker.io/confluentinc/cp-kafka:7.5.0

# 3. Wait for Kafka to start
sleep 15

# 4. Verify Kafka
podman logs amcp-kafka | grep "started"

# 5. Start AMCP (in new terminal)
cd amcp-examples
mvn quarkus:dev

# 6. Run tests (in another terminal)
./test-phase1-phase2.sh

# 7. Cleanup when done
podman stop amcp-kafka amcp-zookeeper
podman rm amcp-kafka amcp-zookeeper
```

---

## ✅ Verification Checklist

- [ ] Podman installed and working
- [ ] Images pulled successfully
- [ ] Zookeeper container running
- [ ] Kafka container running
- [ ] Kafka logs show "started"
- [ ] AMCP application started
- [ ] Health endpoints responding
- [ ] Metrics endpoint working
- [ ] A2A gateway responding
- [ ] All 15 tests passing

---

## 📞 Quick Commands

```bash
# Check Podman version
podman --version

# List running containers
podman ps

# View Kafka logs
podman logs -f amcp-kafka

# Stop all containers
podman stop amcp-kafka amcp-zookeeper

# Remove all containers
podman rm amcp-kafka amcp-zookeeper

# Run tests
./test-phase1-phase2.sh

# Test liveness
curl http://localhost:8080/q/health/live

# Test readiness
curl http://localhost:8080/q/health/ready

# Test metrics
curl http://localhost:8080/q/metrics | grep amcp_
```

---

## 🎉 Summary

**Podman Testing is Ready!**

- ✅ Podman 4.3.1 installed
- ✅ Testing guide created
- ✅ Multiple options provided
- ✅ Troubleshooting included
- ✅ Complete workflow documented

**Next Steps**:
1. Follow the Quick Start guide above
2. Start Kafka containers with Podman
3. Start AMCP application
4. Run the test script
5. Verify all endpoints

**Status**: Ready for production testing with Podman
