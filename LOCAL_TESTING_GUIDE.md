# 🧪 AMCP v1.6 - Local Testing Guide

**Date**: November 10, 2025  
**Status**: ✅ **READY FOR TESTING**  
**Framework**: Red Hat Quarkus  
**Agents**: Weather & Stock with Real Data

---

## 📋 Prerequisites

### System Requirements
- Linux (Ubuntu 22.04+, RHEL 8+)
- Java 21+
- Maven 3.8+
- curl (for API testing)
- jq (for JSON parsing)

### Installation

```bash
# Install Java 21
sudo apt-get install -y openjdk-21-jdk

# Install Maven
sudo apt-get install -y maven

# Install curl and jq
sudo apt-get install -y curl jq

# Verify installations
java --version
mvn --version
curl --version
jq --version
```

---

## 🚀 Quick Start

### Step 1: Build the Application

```bash
cd /home/kalxav/CascadeProjects/amcp-v1.6-opensource

# Build with Maven
mvn clean install -DskipTests -q

# Expected output: BUILD SUCCESS
```

### Step 2: Start the Application

**Terminal 1** - Start Quarkus Application:
```bash
cd amcp-examples
mvn quarkus:dev -Dquarkus.http.port=8080

# Expected output:
# __  ____  __  _____   ___  __ ____  ______
# --/ __ \/ / / / __ \/ __ \/ / __ \/ ____/
# -/ /_/ / /_/ / /_/ / /_/ / / /_/ / __/
# / _, _/ __  / ____/ ____/ / _, _/ /___
# /_/ |_/_/ /_/_/   /_/   /_/ |_/_____/
# 2025-11-10 22:25:00,000 INFO  [io.quarkus] (main) AMCP v1.6 started
# 2025-11-10 22:25:00,100 INFO  [io.quarkus] (main) Listening on: http://0.0.0.0:8080
```

### Step 3: Run Tests

**Terminal 2** - Run Comprehensive Tests:
```bash
cd /home/kalxav/CascadeProjects/amcp-v1.6-opensource

# Run all tests (44 tests total)
./test-agents-local.sh

# Or run individual tests
curl http://localhost:8080/weather/london
curl http://localhost:8080/stock/AAPL
```

---

## 🌍 Weather Agent Tests

### Test Coverage

| Test | Endpoint | Data Source |
|------|----------|-------------|
| **10 Global Cities** | `/weather/{city}` | OpenWeatherMap API |
| **3-Day Forecasts** | `/weather/{city}/forecast` | OpenWeatherMap API |
| **Agent Status** | `/weather/status` | Local |

### Cities Tested

```
1. London (GB)      - Financial hub
2. Paris (FR)       - European capital
3. Tokyo (JP)       - Asian metropolis
4. New York (US)    - American center
5. Sydney (AU)      - Southern hemisphere
6. Dubai (AE)       - Middle East hub
7. Singapore (SG)   - Southeast Asia
8. Hong Kong (HK)   - Asia-Pacific
9. Bangkok (TH)     - Southeast capital
10. Mumbai (IN)     - South Asian hub
```

### Sample Test Commands

```bash
# Get weather for London
curl -X GET http://localhost:8080/weather/london \
  -H "Content-Type: application/json" | jq .

# Get 3-day forecast for Tokyo
curl -X GET http://localhost:8080/weather/tokyo/forecast \
  -H "Content-Type: application/json" | jq .

# Get weather agent status
curl -X GET http://localhost:8080/weather/status \
  -H "Content-Type: application/json" | jq .
```

### Sample Response

```json
{
  "city": "London",
  "country": "GB",
  "temperature": 12.5,
  "feelsLike": 11.8,
  "humidity": 72,
  "pressure": 1013,
  "condition": "Clouds",
  "description": "overcast clouds",
  "windSpeed": 3.5,
  "windDirection": 230,
  "cloudCoverage": 90,
  "visibility": 10000,
  "uvIndex": 0.5,
  "dataSource": "openweathermap (real)",
  "timestamp": 1731256800000
}
```

---

## 📈 Stock Agent Tests

### Test Coverage

| Test | Endpoint | Data Source |
|------|----------|-------------|
| **10 Global Stocks** | `/stock/{symbol}` | Polygon.io API |
| **Detailed Analysis** | `/stock/{symbol}/details` | Polygon.io API |
| **Portfolio** | `/stock/portfolio` | Aggregated |
| **Agent Status** | `/stock/status` | Local |

### Stocks Tested

```
1. AAPL   - Apple Inc.
2. MSFT   - Microsoft Corporation
3. GOOGL  - Alphabet Inc. (Google)
4. AMZN   - Amazon.com Inc.
5. TSLA   - Tesla Inc.
6. META   - Meta Platforms Inc.
7. NVDA   - NVIDIA Corporation
8. JPM    - JPMorgan Chase & Co.
9. V      - Visa Inc.
10. WMT   - Walmart Inc.
```

### Sample Test Commands

```bash
# Get stock quote for AAPL
curl -X GET http://localhost:8080/stock/AAPL \
  -H "Content-Type: application/json" | jq .

# Get detailed analysis for MSFT
curl -X GET http://localhost:8080/stock/MSFT/details \
  -H "Content-Type: application/json" | jq .

# Get portfolio analysis
curl -X GET http://localhost:8080/stock/portfolio \
  -H "Content-Type: application/json" | jq .

# Get stock agent status
curl -X GET http://localhost:8080/stock/status \
  -H "Content-Type: application/json" | jq .
```

### Sample Response

```json
{
  "symbol": "AAPL",
  "price": 228.45,
  "bid": 228.40,
  "ask": 228.50,
  "bidSize": 1500,
  "askSize": 2000,
  "lastTrade": 228.45,
  "lastTradeTime": 1731256800000,
  "dayChange": 2.45,
  "dayChangePercent": 1.08,
  "52WeekHigh": 285.5625,
  "52WeekLow": 171.3375,
  "marketCap": 3200000000000,
  "peRatio": 28.5,
  "eps": 8.05,
  "dividend": 0.24,
  "volume": 50000000,
  "avgVolume": 52000000,
  "dataSource": "polygon.io (real)",
  "timestamp": 1731256800000
}
```

---

## 🧪 Batch & Stress Tests

### Weather Batch Test

```bash
# 5 rapid weather requests
for i in {1..5}; do
  curl -s http://localhost:8080/weather/london | jq .temperature
done

# Expected: All requests complete in <500ms
```

### Stock Batch Test

```bash
# 5 rapid stock requests
for i in {1..5}; do
  curl -s http://localhost:8080/stock/AAPL | jq .price
done

# Expected: All requests complete in <450ms
```

### Concurrent Requests

```bash
# 10 concurrent requests
for i in {1..10}; do
  curl -s http://localhost:8080/weather/london &
done
wait

# Expected: All requests complete successfully
```

---

## 📊 Health & Metrics Tests

### Health Checks

```bash
# Liveness probe
curl http://localhost:8080/q/health/live | jq .

# Readiness probe
curl http://localhost:8080/q/health/ready | jq .

# Startup probe
curl http://localhost:8080/q/health/started | jq .

# Full health
curl http://localhost:8080/q/health | jq .
```

### Metrics

```bash
# Get all metrics
curl http://localhost:8080/q/metrics | head -50

# Get specific metrics
curl http://localhost:8080/q/metrics | grep "http_server_requests"
curl http://localhost:8080/q/metrics | grep "jvm_memory"
curl http://localhost:8080/q/metrics | grep "amcp_agents"
```

---

## 🧬 Running the Comprehensive Test Suite

### Automated Testing

```bash
# Run all 44 tests
./test-agents-local.sh

# Expected output:
# ✓ Test 1: Weather - London ... PASSED
# ✓ Test 2: Weather - Paris ... PASSED
# ...
# ✓ Test 44: Metrics Endpoint ... PASSED
#
# Total Tests Run:    44
# Tests Passed:       44
# Tests Failed:       0
```

### Manual Testing

```bash
# Test weather agent
echo "Testing Weather Agent..."
curl -s http://localhost:8080/weather/london | jq .
curl -s http://localhost:8080/weather/tokyo | jq .
curl -s http://localhost:8080/weather/newyork | jq .

# Test stock agent
echo "Testing Stock Agent..."
curl -s http://localhost:8080/stock/AAPL | jq .
curl -s http://localhost:8080/stock/MSFT | jq .
curl -s http://localhost:8080/stock/GOOGL | jq .

# Test health
echo "Testing Health..."
curl -s http://localhost:8080/q/health | jq .
```

---

## 📈 Performance Metrics

### Expected Performance

| Metric | Value | Notes |
|--------|-------|-------|
| **Response Time** | <50ms | Cached responses |
| **Throughput** | 100+ req/sec | Per instance |
| **Memory Usage** | 256-512 MB | Per pod |
| **CPU Usage** | 100-250m | Per pod |
| **Availability** | 99.9% | With health checks |

### Monitoring Performance

```bash
# Monitor in real-time
watch -n 1 'curl -s http://localhost:8080/q/metrics | grep "http_server_requests"'

# Get response time statistics
curl -s http://localhost:8080/q/metrics | grep "http_server_requests_seconds"
```

---

## 🔍 Troubleshooting

### Issue: Connection Refused

```bash
# Check if server is running
curl http://localhost:8080/q/health

# If not running, start it:
cd amcp-examples && mvn quarkus:dev
```

### Issue: API Key Errors

```bash
# Check if API keys are configured
grep -r "3bd965f39881ba0f116ee0810fdfd058" amcp-examples/src/

# If not found, configure in:
# amcp-examples/src/main/java/io/amcp/examples/WeatherAgentConfigured.java
# amcp-examples/src/main/java/io/amcp/examples/StockAgentConfigured.java
```

### Issue: Timeout Errors

```bash
# Increase timeout in test script
# Change: TIMEOUT=10
# To: TIMEOUT=30

# Or check network connectivity
ping api.openweathermap.org
ping api.polygon.io
```

### Issue: JSON Parse Errors

```bash
# Install jq if not present
sudo apt-get install -y jq

# Test JSON parsing
curl -s http://localhost:8080/weather/london | jq .
```

---

## 📝 Test Results Template

```
AMCP v1.6 - Local Testing Results
==================================

Date: [DATE]
Time: [TIME]
Framework: Quarkus
Agents: Weather & Stock

Weather Agent Tests:
  ✓ 10 Global Cities: PASSED
  ✓ 3-Day Forecasts: PASSED
  ✓ Agent Status: PASSED

Stock Agent Tests:
  ✓ 10 Global Stocks: PASSED
  ✓ Detailed Analysis: PASSED
  ✓ Portfolio Analysis: PASSED
  ✓ Agent Status: PASSED

Batch & Stress Tests:
  ✓ Weather Batch (5 requests): PASSED
  ✓ Stock Batch (5 requests): PASSED
  ✓ Concurrent Requests: PASSED

Health & Metrics:
  ✓ Liveness Probe: PASSED
  ✓ Readiness Probe: PASSED
  ✓ Metrics Endpoint: PASSED

Total: 44 Tests
Passed: 44
Failed: 0
Success Rate: 100%

Performance:
  Response Time: <50ms
  Throughput: 100+ req/sec
  Memory: 256-512 MB
  CPU: 100-250m
```

---

## 🎯 Next Steps

1. **Run Tests**: Execute `./test-agents-local.sh`
2. **Monitor Logs**: Watch application logs in Terminal 1
3. **Check Metrics**: Visit `http://localhost:8080/q/metrics`
4. **Scale Testing**: Run multiple instances with Kafka
5. **Deploy**: Use Kubernetes deployment guide

---

## 📚 Additional Resources

- **Quarkus Docs**: https://quarkus.io/guides/
- **OpenWeatherMap API**: https://openweathermap.org/api
- **Polygon.io API**: https://polygon.io/docs/stocks
- **AMCP Documentation**: See `docs/` directory

---

**Status**: ✅ **READY FOR TESTING**

Start testing AMCP v1.6 Weather & Stock agents with real data!
