package io.amcp.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Represents an MCP (Model Context Protocol) tool definition.
 * 
 * Tools are stateless functions that can be called by LLMs or agents.
 * Examples: Calculator, Weather API, Database Query, Web Search, etc.
 * 
 * @see <a href="https://modelcontextprotocol.io">MCP Specification</a>
 */
public class MCPTool {
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("parameters")
    private MCPToolParameters parameters;
    
    @JsonProperty("returns")
    private Map<String, Object> returns;
    
    @JsonProperty("category")
    private String category;
    
    @JsonProperty("version")
    private String version;

    public MCPTool() {
    }

    public MCPTool(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // Getters and Setters
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MCPToolParameters getParameters() {
        return parameters;
    }

    public void setParameters(MCPToolParameters parameters) {
        this.parameters = parameters;
    }

    public Map<String, Object> getReturns() {
        return returns;
    }

    public void setReturns(Map<String, Object> returns) {
        this.returns = returns;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "MCPTool{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", category='" + category + '\'' +
                '}';
    }

    /**
     * Tool parameters definition.
     */
    public static class MCPToolParameters {
        
        @JsonProperty("type")
        private String type = "object";
        
        @JsonProperty("properties")
        private Map<String, ParameterProperty> properties;
        
        @JsonProperty("required")
        private List<String> required;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, ParameterProperty> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, ParameterProperty> properties) {
            this.properties = properties;
        }

        public List<String> getRequired() {
            return required;
        }

        public void setRequired(List<String> required) {
            this.required = required;
        }
    }

    /**
     * Individual parameter property.
     */
    public static class ParameterProperty {
        
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("description")
        private String description;
        
        @JsonProperty("enum")
        private List<Object> enumValues;

        public ParameterProperty() {
        }

        public ParameterProperty(String type, String description) {
            this.type = type;
            this.description = description;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<Object> getEnumValues() {
            return enumValues;
        }

        public void setEnumValues(List<Object> enumValues) {
            this.enumValues = enumValues;
        }
    }
}
