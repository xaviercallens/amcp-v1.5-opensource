package io.amcp.connectors.ai.normalization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Data normalization engine for LLM responses and agent communication.
 * 
 * @author AMCP Development Team
 * @version 1.5.0
 */
public class DataNormalizationEngine {
    
    private final ObjectMapper objectMapper;
    private final Map<String, Pattern> normalizationPatterns;
    private final LocationNormalizer locationNormalizer;
    
    public DataNormalizationEngine() {
        this.objectMapper = new ObjectMapper();
        this.normalizationPatterns = initializePatterns();
        this.locationNormalizer = new LocationNormalizer();
    }
    
    /**
     * Normalizes raw LLM response data into structured format.
     */
    public Map<String, Object> normalizeLLMResponse(String rawResponse) {
        Map<String, Object> normalized = new HashMap<>();
        
        try {
            // Try to parse as JSON first
            JsonNode jsonNode = objectMapper.readTree(rawResponse);
            normalized = objectMapper.convertValue(jsonNode, Map.class);
        } catch (Exception e) {
            // If not JSON, apply text normalization
            normalized = normalizeTextResponse(rawResponse);
        }
        
        // Apply common normalizations
        return applyCommonNormalizations(normalized);
    }
    
    /**
     * Normalizes event data for consistent processing.
     */
    public Map<String, Object> normalizeEventData(Map<String, Object> eventData) {
        if (eventData == null) return new HashMap<>();
        
        Map<String, Object> normalized = new HashMap<>(eventData);
        
        // Standardize keys
        normalized = standardizeKeys(normalized);
        
        // Clean values
        normalized = cleanValues(normalized);
        
        return normalized;
    }
    
    /**
     * Extracts structured data from free-form text.
     */
    public Map<String, Object> extractStructuredData(String text) {
        Map<String, Object> extracted = new HashMap<>();
        
        // Extract JSON blocks
        extractJsonBlocks(text, extracted);
        
        // Extract key-value pairs
        extractKeyValuePairs(text, extracted);
        
        // Extract lists
        extractLists(text, extracted);
        
        return extracted;
    }
    
    /**
     * Validates and cleans agent response data.
     */
    public Map<String, Object> validateAndClean(Map<String, Object> data) {
        if (data == null) return new HashMap<>();
        
        Map<String, Object> cleaned = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = cleanKey(entry.getKey());
            Object value = cleanValue(entry.getValue());
            
            if (key != null && !key.isEmpty() && value != null) {
                cleaned.put(key, value);
            }
        }
        
        return cleaned;
    }
    
    private Map<String, Pattern> initializePatterns() {
        Map<String, Pattern> patterns = new HashMap<>();
        patterns.put("json_block", Pattern.compile("\\{[^{}]*\\}", Pattern.DOTALL));
        patterns.put("key_value", Pattern.compile("(\\w+)\\s*[:|=]\\s*([^\\n]+)", Pattern.MULTILINE));
        patterns.put("list_item", Pattern.compile("^\\s*[-*•]\\s*(.+)$", Pattern.MULTILINE));
        patterns.put("quoted_string", Pattern.compile("\"([^\"]+)\""));
        return patterns;
    }
    
    private Map<String, Object> normalizeTextResponse(String text) {
        Map<String, Object> result = new HashMap<>();
        
        // Extract main content
        String cleanText = text.trim();
        result.put("content", cleanText);
        
        // Try to extract structured elements
        Map<String, Object> structured = extractStructuredData(cleanText);
        result.putAll(structured);
        
        return result;
    }
    
    private Map<String, Object> applyCommonNormalizations(Map<String, Object> data) {
        Map<String, Object> normalized = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = normalizeKey(entry.getKey());
            Object value = normalizeValue(entry.getValue());
            normalized.put(key, value);
        }
        
        return normalized;
    }
    
    private Map<String, Object> standardizeKeys(Map<String, Object> data) {
        Map<String, Object> standardized = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String standardKey = standardizeKey(entry.getKey());
            standardized.put(standardKey, entry.getValue());
        }
        
        return standardized;
    }
    
    private Map<String, Object> cleanValues(Map<String, Object> data) {
        Map<String, Object> cleaned = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object cleanValue = cleanValue(entry.getValue());
            if (cleanValue != null) {
                cleaned.put(entry.getKey(), cleanValue);
            }
        }
        
        return cleaned;
    }
    
    private void extractJsonBlocks(String text, Map<String, Object> extracted) {
        Pattern jsonPattern = normalizationPatterns.get("json_block");
        Matcher matcher = jsonPattern.matcher(text);
        
        List<Map<String, Object>> jsonBlocks = new ArrayList<>();
        while (matcher.find()) {
            try {
                String jsonText = matcher.group();
                JsonNode jsonNode = objectMapper.readTree(jsonText);
                Map<String, Object> jsonMap = objectMapper.convertValue(jsonNode, Map.class);
                jsonBlocks.add(jsonMap);
            } catch (Exception e) {
                // Skip invalid JSON
            }
        }
        
        if (!jsonBlocks.isEmpty()) {
            extracted.put("json_blocks", jsonBlocks);
        }
    }
    
    private void extractKeyValuePairs(String text, Map<String, Object> extracted) {
        Pattern kvPattern = normalizationPatterns.get("key_value");
        Matcher matcher = kvPattern.matcher(text);
        
        Map<String, String> pairs = new HashMap<>();
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String value = matcher.group(2).trim();
            pairs.put(key, value);
        }
        
        if (!pairs.isEmpty()) {
            extracted.put("key_value_pairs", pairs);
        }
    }
    
    private void extractLists(String text, Map<String, Object> extracted) {
        Pattern listPattern = normalizationPatterns.get("list_item");
        Matcher matcher = listPattern.matcher(text);
        
        List<String> items = new ArrayList<>();
        while (matcher.find()) {
            String item = matcher.group(1).trim();
            items.add(item);
        }
        
        if (!items.isEmpty()) {
            extracted.put("list_items", items);
        }
    }
    
    private String normalizeKey(String key) {
        if (key == null) return null;
        return key.toLowerCase().replaceAll("[^a-z0-9_]", "_");
    }
    
    private Object normalizeValue(Object value) {
        if (value instanceof String) {
            String strValue = (String) value;
            strValue = strValue.trim();
            
            // Try to convert to number
            try {
                if (strValue.contains(".")) {
                    return Double.parseDouble(strValue);
                } else {
                    return Long.parseLong(strValue);
                }
            } catch (NumberFormatException e) {
                // Keep as string
            }
            
            // Convert boolean strings
            if ("true".equalsIgnoreCase(strValue) || "false".equalsIgnoreCase(strValue)) {
                return Boolean.parseBoolean(strValue);
            }
            
            return strValue;
        }
        
        return value;
    }
    
    private String standardizeKey(String key) {
        if (key == null) return null;
        
        // Convert camelCase and PascalCase to snake_case
        String result = key.replaceAll("([a-z])([A-Z])", "$1_$2");
        return result.toLowerCase();
    }
    
    private String cleanKey(String key) {
        if (key == null) return null;
        
        String cleaned = key.trim();
        cleaned = cleaned.replaceAll("[^a-zA-Z0-9_]", "_");
        cleaned = cleaned.replaceAll("_+", "_");
        cleaned = cleaned.replaceAll("^_|_$", "");
        
        return cleaned.isEmpty() ? null : cleaned;
    }
    
    private Object cleanValue(Object value) {
        if (value == null) return null;
        
        if (value instanceof String) {
            String strValue = ((String) value).trim();
            return strValue.isEmpty() ? null : strValue;
        }
        
        if (value instanceof Map) {
            Map<?, ?> mapValue = (Map<?, ?>) value;
            return mapValue.isEmpty() ? null : validateAndClean((Map<String, Object>) mapValue);
        }
        
        if (value instanceof List) {
            List<?> listValue = (List<?>) value;
            List<Object> cleanedList = new ArrayList<>();
            for (Object item : listValue) {
                Object cleanedItem = cleanValue(item);
                if (cleanedItem != null) {
                    cleanedList.add(cleanedItem);
                }
            }
            return cleanedList.isEmpty() ? null : cleanedList;
        }
        
        return value;
    }
    
    /**
     * Normalizes user query text for better LLM processing.
     */
    public String normalizeQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            return "";
        }
        
        String normalized = rawQuery.trim();
        
        // Remove extra whitespace
        normalized = normalized.replaceAll("\\s+", " ");
        
        // Basic query normalization
        normalized = normalized.toLowerCase();
        
        // Remove common stop words for better processing (basic implementation)
        String[] stopWords = {"the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by"};
        for (String stopWord : stopWords) {
            normalized = normalized.replaceAll("\\b" + stopWord + "\\b", " ");
        }
        
        // Clean up extra spaces again
        normalized = normalized.replaceAll("\\s+", " ").trim();
        
        return normalized;
    }
    
    /**
     * Normalizes parameters for consistent LLM processing
     */
    public Map<String, Object> normalizeParameters(Map<String, Object> parameters, String context) {
        if (parameters == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> normalized = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // Normalize key to lowercase
            String normalizedKey = key.toLowerCase().replaceAll("[^a-z0-9_]", "_");
            
            // Normalize value based on type
            Object normalizedValue = normalizeValue(value, context);
            
            normalized.put(normalizedKey, normalizedValue);
        }
        
        return normalized;
    }
    
    private Object normalizeValue(Object value, String context) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof String) {
            String strValue = (String) value;
            // Basic string normalization
            return strValue.trim().replaceAll("\\s+", " ");
        }
        
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapValue = (Map<String, Object>) value;
            return normalizeParameters(mapValue, context);
        }
        
        // Return as-is for other types
        return value;
    }
    
    /**
     * Normalize agent response data
     */
    public Map<String, Object> normalizeAgentResponse(Map<String, Object> response, Set<String> expectedFields) {
        Map<String, Object> normalized = new HashMap<>();
        
        // Copy all response data
        if (response != null) {
            normalized.putAll(response);
        }
        
        // Ensure expected fields are present
        if (expectedFields != null) {
            for (String field : expectedFields) {
                if (!normalized.containsKey(field)) {
                    normalized.put(field, null);
                }
            }
        }
        
        // Add metadata
        normalized.put("normalized_at", System.currentTimeMillis());
        normalized.put("normalization_version", "1.5.0");
        
        return normalized;
    }
    
    /**
     * Normalizes location data (e.g., "Nice, Fr" -> "Nice,FR")
     */
    public String normalizeLocation(String location) {
        return locationNormalizer.normalizeLocation(location);
    }
    
    /**
     * Enriches location with full country name (e.g., "Nice,FR" -> "Nice, France")
     */
    public String enrichLocation(String location) {
        return locationNormalizer.enrichLocation(location);
    }
    
    /**
     * Normalizes date to ISO 8601 format (YYYY-MM-DD)
     */
    public String normalizeDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        
        // Try various date formats
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ISO_LOCAL_DATE,  // YYYY-MM-DD
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("MMM dd, yyyy"),
            DateTimeFormatter.ofPattern("dd MMM yyyy"),
            DateTimeFormatter.ofPattern("MMMM dd, yyyy")
        };
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                LocalDate date = LocalDate.parse(dateString.trim(), formatter);
                return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }
        
        // If no format matches, return original
        return dateString;
    }
    
    /**
     * Normalizes language code to lowercase ISO 639-1 format
     */
    public String normalizeLanguageCode(String languageCode) {
        if (languageCode == null || languageCode.trim().isEmpty()) {
            return null;
        }
        
        String normalized = languageCode.trim().toLowerCase();
        
        // Validate it's a 2-letter code
        if (normalized.matches("^[a-z]{2}$")) {
            return normalized;
        }
        
        // Map common language names to codes
        Map<String, String> languageMap = new HashMap<>();
        languageMap.put("english", "en");
        languageMap.put("french", "fr");
        languageMap.put("german", "de");
        languageMap.put("spanish", "es");
        languageMap.put("italian", "it");
        languageMap.put("portuguese", "pt");
        languageMap.put("japanese", "ja");
        languageMap.put("chinese", "zh");
        languageMap.put("korean", "ko");
        languageMap.put("russian", "ru");
        
        String code = languageMap.get(normalized);
        return code != null ? code : normalized;
    }
    
    /**
     * Normalizes parameters with location, date, and language awareness
     */
    public Map<String, Object> normalizeParametersWithContext(Map<String, Object> parameters) {
        if (parameters == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> normalized = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = entry.getKey().toLowerCase();
            Object value = entry.getValue();
            
            // Apply context-aware normalization
            if (key.contains("location") || key.contains("city") || key.contains("destination")) {
                if (value instanceof String) {
                    normalized.put(key, normalizeLocation((String) value));
                    continue;
                }
            }
            
            if (key.contains("date") || key.contains("departure") || key.contains("arrival")) {
                if (value instanceof String) {
                    normalized.put(key, normalizeDate((String) value));
                    continue;
                }
            }
            
            if (key.contains("language") || key.contains("lang")) {
                if (value instanceof String) {
                    normalized.put(key, normalizeLanguageCode((String) value));
                    continue;
                }
            }
            
            // Default normalization
            normalized.put(key, normalizeValue(value));
        }
        
        return normalized;
    }
}