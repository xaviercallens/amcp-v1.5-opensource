package io.amcp.examples.stockprice;

import io.amcp.core.Agent;
import io.amcp.core.AgentID;
import io.amcp.core.AgentContext;
import io.amcp.core.AgentLifecycle;
import io.amcp.core.Event;
import io.amcp.core.DeliveryOptions;
import io.amcp.tools.ToolManager;
import io.amcp.tools.ToolResponse;
import io.amcp.mobility.MobilityState;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * AMCP v1.5 Enterprise Edition Stock Price Agent.
 * Advanced example agent demonstrating comprehensive stock market data integration
 * with Polygon.io API, real-time price monitoring, portfolio tracking, and market alerts.
 */
public class StockPriceAgent implements Agent {
    
    private AgentID agentId;
    private AgentContext context;
    private AgentLifecycle state = AgentLifecycle.INACTIVE;
    
    private final Map<String, StockAlert> activeAlerts = new ConcurrentHashMap<>();
    private final Map<String, StockData> stockCache = new ConcurrentHashMap<>();
    private final Map<String, Portfolio> portfolios = new ConcurrentHashMap<>();
    private final Map<String, StockRequest> pendingRequests = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong(0);
    private final ReentrantReadWriteLock stateLock = new ReentrantReadWriteLock();
    
    private ScheduledExecutorService scheduler;
    private ToolManager toolManager;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;
    private volatile boolean initialized = false;
    
    // Polygon.io API configuration
    private static final String POLYGON_BASE_URL = "https://api.polygon.io";
    private static final String DEFAULT_API_KEY = "ZGgVNySPtrCA7u1knnya3wdefCLGpJwd"; // Demo API key
    private static final String DEFAULT_AGENT_NAME = "trusting_mestorf";
    private String apiKey;
    
    // Market hours (EST) - simplified for demo
    private static final int MARKET_OPEN_HOUR = 9;
    private static final int MARKET_CLOSE_HOUR = 16;
    
    public StockPriceAgent() {
        this.agentId = AgentID.named(DEFAULT_AGENT_NAME + "-" + System.currentTimeMillis());
        this.apiKey = System.getenv("POLYGON_API_KEY") != null 
            ? System.getenv("POLYGON_API_KEY") 
            : DEFAULT_API_KEY;
    }
    
    public StockPriceAgent(AgentID agentId) {
        this.agentId = agentId;
        this.apiKey = System.getenv("POLYGON_API_KEY") != null 
            ? System.getenv("POLYGON_API_KEY") 
            : DEFAULT_API_KEY;
    }
    
    public StockPriceAgent(AgentID agentId, String polygonApiKey) {
        this.agentId = agentId;
        this.apiKey = polygonApiKey;
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
        return state;
    }
    
    @Override
    public void onActivate() {
        state = AgentLifecycle.ACTIVE;
        
        // Initialize HTTP client and JSON mapper
        httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();
        objectMapper = new ObjectMapper();
        
        // Initialize scheduler for periodic tasks
        scheduler = Executors.newScheduledThreadPool(3, r -> {
            Thread t = new Thread(r, "StockPrice-" + agentId.toString());
            t.setDaemon(true);
            return t;
        });
        
        // Initialize tool manager
        toolManager = ToolManager.getInstance();
        
        // Subscribe to stock-related events
        if (context != null) {
            context.subscribe(agentId, "stock.request.*");
            context.subscribe(agentId, "stock.alert.*");
            context.subscribe(agentId, "portfolio.update.*");
            context.subscribe(agentId, "market.event.*");
            context.subscribe(agentId, "system.mobility.*");
        }
        
        // Start periodic price monitoring for active alerts
        scheduler.scheduleWithFixedDelay(this::monitorPriceAlerts, 1, 2, TimeUnit.MINUTES);
        
        // Start periodic market data refresh
        scheduler.scheduleWithFixedDelay(this::refreshMarketData, 30, 60, TimeUnit.SECONDS);
        
        // Start portfolio performance monitoring
        scheduler.scheduleWithFixedDelay(this::monitorPortfolios, 5, 10, TimeUnit.MINUTES);
        
        this.initialized = true;
        
        logMessage("Stock Price Agent activated: " + agentId + " (API Key configured: " + !DEFAULT_API_KEY.equals(apiKey) + ")");
    }
    
    @Override
    public void onDeactivate() {
        state = AgentLifecycle.INACTIVE;
        this.initialized = false;
        
        // Shutdown scheduler
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        logMessage("Stock Price Agent deactivated: " + agentId);
    }
    
    @Override
    public void onDestroy() {
        // No-op for demo
    }
    
    @Override
    public void onBeforeMigration(String destinationContext) {
        // No-op for demo
    }
    
    @Override
    public void onAfterMigration(String sourceContext) {
        // No-op for demo
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        if (!initialized || state != AgentLifecycle.ACTIVE) {
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                String topic = event.getTopic();
                
                if (topic.startsWith("stock.request.")) {
                    handleStockRequest(event);
                } else if (topic.startsWith("stock.alert.")) {
                    handleStockAlert(event);
                } else if (topic.startsWith("portfolio.update.")) {
                    handlePortfolioUpdate(event);
                } else if (topic.startsWith("market.event.")) {
                    handleMarketEvent(event);
                } else if (topic.startsWith("system.mobility.")) {
                    handleMobilityEvent(event);
                }
                
            } catch (Exception e) {
                logMessage("Error handling event: " + e.getMessage());
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> publishEvent(Event event) {
        if (context != null) {
            return context.publishEvent(event);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> subscribe(String topicPattern) {
        if (context != null) {
            return context.subscribe(agentId, topicPattern);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> unsubscribe(String topicPattern) {
        if (context != null) {
            return context.unsubscribe(agentId, topicPattern);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    public MobilityState captureMobilityState() {
        stateLock.readLock().lock();
        try {
            Map<String, Object> state = new HashMap<>();
            state.put("agentId", agentId.toString());
            state.put("activeAlertsCount", activeAlerts.size());
            state.put("portfoliosCount", portfolios.size());
            state.put("stockCacheSize", stockCache.size());
            state.put("requestCounter", requestCounter.get());
            state.put("initialized", initialized);
            state.put("apiKey", apiKey);
            
            // Capture serializable data
            Map<String, Map<String, Object>> alertData = new HashMap<>();
            for (Map.Entry<String, StockAlert> entry : activeAlerts.entrySet()) {
                alertData.put(entry.getKey(), entry.getValue().toSerializableMap());
            }
            state.put("activeAlerts", alertData);
            
            return MobilityState.STATIONARY;
        } finally {
            stateLock.readLock().unlock();
        }
    }
    
    public CompletableFuture<Void> restoreMobilityState(MobilityState mobilityState) {
        return CompletableFuture.runAsync(() -> {
            stateLock.writeLock().lock();
            try {
                logMessage("Mobility state restored: " + mobilityState);
            } finally {
                stateLock.writeLock().unlock();
            }
        });
    }
    
    // Public API methods for external access
    
    public CompletableFuture<StockData> getStockPrice(String symbol) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Ensure HTTP client is initialized
                if (httpClient == null) {
                    synchronized (this) {
                        if (httpClient == null) {
                            httpClient = HttpClient.newBuilder()
                                .connectTimeout(java.time.Duration.ofSeconds(10))
                                .build();
                        }
                    }
                }
                
                // Ensure object mapper is initialized
                if (objectMapper == null) {
                    objectMapper = new ObjectMapper();
                }
                
                // Check cache first
                StockData cached = stockCache.get(symbol.toUpperCase());
                if (cached != null && !cached.isStale()) {
                    return cached;
                }
                
                // Fetch from Polygon.io API
                String url = String.format("%s/v2/aggs/ticker/%s/prev?adjusted=true&apikey=%s", 
                    POLYGON_BASE_URL, symbol.toUpperCase(), apiKey);
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(15))
                    .GET()
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    JsonNode json = objectMapper.readTree(response.body());
                    StockData stockData = parseStockData(symbol, json);
                    
                    if (stockData != null) {
                        stockCache.put(symbol.toUpperCase(), stockData);
                        return stockData;
                    }
                }
                
                throw new RuntimeException("Failed to fetch stock data for " + symbol);
                
            } catch (Exception e) {
                logMessage("Error fetching stock price for " + symbol + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    public CompletableFuture<List<StockData>> getMultipleStockPrices(List<String> symbols) {
        List<CompletableFuture<StockData>> futures = new ArrayList<>();
        
        for (String symbol : symbols) {
            futures.add(getStockPrice(symbol));
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(java.util.stream.Collectors.toList()));
    }
    
    public String createPriceAlert(String userId, String symbol, BigDecimal targetPrice, String alertType) {
        String alertId = "alert_" + requestCounter.incrementAndGet() + "_" + System.currentTimeMillis();
        
        StockAlert alert = new StockAlert(
            alertId, userId, symbol.toUpperCase(), targetPrice, alertType, System.currentTimeMillis()
        );
        
        activeAlerts.put(alertId, alert);
        
        logMessage("Price alert created: " + alertId + " for " + symbol + " at $" + targetPrice);
        
        // Publish alert created event
        publishStockAlertEvent(alert, "created");
        
        return alertId;
    }
    
    public boolean cancelPriceAlert(String alertId) {
        StockAlert alert = activeAlerts.remove(alertId);
        if (alert != null) {
            alert.setStatus("CANCELLED");
            publishStockAlertEvent(alert, "cancelled");
            return true;
        }
        return false;
    }
    
    public Portfolio createPortfolio(String userId, String portfolioName) {
        String portfolioId = "portfolio_" + requestCounter.incrementAndGet() + "_" + System.currentTimeMillis();
        
        Portfolio portfolio = new Portfolio(portfolioId, userId, portfolioName);
        portfolios.put(portfolioId, portfolio);
        
        logMessage("Portfolio created: " + portfolioId + " (" + portfolioName + ")");
        
        return portfolio;
    }
    
    public boolean addStockToPortfolio(String portfolioId, String symbol, int shares, BigDecimal avgPrice) {
        Portfolio portfolio = portfolios.get(portfolioId);
        if (portfolio != null) {
            portfolio.addHolding(symbol.toUpperCase(), shares, avgPrice);
            publishPortfolioEvent(portfolio, "stock_added");
            return true;
        }
        return false;
    }
    
    private void handleStockRequest(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> requestData = (Map<String, Object>) event.getPayload();
            
            String requestId = (String) requestData.get("requestId");
            String requestType = (String) requestData.get("requestType");
            String userId = (String) requestData.get("userId");
            
            StockRequest request = new StockRequest(requestId, userId, requestType, requestData);
            pendingRequests.put(requestId, request);
            
            processStockRequestAsync(request);
            
        } catch (Exception e) {
            logMessage("Error processing stock request: " + e.getMessage());
        }
    }
    
    private void processStockRequestAsync(StockRequest request) {
        CompletableFuture.runAsync(() -> {
            try {
                String requestType = request.getRequestType();
                Map<String, Object> data = request.getRequestData();
                
                switch (requestType) {
                    case "get_price":
                        handleGetPriceRequest(request, (String) data.get("symbol"));
                        break;
                    case "create_alert":
                        handleCreateAlertRequest(request, data);
                        break;
                    case "portfolio_analysis":
                        handlePortfolioAnalysisRequest(request, (String) data.get("portfolioId"));
                        break;
                    case "market_summary":
                        handleMarketSummaryRequest(request, (List<String>) data.get("symbols"));
                        break;
                    default:
                        logMessage("Unknown request type: " + requestType);
                }
                
                pendingRequests.remove(request.getRequestId());
                
            } catch (Exception e) {
                logMessage("Failed to process stock request " + request.getRequestId() + ": " + e.getMessage());
                publishStockErrorEvent(request.getRequestId(), e.getMessage());
            }
        });
    }
    
    private void handleGetPriceRequest(StockRequest request, String symbol) throws Exception {
        StockData stockData = getStockPrice(symbol).get();
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("requestId", request.getRequestId());
        responseData.put("symbol", symbol);
        responseData.put("stockData", stockData.toMap());
        
        publishStockResponseEvent("price_response", responseData);
    }
    
    private void handleCreateAlertRequest(StockRequest request, Map<String, Object> data) {
        String symbol = (String) data.get("symbol");
        BigDecimal targetPrice = new BigDecimal(data.get("targetPrice").toString());
        String alertType = (String) data.get("alertType");
        
        String alertId = createPriceAlert(request.getUserId(), symbol, targetPrice, alertType);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("requestId", request.getRequestId());
        responseData.put("alertId", alertId);
        responseData.put("symbol", symbol);
        responseData.put("targetPrice", targetPrice);
        
        publishStockResponseEvent("alert_created", responseData);
    }
    
    private void handlePortfolioAnalysisRequest(StockRequest request, String portfolioId) throws Exception {
        Portfolio portfolio = portfolios.get(portfolioId);
        if (portfolio == null) {
            throw new RuntimeException("Portfolio not found: " + portfolioId);
        }
        
        // Get current prices for all holdings
        List<String> symbols = new ArrayList<>(portfolio.getHoldings().keySet());
        List<StockData> currentPrices = getMultipleStockPrices(symbols).get();
        
        // Calculate portfolio performance
        PortfolioAnalysis analysis = calculatePortfolioPerformance(portfolio, currentPrices);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("requestId", request.getRequestId());
        responseData.put("portfolioId", portfolioId);
        responseData.put("analysis", analysis.toMap());
        
        publishStockResponseEvent("portfolio_analysis", responseData);
    }
    
    private void handleMarketSummaryRequest(StockRequest request, List<String> symbols) throws Exception {
        if (symbols == null || symbols.isEmpty()) {
            // Default market symbols for demo
            symbols = List.of("AAPL", "GOOGL", "MSFT", "TSLA", "NVDA");
        }
        
        List<StockData> marketData = getMultipleStockPrices(symbols).get();
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalStocks", marketData.size());
        summary.put("gainers", marketData.stream().filter(s -> s.getChangePercent() > 0).count());
        summary.put("losers", marketData.stream().filter(s -> s.getChangePercent() < 0).count());
        summary.put("averageChange", marketData.stream()
            .mapToDouble(StockData::getChangePercent)
            .average().orElse(0.0));
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("requestId", request.getRequestId());
        responseData.put("summary", summary);
        responseData.put("stockData", marketData.stream()
            .collect(java.util.stream.Collectors.toMap(StockData::getSymbol, StockData::toMap)));
        
        publishStockResponseEvent("market_summary", responseData);
    }
    
    private void handleStockAlert(Event event) {
        // Handle external stock alert events
        logMessage("Received stock alert event: " + event.getTopic());
    }
    
    private void handlePortfolioUpdate(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> updateData = (Map<String, Object>) event.getPayload();
            
            String portfolioId = (String) updateData.get("portfolioId");
            String updateType = (String) updateData.get("updateType");
            
            Portfolio portfolio = portfolios.get(portfolioId);
            if (portfolio != null) {
                handlePortfolioUpdateInternal(portfolio, updateType, updateData);
            }
            
        } catch (Exception e) {
            logMessage("Error handling portfolio update: " + e.getMessage());
        }
    }
    
    private void handlePortfolioUpdateInternal(Portfolio portfolio, String updateType, Map<String, Object> data) {
        switch (updateType) {
            case "add_stock":
                String symbol = (String) data.get("symbol");
                Integer shares = (Integer) data.get("shares");
                BigDecimal price = new BigDecimal(data.get("price").toString());
                portfolio.addHolding(symbol, shares, price);
                break;
            case "remove_stock":
                String removeSymbol = (String) data.get("symbol");
                portfolio.removeHolding(removeSymbol);
                break;
            case "update_shares":
                String updateSymbol = (String) data.get("symbol");
                Integer newShares = (Integer) data.get("shares");
                portfolio.updateShares(updateSymbol, newShares);
                break;
        }
        
        publishPortfolioEvent(portfolio, "updated");
    }
    
    private void handleMarketEvent(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> eventData = (Map<String, Object>) event.getPayload();
            
            String eventType = (String) eventData.get("eventType");
            
            switch (eventType) {
                case "market_open":
                    logMessage("Market opened - starting enhanced monitoring");
                    // Could increase monitoring frequency during market hours
                    break;
                case "market_close":
                    logMessage("Market closed - reducing monitoring frequency");
                    break;
                case "circuit_breaker":
                    String message = (String) eventData.get("message");
                    logMessage("Circuit breaker event: " + message);
                    // Could trigger special alerts
                    break;
            }
            
        } catch (Exception e) {
            logMessage("Error handling market event: " + e.getMessage());
        }
    }
    
    private void handleMobilityEvent(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> mobilityData = (Map<String, Object>) event.getPayload();
            
            String eventType = (String) mobilityData.get("eventType");
            
            if ("migration_requested".equals(eventType)) {
                logMessage("Preparing for potential agent migration");
                captureAndPersistState();
            } else if ("migration_completed".equals(eventType)) {
                logMessage("Agent migration completed, resuming operations");
                if (!toolManager.isInitialized()) {
                    toolManager.initialize();
                }
            }
            
        } catch (Exception e) {
            logMessage("Error handling mobility event: " + e.getMessage());
        }
    }
    
    private void monitorPriceAlerts() {
        if (!initialized || activeAlerts.isEmpty()) {
            return;
        }
        
        try {
            // Group alerts by symbol to minimize API calls
            Map<String, List<StockAlert>> alertsBySymbol = new HashMap<>();
            for (StockAlert alert : activeAlerts.values()) {
                alertsBySymbol.computeIfAbsent(alert.getSymbol(), k -> new ArrayList<>()).add(alert);
            }
            
            // Check each symbol
            for (Map.Entry<String, List<StockAlert>> entry : alertsBySymbol.entrySet()) {
                String symbol = entry.getKey();
                List<StockAlert> symbolAlerts = entry.getValue();
                
                CompletableFuture.runAsync(() -> {
                    try {
                        StockData currentData = getStockPrice(symbol).get();
                        BigDecimal currentPrice = currentData.getClosePrice();
                        
                        for (StockAlert alert : symbolAlerts) {
                            if (!"ACTIVE".equals(alert.getStatus())) {
                                continue;
                            }
                            
                            boolean triggered = false;
                            
                            switch (alert.getAlertType()) {
                                case "ABOVE":
                                    triggered = currentPrice.compareTo(alert.getTargetPrice()) >= 0;
                                    break;
                                case "BELOW":
                                    triggered = currentPrice.compareTo(alert.getTargetPrice()) <= 0;
                                    break;
                                case "CHANGE":
                                    // Trigger on significant change (demo logic)
                                    triggered = Math.abs(currentData.getChangePercent()) >= alert.getTargetPrice().doubleValue();
                                    break;
                            }
                            
                            if (triggered) {
                                triggerPriceAlert(alert, currentData);
                            }
                        }
                        
                    } catch (Exception e) {
                        logMessage("Error monitoring alerts for " + symbol + ": " + e.getMessage());
                    }
                });
            }
            
        } catch (Exception e) {
            logMessage("Error in price alert monitoring: " + e.getMessage());
        }
    }
    
    private void triggerPriceAlert(StockAlert alert, StockData currentData) {
        alert.setStatus("TRIGGERED");
        alert.setTriggeredTime(System.currentTimeMillis());
        alert.setTriggeredPrice(currentData.getClosePrice());
        
        logMessage("Price alert triggered: " + alert.getAlertId() + 
                  " for " + alert.getSymbol() + 
                  " at $" + currentData.getClosePrice());
        
        publishStockAlertEvent(alert, "triggered");
        
        // Remove triggered alerts (or could keep for history)
        activeAlerts.remove(alert.getAlertId());
    }
    
    private void refreshMarketData() {
        if (!initialized || stockCache.isEmpty()) {
            return;
        }
        
        try {
            // Refresh stale cached data
            Set<String> symbolsToRefresh = new HashSet<>();
            
            for (Map.Entry<String, StockData> entry : stockCache.entrySet()) {
                if (entry.getValue().isStale()) {
                    symbolsToRefresh.add(entry.getKey());
                }
            }
            
            if (!symbolsToRefresh.isEmpty()) {
                logMessage("Refreshing market data for " + symbolsToRefresh.size() + " symbols");
                
                // Refresh in background
                for (String symbol : symbolsToRefresh) {
                    CompletableFuture.runAsync(() -> {
                        try {
                            getStockPrice(symbol).get(); // This will update the cache
                        } catch (Exception e) {
                            logMessage("Failed to refresh data for " + symbol + ": " + e.getMessage());
                        }
                    });
                }
            }
            
        } catch (Exception e) {
            logMessage("Error in market data refresh: " + e.getMessage());
        }
    }
    
    private void monitorPortfolios() {
        if (!initialized || portfolios.isEmpty()) {
            return;
        }
        
        try {
            for (Portfolio portfolio : portfolios.values()) {
                CompletableFuture.runAsync(() -> {
                    try {
                        // Get current prices for portfolio holdings
                        List<String> symbols = new ArrayList<>(portfolio.getHoldings().keySet());
                        if (!symbols.isEmpty()) {
                            List<StockData> currentPrices = getMultipleStockPrices(symbols).get();
                            PortfolioAnalysis analysis = calculatePortfolioPerformance(portfolio, currentPrices);
                            
                            // Check for significant changes
                            if (Math.abs(analysis.getTotalChangePercent()) >= 5.0) {
                                publishPortfolioAlertEvent(portfolio, analysis, "significant_change");
                            }
                        }
                        
                    } catch (Exception e) {
                        logMessage("Failed to monitor portfolio " + portfolio.getPortfolioId() + ": " + e.getMessage());
                    }
                });
            }
            
        } catch (Exception e) {
            logMessage("Error in portfolio monitoring: " + e.getMessage());
        }
    }
    
    private PortfolioAnalysis calculatePortfolioPerformance(Portfolio portfolio, List<StockData> currentPrices) {
        Map<String, StockData> priceMap = currentPrices.stream()
            .collect(java.util.stream.Collectors.toMap(StockData::getSymbol, s -> s));
        
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        
        Map<String, HoldingAnalysis> holdingAnalyses = new HashMap<>();
        
        for (Map.Entry<String, Holding> entry : portfolio.getHoldings().entrySet()) {
            String symbol = entry.getKey();
            Holding holding = entry.getValue();
            StockData currentPrice = priceMap.get(symbol);
            
            if (currentPrice != null) {
                BigDecimal currentValue = currentPrice.getClosePrice()
                    .multiply(BigDecimal.valueOf(holding.getShares()));
                BigDecimal cost = holding.getAveragePrice()
                    .multiply(BigDecimal.valueOf(holding.getShares()));
                
                totalValue = totalValue.add(currentValue);
                totalCost = totalCost.add(cost);
                
                HoldingAnalysis holdingAnalysis = new HoldingAnalysis(
                    symbol, holding.getShares(), holding.getAveragePrice(),
                    currentPrice.getClosePrice(), currentValue, cost
                );
                
                holdingAnalyses.put(symbol, holdingAnalysis);
            }
        }
        
        return new PortfolioAnalysis(portfolio.getPortfolioId(), totalValue, totalCost, holdingAnalyses);
    }
    
    private StockData parseStockData(String symbol, JsonNode json) {
        try {
            if (json.has("results") && json.get("results").isArray() && json.get("results").size() > 0) {
                JsonNode result = json.get("results").get(0);
                
                BigDecimal closePrice = BigDecimal.valueOf(result.get("c").asDouble());
                BigDecimal openPrice = BigDecimal.valueOf(result.get("o").asDouble());
                BigDecimal highPrice = BigDecimal.valueOf(result.get("h").asDouble());
                BigDecimal lowPrice = BigDecimal.valueOf(result.get("l").asDouble());
                long volume = result.get("v").asLong();
                
                // Calculate change
                BigDecimal change = closePrice.subtract(openPrice);
                double changePercent = change.divide(openPrice, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
                
                return new StockData(
                    symbol.toUpperCase(),
                    closePrice,
                    openPrice,
                    highPrice,
                    lowPrice,
                    volume,
                    change,
                    changePercent,
                    System.currentTimeMillis()
                );
            }
        } catch (Exception e) {
            logMessage("Error parsing stock data for " + symbol + ": " + e.getMessage());
        }
        
        return null;
    }
    
    private boolean isMarketOpen() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        
        // Simplified market hours check (EST)
        // Real implementation would consider holidays, weekends, etc.
        return hour >= MARKET_OPEN_HOUR && hour < MARKET_CLOSE_HOUR &&
               now.getDayOfWeek().getValue() <= 5; // Monday = 1, Friday = 5
    }
    
    private void publishStockAlertEvent(StockAlert alert, String eventType) {
        if (context == null) return;
        
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("alertId", alert.getAlertId());
        eventData.put("userId", alert.getUserId());
        eventData.put("symbol", alert.getSymbol());
        eventData.put("targetPrice", alert.getTargetPrice());
        eventData.put("alertType", alert.getAlertType());
        eventData.put("status", alert.getStatus());
        eventData.put("eventType", eventType);
        eventData.put("timestamp", System.currentTimeMillis());
        
        if ("triggered".equals(eventType)) {
            eventData.put("triggeredPrice", alert.getTriggeredPrice());
            eventData.put("triggeredTime", alert.getTriggeredTime());
        }
        
        Event event = Event.builder()
            .topic("stock.alert." + eventType)
            .payload(eventData)
            .sender(agentId)
            .deliveryOptions(DeliveryOptions.reliable())
            .build();
        
        context.publishEvent(event);
    }
    
    private void publishPortfolioEvent(Portfolio portfolio, String eventType) {
        if (context == null) return;
        
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("portfolioId", portfolio.getPortfolioId());
        eventData.put("userId", portfolio.getUserId());
        eventData.put("portfolioName", portfolio.getPortfolioName());
        eventData.put("holdingsCount", portfolio.getHoldings().size());
        eventData.put("eventType", eventType);
        eventData.put("timestamp", System.currentTimeMillis());
        
        Event event = Event.builder()
            .topic("portfolio." + eventType)
            .payload(eventData)
            .sender(agentId)
            .deliveryOptions(DeliveryOptions.reliable())
            .build();
        
        context.publishEvent(event);
    }
    
    private void publishPortfolioAlertEvent(Portfolio portfolio, PortfolioAnalysis analysis, String alertType) {
        if (context == null) return;
        
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("portfolioId", portfolio.getPortfolioId());
        eventData.put("userId", portfolio.getUserId());
        eventData.put("alertType", alertType);
        eventData.put("totalValue", analysis.getTotalValue());
        eventData.put("totalCost", analysis.getTotalCost());
        eventData.put("totalGainLoss", analysis.getTotalGainLoss());
        eventData.put("totalChangePercent", analysis.getTotalChangePercent());
        eventData.put("timestamp", System.currentTimeMillis());
        
        Event event = Event.builder()
            .topic("portfolio.alert." + alertType)
            .payload(eventData)
            .sender(agentId)
            .deliveryOptions(DeliveryOptions.reliable())
            .build();
        
        context.publishEvent(event);
    }
    
    private void publishStockResponseEvent(String responseType, Map<String, Object> data) {
        if (context == null) return;
        
        data.put("responseType", responseType);
        data.put("timestamp", System.currentTimeMillis());
        
        Event event = Event.builder()
            .topic("stock.response." + responseType)
            .payload(data)
            .sender(agentId)
            .deliveryOptions(DeliveryOptions.reliable())
            .build();
        
        context.publishEvent(event);
    }
    
    private void publishStockErrorEvent(String requestId, String error) {
        if (context == null) return;
        
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("requestId", requestId);
        errorData.put("error", error);
        errorData.put("timestamp", System.currentTimeMillis());
        
        Event event = Event.builder()
            .topic("stock.error")
            .payload(errorData)
            .sender(agentId)
            .deliveryOptions(DeliveryOptions.reliable())
            .build();
        
        context.publishEvent(event);
    }
    
    private void captureAndPersistState() {
        try {
            MobilityState state = captureMobilityState();
            logMessage("Agent state captured for migration safety");
        } catch (Exception e) {
            logMessage("Failed to capture agent state: " + e.getMessage());
        }
    }
    
    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [" + agentId + "] " + message);
    }
    
    // Getter methods for monitoring
    public int getActiveAlertsCount() {
        return activeAlerts.size();
    }
    
    public int getPortfoliosCount() {
        return portfolios.size();
    }
    
    public int getCachedStocksCount() {
        return stockCache.size();
    }
    
    public StockAlert getAlert(String alertId) {
        return activeAlerts.get(alertId);
    }
    
    public List<StockAlert> getActiveAlerts() {
        return new ArrayList<>(activeAlerts.values());
    }
    
    public Portfolio getPortfolio(String portfolioId) {
        return portfolios.get(portfolioId);
    }
    
    public List<Portfolio> getPortfolios() {
        return new ArrayList<>(portfolios.values());
    }
    
    public StockData getCachedStockData(String symbol) {
        return stockCache.get(symbol.toUpperCase());
    }
    
    // Inner classes and data structures
    
    private static class StockRequest {
        private final String requestId;
        private final String userId;
        private final String requestType;
        private final Map<String, Object> requestData;
        private final long timestamp;
        
        public StockRequest(String requestId, String userId, String requestType, Map<String, Object> requestData) {
            this.requestId = requestId;
            this.userId = userId;
            this.requestType = requestType;
            this.requestData = new HashMap<>(requestData);
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getRequestId() { return requestId; }
        public String getUserId() { return userId; }
        public String getRequestType() { return requestType; }
        public Map<String, Object> getRequestData() { return new HashMap<>(requestData); }
        public long getTimestamp() { return timestamp; }
    }
    
    public static class StockAlert {
        private final String alertId;
        private final String userId;
        private final String symbol;
        private final BigDecimal targetPrice;
        private final String alertType; // "ABOVE", "BELOW", "CHANGE"
        private final long createdTime;
        
        private String status = "ACTIVE"; // "ACTIVE", "TRIGGERED", "CANCELLED"
        private BigDecimal triggeredPrice;
        private long triggeredTime;
        
        public StockAlert(String alertId, String userId, String symbol, BigDecimal targetPrice, 
                         String alertType, long createdTime) {
            this.alertId = alertId;
            this.userId = userId;
            this.symbol = symbol;
            this.targetPrice = targetPrice;
            this.alertType = alertType;
            this.createdTime = createdTime;
        }
        
        public Map<String, Object> toSerializableMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("alertId", alertId);
            map.put("userId", userId);
            map.put("symbol", symbol);
            map.put("targetPrice", targetPrice);
            map.put("alertType", alertType);
            map.put("createdTime", createdTime);
            map.put("status", status);
            if (triggeredPrice != null) map.put("triggeredPrice", triggeredPrice);
            if (triggeredTime > 0) map.put("triggeredTime", triggeredTime);
            return map;
        }
        
        // Getters and setters
        public String getAlertId() { return alertId; }
        public String getUserId() { return userId; }
        public String getSymbol() { return symbol; }
        public BigDecimal getTargetPrice() { return targetPrice; }
        public String getAlertType() { return alertType; }
        public long getCreatedTime() { return createdTime; }
        public String getStatus() { return status; }
        public BigDecimal getTriggeredPrice() { return triggeredPrice; }
        public long getTriggeredTime() { return triggeredTime; }
        
        public void setStatus(String status) { this.status = status; }
        public void setTriggeredPrice(BigDecimal triggeredPrice) { this.triggeredPrice = triggeredPrice; }
        public void setTriggeredTime(long triggeredTime) { this.triggeredTime = triggeredTime; }
    }
    
    public static class StockData {
        private final String symbol;
        private final BigDecimal closePrice;
        private final BigDecimal openPrice;
        private final BigDecimal highPrice;
        private final BigDecimal lowPrice;
        private final long volume;
        private final BigDecimal change;
        private final double changePercent;
        private final long timestamp;
        
        // Cache TTL - 5 minutes for demo
        private static final long CACHE_TTL = 5 * 60 * 1000;
        
        public StockData(String symbol, BigDecimal closePrice, BigDecimal openPrice, 
                        BigDecimal highPrice, BigDecimal lowPrice, long volume,
                        BigDecimal change, double changePercent, long timestamp) {
            this.symbol = symbol;
            this.closePrice = closePrice;
            this.openPrice = openPrice;
            this.highPrice = highPrice;
            this.lowPrice = lowPrice;
            this.volume = volume;
            this.change = change;
            this.changePercent = changePercent;
            this.timestamp = timestamp;
        }
        
        public boolean isStale() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL;
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("symbol", symbol);
            map.put("closePrice", closePrice);
            map.put("openPrice", openPrice);
            map.put("highPrice", highPrice);
            map.put("lowPrice", lowPrice);
            map.put("volume", volume);
            map.put("change", change);
            map.put("changePercent", changePercent);
            map.put("timestamp", timestamp);
            return map;
        }
        
        // Getters
        public String getSymbol() { return symbol; }
        public BigDecimal getClosePrice() { return closePrice; }
        public BigDecimal getOpenPrice() { return openPrice; }
        public BigDecimal getHighPrice() { return highPrice; }
        public BigDecimal getLowPrice() { return lowPrice; }
        public long getVolume() { return volume; }
        public BigDecimal getChange() { return change; }
        public double getChangePercent() { return changePercent; }
        public long getTimestamp() { return timestamp; }
    }
    
    public static class Portfolio {
        private final String portfolioId;
        private final String userId;
        private final String portfolioName;
        private final Map<String, Holding> holdings = new ConcurrentHashMap<>();
        private final long createdTime;
        
        public Portfolio(String portfolioId, String userId, String portfolioName) {
            this.portfolioId = portfolioId;
            this.userId = userId;
            this.portfolioName = portfolioName;
            this.createdTime = System.currentTimeMillis();
        }
        
        public void addHolding(String symbol, int shares, BigDecimal avgPrice) {
            holdings.put(symbol.toUpperCase(), new Holding(symbol.toUpperCase(), shares, avgPrice));
        }
        
        public void removeHolding(String symbol) {
            holdings.remove(symbol.toUpperCase());
        }
        
        public void updateShares(String symbol, int newShares) {
            Holding holding = holdings.get(symbol.toUpperCase());
            if (holding != null) {
                holding.setShares(newShares);
            }
        }
        
        // Getters
        public String getPortfolioId() { return portfolioId; }
        public String getUserId() { return userId; }
        public String getPortfolioName() { return portfolioName; }
        public Map<String, Holding> getHoldings() { return new HashMap<>(holdings); }
        public long getCreatedTime() { return createdTime; }
    }
    
    public static class Holding {
        private final String symbol;
        private int shares;
        private final BigDecimal averagePrice;
        
        public Holding(String symbol, int shares, BigDecimal averagePrice) {
            this.symbol = symbol;
            this.shares = shares;
            this.averagePrice = averagePrice;
        }
        
        // Getters and setters
        public String getSymbol() { return symbol; }
        public int getShares() { return shares; }
        public BigDecimal getAveragePrice() { return averagePrice; }
        public void setShares(int shares) { this.shares = shares; }
    }
    
    public static class HoldingAnalysis {
        private final String symbol;
        private final int shares;
        private final BigDecimal averagePrice;
        private final BigDecimal currentPrice;
        private final BigDecimal currentValue;
        private final BigDecimal cost;
        private final BigDecimal gainLoss;
        private final double changePercent;
        
        public HoldingAnalysis(String symbol, int shares, BigDecimal averagePrice, 
                              BigDecimal currentPrice, BigDecimal currentValue, BigDecimal cost) {
            this.symbol = symbol;
            this.shares = shares;
            this.averagePrice = averagePrice;
            this.currentPrice = currentPrice;
            this.currentValue = currentValue;
            this.cost = cost;
            this.gainLoss = currentValue.subtract(cost);
            this.changePercent = gainLoss.divide(cost, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue();
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("symbol", symbol);
            map.put("shares", shares);
            map.put("averagePrice", averagePrice);
            map.put("currentPrice", currentPrice);
            map.put("currentValue", currentValue);
            map.put("cost", cost);
            map.put("gainLoss", gainLoss);
            map.put("changePercent", changePercent);
            return map;
        }
        
        // Getters
        public String getSymbol() { return symbol; }
        public int getShares() { return shares; }
        public BigDecimal getAveragePrice() { return averagePrice; }
        public BigDecimal getCurrentPrice() { return currentPrice; }
        public BigDecimal getCurrentValue() { return currentValue; }
        public BigDecimal getCost() { return cost; }
        public BigDecimal getGainLoss() { return gainLoss; }
        public double getChangePercent() { return changePercent; }
    }
    
    public static class PortfolioAnalysis {
        private final String portfolioId;
        private final BigDecimal totalValue;
        private final BigDecimal totalCost;
        private final BigDecimal totalGainLoss;
        private final double totalChangePercent;
        private final Map<String, HoldingAnalysis> holdingAnalyses;
        
        public PortfolioAnalysis(String portfolioId, BigDecimal totalValue, BigDecimal totalCost,
                               Map<String, HoldingAnalysis> holdingAnalyses) {
            this.portfolioId = portfolioId;
            this.totalValue = totalValue;
            this.totalCost = totalCost;
            this.totalGainLoss = totalValue.subtract(totalCost);
            this.totalChangePercent = totalCost.compareTo(BigDecimal.ZERO) != 0 ?
                totalGainLoss.divide(totalCost, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0;
            this.holdingAnalyses = new HashMap<>(holdingAnalyses);
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("portfolioId", portfolioId);
            map.put("totalValue", totalValue);
            map.put("totalCost", totalCost);
            map.put("totalGainLoss", totalGainLoss);
            map.put("totalChangePercent", totalChangePercent);
            
            Map<String, Map<String, Object>> holdings = new HashMap<>();
            for (Map.Entry<String, HoldingAnalysis> entry : holdingAnalyses.entrySet()) {
                holdings.put(entry.getKey(), entry.getValue().toMap());
            }
            map.put("holdings", holdings);
            
            return map;
        }
        
        // Getters
        public String getPortfolioId() { return portfolioId; }
        public BigDecimal getTotalValue() { return totalValue; }
        public BigDecimal getTotalCost() { return totalCost; }
        public BigDecimal getTotalGainLoss() { return totalGainLoss; }
        public double getTotalChangePercent() { return totalChangePercent; }
        public Map<String, HoldingAnalysis> getHoldingAnalyses() { return new HashMap<>(holdingAnalyses); }
    }
}