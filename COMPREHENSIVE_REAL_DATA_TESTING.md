# 🧪 Comprehensive Real Data Testing - WeatherAgent & StockAgent

**Date**: November 10, 2025  
**Status**: ✅ **READY FOR EXECUTION**

---

## 📋 Overview

This document provides comprehensive testing procedures for **WeatherAgentReal** and **StockAgentReal** with real-time data from:
- **OpenWeatherMap API** for weather data
- **Alpha Vantage API** for stock data

---

## 🔑 API Keys Setup

### Option 1: Using Free Demo Keys (Limited)

No setup required - agents will use demo keys with limited requests:

```bash
# Demo mode (no API keys needed)
mvn quarkus:dev
```

### Option 2: Using Your Own API Keys (Recommended)

#### OpenWeatherMap API

1. **Get Free API Key**:
   - Visit: https://openweathermap.org/api
   - Sign up for free account
   - Navigate to API Keys section
   - Copy your API key

2. **Set Environment Variable**:
   ```bash
   export OPENWEATHER_API_KEY="your_api_key_here"
   ```

#### Alpha Vantage API

1. **Get Free API Key**:
   - Visit: https://www.alphavantage.co/
   - Sign up for free account
   - Copy your API key from dashboard

2. **Set Environment Variable**:
   ```bash
   export ALPHA_VANTAGE_API_KEY="your_api_key_here"
   ```

---

## 🚀 Quick Start Testing

### Step 1: Build Project

```bash
cd /home/kalxav/CascadeProjects/amcp-v1.6-opensource
mvn clean install -DskipTests -q
```

**Expected Output**: ✅ BUILD SUCCESS

### Step 2: Start Quarkus Instance

```bash
cd amcp-examples

# With demo keys (limited)
mvn quarkus:dev

# Or with your API keys
OPENWEATHER_API_KEY="your_key" ALPHA_VANTAGE_API_KEY="your_key" mvn quarkus:dev
```

**Expected Output**:
```
__  ____  __  _____   ___  __ ____  ______
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/
 -/ /_/ / /_/ / __ |/ ____/ ,< / /_/ /\ \
--\___\_\____/_/ |_/_/   /_/|_|\____/___/
2025-11-10 21:30:00 INFO  [io.quarkus] Quarkus started
```

---

## 🌤️ Weather Agent Testing

### Test 1: Get Current Weather

```bash
curl -X POST http://localhost:8080/weather/request \
  -H "Content-Type: application/json" \
  -d '{"city": "Paris"}'
```

**Expected Response** (Real Data):
```json
{
  "city": "Paris",
  "country": "FR",
  "temperature": 12.5,
  "feelsLike": 11.8,
  "tempMin": 10.2,
  "tempMax": 14.3,
  "humidity": 72,
  "pressure": 1013,
  "condition": "Clouds",
  "description": "overcast clouds",
  "windSpeed": 3.5,
  "windDeg": 240,
  "cloudiness": 90,
  "visibility": 10000,
  "lat": 48.8566,
  "lon": 2.3522,
  "timestamp": 1731256800000,
  "source": "weather-agent-real",
  "dataSource": "openweathermap",
  "real": true
}
```

### Test 2: Get Weather for Multiple Cities

```bash
# London
curl -X POST http://localhost:8080/weather/request \
  -H "Content-Type: application/json" \
  -d '{"city": "London"}'

# Tokyo
curl -X POST http://localhost:8080/weather/request \
  -H "Content-Type: application/json" \
  -d '{"city": "Tokyo"}'

# New York
curl -X POST http://localhost:8080/weather/request \
  -H "Content-Type: application/json" \
  -d '{"city": "New York"}'
```

### Test 3: Get Weather Forecast

```bash
curl -X POST http://localhost:8080/weather/forecast \
  -H "Content-Type: application/json" \
  -d '{"city": "Paris"}'
```

**Expected Response**:
```json
{
  "city": "Paris",
  "today": {
    "temperature": 12.5,
    "condition": "Clouds",
    "humidity": 72
  },
  "tomorrow": {
    "temperature": 13.5,
    "condition": "Sunny",
    "humidity": 60
  },
  "dayAfter": {
    "temperature": 11.5,
    "condition": "Rainy",
    "humidity": 75
  }
}
```

### Test 4: Check Weather Agent Status

```bash
curl http://localhost:8080/weather/status
```

**Expected Response**:
```json
{
  "agent": "WeatherAgentReal",
  "version": "1.6.0",
  "status": "active",
  "timestamp": 1731256800000,
  "apiKey": "configured",
  "citiesSupported": ["paris", "london", "tokyo", "new york"]
}
```

---

## 📈 Stock Agent Testing

### Test 1: Get Current Stock Quote

```bash
curl -X POST http://localhost:8080/stock/request \
  -H "Content-Type: application/json" \
  -d '{"symbol": "AAPL"}'
```

**Expected Response** (Real Data):
```json
{
  "symbol": "AAPL",
  "price": 228.45,
  "change": 2.15,
  "changePercent": 0.95,
  "volume": 45000000,
  "timestamp": "2025-11-10",
  "real": true,
  "52WeekHigh": 285.5625,
  "52WeekLow": 171.3375,
  "timestamp": 1731256800000,
  "source": "stock-agent-real",
  "dataSource": "alphavantage"
}
```

### Test 2: Get Quotes for Multiple Stocks

```bash
# Google
curl -X POST http://localhost:8080/stock/request \
  -H "Content-Type: application/json" \
  -d '{"symbol": "GOOGL"}'

# Microsoft
curl -X POST http://localhost:8080/stock/request \
  -H "Content-Type: application/json" \
  -d '{"symbol": "MSFT"}'

# Tesla
curl -X POST http://localhost:8080/stock/request \
  -H "Content-Type: application/json" \
  -d '{"symbol": "TSLA"}'
```

### Test 3: Get Detailed Stock Quote

```bash
curl -X POST http://localhost:8080/stock/quote \
  -H "Content-Type: application/json" \
  -d '{"symbol": "AAPL"}'
```

**Expected Response**:
```json
{
  "symbol": "AAPL",
  "price": 228.45,
  "change": 2.15,
  "changePercent": 0.95,
  "volume": 45000000,
  "pe": 28.5,
  "eps": 6.85,
  "dividend": 0.92,
  "yield": 0.47,
  "52WeekHigh": 285.5625,
  "52WeekLow": 171.3375
}
```

### Test 4: Get Portfolio Analysis

```bash
curl -X POST http://localhost:8080/stock/portfolio \
  -H "Content-Type: application/json" \
  -d '{}'
```

**Expected Response**:
```json
{
  "stocks": ["AAPL", "GOOGL", "MSFT", "TSLA"],
  "count": 4,
  "totalValue": 989.65,
  "totalChange": 1.5,
  "timestamp": 1731256800000
}
```

### Test 5: Check Stock Agent Status

```bash
curl http://localhost:8080/stock/status
```

**Expected Response**:
```json
{
  "agent": "StockAgentReal",
  "version": "1.6.0",
  "status": "active",
  "timestamp": 1731256800000,
  "apiKey": "configured",
  "stocksSupported": ["AAPL", "GOOGL", "MSFT", "TSLA"],
  "dataSource": "alphavantage"
}
```

---

## 🔄 Multi-Instance Testing

### Start 3 Instances with Kafka

**Terminal 1 - Instance 1 (Port 8080)**:
```bash
cd amcp-examples
AMCP_BROKER_TYPE=kafka AMCP_INSTANCE_ID=instance-1 \
OPENWEATHER_API_KEY="your_key" ALPHA_VANTAGE_API_KEY="your_key" \
mvn quarkus:dev -Dquarkus.http.port=8080
```

**Terminal 2 - Instance 2 (Port 8081)**:
```bash
cd amcp-examples
AMCP_BROKER_TYPE=kafka AMCP_INSTANCE_ID=instance-2 \
OPENWEATHER_API_KEY="your_key" ALPHA_VANTAGE_API_KEY="your_key" \
mvn quarkus:dev -Dquarkus.http.port=8081
```

**Terminal 3 - Instance 3 (Port 8082)**:
```bash
cd amcp-examples
AMCP_BROKER_TYPE=kafka AMCP_INSTANCE_ID=instance-3 \
OPENWEATHER_API_KEY="your_key" ALPHA_VANTAGE_API_KEY="your_key" \
mvn quarkus:dev -Dquarkus.http.port=8082
```

### Test Cross-Instance Communication

**Terminal 4 - Test Script**:
```bash
#!/bin/bash

echo "=== Testing Weather Agent Across Instances ==="

for i in 1 2 3; do
  port=$((8079 + i))
  echo "Instance $i (Port $port):"
  
  curl -s -X POST http://localhost:$port/weather/request \
    -H "Content-Type: application/json" \
    -d '{"city": "Paris"}' | jq '.city, .temperature, .condition'
  
  echo ""
done

echo "=== Testing Stock Agent Across Instances ==="

for i in 1 2 3; do
  port=$((8079 + i))
  echo "Instance $i (Port $port):"
  
  curl -s -X POST http://localhost:$port/stock/request \
    -H "Content-Type: application/json" \
    -d '{"symbol": "AAPL"}' | jq '.symbol, .price, .change'
  
  echo ""
done
```

---

## 📊 Kafka Monitoring

### Monitor Weather Events

```bash
sudo docker exec amcp-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic weather.request \
  --from-beginning
```

### Monitor Stock Events

```bash
sudo docker exec amcp-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic stock.request \
  --from-beginning
```

### Monitor Response Events

```bash
sudo docker exec amcp-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic weather.response \
  --from-beginning

sudo docker exec amcp-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic stock.response \
  --from-beginning
```

### List All Topics

```bash
sudo docker exec amcp-kafka kafka-topics \
  --list --bootstrap-server localhost:9092
```

### Check Consumer Groups

```bash
sudo docker exec amcp-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --list
```

---

## 🧪 Automated Test Script

Create `test-real-data.sh`:

```bash
#!/bin/bash

set -e

BASE_URL="http://localhost:8080"
RESULTS_FILE="test-results-$(date +%s).json"

echo "🧪 Starting Comprehensive Real Data Testing..."
echo ""

# Test Weather Agent
echo "📍 Testing Weather Agent..."
echo "Testing Paris weather..."
curl -s -X POST $BASE_URL/weather/request \
  -H "Content-Type: application/json" \
  -d '{"city": "Paris"}' | jq '.' > /tmp/weather_paris.json

echo "Testing London weather..."
curl -s -X POST $BASE_URL/weather/request \
  -H "Content-Type: application/json" \
  -d '{"city": "London"}' | jq '.' > /tmp/weather_london.json

echo "Testing Tokyo weather..."
curl -s -X POST $BASE_URL/weather/request \
  -H "Content-Type: application/json" \
  -d '{"city": "Tokyo"}' | jq '.' > /tmp/weather_tokyo.json

echo "Testing New York weather..."
curl -s -X POST $BASE_URL/weather/request \
  -H "Content-Type: application/json" \
  -d '{"city": "New York"}' | jq '.' > /tmp/weather_newyork.json

# Test Stock Agent
echo "📈 Testing Stock Agent..."
echo "Testing AAPL stock..."
curl -s -X POST $BASE_URL/stock/request \
  -H "Content-Type: application/json" \
  -d '{"symbol": "AAPL"}' | jq '.' > /tmp/stock_aapl.json

echo "Testing GOOGL stock..."
curl -s -X POST $BASE_URL/stock/request \
  -H "Content-Type: application/json" \
  -d '{"symbol": "GOOGL"}' | jq '.' > /tmp/stock_googl.json

echo "Testing MSFT stock..."
curl -s -X POST $BASE_URL/stock/request \
  -H "Content-Type: application/json" \
  -d '{"symbol": "MSFT"}' | jq '.' > /tmp/stock_msft.json

echo "Testing TSLA stock..."
curl -s -X POST $BASE_URL/stock/request \
  -H "Content-Type: application/json" \
  -d '{"symbol": "TSLA"}' | jq '.' > /tmp/stock_tsla.json

# Compile results
echo ""
echo "✅ All tests completed!"
echo "Results saved to: $RESULTS_FILE"

jq -s '{
  weather: {
    paris: input,
    london: input,
    tokyo: input,
    newyork: input
  },
  stocks: {
    aapl: input,
    googl: input,
    msft: input,
    tsla: input
  }
}' /tmp/weather_paris.json /tmp/weather_london.json /tmp/weather_tokyo.json /tmp/weather_newyork.json \
   /tmp/stock_aapl.json /tmp/stock_googl.json /tmp/stock_msft.json /tmp/stock_tsla.json > $RESULTS_FILE

echo ""
echo "📊 Test Summary:"
jq '.' $RESULTS_FILE
```

**Run the test script**:
```bash
chmod +x test-real-data.sh
./test-real-data.sh
```

---

## 📈 Performance Testing

### Load Testing with Apache Bench

```bash
# Test Weather Agent with 100 requests, 10 concurrent
ab -n 100 -c 10 -p weather-request.json \
  -T "application/json" \
  http://localhost:8080/weather/request

# Test Stock Agent with 100 requests, 10 concurrent
ab -n 100 -c 10 -p stock-request.json \
  -T "application/json" \
  http://localhost:8080/stock/request
```

### Load Testing with wrk

```bash
# Weather Agent
wrk -t4 -c100 -d30s \
  -s weather-load.lua \
  http://localhost:8080/weather/request

# Stock Agent
wrk -t4 -c100 -d30s \
  -s stock-load.lua \
  http://localhost:8080/stock/request
```

---

## ✅ Test Verification Checklist

### Weather Agent

- [ ] Real data fetched from OpenWeatherMap
- [ ] Fallback to simulated data when API unavailable
- [ ] All cities return valid data
- [ ] Forecast endpoint returns 3-day forecast
- [ ] Status endpoint shows correct API key status
- [ ] Multi-instance requests work across Kafka
- [ ] Response includes dataSource field
- [ ] Timestamps are accurate

### Stock Agent

- [ ] Real data fetched from Alpha Vantage
- [ ] Fallback to simulated data when API unavailable
- [ ] All stocks return valid quotes
- [ ] Detailed quote includes PE, EPS, dividend
- [ ] Portfolio analysis aggregates all stocks
- [ ] Status endpoint shows correct API key status
- [ ] Multi-instance requests work across Kafka
- [ ] Response includes dataSource field

### CloudEvents Compliance

- [ ] Events have unique IDs
- [ ] Events have timestamps
- [ ] Events have source URIs
- [ ] Events have type information
- [ ] Payloads are JSON
- [ ] Events route correctly through Kafka

### Performance

- [ ] Single request latency < 100ms
- [ ] Concurrent requests handled correctly
- [ ] No memory leaks under load
- [ ] Kafka consumer groups working
- [ ] Multi-instance load balancing working

---

## 🐛 Troubleshooting

### API Key Issues

**Problem**: "API key not found" or "Invalid API key"

**Solution**:
```bash
# Verify environment variable is set
echo $OPENWEATHER_API_KEY
echo $ALPHA_VANTAGE_API_KEY

# Set if missing
export OPENWEATHER_API_KEY="your_key"
export ALPHA_VANTAGE_API_KEY="your_key"

# Restart Quarkus
mvn quarkus:dev
```

### Timeout Issues

**Problem**: "Request timeout" or "Connection refused"

**Solution**:
```bash
# Check if APIs are accessible
curl -I https://api.openweathermap.org/data/2.5/weather
curl -I https://www.alphavantage.co/query

# Increase timeout in agents (currently 5 seconds)
# Edit WeatherAgentReal.java and StockAgentReal.java
```

### Kafka Connection Issues

**Problem**: "Failed to connect to Kafka"

**Solution**:
```bash
# Check Kafka is running
sudo docker ps | grep kafka

# Start Kafka if needed
sudo docker-compose up -d

# Verify Kafka connectivity
sudo docker exec amcp-kafka kafka-broker-api-versions \
  --bootstrap-server localhost:9092
```

### Fallback Data Issues

**Problem**: Always getting simulated data

**Solution**:
```bash
# Check logs for API errors
mvn quarkus:dev | grep -i "error\|warn"

# Verify API responses manually
curl "https://api.openweathermap.org/data/2.5/weather?q=Paris&appid=YOUR_KEY&units=metric"

curl "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=AAPL&apikey=YOUR_KEY"
```

---

## 📝 Summary

This comprehensive testing suite validates:

✅ **Real Data Integration**
- OpenWeatherMap API for weather
- Alpha Vantage API for stocks
- Fallback to simulated data

✅ **v1.6 Features**
- Quarkus CDI integration
- CloudEvents compliance
- Async processing
- Multi-instance support

✅ **Production Readiness**
- Error handling
- Logging
- Performance
- Reliability

---

**Status**: ✅ **READY FOR COMPREHENSIVE TESTING**

Start testing with your API keys for real-time weather and stock data! 🚀
