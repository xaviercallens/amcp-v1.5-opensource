package io.amcp.examples.stockprice;

import io.amcp.core.AgentContext;
import io.amcp.core.Event;
import io.amcp.core.DeliveryOptions;
import io.amcp.core.AgentID;
import io.amcp.messaging.EventBroker;
import io.amcp.messaging.impl.InMemoryEventBroker;
import io.amcp.mobility.MobilityManager;
import io.amcp.core.impl.SimpleAgentContext;
import io.amcp.mobility.impl.SimpleMobilityManager;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * AMCP v1.4 Stock Price Demo.
 * Simple demonstration of stock price monitoring agent capabilities.
 */
public class StockPriceDemo {
    
    public static void main(String[] args) {
        System.out.println("=== AMCP v1.4 Stock Price Agent Demo ===");
        
        try {
            // Initialize components
            EventBroker eventBroker = new InMemoryEventBroker();
            MobilityManager mobilityManager = new SimpleMobilityManager();
            
            // Create agent context
            AgentContext context = new SimpleAgentContext(eventBroker, mobilityManager);
            
            // Create the stock price agent
            StockPriceAgent stockAgent = new StockPriceAgent();
            
            // Register and activate agent
            context.registerAgent(stockAgent).get(5, TimeUnit.SECONDS);
            context.activateAgent(stockAgent.getAgentId()).get(5, TimeUnit.SECONDS);
            
            System.out.println("Stock Price Agent activated: " + stockAgent.getAgentId());
            
            // Simulate adding stocks to portfolio
            simulatePortfolioManagement(context, stockAgent);
            
            // Simulate price requests
            simulateStockPriceRequests(context, stockAgent);
            
            // Let the agent run for demonstration
            Thread.sleep(10000); // 10 seconds
            
            // Clean shutdown
            context.deactivateAgent(stockAgent.getAgentId());
            context.shutdown();
            
            System.out.println("Demo completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void simulatePortfolioManagement(AgentContext context, StockPriceAgent agent) {
        System.out.println("\n--- Portfolio Management Demo ---");
        
        try {
            // Add AAPL to portfolio
            Map<String, Object> addAAPL = new HashMap<>();
            addAAPL.put("action", "add");
            addAAPL.put("symbol", "AAPL");
            addAAPL.put("shares", 100);
            addAAPL.put("price", 150.50);
            
            Event portfolioEvent1 = Event.builder()
                .topic("portfolio.add")
                .payload(addAAPL)
                .sender(AgentID.named("demo-client"))
                .build();
            
            context.publishEvent(portfolioEvent1);
            Thread.sleep(1000);
            
            // Add GOOGL to portfolio
            Map<String, Object> addGOOGL = new HashMap<>();
            addGOOGL.put("action", "add");
            addGOOGL.put("symbol", "GOOGL");
            addGOOGL.put("shares", 50);
            addGOOGL.put("price", 2800.75);
            
            Event portfolioEvent2 = Event.builder()
                .topic("portfolio.add")
                .payload(addGOOGL)
                .sender(AgentID.named("demo-client"))
                .build();
            
            context.publishEvent(portfolioEvent2);
            Thread.sleep(1000);
            
            System.out.println("Portfolio positions added");
            
        } catch (Exception e) {
            System.err.println("Error in portfolio simulation: " + e.getMessage());
        }
    }
    
    private static void simulateStockPriceRequests(AgentContext context, StockPriceAgent agent) {
        System.out.println("\n--- Stock Price Request Demo ---");
        
        try {
            String[] symbols = {"AAPL", "GOOGL", "MSFT", "TSLA", "AMZN"};
            
            for (String symbol : symbols) {
                Map<String, Object> priceRequest = new HashMap<>();
                priceRequest.put("symbol", symbol);
                priceRequest.put("requestId", "req_" + System.currentTimeMillis());
                
                Event priceEvent = Event.builder()
                    .topic("stock.price.request")
                    .payload(priceRequest)
                    .sender(AgentID.named("demo-client"))
                    .deliveryOptions(DeliveryOptions.reliable())
                    .build();
                
                context.publishEvent(priceEvent);
                Thread.sleep(500); // Brief pause between requests
                
                System.out.println("Requested price for: " + symbol);
            }
            
        } catch (Exception e) {
            System.err.println("Error in price request simulation: " + e.getMessage());
        }
    }
}