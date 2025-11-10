# 🚀 Real Data Testing - Quick Start Guide

**Status**: ✅ **READY TO TEST**

---

## 📦 What's New

Two new agents with **real-time data integration**:

| Agent | Data Source | API | Free Tier |
|-------|------------|-----|-----------|
| **WeatherAgentReal** | OpenWeatherMap | Weather API | ✅ Yes |
| **StockAgentReal** | Alpha Vantage | Stock API | ✅ Yes |

---

## ⚡ 5-Minute Setup

### Step 1: Get Free API Keys (Optional)

**OpenWeatherMap** (Weather):
```bash
# Visit: https://openweathermap.org/api
# Sign up → Get API key → Copy it
export OPENWEATHER_API_KEY="your_key_here"
```

**Alpha Vantage** (Stocks):
```bash
# Visit: https://www.alphavantage.co/
# Sign up → Get API key → Copy it
export ALPHA_VANTAGE_API_KEY="your_key_here"
```

### Step 2: Build Project

```bash
cd /home/kalxav/CascadeProjects/amcp-v1.6-opensource
mvn clean install -DskipTests -q
```

**Result**: ✅ BUILD SUCCESS

### Step 3: Start Quarkus

```bash
cd amcp-examples

# Option A: Demo mode (no API keys needed)
mvn quarkus:dev

# Option B: With your API keys
OPENWEATHER_API_KEY="your_key" ALPHA_VANTAGE_API_KEY="your_key" mvn quarkus:dev
```

**Wait for**:
```
Quarkus started in 2.5s
```

### Step 4: Test in Another Terminal

```bash
# Weather test
curl -X POST http://localhost:8080/weather/request \
  -H "Content-Type: application/json" \
  -d '{"city": "Paris"}'

# Stock test
curl -X POST http://localhost:8080/stock/request \
  -H "Content-Type: application/json" \
  -d '{"symbol": "AAPL"}'
```

**Done!** ✅ You're testing real data!

---

## 🧪 Automated Testing

### Run Full Test Suite

```bash
cd /home/kalxav/CascadeProjects/amcp-v1.6-opensource
./test-real-data.sh
```

**What it tests**:
- ✅ Weather for 4 cities (Paris, London, Tokyo, New York)
- ✅ Weather forecast
- ✅ Stock quotes for 4 symbols (AAPL, GOOGL, MSFT, TSLA)
- ✅ Detailed stock quotes
- ✅ Portfolio analysis
- ✅ Agent status endpoints

**Output**:
```
🧪 Starting Comprehensive Real Data Testing...
Base URL: http://localhost:8080
Results Directory: test-results-20251110-213000

Testing: Weather - Paris
✅ PASSED (HTTP 200)

Testing: Weather - London
✅ PASSED (HTTP 200)

...

Test Summary:
Passed: 13
Failed: 0

Results saved to: test-results-20251110-213000
```

---

## 📊 Sample Real Data Responses

### Weather Response (Real Data)

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

### Stock Response (Real Data)

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
  "source": "stock-agent-real",
  "dataSource": "alphavantage"
}
```

---

## 🔄 Multi-Instance Testing

### Start 3 Instances with Kafka

**Terminal 1**:
```bash
cd amcp-examples
AMCP_BROKER_TYPE=kafka AMCP_INSTANCE_ID=instance-1 \
OPENWEATHER_API_KEY="your_key" ALPHA_VANTAGE_API_KEY="your_key" \
mvn quarkus:dev -Dquarkus.http.port=8080
```

**Terminal 2**:
```bash
cd amcp-examples
AMCP_BROKER_TYPE=kafka AMCP_INSTANCE_ID=instance-2 \
OPENWEATHER_API_KEY="your_key" ALPHA_VANTAGE_API_KEY="your_key" \
mvn quarkus:dev -Dquarkus.http.port=8081
```

**Terminal 3**:
```bash
cd amcp-examples
AMCP_BROKER_TYPE=kafka AMCP_INSTANCE_ID=instance-3 \
OPENWEATHER_API_KEY="your_key" ALPHA_VANTAGE_API_KEY="your_key" \
mvn quarkus:dev -Dquarkus.http.port=8082
```

### Test Cross-Instance Communication

```bash
# Request to instance 1
curl -X POST http://localhost:8080/weather/request \
  -H "Content-Type: application/json" \
  -d '{"city": "Paris"}'

# Request to instance 2
curl -X POST http://localhost:8081/weather/request \
  -H "Content-Type: application/json" \
  -d '{"city": "London"}'

# Request to instance 3
curl -X POST http://localhost:8082/weather/request \
  -H "Content-Type: application/json" \
  -d '{"city": "Tokyo"}'
```

---

## 🎯 Key Features Tested

### Weather Agent Real

✅ **Real Data from OpenWeatherMap**
- Current temperature, humidity, wind speed
- Weather conditions and descriptions
- 52-week high/low estimates
- Coordinates (latitude/longitude)

✅ **Fallback to Simulated Data**
- If API unavailable
- If API key invalid
- If network error

✅ **Multi-City Support**
- Paris, London, Tokyo, New York
- Any city name (OpenWeatherMap supports worldwide)

### Stock Agent Real

✅ **Real Data from Alpha Vantage**
- Current stock price
- Daily change and change percent
- Trading volume
- 52-week high/low

✅ **Fallback to Simulated Data**
- If API unavailable
- If API key invalid
- If network error

✅ **Multi-Stock Support**
- AAPL, GOOGL, MSFT, TSLA
- Any stock symbol

---

## 📈 Expected Performance

| Metric | Value |
|--------|-------|
| **Single Request Latency** | <100ms |
| **Concurrent Requests** | 10+ |
| **Memory per Instance** | ~500MB |
| **Startup Time** | <3s |
| **API Timeout** | 5 seconds |

---

## 🐛 Troubleshooting

### "API key not found"

```bash
# Check if environment variable is set
echo $OPENWEATHER_API_KEY
echo $ALPHA_VANTAGE_API_KEY

# If empty, set them
export OPENWEATHER_API_KEY="your_key"
export ALPHA_VANTAGE_API_KEY="your_key"

# Restart Quarkus
mvn quarkus:dev
```

### "Request timeout"

```bash
# Check API is accessible
curl -I https://api.openweathermap.org/data/2.5/weather
curl -I https://www.alphavantage.co/query

# If not accessible, check internet connection
# Agents will automatically fallback to simulated data
```

### "Always getting simulated data"

```bash
# Check logs for API errors
mvn quarkus:dev | grep -i "error\|warn"

# Test API manually
curl "https://api.openweathermap.org/data/2.5/weather?q=Paris&appid=YOUR_KEY&units=metric"

curl "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=AAPL&apikey=YOUR_KEY"
```

### "Kafka connection failed"

```bash
# Check Kafka is running
sudo docker ps | grep kafka

# Start Kafka if needed
sudo docker-compose up -d

# Verify connectivity
sudo docker exec amcp-kafka kafka-broker-api-versions \
  --bootstrap-server localhost:9092
```

---

## 📋 Test Checklist

- [ ] Build successful (`mvn clean install`)
- [ ] Quarkus starts without errors
- [ ] Weather request returns data
- [ ] Stock request returns data
- [ ] Responses include `dataSource` field
- [ ] Responses include `timestamp` field
- [ ] Multi-city weather works
- [ ] Multi-stock quotes work
- [ ] Status endpoints respond
- [ ] Automated test script passes

---

## 🔗 Documentation

For comprehensive details, see:
- **COMPREHENSIVE_REAL_DATA_TESTING.md** - Full testing guide
- **WEATHER_STOCK_AGENTS_V1.6.md** - Architecture details
- **test-real-data.sh** - Automated test script

---

## 🎓 What You're Testing

✅ **v1.6 Features**:
- Quarkus CDI integration
- CloudEvents v1.0 compliance
- Async event processing
- Multi-instance Kafka support

✅ **Real Data Integration**:
- OpenWeatherMap API
- Alpha Vantage API
- Fallback mechanisms
- Error handling

✅ **Production Readiness**:
- Structured logging
- Health checks
- Performance monitoring
- Distributed mesh

---

## 🚀 Next Steps

1. **Get API Keys** (optional but recommended)
   - OpenWeatherMap: https://openweathermap.org/api
   - Alpha Vantage: https://www.alphavantage.co/

2. **Run Tests**
   ```bash
   ./test-real-data.sh
   ```

3. **Monitor Results**
   ```bash
   cat test-results-*/SUMMARY.txt
   ```

4. **Deploy Multi-Instance**
   - Start 3 Quarkus instances with Kafka
   - Test cross-instance communication
   - Monitor Kafka topics

---

**Status**: ✅ **READY FOR COMPREHENSIVE TESTING**

Start testing real weather and stock data now! 🌤️📈
