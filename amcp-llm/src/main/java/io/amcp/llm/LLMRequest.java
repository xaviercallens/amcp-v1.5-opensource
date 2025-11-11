package io.amcp.llm;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Unified LLM Request model for AMCP v1.6
 * Works across all LLM providers
 */
public class LLMRequest {
    
    private String prompt;
    private List<Message> messages;
    private String model;
    private Double temperature;
    private Integer maxTokens;
    private Double topP;
    private Integer topK;
    private String systemPrompt;
    private boolean stream;
    
    public LLMRequest() {
        this.messages = new ArrayList<>();
        this.stream = false;
    }
    
    public LLMRequest(String prompt) {
        this();
        this.prompt = prompt;
    }
    
    // Builder pattern
    public static LLMRequestBuilder builder() {
        return new LLMRequestBuilder();
    }
    
    // Getters and setters
    public String getPrompt() {
        return prompt;
    }
    
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
    
    public List<Message> getMessages() {
        return messages;
    }
    
    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
    
    public void addMessage(String role, String content) {
        this.messages.add(new Message(role, content));
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public Double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
    
    public Integer getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    public Double getTopP() {
        return topP;
    }
    
    public void setTopP(Double topP) {
        this.topP = topP;
    }
    
    public Integer getTopK() {
        return topK;
    }
    
    public void setTopK(Integer topK) {
        this.topK = topK;
    }
    
    public String getSystemPrompt() {
        return systemPrompt;
    }
    
    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }
    
    public boolean isStream() {
        return stream;
    }
    
    public void setStream(boolean stream) {
        this.stream = stream;
    }
    
    /**
     * Message in conversation
     */
    public static class Message {
        private String role;
        private String content;
        
        public Message() {}
        
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Message message = (Message) o;
            return Objects.equals(role, message.role) && Objects.equals(content, message.content);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(role, content);
        }
    }
    
    /**
     * Builder for fluent API
     */
    public static class LLMRequestBuilder {
        private final LLMRequest request;
        
        public LLMRequestBuilder() {
            this.request = new LLMRequest();
        }
        
        public LLMRequestBuilder prompt(String prompt) {
            request.setPrompt(prompt);
            return this;
        }
        
        public LLMRequestBuilder model(String model) {
            request.setModel(model);
            return this;
        }
        
        public LLMRequestBuilder temperature(double temperature) {
            request.setTemperature(temperature);
            return this;
        }
        
        public LLMRequestBuilder maxTokens(int maxTokens) {
            request.setMaxTokens(maxTokens);
            return this;
        }
        
        public LLMRequestBuilder topP(double topP) {
            request.setTopP(topP);
            return this;
        }
        
        public LLMRequestBuilder topK(int topK) {
            request.setTopK(topK);
            return this;
        }
        
        public LLMRequestBuilder systemPrompt(String systemPrompt) {
            request.setSystemPrompt(systemPrompt);
            return this;
        }
        
        public LLMRequestBuilder addMessage(String role, String content) {
            request.addMessage(role, content);
            return this;
        }
        
        public LLMRequestBuilder stream(boolean stream) {
            request.setStream(stream);
            return this;
        }
        
        public LLMRequest build() {
            return request;
        }
    }
}
