package com.example.demo.controller;

import com.example.demo.dto.TradeExecutionResult;
import com.example.demo.model.Trade;
import com.example.demo.service.TradeExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/trades")
public class TradeController {
    
    @Autowired
    private TradeExecutionService tradeExecutionService;
    
    /**
     * Execute a single trade asynchronously
     * POST /api/trades/execute
     */
    @PostMapping("/execute")
    public CompletableFuture<ResponseEntity<TradeExecutionResult>> executeTrade(@RequestBody Trade trade, Principal principal) {
        String username = principal != null ? principal.getName() : null;
        return tradeExecutionService.executeTrade(trade, username)
            .thenApply(result -> ResponseEntity.ok(result))
            .exceptionally(ex -> ResponseEntity.status(500).body(
                new TradeExecutionResult("ERROR", "N/A", 0, 0.0, "N/A", "FAILED", System.currentTimeMillis())
            ));
    }
    
    /**
     * Execute a trade with timeout and fallback
     * POST /api/trades/execute-with-timeout?timeoutMs=5000
     */
    @PostMapping("/execute-with-timeout")
    public CompletableFuture<ResponseEntity<TradeExecutionResult>> executeTradeWithTimeout(
            @RequestBody Trade trade,
            @RequestParam(defaultValue = "5000") long timeoutMs) {
        return tradeExecutionService.executeTrade(trade)
            .orTimeout(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            .thenApply(result -> ResponseEntity.ok(result))
            .exceptionally(ex -> ResponseEntity.status(500).body(
                new TradeExecutionResult("ERROR", "N/A", 0, 0.0, "N/A", "FAILED", System.currentTimeMillis())
            ));
    }
    
    /**
     * Get executor thread pool stats
     * GET /api/trades/executor-stats
     */
    @GetMapping("/executor-stats")
    public ResponseEntity<String> getExecutorStats() {
        return ResponseEntity.ok(tradeExecutionService.getExecutorStats());
    }
}
