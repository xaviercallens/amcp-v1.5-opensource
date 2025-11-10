package io.amcp.examples;

import io.amcp.core.AbstractMobileAgent;
import io.amcp.core.Event;
import jakarta.enterprise.context.ApplicationScoped;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Stock Agent - Demonstrates v1.6 features with Quarkus integration.
 * 
 * Features:
 * - CloudEvents v1.0 compliance
 * - Quarkus CDI integration (@ApplicationScoped)
 * - Async event handling with CompletableFuture
 * - JSON payload processing
 * - Multi-instance support via Kafka
 * - Real-time stock data simulation
 * 
 * Spec Reference: AMCP v1.6 Architecture Evolution
 */
@ApplicationScoped
public class StockAgent extends AbstractMobileAgent {
    
    private static final ObjectMapper mapper = new ObjectMapper();
    
    // Simulated stock data
    private static final Map<String, Map<String, Object>> STOCK_DATA = new HashMap<>();
    
    static {
        // Initialize sample stock data
        STOCK_DATA.put("AAPL", Map.of(
            "symbol", "AAPL",
            "name", "Apple Inc.",
            "price", 195.50,
            "change", 2.50,
            "changePercent", 1.30,
            "volume", 52_000_000L,
            "marketCap", 3_050_000_000_000L
        ));
        STOCK_DATA.put("GOOGL", Map.of(
            "symbol", "GOOGL",
            "name", "Alphabet Inc.",
            "price", 142.80,
            "change", 1.20,
            "changePercent", 0.85,
            "volume", 28_500_000L,
            "marketCap", 1_890_000_000_000L
        ));
        STOCK_DATA.put("MSFT", Map.of(
            "symbol", "MSFT",
            "name", "Microsoft Corporation",
            "price", 378.90,
            "change", 3.45,
            "changePercent", 0.92,
            "volume", 22_000_000L,
            "marketCap", 2_820_000_000_000L
        ));
        STOCK_DATA.put("TSLA", Map.of(
            "symbol", "TSLA",
            "name", "Tesla Inc.",
            "price", 242.50,
            "change", -5.30,
            "changePercent", -2.14,
            "volume", 145_000_000L,
            "marketCap", 770_000_000_000L
        ));
    }

    @Override
    public void onActivate() {
        super.onActivate();
        
        // Subscribe to stock request topics
        subscribe("stock.**");
        subscribe("stock.request");
        
        logMessage("📈 Stock Agent activated - Ready for v1.6 distributed mesh");
    }

    @Override
    public CompletableFuture<Void> handleEvent(Event event) {
        return CompletableFuture.runAsync(() -> {
            try {
                String topic = event.getTopic();
                logger.debug("Stock Agent handling event: {}", topic);
                
                if (topic.equals("stock.request")) {
                    handleStockRequest(event);
                } else if (topic.equals("stock.quote")) {
                    handleQuoteRequest(event);
                } else if (topic.equals("stock.portfolio")) {
                    handlePortfolioRequest(event);
                } else if (topic.equals("stock.status")) {
                    handleStatusRequest(event);
                }
                
            } catch (Exception e) {
                logger.error("Error handling stock event: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Handles stock quote request for a specific symbol.
     * Demonstrates CloudEvents payload handling.
     */
    private void handleStockRequest(Event event) {
        try {
            // Extract symbol from payload
            Map<String, Object> request = event.getPayload(Map.class);
            String symbol = ((String) request.getOrDefault("symbol", "AAPL")).toUpperCase();
            
            logMessage("💹 Stock quote request for: " + symbol);
            
            // Get stock data
            Map<String, Object> stock = STOCK_DATA.getOrDefault(symbol, STOCK_DATA.get("AAPL"));
            
            // Publish response event (CloudEvents compliant)
            Map<String, Object> response = new HashMap<>(stock);
            response.put("timestamp", System.currentTimeMillis());
            response.put("source", "stock-agent");
            response.put("52WeekHigh", (double) stock.get("price") * 1.25);
            response.put("52WeekLow", (double) stock.get("price") * 0.75);
            
            publishEvent("stock.response", response);
            logMessage("✅ Stock quote response sent for: " + symbol);
            
        } catch (Exception e) {
            logger.error("Error handling stock request: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles stock quote request.
     * Demonstrates async processing.
     */
    private void handleQuoteRequest(Event event) {
        try {
            Map<String, Object> request = event.getPayload(Map.class);
            String symbol = ((String) request.getOrDefault("symbol", "AAPL")).toUpperCase();
            
            logMessage("📊 Stock quote request for: " + symbol);
            
            Map<String, Object> stock = STOCK_DATA.getOrDefault(symbol, STOCK_DATA.get("AAPL"));
            
            // Create detailed quote
            Map<String, Object> quote = new HashMap<>(stock);
            quote.put("pe", 28.5);
            quote.put("eps", 6.85);
            quote.put("dividend", 0.92);
            quote.put("yield", 0.47);
            
            publishEvent("stock.quote.response", quote);
            logMessage("✅ Quote response sent for: " + symbol);
            
        } catch (Exception e) {
            logger.error("Error handling quote request: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles portfolio request.
     * Demonstrates multi-stock processing.
     */
    private void handlePortfolioRequest(Event event) {
        try {
            Map<String, Object> request = event.getPayload(Map.class);
            
            logMessage("💼 Portfolio analysis requested");
            
            // Create portfolio summary
            Map<String, Object> portfolio = new HashMap<>();
            portfolio.put("totalValue", 0.0);
            portfolio.put("totalChange", 0.0);
            portfolio.put("stocks", STOCK_DATA.keySet());
            portfolio.put("count", STOCK_DATA.size());
            
            // Calculate totals
            double totalValue = 0;
            double totalChange = 0;
            for (Map<String, Object> stock : STOCK_DATA.values()) {
                totalValue += (double) stock.get("price");
                totalChange += (double) stock.get("change");
            }
            
            portfolio.put("totalValue", totalValue);
            portfolio.put("totalChange", totalChange);
            portfolio.put("timestamp", System.currentTimeMillis());
            
            publishEvent("stock.portfolio.response", portfolio);
            logMessage("✅ Portfolio response sent");
            
        } catch (Exception e) {
            logger.error("Error handling portfolio request: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles status request.
     * Demonstrates agent health check.
     */
    private void handleStatusRequest(Event event) {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("agent", "StockAgent");
            status.put("version", "1.6.0");
            status.put("status", "active");
            status.put("timestamp", System.currentTimeMillis());
            status.put("stocksSupported", STOCK_DATA.keySet());
            status.put("dataSource", "simulated");
            
            publishEvent("stock.status.response", status);
            logMessage("✅ Status response sent");
            
        } catch (Exception e) {
            logger.error("Error handling status request: {}", e.getMessage(), e);
        }
    }

    @Override
    public void onDeactivate() {
        logMessage("👋 Stock Agent shutting down");
        super.onDeactivate();
    }
}
