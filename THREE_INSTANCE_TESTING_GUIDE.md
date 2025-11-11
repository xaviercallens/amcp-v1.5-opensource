# 🧪 AMCP v1.6 - Three-Instance Testing Guide

**Version**: 1.6.0  
**Status**: ✅ Architecture Validated (1/3), Testing Ready  
**Date**: November 11, 2025

---

## 🎯 Overview

This guide validates the multi-instance distributed mesh architecture of AMCP v1.6 by running three instances of the Quarkus application, all communicating through a shared Kafka broker.

### Test Objective

Prove that multiple AMCP instances can:
1. **Share a Kafka mesh** - All instances connect to the same Kafka cluster
2. **Coordinate agents** - Agents across instances communicate seamlessly
3. **Load balance** - Events distribute across instances via consumer groups
4. **Scale horizontally** - Add/remove instances without downtime

---

## 📋 Prerequisites

### 1. Kafka Running
```bash
# Check if Kafka is running
docker ps | grep kafka

# If not, start Kafka with Docker Compose
cd /path/to/kafka
docker-compose up -d
```

### 2. Build AMCP
```bash
cd /home/kalxav/CascadeProjects/amcp-v1.6-opensource
mvn clean install -DskipTests -q
```

### 3. Prepare Terminals
You'll need **4 terminal windows**:
- Terminal 1: Instance 1 (port 8080)
- Terminal 2: Instance 2 (port 8081)
- Terminal 3: Instance 3 (port 8082)
- Terminal 4: Testing commands

---

## 🚀 Step-by-Step Testing

### Step 1: Start Instance 1

**Terminal 1**:
```bash
cd /home/kalxav/CascadeProjects/amcp-v1.6-opensource/amcp-examples

# Set environment variables
export AMCP_BROKER_TYPE=kafka
export AMCP_INSTANCE_ID=instance-1
export QUARKUS_HTTP_PORT=8080

# Start instance
mvn quarkus:dev -Dquarkus.http.port=8080
```

**Expected Output**:
```
__  ____  __  _____   ___  __ ____  ______
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
                                            
INFO  [io.qua.amc.run.AmcpRecorder] AMCP Agent Mesh initialized successfully
INFO  [io.qua.amc.run.AmcpRecorder] Broker Type: kafka
INFO  [io.qua.amc.run.AmcpRecorder] Agents Registered: 2
```

**Verify**:
```bash
# Check health (Terminal 4)
curl http://localhost:8080/q/health | jq .

# Check metrics
curl http://localhost:8080/q/metrics | grep amcp

# Check agent status
curl http://localhost:8080/agent/status | jq .
```

---

### Step 2: Start Instance 2

**Terminal 2**:
```bash
cd /home/kalxav/CascadeProjects/amcp-v1.6-opensource/amcp-examples

# Set environment variables
export AMCP_BROKER_TYPE=kafka
export AMCP_INSTANCE_ID=instance-2
export QUARKUS_HTTP_PORT=8081

# Start instance
mvn quarkus:dev -Dquarkus.http.port=8081
```

**Expected Output**:
```
INFO  [io.qua.amc.run.AmcpRecorder] AMCP Agent Mesh initialized successfully
INFO  [io.qua.amc.run.AmcpRecorder] Broker Type: kafka
INFO  [io.qua.amc.run.AmcpRecorder] Agents Registered: 2
```

**Verify**:
```bash
# Check health (Terminal 4)
curl http://localhost:8081/q/health | jq .

# Check metrics
curl http://localhost:8081/q/metrics | grep amcp
```

---

### Step 3: Start Instance 3

**Terminal 3**:
```bash
cd /home/kalxav/CascadeProjects/amcp-v1.6-opensource/amcp-examples

# Set environment variables
export AMCP_BROKER_TYPE=kafka
export AMCP_INSTANCE_ID=instance-3
export QUARKUS_HTTP_PORT=8082

# Start instance
mvn quarkus:dev -Dquarkus.http.port=8082
```

**Expected Output**:
```
INFO  [io.qua.amc.run.AmcpRecorder] AMCP Agent Mesh initialized successfully
INFO  [io.qua.amc.run.AmcpRecorder] Broker Type: kafka
INFO  [io.qua.amc.run.AmcpRecorder] Agents Registered: 2
```

**Verify**:
```bash
# Check health (Terminal 4)
curl http://localhost:8082/q/health | jq .

# Check all instances are running
for port in 8080 8081 8082; do
  echo "Instance on port $port:"
  curl -s http://localhost:$port/agent/status | jq '.running'
done
```

---

## 🧪 Multi-Instance Tests

### Test 1: Weather Agent Coordination

**Scenario**: Send weather request, observe which instance responds

```bash
# Send request to Instance 1
curl -X POST http://localhost:8080/weather/london | jq .

# Send request to Instance 2
curl -X POST http://localhost:8081/weather/paris | jq .

# Send request to Instance 3
curl -X POST http://localhost:8082/weather/tokyo | jq .
```

**Expected Behavior**:
- Each request should be handled successfully
- Logs in one or more terminals show weather agent processing
- Responses contain real weather data (or fallback)

---

### Test 2: Stock Agent Load Balancing

**Scenario**: Send multiple stock requests, verify load distribution

```bash
# Send 10 requests rapidly
for i in {1..10}; do
  curl -X POST http://localhost:8080/stock/AAPL &
done
wait

# Check logs in all 3 terminals
# You should see requests distributed across instances
```

**Expected Behavior**:
- Requests distributed across all 3 instances
- Kafka consumer groups ensure load balancing
- No duplicate processing (each request handled once)

---

### Test 3: Agent Discovery Across Instances

**Scenario**: Verify all instances see the same agents

```bash
# Query each instance for agent list
echo "Instance 1:"
curl -s http://localhost:8080/agent/list | jq '.agents'

echo "Instance 2:"
curl -s http://localhost:8081/agent/list | jq '.agents'

echo "Instance 3:"
curl -s http://localhost:8082/agent/list | jq '.agents'
```

**Expected Behavior**:
- All instances report same agent types
- Each instance has its own agent instances
- Agent names include instance ID

---

### Test 4: Health Check Aggregation

**Scenario**: Verify all instances report healthy status

```bash
# Check all health endpoints
for port in 8080 8081 8082; do
  echo "=== Instance on port $port ==="
  curl -s http://localhost:$port/q/health/live | jq '.'
  curl -s http://localhost:$port/q/health/ready | jq '.'
  echo ""
done
```

**Expected Output**:
```json
{
  "status": "UP",
  "checks": [
    {
      "name": "amcp-agent-mesh",
      "status": "UP",
      "data": {
        "broker_type": "kafka",
        "agent_count": 2,
        "context_id": "context-..."
      }
    }
  ]
}
```

---

### Test 5: Metrics Collection

**Scenario**: Verify Prometheus metrics across instances

```bash
# Collect metrics from all instances
for port in 8080 8081 8082; do
  echo "=== Metrics from port $port ==="
  curl -s http://localhost:$port/q/metrics | grep amcp_
  echo ""
done
```

**Expected Metrics**:
```
amcp_agents_total 2.0
amcp_broker_connected 1.0
amcp_mesh_running 1.0
```

---

### Test 6: Instance Failure Recovery

**Scenario**: Stop one instance, verify others continue

```bash
# Stop Instance 2 (Ctrl+C in Terminal 2)

# Verify Instances 1 and 3 still work
curl http://localhost:8080/weather/london | jq .
curl http://localhost:8082/weather/paris | jq .

# Restart Instance 2
# (Re-run Step 2 commands in Terminal 2)

# Verify it rejoins the mesh
curl http://localhost:8081/agent/status | jq .
```

**Expected Behavior**:
- Remaining instances continue processing
- No errors in active instances
- Restarted instance rejoins seamlessly
- Load redistributes automatically

---

## 📊 Validation Checklist

### ✅ Basic Connectivity
- [ ] All 3 instances start successfully
- [ ] All connect to Kafka (localhost:9092)
- [ ] Health checks return UP
- [ ] Metrics endpoints accessible

### ✅ Agent Coordination
- [ ] Weather agents respond on all instances
- [ ] Stock agents respond on all instances
- [ ] Agents communicate via Kafka topics
- [ ] No duplicate event processing

### ✅ Load Balancing
- [ ] Requests distribute across instances
- [ ] Consumer groups work correctly
- [ ] Each event processed exactly once
- [ ] Performance scales with instances

### ✅ Fault Tolerance
- [ ] Surviving instances continue on failure
- [ ] Failed instance can restart
- [ ] No data loss during failure
- [ ] Automatic load rebalancing

---

## 🔧 Troubleshooting

### Issue: Instances won't start

**Symptoms**: Port already in use, connection refused

**Solutions**:
```bash
# Check if ports are available
lsof -i :8080
lsof -i :8081
lsof -i :8082

# Kill existing processes
kill -9 $(lsof -t -i:8080)

# Or use different ports
export QUARKUS_HTTP_PORT=9080
```

---

### Issue: Kafka connection fails

**Symptoms**: "Failed to connect to broker"

**Solutions**:
```bash
# Verify Kafka is running
docker ps | grep kafka

# Test Kafka connection
kafka-console-consumer --bootstrap-server localhost:9092 --topic test --from-beginning

# Check firewall
sudo ufw status
```

---

### Issue: Agents not discovered

**Symptoms**: `agent_count: 0` in health check

**Solutions**:
```bash
# Rebuild with clean
mvn clean install -DskipTests

# Check agent classes are in classpath
find target -name "*Agent.class"

# Verify agent annotations
grep -r "@ApplicationScoped" amcp-examples/src/
```

---

### Issue: Events not distributed

**Symptoms**: Only one instance processes events

**Solutions**:
```bash
# Check Kafka consumer groups
kafka-consumer-groups --bootstrap-server localhost:9092 --list

# Verify instance IDs are unique
echo $AMCP_INSTANCE_ID

# Check topic subscriptions
kafka-topics --bootstrap-server localhost:9092 --list
```

---

## 📈 Performance Validation

### Expected Metrics (3 Instances)

| Metric | Single Instance | 3 Instances | Improvement |
|--------|----------------|-------------|-------------|
| **Throughput** | 100 req/sec | 300 req/sec | 3x |
| **Latency (p50)** | 50ms | 50ms | Same |
| **Latency (p99)** | 200ms | 180ms | 10% better |
| **Memory Total** | 1GB | 3GB | Linear |
| **CPU Usage** | 20% (1 core) | 20% (3 cores) | Parallel |

### Load Test

```bash
# Install Apache Bench
sudo apt-get install apache2-utils

# Run load test (100 requests, 10 concurrent)
ab -n 100 -c 10 http://localhost:8080/weather/london

# Compare with 3 instances
# Requests should complete 3x faster with same concurrency
```

---

## 🎯 Success Criteria

### ✅ Test Passed If:

1. **All 3 instances start** without errors
2. **Health checks pass** on all instances
3. **Metrics show** `amcp_broker_connected = 1`
4. **Agent count matches** across instances
5. **Load distributes** evenly
6. **Fault tolerance works** (2/3 instances survive failure)
7. **No duplicate processing** of events
8. **Performance scales** linearly

### ❌ Test Failed If:

- Any instance fails to start
- Health checks return DOWN
- Broker connection fails
- Events not distributed
- Duplicate event processing
- Instance cannot rejoin after restart

---

## 📝 Test Report Template

```markdown
# 3-Instance Test Report

**Date**: [Date]  
**Tester**: [Name]  
**AMCP Version**: 1.6.0

## Environment
- **Kafka**: [Version]
- **Java**: [Version]
- **OS**: [OS]

## Results

### Instance 1
- Port: 8080
- Status: [UP/DOWN]
- Agents: [Count]
- Issues: [None/Details]

### Instance 2
- Port: 8081
- Status: [UP/DOWN]
- Agents: [Count]
- Issues: [None/Details]

### Instance 3
- Port: 8082
- Status: [UP/DOWN]
- Agents: [Count]
- Issues: [None/Details]

## Tests Executed
- [ ] Basic Connectivity
- [ ] Agent Coordination
- [ ] Load Balancing
- [ ] Fault Tolerance
- [ ] Performance

## Overall Result
**PASS** / **FAIL**

## Notes
[Additional observations]
```

---

## 🚀 Next Steps

After successful 3-instance testing:

1. **Update Status**: Mark as ✅ in `QUARKUS_EXTENSION_IMPLEMENTATION_STATUS.md`
2. **Kubernetes Testing**: Deploy to Kubernetes with 3 pods
3. **Production Deployment**: Scale to 5+ instances
4. **Monitoring**: Set up Grafana dashboards for metrics
5. **Documentation**: Update deployment guides

---

**Status**: ✅ **READY FOR TESTING**

This guide provides complete instructions for validating the multi-instance architecture of AMCP v1.6!
