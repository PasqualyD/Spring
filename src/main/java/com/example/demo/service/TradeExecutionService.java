package com.example.demo.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.demo.dto.TradeExecutionResult;
import com.example.demo.model.Trade;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TradeExecutionService {
    
    private static final Logger logger = LoggerFactory.getLogger(TradeExecutionService.class);
    
    private final ThreadPoolTaskExecutor tradeExecutor;
    
    public TradeExecutionService(@Qualifier("tradeExecutor") ThreadPoolTaskExecutor tradeExecutor) {
        this.tradeExecutor = tradeExecutor;
    }
    
    /**
     * Execute a trade asynchronously using CompletableFuture
     * Simulates a call to an external FIX engine
     */
    public CompletableFuture<TradeExecutionResult> executeTrade(Trade trade) {
        logger.info("Initiating async trade execution for: {}", trade);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate FIX engine processing time
                simulateFIXEngineCall(trade);
                
                // Generate mock execution result
                String executionId = generateExecutionId();
                double executedPrice = trade.getPrice() + (ThreadLocalRandom.current().nextDouble() - 0.5);
                int executedQuantity = trade.getQuantity();
                
                logger.info("Trade executed with ID: {} for {}", executionId, trade.getSymbol());
                
                return new TradeExecutionResult(
                    executionId,
                    trade.getSymbol(),
                    executedQuantity,
                    executedPrice,
                    trade.getSide(),
                    "FILLED",
                    System.currentTimeMillis()
                );
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Trade execution interrupted", e);
                throw new RuntimeException("Trade execution was interrupted", e);
            }
        }, tradeExecutor);
    }
    
    /**
     * Simulates a call to the external FIX engine
     */
    private void simulateFIXEngineCall(Trade trade) throws InterruptedException {
        long processingTime = ThreadLocalRandom.current().nextLong(500, 2000);
        logger.debug("FIX engine processing trade: {} (simulating {}ms delay)", trade.getSymbol(), processingTime);
        Thread.sleep(processingTime);
    }
    
    /**
     * Generate a unique execution ID (simulating FIX engine response)
     */
    private String generateExecutionId() {
        return "EXEC-" + System.nanoTime();
    }
    
    /**
     * Get executor stats for monitoring
     */
    public String getExecutorStats() {
        return String.format(
            "Active: %d, Core: %d, Max: %d, Queue Size: %d",
            tradeExecutor.getActiveCount(),
            tradeExecutor.getCorePoolSize(),
            tradeExecutor.getMaxPoolSize(),
            tradeExecutor.getThreadPoolExecutor().getQueue().size()
        );
    }
}
