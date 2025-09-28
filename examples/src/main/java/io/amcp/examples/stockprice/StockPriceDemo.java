package io.amcp.examples.stockprice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Standalone Stock Price Demo for AMCP v1.5 Enterprise Edition
 */
public class StockPriceDemo {
    
    private static final Random random = new Random();
    
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║        AMCP v1.5 Enterprise Edition Stock Price Demo         ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        try {
            demonstrateStockPriceLookup();
            System.out.println();
            demonstratePortfolioManagement();
            System.out.println();
            demonstrateAvailableSymbols();
            System.out.println();
            System.out.println("✅ Demo completed successfully!");
        } catch (Exception e) {
            System.err.println("❌ Error during demo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void demonstrateStockPriceLookup() {
        System.out.println("📊 Stock Price Lookup Demonstration:");
        System.out.println("-".repeat(35));
        
        String[] symbols = {"AAPL", "GOOGL", "MSFT", "TSLA", "AMZN"};
        for (String symbol : symbols) {
            BigDecimal price = getSimulatedStockPrice(symbol);
            System.out.printf("%-8s: $%,.2f%n", symbol, price);
        }
    }
    
    private static void demonstratePortfolioManagement() {
        System.out.println("💼 Portfolio Management Demonstration:");
        System.out.println("-".repeat(38));
        
        Map<String, Integer> portfolio = new HashMap<>();
        portfolio.put("AAPL", 100);
        portfolio.put("GOOGL", 50);
        portfolio.put("MSFT", 75);
        
        BigDecimal totalValue = BigDecimal.ZERO;
        System.out.println("Portfolio Holdings:");
        
        for (Map.Entry<String, Integer> holding : portfolio.entrySet()) {
            String symbol = holding.getKey();
            int shares = holding.getValue();
            BigDecimal price = getSimulatedStockPrice(symbol);
            BigDecimal holdingValue = price.multiply(BigDecimal.valueOf(shares));
            totalValue = totalValue.add(holdingValue);
            
            System.out.printf("  %-8s: %3d shares @ $%,.2f = $%,.2f%n", 
                symbol, shares, price, holdingValue);
        }
        
        System.out.println();
        System.out.printf("Total Portfolio Value: $%,.2f%n", totalValue);
    }
    
    private static void demonstrateAvailableSymbols() {
        System.out.println("📈 Available Stock Symbols:");
        System.out.println("-".repeat(28));
        
        String[] symbols = {
            "AAPL", "GOOGL", "MSFT", "TSLA", "AMZN", 
            "META", "NFLX", "NVDA", "AMD", "INTC"
        };
        
        for (int i = 0; i < symbols.length; i++) {
            System.out.print(symbols[i]);
            if (i < symbols.length - 1) {
                System.out.print(", ");
            }
            if ((i + 1) % 5 == 0) {
                System.out.println();
            }
        }
        if (symbols.length % 5 != 0) {
            System.out.println();
        }
    }
    
    private static BigDecimal getSimulatedStockPrice(String symbol) {
        Map<String, Double> basePrices = new HashMap<>();
        basePrices.put("AAPL", 175.0);
        basePrices.put("GOOGL", 140.0);
        basePrices.put("MSFT", 380.0);
        basePrices.put("TSLA", 220.0);
        basePrices.put("AMZN", 155.0);
        basePrices.put("META", 320.0);
        basePrices.put("NFLX", 450.0);
        basePrices.put("NVDA", 480.0);
        basePrices.put("AMD", 110.0);
        basePrices.put("INTC", 45.0);
        
        double basePrice = basePrices.getOrDefault(symbol, 100.0);
        double variation = (random.nextGaussian() * 0.05);
        double finalPrice = basePrice * (1 + variation);
        
        return BigDecimal.valueOf(finalPrice).setScale(2, RoundingMode.HALF_UP);
    }
}
