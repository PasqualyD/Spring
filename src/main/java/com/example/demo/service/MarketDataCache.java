package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

/**
 * Thread-safe market data cache using ConcurrentHashMap.
 * Provides atomic price updates and high-performance concurrent access.
 */
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
}
