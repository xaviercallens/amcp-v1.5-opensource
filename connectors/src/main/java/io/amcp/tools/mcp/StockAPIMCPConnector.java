package io.amcp.tools.mcp;

import io.amcp.tools.ToolConnector;
import io.amcp.tools.ToolRequest;
import io.amcp.tools.ToolResponse;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;

/**
 * AMCP v1.5 Stock API MCP Connector.
 * Provides real-time stock market data using Polygon.io API
 * following the Model Context Protocol (MCP) specification.
 * 
 * Supported operations:
 * - stock_quote: Get real-time stock quote for a symbol
 * - stock_search: Search for stocks by company name or ticker
 * - market_status: Get current market status
 * - stock_news: Get latest news for a stock
 * - aggregates: Get historical price aggregates for a stock
 * 
 * @author AMCP Team
 * @version 1.5.0
 * @since 2024-12-18
 */
public class StockAPIMCPConnector implements ToolConnector {
    
    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl = "https://api.polygon.io";
    private volatile boolean initialized = false;
    
    /**
     * Constructor with API key configuration.
     * 
     * @param apiKey The Polygon.io API key
     */
    public StockAPIMCPConnector(String apiKey) {
        this.apiKey = apiKey != null ? apiKey : System.getenv("POLYGON_API_KEY");
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        
        if (this.apiKey != null && !this.apiKey.trim().isEmpty()) {
            this.initialized = true;
        }
    }
    
    /**
     * Default constructor that reads API key from environment.
     */
    public StockAPIMCPConnector() {
        this(null);
    }
    
    @Override
    public String getToolId() {
        return "stock-api-mcp";
    }
    
    @Override
    public String getToolName() {
        return "Stock API";
    }
    
    @Override
    public String getVersion() {
        return "1.5.0";
    }
    
    @Override
    public CompletableFuture<Boolean> isHealthy() {
        return CompletableFuture.completedFuture(initialized);
    }
    
    @Override
    public CompletableFuture<Void> initialize(Map<String, Object> config) {
        return CompletableFuture.runAsync(() -> {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                System.err.println("Warning: POLYGON_API_KEY not found. Stock API connector will not work.");
                initialized = false;
            } else {
                initialized = true;
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            // HTTP client cleanup is automatic
            initialized = false;
        });
    }
    
    @Override
    public CompletableFuture<ToolResponse> invoke(ToolRequest request) {
        long startTime = System.currentTimeMillis();
        return CompletableFuture.supplyAsync(() -> {
            if (!initialized) {
                return ToolResponse.error("Stock API connector not initialized", 
                                        request.getRequestId(), 
                                        System.currentTimeMillis() - startTime);
            }
            
            try {
                String operation = request.getOperation();
                Map<String, Object> params = request.getParameters();
                
                switch (operation) {
                    case "stock_quote":
                        return getStockQuote(params, request.getRequestId(), startTime);
                    case "stock_search":
                        return searchStocks(params, request.getRequestId(), startTime);
                    case "market_status":
                        return getMarketStatus(params, request.getRequestId(), startTime);
                    case "stock_news":
                        return getStockNews(params, request.getRequestId(), startTime);
                    case "aggregates":
                        return getAggregates(params, request.getRequestId(), startTime);
                    default:
                        return ToolResponse.error("Unknown operation: " + operation, 
                                                request.getRequestId(), 
                                                System.currentTimeMillis() - startTime);
                }
            } catch (Exception e) {
                return ToolResponse.error("Stock API error: " + e.getMessage(), 
                                        request.getRequestId(), 
                                        System.currentTimeMillis() - startTime);
            }
        });
    }

    @Override
    public String[] getSupportedOperations() {
        return new String[]{
            "stock_quote",
            "stock_search", 
            "market_status",
            "stock_news",
            "aggregates"
        };
    }
    
    @Override
    public Map<String, Object> getRequestSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", createSchemaProperties());
        return schema;
    }
    
    private Map<String, Object> createSchemaProperties() {
        Map<String, Object> properties = new HashMap<>();
        
        // Stock quote operation
        Map<String, Object> stockQuote = new HashMap<>();
        stockQuote.put("type", "object");
        stockQuote.put("description", "Get real-time stock quote");
        stockQuote.put("required", new String[]{"symbol"});
        Map<String, Object> quoteProps = new HashMap<>();
        quoteProps.put("symbol", Map.of("type", "string", "description", "Stock ticker symbol"));
        stockQuote.put("properties", quoteProps);
        properties.put("stock_quote", stockQuote);
        
        return properties;
    }
    
    private ToolResponse getStockQuote(Map<String, Object> params, String requestId, long startTime) {
        try {
            Object symbolObj = params.get("symbol");
            if (symbolObj == null) {
                return ToolResponse.error("Symbol parameter is required", 
                                        requestId, 
                                        System.currentTimeMillis() - startTime);
            }
            
            String symbol = symbolObj.toString().toUpperCase();
            String url = baseUrl + "/v2/aggs/ticker/" + symbol + "/prev?apikey=" + apiKey;
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                Map<String, Object> result = parseStockQuoteResponse(response.body(), symbol);
                return ToolResponse.success(result, requestId, System.currentTimeMillis() - startTime);
            } else {
                return ToolResponse.error("API request failed: " + response.statusCode(), 
                                        requestId, 
                                        System.currentTimeMillis() - startTime);
            }
        } catch (Exception e) {
            return ToolResponse.error("Failed to get stock quote: " + e.getMessage(), 
                                    requestId, 
                                    System.currentTimeMillis() - startTime);
        }
    }
    
    private ToolResponse searchStocks(Map<String, Object> params, String requestId, long startTime) {
        try {
            Object queryObj = params.get("query");
            if (queryObj == null) {
                return ToolResponse.error("Query parameter is required", 
                                        requestId, 
                                        System.currentTimeMillis() - startTime);
            }
            
            String query = queryObj.toString();
            String url = baseUrl + "/v3/reference/tickers?search=" + query + "&apikey=" + apiKey;
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                Map<String, Object> result = parseSearchResponse(response.body());
                return ToolResponse.success(result, requestId, System.currentTimeMillis() - startTime);
            } else {
                return ToolResponse.error("API request failed: " + response.statusCode(), 
                                        requestId, 
                                        System.currentTimeMillis() - startTime);
            }
        } catch (Exception e) {
            return ToolResponse.error("Failed to search stocks: " + e.getMessage(), 
                                    requestId, 
                                    System.currentTimeMillis() - startTime);
        }
    }
    
    private ToolResponse getMarketStatus(Map<String, Object> params, String requestId, long startTime) {
        try {
            String url = baseUrl + "/v1/marketstatus/now?apikey=" + apiKey;
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                Map<String, Object> result = parseMarketStatusResponse(response.body());
                return ToolResponse.success(result, requestId, System.currentTimeMillis() - startTime);
            } else {
                return ToolResponse.error("API request failed: " + response.statusCode(), 
                                        requestId, 
                                        System.currentTimeMillis() - startTime);
            }
        } catch (Exception e) {
            return ToolResponse.error("Failed to get market status: " + e.getMessage(), 
                                    requestId, 
                                    System.currentTimeMillis() - startTime);
        }
    }
    
    private ToolResponse getStockNews(Map<String, Object> params, String requestId, long startTime) {
        try {
            Object tickerObj = params.get("ticker");
            if (tickerObj == null) {
                return ToolResponse.error("Ticker parameter is required", 
                                        requestId, 
                                        System.currentTimeMillis() - startTime);
            }
            
            String ticker = tickerObj.toString().toUpperCase();
            String url = baseUrl + "/v2/reference/news?ticker=" + ticker + "&limit=10&apikey=" + apiKey;
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                Map<String, Object> result = parseNewsResponse(response.body());
                return ToolResponse.success(result, requestId, System.currentTimeMillis() - startTime);
            } else {
                return ToolResponse.error("API request failed: " + response.statusCode(), 
                                        requestId, 
                                        System.currentTimeMillis() - startTime);
            }
        } catch (Exception e) {
            return ToolResponse.error("Failed to get stock news: " + e.getMessage(), 
                                    requestId, 
                                    System.currentTimeMillis() - startTime);
        }
    }
    
    private ToolResponse getAggregates(Map<String, Object> params, String requestId, long startTime) {
        try {
            Object tickerObj = params.get("ticker");
            if (tickerObj == null) {
                return ToolResponse.error("Ticker parameter is required", 
                                        requestId, 
                                        System.currentTimeMillis() - startTime);
            }
            
            String ticker = tickerObj.toString().toUpperCase();
            String multiplier = params.getOrDefault("multiplier", "1").toString();
            String timespan = params.getOrDefault("timespan", "day").toString();
            String from = params.getOrDefault("from", "2024-01-01").toString();
            String to = params.getOrDefault("to", "2024-12-31").toString();
            
            String url = baseUrl + "/v2/aggs/ticker/" + ticker + "/range/" + multiplier + "/" + timespan + "/" + from + "/" + to + "?apikey=" + apiKey;
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                Map<String, Object> result = parseAggregatesResponse(response.body());
                return ToolResponse.success(result, requestId, System.currentTimeMillis() - startTime);
            } else {
                return ToolResponse.error("API request failed: " + response.statusCode(), 
                                        requestId, 
                                        System.currentTimeMillis() - startTime);
            }
        } catch (Exception e) {
            return ToolResponse.error("Failed to get aggregates: " + e.getMessage(), 
                                    requestId, 
                                    System.currentTimeMillis() - startTime);
        }
    }
    
    // Response parsing methods (simplified JSON parsing)
    
    private Map<String, Object> parseStockQuoteResponse(String json, String symbol) {
        Map<String, Object> result = new HashMap<>();
        result.put("symbol", symbol);
        result.put("timestamp", LocalDateTime.now().toString());
        
        // Simple JSON parsing for demonstration - in production use a proper JSON library
        try {
            if (json.contains("\"results\"")) {
                // Extract values using simple string manipulation
                double close = extractJsonNumber(json, "\"c\":");
                double open = extractJsonNumber(json, "\"o\":");
                double high = extractJsonNumber(json, "\"h\":");
                double low = extractJsonNumber(json, "\"l\":");
                long volume = (long) extractJsonNumber(json, "\"v\":");
                
                result.put("price", close);
                result.put("open", open);
                result.put("high", high);
                result.put("low", low);
                result.put("volume", volume);
                result.put("change", close - open);
                result.put("change_percent", ((close - open) / open) * 100);
            }
        } catch (Exception e) {
            result.put("error", "Failed to parse response: " + e.getMessage());
        }
        
        return result;
    }
    
    private Map<String, Object> parseSearchResponse(String json) {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("source", "polygon.io");
        
        // Simple parsing for demonstration
        result.put("count", json.contains("\"results\"") ? 1 : 0);
        result.put("raw_response", json.length() > 500 ? json.substring(0, 500) + "..." : json);
        
        return result;
    }
    
    private Map<String, Object> parseMarketStatusResponse(String json) {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("market", "US");
        
        // Simple parsing for market status
        result.put("open", json.contains("\"open\"") && json.contains("true"));
        result.put("raw_response", json.length() > 200 ? json.substring(0, 200) + "..." : json);
        
        return result;
    }
    
    private Map<String, Object> parseNewsResponse(String json) {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("source", "polygon.io");
        
        // Count articles by counting occurrences of "title"
        int articleCount = json.split("\"title\"").length - 1;
        result.put("articles_found", articleCount);
        result.put("raw_response", json.length() > 1000 ? json.substring(0, 1000) + "..." : json);
        
        return result;
    }
    
    private Map<String, Object> parseAggregatesResponse(String json) {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("source", "polygon.io");
        
        // Count data points
        int dataPoints = json.split("\"c\":").length - 1;
        result.put("data_points", dataPoints);
        result.put("raw_response", json.length() > 1000 ? json.substring(0, 1000) + "..." : json);
        
        return result;
    }
    
    private double extractJsonNumber(String json, String key) {
        try {
            int startIndex = json.indexOf(key);
            if (startIndex == -1) return 0.0;
            
            startIndex += key.length();
            int endIndex = startIndex;
            
            // Skip whitespace
            while (endIndex < json.length() && Character.isWhitespace(json.charAt(endIndex))) {
                endIndex++;
            }
            
            // Find end of number
            while (endIndex < json.length()) {
                char c = json.charAt(endIndex);
                if (Character.isDigit(c) || c == '.' || c == '-' || c == '+' || c == 'e' || c == 'E') {
                    endIndex++;
                } else {
                    break;
                }
            }
            
            String numberStr = json.substring(startIndex, endIndex).trim();
            return Double.parseDouble(numberStr);
        } catch (Exception e) {
            return 0.0;
        }
    }
}