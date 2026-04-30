package com.example.demo.controller;

import com.example.demo.service.MarketDataService;
import com.example.demo.service.MarketDataCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/api/market")
public class MarketDataController {
    
    @Autowired
    private MarketDataService marketDataService;
    
    /**
     * Initialize market with symbols and prices
     * POST /api/market/initialize
     */
    @PostMapping("/initialize")
    public ResponseEntity<String> initializeMarket(@RequestBody MarketInitRequest request) {
        try {
            marketDataService.initializeMarket(request.getSymbols(), request.getPrices());
            return ResponseEntity.ok("Market initialized with " + request.getSymbols().length + " symbols");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * Update price for a symbol using atomic updatePrice
     * PUT /api/market/price/{symbol}
     */
    @PutMapping("/price/{symbol}")
    public ResponseEntity<String> updatePrice(
            @PathVariable String symbol,
            @RequestBody PriceUpdateRequest request) {
        
        boolean updated = marketDataService.updateMarketPrice(symbol, request.getPrice());
        
        if (updated) {
            return ResponseEntity.ok("Price for " + symbol + " updated to " + request.getPrice());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get current price
     * GET /api/market/price/{symbol}
     */
    @GetMapping("/price/{symbol}")
    public ResponseEntity<?> getPrice(@PathVariable String symbol) {
        Optional<Double> price = marketDataService.getPrice(symbol);
        
        if (price.isPresent()) {
            return ResponseEntity.ok(new PriceResponse(symbol, price.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get all symbols in cache
     * GET /api/market/symbols
     */
    @GetMapping("/symbols")
    public ResponseEntity<?> getAllSymbols() {
        return ResponseEntity.ok(marketDataService.getAllSymbols());
    }
    
    // DTOs for request/response
    
    public static class MarketInitRequest {
        private String[] symbols;
        private double[] prices;
        
        public String[] getSymbols() {
            return symbols;
        }
        
        public void setSymbols(String[] symbols) {
            this.symbols = symbols;
        }
        
        public double[] getPrices() {
            return prices;
        }
        
        public void setPrices(double[] prices) {
            this.prices = prices;
        }
    }
    
    public static class PriceUpdateRequest {
        private double price;
        
        public double getPrice() {
            return price;
        }
        
        public void setPrice(double price) {
            this.price = price;
        }
    }
    
    public static class PriceResponse {
        private String symbol;
        private double price;
        
        public PriceResponse(String symbol, double price) {
            this.symbol = symbol;
            this.price = price;
        }
        
        public String getSymbol() {
            return symbol;
        }
        
        public double getPrice() {
            return price;
        }
    }
}
