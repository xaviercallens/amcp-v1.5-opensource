package io.amcp.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for MCP (Model Context Protocol) tools.
 * 
 * Manages the catalog of tools that can be called via MCP.
 * Supports:
 * - Tool registration and discovery
 * - Dynamic tool addition/removal
 * - Tool schema generation
 * - Tool categorization
 * 
 * Agents can register themselves as tools to be callable by external LLMs.
 */
public class MCPToolRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(MCPToolRegistry.class);
    
    private final Map<String, MCPTool> tools = new ConcurrentHashMap<>();
    private final Map<String, List<MCPTool>> toolsByCategory = new ConcurrentHashMap<>();

    /**
     * Registers a new MCP tool.
     * 
     * @param tool The tool to register
     * @return true if registered, false if tool already exists
     */
    public boolean registerTool(MCPTool tool) {
        if (tool == null || tool.getName() == null) {
            logger.warn("Cannot register null tool or tool without name");
            return false;
        }
        
        if (tools.containsKey(tool.getName())) {
            logger.warn("Tool already registered: {}", tool.getName());
            return false;
        }
        
        tools.put(tool.getName(), tool);
        
        // Add to category index
        if (tool.getCategory() != null) {
            toolsByCategory.computeIfAbsent(tool.getCategory(), k -> new ArrayList<>())
                .add(tool);
        }
        
        logger.info("🔧 Registered MCP tool: {} (category: {})", 
            tool.getName(), tool.getCategory());
        
        return true;
    }

    /**
     * Unregisters an MCP tool.
     * 
     * @param toolName Name of the tool to unregister
     * @return true if unregistered, false if tool not found
     */
    public boolean unregisterTool(String toolName) {
        MCPTool tool = tools.remove(toolName);
        if (tool == null) {
            return false;
        }
        
        // Remove from category index
        if (tool.getCategory() != null) {
            List<MCPTool> categoryTools = toolsByCategory.get(tool.getCategory());
            if (categoryTools != null) {
                categoryTools.remove(tool);
                if (categoryTools.isEmpty()) {
                    toolsByCategory.remove(tool.getCategory());
                }
            }
        }
        
        logger.info("Unregistered MCP tool: {}", toolName);
        return true;
    }

    /**
     * Gets a tool by name.
     * 
     * @param toolName Name of the tool
     * @return The tool or null if not found
     */
    public MCPTool getTool(String toolName) {
        return tools.get(toolName);
    }

    /**
     * Gets all registered tools.
     * 
     * @return Unmodifiable collection of all tools
     */
    public Collection<MCPTool> getAllTools() {
        return Collections.unmodifiableCollection(tools.values());
    }

    /**
     * Gets tools by category.
     * 
     * @param category Category name
     * @return List of tools in category
     */
    public List<MCPTool> getToolsByCategory(String category) {
        List<MCPTool> categoryTools = toolsByCategory.get(category);
        return categoryTools != null ? 
            Collections.unmodifiableList(categoryTools) : 
            Collections.emptyList();
    }

    /**
     * Gets all tool names.
     * 
     * @return Set of tool names
     */
    public Set<String> getToolNames() {
        return Collections.unmodifiableSet(tools.keySet());
    }

    /**
     * Gets all categories.
     * 
     * @return Set of category names
     */
    public Set<String> getCategories() {
        return Collections.unmodifiableSet(toolsByCategory.keySet());
    }

    /**
     * Checks if a tool is registered.
     * 
     * @param toolName Name of the tool
     * @return true if tool exists
     */
    public boolean hasTool(String toolName) {
        return tools.containsKey(toolName);
    }

    /**
     * Gets count of registered tools.
     * 
     * @return Number of tools
     */
    public int getToolCount() {
        return tools.size();
    }

    /**
     * Clears all registered tools.
     */
    public void clear() {
        tools.clear();
        toolsByCategory.clear();
        logger.info("Cleared all MCP tools");
    }

    /**
     * Creates a tool registry pre-populated with common tools.
     * 
     * @return Registry with standard tools
     */
    public static MCPToolRegistry createWithDefaultTools() {
        MCPToolRegistry registry = new MCPToolRegistry();
        
        // Add standard tools
        registry.registerTool(createWeatherTool());
        registry.registerTool(createCalculatorTool());
        
        return registry;
    }

    /**
     * Creates a weather tool definition.
     */
    private static MCPTool createWeatherTool() {
        MCPTool tool = new MCPTool("weather", "Get current weather for a city");
        tool.setCategory("information");
        tool.setVersion("1.0.0");
        
        MCPTool.MCPToolParameters params = new MCPTool.MCPToolParameters();
        Map<String, MCPTool.ParameterProperty> properties = new HashMap<>();
        properties.put("city", new MCPTool.ParameterProperty("string", "City name"));
        properties.put("units", new MCPTool.ParameterProperty("string", "Temperature units (celsius/fahrenheit)"));
        params.setProperties(properties);
        params.setRequired(List.of("city"));
        tool.setParameters(params);
        
        Map<String, Object> returns = new HashMap<>();
        returns.put("type", "object");
        returns.put("description", "Weather information including temperature, conditions, humidity");
        tool.setReturns(returns);
        
        return tool;
    }

    /**
     * Creates a calculator tool definition.
     */
    private static MCPTool createCalculatorTool() {
        MCPTool tool = new MCPTool("calculator", "Perform mathematical calculations");
        tool.setCategory("utility");
        tool.setVersion("1.0.0");
        
        MCPTool.MCPToolParameters params = new MCPTool.MCPToolParameters();
        Map<String, MCPTool.ParameterProperty> properties = new HashMap<>();
        properties.put("expression", new MCPTool.ParameterProperty("string", "Mathematical expression to evaluate"));
        params.setProperties(properties);
        params.setRequired(List.of("expression"));
        tool.setParameters(params);
        
        Map<String, Object> returns = new HashMap<>();
        returns.put("type", "number");
        returns.put("description", "Result of the calculation");
        tool.setReturns(returns);
        
        return tool;
    }
}
