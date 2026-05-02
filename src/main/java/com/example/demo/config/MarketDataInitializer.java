package com.example.demo.config;

import com.example.demo.service.MarketDataService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class MarketDataInitializer implements ApplicationRunner {

    private final MarketDataService marketDataService;

    public MarketDataInitializer(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @Override
    public void run(ApplicationArguments args) {
        String[] symbols = {"AAPL", "TSLA", "MSFT", "NVDA", "AMZN", "GOOGL", "META", "JPM", "BRK.B", "SPY"};
        double[] prices  = {189.42, 248.10, 415.80, 875.20, 192.30, 175.50, 512.40, 198.60, 412.30, 524.80};
        marketDataService.initializeMarket(symbols, prices);
    }
}
