package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

/**
 * Thread-safe market data cache using ConcurrentHashMap.
 * Provides atomic price updates and high-performance concurrent access.
 */
@Component
public class MarketDataCache {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketDataCache.class);
    
    /**
     * Inner class to hold market data for a symbol
     */
    public static class PriceData {
        private double price;
        private long lastUpdated;
        private int updateCount;
        
        public PriceData(double price) {
            this.price = price;
            this.lastUpdated = System.currentTimeMillis();
            this.updateCount = 1;
        }
        
        public PriceData(double price, int updateCount) {
            this.price = price;
            this.lastUpdated = System.currentTimeMillis();
            this.updateCount = updateCount;
        }
        
        public double getPrice() {
            return price;
        }
        
        public long getLastUpdated() {
            return lastUpdated;
        }
        
        public int getUpdateCount() {
            return updateCount;
        }
        
        @Override
        public String toString() {
            return "PriceData{" +
                    "price=" + price +
                    ", lastUpdated=" + lastUpdated +
                    ", updateCount=" + updateCount +
                    '}';
        }
    }
    
    private final ConcurrentHashMap<String, PriceData> priceCache;
    
    public MarketDataCache() {
        this.priceCache = new ConcurrentHashMap<>();
    }
    
    /**
     * Initialize cache with a symbol and price
     */
    public void initializePrice(String symbol, double initialPrice) {
        priceCache.putIfAbsent(symbol, new PriceData(initialPrice));
        logger.info("Initialized price for {}: {}", symbol, initialPrice);
    }
    
    /**
     * Update price atomically using computeIfPresent.
     */
    public boolean updatePrice(String symbol, Double price) {
        if (symbol == null || price == null) {
            logger.warn("Invalid parameters: symbol={}, price={}", symbol, price);
            return false;
        }
        
        PriceData updated = priceCache.computeIfPresent(symbol, (key, oldData) -> {
            double oldPrice = oldData.getPrice();
            logger.debug("Updating {} from {} to {}", key, oldPrice, price);
            PriceData newData = new PriceData(price);
            newData.updateCount = oldData.getUpdateCount() + 1;
            return newData;
        });
        
        if (updated == null) {
            logger.warn("Symbol {} not found in cache, no update performed", symbol);
            return false;
        }
        
        return true;
    }
    
    /**
     * Get current price for a symbol
     */
    public Optional<Double> getPrice(String symbol) {
        PriceData data = priceCache.get(symbol);
        return data != null ? Optional.of(data.getPrice()) : Optional.empty();
    }
    
    /**
     * Get full price data including metadata
     */
    public Optional<PriceData> getPriceData(String symbol) {
        return Optional.ofNullable(priceCache.get(symbol));
    }
    
    /**
     * Get cache size
     */
    public int size() {
        return priceCache.size();
    }
    
    /**
     * Check if symbol exists in cache
     */
    public boolean containsSymbol(String symbol) {
        return priceCache.containsKey(symbol);
    }
    
    /**
     * Get all symbols in cache
     */
    public java.util.Set<String> getAllSymbols() {
        return priceCache.keySet();
    }
    
    /**
     * Insert or update a price regardless of whether it exists
     */
    public void upsertPrice(String symbol, double price) {
        if (symbol == null) {
            logger.warn("Cannot upsert price for null symbol");
            return;
        }
        priceCache.put(symbol, new PriceData(price));
        logger.debug("Upserted price for {}: {}", symbol, price);
    }
    
    /**
     * Remove a symbol from the cache
     */
    public Optional<PriceData> removeSymbol(String symbol) {
        PriceData removed = priceCache.remove(symbol);
        if (removed != null) {
            logger.debug("Removed symbol {} from cache", symbol);
        } else {
            logger.debug("Symbol {} not found in cache, nothing to remove", symbol);
        }
        return Optional.ofNullable(removed);
    }
    
    /**
     * Update price using a calculation function
     */
    public boolean updatePriceByCalculation(String symbol, java.util.function.Function<Double, Double> calculator) {
        if (symbol == null || calculator == null) {
            logger.warn("Invalid parameters for updatePriceByCalculation: symbol={}, calculator={}", symbol, calculator);
            return false;
        }
        
        PriceData updated = priceCache.computeIfPresent(symbol, (key, existing) -> {
            double newPrice = calculator.apply(existing.getPrice());
            logger.debug("Updated {} using calculation: {} -> {}", key, existing.getPrice(), newPrice);
            return new PriceData(newPrice, existing.getUpdateCount() + 1);
        });
        
        return updated != null;
    }
    
    /**
     * Return a stats snapshot as a formatted string
     */
    public String getCacheStats() {
        return String.format("Cache Stats: totalSymbols=%d, symbols=%s", 
                           priceCache.size(), priceCache.keySet());
    }
}
