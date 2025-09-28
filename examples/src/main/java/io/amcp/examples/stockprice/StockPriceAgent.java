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
 * AMCP v1.4 Stock Price Agent.
 * Example agent demonstrating stock market data monitoring
 * with portfolio management and price alerts.
 */
public class StockPriceAgent implements Agent {
    
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
        
        toolManager = ToolManager.getInstance();
        
        if (context != null) {
            context.subscribe(agentId, "stock.price.*");
            context.subscribe(agentId, "portfolio.*");
        }
        
        scheduler.scheduleWithFixedDelay(this::monitorPrices, 1, 5, TimeUnit.MINUTES);
        
        this.initialized = true;
        
        logMessage("Stock Price Agent activated: " + agentId);
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
                
                if (topic.startsWith("stock.price.")) {
                    handleStockPriceRequest(event);
                } else if (topic.startsWith("portfolio.")) {
                    handlePortfolioRequest(event);
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
                // Simulate stock price - in real implementation would call external API
                BigDecimal price = simulatePrice();
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
                
                logMessage("Stock price for " + symbol + ": $" + price);
                
            } catch (Exception e) {
                logMessage("Error processing stock price: " + e.getMessage());
            }
        });
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
        return new BigDecimal(price).setScale(2, BigDecimal.ROUND_HALF_UP);
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
