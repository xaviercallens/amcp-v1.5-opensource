package io.amcp.examples.stock;

import io.amcp.core.Agent;
import io.amcp.core.AgentContext;
import io.amcp.core.AgentID;
import io.amcp.core.AgentLifecycle;
import io.amcp.core.Event;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Stock Price Agent for AMCP v1.5 Multi-Agent System
 * 
 * Provides real-time stock price information and market data.
 * Integrates with AMCP protocol for agent-to-agent communication.
 * 
 * Features:
 * - Stock symbol lookup and price retrieval
 * - Market data and trends analysis
 * - Portfolio tracking and alerts
 * - Integration with external financial APIs
 */
public class StockPriceAgent implements Agent {
    
    private final AgentID agentId;
    private AgentContext context;
    private AgentLifecycle lifecycleState = AgentLifecycle.INACTIVE;
    private final Set<String> subscriptions = new CopyOnWriteArraySet<>();
    
    // Mock stock data for demo
    private final Map<String, StockInfo> stockDatabase = new HashMap<>();
    
    public StockPriceAgent() {
        this.agentId = AgentID.named("StockPriceAgent");
        initializeMockStockData();
    }
    
    /**
     * Stock information data structure
     */
    private static class StockInfo {
        private final String symbol;
        private final String name;
        private final double price;
        private final double change;
        private final double changePercent;
        private final String currency;
        private final long volume;
        
        public StockInfo(String symbol, String name, double price, double change, 
                        double changePercent, String currency, long volume) {
            this.symbol = symbol;
            this.name = name;
            this.price = price;
            this.change = change;
            this.changePercent = changePercent;
            this.currency = currency;
            this.volume = volume;
        }
        
        // Getters
        public String getSymbol() { return symbol; }
        public String getName() { return name; }
        public double getPrice() { return price; }
        public double getChange() { return change; }
        public double getChangePercent() { return changePercent; }
        public String getCurrency() { return currency; }
        public long getVolume() { return volume; }
    }
    
    /**
     * Initialize mock stock data for demonstration
     */
    private void initializeMockStockData() {
        stockDatabase.put("AMADEUS", new StockInfo("AMADEUS", "Amadeus IT Group SA", 68.50, 1.25, 1.86, "EUR", 125000));
        stockDatabase.put("LVMH", new StockInfo("LVMH", "LVMH Moet Hennessy Louis Vuitton SE", 725.80, -12.40, -1.68, "EUR", 85000));
        stockDatabase.put("AAPL", new StockInfo("AAPL", "Apple Inc.", 195.89, 2.45, 1.27, "USD", 2100000));
        stockDatabase.put("MSFT", new StockInfo("MSFT", "Microsoft Corporation", 419.74, 5.82, 1.41, "USD", 1850000));
        stockDatabase.put("GOOGL", new StockInfo("GOOGL", "Alphabet Inc.", 175.32, -1.15, -0.65, "USD", 950000));
        stockDatabase.put("TSLA", new StockInfo("TSLA", "Tesla Inc.", 248.98, 8.45, 3.51, "USD", 3200000));
        stockDatabase.put("SAP", new StockInfo("SAP", "SAP SE", 218.45, 3.20, 1.49, "EUR", 145000));
        stockDatabase.put("ASML", new StockInfo("ASML", "ASML Holding NV", 695.80, 15.30, 2.25, "EUR", 78000));
    }
    
    @Override
    public AgentID getAgentId() {
        return agentId;
    }
    
    @Override
    public AgentContext getContext() {
        return context;
    }
    
    @Override
    public AgentLifecycle getLifecycleState() {
        return lifecycleState;
    }
    
    public void setContext(AgentContext context) {
        this.context = context;
    }
    
    @Override
    public void onActivate() {
        try {
            logMessage("Activating Stock Price Agent...");
            lifecycleState = AgentLifecycle.ACTIVE;
            
            // Subscribe to stock-related events
            subscriptions.add("stock.request.**");
            subscriptions.add("stock.query.**");
            subscriptions.add("market.data.**");
            
            for (String topic : subscriptions) {
                subscribe(topic);
            }
            
            logMessage("Stock Price Agent activated with " + subscriptions.size() + " subscriptions");
            
        } catch (Exception e) {
            logMessage("Failed to activate Stock Price Agent: " + e.getMessage());
            throw new RuntimeException("Stock Price Agent activation failed", e);
        }
    }
    
    @Override
    public void onDeactivate() {
        logMessage("Deactivating Stock Price Agent...");
        lifecycleState = AgentLifecycle.INACTIVE;
        subscriptions.clear();
    }
    
    @Override
    public void onDestroy() {
        logMessage("Destroying Stock Price Agent...");
        subscriptions.clear();
        lifecycleState = AgentLifecycle.INACTIVE;
    }
    
    @Override
    public void onBeforeMigration(String destinationContext) {
        logMessage("Preparing for migration to: " + destinationContext);
    }
    
    @Override
    public void onAfterMigration(String sourceContext) {
        logMessage("Completed migration from: " + sourceContext);
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
    
    @Override
    public CompletableFuture<Void> publishEvent(Event event) {
        if (context != null) {
            context.publishEvent(event);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                String topic = event.getTopic();
                logMessage("Processing stock request: " + topic);
                
                if (topic.startsWith("stock.request")) {
                    handleStockRequest(event);
                } else if (topic.startsWith("stock.query")) {
                    handleStockQuery(event);
                } else if (topic.startsWith("market.data")) {
                    handleMarketDataRequest(event);
                }
                
            } catch (Exception e) {
                logMessage("Failed to handle event: " + event.getTopic() + " - " + e.getMessage());
                sendErrorResponse(event, e.getMessage());
            }
        });
    }
    
    /**
     * Handle stock price requests
     */
    private void handleStockRequest(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = event.getPayload(Map.class);
            
            String query = (String) payload.get("query");
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = (Map<String, Object>) payload.get("parameters");
            
            String response = processStockRequest(query, parameters);
            sendStockResponse(event, response);
            
        } catch (Exception e) {
            logMessage("Error processing stock request: " + e.getMessage());
            sendErrorResponse(event, "Failed to process stock request");
        }
    }
    
    /**
     * Handle stock queries
     */
    private void handleStockQuery(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = event.getPayload(Map.class);
            String symbol = (String) payload.get("symbol");
            
            if (symbol != null) {
                String response = getStockPrice(symbol.toUpperCase());
                sendStockResponse(event, response);
            } else {
                sendErrorResponse(event, "Stock symbol is required");
            }
            
        } catch (Exception e) {
            logMessage("Error processing stock query: " + e.getMessage());
            sendErrorResponse(event, "Failed to process stock query");
        }
    }
    
    /**
     * Handle market data requests
     */
    private void handleMarketDataRequest(Event event) {
        try {
            String response = getMarketSummary();
            sendStockResponse(event, response);
            
        } catch (Exception e) {
            logMessage("Error processing market data request: " + e.getMessage());
            sendErrorResponse(event, "Failed to process market data request");
        }
    }
    
    /**
     * Process stock price request with natural language
     */
    private String processStockRequest(String query, Map<String, Object> parameters) {
        logMessage("Processing stock request: " + query);
        
        if (parameters != null && parameters.containsKey("symbols")) {
            @SuppressWarnings("unchecked")
            List<String> symbols = (List<String>) parameters.get("symbols");
            return getMultipleStockPrices(symbols);
        }
        
        // Extract stock symbol from query
        String extractedSymbol = extractStockSymbol(query);
        if (extractedSymbol != null) {
            return getStockPrice(extractedSymbol);
        }
        
        // If no specific symbol found, provide general market info
        return getMarketSummary();
    }
    
    /**
     * Extract stock symbol from natural language query
     */
    private String extractStockSymbol(String query) {
        String upperQuery = query.toUpperCase();
        
        // Direct symbol matches
        for (String symbol : stockDatabase.keySet()) {
            if (upperQuery.contains(symbol)) {
                return symbol;
            }
        }
        
        // Company name matches
        for (StockInfo stock : stockDatabase.values()) {
            if (upperQuery.contains(stock.getName().toUpperCase()) || 
                upperQuery.contains(stock.getName().split(" ")[0].toUpperCase())) {
                return stock.getSymbol();
            }
        }
        
        return null;
    }
    
    /**
     * Get stock price for a single symbol
     */
    private String getStockPrice(String symbol) {
        StockInfo stock = stockDatabase.get(symbol.toUpperCase());
        
        if (stock == null) {
            return "Sorry, I don't have information for stock symbol: " + symbol + 
                   ". Available symbols: " + String.join(", ", stockDatabase.keySet());
        }
        
        StringBuilder response = new StringBuilder();
        response.append("📈 Stock Information for ").append(stock.getName()).append(" (").append(stock.getSymbol()).append("):\n\n");
        response.append("💰 Current Price: ").append(formatCurrency(stock.getPrice(), stock.getCurrency())).append("\n");
        response.append("📊 Change: ").append(formatChange(stock.getChange(), stock.getChangePercent(), stock.getCurrency())).append("\n");
        response.append("📈 Volume: ").append(formatVolume(stock.getVolume())).append("\n");
        response.append("🕒 Last Updated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        
        return response.toString();
    }
    
    /**
     * Get multiple stock prices
     */
    private String getMultipleStockPrices(List<String> symbols) {
        StringBuilder response = new StringBuilder();
        response.append("📈 Multiple Stock Prices:\n\n");
        
        for (String symbol : symbols) {
            StockInfo stock = stockDatabase.get(symbol.toUpperCase());
            if (stock != null) {
                response.append("• ").append(stock.getSymbol()).append(" (").append(stock.getName()).append("): ");
                response.append(formatCurrency(stock.getPrice(), stock.getCurrency()));
                response.append(" ").append(formatChange(stock.getChange(), stock.getChangePercent(), stock.getCurrency()));
                response.append("\n");
            }
        }
        
        response.append("\n🕒 Last Updated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        return response.toString();
    }
    
    /**
     * Get market summary
     */
    private String getMarketSummary() {
        StringBuilder response = new StringBuilder();
        response.append("📊 Market Summary:\n\n");
        
        // Calculate market stats
        long totalVolume = stockDatabase.values().stream()
                .mapToLong(StockInfo::getVolume)
                .sum();
        
        long gainers = stockDatabase.values().stream()
                .mapToLong(stock -> stock.getChange() > 0 ? 1 : 0)
                .sum();
        
        long losers = stockDatabase.values().stream()
                .mapToLong(stock -> stock.getChange() < 0 ? 1 : 0)
                .sum();
        
        response.append("📈 Gainers: ").append(gainers).append("\n");
        response.append("📉 Losers: ").append(losers).append("\n");
        response.append("📊 Total Volume: ").append(formatVolume(totalVolume)).append("\n\n");
        
        response.append("Top Performers:\n");
        stockDatabase.values().stream()
                .sorted((a, b) -> Double.compare(b.getChangePercent(), a.getChangePercent()))
                .limit(3)
                .forEach(stock -> {
                    response.append("• ").append(stock.getSymbol()).append(": ");
                    response.append(formatChange(stock.getChange(), stock.getChangePercent(), stock.getCurrency()));
                    response.append("\n");
                });
        
        response.append("\n🕒 Market Data as of: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        return response.toString();
    }
    
    /**
     * Format currency values
     */
    private String formatCurrency(double value, String currency) {
        return String.format("%.2f %s", value, currency);
    }
    
    /**
     * Format price changes
     */
    private String formatChange(double change, double changePercent, String currency) {
        String changeStr = change >= 0 ? "+" + String.format("%.2f", change) : String.format("%.2f", change);
        String percentStr = changePercent >= 0 ? "+" + String.format("%.2f", changePercent) : String.format("%.2f", changePercent);
        String emoji = change >= 0 ? "🟢" : "🔴";
        
        return emoji + " " + changeStr + " " + currency + " (" + percentStr + "%)";
    }
    
    /**
     * Format volume numbers
     */
    private String formatVolume(long volume) {
        if (volume >= 1_000_000) {
            return String.format("%.1fM", volume / 1_000_000.0);
        } else if (volume >= 1_000) {
            return String.format("%.1fK", volume / 1_000.0);
        } else {
            return String.valueOf(volume);
        }
    }
    
    /**
     * Send stock response
     */
    private void sendStockResponse(Event originalEvent, String response) {
        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("response", response);
        responsePayload.put("agentId", agentId.toString());
        responsePayload.put("timestamp", LocalDateTime.now().toString());
        responsePayload.put("status", "success");
        
        publishEvent(Event.builder()
                .topic("stock.response")
                .payload(responsePayload)
                .correlationId(originalEvent.getCorrelationId())
                .sender(agentId)
                .build());
        
        logMessage("Sent stock response for correlation: " + originalEvent.getCorrelationId());
    }
    
    /**
     * Send error response
     */
    private void sendErrorResponse(Event originalEvent, String errorMessage) {
        Map<String, Object> errorPayload = new HashMap<>();
        errorPayload.put("response", "Stock service error: " + errorMessage);
        errorPayload.put("agentId", agentId.toString());
        errorPayload.put("timestamp", LocalDateTime.now().toString());
        errorPayload.put("status", "error");
        errorPayload.put("error", errorMessage);
        
        publishEvent(Event.builder()
                .topic("stock.response")
                .payload(errorPayload)
                .correlationId(originalEvent.getCorrelationId())
                .sender(agentId)
                .build());
        
        logMessage("Sent error response: " + errorMessage);
    }
    
    /**
     * Get available stock symbols
     */
    public Set<String> getAvailableSymbols() {
        return new HashSet<>(stockDatabase.keySet());
    }
    
    /**
     * Get stock capabilities
     */
    public List<String> getCapabilities() {
        return Arrays.asList(
            "stock.price.lookup",
            "market.data.summary",
            "stock.portfolio.tracking",
            "market.trends.analysis"
        );
    }
    
    private void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] [StockPriceAgent] " + message);
    }
}