# AMCP Stock Price Agent - Demo Scenario

## 🎯 Interactive Demo Walkthrough

This guided scenario demonstrates the key features of the AMCP v1.5 Enterprise Edition Stock Price Agent with real market data from Polygon.io. Follow along step-by-step to explore stock monitoring, price alerts, and portfolio management.

## 🚀 Starting the Demo

```bash
# Launch the Stock Price Agent Demo
./run-stockprice-demo.sh
```

Expected output:
```
╔═══════════════════════════════════════════════════════════════╗
║                    AMCP Stock Price Demo                      ║
║                        Version 1.5                           ║
║                     Enterprise Edition                        ║
║                     Polygon.io Integration                    ║
╚═══════════════════════════════════════════════════════════════╝

🚀 Starting AMCP Stock Price Agent Demo...
📊 Java Version: openjdk 21.0.1
🔑 Using built-in demo API key for Polygon.io
💾 Memory: 512MB - 2GB

🎯 Launching Stock Price Agent Demo...
   Use 'help' command for available options
   Use 'activate' to start the agent monitoring
   Use 'exit' to quit the demo

📈 Welcome to the AMCP Stock Price Demo!
✅ Using built-in demo API key for real market data!
Type 'help' for available commands or 'exit' to quit.

stock>
```

## 📋 Demo Scenario: Building an Investment Portfolio

### Step 1: Agent Activation and Status Check

```bash
stock> activate
```

Expected output:
```
🚀 Activating Stock Price Agent...
✅ Agent activated successfully!
📊 Agent is now ready to monitor stock prices and manage portfolios.
🔔 Price alerts will be checked every 2 minutes.
```

Check agent status:
```bash
stock> status
```

Expected output:
```
📊 Stock Price Agent Status
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🤖 Agent ID: stock-price-1727563215789
📊 Lifecycle State: ACTIVE
🔔 Active Alerts: 0
💼 Portfolios: 0
📈 Cached Stocks: 0
🔑 API Key Configured: Yes

🔧 Agent Capabilities:
   • Real-time stock price monitoring via Polygon.io API
   • Price alerts with customizable thresholds
   • Portfolio management and performance tracking
   • Multi-stock market analysis
   • Historical data integration
   • Market hours awareness
```

### Step 2: Market Overview

Get a quick market summary for major tech stocks:
```bash
stock> market AAPL,GOOGL,MSFT,TSLA,NVDA
```

Expected output:
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
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📊 Market Summary:
   🟢 Gainers: 3
   🔴 Losers: 2
   📈 Market Status: OPEN
```

### Step 3: Individual Stock Analysis

Get detailed information for Apple stock:
```bash
stock> price AAPL
```

Expected output:
```
📈 Fetching stock price for AAPL...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
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

Check multiple stocks at once:
```bash
stock> prices AAPL,MSFT,GOOGL
```

Expected output:
```
📊 Fetching stock prices for 3 symbols...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Symbol   Price        Change       Change%     Volume     Updated
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
AAPL     $173.25      +$1.45      +0.85%     45,123,456  14:30:15
MSFT     $421.18      +$2.34      +0.56%     31,234,567  14:30:17
GOOGL    $134.82      -$0.67      -0.49%     28,456,789  14:30:16
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### Step 4: Setting Up Price Alerts

Create an alert for Apple stock if it goes above $175:
```bash
stock> alert AAPL 175.00 ABOVE
```

Expected output:
```
🔔 Price alert created successfully!
   Alert ID: alert_1_1727563215789
   Symbol: AAPL
   Target: $175.00
   Type: ABOVE
```

Set up an alert for Tesla if it drops below $240:
```bash
stock> alert TSLA 240.00 BELOW
```

Expected output:
```
🔔 Price alert created successfully!
   Alert ID: alert_2_1727563225123
   Symbol: TSLA
   Target: $240.00
   Type: BELOW
```

Create a volatility alert for NVIDIA (5% change):
```bash
stock> alert NVDA 5.0 CHANGE
```

Expected output:
```
🔔 Price alert created successfully!
   Alert ID: alert_3_1727563235456
   Symbol: NVDA
   Target: $5.0
   Type: CHANGE
```

View all active alerts:
```bash
stock> alerts
```

Expected output:
```
🔔 Active Price Alerts
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔔 Alert ID: alert_1_1727563215789
   Symbol: AAPL
   Target Price: $175.00
   Type: ABOVE
   Status: ACTIVE
   Created: Sat Sep 28 14:33:35 PDT 2025

🔔 Alert ID: alert_2_1727563225123
   Symbol: TSLA
   Target Price: $240.00
   Type: BELOW
   Status: ACTIVE
   Created: Sat Sep 28 14:33:45 PDT 2025

🔔 Alert ID: alert_3_1727563235456
   Symbol: NVDA
   Target Price: $5.0
   Type: CHANGE
   Status: ACTIVE
   Created: Sat Sep 28 14:33:55 PDT 2025

Total active alerts: 3
```

### Step 5: Portfolio Management

Create a technology-focused portfolio:
```bash
stock> portfolio create "Tech Growth Portfolio"
```

Expected output:
```
💼 Portfolio created successfully!
   Portfolio ID: portfolio_1_1727563315789
   Name: Tech Growth Portfolio
   User: demo-user
   Created: Sat Sep 28 14:35:15 PDT 2025
```

Add Apple shares to the portfolio (purchased at $168.50):
```bash
stock> portfolio add portfolio_1_1727563315789 AAPL 100 168.50
```

Expected output:
```
✅ Stock added to portfolio successfully!
   Portfolio: portfolio_1_1727563315789
   Symbol: AAPL
   Shares: 100
   Average Price: $168.50
```

Add Microsoft shares:
```bash
stock> portfolio add portfolio_1_1727563315789 MSFT 50 415.00
```

Expected output:
```
✅ Stock added to portfolio successfully!
   Portfolio: portfolio_1_1727563315789
   Symbol: MSFT
   Shares: 50
   Average Price: $415.00
```

Add Google shares:
```bash
stock> portfolio add portfolio_1_1727563315789 GOOGL 75 138.25
```

Expected output:
```
✅ Stock added to portfolio successfully!
   Portfolio: portfolio_1_1727563315789
   Symbol: GOOGL
   Shares: 75
   Average Price: $138.25
```

View the portfolio details:
```bash
stock> portfolio show portfolio_1_1727563315789
```

Expected output:
```
💼 Portfolio Details
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Portfolio ID: portfolio_1_1727563315789
Name: Tech Growth Portfolio
User: demo-user
Created: Sat Sep 28 14:35:15 PDT 2025

📊 Holdings:
Symbol   Shares     Avg Price
────────────────────────────────
AAPL     100        $168.50
MSFT     50         $415.00
GOOGL    75         $138.25
```

### Step 6: Portfolio Performance Analysis

Analyze the portfolio performance with current market prices:
```bash
stock> portfolio analyze portfolio_1_1727563315789
```

Expected output:
```
📊 Analyzing portfolio performance...
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

### Step 7: Portfolio Management Operations

List all portfolios:
```bash
stock> portfolio list
```

Expected output:
```
💼 Portfolios
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💼 Portfolio ID: portfolio_1_1727563315789
   Name: Tech Growth Portfolio
   Holdings: 3 stocks
   Created: Sat Sep 28 14:35:15 PDT 2025

Total portfolios: 1
```

Create a second portfolio for diversification:
```bash
stock> portfolio create "Dividend Income Portfolio"
```

Expected output:
```
💼 Portfolio created successfully!
   Portfolio ID: portfolio_2_1727563415123
   Name: Dividend Income Portfolio
   User: demo-user
   Created: Sat Sep 28 14:36:55 PDT 2025
```

### Step 8: Advanced Market Monitoring

Monitor a broader set of stocks:
```bash
stock> market SPY,QQQ,IWM,AMZN,META,NFLX
```

Check individual performance of high-profile stocks:
```bash
stock> prices TSLA,NVDA,AMD,CRM
```

### Step 9: Alert Management

Check if any alerts have been triggered (wait a few minutes):
```bash
stock> alerts
```

Cancel an alert if needed:
```bash
stock> cancel alert_3_1727563235456
```

Expected output:
```
✅ Alert cancelled successfully: alert_3_1727563235456
```

### Step 10: Final Status Check

Check the final agent status:
```bash
stock> status
```

Expected output:
```
📊 Stock Price Agent Status
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🤖 Agent ID: stock-price-1727563215789
📊 Lifecycle State: ACTIVE
🔔 Active Alerts: 2
💼 Portfolios: 2
📈 Cached Stocks: 10
🔑 API Key Configured: Yes

🔧 Agent Capabilities:
   • Real-time stock price monitoring via Polygon.io API
   • Price alerts with customizable thresholds
   • Portfolio management and performance tracking
   • Multi-stock market analysis
   • Historical data integration
   • Market hours awareness
```

### Step 11: Clean Exit

Deactivate the agent and exit:
```bash
stock> deactivate
```

Expected output:
```
⏹️ Deactivating Stock Price Agent...
✅ Agent deactivated successfully!
💤 Agent monitoring services have been stopped.
```

Exit the demo:
```bash
stock> exit
```

Expected output:
```
👋 Thank you for using AMCP Stock Price Demo!

✅ Demo completed successfully!
```

## 🎯 Demo Summary

This scenario demonstrated:

1. **🤖 Agent Lifecycle**: Activation, monitoring, and deactivation
2. **📈 Real-time Market Data**: Individual stocks and market overviews
3. **🔔 Price Alerts**: Multiple alert types with active monitoring
4. **💼 Portfolio Management**: Creation, stock additions, and performance analysis
5. **📊 Performance Tracking**: Real-time P&L calculations
6. **⚡ Background Monitoring**: Automatic price checking every 2 minutes
7. **🔄 AMCP Integration**: Full agent mobility and event-driven architecture

## 🚀 Next Steps

After completing this demo, you can:

1. **📖 Explore the Code**: Review `StockPriceAgent.java` for implementation details
2. **🔧 Customize**: Modify alert frequencies and thresholds
3. **📊 Extend**: Add new features like technical indicators
4. **🌐 Deploy**: Use the agent in distributed AMCP environments
5. **💡 Build**: Create your own financial agents using this pattern

## 💡 Tips for Production Use

- **🔑 API Key**: Get your own Polygon.io API key for production use
- **📈 Rate Limits**: Monitor API usage to avoid rate limiting
- **💾 Persistence**: Add database storage for portfolios and alerts
- **🚨 Notifications**: Integrate with email/SMS for alert delivery
- **📊 Analytics**: Add charting and technical analysis features

## 🏗️ AMCP v1.5 Enterprise Edition Features

This demo showcases several key features of AMCP v1.5 Enterprise Edition:

- **📡 Enhanced Event Broker**: Real-time stock price events
- **🔐 Advanced Security**: Secure API key management
- **⚡ Optimized Performance**: Efficient concurrent processing
- **📊 CloudEvents Compliance**: Standard event formatting
- **🌐 A2A Bridge**: Agent-to-agent communication
- **🔧 Testing Framework**: Comprehensive test coverage

---

*AMCP v1.5 Enterprise Edition Stock Price Agent Demo - Showcasing real-time financial data integration with enterprise-grade agent framework*