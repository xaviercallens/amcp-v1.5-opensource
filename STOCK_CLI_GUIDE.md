# AMCP Stock Price Agent - CLI Guide

## Overview

The AMCP Stock Price Agent is an advanced example agent that demonstrates comprehensive stock market data integration with real-time monitoring, portfolio management, and market alerts. It leverages the Polygon.io API to provide up-to-date market data and sophisticated investment tracking capabilities.

## Features

- 📈 **Real-time Stock Price Monitoring** - Get current prices, changes, and market data
- 🔔 **Price Alerts** - Set customizable alerts for price thresholds and changes  
- 💼 **Portfolio Management** - Create and manage investment portfolios
- 📊 **Portfolio Analysis** - Comprehensive performance tracking and analytics
- 🌐 **Market Overview** - Multi-stock market summaries and insights
- ⚡ **Live Monitoring** - Background monitoring with configurable intervals
- 🔄 **Agent Mobility** - Full AMCP mobility support for distributed deployments

## Quick Start

### Prerequisites

1. **Java 21+** - Download from [OpenJDK](https://openjdk.org/projects/jdk/21/)
2. **Maven 3.8+** - For building the project
3. **Polygon.io API Key** (Optional) - For real-time data

### Getting Your Polygon.io API Key

1. Visit [polygon.io](https://polygon.io) and create a free account
2. Navigate to your dashboard to find your API key
3. Copy the API key for use with the demo

### Running the Demo

1. **Set API Key** (Optional - demo works with built-in key):
   ```bash
   export POLYGON_API_KEY=your_api_key_here
   ```

2. **Launch the Demo**:
   ```bash
   ./run-stockprice-demo.sh
   ```

3. **Follow the Guided Demo**:
   - See `STOCK_DEMO_SCENARIO.md` for a complete step-by-step walkthrough
   - The scenario includes portfolio building, alerts setup, and market analysis

4. **Activate the Agent**:
   ```
   stock> activate
   ```

5. **Start Using Commands**:
   ```
   stock> help
   ```

## Command Reference

### 🤖 Agent Management

#### `activate`
Start the agent and begin monitoring services.
```bash
stock> activate
```
**Example output:**
```
🚀 Activating Stock Price Agent...
✅ Agent activated successfully!
📊 Agent is now ready to monitor stock prices and manage portfolios.
🔔 Price alerts will be checked every 2 minutes.
```

#### `deactivate`  
Stop the agent and all monitoring services.
```bash
stock> deactivate
```

#### `status`
Display comprehensive agent status information.
```bash
stock> status
```
**Example output:**
```
📊 Stock Price Agent Status
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🤖 Agent ID: stock-price-1727563215789
📊 Lifecycle State: ACTIVE
🔔 Active Alerts: 3
💼 Portfolios: 2
📈 Cached Stocks: 15
🔑 API Key Configured: Yes
```

### 📈 Stock Price Monitoring

#### `price <SYMBOL>`
Get detailed information for a single stock.
```bash
stock> price AAPL
```
**Example output:**
```
📈 AAPL Stock Information
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💰 Current Price: $173.25
📊 Open Price: $171.80
📈 High: $174.50
📉 Low: $170.95
🟢 Change: +$1.45 (+0.85%)
📊 Volume: 45,123,456
⏰ Updated: Sat Sep 28 14:30:15 PDT 2025
```

#### `prices <SYMBOL1>,<SYMBOL2>,...`
Get prices for multiple stocks at once.
```bash
stock> prices AAPL,MSFT,GOOGL
```

#### `market <SYMBOL1>,<SYMBOL2>,...`
Get a comprehensive market summary for multiple stocks.
```bash
stock> market AAPL,GOOGL,MSFT,TSLA,NVDA
```
**Example output:**
```
📊 Market Summary for 5 symbols
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Symbol   Price        Change       Change%     Status
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
AAPL     $173.25      +$1.45      +0.85%      🟢
GOOGL    $134.82      -$0.67      -0.49%      🔴
MSFT     $421.18      +$2.34      +0.56%      🟢
TSLA     $248.95      -$3.21      -1.27%      🔴
NVDA     $457.33      +$8.92      +1.99%      🟢
```

### 🔔 Price Alerts

#### `alert <SYMBOL> <PRICE> <TYPE>`
Create a price alert for a stock.

**Alert Types:**
- `ABOVE` - Alert when price goes above target
- `BELOW` - Alert when price goes below target
- `CHANGE` - Alert when price changes by percentage amount

```bash
stock> alert AAPL 175.00 ABOVE
stock> alert TSLA 240.00 BELOW
stock> alert NVDA 5.0 CHANGE
```

#### `alerts`
List all active price alerts.
```bash
stock> alerts
```

#### `cancel <ALERT_ID>`
Cancel a specific price alert.
```bash
stock> cancel alert_1_1727563215789
```

### 💼 Portfolio Management

#### `portfolio create "<NAME>"`
Create a new investment portfolio.
```bash
stock> portfolio create "Tech Growth Portfolio"
```

#### `portfolio add <PORTFOLIO_ID> <SYMBOL> <SHARES> <PRICE>`
Add stock holdings to a portfolio.
```bash
stock> portfolio add portfolio_1_1727563315789 AAPL 100 168.50
```

#### `portfolio show <PORTFOLIO_ID>`
Display portfolio details and holdings.
```bash
stock> portfolio show portfolio_1_1727563315789
```

#### `portfolio analyze <PORTFOLIO_ID>`
Perform comprehensive performance analysis.
```bash
stock> portfolio analyze portfolio_1_1727563315789
```
**Example output:**
```
📊 Portfolio Analysis: Tech Growth Portfolio
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💰 Total Value: $52,136.25
💵 Total Cost: $51,618.75
🟢 Total Gain/Loss: +$517.50 (+1.00%)

📊 Holdings Performance:
Symbol   Shares   Avg Price    Current      Value        Gain/Loss
──────────────────────────────────────────────────────────────
AAPL     100      $168.50      $173.25      $17,325.00   +$475.00
MSFT     50       $415.00      $421.18      $21,059.00   +$309.00
GOOGL    75       $138.25      $134.82      $10,111.50   -$257.25
```

#### `portfolio list`
List all portfolios.
```bash
stock> portfolio list
```

#### `portfolio remove <PORTFOLIO_ID> <SYMBOL> <SHARES>`
Remove shares from a portfolio.
```bash
stock> portfolio remove portfolio_1_1727563315789 AAPL 50
```

#### `portfolio delete <PORTFOLIO_ID>`
Delete an entire portfolio.
```bash
stock> portfolio delete portfolio_1_1727563315789
```

### ℹ️ General Commands

#### `help`
Display comprehensive command help.
```bash
stock> help
```

#### `version`
Show agent version and build information.
```bash
stock> version
```

#### `exit`
Exit the demo application.
```bash
stock> exit
```

## 🔧 Configuration

### API Key Setup

The demo includes a built-in API key for immediate use. For production or extended use:

1. **Environment Variable** (Recommended):
   ```bash
   export POLYGON_API_KEY=your_api_key_here
   ./run-stockprice-demo.sh
   ```

2. **Runtime Configuration**:
   The agent will automatically detect and use the environment variable.

### Alert Configuration

- **Check Interval**: Alerts are checked every 2 minutes by default
- **Alert Types**: Support for price thresholds and percentage changes
- **Persistence**: Alerts remain active until triggered or cancelled

### Performance Tuning

- **Cache Duration**: Stock data is cached for 1 minute to reduce API calls
- **Concurrent Requests**: Supports parallel stock price fetching
- **Memory Usage**: Optimized for portfolios with up to 1000+ stocks

## 🎯 Demo Scenarios

### Basic Stock Monitoring
1. Activate the agent: `activate`
2. Check a single stock: `price AAPL`
3. Monitor multiple stocks: `market AAPL,MSFT,GOOGL`

### Setting Up Alerts
1. Create price alerts: `alert AAPL 175.00 ABOVE`
2. Monitor alert status: `alerts`
3. Wait for notifications (background monitoring)

### Portfolio Management
1. Create portfolio: `portfolio create "My Portfolio"`
2. Add holdings: `portfolio add <id> AAPL 100 170.00`
3. Analyze performance: `portfolio analyze <id>`

### Advanced Usage
1. Multiple portfolios for diversification
2. Complex alert strategies with different thresholds
3. Performance tracking over time

## 🚨 Troubleshooting

### Common Issues

#### Build Errors
```bash
# Clean and rebuild
mvn clean compile -f examples/pom.xml
```

#### API Connection Issues
- Verify internet connection
- Check API key validity at polygon.io
- Monitor rate limits (free tier: 5 calls/minute)

#### Memory Issues
- Increase JVM memory: Edit `run-stockprice-demo.sh`
- Reduce portfolio size for large datasets

#### Java Version
- Ensure Java 21+ is installed and active
- Check: `java -version`

### Error Messages

#### "API Key not found"
**Solution**: Set POLYGON_API_KEY environment variable or use built-in demo key.

#### "Rate limit exceeded"
**Solution**: Wait 1 minute between requests or upgrade Polygon.io plan.

#### "Stock symbol not found"
**Solution**: Verify stock symbol exists and is traded on US exchanges.

## 🌟 Advanced Features

### Background Monitoring
- Automatic price checking every 2 minutes
- Real-time alert notifications
- Cache management for optimal performance

### Portfolio Analytics
- Real-time P&L calculations
- Performance tracking vs. purchase price
- Diversification analysis

### Market Data Integration
- Real-time and delayed quotes
- Volume and trading data
- Market hours awareness

## 📖 Additional Resources

- **[Polygon.io API Documentation](https://polygon.io/docs)** - Complete API reference
- **[AMCP v1.5 Enterprise Edition Guide](README.md)** - Framework overview
- **[Demo Scenario](STOCK_DEMO_SCENARIO.md)** - Step-by-step walkthrough
- **[Java 21 Documentation](https://openjdk.org/projects/jdk/21/)** - Language features

---

*AMCP v1.5 Enterprise Edition - Stock Price Agent CLI Guide*