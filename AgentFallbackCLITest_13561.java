import io.amcp.connectors.ai.async.AsyncLLMConnector;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AgentFallbackCLITest_13561 {
    public static void main(String[] args) {
        try {
            // Force fallback mode with non-existent URL
            AsyncLLMConnector connector = new AsyncLLMConnector("http://localhost:99999", 1, 2, true);
            
            Map<String, Object> parameters = new HashMap<>();
            String query = "What's the weather like in Paris today?";
            String model = "qwen2.5:0.5b";
            
            System.out.println("AGENT_TYPE: Weather");
            System.out.println("QUERY: " + query);
            System.out.println("MODEL: " + model);
            System.out.println("FALLBACK_URL: http://localhost:99999");
            System.out.println("---");
            
            long startTime = System.currentTimeMillis();
            CompletableFuture<String> future = connector.generateAsync(query, model, parameters);
            String response = future.get();
            long duration = System.currentTimeMillis() - startTime;
            
            System.out.println("RESPONSE: " + response);
            System.out.println("DURATION_MS: " + duration);
            System.out.println("---");
            
            // Get statistics
            AsyncLLMConnector.ConnectorStats stats = connector.getStats();
            System.out.println("TOTAL_REQUESTS: " + stats.getTotalRequests());
            System.out.println("FALLBACK_RESPONSES: " + stats.getFallbackResponses());
            System.out.println("FAILED_REQUESTS: " + stats.getFailedRequests());
            System.out.println("AVG_LATENCY_MS: " + String.format("%.2f", stats.getAvgLatencyMs()));
            
            if (stats.getFallbackStats() != null) {
                System.out.println("FALLBACK_ATTEMPTS: " + stats.getFallbackStats().getFallbackAttempts());
                System.out.println("SUCCESSFUL_FALLBACKS: " + stats.getFallbackStats().getSuccessfulFallbacks());
                System.out.println("SUCCESS_RATE: " + String.format("%.2f%%", stats.getFallbackStats().getSuccessRate() * 100));
            }
            
            connector.shutdown();
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
