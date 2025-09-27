package io.amcp.examples.travel;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Amadeus API client following AMCP patterns from WeatherCollectorAgent.
 * Handles OAuth2 authentication and Flight Offers Search integration.
 * Uses Java 8 HttpURLConnection for compatibility.
 */
public class AmadeusApiClient {
    
    // Amadeus API configuration
    private static final String TEST_API_BASE_URL = "https://test.api.amadeus.com";
    private static final String PROD_API_BASE_URL = "https://api.amadeus.com";
    private static final String TOKEN_ENDPOINT = "/v1/security/oauth2/token";
    private static final String FLIGHT_OFFERS_ENDPOINT = "/v2/shopping/flight-offers";
    
    // Environment variable keys for API credentials
    private static final String API_KEY_ENV = "AMADEUS_API_KEY";
    private static final String API_SECRET_ENV = "AMADEUS_API_SECRET";
    private static final String USE_PRODUCTION_ENV = "AMADEUS_USE_PRODUCTION";
    
    // Default credentials for testing (would be replaced with real credentials)
    private static final String DEFAULT_TEST_API_KEY = "your_test_api_key";
    private static final String DEFAULT_TEST_API_SECRET = "your_test_api_secret";
    
    // Configuration
    private final String apiKey;
    private final String apiSecret;
    private final String baseUrl;
    private final boolean useProduction;
    
    // OAuth2 state management
    private final AtomicReference<String> accessToken = new AtomicReference<>();
    private final AtomicReference<Instant> tokenExpiresAt = new AtomicReference<>();
    
    // API call tracking (following weather agent pattern)
    private final AtomicLong apiCallCounter = new AtomicLong(0);
    private final String clientId;
    
    public AmadeusApiClient(String clientId) {
        this.clientId = clientId != null ? clientId : "amadeus-client-" + System.currentTimeMillis();
        
        // Load configuration from environment variables with fallback to defaults
        this.apiKey = getEnvironmentVariable(API_KEY_ENV, DEFAULT_TEST_API_KEY);
        this.apiSecret = getEnvironmentVariable(API_SECRET_ENV, DEFAULT_TEST_API_SECRET);
        this.useProduction = Boolean.parseBoolean(getEnvironmentVariable(USE_PRODUCTION_ENV, "false"));
        this.baseUrl = useProduction ? PROD_API_BASE_URL : TEST_API_BASE_URL;
        
        log("AmadeusApiClient initialized for " + clientId);
        log("Environment: " + (useProduction ? "PRODUCTION" : "TEST"));
        log("API Key type: " + (apiKey.equals(DEFAULT_TEST_API_KEY) ? "default" : "custom"));
    }
    
    /**
     * Search for flight offers using the Amadeus Flight Offers Search API.
     * Follows the same pattern as weather API integration.
     */
    public FlightOffersSearchResult searchFlightOffers(FlightSearchRequest request) throws IOException {
        if (request == null) {
            throw new IllegalArgumentException("FlightSearchRequest cannot be null");
        }
        
        long callNumber = apiCallCounter.incrementAndGet();
        long startTime = System.currentTimeMillis();
        
        try {
            log("API Call #" + callNumber + " - Flight Offers Search: " + 
                request.getOrigin() + " -> " + request.getDestination());
            
            // Ensure we have a valid access token
            ensureValidAccessToken();
            
            // Build the request URL with parameters
            String requestUrl = buildFlightOffersUrl(request);
            
            // Make the API call
            String jsonResponse = makeHttpGetRequest(requestUrl, accessToken.get());
            
            // Parse the response
            FlightOffersSearchResult result = parseFlightOffersResponse(jsonResponse, request);
            
            long duration = System.currentTimeMillis() - startTime;
            log("API Call #" + callNumber + " successful - found " + 
                result.getFlightOffers().size() + " offers (took " + duration + "ms)");
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log("API Call #" + callNumber + " failed: " + e.getMessage() + " (took " + duration + "ms)");
            throw new IOException("Flight offers search failed", e);
        }
    }
    
    /**
     * Ensures we have a valid OAuth2 access token, refreshing if necessary.
     */
    private void ensureValidAccessToken() throws IOException {
        Instant now = Instant.now();
        Instant expiresAt = tokenExpiresAt.get();
        
        // Check if we need to obtain or refresh the token
        if (accessToken.get() == null || expiresAt == null || now.isAfter(expiresAt.minusSeconds(60))) {
            log("Obtaining new OAuth2 access token from Amadeus");
            obtainAccessToken();
        }
    }
    
    /**
     * Obtains a new OAuth2 access token using client credentials flow.
     */
    private void obtainAccessToken() throws IOException {
        String tokenUrl = baseUrl + TOKEN_ENDPOINT;
        
        // Prepare the request body for OAuth2 client credentials grant
        String requestBody = "grant_type=client_credentials&client_id=" + 
                           URLEncoder.encode(apiKey, "UTF-8") + 
                           "&client_secret=" + URLEncoder.encode(apiSecret, "UTF-8");
        
        HttpURLConnection connection = null;
        try {
            URL url = new URL(tokenUrl);
            connection = (HttpURLConnection) url.openConnection();
            
            // Configure the request
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("User-Agent", "AMCP-TravelAgent/1.0");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setDoOutput(true);
            
            // Send the request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200) {
                // Read and parse the token response
                String response = readResponse(connection.getInputStream());
                parseTokenResponse(response);
                log("Successfully obtained OAuth2 access token");
            } else {
                String errorResponse = readResponse(connection.getErrorStream());
                throw new IOException("OAuth2 token request failed - HTTP " + responseCode + ": " + errorResponse);
            }
            
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Parses the OAuth2 token response and updates the token state.
     */
    private void parseTokenResponse(String jsonResponse) {
        try {
            // Simple JSON parsing for access_token and expires_in
            String token = extractJsonValue(jsonResponse, "\"access_token\":");
            String expiresInStr = extractJsonValue(jsonResponse, "\"expires_in\":");
            
            if (token.isEmpty()) {
                throw new IOException("No access token in response");
            }
            
            long expiresIn = expiresInStr.isEmpty() ? 3600 : Long.parseLong(expiresInStr);
            Instant expiresAt = Instant.now().plusSeconds(expiresIn);
            
            accessToken.set(token);
            tokenExpiresAt.set(expiresAt);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse token response", e);
        }
    }
    
    /**
     * Builds the flight offers search URL with query parameters.
     */
    private String buildFlightOffersUrl(FlightSearchRequest request) throws UnsupportedEncodingException {
        StringBuilder url = new StringBuilder(baseUrl + FLIGHT_OFFERS_ENDPOINT);
        url.append("?");
        
        // Required parameters
        url.append("originLocationCode=").append(URLEncoder.encode(request.getOrigin(), "UTF-8"));
        url.append("&destinationLocationCode=").append(URLEncoder.encode(request.getDestination(), "UTF-8"));
        url.append("&departureDate=").append(URLEncoder.encode(request.getDepartureDate().toString(), "UTF-8"));
        url.append("&adults=").append(request.getAdults());
        
        // Optional parameters
        if (request.getChildren() > 0) {
            url.append("&children=").append(request.getChildren());
        }
        
        if (request.getReturnDate() != null) {
            url.append("&returnDate=").append(URLEncoder.encode(request.getReturnDate().toString(), "UTF-8"));
        }
        
        url.append("&travelClass=").append(URLEncoder.encode(request.getCabinClass(), "UTF-8"));
        url.append("&currencyCode=").append(URLEncoder.encode(request.getCurrencyCode(), "UTF-8"));
        url.append("&max=").append(request.getMaxResults());
        
        if (request.isNonStop()) {
            url.append("&nonStop=true");
        }
        
        return url.toString();
    }
    
    /**
     * Makes an authenticated HTTP GET request to the Amadeus API.
     */
    private String makeHttpGetRequest(String urlString, String bearerToken) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
            connection.setRequestProperty("User-Agent", "AMCP-TravelAgent/1.0");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000); // Flight search can take longer
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200) {
                return readResponse(connection.getInputStream());
            } else {
                String errorResponse = readResponse(connection.getErrorStream());
                throw new IOException("HTTP " + responseCode + ": " + errorResponse);
            }
            
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Reads the response from an input stream.
     */
    private String readResponse(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }
    
    /**
     * Parses the flight offers search response from Amadeus API.
     * Simplified JSON parsing without external dependencies.
     */
    private FlightOffersSearchResult parseFlightOffersResponse(String jsonResponse, FlightSearchRequest originalRequest) {
        try {
            // For this example, we'll create a simplified parser
            // In production, you might want to use Jackson or similar
            
            FlightOffersSearchResult.Builder resultBuilder = FlightOffersSearchResult.builder()
                    .originalRequest(originalRequest)
                    .retrievedAt(Instant.now())
                    .rawResponse(jsonResponse);
            
            // Simple parsing to extract the number of offers
            // This would need to be expanded for full parsing
            String dataSection = extractJsonSection(jsonResponse, "\"data\":");
            
            if (!dataSection.isEmpty()) {
                // Count the number of flight offers in the data array
                int offerCount = countJsonArrayElements(dataSection);
                resultBuilder.resultCount(offerCount);
                
                log("Parsed flight offers response: " + offerCount + " offers found");
            }
            
            // For now, return basic result structure
            // Full implementation would parse individual flight offers
            return resultBuilder.build();
            
        } catch (Exception e) {
            log("Error parsing flight offers response: " + e.getMessage());
            return FlightOffersSearchResult.builder()
                    .originalRequest(originalRequest)
                    .retrievedAt(Instant.now())
                    .rawResponse(jsonResponse)
                    .resultCount(0)
                    .error("Parsing failed: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * Helper method to extract JSON values (from weather agent pattern).
     */
    private String extractJsonValue(String json, String key) {
        try {
            int keyIndex = json.indexOf(key);
            if (keyIndex == -1) return "";
            
            int startIndex = json.indexOf(':', keyIndex) + 1;
            if (startIndex <= keyIndex) return "";
            
            // Skip whitespace
            while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
                startIndex++;
            }
            
            if (startIndex >= json.length()) return "";
            
            int endIndex;
            if (json.charAt(startIndex) == '"') {
                // String value
                startIndex++; // Skip opening quote
                endIndex = json.indexOf('"', startIndex);
                if (endIndex == -1) return "";
            } else {
                // Numeric value
                endIndex = startIndex;
                while (endIndex < json.length() && 
                       (Character.isDigit(json.charAt(endIndex)) || 
                        json.charAt(endIndex) == '.' || 
                        json.charAt(endIndex) == '-')) {
                    endIndex++;
                }
            }
            
            return json.substring(startIndex, endIndex);
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * Extracts a JSON section (array or object).
     */
    private String extractJsonSection(String json, String key) {
        try {
            int keyIndex = json.indexOf(key);
            if (keyIndex == -1) return "";
            
            int startIndex = json.indexOf(':', keyIndex) + 1;
            
            // Skip whitespace
            while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
                startIndex++;
            }
            
            if (startIndex >= json.length()) return "";
            
            char startChar = json.charAt(startIndex);
            char endChar;
            int depth = 1;
            int endIndex = startIndex + 1;
            
            if (startChar == '[') {
                endChar = ']';
            } else if (startChar == '{') {
                endChar = '}';
            } else {
                return "";
            }
            
            while (endIndex < json.length() && depth > 0) {
                char c = json.charAt(endIndex);
                if (c == startChar) {
                    depth++;
                } else if (c == endChar) {
                    depth--;
                }
                endIndex++;
            }
            
            if (depth == 0) {
                return json.substring(startIndex, endIndex);
            }
            
            return "";
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * Counts elements in a JSON array (simplified implementation).
     */
    private int countJsonArrayElements(String jsonArray) {
        if (jsonArray.isEmpty() || !jsonArray.startsWith("[")) {
            return 0;
        }
        
        // Simple count of top-level objects in array
        int count = 0;
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        
        for (int i = 1; i < jsonArray.length() - 1; i++) { // Skip outer brackets
            char c = jsonArray.charAt(i);
            
            if (escape) {
                escape = false;
                continue;
            }
            
            if (c == '\\') {
                escape = true;
                continue;
            }
            
            if (c == '"' && !escape) {
                inString = !inString;
                continue;
            }
            
            if (inString) {
                continue;
            }
            
            if (c == '{' || c == '[') {
                if (depth == 0 && c == '{') {
                    count++; // Found start of new object
                }
                depth++;
            } else if (c == '}' || c == ']') {
                depth--;
            }
        }
        
        return count;
    }
    
    /**
     * Gets environment variable with fallback to default value.
     */
    private String getEnvironmentVariable(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null && !value.trim().isEmpty() ? value.trim() : defaultValue;
    }
    
    /**
     * Logging method following the weather agent pattern.
     */
    private void log(String message) {
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        System.out.println("[" + timestamp + "] [" + clientId + "] " + message);
    }
    
    // Public API methods
    
    public long getApiCallCount() {
        return apiCallCounter.get();
    }
    
    public String getApiKeyType() {
        return apiKey.equals(DEFAULT_TEST_API_KEY) ? "default" : "custom";
    }
    
    public boolean isUsingProduction() {
        return useProduction;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public boolean hasValidToken() {
        Instant expiresAt = tokenExpiresAt.get();
        return accessToken.get() != null && expiresAt != null && Instant.now().isBefore(expiresAt);
    }
}