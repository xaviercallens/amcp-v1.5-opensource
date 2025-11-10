package io.amcp.examples;

import io.amcp.core.AbstractMobileAgent;
import io.amcp.core.Event;
import jakarta.enterprise.context.ApplicationScoped;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Stock Agent with Real Data Integration - v1.6 Quarkus
 * 
 * Fetches real stock data from Alpha Vantage API.
 * Falls back to simulated data if API is unavailable.
 * 
 * Environment Variables:
 * - ALPHA_VANTAGE_API_KEY: Alpha Vantage API key (optional, uses demo if not set)
 */
@ApplicationScoped
public class StockAgentReal extends AbstractMobileAgent {
    
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(StockAgentReal.class);
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    
    // Alpha Vantage API configuration
    private static final String ALPHA_VANTAGE_API_KEY = System.getenv().getOrDefault("ALPHA_VANTAGE_API_KEY", "demo");
    private static final String ALPHA_VANTAGE_URL = "https://www.alphavantage.co/query";
    
    // Fallback simulated stock data
    private static final Map<String, Map<String, Object>> STOCK_DATA = new HashMap<>();
    
    static {
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
        subscribe("stock.**");
        subscribe("stock.request");
        logMessage("📈 Stock Agent (Real Data) activated");
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

    private void handleStockRequest(Event event) {
        try {
            Map<String, Object> request = event.getPayload(Map.class);
            String symbol = ((String) request.getOrDefault("symbol", "AAPL")).toUpperCase();
            
            logMessage("💹 Stock quote request for: " + symbol);
            
            // Try to fetch real data from Alpha Vantage
            Map<String, Object> stock = fetchRealStockData(symbol);
            
            if (stock == null) {
                stock = STOCK_DATA.getOrDefault(symbol, STOCK_DATA.get("AAPL"));
                logMessage("⚠️  Using fallback data for: " + symbol);
            }
            
            Map<String, Object> response = new HashMap<>(stock);
            response.put("timestamp", System.currentTimeMillis());
            response.put("source", "stock-agent-real");
            response.put("dataSource", stock.containsKey("real") ? "alphavantage" : "simulated");
            response.put("52WeekHigh", ((Number) stock.get("price")).doubleValue() * 1.25);
            response.put("52WeekLow", ((Number) stock.get("price")).doubleValue() * 0.75);
            
            publishEvent("stock.response", response);
            logMessage("✅ Stock quote response sent for: " + symbol);
            
        } catch (Exception e) {
            logger.error("Error handling stock request: {}", e.getMessage(), e);
        }
    }

    private Map<String, Object> fetchRealStockData(String symbol) {
        try {
            String url = ALPHA_VANTAGE_URL + "?function=GLOBAL_QUOTE&symbol=" + symbol + "&apikey=" + ALPHA_VANTAGE_API_KEY;
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(java.time.Duration.ofSeconds(5))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                JsonNode quote = root.get("Global Quote");
                
                if (quote != null && quote.has("05. price")) {
                    Map<String, Object> stock = new HashMap<>();
                    stock.put("symbol", quote.get("01. symbol").asText());
                    stock.put("price", Double.parseDouble(quote.get("05. price").asText()));
                    stock.put("change", Double.parseDouble(quote.get("09. change").asText()));
                    stock.put("changePercent", Double.parseDouble(quote.get("10. change percent").asText().replace("%", "")));
                    stock.put("volume", Long.parseLong(quote.get("06. volume").asText()));
                    stock.put("timestamp", quote.get("07. latest trading day").asText());
                    stock.put("real", true);
                    
                    logger.info("✅ Real stock data fetched for {}", symbol);
                    return stock;
                }
            }
            logger.warn("Alpha Vantage API returned status: {}", response.statusCode());
            return null;
        } catch (Exception e) {
            logger.warn("Failed to fetch real stock data: {}", e.getMessage());
            return null;
        }
    }

    private void handleQuoteRequest(Event event) {
        try {
            Map<String, Object> request = event.getPayload(Map.class);
            String symbol = ((String) request.getOrDefault("symbol", "AAPL")).toUpperCase();
            
            logMessage("📊 Stock quote request for: " + symbol);
            
            Map<String, Object> stock = fetchRealStockData(symbol);
            if (stock == null) {
                stock = STOCK_DATA.getOrDefault(symbol, STOCK_DATA.get("AAPL"));
            }
            
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

    private void handlePortfolioRequest(Event event) {
        try {
            logMessage("💼 Portfolio analysis requested");
            
            Map<String, Object> portfolio = new HashMap<>();
            portfolio.put("stocks", STOCK_DATA.keySet());
            portfolio.put("count", STOCK_DATA.size());
            portfolio.put("timestamp", System.currentTimeMillis());
            
            double totalValue = 0;
            double totalChange = 0;
            for (Map<String, Object> stock : STOCK_DATA.values()) {
                totalValue += ((Number) stock.get("price")).doubleValue();
                totalChange += ((Number) stock.get("change")).doubleValue();
            }
            
            portfolio.put("totalValue", totalValue);
            portfolio.put("totalChange", totalChange);
            
            publishEvent("stock.portfolio.response", portfolio);
            logMessage("✅ Portfolio response sent");
            
        } catch (Exception e) {
            logger.error("Error handling portfolio request: {}", e.getMessage(), e);
        }
    }

    private void handleStatusRequest(Event event) {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("agent", "StockAgentReal");
            status.put("version", "1.6.0");
            status.put("status", "active");
            status.put("timestamp", System.currentTimeMillis());
            status.put("apiKey", ALPHA_VANTAGE_API_KEY.equals("demo") ? "demo (limited)" : "configured");
            status.put("stocksSupported", STOCK_DATA.keySet());
            status.put("dataSource", "alphavantage");
            
            publishEvent("stock.status.response", status);
            logMessage("✅ Status response sent");
            
        } catch (Exception e) {
            logger.error("Error handling status request: {}", e.getMessage(), e);
        }
    }

    @Override
    public void onDeactivate() {
        logMessage("👋 Stock Agent (Real Data) shutting down");
        super.onDeactivate();
    }
}
