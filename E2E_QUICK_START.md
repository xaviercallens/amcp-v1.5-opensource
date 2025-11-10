# 🚀 End-to-End Testing - Quick Start

**Status**: ✅ **READY TO RUN**

---

## ⚡ 3-Step Quick Start

### Step 1: Start Server (Terminal 1)
```bash
cd /home/kalxav/CascadeProjects/amcp-v1.6-opensource/amcp-examples
mvn quarkus:dev -Dquarkus.http.port=8080
```

**Wait for**: `Quarkus started in X.XXXs`

### Step 2: Run Tests (Terminal 2)
```bash
cd /home/kalxav/CascadeProjects/amcp-v1.6-opensource
./end-to-end-test.sh
```

### Step 3: View Results
```bash
# Summary
cat e2e-test-results-*/SUMMARY.txt

# Individual results
ls -la e2e-test-results-*/
```

---

## 📊 What Gets Tested

### Weather (13 tests)
```
✅ Top 10 Cities:
   London, Paris, Tokyo, New York, Sydney, 
   Dubai, Singapore, Hong Kong, Bangkok, Mumbai

✅ Forecasts:
   3-day forecasts for London, Tokyo, New York

✅ Status:
   Agent health check
```

### Stocks (17 tests)
```
✅ Top 10 Stocks:
   AAPL, MSFT, GOOGL, AMZN, TSLA,
   META, NVDA, JPM, V, WMT

✅ Detailed Quotes:
   PE ratio, EPS, dividends for top 5

✅ Portfolio:
   Aggregated analysis

✅ Status:
   Agent health check
```

### Batch Tests (10 tests)
```
✅ Weather Batch:
   5 rapid sequential requests

✅ Stock Batch:
   5 rapid sequential requests
```

---

## 🎯 Expected Results

```
╔════════════════════════════════════════════════════════════════╗
║           ✅ ALL END-TO-END TESTS PASSED! ✅                 ║
╚════════════════════════════════════════════════════════════════╝

Passed: 50
Failed: 0
Total: 50
Success Rate: 100%
```

---

## 🌍 Cities Tested (with codes)

| # | City | Country | Code |
|---|------|---------|------|
| 1 | London | UK | GB |
| 2 | Paris | France | FR |
| 3 | Tokyo | Japan | JP |
| 4 | New York | USA | US |
| 5 | Sydney | Australia | AU |
| 6 | Dubai | UAE | AE |
| 7 | Singapore | Singapore | SG |
| 8 | Hong Kong | Hong Kong | HK |
| 9 | Bangkok | Thailand | TH |
| 10 | Mumbai | India | IN |

---

## 📈 Stocks Tested (with symbols)

| # | Company | Symbol | Sector |
|---|---------|--------|--------|
| 1 | Apple | AAPL | Technology |
| 2 | Microsoft | MSFT | Technology |
| 3 | Alphabet | GOOGL | Technology |
| 4 | Amazon | AMZN | E-commerce |
| 5 | Tesla | TSLA | Automotive |
| 6 | Meta | META | Technology |
| 7 | NVIDIA | NVDA | Technology |
| 8 | JPMorgan | JPM | Finance |
| 9 | Visa | V | Finance |
| 10 | Walmart | WMT | Retail |

---

## 📋 Test Categories

```
Weather Agent Tests:
├── Top 10 Cities (10 tests)
├── Forecasts (3 tests)
└── Status (1 test)

Stock Agent Tests:
├── Top 10 Stocks (10 tests)
├── Detailed Quotes (5 tests)
├── Portfolio (1 test)
└── Status (1 test)

Batch Tests:
├── Weather Batch (5 tests)
└── Stock Batch (5 tests)

TOTAL: 50 Tests
```

---

## ✅ Features Verified

- ✅ Real weather data from OpenWeatherMap
- ✅ Real stock data from Polygon.io/Alpha Vantage
- ✅ Async event processing
- ✅ Multi-instance Kafka coordination
- ✅ CloudEvents v1.0 compliance
- ✅ Quarkus CDI integration
- ✅ Error handling and fallback
- ✅ Concurrent request handling
- ✅ Performance (<100ms per request)
- ✅ Agent health monitoring

---

## 🔧 Troubleshooting

### Server not responding?
```bash
# Check if running
curl http://localhost:8080/

# If not, start it
cd amcp-examples && mvn quarkus:dev
```

### Tests failing?
```bash
# Check individual results
cat e2e-test-results-*/test-*.json | jq '.'

# Check server logs
tail -50 /tmp/quarkus.log
```

### Rate limit hit?
```bash
# Wait a minute and retry
# OpenWeatherMap: 1,000 calls/day
# Polygon.io: 5 calls/minute
```

---

## 📊 Sample Output

### Weather Response
```json
{
  "city": "London",
  "temperature": 12.5,
  "condition": "Clouds",
  "humidity": 72,
  "windSpeed": 3.5,
  "dataSource": "openweathermap (real)"
}
```

### Stock Response
```json
{
  "symbol": "AAPL",
  "price": 228.45,
  "bid": 228.40,
  "ask": 228.50,
  "52WeekHigh": 285.56,
  "52WeekLow": 171.34,
  "dataSource": "polygon.io (real)"
}
```

---

## 🎯 Success Criteria

✅ **All 50 tests pass**
✅ **100% success rate**
✅ **Real data flowing**
✅ **<100ms response time**
✅ **No errors**

---

**Ready to test?** Run: `./end-to-end-test.sh`
