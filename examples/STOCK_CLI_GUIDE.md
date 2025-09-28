# AMCP Stock Price Agent CLI Guide

## Overview
The Stock Price Agent demonstrates AMCP v1.5's financial market data monitoring capabilities with portfolio management and real-time price tracking.

## Features
- **Portfolio Management**: Add/remove stock positions
- **Price Monitoring**: Real-time stock price retrieval 
- **Event-Driven Architecture**: Async messaging between agents
- **Simulated Market Data**: Demo-friendly price simulation

## Running the Demo

### Basic Demo
```bash
cd examples
java -cp ../core/target/amcp-core-1.5.0.jar:target/classes io.amcp.examples.stockprice.StockPriceDemo
```

### Interactive Mode
The demo automatically:
1. Creates and activates a Stock Price Agent
2. Adds sample portfolio positions (AAPL, GOOGL)
3. Requests prices for multiple stocks (AAPL, GOOGL, MSFT, TSLA, AMZN)
4. Demonstrates periodic price monitoring
5. Clean shutdown after 10 seconds

## Sample Output
```
=== AMCP v1.4 Stock Price Agent Demo ===
Stock Price Agent activated: stock-price-1234567890

--- Portfolio Management Demo ---
[12:34:56] [stock-price-1234567890] Added to portfolio: AAPL
[12:34:57] [stock-price-1234567890] Added to portfolio: GOOGL
Portfolio positions added

--- Stock Price Request Demo ---
[12:34:58] [stock-price-1234567890] Stock price for AAPL: $127.45
[12:34:59] [stock-price-1234567890] Stock price for GOOGL: $89.23
Requested price for: AAPL
Requested price for: GOOGL
...

Demo completed successfully!
```

## Agent Events

### Portfolio Management
- **Topic**: `portfolio.add`
- **Payload**: `{"action": "add", "symbol": "AAPL", "shares": 100, "price": 150.50}`

### Price Requests
- **Topic**: `stock.price.request` 
- **Payload**: `{"symbol": "AAPL", "requestId": "req_1234567890"}`

### Price Responses
- **Topic**: `stock.price.response`
- **Payload**: `{"symbol": "AAPL", "price": "127.45", "timestamp": 1234567890}`

## Integration Notes

### External API Integration
The current implementation uses simulated prices. For production:
1. Replace `simulatePrice()` with actual Polygon.io API calls
2. Add API key configuration via environment variables
3. Implement rate limiting and error handling

### Event Patterns
- Follows AMCP v1.5 event-driven architecture
- Uses reliable delivery for critical price updates
- Supports agent mobility and state serialization

## Customization

### Adding New Stocks
Modify the symbols array in `simulateStockPriceRequests()`:
```java
String[] symbols = {"AAPL", "GOOGL", "MSFT", "TSLA", "AMZN", "NVDA", "META"};
```

### Adjusting Monitoring Frequency
Change the scheduler delay in `onActivate()`:
```java
scheduler.scheduleWithFixedDelay(this::monitorPrices, 30, 60, TimeUnit.SECONDS);
```

## Troubleshooting

### Common Issues
1. **Agent not responding**: Check that context subscription topics match event topics
2. **No price updates**: Verify scheduler is running and agent is in ACTIVE state
3. **Memory issues**: Reduce monitoring frequency for large portfolios

### Debugging
Enable verbose logging by modifying `logMessage()` to include more detail:
```java
System.out.println("[" + timestamp + "] [DEBUG] [" + agentId + "] " + message);
```