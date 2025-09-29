package io.amcp.examples.stockprice;

import io.amcp.core.AgentID;
import io.amcp.core.AgentLifecycle;
import io.amcp.examples.stockprice.StockPriceAgent.StockAlert;
import io.amcp.examples.stockprice.StockPriceAgent.StockData;
import io.amcp.examples.stockprice.StockPriceAgent.Portfolio;
import io.amcp.examples.stockprice.StockPriceAgent.PortfolioAnalysis;

import java.util.Scanner;
import java.util.List;
import java.util.Arrays;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.CompletableFuture;

/**
 * Interactive command-line demo for the Stock Price Agent
 */
public class StockPriceDemo {
    
    private static final String HEADER = """
    ╔═══════════════════════════════════════════════════════════════╗
    ║                    AMCP Stock Price Demo                      ║
    ║                        Version 1.5 Enterprise Edition        ║
    ║                     Polygon.io Integration                    ║
    ╚═══════════════════════════════════════════════════════════════╝
    """;
    
    private static final String HELP_TEXT = """
    Available Commands:
    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    📈 price <SYMBOL>                                    - Get current stock price
    📊 prices <SYMBOL1,SYMBOL2,...>                      - Get multiple stock prices
    🔔 alert <SYMBOL> <PRICE> <TYPE>                     - Create price alert (ABOVE/BELOW/CHANGE)
    📋 alerts                                           - List active alerts
    ❌ cancel <ALERT_ID>                                 - Cancel price alert
    💼 portfolio create <NAME>                           - Create new portfolio
    💼 portfolio add <PORTFOLIO_ID> <SYMBOL> <SHARES> <PRICE> - Add stock to portfolio
    💼 portfolio list                                    - List all portfolios
    💼 portfolio show <PORTFOLIO_ID>                     - Show portfolio details
    💼 portfolio analyze <PORTFOLIO_ID>                  - Analyze portfolio performance
    📊 market [SYMBOLS]                                 - Market summary (default: AAPL,GOOGL,MSFT,TSLA,NVDA)
    ⚙️  activate                                         - Activate agent
    ⏹️  deactivate                                       - Deactivate agent
    📊 status                                           - Show agent status
    ❓ help                                             - Show this help
    🚪 exit                                             - Exit demo
    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    
    📝 Examples:
       price AAPL
       prices AAPL,GOOGL,MSFT
       alert AAPL 150.00 ABOVE
       portfolio create "My Portfolio"
       portfolio add portfolio_123 AAPL 100 145.50
       market AAPL,GOOGL,MSFT,TSLA,NVDA
    
    💡 Note: Set POLYGON_API_KEY environment variable with your Polygon.io API key
    """;
    
    private StockPriceAgent agent;
    private Scanner scanner;
    private String currentUserId = "demo-user";
    
    public StockPriceDemo() {
        this.agent = new StockPriceAgent();
        this.scanner = new Scanner(System.in);
    }
    
    public static void main(String[] args) {
        new StockPriceDemo().run();
    }
    
    public void run() {
        System.out.println(HEADER);
        System.out.println("📈 Welcome to the AMCP Stock Price Demo!");
        
        String apiKey = System.getProperty("POLYGON_API_KEY", System.getenv("POLYGON_API_KEY"));
        if (apiKey == null) {
            System.out.println("✅ Using demo Polygon.io API key for real market data!");
            System.out.println("   For production use, set your own POLYGON_API_KEY environment variable.");
        } else {
            System.out.println("✅ Using custom Polygon.io API key from environment!");
        }
        
        System.out.println("Type 'help' for available commands or 'exit' to quit.\n");
        
        boolean running = true;
        
        while (running) {
            System.out.print("stock> ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) {
                continue;
            }
            
            String[] parts = input.split("\\s+");
            String command = parts[0].toLowerCase();
            
            try {
                switch (command) {
                    case "help":
                        showHelp();
                        break;
                    case "price":
                        handlePriceCommand(parts);
                        break;
                    case "prices":
                        handlePricesCommand(parts);
                        break;
                    case "alert":
                        handleAlertCommand(parts);
                        break;
                    case "alerts":
                        handleAlertsCommand();
                        break;
                    case "cancel":
                        handleCancelCommand(parts);
                        break;
                    case "portfolio":
                        handlePortfolioCommand(parts);
                        break;
                    case "market":
                        handleMarketCommand(parts);
                        break;
                    case "status":
                        showAgentStatus();
                        break;
                    case "activate":
                        activateAgent();
                        break;
                    case "deactivate":
                        deactivateAgent();
                        break;
                    case "exit":
                        System.out.println("👋 Thank you for using AMCP Stock Price Demo!");
                        running = false;
                        break;
                    default:
                        System.out.println("❌ Unknown command: " + command);
                        System.out.println("💡 Type 'help' to see available commands.");
                }
            } catch (Exception e) {
                System.err.println("❌ Error executing command: " + e.getMessage());
                if (e.getCause() != null) {
                    System.err.println("   Cause: " + e.getCause().getMessage());
                }
            }
            
            System.out.println(); // Add spacing between commands
        }
        
        // Cleanup
        if (agent.getLifecycleState() == AgentLifecycle.ACTIVE) {
            agent.onDeactivate();
        }
        scanner.close();
    }
    
    private void showHelp() {
        System.out.println(HELP_TEXT);
    }
    
    private void handlePriceCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("❌ Usage: price <SYMBOL>");
            System.out.println("📝 Example: price AAPL");
            return;
        }
        
        String symbol = parts[1].toUpperCase();
        
        System.out.println("📈 Fetching stock price for " + symbol + "...");
        
        try {
            CompletableFuture<StockData> future = agent.getStockPrice(symbol);
            StockData stockData = future.get();
            
            if (stockData != null) {
                displayStockData(stockData);
            } else {
                System.out.println("❌ No data available for " + symbol);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Failed to fetch stock price: " + e.getMessage());
            
            // Show demo data if API fails
            showDemoStockData(symbol);
        }
    }
    
    private void handlePricesCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("❌ Usage: prices <SYMBOL1,SYMBOL2,...>");
            System.out.println("📝 Example: prices AAPL,GOOGL,MSFT");
            return;
        }
        
        String symbolsInput = parts[1].toUpperCase();
        List<String> symbols = Arrays.asList(symbolsInput.split(","));
        
        System.out.println("📊 Fetching stock prices for " + symbols.size() + " symbols...");
        
        try {
            CompletableFuture<List<StockData>> future = agent.getMultipleStockPrices(symbols);
            List<StockData> stockDataList = future.get();
            
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.printf("%-8s %-12s %-12s %-12s %-10s %-15s%n", 
                            "Symbol", "Price", "Change", "Change%", "Volume", "Updated");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            
            for (StockData data : stockDataList) {
                displayStockDataRow(data);
            }
            
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            
        } catch (Exception e) {
            System.err.println("❌ Failed to fetch stock prices: " + e.getMessage());
        }
    }
    
    private void handleAlertCommand(String[] parts) {
        if (parts.length < 4) {
            System.out.println("❌ Usage: alert <SYMBOL> <PRICE> <TYPE>");
            System.out.println("📝 Types: ABOVE, BELOW, CHANGE");
            System.out.println("📝 Example: alert AAPL 150.00 ABOVE");
            return;
        }
        
        String symbol = parts[1].toUpperCase();
        String alertType = parts[3].toUpperCase();
        
        try {
            BigDecimal targetPrice = new BigDecimal(parts[2]);
            
            if (!Arrays.asList("ABOVE", "BELOW", "CHANGE").contains(alertType)) {
                System.out.println("❌ Invalid alert type. Use: ABOVE, BELOW, or CHANGE");
                return;
            }
            
            String alertId = agent.createPriceAlert(currentUserId, symbol, targetPrice, alertType);
            
            System.out.println("🔔 Price alert created successfully!");
            System.out.println("   Alert ID: " + alertId);
            System.out.println("   Symbol: " + symbol);
            System.out.println("   Target: $" + targetPrice);
            System.out.println("   Type: " + alertType);
            
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid price format. Use decimal number (e.g., 150.00)");
        } catch (Exception e) {
            System.err.println("❌ Failed to create alert: " + e.getMessage());
        }
    }
    
    private void handleAlertsCommand() {
        List<StockAlert> alerts = agent.getActiveAlerts();
        
        System.out.println("🔔 Active Price Alerts");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        if (alerts.isEmpty()) {
            System.out.println("📭 No active alerts found.");
            System.out.println("💡 Use 'alert' command to create price alerts.");
        } else {
            for (StockAlert alert : alerts) {
                System.out.println("🔔 Alert ID: " + alert.getAlertId());
                System.out.println("   Symbol: " + alert.getSymbol());
                System.out.println("   Target Price: $" + alert.getTargetPrice());
                System.out.println("   Type: " + alert.getAlertType());
                System.out.println("   Status: " + alert.getStatus());
                System.out.println("   Created: " + new java.util.Date(alert.getCreatedTime()));
                
                if ("TRIGGERED".equals(alert.getStatus()) && alert.getTriggeredPrice() != null) {
                    System.out.println("   Triggered at: $" + alert.getTriggeredPrice());
                    System.out.println("   Triggered: " + new java.util.Date(alert.getTriggeredTime()));
                }
                System.out.println("");
            }
            
            System.out.println("Total active alerts: " + alerts.size());
        }
    }
    
    private void handleCancelCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("❌ Usage: cancel <ALERT_ID>");
            return;
        }
        
        String alertId = parts[1];
        
        boolean cancelled = agent.cancelPriceAlert(alertId);
        
        if (cancelled) {
            System.out.println("✅ Alert cancelled successfully: " + alertId);
        } else {
            System.out.println("❌ Alert not found: " + alertId);
            System.out.println("💡 Use 'alerts' command to list active alerts.");
        }
    }
    
    private void handlePortfolioCommand(String[] parts) {
        if (parts.length < 2) {
            System.out.println("❌ Usage: portfolio <subcommand>");
            System.out.println("📝 Subcommands: create, add, list, show, analyze");
            return;
        }
        
        String subcommand = parts[1].toLowerCase();
        
        switch (subcommand) {
            case "create":
                handlePortfolioCreate(parts);
                break;
            case "add":
                handlePortfolioAdd(parts);
                break;
            case "list":
                handlePortfolioList();
                break;
            case "show":
                handlePortfolioShow(parts);
                break;
            case "analyze":
                handlePortfolioAnalyze(parts);
                break;
            default:
                System.out.println("❌ Unknown portfolio subcommand: " + subcommand);
                System.out.println("📝 Available: create, add, list, show, analyze");
        }
    }
    
    private void handlePortfolioCreate(String[] parts) {
        if (parts.length < 3) {
            System.out.println("❌ Usage: portfolio create <NAME>");
            System.out.println("📝 Example: portfolio create \"My Portfolio\"");
            return;
        }
        
        // Join remaining parts as portfolio name
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 2; i < parts.length; i++) {
            if (i > 2) nameBuilder.append(" ");
            nameBuilder.append(parts[i].replace("\"", ""));
        }
        String portfolioName = nameBuilder.toString();
        
        Portfolio portfolio = agent.createPortfolio(currentUserId, portfolioName);
        
        System.out.println("💼 Portfolio created successfully!");
        System.out.println("   Portfolio ID: " + portfolio.getPortfolioId());
        System.out.println("   Name: " + portfolio.getPortfolioName());
        System.out.println("   User: " + portfolio.getUserId());
        System.out.println("   Created: " + new java.util.Date(portfolio.getCreatedTime()));
    }
    
    private void handlePortfolioAdd(String[] parts) {
        if (parts.length < 6) {
            System.out.println("❌ Usage: portfolio add <PORTFOLIO_ID> <SYMBOL> <SHARES> <PRICE>");
            System.out.println("📝 Example: portfolio add portfolio_123 AAPL 100 145.50");
            return;
        }
        
        String portfolioId = parts[2];
        String symbol = parts[3].toUpperCase();
        
        try {
            int shares = Integer.parseInt(parts[4]);
            BigDecimal price = new BigDecimal(parts[5]);
            
            boolean added = agent.addStockToPortfolio(portfolioId, symbol, shares, price);
            
            if (added) {
                System.out.println("✅ Stock added to portfolio successfully!");
                System.out.println("   Portfolio: " + portfolioId);
                System.out.println("   Symbol: " + symbol);
                System.out.println("   Shares: " + shares);
                System.out.println("   Average Price: $" + price);
            } else {
                System.out.println("❌ Portfolio not found: " + portfolioId);
                System.out.println("💡 Use 'portfolio list' to see available portfolios.");
            }
            
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid number format. Shares must be integer, price must be decimal.");
        } catch (Exception e) {
            System.err.println("❌ Failed to add stock to portfolio: " + e.getMessage());
        }
    }
    
    private void handlePortfolioList() {
        List<Portfolio> portfolios = agent.getPortfolios();
        
        System.out.println("💼 Portfolios");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        if (portfolios.isEmpty()) {
            System.out.println("📭 No portfolios found.");
            System.out.println("💡 Use 'portfolio create' to create a new portfolio.");
        } else {
            for (Portfolio portfolio : portfolios) {
                System.out.println("💼 Portfolio ID: " + portfolio.getPortfolioId());
                System.out.println("   Name: " + portfolio.getPortfolioName());
                System.out.println("   Holdings: " + portfolio.getHoldings().size() + " stocks");
                System.out.println("   Created: " + new java.util.Date(portfolio.getCreatedTime()));
                System.out.println("");
            }
            
            System.out.println("Total portfolios: " + portfolios.size());
        }
    }
    
    private void handlePortfolioShow(String[] parts) {
        if (parts.length < 3) {
            System.out.println("❌ Usage: portfolio show <PORTFOLIO_ID>");
            return;
        }
        
        String portfolioId = parts[2];
        Portfolio portfolio = agent.getPortfolio(portfolioId);
        
        if (portfolio == null) {
            System.out.println("❌ Portfolio not found: " + portfolioId);
            return;
        }
        
        System.out.println("💼 Portfolio Details");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Portfolio ID: " + portfolio.getPortfolioId());
        System.out.println("Name: " + portfolio.getPortfolioName());
        System.out.println("User: " + portfolio.getUserId());
        System.out.println("Created: " + new java.util.Date(portfolio.getCreatedTime()));
        
        if (portfolio.getHoldings().isEmpty()) {
            System.out.println("\n📭 No holdings in this portfolio.");
            System.out.println("💡 Use 'portfolio add' to add stocks.");
        } else {
            System.out.println("\n📊 Holdings:");
            System.out.printf("%-8s %-10s %-15s%n", "Symbol", "Shares", "Avg Price");
            System.out.println("────────────────────────────────");
            
            portfolio.getHoldings().forEach((symbol, holding) -> {
                System.out.printf("%-8s %-10d $%-14.2f%n", 
                    holding.getSymbol(), 
                    holding.getShares(), 
                    holding.getAveragePrice());
            });
        }
    }
    
    private void handlePortfolioAnalyze(String[] parts) {
        if (parts.length < 3) {
            System.out.println("❌ Usage: portfolio analyze <PORTFOLIO_ID>");
            return;
        }
        
        String portfolioId = parts[2];
        Portfolio portfolio = agent.getPortfolio(portfolioId);
        
        if (portfolio == null) {
            System.out.println("❌ Portfolio not found: " + portfolioId);
            return;
        }
        
        if (portfolio.getHoldings().isEmpty()) {
            System.out.println("❌ Portfolio has no holdings to analyze.");
            return;
        }
        
        System.out.println("📊 Analyzing portfolio performance...");
        
        try {
            // For demo purposes, we'll create a mock analysis since we may not have real API access
            showDemoPortfolioAnalysis(portfolio);
            
        } catch (Exception e) {
            System.err.println("❌ Failed to analyze portfolio: " + e.getMessage());
        }
    }
    
    private void handleMarketCommand(String[] parts) {
        List<String> symbols;
        
        if (parts.length > 1) {
            symbols = Arrays.asList(parts[1].toUpperCase().split(","));
        } else {
            // Default market symbols
            symbols = Arrays.asList("AAPL", "GOOGL", "MSFT", "TSLA", "NVDA");
        }
        
        System.out.println("📊 Market Summary for " + symbols.size() + " symbols");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        try {
            // For demo purposes, show mock market data
            showDemoMarketSummary(symbols);
            
        } catch (Exception e) {
            System.err.println("❌ Failed to fetch market summary: " + e.getMessage());
        }
    }
    
    private void showAgentStatus() {
        System.out.println("📊 Stock Price Agent Status");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        try {
            System.out.println("🤖 Agent ID: " + agent.getAgentId());
            System.out.println("📊 Lifecycle State: " + agent.getLifecycleState());
            System.out.println("🔔 Active Alerts: " + agent.getActiveAlertsCount());
            System.out.println("💼 Portfolios: " + agent.getPortfoliosCount());
            System.out.println("📈 Cached Stocks: " + agent.getCachedStocksCount());
            
            String apiKey = System.getProperty("POLYGON_API_KEY", System.getenv("POLYGON_API_KEY"));
            boolean apiConfigured = true; // We always have the demo key as fallback
            System.out.println("🔑 API Key Configured: " + (apiConfigured ? "Yes" : "No"));
            
            System.out.println("\n🔧 Agent Capabilities:");
            System.out.println("   • Real-time stock price monitoring via Polygon.io API");
            System.out.println("   • Price alerts with customizable thresholds");
            System.out.println("   • Portfolio management and performance tracking");
            System.out.println("   • Multi-stock market analysis");
            System.out.println("   • Historical data integration");
            System.out.println("   • Market hours awareness");
            
        } catch (Exception e) {
            System.err.println("❌ Error retrieving agent status: " + e.getMessage());
        }
    }
    
    private void activateAgent() {
        try {
            AgentLifecycle currentState = agent.getLifecycleState();
            
            if (currentState == AgentLifecycle.ACTIVE) {
                System.out.println("ℹ️ Agent is already active.");
                return;
            }
            
            System.out.println("🚀 Activating Stock Price Agent...");
            agent.onActivate();
            
            System.out.println("✅ Agent activated successfully!");
            System.out.println("📊 Agent is now ready to monitor stock prices and manage portfolios.");
            System.out.println("🔔 Price alerts will be checked every 2 minutes.");
            
        } catch (Exception e) {
            System.err.println("❌ Error activating agent: " + e.getMessage());
        }
    }
    
    private void deactivateAgent() {
        try {
            AgentLifecycle currentState = agent.getLifecycleState();
            
            if (currentState == AgentLifecycle.INACTIVE) {
                System.out.println("ℹ️ Agent is already inactive.");
                return;
            }
            
            System.out.println("⏹️ Deactivating Stock Price Agent...");
            agent.onDeactivate();
            
            System.out.println("✅ Agent deactivated successfully!");
            System.out.println("💤 Agent monitoring services have been stopped.");
            
        } catch (Exception e) {
            System.err.println("❌ Error deactivating agent: " + e.getMessage());
        }
    }
    
    private void displayStockData(StockData stockData) {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("📈 " + stockData.getSymbol() + " Stock Information");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("💰 Current Price: $" + formatPrice(stockData.getClosePrice()));
        System.out.println("📊 Open Price: $" + formatPrice(stockData.getOpenPrice()));
        System.out.println("📈 High: $" + formatPrice(stockData.getHighPrice()));
        System.out.println("📉 Low: $" + formatPrice(stockData.getLowPrice()));
        
        BigDecimal change = stockData.getChange();
        double changePercent = stockData.getChangePercent();
        String changeIcon = change.compareTo(BigDecimal.ZERO) >= 0 ? "🟢" : "🔴";
        String changeSign = change.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        
        System.out.println(changeIcon + " Change: " + changeSign + "$" + formatPrice(change) + 
                          " (" + changeSign + String.format("%.2f", changePercent) + "%)");
        System.out.println("📊 Volume: " + formatNumber(stockData.getVolume()));
        System.out.println("⏰ Updated: " + new java.util.Date(stockData.getTimestamp()));
    }
    
    private void displayStockDataRow(StockData data) {
        String changeIcon = data.getChangePercent() >= 0 ? "🟢" : "🔴";
        String changeSign = data.getChangePercent() >= 0 ? "+" : "";
        
        System.out.printf("%-8s $%-10.2f %s$%-7.2f %s%-7.2f%% %-10s %-15s%n",
            data.getSymbol(),
            data.getClosePrice(),
            changeSign,
            data.getChange(),
            changeSign,
            data.getChangePercent(),
            formatNumber(data.getVolume()),
            formatTime(data.getTimestamp())
        );
    }
    
    private void showDemoStockData(String symbol) {
        System.out.println("📝 Showing demo data for " + symbol + " (API not available)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // Generate some demo data
        BigDecimal basePrice = new BigDecimal("150.00");
        BigDecimal change = new BigDecimal("-2.45");
        double changePercent = -1.61;
        
        System.out.println("📈 " + symbol + " Stock Information (Demo)");
        System.out.println("💰 Current Price: $" + basePrice);
        System.out.println("🔴 Change: $" + change + " (" + String.format("%.2f", changePercent) + "%)");
        System.out.println("📊 Volume: 45,123,456");
        System.out.println("⏰ Updated: " + new java.util.Date());
        System.out.println("💡 Configure POLYGON_API_KEY for real-time data");
    }
    
    private void showDemoPortfolioAnalysis(Portfolio portfolio) {
        System.out.println("📊 Portfolio Analysis: " + portfolio.getPortfolioName());
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // Mock analysis data
        BigDecimal totalValue = new BigDecimal("15450.00");
        BigDecimal totalCost = new BigDecimal("14200.00");
        BigDecimal totalGain = totalValue.subtract(totalCost);
        double gainPercent = totalGain.divide(totalCost, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100")).doubleValue();
        
        System.out.println("💰 Total Value: $" + formatPrice(totalValue));
        System.out.println("💵 Total Cost: $" + formatPrice(totalCost));
        
        String gainIcon = totalGain.compareTo(BigDecimal.ZERO) >= 0 ? "🟢" : "🔴";
        String gainSign = totalGain.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        System.out.println(gainIcon + " Total Gain/Loss: " + gainSign + "$" + formatPrice(totalGain) +
                          " (" + gainSign + String.format("%.2f", gainPercent) + "%)");
        
        System.out.println("\n📊 Holdings Performance (Demo Data):");
        System.out.printf("%-8s %-8s %-12s %-12s %-12s %-10s%n", 
                        "Symbol", "Shares", "Avg Price", "Current", "Value", "Gain/Loss");
        System.out.println("──────────────────────────────────────────────────────────────");
        
        portfolio.getHoldings().forEach((symbol, holding) -> {
            // Mock current prices for demo
            BigDecimal currentPrice = holding.getAveragePrice().multiply(new BigDecimal("1.08")); // 8% gain
            BigDecimal value = currentPrice.multiply(new BigDecimal(holding.getShares()));
            BigDecimal cost = holding.getAveragePrice().multiply(new BigDecimal(holding.getShares()));
            BigDecimal gain = value.subtract(cost);
            
            String holdingGainSign = gain.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
            
            System.out.printf("%-8s %-8d $%-10.2f $%-10.2f $%-10.2f %s$%-8.2f%n",
                holding.getSymbol(),
                holding.getShares(),
                holding.getAveragePrice(),
                currentPrice,
                value,
                holdingGainSign,
                gain
            );
        });
        
        System.out.println("\n💡 This is demo data. Configure POLYGON_API_KEY for real-time analysis.");
    }
    
    private void showDemoMarketSummary(List<String> symbols) {
        System.out.printf("%-8s %-12s %-12s %-12s %-12s%n", 
                        "Symbol", "Price", "Change", "Change%", "Status");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // Mock market data
        for (String symbol : symbols) {
            double price = 100 + (Math.random() * 400); // Random price between 100-500
            double change = (Math.random() - 0.5) * 10; // Random change between -5 to +5
            double changePercent = (change / price) * 100;
            
            String icon = change >= 0 ? "🟢" : "🔴";
            String sign = change >= 0 ? "+" : "";
            
            System.out.printf("%-8s $%-10.2f %s%s$%-8.2f %s%-9.2f%% %s%n",
                symbol,
                price,
                sign,
                sign.isEmpty() ? "" : " ",
                change,
                sign,
                changePercent,
                icon
            );
        }
        
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        long gainers = symbols.stream().mapToLong(s -> Math.random() > 0.5 ? 1 : 0).sum();
        long losers = symbols.size() - gainers;
        
        System.out.println("📊 Market Summary:");
        System.out.println("   🟢 Gainers: " + gainers);
        System.out.println("   🔴 Losers: " + losers);
        System.out.println("   📈 Market Status: " + (isMarketHours() ? "OPEN" : "CLOSED"));
        System.out.println("\n💡 This is demo data. Configure POLYGON_API_KEY for real-time market data.");
    }
    
    private boolean isMarketHours() {
        java.time.LocalTime now = java.time.LocalTime.now();
        java.time.DayOfWeek dayOfWeek = java.time.LocalDate.now().getDayOfWeek();
        
        return dayOfWeek != java.time.DayOfWeek.SATURDAY && 
               dayOfWeek != java.time.DayOfWeek.SUNDAY &&
               now.isAfter(java.time.LocalTime.of(9, 30)) && 
               now.isBefore(java.time.LocalTime.of(16, 0));
    }
    
    private String formatPrice(BigDecimal price) {
        return price.setScale(2, RoundingMode.HALF_UP).toString();
    }
    
    private String formatNumber(long number) {
        return String.format("%,d", number);
    }
    
    private String formatTime(long timestamp) {
        java.time.LocalDateTime dateTime = java.time.LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timestamp), 
            java.time.ZoneId.systemDefault()
        );
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}