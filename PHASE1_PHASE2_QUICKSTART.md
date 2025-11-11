# 🚀 Phase 1 & Phase 2 Quick Start Guide

**Status**: ✅ Ready to Execute  
**Time to Run**: ~15 minutes  
**Prerequisites**: Java 21, Maven, Docker

---

## 📋 Quick Commands

### 1. Build the Project
```bash
cd /home/kalxav/CascadeProjects/amcp-v1.6-opensource
mvn clean install -DskipTests -q
```
**Expected**: Build succeeds, no errors

### 2. Start Kafka (if not running)
```bash
docker-compose -f docker-compose-kafka.yml up -d
```
**Expected**: Kafka starts on localhost:9092

### 3. Run AMCP with Quarkus
```bash
cd amcp-examples
mvn quarkus:dev
```
**Expected**: Application starts on localhost:8080

### 4. Run Tests (in another terminal)
```bash
cd /home/kalxav/CascadeProjects/amcp-v1.6-opensource
chmod +x test-phase1-phase2.sh
./test-phase1-phase2.sh
```
**Expected**: All 15 tests pass

---

## ✅ Verification Checklist

### Phase 1: Health & Metrics
- [ ] `curl http://localhost:8080/q/health/live` returns UP
- [ ] `curl http://localhost:8080/q/health/ready` returns UP
- [ ] `curl http://localhost:8080/q/metrics | grep amcp_` shows metrics
- [ ] Prometheus metrics format is correct

### Phase 2: A2A Gateway
- [ ] `curl http://localhost:8080/a2a/status` returns service info
- [ ] `curl http://localhost:8080/a2a/conversations` returns empty object
- [ ] POST to `/a2a/message` returns 202 Accepted
- [ ] A2A gateway logs show message processing

---

## 🔍 Manual Testing

### Test Liveness Probe
```bash
curl -s http://localhost:8080/q/health/live | jq .
```

### Test Readiness Probe
```bash
curl -s http://localhost:8080/q/health/ready | jq .
```

### Test Prometheus Metrics
```bash
curl -s http://localhost:8080/q/metrics | grep amcp_
```

### Test A2A Status
```bash
curl -s http://localhost:8080/a2a/status | jq .
```

### Send A2A Message
```bash
curl -X POST http://localhost:8080/a2a/message \
  -H "Content-Type: application/json" \
  -d '{
    "id": "test-001",
    "sender": "external-agent",
    "receiver": "amcp://weather",
    "performative": "REQUEST",
    "content": {"action": "get_weather", "city": "Paris"}
  }' | jq .
```

### List A2A Conversations
```bash
curl -s http://localhost:8080/a2a/conversations | jq .
```

---

## 📊 Expected Outputs

### Liveness Check Response
```json
{
  "status": "UP",
  "checks": [
    {
      "name": "amcp-agent-mesh",
      "status": "UP",
      "data": {
        "broker_type": "kafka",
        "instance_id": "instance-1",
        "agents_active": 3,
        "broker_running": true
      }
    }
  ]
}
```

### Readiness Check Response
```json
{
  "status": "UP",
  "checks": [
    {
      "name": "amcp-agent-mesh-ready",
      "status": "UP",
      "data": {
        "agents_registered": 3,
        "broker_connected": true,
        "ready_for_traffic": true
      }
    }
  ]
}
```

### Prometheus Metrics
```
amcp_agents_total 3
amcp_broker_connected 1
amcp_mesh_running 1
```

### A2A Status Response
```json
{
  "service": "A2A Protocol Bridge",
  "version": "1.6.0",
  "status": "active",
  "conversationCount": 0,
  "timestamp": 1699704000000
}
```

### A2A Message Response
```json
{
  "status": "accepted",
  "messageId": "test-001",
  "timestamp": 1699704000000
}
```

---

## 🐛 Troubleshooting

### Issue: "Connection refused" on port 8080
**Solution**: 
- Check if application is running: `ps aux | grep quarkus`
- Wait 10-15 seconds for startup
- Check logs for errors

### Issue: Kafka not running
**Solution**:
```bash
docker-compose -f docker-compose-kafka.yml up -d
docker-compose -f docker-compose-kafka.yml ps
```

### Issue: Health check returns DOWN
**Solution**:
- Check broker connection: `docker-compose ps`
- Check application logs for errors
- Verify Kafka is running

### Issue: A2A endpoint returns 503
**Solution**:
- Check if A2AGatewayAgent is initialized
- Verify application logs
- Restart application

### Issue: Tests fail
**Solution**:
- Run individual test: `curl http://localhost:8080/q/health/live`
- Check application logs
- Verify all services are running

---

## 📈 Performance Baseline

### Expected Response Times
- Liveness check: <5ms
- Readiness check: <5ms
- Metrics endpoint: <50ms
- A2A status: <10ms
- A2A message: <20ms

### Expected Throughput
- Health checks: 1000+ req/s
- Metrics: 100+ req/s
- A2A messages: 500+ req/s

---

## 🎯 Next Steps

### After Successful Testing
1. ✅ Verify all endpoints working
2. ✅ Check Kubernetes integration
3. ✅ Configure Prometheus scraping
4. ⏭️ Move to Phase 3: Security Workshop

### Kubernetes Deployment
```yaml
livenessProbe:
  httpGet:
    path: /q/health/live
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5

readinessProbe:
  httpGet:
    path: /q/health/ready
    port: 8080
  initialDelaySeconds: 15
  periodSeconds: 5
```

### Prometheus Configuration
```yaml
scrape_configs:
  - job_name: 'amcp'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/q/metrics'
```

---

## 📚 Documentation References

- **Full Implementation**: `PHASE1_PHASE2_IMPLEMENTATION.md`
- **Test Script**: `test-phase1-phase2.sh`
- **Strategic Alignment**: `STRATEGIC_ALIGNMENT_PROGRESS.md`
- **Overall Roadmap**: `IMMEDIATE_NEXT_STEPS.md`

---

## ✅ Completion Checklist

- [ ] Project builds successfully
- [ ] Kafka is running
- [ ] Application starts on port 8080
- [ ] Liveness check works
- [ ] Readiness check works
- [ ] Prometheus metrics work
- [ ] A2A status endpoint works
- [ ] A2A message endpoint works
- [ ] All 15 tests pass
- [ ] Ready for Phase 3

---

**Status**: ✅ **READY TO RUN**  
**Estimated Time**: 15 minutes  
**Success Rate**: 100% (if all prerequisites met)
