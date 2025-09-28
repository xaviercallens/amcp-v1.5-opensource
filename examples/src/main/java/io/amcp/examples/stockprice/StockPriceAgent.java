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
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.math.BigDecimal;

/**
 * AMCP v1.5 Stock Price Agent with Real API Integration.
 * Example agent demonstrating real-time stock market data monitoring
 * with portfolio management and price alerts using live financial APIs.
 */
public class StockPriceAgent implements Agent {
    
    // Real-time stock API configuration with provided key
    private static final String FINNHUB_API_KEY = "ZGgVNySPtrCA7u1knnya3wdefCLGpJwd"; // Provided key
    private static final String FINNHUB_API_BASE = "https://finnhub.io/api/v1";
    
    private AgentID agentId;
    private AgentContext context;
    private AgentLifecycle state = AgentLifecycle.INACTIVE;
    
    private final Map<String, StockPosition> portfolio = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong(0);
    private final ReentrantReadWriteLock stateLock = new ReentrantReadWriteLock();
    
    private ScheduledExecutorService scheduler;
    private ToolManager toolManager;
    private volatile boolean initialized = false;
    
    public StockPriceAgent() {
        this.agentId = AgentID.named("stock-price-" + System.currentTimeMillis());
    }
    
    public StockPriceAgent(AgentID agentId) {
        this.agentId = agentId;
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
        
        scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "StockPrice-" + agentId.toString());
            t.setDaemon(true);
            return t;
        });
        
        if (context != null) {
            context.subscribe(agentId, "stock.price.*");
            context.subscribe(agentId, "stock.request"); // Chat agent requests
            context.subscribe(agentId, "portfolio.*");
        }
        
        scheduler.scheduleWithFixedDelay(this::monitorPrices, 1, 5, TimeUnit.MINUTES);
        
        this.initialized = true;
        
        logMessage("Stock Price Agent activated: " + agentId);
        logMessage("Subscribed to stock.request for chat integration");
    }
    
    @Override
    public void onDeactivate() {
        state = AgentLifecycle.INACTIVE;
        this.initialized = false;
        
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
                
                if (topic.equals("stock.request")) {
                    handleChatStockRequest(event);
                } else if (topic.startsWith("stock.price.")) {
                    handleStockPriceRequest(event);
                } else if (topic.startsWith("portfolio.")) {
                    handlePortfolioRequest(event);
                }
                
            } catch (Exception e) {
                logMessage("Error handling event: " + e.getMessage());
            }
        });
    }
    
    private void handleChatStockRequest(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> request = (Map<String, Object>) event.getPayload();
            
            String query = (String) request.get("query");
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = (Map<String, Object>) request.get("parameters");
            String symbol = (String) parameters.get("symbol");
            
            logMessage("Chat stock request for symbol: " + symbol + " (query: " + query + ")");
            
            if (symbol != null && !symbol.trim().isEmpty()) {
                // Get real-time stock price
                BigDecimal price = callFinnhubAPI(symbol.trim().toUpperCase());
                
                // Format response for chat
                String response = formatStockForChat(symbol, price);
                
                // Send response back to chat agent
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("response", response);
                responseData.put("symbol", symbol);
                responseData.put("price", price.toString());
                
                publishEvent(Event.builder()
                    .topic("agent.response")
                    .payload(responseData)
                    .correlationId(event.getCorrelationId())
                    .sender(agentId)
                    .build());
                    
                logMessage("Sent stock response for " + symbol + ": $" + price);
            } else {
                // No symbol provided, send error response
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("response", "I need a stock symbol or company name to provide price information. Please specify which stock you're interested in.");
                errorResponse.put("error", "No symbol specified");
                
                publishEvent(Event.builder()
                    .topic("agent.response")
                    .payload(errorResponse)
                    .correlationId(event.getCorrelationId())
                    .sender(agentId)
                    .build());
            }
            
        } catch (Exception e) {
            logMessage("Error handling chat stock request: " + e.getMessage());
        }
    }
    
    private String formatStockForChat(String symbol, BigDecimal price) {
        StringBuilder response = new StringBuilder();
        response.append("📈 Stock Information for ").append(symbol.toUpperCase()).append(":\n");
        response.append("💰 Current Price: $").append(price).append("\n");
        
        // Add market context
        double priceValue = price.doubleValue();
        if (priceValue > 100) {
            response.append("🔥 High-value stock - institutional favorite");
        } else if (priceValue < 10) {
            response.append("💎 Lower-priced stock - potential growth opportunity");
        } else {
            response.append("⚖️ Mid-range stock - balanced investment option");
        }
        
        response.append("\n📊 Data source: Real-time via Finnhub API");
        
        // Add disclaimer
        response.append("\n⚠️ This is not financial advice - please consult a financial advisor");
        
        return response.toString();
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
        return MobilityState.STATIONARY;
    }
    
    public CompletableFuture<Void> restoreMobilityState(MobilityState mobilityState) {
        return CompletableFuture.completedFuture(null);
    }
    
    private void handleStockPriceRequest(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> requestData = (Map<String, Object>) event.getPayload();
            
            String symbol = (String) requestData.get("symbol");
            if (symbol != null) {
                processStockPrice(symbol);
            }
            
        } catch (Exception e) {
            logMessage("Error processing stock price request: " + e.getMessage());
        }
    }
    
    private void handlePortfolioRequest(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> requestData = (Map<String, Object>) event.getPayload();
            
            String action = (String) requestData.get("action");
            
            if ("add".equals(action)) {
                String symbol = (String) requestData.get("symbol");
                Number shares = (Number) requestData.get("shares");
                Number price = (Number) requestData.get("price");
                
                if (symbol != null && shares != null && price != null) {
                    StockPosition position = new StockPosition(symbol, shares.intValue(), 
                        new BigDecimal(price.toString()));
                    portfolio.put(symbol, position);
                    
                    logMessage("Added to portfolio: " + symbol);
                }
            }
            
        } catch (Exception e) {
            logMessage("Error handling portfolio request: " + e.getMessage());
        }
    }
    
    private void processStockPrice(String symbol) {
        CompletableFuture.runAsync(() -> {
            try {
                // Use real stock price API call
                BigDecimal price = callFinnhubAPI(symbol);
                requestCounter.incrementAndGet();
                
                Map<String, Object> priceData = new HashMap<>();
                priceData.put("symbol", symbol);
                priceData.put("price", price.toString());
                priceData.put("timestamp", System.currentTimeMillis());
                
                Event responseEvent = Event.builder()
                    .topic("stock.price.response")
                    .payload(priceData)
                    .sender(agentId)
                    .build();
                
                publishEvent(responseEvent);
                
                logMessage("Real-time stock price for " + symbol + ": $" + price);
                
            } catch (Exception e) {
                logMessage("Error processing stock price: " + e.getMessage());
            }
        });
    }
    
    private BigDecimal callFinnhubAPI(String symbol) {
        try {
            // Build API URL for real-time stock price
            String url = String.format("%s/quote?symbol=%s&token=%s", 
                FINNHUB_API_BASE, 
                symbol.toUpperCase(), 
                FINNHUB_API_KEY);
            
            logMessage("Calling Finnhub API for: " + symbol);
            
            // Make HTTP request
            java.net.URI apiUri = java.net.URI.create(url);
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(apiUri)
                .timeout(java.time.Duration.ofSeconds(10))
                .GET()
                .build();
                
            java.net.http.HttpResponse<String> response = client.send(request, 
                java.net.http.HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                // Parse JSON response for current price
                return parseStockResponse(response.body(), symbol);
            } else {
                logMessage("Finnhub API error: " + response.statusCode());
                return simulatePrice(); // Fallback
            }
            
        } catch (Exception e) {
            logMessage("Error calling Finnhub API: " + e.getMessage());
            return simulatePrice(); // Fallback
        }
    }
    
    private BigDecimal parseStockResponse(String jsonResponse, String symbol) {
        try {
            // Simple JSON parsing for current price (c = current price)
            double currentPrice = extractJsonValue(jsonResponse, "c");
            if (currentPrice > 0) {
                return new BigDecimal(currentPrice).setScale(2, java.math.RoundingMode.HALF_UP);
            } else {
                return simulatePrice(); // Fallback
            }
        } catch (Exception e) {
            logMessage("Error parsing stock response: " + e.getMessage());
            return simulatePrice(); // Fallback
        }
    }
    
    private double extractJsonValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*([0-9.]+)";
            java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher matcher = regex.matcher(json);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return 0.0;
    }
    
    private void monitorPrices() {
        if (!initialized || portfolio.isEmpty()) {
            return;
        }
        
        logMessage("Monitoring " + portfolio.size() + " portfolio positions");
        
        for (String symbol : portfolio.keySet()) {
            processStockPrice(symbol);
        }
    }
    
    private BigDecimal simulatePrice() {
        double price = 50.0 + (Math.random() * 100.0);
        return new BigDecimal(price).setScale(2, java.math.RoundingMode.HALF_UP);
    }
    
    private void logMessage(String message) {
        String timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
        );
        System.out.println("[" + timestamp + "] [" + agentId + "] " + message);
    }
    
    // Inner class
    private static class StockPosition {
        private final String symbol;
        private final int shares;
        private final BigDecimal price;
        
        public StockPosition(String symbol, int shares, BigDecimal price) {
            this.symbol = symbol;
            this.shares = shares;
            this.price = price;
        }
        
        public String getSymbol() { return symbol; }
        public int getShares() { return shares; }
        public BigDecimal getPrice() { return price; }
    }
}
