package io.amcp.examples.meshchat;

import io.amcp.core.*;
import io.amcp.mobility.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;

/**
 * StockAgent - Specialized agent for financial data retrieval, market analysis,
 * stock quotes, and investment insights. Handles finance-related queries from
 * the orchestrator and provides comprehensive financial services.
 */
public class StockAgent implements MobileAgent {
    
    private static final String AGENT_TYPE = "StockAgent";
    private static final String VERSION = "1.0.0";
    
    // Topic patterns for financial services
    private static final String STOCK_REQUEST_TOPIC = "stock.request.**";
    private static final String FINANCE_REQUEST_TOPIC = "finance.request.**";
    private static final String ORCHESTRATOR_RESPONSE_TOPIC = "orchestrator.response";
    
    // Agent state
    private final AgentID agentId;
    private AgentContext context;
    private AgentLifecycle lifecycleState = AgentLifecycle.INACTIVE;
    
    // Polygon.io API integration
    private static final String POLYGON_API_BASE_URL = "https://api.polygon.io";
    private final HttpClient httpClient;
    private final String polygonApiKey;
    private final boolean useRealTimeData;
    
    // Financial data storage
    private final Map<String, StockData> stockDatabase = new HashMap<>();
    private final Map<String, MarketIndex> marketIndices = new HashMap<>();
    private final List<String> financialTips = new ArrayList<>();
    
    public StockAgent() {
        this.agentId = new AgentID();
        
        // Initialize HTTP client for API calls
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
            
        // Get Polygon.io API key from environment
        this.polygonApiKey = System.getenv("POLYGON_API_KEY");
        this.useRealTimeData = polygonApiKey != null && !polygonApiKey.trim().isEmpty();
        
        if (useRealTimeData) {
            logMessage("🔑 Polygon.io API key found - using real-time stock data");
        } else {
            logMessage("⚠️ No Polygon.io API key found - using simulated stock data");
        }
        
        initializeFinancialData();
    }
    
    @Override
    public AgentID getAgentId() {
        return agentId;
    }
    
    @Override
    public AgentContext getContext() {
        return context;
    }
    
    public void setContext(AgentContext context) {
        this.context = context;
    }
    
    @Override
    public AgentLifecycle getLifecycleState() {
        return lifecycleState;
    }
    
    public void setLifecycleState(AgentLifecycle state) {
        this.lifecycleState = state;
    }
    
    @Override
    public void onActivate() {
        try {
            setLifecycleState(AgentLifecycle.ACTIVE);
            logMessage("📈 StockAgent activating...");
            
            // Subscribe to finance-related topics
            subscribe(STOCK_REQUEST_TOPIC);
            subscribe(FINANCE_REQUEST_TOPIC);
            subscribe("orchestrator.task.finance.**");
            subscribe("orchestrator.task.stock.**");
            subscribe("meshchat.finance.**");
            subscribe("meshchat.stock.**");
            
            // Register capabilities with orchestrator
            registerCapabilities();
            
            logMessage("✅ StockAgent activated successfully");
            
        } catch (Exception e) {
            logMessage("❌ Error during activation: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void onDeactivate() {
        logMessage("⏸️ StockAgent deactivating...");
        setLifecycleState(AgentLifecycle.INACTIVE);
    }
    
    @Override
    public void onDestroy() {
        logMessage("🔥 StockAgent destroyed");
        setLifecycleState(AgentLifecycle.DESTROYED);
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                String topic = event.getTopic();
                logMessage("📨 Processing event: " + topic);
                
                if (topic.startsWith("stock.request") || topic.startsWith("finance.request") 
                    || topic.startsWith("orchestrator.task.finance") || topic.startsWith("orchestrator.task.stock")
                    || topic.startsWith("meshchat.finance") || topic.startsWith("meshchat.stock")) {
                    handleFinanceRequest(event);
                } else {
                    logMessage("⚠️ Unhandled topic: " + topic);
                }
                
            } catch (Exception e) {
                logMessage("❌ Error handling event: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    @Override
    public MobilityStrategy getMobilityStrategy() {
        return null;
    }
    
    // Required Agent interface methods
    @Override
    public CompletableFuture<Void> subscribe(String topicPattern) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (getContext() != null) {
                    getContext().subscribe(getAgentId(), topicPattern);
                    logMessage("📡 Subscribed to: " + topicPattern);
                }
            } catch (Exception e) {
                logMessage("❌ Failed to subscribe to " + topicPattern + ": " + e.getMessage());
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> unsubscribe(String topicPattern) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (getContext() != null) {
                    getContext().unsubscribe(getAgentId(), topicPattern);
                    logMessage("📡 Unsubscribed from: " + topicPattern);
                }
            } catch (Exception e) {
                logMessage("❌ Failed to unsubscribe from " + topicPattern + ": " + e.getMessage());
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> publishEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (getContext() != null) {
                    getContext().publishEvent(event);
                }
            } catch (Exception e) {
                logMessage("❌ Failed to publish event: " + e.getMessage());
            }
        });
    }
    
    @Override
    public void onBeforeMigration(String destinationContext) {
        logMessage("⚠️ Preparing for migration to: " + destinationContext);
    }
    
    @Override
    public void onAfterMigration(String sourceContext) {
        logMessage("✅ Migration completed from: " + sourceContext);
    }
    
    // Required MobileAgent interface methods
    @Override
    public CompletableFuture<Void> dispatch(String destinationContext) {
        return CompletableFuture.runAsync(() -> {
            logMessage("🚀 Dispatching to: " + destinationContext);
        });
    }
    
    @Override
    public CompletableFuture<AgentID> clone(String destinationContext) {
        return CompletableFuture.supplyAsync(() -> {
            logMessage("👥 Cloning to: " + destinationContext);
            return new AgentID();
        });
    }
    
    @Override
    public CompletableFuture<Void> retract(String sourceContext) {
        return CompletableFuture.runAsync(() -> {
            logMessage("↩️ Retracting from: " + sourceContext);
        });
    }
    
    @Override
    public CompletableFuture<Void> migrate(MigrationOptions options) {
        return CompletableFuture.runAsync(() -> {
            logMessage("🔄 Migrating with options: " + options);
        });
    }
    
    @Override
    public CompletableFuture<List<AgentID>> replicate(String... contexts) {
        return CompletableFuture.supplyAsync(() -> {
            logMessage("📋 Replicating to contexts: " + Arrays.toString(contexts));
            return Arrays.asList(new AgentID());
        });
    }
    
    @Override
    public CompletableFuture<Void> federateWith(List<AgentID> agents, String federationId) {
        return CompletableFuture.runAsync(() -> {
            logMessage("🤝 Federating with agents: " + agents + " in federation: " + federationId);
        });
    }
    
    @Override
    public MobilityState getMobilityState() {
        return MobilityState.STATIONARY;
    }
    
    @Override
    public List<MigrationEvent> getMigrationHistory() {
        return new ArrayList<>();
    }
    
    @Override
    public CompletableFuture<MobilityAssessment> assessMobility(String destinationContext) {
        return CompletableFuture.supplyAsync(() -> {
            return null;
        });
    }
    
    @Override
    public void setMobilityStrategy(MobilityStrategy strategy) {
        // Implementation would set mobility strategy
    }
    
    /**
     * Handles finance-related requests from various sources
     */
    private void handleFinanceRequest(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            
            String query = (String) payload.get("query");
            String taskType = (String) payload.get("taskType");
            String correlationId = event.getCorrelationId();
            
            if (query == null) {
                query = (String) payload.get("message");
            }
            
            if (query == null) {
                logMessage("⚠️ No query found in finance request");
                return;
            }
            
            logMessage("💰 Processing finance query: " + query);
            
            // Analyze query and determine financial service needed
            FinanceResponse response = processFinanceQuery(query, taskType);
            
            // Send response back
            sendFinanceResponse(response, correlationId, event.getMetadata());
            
        } catch (Exception e) {
            logMessage("❌ Error processing finance request: " + e.getMessage());
            sendErrorResponse(event.getCorrelationId(), "Failed to process finance request: " + e.getMessage());
        }
    }
    
    /**
     * Processes finance query and returns appropriate response
     */
    private FinanceResponse processFinanceQuery(String query, String taskType) {
        String lowerQuery = query.toLowerCase();
        
        // Stock quotes and prices
        if (lowerQuery.contains("stock") && (lowerQuery.contains("price") || lowerQuery.contains("quote") 
            || lowerQuery.contains("value"))) {
            return generateStockQuote(query);
        }
        
        // Market analysis
        if (lowerQuery.contains("market") && (lowerQuery.contains("analysis") || lowerQuery.contains("trend") 
            || lowerQuery.contains("outlook"))) {
            return generateMarketAnalysis(query);
        }
        
        // Investment advice
        if (lowerQuery.contains("invest") || lowerQuery.contains("portfolio") 
            || lowerQuery.contains("buy") || lowerQuery.contains("sell")) {
            return generateInvestmentAdvice(query);
        }
        
        // Financial news and updates
        if (lowerQuery.contains("news") || lowerQuery.contains("update") 
            || lowerQuery.contains("latest") || lowerQuery.contains("recent")) {
            return generateFinancialNews(query);
        }
        
        // Economic indicators
        if (lowerQuery.contains("economic") || lowerQuery.contains("inflation") 
            || lowerQuery.contains("gdp") || lowerQuery.contains("unemployment")) {
            return generateEconomicIndicators(query);
        }
        
        // Cryptocurrency
        if (lowerQuery.contains("crypto") || lowerQuery.contains("bitcoin") 
            || lowerQuery.contains("ethereum") || lowerQuery.contains("blockchain")) {
            return generateCryptoAnalysis(query);
        }
        
        // Financial planning
        if (lowerQuery.contains("plan") || lowerQuery.contains("retire") 
            || lowerQuery.contains("save") || lowerQuery.contains("budget")) {
            return generateFinancialPlanning(query);
        }
        
        // General financial assistance
        return generateGeneralFinanceAssistance(query);
    }
    
    /**
     * Generates stock quote information
     */
    private FinanceResponse generateStockQuote(String query) {
        StringBuilder response = new StringBuilder();
        response.append("📈 **Stock Market Information**\n\n");
        
        // Extract ticker symbols if mentioned
        List<String> tickers = extractTickerSymbols(query);
        
        if (tickers.isEmpty()) {
            // Show popular stocks
            tickers = Arrays.asList("AAPL", "GOOGL", "MSFT", "TSLA", "AMZN");
            response.append("**Popular Stock Quotes:**\n");
        } else {
            response.append("**Requested Stock Quotes:**\n");
        }
        
        for (String ticker : tickers) {
            StockData stock = null;
            
            // Try to get real-time data first
            if (useRealTimeData) {
                try {
                    stock = fetchRealTimeStockData(ticker);
                    logMessage("📊 Retrieved real-time data for " + ticker);
                } catch (Exception e) {
                    logMessage("⚠️ Failed to fetch real-time data for " + ticker + ": " + e.getMessage());
                }
            }
            
            // Fallback to simulated data if real-time fails
            if (stock == null) {
                stock = stockDatabase.get(ticker);
                if (stock != null) {
                    logMessage("📉 Using simulated data for " + ticker);
                }
            }
            
            if (stock != null) {
                response.append("🏢 **").append(ticker).append(" - ").append(stock.getCompanyName()).append("**\n");
                response.append("   Current Price: $").append(stock.getCurrentPrice()).append("\n");
                response.append("   Daily Change: ").append(stock.getDailyChangeFormatted()).append("\n");
                response.append("   Volume: ").append(stock.getFormattedVolume()).append("\n");
                response.append("   52-Week Range: $").append(stock.getWeekLow52()).append(" - $").append(stock.getWeekHigh52()).append("\n\n");
            }
        }
        
        if (useRealTimeData) {
            response.append("📡 **Real-time data provided by Polygon.io**\n");
        } else {
            response.append("💡 **Note:** Stock prices are simulated for demonstration purposes.\n");
        }
        response.append("For real trading, please consult a licensed financial advisor and use actual market data.");
        
        return new FinanceResponse("stock_quotes", response.toString());
    }
    
    /**
     * Generates market analysis
     */
    private FinanceResponse generateMarketAnalysis(String query) {
        StringBuilder response = new StringBuilder();
        response.append("📊 **Market Analysis**\n\n");
        
        response.append("**Major Market Indices:**\n");
        for (MarketIndex index : marketIndices.values()) {
            response.append("📈 **").append(index.getName()).append("**\n");
            response.append("   Current Level: ").append(index.getCurrentLevel()).append("\n");
            response.append("   Daily Change: ").append(index.getDailyChangeFormatted()).append("\n");
            response.append("   YTD Performance: ").append(index.getYtdPerformanceFormatted()).append("\n\n");
        }
        
        response.append("**Market Sentiment:**\n");
        response.append("• Overall market shows mixed signals with moderate volatility\n");
        response.append("• Technology sector continues to show resilience\n");
        response.append("• Energy and financial sectors showing rotation patterns\n");
        response.append("• Inflation concerns remain a key market driver\n\n");
        
        response.append("**Key Factors to Watch:**\n");
        response.append("• Federal Reserve policy decisions\n");
        response.append("• Corporate earnings reports\n");
        response.append("• Geopolitical developments\n");
        response.append("• Economic data releases\n\n");
        
        response.append("⚠️ **Disclaimer:** This analysis is for educational purposes only and should not be considered investment advice.");
        
        return new FinanceResponse("market_analysis", response.toString());
    }
    
    /**
     * Generates investment advice
     */
    private FinanceResponse generateInvestmentAdvice(String query) {
        StringBuilder response = new StringBuilder();
        response.append("💼 **Investment Guidance**\n\n");
        
        response.append("**General Investment Principles:**\n");
        response.append("• **Diversification:** Spread investments across different asset classes\n");
        response.append("• **Risk Tolerance:** Align investments with your risk capacity\n");
        response.append("• **Time Horizon:** Consider your investment timeline\n");
        response.append("• **Regular Review:** Periodically assess and rebalance portfolio\n\n");
        
        response.append("**Asset Allocation Suggestions:**\n");
        response.append("**Conservative Portfolio (Low Risk):**\n");
        response.append("• 60% Bonds and Fixed Income\n");
        response.append("• 30% Large-Cap Stocks\n");
        response.append("• 10% Cash and Equivalents\n\n");
        
        response.append("**Moderate Portfolio (Medium Risk):**\n");
        response.append("• 40% Stocks (Large & Mid-Cap)\n");
        response.append("• 40% Bonds and Fixed Income\n");
        response.append("• 15% International Stocks\n");
        response.append("• 5% Alternative Investments\n\n");
        
        response.append("**Aggressive Portfolio (High Risk):**\n");
        response.append("• 70% Stocks (All Cap Sizes)\n");
        response.append("• 15% International Stocks\n");
        response.append("• 10% Bonds\n");
        response.append("• 5% Alternative Investments\n\n");
        
        response.append("**Investment Tips:**\n");
        Collections.shuffle(financialTips);
        for (int i = 0; i < Math.min(5, financialTips.size()); i++) {
            response.append("• ").append(financialTips.get(i)).append("\n");
        }
        
        response.append("\n⚠️ **Important:** This is general guidance only. Please consult with a qualified financial advisor for personalized investment advice.");
        
        return new FinanceResponse("investment_advice", response.toString());
    }
    
    /**
     * Generates financial news and updates
     */
    private FinanceResponse generateFinancialNews(String query) {
        StringBuilder response = new StringBuilder();
        response.append("📰 **Financial News & Updates**\n\n");
        
        response.append("**Market Headlines:**\n");
        response.append("• Tech stocks show mixed performance amid earning season\n");
        response.append("• Federal Reserve signals potential rate adjustments\n");
        response.append("• Oil prices fluctuate on supply chain concerns\n");
        response.append("• Cryptocurrency market experiences continued volatility\n");
        response.append("• Consumer confidence index shows moderate improvement\n\n");
        
        response.append("**Sector Performance:**\n");
        response.append("📈 **Top Performers:**\n");
        response.append("• Healthcare: +2.3%\n");
        response.append("• Technology: +1.8%\n");
        response.append("• Consumer Staples: +1.2%\n\n");
        
        response.append("📉 **Underperformers:**\n");
        response.append("• Energy: -1.5%\n");
        response.append("• Real Estate: -0.8%\n");
        response.append("• Utilities: -0.3%\n\n");
        
        response.append("**Upcoming Events:**\n");
        response.append("• Federal Reserve meeting next week\n");
        response.append("• Major tech earnings reports\n");
        response.append("• Monthly employment data release\n");
        response.append("• GDP growth rate announcement\n\n");
        
        response.append("💡 **Note:** Financial news is simulated for demonstration purposes. For real-time updates, consult reliable financial news sources.");
        
        return new FinanceResponse("financial_news", response.toString());
    }
    
    /**
     * Generates economic indicators information
     */
    private FinanceResponse generateEconomicIndicators(String query) {
        StringBuilder response = new StringBuilder();
        response.append("📊 **Economic Indicators**\n\n");
        
        response.append("**Key Economic Metrics:**\n");
        response.append("🏛️ **GDP Growth Rate:** 2.3% (Annual)\n");
        response.append("📈 **Inflation Rate (CPI):** 3.1% (Year-over-Year)\n");
        response.append("👥 **Unemployment Rate:** 3.8%\n");
        response.append("🏠 **Housing Starts:** 1.35M (Annualized)\n");
        response.append("🏭 **Industrial Production:** +0.8% (Monthly)\n");
        response.append("💰 **Consumer Confidence:** 104.5\n\n");
        
        response.append("**Federal Reserve Data:**\n");
        response.append("• Federal Funds Rate: 5.25% - 5.50%\n");
        response.append("• 10-Year Treasury Yield: 4.35%\n");
        response.append("• Money Supply Growth: 2.1%\n\n");
        
        response.append("**International Indicators:**\n");
        response.append("• US Dollar Index: 103.2\n");
        response.append("• Gold Price: $1,985/oz\n");
        response.append("• Crude Oil (WTI): $78.50/barrel\n\n");
        
        response.append("**Economic Outlook:**\n");
        response.append("• Moderate growth expected to continue\n");
        response.append("• Inflation showing signs of moderation\n");
        response.append("• Labor market remains resilient\n");
        response.append("• Consumer spending patterns shifting\n\n");
        
        response.append("⚠️ **Note:** Economic indicators are simulated for demonstration purposes.");
        
        return new FinanceResponse("economic_indicators", response.toString());
    }
    
    /**
     * Generates cryptocurrency analysis
     */
    private FinanceResponse generateCryptoAnalysis(String query) {
        StringBuilder response = new StringBuilder();
        response.append("₿ **Cryptocurrency Analysis**\n\n");
        
        response.append("**Major Cryptocurrencies:**\n");
        response.append("₿ **Bitcoin (BTC)**\n");
        response.append("   Price: $42,350\n");
        response.append("   24h Change: +2.1%\n");
        response.append("   Market Cap: $828B\n\n");
        
        response.append("Ξ **Ethereum (ETH)**\n");
        response.append("   Price: $2,485\n");
        response.append("   24h Change: +1.8%\n");
        response.append("   Market Cap: $298B\n\n");
        
        response.append("🏠 **Market Analysis:**\n");
        response.append("• Total crypto market cap: $1.65T\n");
        response.append("• Bitcoin dominance: 50.2%\n");
        response.append("• 24h trading volume: $58B\n");
        response.append("• Fear & Greed Index: 61 (Greed)\n\n");
        
        response.append("**Key Trends:**\n");
        response.append("• Institutional adoption continues to grow\n");
        response.append("• Regulatory clarity improving in major markets\n");
        response.append("• DeFi protocols showing innovation\n");
        response.append("• NFT market experiencing normalization\n\n");
        
        response.append("**Risk Considerations:**\n");
        response.append("• High volatility and price swings\n");
        response.append("• Regulatory uncertainty in some regions\n");
        response.append("• Technology and security risks\n");
        response.append("• Market manipulation concerns\n\n");
        
        response.append("⚠️ **Warning:** Cryptocurrency investments are highly speculative and risky. Only invest what you can afford to lose.");
        
        return new FinanceResponse("crypto_analysis", response.toString());
    }
    
    /**
     * Generates financial planning advice
     */
    private FinanceResponse generateFinancialPlanning(String query) {
        StringBuilder response = new StringBuilder();
        response.append("📋 **Financial Planning Guide**\n\n");
        
        response.append("**Financial Planning Steps:**\n");
        response.append("1️⃣ **Set Financial Goals**\n");
        response.append("   • Short-term (1-2 years): Emergency fund, vacation\n");
        response.append("   • Medium-term (3-5 years): Home down payment, car\n");
        response.append("   • Long-term (5+ years): Retirement, children's education\n\n");
        
        response.append("2️⃣ **Create a Budget**\n");
        response.append("   • Track income and expenses\n");
        response.append("   • Use 50/30/20 rule: Needs/Wants/Savings\n");
        response.append("   • Identify areas to reduce spending\n\n");
        
        response.append("3️⃣ **Build Emergency Fund**\n");
        response.append("   • Save 3-6 months of expenses\n");
        response.append("   • Keep in high-yield savings account\n");
        response.append("   • Use only for true emergencies\n\n");
        
        response.append("4️⃣ **Manage Debt**\n");
        response.append("   • Pay off high-interest debt first\n");
        response.append("   • Consider debt consolidation\n");
        response.append("   • Avoid unnecessary new debt\n\n");
        
        response.append("5️⃣ **Invest for the Future**\n");
        response.append("   • Start with employer 401(k) match\n");
        response.append("   • Open IRA or Roth IRA\n");
        response.append("   • Diversify across asset classes\n\n");
        
        response.append("**Retirement Planning:**\n");
        response.append("• Contribute to employer-sponsored plans\n");
        response.append("• Take advantage of tax-advantaged accounts\n");
        response.append("• Consider target-date funds for simplicity\n");
        response.append("• Review and adjust annually\n\n");
        
        response.append("💡 **Pro Tip:** Start early and be consistent. Time and compound interest are your best friends in wealth building!");
        
        return new FinanceResponse("financial_planning", response.toString());
    }
    
    /**
     * Generates general financial assistance
     */
    private FinanceResponse generateGeneralFinanceAssistance(String query) {
        StringBuilder response = new StringBuilder();
        response.append("💰 **Financial Assistance**\n\n");
        
        response.append("I'm here to help with all your financial questions! I can assist with:\n\n");
        
        response.append("📈 **Stock Market & Investments**\n");
        response.append("• Stock quotes and market data\n");
        response.append("• Investment strategies and portfolio advice\n");
        response.append("• Market analysis and trends\n\n");
        
        response.append("📊 **Economic Information**\n");
        response.append("• Economic indicators and reports\n");
        response.append("• Federal Reserve policy updates\n");
        response.append("• International market news\n\n");
        
        response.append("₿ **Cryptocurrency**\n");
        response.append("• Crypto market analysis\n");
        response.append("• Price tracking and trends\n");
        response.append("• Blockchain technology insights\n\n");
        
        response.append("📋 **Financial Planning**\n");
        response.append("• Budgeting and saving strategies\n");
        response.append("• Retirement planning guidance\n");
        response.append("• Debt management advice\n\n");
        
        response.append("📰 **Market News & Updates**\n");
        response.append("• Latest financial news\n");
        response.append("• Sector performance updates\n");
        response.append("• Upcoming economic events\n\n");
        
        response.append("Feel free to ask me specific questions about any financial topic!");
        
        return new FinanceResponse("general_assistance", response.toString());
    }
    
    /**
     * Fetches real-time stock data from Polygon.io API
     */
    private StockData fetchRealTimeStockData(String ticker) throws Exception {
        if (!useRealTimeData) {
            throw new IllegalStateException("Real-time data not available");
        }
        
        // Get current quote
        String quoteUrl = POLYGON_API_BASE_URL + "/v2/snapshot/locale/us/markets/stocks/tickers/" + ticker + "?apikey=" + polygonApiKey;
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(quoteUrl))
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();
            
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("API request failed with status: " + response.statusCode() + " - " + response.body());
        }
        
        return parsePolygonResponse(response.body(), ticker);
    }
    
    /**
     * Parses Polygon.io API response and creates StockData object
     */
    private StockData parsePolygonResponse(String jsonResponse, String ticker) {
        try {
            // Simple JSON parsing for Polygon.io snapshot response
            // Expected format: {"results":{"ticker":"AAPL","day":{"c":182.52,"h":184.30,"l":181.50,"o":183.20,"v":45250000},...}}
            
            String companyName = getCompanyNameForTicker(ticker);
            
            // Extract current price from "day.c" (close price)
            BigDecimal currentPrice = extractJsonNumber(jsonResponse, "\"c\":");
            if (currentPrice == null) {
                throw new RuntimeException("Could not parse current price from response");
            }
            
            // Extract volume from "day.v"
            BigDecimal volumeDecimal = extractJsonNumber(jsonResponse, "\"v\":");
            Long volume = volumeDecimal != null ? volumeDecimal.longValue() : 0L;
            
            // Extract high/low from "day.h" and "day.l"
            BigDecimal dayHigh = extractJsonNumber(jsonResponse, "\"h\":");
            BigDecimal dayLow = extractJsonNumber(jsonResponse, "\"l\":");
            BigDecimal openPrice = extractJsonNumber(jsonResponse, "\"o\":");
            
            // Calculate daily change
            BigDecimal dailyChange = BigDecimal.ZERO;
            BigDecimal dailyChangePercent = BigDecimal.ZERO;
            
            if (openPrice != null && openPrice.compareTo(BigDecimal.ZERO) > 0) {
                dailyChange = currentPrice.subtract(openPrice);
                dailyChangePercent = dailyChange.divide(openPrice, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
            }
            
            // Use fallback values for 52-week range if not in snapshot
            BigDecimal weekLow52 = dayLow != null ? dayLow : currentPrice.multiply(new BigDecimal("0.8"));
            BigDecimal weekHigh52 = dayHigh != null ? dayHigh : currentPrice.multiply(new BigDecimal("1.2"));
            
            return new StockData(ticker, companyName, currentPrice, dailyChange, dailyChangePercent, 
                               volume, weekLow52, weekHigh52);
                               
        } catch (Exception e) {
            logMessage("❌ Error parsing Polygon.io response: " + e.getMessage());
            throw new RuntimeException("Failed to parse API response", e);
        }
    }
    
    /**
     * Extracts a numeric value from JSON string
     */
    private BigDecimal extractJsonNumber(String json, String key) {
        try {
            int startIndex = json.indexOf(key);
            if (startIndex == -1) return null;
            
            startIndex += key.length();
            int endIndex = startIndex;
            
            // Find the end of the number (comma, bracket, or brace)
            while (endIndex < json.length()) {
                char c = json.charAt(endIndex);
                if (c == ',' || c == '}' || c == ']') {
                    break;
                }
                endIndex++;
            }
            
            String numberStr = json.substring(startIndex, endIndex).trim();
            return new BigDecimal(numberStr);
            
        } catch (Exception e) {
            logMessage("⚠️ Failed to extract " + key + " from JSON: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets company name for ticker symbol
     */
    private String getCompanyNameForTicker(String ticker) {
        Map<String, String> companyNames = Map.of(
            "AAPL", "Apple Inc.",
            "GOOGL", "Alphabet Inc.",
            "MSFT", "Microsoft Corporation",
            "TSLA", "Tesla, Inc.",
            "AMZN", "Amazon.com, Inc.",
            "NVDA", "NVIDIA Corporation",
            "META", "Meta Platforms, Inc.",
            "NFLX", "Netflix, Inc.",
            "SPOT", "Spotify Technology S.A.",
            "UBER", "Uber Technologies, Inc."
        );
        
        return companyNames.getOrDefault(ticker, ticker + " Corp.");
    }
    
    /**
     * Extracts ticker symbols from query text
     */
    private List<String> extractTickerSymbols(String query) {
        List<String> tickers = new ArrayList<>();
        String upperQuery = query.toUpperCase();
        
        // Check for common ticker symbols
        for (String ticker : stockDatabase.keySet()) {
            if (upperQuery.contains(ticker)) {
                tickers.add(ticker);
            }
        }
        
        return tickers;
    }
    
    /**
     * Sends finance response back to the requestor
     */
    private void sendFinanceResponse(FinanceResponse financeResponse, String correlationId, Map<String, Object> originalMetadata) {
        try {
            Map<String, Object> responsePayload = new HashMap<>();
            responsePayload.put("response", financeResponse.getContent());
            responsePayload.put("serviceType", financeResponse.getServiceType());
            responsePayload.put("agentType", AGENT_TYPE);
            responsePayload.put("timestamp", LocalDateTime.now().toString());
            responsePayload.put("success", true);
            
            // Determine response topic based on original request
            String responseTopic = ORCHESTRATOR_RESPONSE_TOPIC;
            if (originalMetadata != null && "meshchat".equals(originalMetadata.get("source"))) {
                responseTopic = "meshchat.agent.response";
            }
            
            Event responseEvent = Event.builder()
                .topic(responseTopic)
                .payload(responsePayload)
                .correlationId(correlationId)
                .metadata("source", "stock_agent")
                .metadata("agentId", agentId.toString())
                .build();
            
            publishEvent(responseEvent);
            
            logMessage("📤 Sent finance response [" + correlationId + "]");
            
        } catch (Exception e) {
            logMessage("❌ Error sending finance response: " + e.getMessage());
        }
    }
    
    /**
     * Sends error response
     */
    private void sendErrorResponse(String correlationId, String errorMessage) {
        try {
            Map<String, Object> errorPayload = new HashMap<>();
            errorPayload.put("response", "❌ " + errorMessage);
            errorPayload.put("success", false);
            errorPayload.put("error", true);
            errorPayload.put("agentType", AGENT_TYPE);
            
            Event errorEvent = Event.builder()
                .topic(ORCHESTRATOR_RESPONSE_TOPIC)
                .payload(errorPayload)
                .correlationId(correlationId)
                .build();
            
            publishEvent(errorEvent);
            
        } catch (Exception e) {
            logMessage("❌ Error sending error response: " + e.getMessage());
        }
    }
    
    /**
     * Registers agent capabilities with the orchestrator
     */
    private void registerCapabilities() {
        try {
            Map<String, Object> capabilities = new HashMap<>();
            capabilities.put("stock.quotes.prices", "Stock quotes and current market prices");
            capabilities.put("market.analysis.trends", "Market analysis and trend identification");
            capabilities.put("investment.advice.portfolio", "Investment guidance and portfolio recommendations");
            capabilities.put("finance.news.updates", "Financial news and market updates");
            capabilities.put("economic.indicators.data", "Economic indicators and government data");
            capabilities.put("crypto.analysis.prices", "Cryptocurrency analysis and pricing");
            capabilities.put("financial.planning.advice", "Financial planning and retirement guidance");
            
            Map<String, Object> registrationData = new HashMap<>();
            registrationData.put("agentId", agentId.toString());
            registrationData.put("agentType", AGENT_TYPE);
            registrationData.put("version", VERSION);
            registrationData.put("capabilities", capabilities);
            registrationData.put("status", "active");
            registrationData.put("specialization", "financial_services");
            registrationData.put("timestamp", LocalDateTime.now().toString());
            
            Event registrationEvent = Event.builder()
                .topic("registry.agent.register")
                .payload(registrationData)
                .correlationId("stock-registration-" + System.currentTimeMillis())
                .build();
            
            publishEvent(registrationEvent);
            
            logMessage("📋 Registered financial services capabilities");
            
        } catch (Exception e) {
            logMessage("⚠️ Failed to register capabilities: " + e.getMessage());
        }
    }
    
    /**
     * Initialize financial data for demonstration
     */
    private void initializeFinancialData() {
        // Initialize stock data with simulated prices
        stockDatabase.put("AAPL", new StockData("AAPL", "Apple Inc.", new BigDecimal("182.52"), 
            new BigDecimal("2.35"), new BigDecimal("1.31"), 45250000L, new BigDecimal("164.08"), new BigDecimal("199.62")));
            
        stockDatabase.put("GOOGL", new StockData("GOOGL", "Alphabet Inc.", new BigDecimal("138.21"), 
            new BigDecimal("-1.85"), new BigDecimal("-1.32"), 28750000L, new BigDecimal("83.34"), new BigDecimal("151.55")));
            
        stockDatabase.put("MSFT", new StockData("MSFT", "Microsoft Corporation", new BigDecimal("378.85"), 
            new BigDecimal("4.72"), new BigDecimal("1.26"), 22180000L, new BigDecimal("213.43"), new BigDecimal("384.52")));
            
        stockDatabase.put("TSLA", new StockData("TSLA", "Tesla, Inc.", new BigDecimal("248.50"), 
            new BigDecimal("-8.25"), new BigDecimal("-3.21"), 68930000L, new BigDecimal("101.81"), new BigDecimal("293.34")));
            
        stockDatabase.put("AMZN", new StockData("AMZN", "Amazon.com, Inc.", new BigDecimal("145.86"), 
            new BigDecimal("1.92"), new BigDecimal("1.33"), 34520000L, new BigDecimal("81.43"), new BigDecimal("170.00")));
            
        stockDatabase.put("NVDA", new StockData("NVDA", "NVIDIA Corporation", new BigDecimal("722.48"), 
            new BigDecimal("15.82"), new BigDecimal("2.24"), 41280000L, new BigDecimal("180.96"), new BigDecimal("974.00")));
            
        stockDatabase.put("META", new StockData("META", "Meta Platforms, Inc.", new BigDecimal("338.54"), 
            new BigDecimal("6.73"), new BigDecimal("2.03"), 25640000L, new BigDecimal("88.09"), new BigDecimal("384.33")));
        
        // Initialize market indices
        marketIndices.put("SPX", new MarketIndex("S&P 500", new BigDecimal("4567.18"), 
            new BigDecimal("23.45"), new BigDecimal("0.52"), new BigDecimal("12.8")));
            
        marketIndices.put("IXIC", new MarketIndex("NASDAQ Composite", new BigDecimal("14329.76"), 
            new BigDecimal("89.27"), new BigDecimal("0.63"), new BigDecimal("15.2")));
            
        marketIndices.put("DJI", new MarketIndex("Dow Jones Industrial Average", new BigDecimal("35742.10"), 
            new BigDecimal("124.75"), new BigDecimal("0.35"), new BigDecimal("8.9")));
        
        // Initialize financial tips
        financialTips.addAll(Arrays.asList(
            "Start investing early to take advantage of compound interest",
            "Diversify your portfolio across different asset classes",
            "Don't try to time the market - consistency beats timing",
            "Keep investment costs low with index funds and ETFs",
            "Rebalance your portfolio annually to maintain target allocation",
            "Invest in what you understand and research thoroughly",
            "Keep 3-6 months of expenses in an emergency fund",
            "Max out employer 401(k) match - it's free money",
            "Consider tax-advantaged accounts like IRAs and HSAs",
            "Don't let emotions drive your investment decisions",
            "Regular investing through dollar-cost averaging reduces risk",
            "Review and update your financial goals annually",
            "Understand the fees and expenses of your investments",
            "Consider your risk tolerance and time horizon",
            "Stay informed but don't obsess over daily market movements"
        ));
    }
    
    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [StockAgent] " + message);
    }
    
    /**
     * Stock data class for storing stock information
     */
    public static class StockData {
        private final String symbol;
        private final String companyName;
        private final BigDecimal currentPrice;
        private final BigDecimal dailyChange;
        private final BigDecimal dailyChangePercent;
        private final Long volume;
        private final BigDecimal weekLow52;
        private final BigDecimal weekHigh52;
        
        public StockData(String symbol, String companyName, BigDecimal currentPrice, 
                        BigDecimal dailyChange, BigDecimal dailyChangePercent, Long volume,
                        BigDecimal weekLow52, BigDecimal weekHigh52) {
            this.symbol = symbol;
            this.companyName = companyName;
            this.currentPrice = currentPrice;
            this.dailyChange = dailyChange;
            this.dailyChangePercent = dailyChangePercent;
            this.volume = volume;
            this.weekLow52 = weekLow52;
            this.weekHigh52 = weekHigh52;
        }
        
        public String getSymbol() { return symbol; }
        public String getCompanyName() { return companyName; }
        public BigDecimal getCurrentPrice() { return currentPrice; }
        public BigDecimal getDailyChange() { return dailyChange; }
        public BigDecimal getDailyChangePercent() { return dailyChangePercent; }
        public Long getVolume() { return volume; }
        public BigDecimal getWeekLow52() { return weekLow52; }
        public BigDecimal getWeekHigh52() { return weekHigh52; }
        
        public String getDailyChangeFormatted() {
            String sign = dailyChange.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
            return sign + dailyChange.setScale(2, RoundingMode.HALF_UP) + " (" + 
                   sign + dailyChangePercent.setScale(2, RoundingMode.HALF_UP) + "%)";
        }
        
        public String getFormattedVolume() {
            if (volume >= 1000000) {
                return String.format("%.1fM", volume / 1000000.0);
            } else if (volume >= 1000) {
                return String.format("%.1fK", volume / 1000.0);
            }
            return volume.toString();
        }
    }
    
    /**
     * Market index data class
     */
    public static class MarketIndex {
        private final String name;
        private final BigDecimal currentLevel;
        private final BigDecimal dailyChange;
        private final BigDecimal dailyChangePercent;
        private final BigDecimal ytdPerformance;
        
        public MarketIndex(String name, BigDecimal currentLevel, BigDecimal dailyChange,
                          BigDecimal dailyChangePercent, BigDecimal ytdPerformance) {
            this.name = name;
            this.currentLevel = currentLevel;
            this.dailyChange = dailyChange;
            this.dailyChangePercent = dailyChangePercent;
            this.ytdPerformance = ytdPerformance;
        }
        
        public String getName() { return name; }
        public BigDecimal getCurrentLevel() { return currentLevel; }
        public BigDecimal getDailyChange() { return dailyChange; }
        public BigDecimal getDailyChangePercent() { return dailyChangePercent; }
        public BigDecimal getYtdPerformance() { return ytdPerformance; }
        
        public String getDailyChangeFormatted() {
            String sign = dailyChange.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
            return sign + dailyChange.setScale(2, RoundingMode.HALF_UP) + " (" + 
                   sign + dailyChangePercent.setScale(2, RoundingMode.HALF_UP) + "%)";
        }
        
        public String getYtdPerformanceFormatted() {
            String sign = ytdPerformance.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
            return sign + ytdPerformance.setScale(1, RoundingMode.HALF_UP) + "%";
        }
    }
    
    /**
     * Finance response data class
     */
    public static class FinanceResponse {
        private final String serviceType;
        private final String content;
        
        public FinanceResponse(String serviceType, String content) {
            this.serviceType = serviceType;
            this.content = content;
        }
        
        public String getServiceType() { return serviceType; }
        public String getContent() { return content; }
    }
}