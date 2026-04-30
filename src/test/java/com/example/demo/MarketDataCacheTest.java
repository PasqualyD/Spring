package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MarketDataCacheTest {
    
    private MarketDataCache cache;
    
    @BeforeEach
    public void setUp() {
        cache = new MarketDataCache();
    }
    
    @Test
    public void testInitializeAndGetPrice() {
        cache.initializePrice("AAPL", 150.50);
        Optional<Double> price = cache.getPrice("AAPL");
        
        assertTrue(price.isPresent());
        assertEquals(150.50, price.get());
    }
    
    @Test
    public void testUpdatePriceWithComputeIfPresent() {
        cache.initializePrice("GOOGL", 140.00);
        
        // Update existing symbol - should succeed
        boolean updated = cache.updatePrice("GOOGL", 145.50);
        assertTrue(updated);
        assertEquals(145.50, cache.getPrice("GOOGL").get());
        
        // Try to update non-existing symbol - should fail
        boolean notUpdated = cache.updatePrice("INVALID", 100.00);
        assertFalse(notUpdated);
    }
    
    @Test
    public void testUpdatePriceIsAtomic() {
        cache.initializePrice("MSFT", 300.00);
        
        Optional<MarketDataCache.PriceData> priceDataBefore = cache.getPriceData("MSFT");
        assertEquals(1, priceDataBefore.get().getUpdateCount());
        
        cache.updatePrice("MSFT", 310.00);
        
        Optional<MarketDataCache.PriceData> priceDataAfter = cache.getPriceData("MSFT");
        assertEquals(2, priceDataAfter.get().getUpdateCount());
        assertEquals(310.00, priceDataAfter.get().getPrice());
    }
    
    @Test
    public void testUpsertPrice() {
        // Upsert new symbol
        cache.upsertPrice("TSLA", 200.00);
        assertEquals(200.00, cache.getPrice("TSLA").get());
        
        // Upsert existing symbol
        cache.upsertPrice("TSLA", 210.00);
        assertEquals(210.00, cache.getPrice("TSLA").get());
    }
    
    @Test
    public void testUpdatePriceByCalculation() {
        cache.initializePrice("NFLX", 400.00);
        
        // Apply 10% increase
        cache.updatePriceByCalculation("NFLX", price -> price * 1.10);
        assertEquals(440.00, cache.getPrice("NFLX").get());
        
        // Apply absolute change
        cache.updatePriceByCalculation("NFLX", price -> price + 50);
        assertEquals(490.00, cache.getPrice("NFLX").get());
    }
    
    @Test
    public void testThreadSafetyConcurrentUpdates() throws InterruptedException {
        cache.initializePrice("THREAD-SAFE", 100.00);
        
        int numberOfThreads = 10;
        int updatesPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        
        // Each thread updates the price multiple times
        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < updatesPerThread; j++) {
                    cache.updatePrice("THREAD-SAFE", 100.00 + j);
                }
                latch.countDown();
            });
        }
        
        latch.await();
        executor.shutdown();
        
        // Verify final state is consistent
        Optional<MarketDataCache.PriceData> finalData = cache.getPriceData("THREAD-SAFE");
        assertTrue(finalData.isPresent());
        // Update count should be numberOfThreads * updatesPerThread + 1 (initial)
        assertEquals(numberOfThreads * updatesPerThread + 1, finalData.get().getUpdateCount());
    }
    
    @Test
    public void testThreadSafetyMultipleSymbols() throws InterruptedException {
        String[] symbols = {"AAPL", "GOOGL", "MSFT", "AMZN", "TSLA"};
        double[] prices = {150.0, 140.0, 300.0, 3200.0, 200.0};
        
        for (int i = 0; i < symbols.length; i++) {
            cache.initializePrice(symbols[i], prices[i]);
        }
        
        int numberOfThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // Each thread updates different symbols
        for (int threadId = 0; threadId < numberOfThreads; threadId++) {
            final int tid = threadId;
            executor.submit(() -> {
                String symbol = symbols[tid];
                for (int j = 0; j < 50; j++) {
                    if (cache.updatePrice(symbol, prices[tid] + j)) {
                        successCount.incrementAndGet();
                    }
                }
                latch.countDown();
            });
        }
        
        latch.await();
        executor.shutdown();
        
        // All updates should succeed (50 per thread)
        assertEquals(numberOfThreads * 50, successCount.get());
    }
    
    @Test
    public void testCacheStats() {
        cache.initializePrice("AAPL", 150.0);
        cache.initializePrice("GOOGL", 140.0);
        
        assertEquals(2, cache.size());
        assertTrue(cache.containsSymbol("AAPL"));
        assertFalse(cache.containsSymbol("INVALID"));
        
        String stats = cache.getCacheStats();
        assertTrue(stats.contains("AAPL"));
        assertTrue(stats.contains("GOOGL"));
    }
    
    @Test
    public void testRemoveSymbol() {
        cache.initializePrice("REMOVE-ME", 100.0);
        assertEquals(1, cache.size());
        
        Optional<MarketDataCache.PriceData> removed = cache.removeSymbol("REMOVE-ME");
        assertTrue(removed.isPresent());
        assertEquals(0, cache.size());
    }
}
