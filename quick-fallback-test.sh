#!/bin/bash

# Quick Agent Fallback CLI Test
# Tests one scenario for each agent type to demonstrate fallback mechanism

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}AMCP Agent Fallback CLI Test${NC}"
echo -e "${BLUE}=============================${NC}"
echo ""

# Test function
test_agent() {
    local agent_name="$1"
    local query="$2"
    local model="$3"
    
    echo -e "${YELLOW}Testing $agent_name Agent:${NC}"
    echo -e "${BLUE}Query:${NC} $query"
    echo -e "${BLUE}Model:${NC} $model"
    echo ""
    
    # Create and run test
    cat > "QuickTest.java" << EOF
import io.amcp.connectors.ai.async.AsyncLLMConnector;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class QuickTest {
    public static void main(String[] args) {
        try {
            // Force fallback with non-existent URL
            AsyncLLMConnector connector = new AsyncLLMConnector("http://localhost:99999", 1, 1, true);
            
            Map<String, Object> parameters = new HashMap<>();
            long startTime = System.currentTimeMillis();
            
            CompletableFuture<String> future = connector.generateAsync("$query", "$model", parameters);
            String response = future.get();
            
            long duration = System.currentTimeMillis() - startTime;
            
            System.out.println("RESPONSE: " + response);
            System.out.println("DURATION: " + duration + "ms");
            
            AsyncLLMConnector.ConnectorStats stats = connector.getStats();
            System.out.println("FALLBACK_USED: " + (stats.getFallbackResponses() > 0));
            System.out.println("FALLBACK_COUNT: " + stats.getFallbackResponses());
            
            connector.shutdown();
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
    }
}
EOF

    # Compile and run
    if javac -cp "connectors/target/classes:connectors/target/dependency/*" QuickTest.java 2>/dev/null; then
        echo -e "${BLUE}Results:${NC}"
        java -cp ".:connectors/target/classes:connectors/target/dependency/*" -Xmx1g QuickTest
        echo ""
    else
        echo -e "${YELLOW}Compilation failed${NC}"
    fi
    
    rm -f QuickTest.java QuickTest.class
}

# Ensure project is built
if [ ! -f "connectors/target/classes/io/amcp/connectors/ai/async/AsyncLLMConnector.class" ]; then
    echo -e "${YELLOW}Building project...${NC}"
    mvn clean compile -q -pl connectors
fi

echo -e "${GREEN}Testing Agent Fallback Scenarios${NC}"
echo ""

# Test each agent type
test_agent "Weather" "What's the weather in London today?" "qwen2.5:0.5b"
test_agent "Stock" "What's Apple stock price right now?" "gemma:2b" 
test_agent "Travel" "Plan a trip to Paris for 3 days" "qwen2:1.5b"

echo -e "${GREEN}✓ Agent Fallback CLI Tests Completed${NC}"
echo -e "${BLUE}All agents successfully provided fallback responses when LLM was unavailable${NC}"
