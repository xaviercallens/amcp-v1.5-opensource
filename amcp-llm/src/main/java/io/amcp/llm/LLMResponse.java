package io.amcp.llm;

import java.util.HashMap;
import java.util.Map;

/**
 * Unified LLM Response model for AMCP v1.6
 * Normalizes responses from different providers
 */
public class LLMResponse {
    
    private String content;
    private String model;
    private LLMProvider provider;
    private int tokenCount;
    private long responseTimeMs;
    private boolean cached;
    private Map<String, Object> metadata;
    private String error;
    
    public LLMResponse() {
        this.metadata = new HashMap<>();
        this.cached = false;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public LLMProvider getProvider() {
        return provider;
    }
    
    public void setProvider(LLMProvider provider) {
        this.provider = provider;
    }
    
    public int getTokenCount() {
        return tokenCount;
    }
    
    public void setTokenCount(int tokenCount) {
        this.tokenCount = tokenCount;
    }
    
    public long getResponseTimeMs() {
        return responseTimeMs;
    }
    
    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }
    
    public boolean isCached() {
        return cached;
    }
    
    public void setCached(boolean cached) {
        this.cached = cached;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public boolean hasError() {
        return error != null && !error.isEmpty();
    }
    
    @Override
    public String toString() {
        return "LLMResponse{" +
                "provider=" + provider +
                ", model='" + model + '\'' +
                ", tokenCount=" + tokenCount +
                ", responseTimeMs=" + responseTimeMs +
                ", cached=" + cached +
                ", hasError=" + hasError() +
                '}';
    }
}
