package com.example.demo.service;

import com.example.demo.dto.TradeExecutionResult;
import com.example.demo.model.Trade;
import com.example.demo.model.User;
import com.example.demo.repository.TradeRepository;
import com.example.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TradeExecutionService {
    
    private static final Logger logger = LoggerFactory.getLogger(TradeExecutionService.class);
    
    private final ThreadPoolTaskExecutor tradeExecutor;
    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;
    
    public TradeExecutionService(@Qualifier("tradeExecutor") ThreadPoolTaskExecutor tradeExecutor,
                                 TradeRepository tradeRepository,
                                 UserRepository userRepository) {
        this.tradeExecutor = tradeExecutor;
        this.tradeRepository = tradeRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * Execute a trade asynchronously using CompletableFuture
     * Simulates a call to an external FIX engine
     */
    public CompletableFuture<TradeExecutionResult> executeTrade(Trade trade, String username) {
        logger.info("Initiating async trade execution for: {}", trade);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate FIX engine processing time
                simulateFIXEngineCall(trade);
                
                // Generate mock execution result
                String executionId = generateExecutionId();
                double executedPrice = trade.getPrice() + (ThreadLocalRandom.current().nextDouble() - 0.5);
                int executedQuantity = trade.getQuantity();
                
                TradeExecutionResult result = new TradeExecutionResult(
                    executionId,
                    trade.getSymbol(),
                    executedQuantity,
                    executedPrice,
                    trade.getSide(),
                    "FILLED",
                    System.currentTimeMillis()
                );

                persistExecutedTrade(trade, username, result);
                logger.info("Trade executed with ID: {} for {}", executionId, trade.getSymbol());
                return result;
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
    public CompletableFuture<TradeExecutionResult> executeTrade(Trade trade) {
        return executeTrade(trade, null);
    }

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
    
    private void persistExecutedTrade(Trade trade, String username, TradeExecutionResult result) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            logger.warn("Unable to persist trade, user not found: {}", username);
            return;
        }

        trade.setStatus(result.getStatus());
        trade.setExecutedAt(LocalDateTime.now());
        trade.setPrice(result.getExecutedPrice());
        trade.setQuantity(result.getExecutedQuantity());
        trade.setUser(user);
        tradeRepository.save(trade);
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
