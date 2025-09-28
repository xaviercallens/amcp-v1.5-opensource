package io.amcp.tools.mcp;

import io.amcp.tools.ToolConnector;
import io.amcp.tools.ToolRequest;
import io.amcp.tools.ToolResponse;

import java.net.URL;
import java.net.URI;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * AMCP v1.4 DuckDuckGo MCP Connector.
 * Provides web search capabilities using DuckDuckGo Instant Answer API
 * following the Model Context Protocol (MCP) specification.
 */
public class DuckDuckGoMCPConnector implements ToolConnector {
    
    private static final String TOOL_ID = "duckduckgo-mcp";
    private static final String TOOL_NAME = "DuckDuckGo Search";
    private static final String VERSION = "1.4.0";
    private static final String API_BASE_URL = "https://api.duckduckgo.com/";
    
    private volatile boolean initialized = false;
    private int timeoutMs = 30000;
    private String userAgent = "AMCP-v1.4-Agent/1.0";
    
    @Override
    public String getToolId() {
        return TOOL_ID;
    }
    
    @Override
    public String getToolName() {
        return TOOL_NAME;
    }
    
    @Override
    public String getVersion() {
        return VERSION;
    }
    
    @Override
    public CompletableFuture<Boolean> isHealthy() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = URI.create(API_BASE_URL + "?q=test&format=json&no_html=1&skip_disambig=1").toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("User-Agent", userAgent);
                
                int responseCode = connection.getResponseCode();
                connection.disconnect();
                return responseCode == 200;
            } catch (Exception e) {
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> initialize(Map<String, Object> config) {
        return CompletableFuture.runAsync(() -> {
            if (config != null) {
                Integer timeout = (Integer) config.get("timeoutMs");
                if (timeout != null && timeout > 0) {
                    this.timeoutMs = timeout;
                }
                
                String ua = (String) config.get("userAgent");
                if (ua != null && !ua.trim().isEmpty()) {
                    this.userAgent = ua;
                }
            }
            this.initialized = true;
        });
    }
    
    @Override
    public CompletableFuture<ToolResponse> invoke(ToolRequest request) {
        if (!initialized) {
            return CompletableFuture.completedFuture(
                ToolResponse.error("Connector not initialized", request.getRequestId(), 0)
            );
        }
        
        long startTime = System.currentTimeMillis();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String operation = request.getOperation();
                
                switch (operation) {
                    case "search":
                        return performSearch(request, startTime);
                    case "instant_answer":
                        return getInstantAnswer(request, startTime);
                    case "suggestions":
                        return getSuggestions(request, startTime);
                    default:
                        long duration = System.currentTimeMillis() - startTime;
                        return ToolResponse.error("Unsupported operation: " + operation, 
                                                request.getRequestId(), duration);
                }
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                return ToolResponse.error("Search failed: " + e.getMessage(), 
                                        request.getRequestId(), duration);
            }
        });
    }
    
    private ToolResponse performSearch(ToolRequest request, long startTime) throws Exception {
        String query = request.getStringParameter("query");
        if (query == null || query.trim().isEmpty()) {
            long duration = System.currentTimeMillis() - startTime;
            return ToolResponse.error("Query parameter is required", request.getRequestId(), duration);
        }
        
        String encodedQuery = URLEncoder.encode(query.trim(), "UTF-8");
        String urlStr = API_BASE_URL + "?q=" + encodedQuery + "&format=json&no_html=1&skip_disambig=1";
        
        String jsonResponse = makeHttpRequest(urlStr);
        Map<String, Object> searchResult = parseSearchResponse(jsonResponse, query);
        
        long duration = System.currentTimeMillis() - startTime;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("query", query);
        metadata.put("api_endpoint", "search");
        
        return ToolResponse.success(searchResult, request.getRequestId(), duration, metadata);
    }
    
    private ToolResponse getInstantAnswer(ToolRequest request, long startTime) throws Exception {
        String query = request.getStringParameter("query");
        if (query == null || query.trim().isEmpty()) {
            long duration = System.currentTimeMillis() - startTime;
            return ToolResponse.error("Query parameter is required", request.getRequestId(), duration);
        }
        
        String encodedQuery = URLEncoder.encode(query.trim(), "UTF-8");
        String urlStr = API_BASE_URL + "?q=" + encodedQuery + "&format=json&no_html=1";
        
        String jsonResponse = makeHttpRequest(urlStr);
        Map<String, Object> instantAnswer = parseInstantAnswer(jsonResponse, query);
        
        long duration = System.currentTimeMillis() - startTime;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("query", query);
        metadata.put("api_endpoint", "instant_answer");
        
        return ToolResponse.success(instantAnswer, request.getRequestId(), duration, metadata);
    }
    
    private ToolResponse getSuggestions(ToolRequest request, long startTime) throws Exception {
        String query = request.getStringParameter("query");
        if (query == null || query.trim().isEmpty()) {
            long duration = System.currentTimeMillis() - startTime;
            return ToolResponse.error("Query parameter is required", request.getRequestId(), duration);
        }
        
        // DuckDuckGo doesn't provide direct suggestion API, so we'll use autocomplete approach
        String encodedQuery = URLEncoder.encode(query.trim(), "UTF-8");
        String urlStr = "https://duckduckgo.com/ac/?q=" + encodedQuery + "&type=list";
        
        String jsonResponse = makeHttpRequest(urlStr);
        Map<String, Object> suggestions = parseSuggestions(jsonResponse, query);
        
        long duration = System.currentTimeMillis() - startTime;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("query", query);
        metadata.put("api_endpoint", "suggestions");
        
        return ToolResponse.success(suggestions, request.getRequestId(), duration, metadata);
    }
    
    private String makeHttpRequest(String urlStr) throws Exception {
        URL url = URI.create(urlStr).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);
            connection.setRequestProperty("User-Agent", userAgent);
            connection.setRequestProperty("Accept", "application/json");
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("HTTP " + responseCode + " response from DuckDuckGo API");
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            
            return response.toString();
        } finally {
            connection.disconnect();
        }
    }
    
    private Map<String, Object> parseSearchResponse(String jsonResponse, String query) {
        Map<String, Object> result = new HashMap<>();
        result.put("query", query);
        result.put("type", "search_results");
        
        // Simple JSON parsing for key fields
        result.put("abstract", extractJsonField(jsonResponse, "Abstract"));
        result.put("abstract_text", extractJsonField(jsonResponse, "AbstractText"));
        result.put("abstract_source", extractJsonField(jsonResponse, "AbstractSource"));
        result.put("abstract_url", extractJsonField(jsonResponse, "AbstractURL"));
        result.put("image", extractJsonField(jsonResponse, "Image"));
        result.put("heading", extractJsonField(jsonResponse, "Heading"));
        result.put("answer", extractJsonField(jsonResponse, "Answer"));
        result.put("answer_type", extractJsonField(jsonResponse, "AnswerType"));
        
        // Extract definition if available
        String definition = extractJsonField(jsonResponse, "Definition");
        if (definition != null && !definition.isEmpty()) {
            result.put("definition", definition);
            result.put("definition_source", extractJsonField(jsonResponse, "DefinitionSource"));
            result.put("definition_url", extractJsonField(jsonResponse, "DefinitionURL"));
        }
        
        result.put("raw_response", jsonResponse);
        return result;
    }
    
    private Map<String, Object> parseInstantAnswer(String jsonResponse, String query) {
        Map<String, Object> result = new HashMap<>();
        result.put("query", query);
        result.put("type", "instant_answer");
        
        String answer = extractJsonField(jsonResponse, "Answer");
        String answerType = extractJsonField(jsonResponse, "AnswerType");
        
        if (answer != null && !answer.isEmpty()) {
            result.put("answer", answer);
            result.put("answer_type", answerType);
            result.put("has_instant_answer", true);
        } else {
            result.put("has_instant_answer", false);
            result.put("abstract", extractJsonField(jsonResponse, "AbstractText"));
            result.put("abstract_source", extractJsonField(jsonResponse, "AbstractSource"));
        }
        
        return result;
    }
    
    private Map<String, Object> parseSuggestions(String jsonResponse, String query) {
        Map<String, Object> result = new HashMap<>();
        result.put("query", query);
        result.put("type", "suggestions");
        
        // Parse JSON array response for suggestions
        // Simple parsing approach for JSON arrays
        if (jsonResponse.startsWith("[") && jsonResponse.endsWith("]")) {
            String content = jsonResponse.substring(1, jsonResponse.length() - 1);
            String[] suggestions = content.split(",");
            
            String[] cleanSuggestions = new String[suggestions.length];
            for (int i = 0; i < suggestions.length; i++) {
                cleanSuggestions[i] = suggestions[i].trim().replaceAll("^\"|\"$", "");
            }
            result.put("suggestions", cleanSuggestions);
        } else {
            result.put("suggestions", new String[0]);
        }
        
        return result;
    }
    
    private String extractJsonField(String json, String fieldName) {
        // Simple JSON field extraction
        String pattern = "\"" + fieldName + "\"\\s*:\\s*\"([^\"]*?)\"";
        Pattern regex = Pattern.compile(pattern, Pattern.DOTALL);
        Matcher matcher = regex.matcher(json);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    @Override
    public Map<String, Object> getRequestSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        // Query parameter
        Map<String, Object> queryParam = new HashMap<>();
        queryParam.put("type", "string");
        queryParam.put("description", "Search query string");
        queryParam.put("required", true);
        properties.put("query", queryParam);
        
        schema.put("properties", properties);
        schema.put("required", new String[]{"query"});
        
        return schema;
    }
    
    @Override
    public String[] getSupportedOperations() {
        return new String[]{"search", "instant_answer", "suggestions"};
    }
    
    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            this.initialized = false;
        });
    }
}