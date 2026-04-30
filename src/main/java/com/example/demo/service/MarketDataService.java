package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;

/**
 * Service layer for market data management using the thread-safe cache
 */
@Service
public class MarketDataService {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);
    
    private final MarketDataCache cache;
    
    public MarketDataService() {
        this.cache = new MarketDataCache();
    }
    
    /**
     * Initialize market data with symbols
     */
    public void initializeMarket(String[] symbols, double[] prices) {
        if (symbols.length != prices.length) {
            throw new IllegalArgumentException("Symbols and prices arrays must have the same length");
        }
        
        for (int i = 0; i < symbols.length; i++) {
            cache.initializePrice(symbols[i], prices[i]);
        }
    }
    
    /**
     * Update price for a symbol using atomic updatePrice method
     */
    public boolean updateMarketPrice(String symbol, double newPrice) {
        return cache.updatePrice(symbol, newPrice);
    }
    
    /**
     * Get current price
     */
    public Optional<Double> getPrice(String symbol) {
        return cache.getPrice(symbol);
    }
    
    /**
     * Get full price data
     */
    public Optional<MarketDataCache.PriceData> getPriceData(String symbol) {
        return cache.getPriceData(symbol);
    }
    
    /**
     * Get all symbols
     */
    public java.util.Set<String> getAllSymbols() {
        return cache.getAllSymbols();
    }
}
